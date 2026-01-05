package com.example.meme.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 配置类
 * 配置静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录
        String projectRoot = System.getProperty("user.dir");
        Path outputPath = Paths.get(projectRoot, "output").toAbsolutePath();
        String outputDir = outputPath.toString().replace("\\", "/");
        
        // 映射 /output/** 到项目根目录的 output 文件夹
        // Windows 路径需要转换为 file:/// 格式
        String fileUrl = "file:///" + outputDir.replace(":", ":/") + "/";
        registry.addResourceHandler("/output/**")
                .addResourceLocations(fileUrl);
        
        System.out.println("静态资源映射: /output/** -> " + fileUrl);
    }
}

