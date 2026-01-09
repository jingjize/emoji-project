package com.example.meme.config;

import com.example.meme.interceptor.LoggingInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 配置类
 * 配置静态资源映射和拦截器
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private LoggingInterceptor loggingInterceptor;
    
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
        
        log.info("静态资源映射: /output/** -> {}", fileUrl);
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册日志拦截器，拦截所有 /api/meme 路径下的请求
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/meme/**")
                .excludePathPatterns("/api/meme/health"); // 健康检查接口不记录日志
    }
}

