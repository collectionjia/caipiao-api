package com.xytl.project.caipiaoapi.config;

import java.util.HashMap;
import java.util.Map;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

public class Snippet {
    // 飞艇 48
    // 赛车 67
    public static void main(String[] args) throws InterruptedException {
        Map<String, String> header = new HashMap<String, String>();
        header.put("token", "9246a8805b974826a55b72df333dfd251712836961176");
        header.put("User-Agent",
                "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        HttpRequest request = HttpUtil.createGet("https://cawsapibwp2.qh7ud71h.com/coron/trendGraph/chart/history?ticketId=67&num=2")
                .addHeaders(header);
        request.setConnectionTimeout(20000);
        request.setReadTimeout(20000);
        for (int i = 0; i < 100; i++) {
            HttpResponse resp = request.execute();
            System.out.println(resp.body());
            Thread.sleep(1000);
        }
    }
}
