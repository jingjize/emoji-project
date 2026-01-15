package com.example.meme.service;

import com.example.meme.client.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 热门黑话服务
 * 每次调用都返回最新、不同的热门词语
 */
@Slf4j
@Service
public class HotSlangService {
    
    @Autowired
    private AiClient aiClient;
    
    // 默认热门词语（作为降级方案）
    private static final List<String> DEFAULT_HOT_SLANGS = Arrays.asList(
        "yyds", "破防", "内卷", "社死", "emo", "摆烂", "躺平", "打工人",
        "内耗", "摆烂", "躺平", "打工人", "社畜", "摸鱼", "卷王", "摆子",
        "破圈", "出圈", "翻车", "翻盘", "真香", "打脸", "真香", "上头"
    );
    
    /**
     * 获取热门词语列表
     * 每次调用都重新生成，确保返回最新、不同的词语
     * 
     * @return 热门词语列表（8个）
     */
    public List<String> getTodayHotSlangs() {
        // 每次都重新生成，确保返回最新、不同的词语
        return updateHotSlangs();
    }
    
    /**
     * 更新热门词语列表（支持强制刷新）
     * 
     * @param forceRefresh 是否强制刷新（忽略缓存，已废弃，保留兼容性）
     * @return 热门词语列表
     */
    public List<String> updateHotSlangs(boolean forceRefresh) {
        return updateHotSlangs();
    }
    
    /**
     * 更新热门词语列表
     * 每次调用都重新生成，确保返回最新、不同的词语
     * 
     * @return 热门词语列表
     */
    private List<String> updateHotSlangs() {
        // 策略1：使用AI生成热门词语（主要方式）
        List<String> aiWords = generateHotWordsFromAI();
        if (aiWords != null && !aiWords.isEmpty() && aiWords.size() >= 4) {
            log.info("从AI获取热门词语成功: {}", aiWords);
            return aiWords;
        }
        
        // 策略2：使用默认词语（降级方案），每次随机打乱
        List<String> defaultWords = new ArrayList<>(DEFAULT_HOT_SLANGS);
        Collections.shuffle(defaultWords);
        List<String> result = defaultWords.subList(0, Math.min(8, defaultWords.size()));
        log.warn("AI生成失败，使用随机默认热门词语: {}", result);
        return result;
    }
    
    /**
     * 使用AI生成热门词语
     * 每次调用都会生成不同的、最新的热门词语
     */
    private List<String> generateHotWordsFromAI() {
        try {
            // 添加时间戳和随机性提示，确保每次返回不同的词语
            String currentTime = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
            );
            
            String prompt = String.format(
                "请推荐8个当前（%s）最热门的中国互联网网络用语/黑话，要求：\n" +
                "1. 只返回词语本身，每行一个，不要编号，不要说明，不要引号\n" +
                "2. 每个词语2-6个字符\n" +
                "3. 返回格式：每行一个词语，共8行\n" +
                "4. 只返回词语，不要其他文字、标点符号或说明\n" +
                "5. 词语要真实存在且当前最新流行\n" +
                "6. 尽量选择与之前推荐不同的词语，增加多样性\n" +
                "7. 优先选择最近新出现的网络热词\n\n" +
                "请直接返回8个词语，每行一个，不要任何其他内容。",
                currentTime
            );
            
            // 使用AI客户端调用
            String response = aiClient.generateText(prompt);
            
            if (response != null && !response.trim().isEmpty()) {
                List<String> words = parseAIResponse(response);
                if (words.size() >= 8) {
                    return words.subList(0, 8);
                } else if (words.size() >= 4) {
                    // 如果不够8个但至少有4个，补充默认词语
                    List<String> result = new ArrayList<>(words);
                    for (String defaultWord : DEFAULT_HOT_SLANGS) {
                        if (!result.contains(defaultWord) && result.size() < 8) {
                            result.add(defaultWord);
                        }
                    }
                    Collections.shuffle(result);
                    return result.subList(0, Math.min(8, result.size()));
                } else if (!words.isEmpty()) {
                    // 如果只有少量词语，直接返回
                    return words;
                }
            }
        } catch (Exception e) {
            log.warn("使用AI生成热门词语失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 解析AI返回的文本，提取词语列表
     */
    private List<String> parseAIResponse(String response) {
        List<String> words = new ArrayList<>();
        
        try {
            String[] lines = response.split("\n");
            for (String line : lines) {
                String word = line.trim();
                
                // 跳过空行
                if (word.isEmpty()) {
                    continue;
                }
                
                // 移除可能的编号（如 "1. ", "1、"等）
                word = word.replaceAll("^\\d+[.、]\\s*", "");
                
                // 移除可能的引号、括号等
                word = word.replaceAll("^[\"'`（(]|[\"'`）)]$", "");
                
                // 移除可能的说明文字（如 "词语："、"推荐："等）
                if (word.contains("：") || word.contains(":")) {
                    String[] parts = word.split("[：:]");
                    if (parts.length > 1) {
                        word = parts[parts.length - 1].trim();
                    }
                }
                
                // 验证词语有效性（长度2-8个字符，不包含特殊字符）
                if (word.length() >= 2 && word.length() <= 8 
                    && !word.contains("http") && !word.contains("www")
                    && !word.matches(".*\\d{3,}.*")) {
                    if (!words.contains(word)) {
                        words.add(word);
                    }
                }
            }
            
            log.info("从AI响应中解析出 {} 个词语: {}", words.size(), words);
        } catch (Exception e) {
            log.warn("解析AI响应失败: {}", e.getMessage());
        }
        
        return words;
    }
}

