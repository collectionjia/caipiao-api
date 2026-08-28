package com.xytl.project.caipiaoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CaiPiaoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaiPiaoApiApplication.class, args);
    }

}
