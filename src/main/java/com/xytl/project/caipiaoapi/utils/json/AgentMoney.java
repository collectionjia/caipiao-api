package com.xytl.project.caipiaoapi.utils.json;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class AgentMoney {


    public static void writefilepath(String filePath, String content) {
        String osName = System.getProperty("os.name", "");
        if (osName.toLowerCase().contains("win")) {
            log.debug("Windows环境跳过文件写入: {}", filePath);
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(content);
        } catch (IOException e) {
            log.warn("追加内容到文件时发生错误: {} - {}", filePath, e.getMessage());
        }
    }



}
