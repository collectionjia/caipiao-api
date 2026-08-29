package com.xytl.project.caipiaoapi.service.piaocoder;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xytl.project.caipiaoapi.config.RedisService;
import com.xytl.project.caipiaoapi.domain.data.BanlanceResult;
import com.xytl.project.caipiaoapi.domain.data.CachedRealPayDetail;
import com.xytl.project.caipiaoapi.domain.data.CachedRealPayResult;
import com.xytl.project.caipiaoapi.domain.data.ResultList;
import com.xytl.project.caipiaoapi.domain.data.TimeList;
import com.xytl.project.caipiaoapi.domain.piaocoder.PiaoCoder;
import com.xytl.project.caipiaoapi.service.user.SystemConfigService;
import com.xytl.project.caipiaoapi.utils.json.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class PiaoCoderService {

    static {
        try {
            System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
            System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
        } catch (Throwable ignore) {
        }
    }

    public static volatile boolean TOKEN_HAVE_ERROR = false;
	@Autowired(required = false)
	private PiaoCoderMapper coderMapper;
	@Resource
	private SystemConfigService systemConfigService;

    @Autowired
    RedisService redisService;

    @Resource
    MyBean mybean;

    private String apiurl="https://bwapi-cf.rhgknx.com:2083";

    private static final String REAL_PAY_LOCK_PREFIX = "realPayLock:";
    private static final String REAL_PAY_DONE_PREFIX = "realPayDone:";
    private static final String PLAN_ROUND_LOCK_PREFIX = "planRoundLock:";
    private static final String PAY_BATCH_DONE_PREFIX = "payBatchDone:";
    private static final String CRON_JOB_LOCK_PREFIX = "cronJobLock:";

    private final ThreadLocal<PayBatchContext> payBatchContext = new ThreadLocal<>();
    private final ThreadLocal<WinBatchContext> winBatchContext = new ThreadLocal<>();

    @Value("${caipiao.parallel.enabled:true}")
    private boolean parallelEnabled;

    @Value("${caipiao.parallel.threads:10}")
    private int parallelThreads;

    private ExecutorService parallelExecutor;

    private static class RoadTask {
        private final String playNo;
        private final String numstr;

        private RoadTask(String playNo, String numstr) {
            this.playNo = playNo;
            this.numstr = numstr;
        }
    }

    private static class CachedRealPayTask {
        private final String playNo;
        private final String dparams;
        private final int betCount;

        private CachedRealPayTask(String playNo, String dparams, int betCount) {
            this.playNo = playNo;
            this.dparams = dparams;
            this.betCount = betCount;
        }
    }

    private static class OrderParams {
        private final String params;
        private final int betCount;

        private OrderParams(String params, int betCount) {
            this.params = params;
            this.betCount = betCount;
        }
    }

    @PostConstruct
    public void initParallelExecutor() {
        int threads = Math.max(1, parallelThreads);
        parallelExecutor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "caipiao-parallel");
            t.setDaemon(true);
            return t;
        });
        log.info("[并行] 线程池已初始化 enabled={} threads={}", parallelEnabled, threads);
    }

    @PreDestroy
    public void shutdownParallelExecutor() {
        if (parallelExecutor != null) {
            parallelExecutor.shutdown();
        }
    }

    private static class PayBatchContext {
        private String planNo;
        private double initmoney;
        private Map<String, String> countMapold;
        private String existingPayRecord;
        private final List<String> payLines = new ArrayList<>();
        private final StringBuilder fileContent = new StringBuilder();
        private boolean dirty;
    }

    private static class WinBatchContext {
        private String planNo;
        private double initmoney;
        private Map<String, String> countMap;
        private String existingResultIsZhong;
        private final List<String> winLines = new ArrayList<>();
        private final StringBuilder fileContent = new StringBuilder();
        private final Map<String, String> redisBatch = new HashMap<>();
        private boolean dirty;
    }

    /**
     * 毫秒转秒，用于日志展示
     */
    private static String formatCostSeconds(long costMs) {
        return String.format("%.2f", costMs / 1000.0) + "s";
    }

    /**
     * 执行 HTTP 请求并统计耗时
     */
    private HttpResponse executeWithTiming(HttpRequest request, String apiName) {
        long start = System.currentTimeMillis();
        try {
            HttpResponse resp = request.execute();
            long cost = System.currentTimeMillis() - start;
            log.info("[HTTP耗时] {} 耗时:{} status:{}", apiName, formatCostSeconds(cost), resp.getStatus());
            return resp;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("[HTTP耗时] {} 耗时:{} 失败: {}", apiName, formatCostSeconds(cost), e.getMessage());
            throw e;
        }
    }

    /**
     *
     * @param ticketId 哪个类型游戏
     */
    public String loadTimeByCronByjava(int ticketId) throws Exception {
        String cronLockKey = CRON_JOB_LOCK_PREFIX + ticketId;
        if (!redisService.setIfAbsent(cronLockKey, "1", 180)) {
            log.warn("[定时任务] ticketId={} 上一轮任务未完成，跳过", ticketId);
            return "";
        }
        long roundStart = System.currentTimeMillis();
        long phaseStart = roundStart;
        redisService.beginSession();
        try {
        log.info("loadTimeByCronByjava -- 进入的秒数"+DateUtil.getNowSecond());
        redisService.set("isTouzhu","true");
        String flagstr="";
        String token = redisService.get("tokenlogin");
        if(MyStringUtils.valueIsEmpty(token)) {
            log.error("未设置采集数据的token");
            return "未设置采集数据的token";
        }
        //开关打开
        isSwtich();
        //获取请求头
        Map<String, String> header=getHeader(token);
        //获取系统秒数
        int nowscord=DateUtil.getNowSecond();
        log.info("loadTimeByCronByjava -- 开始进入的判断"+nowscord);
        if(nowscord>=1 && nowscord<40){
        log.info("loadTimeByCronByjava -- 进入的判断"+DateUtil.getNowSecond());
        //获取当期与上一期的期数
        TimeList.DataItem  dateitme=getPlanNow(ticketId,header);
        if(dateitme!=null){
            //当局序号，上局序号
            String curentPlanNo=dateitme.getPlanId(),
            beforePlanNo=dateitme.getBeforePlanNo();
            //判断当前数据是否下单
                String beforeplannostr=queryNumberByPlanNo(ticketId,beforePlanNo,header,2);
                if(MyStringUtils.valueIsEmpty(beforeplannostr)){
                    for (int i=0;i<1000;i++){
                        beforeplannostr=queryNumberByPlanNo(ticketId,beforePlanNo,header,2);
                        if(MyStringUtils.valueIsNotEmpty(beforeplannostr)){
                            break;
                        }
                    }
                }
                if(MyStringUtils.valueIsEmpty(beforeplannostr)){
                    truancatnum();
                }else{
                String planLockKey = PLAN_ROUND_LOCK_PREFIX + curentPlanNo;
                if (!redisService.setIfAbsent(planLockKey, "1", 180)) {
                    log.warn("[定时任务] 期数:{} 已有实例在处理，跳过重复执行", curentPlanNo);
                } else {
                try {
                countNumWincount(curentPlanNo,beforeplannostr,beforePlanNo);
                log.info("[阶段耗时] 中奖结算 耗时:{}", formatCostSeconds(System.currentTimeMillis() - phaseStart));
                phaseStart = System.currentTimeMillis();
                //1,进行预测，拿到对应的预测开奖号
                HashMap<String,String> parammap=predictionbyjava(ticketId,header,curentPlanNo);
                log.info("[阶段耗时] 预测拉数+算分 耗时:{}", formatCostSeconds(System.currentTimeMillis() - phaseStart));
                phaseStart = System.currentTimeMillis();
                redisService.del("planNumberMap");
                log.info("map是否为空"+parammap.size());
               if(!parammap.isEmpty()){
                   HashMap<String,String> rawParammap=new HashMap<>(parammap);
                   printGiveNumber(curentPlanNo,rawParammap);//打印原始出数
                   String fanwei=redisService.get("countNum");
                   String suanfatype=redisService.get("suanfatype");
                   String beilv=redisService.get("duozu");
                   double beilvValue=1;
                   if(MyStringUtils.valueIsNotEmpty(beilv)){
                       beilvValue=Double.parseDouble(beilv);
                   }
                   Map<String,String> redisBatch=new HashMap<>();
                   for (Map.Entry<String, String> entry : parammap.entrySet()) {
                       String  luindex=entry.getKey();
                       String luvalue2=entry.getValue();
                       String luvalue=MyStringUtils.filterAndDeduplicateStrings(luvalue2,fanwei);
                       parammap.put(luindex, luvalue);
                       if(MyStringUtils.valueIsNotEmpty(luvalue)){
                           String[] lunumber=luvalue.split(",");
                           for (int i=0;i< lunumber.length;i++){
                               String[] parts=MyStringUtils.parseBetItem(lunumber[i]);
                               if(parts==null || parts.length<3){
                                   continue;
                               }
                               String  numberObject=parts[0];
                               String  numberObjectScore=parts[1];
                               String  numberzu=parts[2];
                               String key=luindex+"-"+numberObject;
                               List<String> stateValues=redisService.mget(key+"-notPayCount", key+"-payCount");
                               String luNotpaycount=stateValues.get(0);
                               if(MyStringUtils.valueIsEmpty(luNotpaycount)){
                                   luNotpaycount="1";
                               }
                               String luPayCount=stateValues.get(1);
                               if(MyStringUtils.valueIsEmpty(luPayCount)){
                                   luPayCount="1";
                               }

                               boolean isopenpayv3=isOpenPayv3(luindex,numberObject,numberObjectScore,0,numberzu,fanwei,redisBatch);
                               if(isopenpayv3){
                                   int[] suanfa=PlanType.suanfa6(luNotpaycount,luPayCount);
                                   if("1".equals(suanfatype)){//阿基米德
                                       suanfa=PlanType.suanfa1plus(luNotpaycount,luPayCount);
                                   }else if("2".equals(suanfatype)){//高斯
                                       suanfa=PlanType.suanfa2plus(luNotpaycount,luPayCount);
                                   }else if("3".equals(suanfatype)){//祖冲之
                                       suanfa=PlanType.suanfa3plus(luNotpaycount,luPayCount);
                                   }else if("4".equals(suanfatype)){//华罗庚
                                       suanfa=PlanType.suanfa4plus(luNotpaycount,luPayCount);
                                   }
                                   int luPayMoneyInt=suanfa[0];
                                   int luNotpaycountInt=suanfa[1];
                                   int luPayCountInt=suanfa[2];

                                   double doublePayMoney=beilvValue* luPayMoneyInt;

                                   redisBatch.put(key+"-notPayCount",luNotpaycountInt+"");
                                   redisBatch.put(key+"-payCount",luPayCountInt+"");
                                   redisBatch.put(key+"-payMoney",doublePayMoney+"");
                               }
                           }
                       }
                   }
                   if(!redisBatch.isEmpty()){
                       redisService.mset(redisBatch);
                   }
//                   log.info("11111打印原始出数");

                   //2,设置多路的次数与金额
//                   countNumCountAndMoney(parammap);

                   //3,根据出的号进行投注
                   redisService.hmset("planNumberMap",parammap);
                   redisService.set("planNumberMapPlanNo", curentPlanNo);
                   log.info("[阶段耗时] 过滤号码+算金额 耗时:{}", formatCostSeconds(System.currentTimeMillis() - phaseStart));
                   phaseStart = System.currentTimeMillis();
                   createOrderConditionbyjavaMuti(ticketId,beforePlanNo,curentPlanNo,header,parammap);//下单
                   log.info("[阶段耗时] 虚拟投注 耗时:{}", formatCostSeconds(System.currentTimeMillis() - phaseStart));
                   phaseStart = System.currentTimeMillis();
                   queryBalanceOnly(header);
                   log.info("[阶段耗时] 查余额 耗时:{}", formatCostSeconds(System.currentTimeMillis() - phaseStart));
               }else{
                   log.info("参数map为空");
               }
                } finally {
                    redisService.del(planLockKey);
                }
                }
                }
            }else{
                System.out.println("对象是空...");
            }
        }else{
            System.out.println("超时了，无法下单...");
            return "";
        }
        return flagstr;
        } finally {
            redisService.endSession();
            redisService.del(cronLockKey);
            log.info("[整局耗时] ticketId={} 总耗时:{}", ticketId, formatCostSeconds(System.currentTimeMillis() - roundStart));
        }
    }


    /***
     * 是否打开单路投注
     */
    private boolean isOpenPayv3(String playNo, String number, String playNumScore, int countWinCount, String numberzu) {
        String fanwei = redisService.get("countNum");
        return isOpenPayv3(playNo, number, playNumScore, countWinCount, numberzu, fanwei, null);
    }

    private boolean isOpenPayv3(String playNo, String number, String playNumScore, int countWinCount, String numberzu, String fanwei) {
        return isOpenPayv3(playNo, number, playNumScore, countWinCount, numberzu, fanwei, null);
    }

    private boolean isOpenPayv3(String playNo, String number, String playNumScore, int countWinCount, String numberzu, String fanwei, Map<String, String> touzhuBatch) {
        if (MyStringUtils.valueIsEmpty(fanwei)) {
            fanwei = "0";
        }
        boolean flag = isNumberObject(fanwei, playNumScore, numberzu);
        String touzhuKey = "touzhu" + playNo + "-" + number;
        String touzhuValue = flag ? "true" : "false";
        if (touzhuBatch != null) {
            touzhuBatch.put(touzhuKey, touzhuValue);
        } else {
            redisService.set(touzhuKey, touzhuValue);
        }
        return flag;
    }




    private static boolean isValidScoreKey(String key) {
        if (MyStringUtils.valueIsEmpty(key)) {
            return false;
        }
        try {
            Double.parseDouble(key.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isPositiveBetCount(String count) {
        if (MyStringUtils.valueIsEmpty(count)) {
            return false;
        }
        try {
            return Integer.parseInt(count.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int compareScoreKeys(String key1, String key2) {
        return Double.compare(Double.parseDouble(key1.trim()), Double.parseDouble(key2.trim()));
    }

    private static boolean isNumberObject(String fanwei2, String playNumScore, String numberzu) {
        boolean flag = false;
        if (!isValidScoreKey(playNumScore) || fanwei2.indexOf(";") <= -1) {
            return false;
        }
        String[] fanweistr = fanwei2.split(";");
        String group = fanweistr[Integer.parseInt(numberzu)];
        if (group.indexOf(",") > -1) {
            String[] fanweiarray = group.split(",");
            double targetScore = Double.parseDouble(playNumScore.trim());
            for (int i = 0; i < fanweiarray.length; i++) {
                if (isValidScoreKey(fanweiarray[i]) && Double.parseDouble(fanweiarray[i].trim()) == targetScore) {
                    flag = true;
                    break;
                }
            }
        } else if (isValidScoreKey(group)) {
            flag = Double.parseDouble(group.trim()) == Double.parseDouble(playNumScore.trim());
        }
        return flag;
    }


    /***
     * 是否打开单路投注
     */
    private boolean isOpenPayv2(String playNo,String number,String playNumScore,int countWinCount){
        boolean flag=false;
        String fanwei=redisService.get("countNum");
        if(MyStringUtils.valueIsEmpty(fanwei)){
            fanwei="0";
        }

        String cishu=redisService.get("countZu");
        if(MyStringUtils.valueIsEmpty(cishu)){
            cishu="1";
        }
//        log.info(fanwei+"得到的fanming"+playNumScore);
        if(fanwei.indexOf(playNumScore)>-1 && countWinCount<=3){
            redisService.set("touzhu"+playNo+"-"+number, "true");
            flag=true;
        }else{
            redisService.set("touzhu"+playNo+"-"+number, "false");
            flag=false;
        }
        return  flag;
    }


    /***
     * 是否打开单路投注
     */
    private boolean isOpenPay(String playNo,String number,String playNumScore,int countNotWinCount){
        boolean flag=false;
        String fenshu=redisService.get("countNum");
        if(MyStringUtils.valueIsEmpty(fenshu)){
            fenshu="25";
        }

        String cishu=redisService.get("countZu");
        if(MyStringUtils.valueIsEmpty(cishu)){
            fenshu="1";
        }
        if(Double.parseDouble(playNumScore)==Double.parseDouble(fenshu) && countNotWinCount>=Integer.parseInt(cishu) ){
            redisService.set("touzhu"+playNo+"-"+number, "true");
            flag=true;
        }else{
            redisService.set("touzhu"+playNo+"-"+number, "false");
            flag=false;
        }
        return  flag;
    }



    public static String getValueByKey(String input, String targetKey) {
        String strflag="";
        String[] pairs = input.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length >= 2 && keyValue[0].trim().equals(targetKey)) {
                strflag= keyValue[1].trim();
            }
        }
        return strflag;
    }

    private static Map<String, String> parseNumberScoreMap(String mapvalue) {
        Map<String, String> scoreMap = new HashMap<>();
        if (MyStringUtils.valueIsEmpty(mapvalue)) {
            return scoreMap;
        }
        String[] items = mapvalue.split(",");
        for (String item : items) {
            String[] parts = MyStringUtils.parseBetItem(item);
            if (parts != null && MyStringUtils.valueIsNotEmpty(parts[0])) {
                scoreMap.put(parts[0], parts[1]);
            }
        }
        return scoreMap;
    }



    /**
     * 次数累加赢钱
     */
    public void countNumWincount(String nowplanno,String beforePlayNum,String beforno){
        AtomicInteger wincountnum=new AtomicInteger(0);
        log.debug("countNumWincount 上期开奖:{}", beforePlayNum);
        String[] beforenumarry=beforePlayNum.split(" ");
        String chooseIndex=redisService.get("lushu");
        String[] chooseIndexValue=MyStringUtils.stringNotNull(chooseIndex,"1").split(",");
        Map<String,String> planMapkey=redisService.hget("planNumberMap");
        if (planMapkey == null) {
            planMapkey = new HashMap<>();
        }
        final Map<String,String> planMapFinal=planMapkey;
        String plaggamewin=redisService.get("playGame"+beforno);
        beginWinBatch(beforno);
        WinBatchContext winCtx=winBatchContext.get();
        try {
            if(!planMapkey.isEmpty() && MyStringUtils.valueIsNotEmpty(beforePlayNum)){
                List<String> redisKeys=new ArrayList<>();
                List<int[]> roadMeta=new ArrayList<>();
                for(int i=0;i< chooseIndexValue.length;i++){
                    if(MyStringUtils.valueIsEmpty(chooseIndexValue[i])){
                        continue;
                    }
                    int numbstr=Integer.parseInt(chooseIndexValue[i])-1;
                    if(numbstr<0 || numbstr>=beforenumarry.length){
                        continue;
                    }
                    String playNo=chooseIndexValue[i];
                    String mapvalue=planMapFinal.get(playNo);
                    if(MyStringUtils.valueIsEmpty(mapvalue)){
                        continue;
                    }
                    String oldnum=beforenumarry[numbstr];
                    if(mapvalue.indexOf(oldnum+":")<=-1){
                        continue;
                    }
                    String key=playNo+"-"+oldnum;
                    roadMeta.add(new int[]{i, redisKeys.size()});
                    redisKeys.add("touzhu"+playNo+"-"+oldnum);
                    redisKeys.add(key+"-payMoney");
                    redisKeys.add(key+"-notPayCount");
                    redisKeys.add(key+"-payCount");
                    redisKeys.add("payNumDetail"+playNo+"-"+oldnum);
                }
                List<String> stateValues=redisKeys.isEmpty()
                        ? Collections.emptyList()
                        : redisService.mget(redisKeys.toArray(new String[0]));
                if(parallelEnabled && roadMeta.size()>1){
                    List<Future<?>> futures=new ArrayList<>();
                    for(int r=0;r<roadMeta.size();r++){
                        final int roadIndex=roadMeta.get(r)[0];
                        final int base=roadMeta.get(r)[1];
                        futures.add(parallelExecutor.submit(() -> {
                            winBatchContext.set(winCtx);
                            try {
                                String playNo=chooseIndexValue[roadIndex];
                                int numbstr=Integer.parseInt(playNo)-1;
                                String oldnum=beforenumarry[numbstr];
                                String mapvalue=planMapFinal.get(playNo);
                                String touzhuFlag=stateValues.get(base);
                                if(!"true".equals(touzhuFlag)){
                                    return;
                                }
                                String luPayMoney=stateValues.get(base+1);
                                String luNotPayCount=stateValues.get(base+2);
                                String luPayCount=stateValues.get(base+3);
                                String payNumDetail=stateValues.get(base+4);
                                String number_score=parseNumberScoreMap(mapvalue).get(oldnum);
                                if(number_score==null){
                                    number_score=getValueByKey(mapvalue, oldnum);
                                }
                                processWinRoad(luPayMoney,beforno,playNo,luNotPayCount,luPayCount,oldnum,number_score,
                                        plaggamewin,payNumDetail,winCtx,wincountnum);
                            } finally {
                                winBatchContext.remove();
                            }
                        }));
                    }
                    awaitFutures(futures);
                }else{
                    for(int r=0;r<roadMeta.size();r++){
                        int roadIndex=roadMeta.get(r)[0];
                        int base=roadMeta.get(r)[1];
                        String playNo=chooseIndexValue[roadIndex];
                        int numbstr=Integer.parseInt(playNo)-1;
                        String oldnum=beforenumarry[numbstr];
                        String mapvalue=planMapFinal.get(playNo);
                        String touzhuFlag=stateValues.get(base);
                        if(!"true".equals(touzhuFlag)){
                            continue;
                        }
                        String luPayMoney=stateValues.get(base+1);
                        String luNotPayCount=stateValues.get(base+2);
                        String luPayCount=stateValues.get(base+3);
                        String payNumDetail=stateValues.get(base+4);
                        String number_score=parseNumberScoreMap(mapvalue).get(oldnum);
                        if(number_score==null){
                            number_score=getValueByKey(mapvalue, oldnum);
                        }
                        processWinRoad(luPayMoney,beforno,playNo,luNotPayCount,luPayCount,oldnum,number_score,
                                plaggamewin,payNumDetail,winCtx,wincountnum);
                    }
                }
            }
        } finally {
            endWinBatch(beforno);
        }
        log.debug("countNumWincount 期数:{} 上期:{} 中奖:{}", nowplanno, beforno, wincountnum.get());
        redisService.set("wincountnum"+beforno, wincountnum.get()+"");
    }

    private void processWinRoad(String luPayMoney,String beforno,String playNo,String luNotPayCount,String luPayCount,
                                String oldnum,String number_score,String plaggamewin,String payNumDetail,
                                WinBatchContext winCtx,AtomicInteger wincountnum){
        if (!isPositiveBetCount(luPayCount)) {
            log.debug("[中奖结算] 期数:{} 路数:{} 号码:{} 投注次数为{}，跳过中奖记录", beforno, playNo, oldnum, luPayCount);
            return;
        }
        if(!"false".equals(plaggamewin)){
            agentWinMoney(luPayMoney,beforno,playNo,luNotPayCount,luPayCount,oldnum,number_score);
        }
        wincountnum.incrementAndGet();
        markNumberIsZhongInBatch(playNo,oldnum,payNumDetail);
        if(winCtx!=null){
            synchronized (winCtx){
                winCtx.redisBatch.put("countZuStr"+playNo+"-"+oldnum,"0");
            }
        }
    }

    private void markNumberIsZhongInBatch(String playNo,String playNumNo,String currentPayNumDetail){
        WinBatchContext ctx=winBatchContext.get();
        if(ctx==null){
            setNumberIsZhong(playNo, playNumNo, null);
            return;
        }
        synchronized (ctx){
            String payNumDetailKey="payNumDetail"+playNo+"-"+playNumNo;
            if(ctx.redisBatch.containsKey(payNumDetailKey)){
                return;
            }
            if(MyStringUtils.valueIsNotEmpty(currentPayNumDetail) && !"true".equals(currentPayNumDetail)){
                ctx.redisBatch.put(payNumDetailKey,"true");
            }
        }
    }

    private void beginWinBatch(String planNo) {
        WinBatchContext ctx = new WinBatchContext();
        ctx.planNo = planNo;
        String initmoney = redisService.get("initmoney");
        if (MyStringUtils.valueIsEmpty(initmoney)) {
            initmoney = "0";
        }
        ctx.initmoney = Double.parseDouble(initmoney);
        ctx.countMap = redisService.hget("countMap");
        if (ctx.countMap == null) {
            ctx.countMap = new HashMap<>();
        }
        ctx.existingResultIsZhong = redisService.get("resultiszhong");
        winBatchContext.set(ctx);
    }

    private void endWinBatch(String planNo) {
        WinBatchContext ctx = winBatchContext.get();
        winBatchContext.remove();
        if (ctx == null || !ctx.dirty) {
            if (ctx != null && !ctx.redisBatch.isEmpty()) {
                redisService.mset(ctx.redisBatch);
            }
            return;
        }
        redisService.set("initmoney", MyStringUtils.string3double(ctx.initmoney + ""));
        redisService.hmset("countMap", new HashMap<>(ctx.countMap));
        saveCountMapToRedis(planNo);
        costSwtichByBanlance(ctx.initmoney + "");

        StringBuilder winRecordBuilder = new StringBuilder();
        for (int i = 0; i < ctx.winLines.size(); i++) {
            if (i > 0) {
                winRecordBuilder.append("<br/>");
            }
            winRecordBuilder.append(ctx.winLines.get(i));
        }
        String resultiszhong = winRecordBuilder.toString();
        if (MyStringUtils.valueIsNotEmpty(ctx.existingResultIsZhong)) {
            resultiszhong = resultiszhong + "<br/>" + ctx.existingResultIsZhong;
        }
        resultiszhong = trimWinRecord(resultiszhong);
        if (MyStringUtils.valueIsNotEmpty(resultiszhong)) {
            redisService.set("resultiszhong", resultiszhong);
        }

        if (!ctx.redisBatch.isEmpty()) {
            redisService.mset(ctx.redisBatch);
        }

        if (ctx.fileContent.length() > 0) {
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String formattedDate = dateFormat.format(currentDate);
            String filePath = mybean.getFilepath() + "winmoney" + formattedDate + ".txt";
            AgentMoney.writefilepath(filePath, ctx.fileContent.toString());
        }
        log.info("[批量中奖] 期数:{} 笔数:{}", planNo, ctx.winLines.size());
    }

    private String trimWinRecord(String winRecord) {
        String[] arr = winRecord.split("<br/>");
        if (arr.length <= 100) {
            return winRecord;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            if (i > 0) {
                sb.append("<br/>");
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    /**
     * 设置方案走向
     */
    private void setNumberIsZhong(String playNo,String playNumNo){
        setNumberIsZhong(playNo, playNumNo, null);
    }

    private void setNumberIsZhong(String playNo,String playNumNo,Map<String,String> winBatch){
        String payNumDetailKey="payNumDetail"+playNo+"-"+playNumNo;
        if(winBatch!=null){
            if(winBatch.containsKey(payNumDetailKey)){
                return;
            }
            String payNumDetail=redisService.get(payNumDetailKey);
            if(MyStringUtils.valueIsNotEmpty(payNumDetail) && !"true".equals(payNumDetail)){
                winBatch.put(payNumDetailKey,"true");
            }
        }else{
            String payNumDetail=redisService.get(payNumDetailKey);
            if(MyStringUtils.valueIsNotEmpty(payNumDetail) && !"true".equals(payNumDetail)){
                redisService.set(payNumDetailKey,"true");
            }
        }
    }




    /**
     * 开关设置
     */
    private void isSwtich(){
        costSwtich();
    }

    /**
     * 预算开关
     */
    private void costSwtich(){
        String isColseByMoney=redisService.get("isColseByMoney");
        String banlance=redisService.get("banlance");//余额

        if(MyStringUtils.valueIsEmpty(banlance)){
            banlance="0";
        }

        String mincost=redisService.get("mincost");
        String maxcost=redisService.get("maxcost");
        if(MyStringUtils.valueIsNotEmpty(isColseByMoney)){ //根据预算做关闭
            log.info("最小值："+mincost+"   本金："+banlance+"  "+"  最大值："+maxcost);
            if("true".equals(isColseByMoney)){
                if(MyStringUtils.valueIsNotEmpty(mincost)){
                    if(Double.parseDouble(banlance)<=Double.parseDouble(mincost)){
                        log.info("最小值："+mincost+"   本金："+banlance+"  "+"  最大值："+maxcost+" 11111");
                        redisService.set("isopen", "false");
                        redisService.set("isTouzhu", "false");
                    }
                }

                if(MyStringUtils.valueIsNotEmpty(maxcost)){
                    if(Double.parseDouble(banlance)>=Double.parseDouble(maxcost)){
                        log.info("最小值："+mincost+"   本金："+banlance+"  "+"  最大值："+maxcost+" 22222");
                        redisService.set("isopen", "false");
                        redisService.set("isTouzhu", "false");
                    }
                }
            }
        }
    }



    /**
     * 预算开关
     */
    private void costSwtichByBanlance(String banlance){
        String isColseByMoney=redisService.get("isColseByMoney");

        if(MyStringUtils.valueIsEmpty(banlance)){
            banlance="0";
        }

        String mincost=redisService.get("mincost");
        String maxcost=redisService.get("maxcost");
        if(MyStringUtils.valueIsNotEmpty(isColseByMoney)){ //根据预算做关闭
            // log.info("最小值："+mincost+"   本金："+banlance+"  "+"  最大值："+maxcost);
            if("true".equals(isColseByMoney)){
                if(MyStringUtils.valueIsNotEmpty(mincost)){
                    if(Double.parseDouble(banlance)<=Double.parseDouble(mincost)){
                        // log.info("最小值："+mincost+"   本金："+banlance+"  "+"  最大值："+maxcost+" 11111");
                        redisService.set("isopen", "false");
                        redisService.set("isTouzhu", "false");
                    }
                }

                if(MyStringUtils.valueIsNotEmpty(maxcost)){
                    if(Double.parseDouble(banlance)>=Double.parseDouble(maxcost)){
                        // log.info("最小值："+mincost+"   本金："+banlance+"  "+"  最大值："+maxcost+" 22222");
                        redisService.set("isopen", "false");
                        redisService.set("isTouzhu", "false");
                    }
                }
            }
        }
    }


    public List<PiaoCoder> listTopN(int type, int n) {
        if (coderMapper == null) {
            return Collections.emptyList();
        }
	    LambdaQueryWrapper<PiaoCoder> query = Wrappers.lambdaQuery();
	    query.eq(PiaoCoder::getType, type);
	    query.orderByDesc(PiaoCoder::getIssue);
	    query.last("limit " + n);
		return coderMapper.selectList(query);
    }


    /**
     * 获取当期的轮数和上一期的轮数
     */
    public TimeList.DataItem queryPlanNow(int ticketId) {
        String token = redisService.get("tokenlogin");
        return getPlanNow(ticketId, getHeader(token));
    }

    public TimeList.DataItem getPlanNow(int ticketId,Map<String, String> header){
        HttpRequest request = HttpUtil.createGet(apiurl+"/coron/ticketmod/currentSaleIssue/list?ticketIds="+ticketId)
                .addHeaders(header);
        request.setConnectionTimeout(20000);
        request.setReadTimeout(20000);
        HttpResponse resp = executeWithTiming(request, "getPlanNow-currentSaleIssue ticketId=" + ticketId);
        TimeList timelist = JSON.parseObject(resp.body(), TimeList.class);
        if(timelist.isTokenError()) {
            TOKEN_HAVE_ERROR = true;
            return null;
        }
        TimeList.DataItem dateitme=timelist.getData().get(0);
        return dateitme;
    }

    /**
     * 获取当前100期并预测一组数据
     */
    public HashMap<String,String> predictionbyjava(int ticketId,Map<String,String> header,String nowPlanNo) throws IOException {
        String qishu=redisService.get("qishu");
        if(StringUtils.isEmpty(qishu)){
            qishu="49";
        }
        String suanfatype=redisService.get("suanfatype");
        String onceNum=redisService.get("onceNum");
        HashMap<String,String> querycountnum=new HashMap<>();
        String[] qishuarray=qishu.split(";");

        List<String> validQishuList=new ArrayList<>();
        int maxNum=0;
        for(String qishustr:qishuarray){
            if(StringUtils.isEmpty(qishustr)){
                continue;
            }
            String trimmed=qishustr.trim();
            validQishuList.add(trimmed);
            int num=Integer.parseInt(trimmed);
            if(num>maxNum){
                maxNum=num;
            }
        }
        if(maxNum<=0 || validQishuList.isEmpty()){
            return querycountnum;
        }

        List<String> allCodeLines=fetchTicketSourceResultCodes(ticketId,header,maxNum);
        int[][] fullMatrix=buildMatrixFromCodeLines(allCodeLines, maxNum);
        for(int j=0;j<validQishuList.size();j++){
            String qishustr=validQishuList.get(j);
            int requestedNum=Integer.parseInt(qishustr);
            if(requestedNum<maxNum){
                log.debug("[预测数据] num={} 复用已获取的 num={} 数据，截取前{}期", requestedNum, maxNum, Math.min(requestedNum, allCodeLines.size()));
            }
            int rowLimit=Math.min(requestedNum, fullMatrix.length);
            if(rowLimit<=0){
                continue;
            }
            int[][] frequency=buildFrequencyMatrix(fullMatrix, rowLimit);
            HashMap<String,String> loopResult=queryCountNumFromFrequency(frequency,qishustr,j+"",suanfatype,onceNum);
            for (Map.Entry<String, String> entry : loopResult.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if(StringUtils.isEmpty(value)){
                    continue;
                }
                if(querycountnum.containsKey(key) && StringUtils.isNotEmpty(querycountnum.get(key))){
                    querycountnum.put(key, querycountnum.get(key) + "," + value);
                }else{
                    querycountnum.put(key, value);
                }
            }
        }
        return querycountnum;
    }

    /**
     * 拉取历史开奖号码，按期号从新到旧排列
     */
    private List<String> fetchTicketSourceResultCodes(int ticketId,Map<String,String> header,int num){
        String queryNumUrl=apiurl+"/coron/api/ticketSourceResult/ticketSourceResultList.json";
        String jsonParams = "{\"ticketId\":"+ticketId+",\"num\":"+num+"}";
        HttpRequest createnum=HttpUtil.createPost(queryNumUrl).addHeaders(header);
        createnum.body(jsonParams);
        String response2 = executeWithTiming(createnum, "predictionbyjava-ticketSourceResult num=" + num).body();
        ResultList resultList = JSON.parseObject(response2, ResultList.class);
        List<String> codeLines=new ArrayList<>();
        if(resultList.getData()!=null && resultList.getData().size()>0){
            for(ResultList.DataSourceItem dsitem:resultList.getData()){
                if(MyStringUtils.valueIsNotEmpty(dsitem.getCode())){
                    codeLines.add(dsitem.getCode());
                }
            }
        }
        return codeLines;
    }

    /**
     * 从已拉取的数据中截取前 N 期；若 N 小于最大拉取期数则复用缓存不再请求
     */
    private String buildCodeDataStr(List<String> allCodeLines,int requestedNum,int fetchedNum){
        if(allCodeLines==null || allCodeLines.isEmpty()){
            return "";
        }
        int limit=Math.min(requestedNum, allCodeLines.size());
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<limit;i++){
            sb.append(allCodeLines.get(i)).append("\n");
        }
        return sb.toString();
    }

    private int[][] buildMatrixFromCodeLines(List<String> codeLines, int rowLimit) {
        if (codeLines == null || codeLines.isEmpty()) {
            return new int[0][0];
        }
        int limit = Math.min(rowLimit, codeLines.size());
        String[] firstRow = codeLines.get(0).trim().split("\\s+");
        int cols = firstRow.length;
        int[][] matrix = new int[limit][cols];
        for (int row = 0; row < limit; row++) {
            String[] parts = codeLines.get(row).trim().split("\\s+");
            for (int col = 0; col < cols; col++) {
                matrix[row][col] = Integer.parseInt(parts[col]);
            }
        }
        return matrix;
    }

    private int[][] buildFrequencyMatrix(int[][] matrix, int rowLimit) {
        if (matrix.length == 0) {
            return new int[0][11];
        }
        int cols = matrix[0].length;
        int rows = Math.min(rowLimit, matrix.length);
        int[][] frequency = new int[cols][11];
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                frequency[col][matrix[row][col]]++;
            }
        }
        return frequency;
    }

    private HashMap<String, String> queryCountNumFromFrequency(int[][] frequency, String qishuStr, String jindex,
                                                               String suanfatype, String onceNum) {
        HashMap<String, String> mapparam = new HashMap<>();
        if (frequency.length == 0) {
            return mapparam;
        }
        int qishuInt = 49;
        try {
            qishuInt = Integer.parseInt(qishuStr.trim());
        } catch (Exception e) {
            qishuInt = 49;
        }
        boolean filterByScore = StringUtils.isNotEmpty(suanfatype) && StringUtils.isNotEmpty(onceNum);
        int startnum = 0;
        int endnum = 0;
        if (filterByScore) {
            String[] numse = onceNum.split(",");
            startnum = Integer.parseInt(numse[0]);
            endnum = Integer.parseInt(numse[1]);
        }
        for (int col = 0; col < frequency.length; col++) {
            StringBuilder numstrStr = new StringBuilder();
            int colstr = col + 1;
            for (int num = 1; num <= 10; num++) {
                int count = frequency[col][num];
                int score = qishuInt / (count + 1);
                if (filterByScore && score >= startnum && score <= endnum) {
                    numstrStr.append(num).append(":").append(score).append(":").append(jindex).append(",");
                }
            }
            if (numstrStr.length() > 0) {
                mapparam.put(colstr + "", numstrStr.substring(0, numstrStr.length() - 1));
            }
        }
        return mapparam;
    }



    /**
     * 查询历史2条数据
     * @return
     */
    public String  queryNumberByPlanNo(int ticketId,String beforePlanNo,Map<String, String> header,int num){
        String queryNumUrl=apiurl+"/coron/api/ticketSourceResult/ticketSourceResultList.json";
        String jsonParams = "{\"ticketId\":"+ticketId+",\"num\":"+num+"}";
        //下单数字
        HttpRequest createnum=HttpUtil.createPost(queryNumUrl).addHeaders(header);
        createnum.body(jsonParams);
        String response2 = executeWithTiming(createnum, "queryNumberByPlanNo planNo=" + beforePlanNo + " num=" + num).body();
        log.debug("queryNumberByPlanNo planNo={} 返回条数待解析", beforePlanNo);
        ResultList resultList = JSON.parseObject(response2, ResultList.class);
        String sourceNum="";
        if(resultList.getData().size()>0){
            List<ResultList.DataSourceItem> datalist=resultList.getData();
            Map<String,Object> pmaps=new HashMap<>();
            for(int i=0;i<datalist.size();i++){
                ResultList.DataSourceItem dsitem=datalist.get(i);
                pmaps.put(dsitem.getIssue(),dsitem.getCode());
            }
            sourceNum=pmaps.get(beforePlanNo)+"";//得到原有的值
        }
        return sourceNum;
    }



    public  HashMap<String,String> queryCountNum(String nowPlanNo,String datastr,String qishuStr,String jindex,String suanfatype,String onceNum){
        HashMap<String,String> mapparam=new HashMap<>();
        // 将字符串转换为二维数组
        int[][] matrix = convertStringTo2DArray(datastr);
        // 获取矩阵的行数和列数
        int rows = matrix.length;
        int cols = matrix[0].length;
        // 用于存储每列每个数字的出现次数
        int[][] frequency = new int[cols][11]; // 假设数字范围是1到10
        // 遍历矩阵，统计每列每个数字的出现次数
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                int number = matrix[row][col];
                frequency[col][number]++;
            }
        }
        String qishu=qishuStr;
        if(StringUtils.isEmpty(qishu)){
            qishu="49";
        }
        qishu=qishu.trim();
        int qishuInt=49;
        try{
            qishuInt=Integer.parseInt(qishu);
        }catch (Exception e){
            qishuInt=49;
        }

        boolean filterByScore=StringUtils.isNotEmpty(suanfatype) && StringUtils.isNotEmpty(onceNum);
        int startnum=0;
        int endnum=0;
        if(filterByScore){
            String[] numse=onceNum.split(",");
            startnum=Integer.parseInt(numse[0]);
            endnum=Integer.parseInt(numse[1]);
        }

        double score =0.0;
        for (int col = 0; col < cols; col++) {
            String numstrStr="";
            int  colstr=col+1;
            for (int num = 1; num <= 10; num++) {
                int count = frequency[col][num];
                score = qishuInt / (count + 1);
                if(filterByScore){
                    if(score >=startnum && score<=endnum){
                        numstrStr=numstrStr+num+":"+score+":"+jindex+",";
                    }
                }
            }
            if(numstrStr.indexOf(",")>-1){
                mapparam.put(colstr+"", numstrStr.substring(0,numstrStr.length()-1));
            }
        }
        return mapparam;
    }





    /**
     * 投注打印 - 原始出号（每路一行，号码横向排列）
     */
    private void printGiveNumber(String nowPlanNo,Map<String,String> mapparam){
        StringBuilder scorestrsbstr=new StringBuilder();
        for (Map.Entry<String, String> entry : mapparam.entrySet()) {
            String colstr=entry.getKey();
            String numbervalue=entry.getValue();
            if(MyStringUtils.valueIsEmpty(numbervalue)){
                continue;
            }
            scorestrsbstr.append(MyStringUtils.spanRed(colstr)).append("  ");
            String[] numlist=numbervalue.split(",");
            boolean first=true;
            for (String item : numlist) {
                String[] parts=MyStringUtils.parseBetItem(item);
                if(parts==null){
                    continue;
                }
                if(!first){
                    scorestrsbstr.append("  ");
                }
                scorestrsbstr.append(MyStringUtils.spanRed(parts[0])).append(":")
                        .append(MyStringUtils.spanBlue(parts[1]));
                first=false;
            }
            scorestrsbstr.append("(期数:").append(nowPlanNo).append(")<br/>");
        }
        redisService.set("currentNum", scorestrsbstr.toString());
    }


    /**
     * 当期投注号：与投注记录同格式，仅保留本期
     */
    private void appendChooseNum(String line) {
        String chooseNum=redisService.get("chooseNum");
        if(MyStringUtils.valueIsEmpty(chooseNum)){
            chooseNum=line;
        }else{
            chooseNum=line+"<br/>"+chooseNum;
        }
        redisService.set("chooseNum", chooseNum);
        redisService.set("shownumchoose", chooseNum);
    }


    /**
     * 数值转化成二维数组
     * @param input
     * @return
     */
    public static int[][] convertStringTo2DArray(String input) {
        // 按换行符分割字符串，得到每一行
        String[] rows = input.split("\n");
        int rowCount = rows.length;

        // 检查是否有行，避免空数组
        if (rowCount == 0) {
            return new int[0][0];
        }

        // 按空格分割第一行，得到列数
        String[] firstRowElements = rows[0].split(" ");
        int colCount = firstRowElements.length;

        // 创建二维数组
        int[][] array = new int[rowCount][colCount];

        // 遍历每一行和每一列，将字符串转换为整数并存储到二维数组
        for (int i = 0; i < rowCount; i++) {
            String[] elements = rows[i].split(" ");
            for (int j = 0; j < colCount; j++) {
                array[i][j] = Integer.parseInt(elements[j]);
            }
        }

        return array;
    }

    /**
     * 获取请求头
     * @return
     */
    private Map<String, String>  getHeader(String token){
        Map<String, String> header = new HashMap<>();
        if(StringUtils.isNotEmpty(token)){
            header.put("token", token);
        }else{
            header.put("token", "49fde1f2f5194e2c9ff2e6bcdaabc0691732324152624");
        }
        header.put("User-Agent",
                "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        header.put("device","4");
        return header;
    }



    /**
     * 下单
     * @param ticketId 赛车67/飞艇48
     * @param currentPlanNo 当前的编号
     * @param header 信息头
     * @return
     */
    public void  createOrderConditionbyjavaMuti(int ticketId,String beforePlanNo,String currentPlanNo,Map<String, String> header,Map<String, String> parammap){
        String isTouzhu=redisService.get("isTouzhu");
        log.info("开始投注"+isTouzhu);
        if(StringUtils.isNotEmpty(isTouzhu) && "true".equals(isTouzhu)){
            if (!redisService.setIfAbsent(PAY_BATCH_DONE_PREFIX + currentPlanNo, "1", 600)) {
                log.warn("[批量投注] 期数:{} 本期已虚拟投注，跳过", currentPlanNo);
                return;
            }
            redisService.set("chooseNum", "");
            redisService.set("shownumchoose", "");
            beginPayBatch(currentPlanNo);
            try {
            String chooseIndex=redisService.get("lushu");
            String[] chooseIndexValue=MyStringUtils.stringNotNull(chooseIndex,"1").split(",");
            List<RoadTask> roadTasks=buildRoadTasks(chooseIndexValue, parammap);
            int totalPayCount=runParallelPreCreate(ticketId, beforePlanNo, currentPlanNo, roadTasks);
            PayBatchContext batch = payBatchContext.get();
            int betCount = batch != null ? batch.payLines.size() : totalPayCount;
            updatePayCountNum(currentPlanNo, betCount);

            this.polyIsOpen(beforePlanNo,currentPlanNo);
            String startpay=redisService.get("startpay");
            if("true".equals(startpay)){
                String istouzhuflag=redisService.get("playGame"+currentPlanNo);
                if("true".equals(istouzhuflag)){
                    runParallelCreateOrder(ticketId, beforePlanNo, currentPlanNo, header, roadTasks);
                }
            } else {
                log.debug("startpay=false，跳过真实下单拼参，仅保留虚拟投注");
            }
            } finally {
                endPayBatch(currentPlanNo,beforePlanNo);
            }
        }
    }


    /**
     * 下单
     * @param ticketId 赛车67/飞艇48
     * @param currentPlanNo 当前的编号
     * @param header 信息头
     * @return
     */
    private String  preCreateOrderjavaMuti(int ticketId,String beforePlanNo,String currentPlanNo,Map<String, String> header,String numberstype,String number,String indexstr){
        String dparams=this.preCreateOrderParamsChoosebyjavaMuti(ticketId,beforePlanNo,currentPlanNo,numberstype,number,indexstr);
        return dparams;
    }


    /**
     * 下单
     * @param ticketId 赛车67/飞艇48
     * @param currentPlanNo 当前的编号
     * @param header 信息头
     * @return
     */
    private String  createOrderjavaMuti(int ticketId,String beforePlanNo,String currentPlanNo,Map<String, String> header,String numberstype,String number,String indexstr){
        String orderurl=apiurl+"/coron/order/double/create";
        String dparams=this.createOrderParamsChoosebyjavaMuti(ticketId,beforePlanNo,currentPlanNo,numberstype,number,indexstr);
        log.debug("支付参数{}", dparams);
        String istouzhuflag=redisService.get("playGame"+currentPlanNo);
        log.debug("{} 支付是否需要 {}", istouzhuflag, currentPlanNo);
        if(dparams.indexOf("bet")>-1){
            if("true".equals(istouzhuflag)){
                    String starpay=redisService.get("startpay");
                    log.debug("{} 真实下单 {}", starpay, currentPlanNo);
                    if("true".equals(starpay) ){
                        log.debug("真实下单执行 planNo={}", currentPlanNo);
                    HttpRequest createnum=HttpUtil.createPost(orderurl).addHeaders(header);
                    createnum.body(dparams);
                    createnum.header("Content-Type", "application/x-www-form-urlencoded");
                    String response2 = executeWithTiming(createnum, "createOrder planNo=" + currentPlanNo).body();
                    System.out.println(response2+"  下单 ");
                    }
            }
        }else{
//            log.info(number+"没有下单参数");
            redisService.set("createcount","1");//次数置1
            redisService.set("createmoney","1");//投注额置1
        }
        return "";
    }



    /**
     * 多路拼接投注参数
     * @param ticketId 赛车67/飞艇48
     * @param planNo 当前的编号
     * @param numbertype 路数
     * @param number 投注号码
     * @return
     */
    public  String  preCreateOrderParamsChoosebyjavaMuti(int ticketId,String beforePlanNo,String planNo,String numbertype,String number,String playNo){
        isPayFlagForRoad(ticketId, beforePlanNo, planNo, numbertype, number, playNo);
        return "";
    }


    /**
     * 多路拼接投注参数
     * @param ticketId 赛车67/飞艇48
     * @param planNo 当前的编号
     * @param numbertype 路数
     * @param number 投注号码
     * @return
     */
    public String createOrderParamsChoosebyjavaMuti(int ticketId, String beforePlanNo, String planNo, String numbertype,
                                                    String number, String playNo) {
        return buildOrderParamsChoosebyjavaMuti(ticketId, beforePlanNo, planNo, numbertype, number, playNo).params;
    }

    private OrderParams buildOrderParamsChoosebyjavaMuti(int ticketId, String beforePlanNo, String planNo, String numbertype,
                                                         String number, String playNo) {
        StringBuffer sbstr=new StringBuffer();
        String tid=ticketId+"";
        sbstr.append("ticketId="+tid).append("&planNo="+planNo);
        String ntype=numbertype;
        String[] numberstrlist=number.split(",");
        List<String> numbers=new ArrayList<>();
        for (int i=0;i< numberstrlist.length;i++){
            String[] parts=MyStringUtils.parseBetItem(numberstrlist[i]);
            if(parts!=null && MyStringUtils.valueIsNotEmpty(parts[0])){
                numbers.add(parts[0]);
            }
        }
        List<String> stateValues=mgetBetStateForNumbers(playNo, numbers);
        int s=0;
        int paycountnum=0;
        for (int i=0;i< numbers.size();i++){
            String nowNumber=numbers.get(i);
            int base=i*4;
            String luPayMoney=stateValues.get(base);
            String payNumDetail=stateValues.get(base+1);
            String luNotPayCount=stateValues.get(base+2);
            String luPayCount=stateValues.get(base+3);
            if(MyStringUtils.valueIsNotEmpty(payNumDetail) && "true".equals(payNumDetail)
                    && MyStringUtils.valueIsNotEmpty(luPayMoney) && Double.parseDouble(luPayMoney)>0){
                paycountnum++;
                String numberobject="10".equals(nowNumber)?nowNumber:"0"+nowNumber;
                String playid=ticketId+"020"+ntype+"01"+numberobject;
                if("10".equals(ntype)){
                    playid=ticketId+"02"+ntype+"01"+numberobject;
                }
                int monestr = (int)Math.round(Double.parseDouble(luPayMoney));
                sbstr.append("&bet["+s+"].playId="+playid).append("&bet["+s+"].betNum="+nowNumber).append("&bet["+s+"].betAmount="+monestr).
                        append("&bet["+s+"].betCount=1").append("&bet["+s+"].content="+nowNumber);
                s++;
            }
        }
        log.debug(planNo+" 支付参数设置 本期笔数:"+paycountnum);
        sbstr.append("&orderSource=2");
        return new OrderParams(sbstr.toString(), paycountnum);
    }

    /**
     * 一路内所有号码的投注状态一次 mget（每个号码 4 个 key）
     */
    private List<String> mgetBetStateForNumbers(String playNo, List<String> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return Collections.emptyList();
        }
        String[] keys = new String[numbers.size() * 4];
        for (int i = 0; i < numbers.size(); i++) {
            String nowNumber = numbers.get(i);
            String key = playNo + "-" + nowNumber;
            int base = i * 4;
            keys[base] = key + "-payMoney";
            keys[base + 1] = "touzhu" + playNo + "-" + nowNumber;
            keys[base + 2] = key + "-notPayCount";
            keys[base + 3] = key + "-payCount";
        }
        return redisService.mget(keys);
    }

    /**
     * 统计本路需要虚拟投注的号码个数
     */
    private int isPayFlagForRoad(int ticketId,String beforePlanNo,String planNo,String numbertype,String number,String playNo){
        String[] numberstrlist=number.split(",");
        List<String> numbers=new ArrayList<>();
        List<String> scores=new ArrayList<>();
        int paycountnum=0;
        for (int i=0;i< numberstrlist.length;i++){
            String[] parts=MyStringUtils.parseBetItem(numberstrlist[i]);
            if(parts==null){
                continue;
            }
            String nowNumber=parts[0];
            String playNumScore=parts[1];
            if(MyStringUtils.valueIsNotEmpty(nowNumber)){
                numbers.add(nowNumber);
                scores.add(playNumScore);
            }
        }
        List<String> stateValues=mgetBetStateForNumbers(playNo, numbers);
        for (int i=0;i<numbers.size();i++){
            int base=i*4;
            String nowNumber=numbers.get(i);
            String playNumScore=scores.get(i);
            String luPayMoney=stateValues.get(base);
            String payNumDetail=stateValues.get(base+1);
            String luNotPayCount=stateValues.get(base+2);
            String luPayCount=stateValues.get(base+3);
            if(MyStringUtils.valueIsNotEmpty(payNumDetail) && "true".equals(payNumDetail)
                    && MyStringUtils.valueIsNotEmpty(luPayMoney) && Double.parseDouble(luPayMoney)>0){
                paycountnum++;
                agentPayMoney(planNo,playNo,luNotPayCount,luPayCount,luPayMoney,nowNumber,playNumScore);
            }
        }
        return paycountnum;
    }



    /**
     * 策略开关
     * @return
     */
    private boolean polyIsOpen(String beforenum,String currentnum){
        boolean flag=false;
        String before_paycountnum=redisService.get("paycountnum"+beforenum);
        String current_paycountnum=redisService.get("paycountnum"+currentnum);
        String before_wincountnum=redisService.get("wincountnum"+beforenum);

        log.info("上一期:"+beforenum+"     当期:"+currentnum+"      上期投注:"+before_paycountnum+"       当期投注:"+current_paycountnum+"   上期中奖:"+before_wincountnum+"   ");
        if(StringUtils.isNotEmpty(before_paycountnum) && StringUtils.isNotEmpty(current_paycountnum) && StringUtils.isNotEmpty(before_wincountnum)){
            int before_bets=Integer.parseInt(before_paycountnum);
            int current_bets=Integer.parseInt(current_paycountnum);
            int before_wins=Integer.parseInt(before_wincountnum);
            double ratio = before_wins > 0 ? (double) before_bets / before_wins : 0;

            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String formattedDate = dateFormat.format(currentDate);

            String filePath = mybean.getFilepath()+"countcompare"+formattedDate+".txt";
            if(current_bets>20){
                flag=false;
            }else{
                flag=true;
            }

            String content2="\n期数:   上一期:"+beforenum+"当期:"+currentnum+"  上期投注："+before_bets+"  上期中奖："+before_wins+"  上期中奖率"+ratio+"  本期投注："+current_bets+" 是否本期投注:"+flag;
            String beforenumstr=redisService.get("iswirtenum");
            if(MyStringUtils.valueIsEmpty(beforenumstr)  || !beforenumstr.equals(beforenum)){
                log.info("\n最后一个期数:   上一期:"+beforenum+"当期:"+currentnum+"  上期投注："+before_bets+"  上期中奖："+before_wins+"  上期中奖率"+ratio+"  本期投注："+current_bets+" 是否本期投注:"+flag);
                redisService.set("iswirtenum", beforenum);
                AgentMoney.writefilepath(filePath,content2);
            }
        } else {
            flag=true;
            log.info("策略开关: 首期或无历史统计，默认允许投注 当期:{}", currentnum);
        }
        if(flag){
            redisService.set("playGame"+currentnum, "true");
            log.info(beforenum+"  投注  策略开关 本期允许投注");
        }else{
            redisService.set("playGame"+currentnum, "false");
            log.info(beforenum+"  投注  策略开关 本期暂停投注");
        }
        return flag;
    }

    /**
     * 只查询余额
     * @return
     */
    public double  queryBalanceOnly(Map<String, String> header){
        double banlance=0;
        String queryNumUrl=apiurl+"/boracay/member/front/userInfo";
        //下单数字
        HttpRequest createnum=HttpUtil.createPost(queryNumUrl).addHeaders(header);
        String response2 = executeWithTiming(createnum, "queryBalanceOnly-userInfo").body();
        BanlanceResult resultList = JSON.parseObject(response2, BanlanceResult.class);
        if(resultList.getCode()!=-1) {
            banlance = Double.parseDouble(resultList.getData().get(0).getBalance());//账户余额
            redisService.set("banlance", banlance + "");
        }
        return banlance;
    }


    /**
     * 重置所有的
     */
    public void truancatnum(){
        for(int i=0;i<10;i++){
            int playNo=i+1;
            redisService.set("countNotWinCount"+playNo,"0");
            redisService.set("payMoney"+playNo,"0");
            redisService.set("oldwin"+playNo,"0");
            redisService.set("payNum"+playNo, "0");
            for(int j=0;j<10;j++) {
                int playNo2 = j + 1;
                redisService.set("payNumDetail"+playNo+"-"+playNo2,"false");
                redisService.set("touzhu"+playNo+"-"+playNo2,"true");
                redisService.set("countZuStr"+playNo+"-"+playNo2,"0");
                redisService.set(playNo+"-"+playNo2+"-notPayCount","0");
                redisService.set(playNo+"-"+playNo2+"-payCount","0");
                redisService.set(playNo+"-"+playNo2+"-payMoney","0");
            }
        }
    }



    /**
     * 模拟中了的金额
     * @param payMoneystr
     * @param
     */
    public   double  agentWinMoney(String payMoneystr,String planno,String playNo,String xianzhicishu,String cishu,String oldnum,String number_score){
        WinBatchContext batch = winBatchContext.get();
        if (batch != null) {
            synchronized (batch) {
                return recordAgentWinMoney(batch, payMoneystr, planno, playNo, xianzhicishu, cishu, oldnum, number_score);
            }
        }
        return recordAgentWinMoneyImmediate(payMoneystr, planno, playNo, xianzhicishu, cishu, oldnum, number_score);
    }

    private double recordAgentWinMoney(WinBatchContext batch, String payMoneystr, String planno, String playNo,
                                       String xianzhicishu, String cishu, String oldnum, String number_score) {
        String initmoneyBefore = MyStringUtils.string3double(batch.initmoney + "");
        double initmoneyDouble = Double.parseDouble(payMoneystr) * 9.925;
        batch.initmoney += initmoneyDouble;
        log.debug("赢了 期数:{} 路数:{} 号码:{} 奖金:{}", planno, playNo, oldnum, initmoneyDouble);
        String resultstr = MyStringUtils.formatWinDisplayHtml(planno, playNo, oldnum, number_score, xianzhicishu, cishu,
                initmoneyBefore, initmoneyDouble + "", batch.initmoney + "");
        batch.winLines.add(0, resultstr);
        batch.fileContent.append("\n赢了 期数:").append(planno)
                .append(" 路数:").append(playNo)
                .append(" 投注号码:").append(oldnum)
                .append("  投注分数:").append(number_score)
                .append("   限制次数:").append(xianzhicishu)
                .append("   投注次数:").append(cishu)
                .append(" 投注前金额:").append(initmoneyBefore)
                .append(" 挣到金额:").append(initmoneyDouble)
                .append(" 余额:").append(batch.initmoney);
        if (isValidScoreKey(number_score)) {
            String currentCount = batch.countMap.get(number_score);
            int newCount = (currentCount != null) ? Integer.parseInt(currentCount) + 1 : 1;
            batch.countMap.put(number_score, String.valueOf(newCount));
        } else {
            log.warn("中奖分数为空或非法，跳过countMap统计 期数:{} 路数:{} 号码:{} score:{}", planno, playNo, oldnum, number_score);
        }
        String key = playNo + "-" + oldnum;
        batch.redisBatch.put(key + "-notPayCount", "0");
        batch.redisBatch.put(key + "-payCount", "0");
        batch.redisBatch.put(key + "-payMoney", "0");
        batch.dirty = true;
        return batch.initmoney;
    }

    private double recordAgentWinMoneyImmediate(String payMoneystr, String planno, String playNo, String xianzhicishu,
                                                String cishu, String oldnum, String number_score) {
        log.info("进入赢的方法 agentWinMoney 期数:{} 路数:{} 号码:{}", planno, playNo, oldnum);
        String  initmoney=redisService.get("initmoney");//设置的数字
        if(MyStringUtils.valueIsEmpty(initmoney)){
            initmoney="0";
        }
        double initmoneytotal=Double.parseDouble(initmoney);
        double initmoney_double=Double.parseDouble(payMoneystr)*9.925;
            initmoneytotal=Double.parseDouble(initmoney)+initmoney_double;
            redisService.set("initmoney",MyStringUtils.string3double(initmoneytotal+""));
            log.debug("赢了 期数:{} 路数:{} 号码:{} 奖金:{}", planno, playNo, oldnum, initmoney_double);
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String formattedDate = dateFormat.format(currentDate);

            String filePath = mybean.getFilepath()+"winmoney"+formattedDate+".txt";
            String content = "\n赢了 期数:"+planno+" 路数:"+playNo+" 投注号码:"+oldnum+"  投注分数:"+number_score+"   限制次数:"+xianzhicishu+"   投注次数:"+cishu+" 投注前金额:"+initmoney+" 挣到金额:"+initmoney_double+" 余额:"+initmoneytotal;
            String resultstr=MyStringUtils.formatWinDisplayHtml(planno, playNo, oldnum, number_score, xianzhicishu, cishu,
                    initmoney, initmoney_double + "", initmoneytotal + "");

            Map<String, String> countMaphset = (Map<String, String>) redisService.hget("countMap");
            if (countMaphset == null) {
                countMaphset = new HashMap<>();
            }

            if (isValidScoreKey(number_score)) {
                String currentCount = countMaphset.get(number_score);
                int newCount = (currentCount != null) ? Integer.parseInt(currentCount) + 1 : 1;
                countMaphset.put(number_score, String.valueOf(newCount));
                redisService.hmset("countMap", (HashMap<String, String>) countMaphset);
                saveCountMapToRedis(planno);
            } else {
                log.warn("中奖分数为空或非法，跳过countMap统计 期数:{} 路数:{} 号码:{} score:{}", planno, playNo, oldnum, number_score);
            }

            costSwtichByBanlance(initmoneytotal+"");
            String resultiszhong=redisService.get("resultiszhong");
            if(MyStringUtils.valueIsNotEmpty(resultstr)){
                resultiszhong=resultstr+"<br/>"+resultiszhong;

                if(resultiszhong.split("<br/>").length>100){
                    int length=100;
                    String[] arr = resultiszhong.split("<br/>");
                    StringBuilder sb = new StringBuilder();
                    for(int i=0;i<length;i++){
                        sb.append(arr[i]).append("<br/>");
                    }
                    resultiszhong=sb.toString();
                }

                redisService.set("resultiszhong", resultiszhong);
            }

            AgentMoney.writefilepath(filePath,content);

            String key=playNo+"-"+oldnum;
            redisService.set(key+"-notPayCount","0");
            redisService.set(key+"-payCount","0");
            redisService.set(key+"-payMoney","0");

        return initmoneytotal;
    }



       // 将countMap转换为字符串并存储到Redis中
    public void saveCountMapToRedis(String planno) {

        String firstplanno=redisService.get("firstplanno");
        if(StringUtils.isEmpty(firstplanno)){
            redisService.set("firstplanno",planno);
        }

        redisService.set("otherplanno",planno);

        // 从Redis获取统计数据
        Map<String, String> countMaphset = (Map<String, String>) redisService.hget("countMap");
        if (countMaphset == null) {
            countMaphset = new HashMap<>();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(MyStringUtils.spanBlue(" "+firstplanno+"  ~  "+planno)).append("<br/>");
        // 使用Stream API按key升序排序（转换为Double进行排序）
        countMaphset.entrySet().stream()
                .filter(entry -> isValidScoreKey(entry.getKey()))
                .sorted((e1, e2) -> compareScoreKeys(e1.getKey(), e2.getKey()))
                .forEach(entry -> {
                    sb.append("&nbsp;&nbsp;").append(MyStringUtils.spanBlue(entry.getKey()))
                      .append(":").append(MyStringUtils.spanRed(entry.getValue()))
                      .append("<br/>");
                });

        log.debug(sb.toString()+"获取的字符串");
        // 存储到Redis中
        redisService.set("countMapData", sb.toString());
    }



    // 将countMap转换为字符串并存储到Redis中
    public void saveCountMapToRedisold(String planno) {

        String firstplanno=redisService.get("firstplanno");
        if(StringUtils.isEmpty(firstplanno)){
            redisService.set("firstplanno",planno);
        }

        redisService.set("otherplanno",planno);

        // 从Redis获取统计数据
        Map<String, String> countMaphset = (Map<String, String>) redisService.hget("countMapold");
        if (countMaphset == null) {
            countMaphset = new HashMap<>();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(MyStringUtils.spanBlue(" "+firstplanno+"  ~  "+planno)).append("<br/>");
        // 使用Stream API按key升序排序（转换为Double进行排序）
        countMaphset.entrySet().stream()
                .filter(entry -> isValidScoreKey(entry.getKey()))
                .sorted((e1, e2) -> compareScoreKeys(e1.getKey(), e2.getKey()))
                .forEach(entry -> {
                    sb.append("&nbsp;&nbsp;").append(MyStringUtils.spanBlue(entry.getKey()))
                            .append(":").append(MyStringUtils.spanRed(entry.getValue()))
                            .append("<br/>");
                });

        // log.info(sb.toString()+"获取的字符串");
        // 存储到Redis中
        redisService.set("countMapDataold", sb.toString());
    }



    private void beginPayBatch(String planNo) {
        PayBatchContext ctx = new PayBatchContext();
        ctx.planNo = planNo;
        String initmoney = redisService.get("initmoney");
        if (MyStringUtils.valueIsEmpty(initmoney)) {
            initmoney = "0";
        }
        ctx.initmoney = Double.parseDouble(initmoney);
        ctx.countMapold = redisService.hget("countMapold");
        if (ctx.countMapold == null) {
            ctx.countMapold = new HashMap<>();
        }
        ctx.existingPayRecord = redisService.get("payRecord");
        payBatchContext.set(ctx);
    }

    private void endPayBatch(String currentPlanNo,String beforenum) {
        PayBatchContext ctx = payBatchContext.get();
        payBatchContext.remove();
        if (ctx == null || !ctx.dirty) {
            return;
        }
        redisService.set("initmoney", MyStringUtils.string3double(ctx.initmoney + ""));
        redisService.hmset("countMapold", new HashMap<>(ctx.countMapold));
        saveCountMapToRedisold(ctx.planNo);
        costSwtichByBanlance(ctx.initmoney + "");

        StringBuilder payRecordBuilder = new StringBuilder();
        for (int i = 0; i < ctx.payLines.size(); i++) {
            if (i > 0) {
                payRecordBuilder.append("<br/>");
            }
            payRecordBuilder.append(ctx.payLines.get(i));
        }
        String payRecord = payRecordBuilder.toString();
        if (MyStringUtils.valueIsNotEmpty(ctx.existingPayRecord)) {
            payRecord = payRecord + "<br/>" + ctx.existingPayRecord;
        }
        payRecord = trimPayRecord(payRecord);
        int actualBetCount = ctx.payLines.size();
        if (actualBetCount > 0) {
            updatePayCountNum(currentPlanNo, actualBetCount);
        }
        String current_paycountnum = actualBetCount > 0 ? String.valueOf(actualBetCount)
                : redisService.get("paycountnum" + currentPlanNo);
        String before_wincountnum=redisService.get("wincountnum"+beforenum);
        String newpayrecore="<font color='blue'>期数："+currentPlanNo+"    上期赢的个数："+before_wincountnum+"     投入号个数："+current_paycountnum+" </font><br/>"+payRecord;
        redisService.set("payRecord", newpayrecore);

        String chooseNum = payRecordBuilder.toString();
        redisService.set("chooseNum", chooseNum);
        redisService.set("shownumchoose", chooseNum);

        if (ctx.fileContent.length() > 0) {
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String formattedDate = dateFormat.format(currentDate);
            String filePath = mybean.getFilepath() + "paymoney" + formattedDate + ".txt";
            AgentMoney.writefilepath(filePath, ctx.fileContent.toString());
        }
        log.info("[批量投注] 期数:{} 笔数:{}", ctx.planNo, actualBetCount);
    }

    private String trimPayRecord(String payRecord) {
        String[] arr = payRecord.split("<br/>");
        if (arr.length <= 100) {
            return payRecord;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            if (i > 0) {
                sb.append("<br/>");
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    /**
     * 模拟扣钱
     * @param
     * @param
     */
    public   void  preAgentPayMoney(String planno,String lushu,String xianzhicishu,String cishu,String payMoneystr2,String playnum,String playNumScore){
        log.warn("支付 期数:"+planno+" 路数:"+lushu+" 号码:"+playnum+" 分数:"+playNumScore+" 限制次数:"+xianzhicishu+" 投注次数:"+cishu+" 投注前金额:"+0+" 投注金额:"+payMoneystr2+" 余额:"+0);

    }


    /**
     * 模拟扣钱
     * @param
     * @param
     */
    public   void  agentPayMoney(String planno,String lushu,String xianzhicishu,String cishu,String payMoneystr2,String playnum,String playNumScore){
        PayBatchContext batch = payBatchContext.get();
        if (batch != null) {
            synchronized (batch) {
                recordAgentPayMoney(batch, planno, lushu, xianzhicishu, cishu, payMoneystr2, playnum, playNumScore);
            }
            return;
        }
        recordAgentPayMoneyImmediate(planno, lushu, xianzhicishu, cishu, payMoneystr2, playnum, playNumScore);
    }

    private void recordAgentPayMoney(PayBatchContext batch, String planno, String lushu, String xianzhicishu,
                                     String cishu, String payMoneystr2, String playnum, String playNumScore) {
        String initmoneyBefore = MyStringUtils.string3double(batch.initmoney + "");
        double payMoney = Double.parseDouble(payMoneystr2);
        batch.initmoney -= payMoney;
        log.debug("支付 期数:{} 路数:{} 号码:{} 金额:{}", planno, lushu, playnum, payMoneystr2);
        String content = MyStringUtils.formatPayDisplayHtml(planno, lushu, playnum, playNumScore, xianzhicishu, cishu,
                initmoneyBefore, payMoneystr2, batch.initmoney + "");
        batch.payLines.add(0, content);
        batch.fileContent.append("\n支付 ")
                .append(" 期数:").append(planno)
                .append(" 路数:").append(lushu)
                .append(" 号码:").append(playnum)
                .append(" 分数:").append(playNumScore)
                .append(" 限制次数:").append(xianzhicishu)
                .append(" 投注次数:").append(cishu)
                .append(" 投注前金额:").append(MyStringUtils.string3double(initmoneyBefore))
                .append(" 投注金额:").append(MyStringUtils.string3double(payMoneystr2))
                .append(" 余额:").append(MyStringUtils.string3double(batch.initmoney + ""));
        String currentCount = batch.countMapold.get(playNumScore);
        if (isValidScoreKey(playNumScore)) {
            int newCount = (currentCount != null) ? Integer.parseInt(currentCount) + 1 : 1;
            batch.countMapold.put(playNumScore, String.valueOf(newCount));
        }
        batch.dirty = true;
    }

    private void recordAgentPayMoneyImmediate(String planno, String lushu, String xianzhicishu, String cishu,
                                              String payMoneystr2, String playnum, String playNumScore) {
        String  initmoney=redisService.get("initmoney");//设置的数字
        if(MyStringUtils.valueIsEmpty(initmoney)){
            initmoney="0";
        }
        double initmoneytotal = Double.parseDouble(initmoney) - Double.parseDouble(payMoneystr2);
        redisService.set("initmoney", MyStringUtils.string3double(initmoneytotal + ""));
         log.warn("支付 期数:"+planno+" 路数:"+lushu+" 号码:"+playnum+" 分数:"+playNumScore+" 限制次数:"+xianzhicishu+" 投注次数:"+cishu+" 投注前金额:"+initmoney+" 投注金额:"+payMoneystr2+" 余额:"+initmoneytotal);
        String content=MyStringUtils.formatPayDisplayHtml(planno, lushu, playnum, playNumScore, xianzhicishu, cishu,
                initmoney, payMoneystr2, initmoneytotal + "");
        appendChooseNum(content);

        String content2="\n支付 " +
                " 期数:"+planno+"" +
                " 路数:"+lushu+"" +
                " 号码:"+playnum+"" +
                " 分数:"+playNumScore+"" +
                " 限制次数:"+xianzhicishu+"" +
                " 投注次数:"+cishu+" " +
                " 投注前金额:"+MyStringUtils.string3double(initmoney)+"" +
                " 投注金额:"+MyStringUtils.string3double(payMoneystr2)+" " +
                " 余额:"+MyStringUtils.string3double(initmoneytotal+"")+"";

        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(currentDate);
        String filePath = mybean.getFilepath()+"paymoney"+formattedDate+".txt";
        AgentMoney.writefilepath(filePath,content2);

        Map<String, String> countMaphsetold = (Map<String, String>) redisService.hget("countMapold");
        if (countMaphsetold == null) {
            countMaphsetold = new HashMap<>();
        }
        String currentCount = countMaphsetold.get(playNumScore);
        if (isValidScoreKey(playNumScore)) {
            int newCount = (currentCount != null) ? Integer.parseInt(currentCount) + 1 : 1;
            countMaphsetold.put(playNumScore, String.valueOf(newCount));
            redisService.hmset("countMapold", (HashMap<String, String>) countMaphsetold);
        }
        saveCountMapToRedisold(planno);
        costSwtichByBanlance(initmoneytotal+"");
        String sbstr=redisService.get("payRecord");
        if(MyStringUtils.valueIsNotEmpty(sbstr)){
            sbstr=content+"<br/>"+sbstr;
        }else{
            sbstr=content;
        }
        redisService.set("payRecord", trimPayRecord(sbstr));
    }

    /**
     * 即点即投（cached）：用 Redis 缓存出号立刻真钱下单，绕过 polyIsOpen 策略。
     */
    public CachedRealPayResult betNowRealFromCached(int ticketId) {
        CachedRealPayResult result = new CachedRealPayResult();
        String lockKey = REAL_PAY_LOCK_PREFIX + ticketId;
        if (!redisService.setIfAbsent(lockKey, "1", 30)) {
            return failCachedRealPay(result, "操作进行中，请勿重复点击");
        }
        try {
            return doBetNowRealFromCached(ticketId, result);
        } finally {
            redisService.del(lockKey);
        }
    }

    private CachedRealPayResult doBetNowRealFromCached(int ticketId, CachedRealPayResult result) {
        String token = redisService.get("tokenlogin");
        if (MyStringUtils.valueIsEmpty(token)) {
            return failCachedRealPay(result, "未设置token");
        }
        String tokenmsg = redisService.get("tokenmsg");
        if (MyStringUtils.valueIsNotEmpty(tokenmsg) || TOKEN_HAVE_ERROR) {
            return failCachedRealPay(result, "会话过期，请重新登录");
        }

        Map<String, String> header = getHeader(token);
        TimeList.DataItem planItem = getPlanNow(ticketId, header);
        if (planItem == null) {
            return failCachedRealPay(result, "获取当期期号失败");
        }
        String currentPlanNo = planItem.getPlanId();
        String beforePlanNo = planItem.getBeforePlanNo();
        result.setPlanNo(currentPlanNo);

        if ("true".equals(redisService.get(REAL_PAY_DONE_PREFIX + currentPlanNo))) {
            return failCachedRealPay(result, "本期已真钱投注，请勿重复",
                    "本期已真钱投注，请勿重复, planNo=" + currentPlanNo);
        }

        Map<String, String> planNumberMap = redisService.hget("planNumberMap");
        if (planNumberMap == null || planNumberMap.isEmpty()) {
            return failCachedRealPay(result, "无缓存出号，请先等待预测或刷新");
        }

        String cachedPlanNo = redisService.get("planNumberMapPlanNo");
        result.setCachedPlanNo(cachedPlanNo);
        if (MyStringUtils.valueIsNotEmpty(cachedPlanNo) && !cachedPlanNo.equals(currentPlanNo)) {
            return failCachedRealPay(result, "缓存期号与当期不一致，请刷新后重试",
                    "缓存期号与当期不一致, cachedPlanNo=" + cachedPlanNo + ", currentPlanNo=" + currentPlanNo);
        }

        if (!hasAnyCachedBettableNumbers(planNumberMap)) {
            return failCachedRealPay(result, "无可投号码",
                    "无可投号码, planNo=" + currentPlanNo);
        }

        List<CachedRealPayTask> tasks = buildCachedRealPayTasks(ticketId, beforePlanNo, currentPlanNo, planNumberMap);
        List<CachedRealPayDetail> details = new ArrayList<>();
        for (CachedRealPayTask task : tasks) {
            details.add(executeCachedRealPayTask(header, currentPlanNo, task));
        }
        result.getDetails().addAll(details);
        int totalBets = 0;
        int failCount = 0;
        for (CachedRealPayDetail detail : details) {
            if (detail.isSuccess()) {
                totalBets += detail.getBetCount();
            } else {
                failCount++;
            }
        }

        if (totalBets == 0) {
            result.setSuccess(false);
            result.setMessage(failCount > 0 ? "真钱下单失败" : "无可投号码");
            result.setRealBetCount(0);
            result.setFailCount(failCount);
            logCachedRealPayRoadFailures(details);
            log.warn("[即点真投] planNo={} 失败: {}, 失败路数={}", currentPlanNo, result.getMessage(), failCount);
            fillBalanceAfterRealPay(header, result);
            return result;
        }

        if (failCount == 0) {
            redisService.set(REAL_PAY_DONE_PREFIX + currentPlanNo, "true");
            result.setSuccess(true);
            result.setMessage("真钱下单成功");
            log.info("[即点真投] planNo={} 成功, 笔数={}", currentPlanNo, totalBets);
        } else {
            result.setSuccess(false);
            result.setMessage("部分路数下单失败，请查看明细");
            logCachedRealPayRoadFailures(details);
            log.warn("[即点真投] planNo={} 部分失败, 成功笔数={}, 失败路数={}", currentPlanNo, totalBets, failCount);
        }
        result.setRealBetCount(totalBets);
        result.setFailCount(failCount);
        fillBalanceAfterRealPay(header, result);
        return result;
    }

    private CachedRealPayResult failCachedRealPay(CachedRealPayResult result, String userMessage) {
        return failCachedRealPay(result, userMessage, userMessage);
    }

    private CachedRealPayResult failCachedRealPay(CachedRealPayResult result, String userMessage, String logDetail) {
        result.setSuccess(false);
        result.setMessage(userMessage);
        log.warn("[即点真投] 失败: {}", logDetail);
        return result;
    }

    private void logCachedRealPayRoadFailures(List<CachedRealPayDetail> details) {
        for (CachedRealPayDetail detail : details) {
            if (!detail.isSuccess()) {
                log.warn("[即点真投] 路数:{} 失败, 响应: {}", detail.getPlayNo(), detail.getResponse());
            }
        }
    }

    private List<CachedRealPayTask> buildCachedRealPayTasks(int ticketId, String beforePlanNo, String currentPlanNo,
                                                            Map<String, String> planNumberMap) {
        String lushu = redisService.get("lushu");
        String[] lushuPaths = MyStringUtils.stringNotNull(lushu, "1").split(",");
        List<CachedRealPayTask> tasks = new ArrayList<>();
        for (String playNo : lushuPaths) {
            if (MyStringUtils.valueIsEmpty(playNo)) {
                continue;
            }
            String lutouzhu = redisService.get("touzhu" + playNo);
            if (MyStringUtils.valueIsNotEmpty(lutouzhu) && "false".equals(lutouzhu)) {
                continue;
            }
            String numstr = planNumberMap.get(playNo);
            if (MyStringUtils.valueIsEmpty(numstr)) {
                continue;
            }
            OrderParams orderParams = buildOrderParamsChoosebyjavaMuti(ticketId, beforePlanNo, currentPlanNo, playNo, numstr, playNo);
            if (orderParams.params.indexOf("bet") <= -1) {
                continue;
            }
            tasks.add(new CachedRealPayTask(playNo, orderParams.params, orderParams.betCount));
        }
        return tasks;
    }

    private CachedRealPayDetail executeCachedRealPayTask(Map<String, String> header, String currentPlanNo,
                                                         CachedRealPayTask task) {
        CachedRealPayDetail detail = new CachedRealPayDetail();
        detail.setPlayNo(task.playNo);
        detail.setBetCount(task.betCount);
        try {
            String response = postRealCreateOrder(header, currentPlanNo, task.dparams);
            detail.setResponse(response);
            detail.setSuccess(isRealOrderResponseSuccess(response));
            if (!detail.isSuccess()) {
                log.warn("[即点真投] 路数:{} 下单被拒, 响应: {}", task.playNo, response);
            }
        } catch (Exception e) {
            log.error("[即点真投] 路数:{} 下单异常: {}", task.playNo, e.getMessage());
            detail.setSuccess(false);
            detail.setResponse(e.getMessage());
        }
        return detail;
    }

    private void fillBalanceAfterRealPay(Map<String, String> header, CachedRealPayResult result) {
        try {
            double balance = queryBalanceOnly(header);
            result.setBalance(MyStringUtils.string3double(balance + ""));
        } catch (Exception e) {
            log.warn("[即点真投] 查询余额失败: {}", e.getMessage());
        }
    }

    private boolean hasAnyCachedBettableNumbers(Map<String, String> planNumberMap) {
        String lushu = redisService.get("lushu");
        String[] lushuPaths = MyStringUtils.stringNotNull(lushu, "1").split(",");
        for (String playNo : lushuPaths) {
            if (MyStringUtils.valueIsEmpty(playNo)) {
                continue;
            }
            String numstr = planNumberMap.get(playNo);
            if (MyStringUtils.valueIsNotEmpty(numstr) && hasCachedBettableNumbersOnRoad(playNo, numstr)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCachedBettableNumbersOnRoad(String playNo, String numstr) {
        String[] items = numstr.split(",");
        for (String item : items) {
            String[] parts = MyStringUtils.parseBetItem(item);
            if (parts == null) {
                continue;
            }
            String nowNumber = parts[0];
            if (MyStringUtils.valueIsEmpty(nowNumber)) {
                continue;
            }
            String key = playNo + "-" + nowNumber;
            String payMoney = redisService.get(key + "-payMoney");
            String touzhu = redisService.get("touzhu" + playNo + "-" + nowNumber);
            if ("true".equals(touzhu) && MyStringUtils.valueIsNotEmpty(payMoney) && Double.parseDouble(payMoney) > 0) {
                return true;
            }
        }
        return false;
    }

    private String postRealCreateOrder(Map<String, String> header, String planNo, String dparams) {
        String orderurl = apiurl + "/coron/order/double/create";
        HttpRequest createnum = HttpUtil.createPost(orderurl).addHeaders(header);
        createnum.body(dparams);
        createnum.header("Content-Type", "application/x-www-form-urlencoded");
        HttpResponse response = executeWithTiming(createnum, "betNowRealCached planNo=" + planNo);
        return response.body();
    }

    private boolean isRealOrderResponseSuccess(String response) {
        if (MyStringUtils.valueIsEmpty(response)) {
            return false;
        }
        String lower = response.toLowerCase();
        if (lower.contains("token") && (lower.contains("error") || lower.contains("过期") || lower.contains("invalid"))) {
            TOKEN_HAVE_ERROR = true;
            return false;
        }
        try {
            Map<String, Object> map = JSON.parseObject(response, Map.class);
            if (map != null && map.get("code") != null) {
                int code = Integer.parseInt(map.get("code").toString());
                return code == 0 || code == 200;
            }
        } catch (Exception ignore) {
        }
        return !lower.contains("\"success\":false") && !lower.contains("\"code\":-1");
    }

    private List<RoadTask> buildRoadTasks(String[] chooseIndexValue, Map<String, String> parammap) {
        List<RoadTask> tasks = new ArrayList<>();
        for (String playNo : chooseIndexValue) {
            if (MyStringUtils.valueIsEmpty(playNo)) {
                continue;
            }
            String lutouzhu = redisService.get("touzhu" + playNo);
            if (MyStringUtils.valueIsNotEmpty(lutouzhu) && "false".equals(lutouzhu)) {
                log.info(lutouzhu + "不投注....");
                continue;
            }
            String numstr = parammap.get(playNo) + "";
            tasks.add(new RoadTask(playNo, numstr));
        }
        return tasks;
    }

    private int runParallelPreCreate(int ticketId, String beforePlanNo, String currentPlanNo, List<RoadTask> tasks) {
        if (tasks.isEmpty()) {
            return 0;
        }
        if (!parallelEnabled || tasks.size() == 1) {
            int sum = 0;
            for (RoadTask task : tasks) {
                sum += isPayFlagForRoad(ticketId, beforePlanNo, currentPlanNo, task.playNo, task.numstr, task.playNo);
            }
            return sum;
        }
        List<Future<Integer>> futures = new ArrayList<>();
        PayBatchContext batch = payBatchContext.get();
        for (RoadTask task : tasks) {
            futures.add(parallelExecutor.submit(() -> {
                payBatchContext.set(batch);
                try {
                    return isPayFlagForRoad(ticketId, beforePlanNo, currentPlanNo, task.playNo, task.numstr, task.playNo);
                } finally {
                    payBatchContext.remove();
                }
            }));
        }
        int sum = 0;
        for (Future<Integer> future : futures) {
            try {
                Integer n = future.get();
                if (n != null) {
                    sum += n;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("虚拟投注并行任务被中断", e);
            } catch (ExecutionException e) {
                log.error("虚拟投注并行任务失败", e.getCause());
                throw new RuntimeException(e.getCause());
            }
        }
        log.debug("[并行投注] 虚拟投注 {} 路完成, 本批投入号数:{}", tasks.size(), sum);
        return sum;
    }

    private void runParallelCreateOrder(int ticketId, String beforePlanNo, String currentPlanNo,
                                        Map<String, String> header, List<RoadTask> tasks) {
        if (tasks.isEmpty()) {
            return;
        }
        for (RoadTask task : tasks) {
            createOrderjavaMuti(ticketId, beforePlanNo, currentPlanNo, header, task.playNo, task.numstr, task.playNo);
        }
        log.debug("[串行投注] 真实下单 {} 路完成", tasks.size());
    }

    private void updatePayCountNum(String planNo, int paycountnum) {
        if (paycountnum <= 0) {
            return;
        }
        redisService.set("paycountnumnow" + planNo, paycountnum + "");
        redisService.set("paycountnum" + planNo, paycountnum + "");
    }

    private void awaitFutures(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("并行任务被中断", e);
            } catch (ExecutionException e) {
                log.error("并行任务失败", e.getCause());
                throw new RuntimeException(e.getCause());
            }
        }
    }

}
