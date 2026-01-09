package com.example.meme.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求日志记录注解
 * 用于标记需要记录请求和响应日志的接口方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogRequest {
    /**
     * 日志描述，用于标识接口功能
     */
    String value() default "";
    
    /**
     * 是否记录请求参数（默认记录）
     */
    boolean logParams() default true;
    
    /**
     * 是否记录响应结果（默认记录）
     */
    boolean logResponse() default true;
}

