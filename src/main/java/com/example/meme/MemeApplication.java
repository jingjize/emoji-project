package com.example.meme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 表情包生成应用主启动类
 * 
 * @author AI Assistant
 */
@SpringBootApplication
public class MemeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemeApplication.class, args);
        System.out.println("=================================");
        System.out.println("表情包生成服务已启动！");
        System.out.println("访问地址: http://localhost:8443");
        System.out.println("API 文档: POST /api/meme/generate");
        System.out.println("=================================");
    }
}

