package com.example.meme.interceptor;

import com.example.meme.annotation.LogRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求日志拦截器
 * 自动记录带有 @LogRequest 注解的接口的请求和响应信息
 */
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只处理方法级别的处理器
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        
        // 检查方法是否有 @LogRequest 注解
        LogRequest logRequest = method.getAnnotation(LogRequest.class);
        if (logRequest == null) {
            return true;
        }
        
        // 记录请求参数
        if (logRequest.logParams()) {
            Map<String, Object> params = new HashMap<>();
            
            // 获取所有请求参数
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);
                params.put(paramName, paramValue);
            }
            
            if (!params.isEmpty()) {
                log.info("请求参数: {} {}", request.getMethod(), request.getRequestURI());
                log.info("{}", formatParams(params));
            }
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, 
                          ModelAndView modelAndView) throws Exception {
        // 后置处理在 afterCompletion 中进行
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        // 响应日志在 ResponseBodyAdvice 中记录
        if (ex != null) {
            log.error("请求异常: {} {}", request.getMethod(), request.getRequestURI(), ex);
        }
    }
    
    /**
     * 格式化参数为JSON字符串
     */
    private String formatParams(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return params.toString();
        }
    }
}

