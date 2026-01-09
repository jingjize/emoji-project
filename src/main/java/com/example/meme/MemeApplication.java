package com.example.meme;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 表情包生成应用主启动类
 * 
 * @author AI Assistant
 */
@Slf4j
@SpringBootApplication
public class MemeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemeApplication.class, args);
        log.info("=================================");
        log.info("表情包生成服务已启动！");
        log.info("访问地址: http://localhost:8443");
        log.info("API 文档: POST /api/meme/generate");
        log.info("=================================");
    }
}

