package com.xytl.project.caipiaoapi.controller;

import com.xytl.project.caipiaoapi.config.RedisService;
import com.xytl.project.caipiaoapi.domain.data.CachedRealPayResult;
import com.xytl.project.caipiaoapi.domain.data.InitResult;
import com.xytl.project.caipiaoapi.domain.data.MsgResult;
import com.xytl.project.caipiaoapi.domain.data.TimeList;
import com.xytl.project.caipiaoapi.domain.piaocoder.CodeStat;
import com.xytl.project.caipiaoapi.domain.piaocoder.CodeStatItem;
import com.xytl.project.caipiaoapi.domain.piaocoder.PiaoCoder;
import com.xytl.project.caipiaoapi.domain.resp.Resp;
import com.xytl.project.caipiaoapi.domain.systemconfig.SystemConfig;
import com.xytl.project.caipiaoapi.service.piaocoder.PiaoCoderService;
import com.xytl.project.caipiaoapi.service.user.SystemConfigService;
import com.xytl.project.caipiaoapi.utils.json.DateUtil;
import com.xytl.project.caipiaoapi.utils.json.MyBean;
import com.xytl.project.caipiaoapi.utils.json.MyStringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

@Tag(name = "彩票相关接口")
@RestController
@Slf4j
public class CaiPiaoApiController {
    @Resource
    private PiaoCoderService piaoCoderService;
    @Resource
    private SystemConfigService systemConfigService;

    @Autowired
    RedisService redisService;
    @Resource
    MyBean mybean;

    @Operation(summary = "ai测算7数据", description = "count生成条数")
    @GetMapping("/api/cp/ai")
    public Resp<String> ai7(int type, int count) throws Exception {
        String numstrstr="";
        String isopen=redisService.get("isopen");
        if("true".equals(isopen)){
            numstrstr=piaoCoderService.loadTimeByCronByjava(type);
        }else{
            numstrstr="停止预测";
        }
        return Resp.okData(numstrstr);
    }




    @Operation(summary = "ai测算7数据", description = "count生成条数")
    @GetMapping("/api/cp/aishow")
    public Resp<MsgResult> ai7show() throws IOException {
        String banlance=redisService.get("banlance");//余额
        String initmoney=redisService.get("initmoney");//余额
        String shownumchoose=redisService.get("shownumchoose");
        String chooseNum=redisService.get("chooseNum");
        if(MyStringUtils.valueIsEmpty(shownumchoose)){
            shownumchoose=chooseNum;
        }
        String currentNum=redisService.get("currentNum");
        String payRecord=redisService.get("payRecord");
        String countMapData=redisService.get("countMapData");
        String countMapDataold=redisService.get("countMapDataold");


        int gametype=Integer.parseInt(mybean.getGametype());
        String tokenmsg=redisService.get("tokenmsg");
        String str="";
        if(StringUtils.isNotEmpty(tokenmsg)){
            str=str+"<font>会话过期,请重新登录</font><br/>";
        }
        String resultiszhong=redisService.get("resultiszhong");
        str=str+"虚拟余额："+MyStringUtils.spanRed(initmoney)+"<br/>"+"  " +"真实余额："+banlance+"<br/>"+"  " +
                "<div style='float:left;border:1px red solid;margin:4px;' > "+shownumchoose+" </div> <br/>"+resultiszhong ;
        MsgResult msgResult=new MsgResult();
        msgResult.setChooseNum(MyStringUtils.fontToSpan(shownumchoose));
        msgResult.setWinRecord(MyStringUtils.fontToSpan(resultiszhong));
        msgResult.setCurrentNum(MyStringUtils.fontToSpan(currentNum));
        msgResult.setPayRecord(MyStringUtils.fontToSpan(payRecord));
        msgResult.setInitmoney(initmoney);
        msgResult.setAgentmoney(banlance);
        msgResult.setCountmapdata(MyStringUtils.fontToSpan(countMapData));
        msgResult.setCountmapdataold(MyStringUtils.fontToSpan(countMapDataold));

        return Resp.okData(msgResult);
    }




    @Operation(summary = "获取近期的彩票数据最多100条")
    @PostMapping("/api/cp/listTopData")
    @Parameters({ @Parameter(name = "type", description = "类型48飞艇67赛车", example = "48"),
            @Parameter(name = "count", description = "返回条数", example = "100") })
    public Resp<List<PiaoCoder>> listTopData(int type, int count) {
        return Resp.okData(piaoCoderService.listTopN(type, count));
    }

