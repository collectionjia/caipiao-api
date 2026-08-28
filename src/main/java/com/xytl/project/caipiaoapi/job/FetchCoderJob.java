package com.xytl.project.caipiaoapi.job;

import com.xytl.project.caipiaoapi.controller.CaiPiaoApiController;
import com.xytl.project.caipiaoapi.service.piaocoder.PiaoCoderService;
import com.xytl.project.caipiaoapi.utils.json.MyBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Slf4j
@Component
public class FetchCoderJob {

    @Resource
    private PiaoCoderService piaoCoderService;

	@Resource
	private CaiPiaoApiController caiPiaoApiController;

	@Resource
	MyBean mybean;


	@Scheduled(cron = "2 * * * * ?")
	private synchronized void yuce() {
	    log.info("loadCaiPiaoData job start");
		try {
			//飞艇
//			caiPiaoApiController.ai7(48, 3);
			//赛车
			String gametype=mybean.getGametype();
			caiPiaoApiController.ai7(Integer.parseInt(gametype), 3);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("loadCaiPiaoData job end");
		log.info("===========================================================");
	}



}
