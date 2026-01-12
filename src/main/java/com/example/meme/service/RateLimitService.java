package com.example.meme.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流服务
 * 控制每天最多生成20张图片
 */
@Slf4j
@Service
public class RateLimitService {
    
    // 每天最大生成次数
    private static final int MAX_GENERATIONS_PER_DAY = 20;
    
    // 存储每个IP每天的生成次数
    // Key: IP地址 + 日期字符串 (格式: "IP_YYYY-MM-DD")
    // Value: 生成次数
    private final Map<String, Integer> dailyCounts = new ConcurrentHashMap<>();
    
    // 当前日期，用于自动清理过期数据
    private LocalDate currentDate = LocalDate.now();
    
    /**
     * 检查是否可以生成图片
     * 
     * @param clientIp 客户端IP地址
     * @return true 可以生成，false 超过限制
     */
    public boolean canGenerate(String clientIp) {
        // 清理过期数据（如果日期变化）
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            synchronized (this) {
                if (!today.equals(currentDate)) {
                    dailyCounts.clear();
                    currentDate = today;
                    log.info("日期变化，已清理限流计数，新日期: {}", today);
                }
            }
        }
        
        String key = generateKey(clientIp, today);
        int count = dailyCounts.getOrDefault(key, 0);
        
        return count < MAX_GENERATIONS_PER_DAY;
    }
    
    /**
     * 增加生成次数
     * 
     * @param clientIp 客户端IP地址
     */
    public void incrementCount(String clientIp) {
        LocalDate today = LocalDate.now();
        String key = generateKey(clientIp, today);
        int newCount = dailyCounts.merge(key, 1, Integer::sum);
        log.debug("IP {} 今日生成次数: {}/{}", clientIp, newCount, MAX_GENERATIONS_PER_DAY);
    }
    
    /**
     * 获取今日剩余生成次数
     * 
     * @param clientIp 客户端IP地址
     * @return 剩余次数
     */
    public int getRemainingCount(String clientIp) {
        LocalDate today = LocalDate.now();
        String key = generateKey(clientIp, today);
        int count = dailyCounts.getOrDefault(key, 0);
        return Math.max(0, MAX_GENERATIONS_PER_DAY - count);
    }
    
    /**
     * 生成存储键
     * 
     * @param clientIp IP地址
     * @param date 日期
     * @return 键值
     */
    private String generateKey(String clientIp, LocalDate date) {
        return clientIp + "_" + date.toString();
    }
}