    @Operation(summary = "获取近100期的数据分析")
    @PostMapping("/api/cp/stat")
    @Parameters({ @Parameter(name = "type", description = "类型48飞艇67赛车", example = "48"),
            @Parameter(name = "count", description = "分析期数、默认100", example = "100") })
    public Resp<List<CodeStat>> listTopData(int type, @RequestParam(required = false) Integer count) {
        if (count == null) {
            count = 100;
        }
        List<PiaoCoder> list = piaoCoderService.listTopN(type, count);
        Map<Integer, Map<Integer, Integer>> allMap = new TreeMap<>();
        List<CodeStat> resList = new ArrayList<CodeStat>();
        for (PiaoCoder coder : list) {
            String[] nums = coder.getCode().split(",");
            for (int i = 0; i < nums.length; i++) {
                Map<Integer, Integer> indexMap = allMap.get(i);
                if (indexMap == null) {
                    indexMap = new HashMap<Integer, Integer>();
                    allMap.put(i, indexMap);
                }
                int num = Integer.parseInt(nums[i]);
                Integer times = indexMap.get(num);
                if (times == null) {
                    indexMap.put(num, 1);
                } else {
                    indexMap.put(num, times + 1);
                }
            }
        }

        for (Entry<Integer, Map<Integer, Integer>> entry : allMap.entrySet()) {
            CodeStat stat = new CodeStat();
            resList.add(stat);
            stat.setIndex(entry.getKey());
            List<CodeStatItem> sList = new ArrayList<CodeStatItem>();
            stat.setStat(sList);
            for (int i = 1; i <= 10; i++) {
                CodeStatItem item = new CodeStatItem();
                sList.add(item);
                item.setNumber(i);
                Integer times = entry.getValue().get(i);
                if (times == null) {
                    times = 0;
                }
                item.setTimes(times == null ? 0 : times.intValue());
                item.setRate(times * 100 / count);
                item.setWillRate((count - times) * 100 / count);
            }
        }
        return Resp.okData(resList);
    }



    @Operation(summary = "更新采集Token")
    @PostMapping("/api/cp/updateToken")
    @Parameters({ @Parameter(name = "token", description = "token", example = "") })
    public Resp<Boolean> updateToken(String token) {
        redisService.set("tokenlogin", token);
        systemConfigService.updateToken(token);
        return Resp.okData(true);
    }

    @Operation(summary = "获取系统配置")
    @PostMapping("/api/cp/getConfig")
    @Parameters({ @Parameter(name = "code", description = "编码,openai-key,openai-url", example = "")})
    public Resp<SystemConfig> getConfig(String code) {
        return Resp.okData(systemConfigService.getByCode(code));
    }

    @Operation(summary = "更新系统配置")
    @PostMapping("/api/cp/updateConfig")
    @Parameters({ @Parameter(name = "code", description = "编码,openai-key,openai-url", example = "")
    ,@Parameter(name = "value", description = "值", example = "")})
    public Resp<Boolean> updateConfig(String code, String value) {
        SystemConfig config = systemConfigService.getByCode(code);
        if(config == null) {
            return Resp.errorByMsg("不存在该编码");
        }
        config.setValue(value);
        systemConfigService.update(config);
        return Resp.okData(true);
    }

    @Operation(summary = "获取Token状态")
    @GetMapping("/api/cp/getTokenState")
    public Resp<Boolean> getTokenState() {
        return Resp.okData(!PiaoCoderService.TOKEN_HAVE_ERROR);
    }

    @Operation(summary = "获取当期销售期倒计时")
    @GetMapping("/api/cp/getNow")
    @Parameters({ @Parameter(name = "type", description = "类型48飞艇67赛车", example = "48") })
    public Resp<TimeList.DataItem> getNow(int type) {
        TimeList.DataItem item = piaoCoderService.queryPlanNow(type);
        if (item == null) {
            return Resp.errorByMsg("获取当期期数失败，请检查 Token 是否有效");
        }
        return Resp.okData(item);
    }



