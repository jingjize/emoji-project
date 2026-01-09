package com.example.meme.advice;

import com.example.meme.annotation.LogRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体日志记录增强器
 * 用于记录带有 @LogRequest 注解的接口的响应体内容
 */
@Slf4j
@ControllerAdvice
public class LoggingResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 检查方法是否有 @LogRequest 注解
        return returnType.hasMethodAnnotation(LogRequest.class);
    }
    
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        LogRequest logRequest = returnType.getMethodAnnotation(LogRequest.class);
        if (logRequest == null || !logRequest.logResponse()) {
            return body;
        }
        
        try {
            // 直接记录响应参数
            if (body != null) {
                log.info("响应参数: {} {}", request.getMethod(), request.getURI().getPath());
                log.info("{}", objectMapper.writeValueAsString(body));
            }
        } catch (Exception e) {
            log.warn("记录响应日志失败: {}", e.getMessage());
        }
        
        return body;
    }
}

