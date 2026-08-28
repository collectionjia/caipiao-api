package com.xytl.project.caipiaoapi.utils.json;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

    @Value("${gametype}")
    private String gametype;

    @Value("${filepath}")
    private String filepath;

    public String getFilepath() {
        return filepath;
    }



    public String getGametype() {
        return gametype;
    }






}