    @Operation(summary = "清空预测结果")
    @GetMapping("/api/cp/tuncateResult")
    public Resp<String> tuncateResult() throws IOException {
        String numstrstr="操作成功";
        String datestr= DateUtil.getDate();
        redisService.del(datestr+"-num");
        redisService.del(datestr+"-count");
        redisService.del(datestr+"-numcount");
        redisService.del("num1");
        redisService.del("goodnum");

        redisService.del("showCurrentNum");
        redisService.del("showBeforeNum");
        redisService.del("beforeRecommandluck");
        redisService.del("showCurrentNumAll");
        redisService.del("recommandCount");
        redisService.del("*recommand");
        redisService.del("wincountnum*");
        redisService.del("paycountnum*");
        redisService.set("resultiszhong","");
        redisService.set("banlance","");
        redisService.set("initmoney","");
        redisService.set("shownumchoose","");
        redisService.set("currentNum", "");
        redisService.set("payRecord", "");
        redisService.set("countMapData", "");
        redisService.set("countMapDataold", "");

        redisService.set("firstplanno", "");
        redisService.set("otherplanno", "");
        redisService.del("countMap");
        redisService.del("countMapold");




        for(int i=0;i<10;i++){
            int playNo=i+1;
            redisService.set("countNotWinCount"+playNo,"0");
            redisService.set("payMoney"+playNo,"0");
            redisService.set("oldwin"+playNo,"0");
            redisService.set("touzhu"+playNo,"true");
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
        redisService.del("planNumberMap");
        redisService.getother("luckLu");

        return Resp.okData(numstrstr);
    }


    @Operation(summary = "是否打开真实投注")
    @GetMapping("/api/cp/startpay")
    public Resp<String> startpay(String startpay) throws IOException {
        String numstrstr="操作成功";
        redisService.set("startpay",startpay);//一次预测多少组数据
        return Resp.okData(numstrstr);
    }

    @Operation(summary = "即点即投（cached）：用缓存出号立刻真钱下单")
    @PostMapping("/api/cp/betNowRealCached")
    public Resp<CachedRealPayResult> betNowRealCached(@RequestParam(required = false) Integer type) {
        int ticketId = type != null ? type : Integer.parseInt(mybean.getGametype());
        CachedRealPayResult result = piaoCoderService.betNowRealFromCached(ticketId);
        if (result.isSuccess()) {
            return Resp.okData(result);
        }
        return Resp.error(result.getMessage(), result);
    }


    @Operation(summary = "是否打开AI预测")
    @PostMapping("/api/cp/isopen")
    public Resp<String> openAiYuCe(String isopen,String qishu,String onceNum,String countNum,String isTouzhu,String suanfatype,String lushu,String createmoney,
                                   String initmoney,String isColseByMoney,String isColseByCount,String nocount,String countZu,String duozu,String mincost,String maxcost,String agentpay,String startpay) throws IOException {
        String numstrstr="操作成功";
        redisService.set("isopen",isopen);//获取是否打开预测
        redisService.set("isTouzhu",isTouzhu);//获取是否打开预测
        redisService.set("qishu",qishu);//获取最新的多少期
        redisService.set("onceNum",onceNum);//一次预测多少组数据
        redisService.set("countNum",countNum);//一共预测几把
        redisService.set("countZu",countZu);//一次预测多少组数据
        redisService.set("agentpay",agentpay);//一次预测多少组数据
        redisService.set("startpay",startpay);//一次预测多少组数据
        redisService.set("suanfatype",suanfatype);//算法类型
        redisService.set("lushu",lushu);//算法类型
        redisService.set("createmoney",createmoney);//投注金额
        redisService.set("initmoney",initmoney);//成本
        redisService.set("isColseByMoney",isColseByMoney);//是否根据金额的额度进行做关闭
        redisService.set("isColseByCount",isColseByCount);//是否根据次数进行统计
        redisService.set("nocount",nocount);//没中的次数
        redisService.set("duozu",duozu);//是否根据次数进行统计
        redisService.set("mincost",mincost);//最小预算值
        redisService.set("maxcost",maxcost);//最大预算值


        return Resp.okData(numstrstr);
    }


    @Operation(summary = "页面值初始化", description = "页面值初始化")
    @GetMapping("/api/cp/initdata")
    public Resp<InitResult> init()  {
        log.info("redis链接上了");
        String isopen=redisService.get("isopen");//获取是否打开预测
        String isTouzhu=redisService.get("isTouzhu");//获取是否打开投注
        String qishu=redisService.get("qishu");//获取最新的多少期
        String onceNum=redisService.get("onceNum");//一次预测多少组数据
        String countNum=redisService.get("countNum");//一共预测几把
        String countZu=redisService.get("countZu");//一共预测几把
        String suanfatype=redisService.get("suanfatype");//算法类型
        String lushu=redisService.get("lushu");//算法类型
        String createmoney=redisService.get("createmoney");//投注金额
        String initmoney=redisService.get("initmoney");//投注金额
        String isColseByMoney=redisService.get("isColseByMoney");//投注金额
        String isColseByCount=redisService.get("isColseByCount");//是否根据次数进行关停
        String nocount=redisService.get("nocount");//未中的次数

        String duozu=redisService.get("duozu");//是否多组

        String mincost=redisService.get("mincost");//是否多组
        String maxcost=redisService.get("maxcost");//是否多组
        String agentpay=redisService.get("agentpay");//是否自动模拟支付
        String startpay=redisService.get("startpay");//是否自动模拟支付

        String countMapData=redisService.get("countMapData");//是否自动模拟支付
        String countMapDataold=redisService.get("countMapDataold");//是否自动模拟支付



        InitResult initResult=new InitResult();
        initResult.setIsTouzhu(Boolean.valueOf(isTouzhu));
        initResult.setIsopen(Boolean.valueOf(isopen));
        initResult.setQishu(qishu);
        initResult.setOnceNum(onceNum);
        initResult.setSuanfatype(suanfatype);
        initResult.setCountNum(countNum);
        initResult.setCountZu(countZu);
        initResult.setCreatemoney(createmoney);
        initResult.setLushu(lushu);
        initResult.setInitmoney(initmoney);
        initResult.setIsColseByMoney(Boolean.valueOf(isColseByMoney));
        initResult.setIsColseByCount(Boolean.valueOf(isColseByCount));
        initResult.setNocount(nocount);
        initResult.setDuozu(duozu);
        initResult.setMincost(mincost);
        initResult.setMaxcost(maxcost);
        initResult.setAgentpay(Boolean.valueOf(agentpay));
        initResult.setStartpay(Boolean.valueOf(startpay));
        initResult.setCountmapdata(countMapData);
        initResult.setCountmapdataold(countMapDataold);
        return Resp.okData(initResult);
    }


}
