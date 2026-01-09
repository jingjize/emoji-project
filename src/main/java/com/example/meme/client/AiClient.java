package com.example.meme.client;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.example.meme.model.EmotionType;
import com.example.meme.model.ImageGenerateResult;
import com.example.meme.model.ImageUnderstandResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI 客户端
 * 使用阿里云 DashScope SDK 调用百炼 API
 */
@Slf4j
@Component
public class AiClient {
    
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.dashscope.chat.options.model:qwen-vl-plus}")
    private String chatModel;
    
    @Value("${spring.ai.dashscope.image.options.model:qwen-image-plus}")
    private String imageModel;
    
    @Value("${spring.ai.dashscope.image.options.models:qwen-image-plus,qwen-image-max,qwen-image-max-2025-12-30}")
    private String imageModels;
    
    @Value("${spring.ai.dashscope.image.options.size:1328*1328}")
    private String imageSize;
    
    private final ImageSynthesis imageSynthesis;
    private final MultiModalConversation multiModalConversation;
    private final ObjectMapper objectMapper;
    
    public AiClient() {
        this.imageSynthesis = new ImageSynthesis();
        this.multiModalConversation = new MultiModalConversation();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 理解图片内容并生成描述
     * 使用多模态对话 API 支持图片理解
     * 
     * @param imageBase64 图片的 Base64 编码
     * @return 图片理解结果，包含描述
     */
    public ImageUnderstandResult understandImage(String imageBase64) {
        try {
            // 构建提示词
            String promptText = "请仔细观察这张图片，理解图片的内容和情绪。请以 JSON 格式返回，格式：{\"description\": \"图片描述\", \"text\": \"简短描述\"}";
            
            // 构建多模态消息（图片 + 文本）
            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(Arrays.asList(
                            Collections.singletonMap("image", "data:image/jpeg;base64," + imageBase64),
                            Collections.singletonMap("text", promptText)
                    ))
                    .build();
            
            // 调用多模态对话 API
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model(chatModel)  // 使用配置的模型，如 qwen-vl-plus
                    .messages(Arrays.asList(userMessage))
                    .build();
            
            MultiModalConversationResult result = multiModalConversation.call(param);
            
            if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null
                    && !result.getOutput().getChoices().isEmpty()) {
                //获取返回的内容
                List<Map<String, Object>> content = result.getOutput().getChoices().get(0).getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    // 查找文本内容
                    for (Map<String, Object> item : content) {
                        if (item.containsKey("text")) {
                            String textContent = item.get("text").toString();
                            return parseResponse(textContent);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("AI API 调用失败，使用模拟数据", e);
        }
        
        return getMockResult();
    }
    
    /**
     * 解析 API 响应
     */
    private ImageUnderstandResult parseResponse(String response) {
        try {
            // 尝试从 JSON 字符串中提取
            if (response.contains("{")) {
                int start = response.indexOf("{");
                int end = response.lastIndexOf("}") + 1;
                String jsonStr = response.substring(start, end);
                return objectMapper.readValue(jsonStr, ImageUnderstandResult.class);
            }
        } catch (Exception e) {
            log.warn("解析 AI 响应失败: {}", e.getMessage());
        }
        
        // 解析失败时返回模拟数据
        return getMockResult();
    }
    
    /**
     * 获取模拟结果（用于测试或 API 不可用时）
     */
    private ImageUnderstandResult getMockResult() {
        // 随机返回一些示例文案
        String[] mockTexts = {
            "一个表情丰富的图片",
            "一张有趣的图片",
            "一个生动的表情",
            "一张搞笑的图片"
        };
        String randomText = mockTexts[(int) (Math.random() * mockTexts.length)];
        return new ImageUnderstandResult(randomText, "描述");
    }
    
    /**
     * 根据原图和情绪类型生成表情图片
     * 支持多模型自动切换，当某个模型失败时自动尝试下一个
     * 
     * @param imageBase64 原图的 Base64 编码
     * @param emotionType 情绪类型
     * @return 生成的图片结果
     */
    public ImageGenerateResult generateEmotionImage(String imageBase64, EmotionType emotionType) {
        // 先理解原图内容
        ImageUnderstandResult understandResult = understandImage(imageBase64);
        String description = understandResult.getDescription();
        
        // 构建图像生成提示词
        String prompt = buildImagePrompt(description, emotionType);
        
        // 获取模型列表
        List<String> models = getImageModels();
        
        // 尝试每个模型，直到成功
        Exception lastException = null;
        for (String model : models) {
            try {
                log.info("尝试使用模型: {}", model);
                
                ImageGenerateResult result = tryGenerateWithModel(model, prompt, description, emotionType);
                
                if (result != null && result.getImageUrl() != null && !result.getImageUrl().isEmpty()) {
                    log.info("模型 {} 生成成功", model);
                    return result;
                }
                
            } catch (Exception e) {
                lastException = e;
                String errorMsg = e.getMessage();
                
                // 判断是否是额度不足或配额错误
                if (isQuotaExceeded(errorMsg)) {
                    log.warn("模型 {} 额度不足，尝试下一个模型: {}", model, errorMsg);
                } else {
                    log.warn("模型 {} 调用失败，尝试下一个模型: {}", model, errorMsg);
                }
                
                // 继续尝试下一个模型
            }
        }
        
        // 所有模型都失败
        log.error("所有模型都调用失败，使用模拟数据", lastException);
        
        return getMockImageResult(emotionType);
    }
    
    /**
     * 使用指定模型尝试生成图片
     */
    private ImageGenerateResult tryGenerateWithModel(String model, String prompt, String description, EmotionType emotionType) throws Exception {
        // 调用 DashScope ImageSynthesis API
        ImageSynthesisParam param = ImageSynthesisParam.builder()
                .apiKey(apiKey)
                .model(model)
                .prompt(prompt)
                .n(1)
                .size(imageSize)  // 使用配置的尺寸，qwen-image 支持：1664*928, 1472*1140, 1328*1328, 1140*1472, 928*1664
                .build();
        
        var result = imageSynthesis.call(param);
        
        if (result != null && result.getOutput() != null && result.getOutput().getResults() != null
                && !result.getOutput().getResults().isEmpty()) {
            // 获取生成的图片数据（可能是 Map 或对象）
            var imageData = result.getOutput().getResults().get(0);
            String imageUrl = null;
            
            // 尝试从 Map 中获取 URL
            if (imageData instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) (java.util.Map<?, ?>) imageData;
                Object urlObj = map.get("url");
                Object b64Obj = map.get("b64_encoded");
                
                if (urlObj != null && !urlObj.toString().isEmpty()) {
                    imageUrl = urlObj.toString();
                } else if (b64Obj != null && !b64Obj.toString().isEmpty()) {
                    imageUrl = "data:image/png;base64," + b64Obj.toString();
                }
            } else {
                // 如果是对象，尝试反射获取
                try {
                    java.lang.reflect.Method getUrlMethod = imageData.getClass().getMethod("getUrl");
                    Object urlObj = getUrlMethod.invoke(imageData);
                    if (urlObj != null) {
                        imageUrl = urlObj.toString();
                    }
                } catch (Exception e) {
                    // 忽略反射错误
                }
            }
            
            if (imageUrl != null) {
                return new ImageGenerateResult(imageUrl, description, emotionType.getChineseName());
            } else {
                throw new Exception("模型 " + model + " 返回的图片数据为空");
            }
        } else {
            throw new Exception("模型 " + model + " 返回结果为空");
        }
    }
    
    /**
     * 获取图片生成模型列表
     */
    private List<String> getImageModels() {
        if (imageModels != null && !imageModels.trim().isEmpty()) {
            // 解析逗号分隔的模型列表
            return Arrays.asList(imageModels.split(","));
        } else {
            // 如果没有配置多个模型，使用默认模型
            return Arrays.asList(imageModel);
        }
    }
    
    /**
     * 判断错误是否是额度不足
     */
    private boolean isQuotaExceeded(String errorMsg) {
        if (errorMsg == null) {
            return false;
        }
        String lowerMsg = errorMsg.toLowerCase();
        return lowerMsg.contains("quota") || 
               lowerMsg.contains("额度") || 
               lowerMsg.contains("limit") || 
               lowerMsg.contains("exceeded") ||
               lowerMsg.contains("insufficient") ||
               lowerMsg.contains("余额不足") ||
               lowerMsg.contains("配额");
    }
    
    /**
     * 构建图像生成提示词
     */
    private String buildImagePrompt(String originalDescription, EmotionType emotionType) {
        return String.format(
            "一个表情包风格的图片，基于以下描述：%s。要求：1. 表现出%s的情绪（%s）2. 表情夸张、生动 3. 适合作为表情包使用 4. 简洁的背景 5. 高质量、清晰的图像",
            originalDescription,
            emotionType.getChineseName(),
            emotionType.getDescription()
        );
    }
    
    /**
     * 获取模拟图像结果（用于测试）
     */
    private ImageGenerateResult getMockImageResult(EmotionType emotionType) {
        // 返回一个占位符，实际项目中可以返回一个默认图片的 Base64
        return new ImageGenerateResult(
            null, // 实际项目中可以返回一个默认表情图片
            "模拟生成的" + emotionType.getChineseName() + "表情",
            emotionType.getChineseName()
        );
    }
    
    /**
     * 将中文关键词翻译成英文（用于图片搜索）
     * 使用 DashScope 多模态对话 API 进行翻译
     * 重要：Pixabay API 只支持英文搜索关键词，必须将中文翻译为英文
     * 
     * @param chineseText 中文文本
     * @return 英文翻译结果，如果翻译失败则返回原文本（调用方需要检查）
     */
    public String translateToEnglish(String chineseText) {
        if (chineseText == null || chineseText.trim().isEmpty()) {
            return chineseText;
        }
        
        String trimmedText = chineseText.trim();
        
        // 简单判断是否包含中文字符
        if (!containsChinese(trimmedText)) {
            return trimmedText; // 不包含中文，直接返回
        }
        
        try {
            // 构建翻译提示词，强调只返回英文关键词，用于图片搜索
            String promptText = String.format(
                "请将以下中文关键词翻译成英文，用于图片搜索。要求：\n" +
                "1. 只返回英文翻译结果，不要包含任何其他文字、说明或标点符号\n" +
                "2. 如果是多个词，用空格分隔，不要用逗号\n" +
                "3. 如果输入已经是英文，直接返回原文本\n" +
                "4. 翻译要准确，适合作为图片搜索关键词\n\n" +
                "关键词：%s",
                trimmedText
            );
            
            // 构建文本消息
            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(Arrays.asList(
                            Collections.singletonMap("text", promptText)
                    ))
                    .build();
            
            // 调用多模态对话 API
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model(chatModel)
                    .messages(Arrays.asList(userMessage))
                    .build();
            
            MultiModalConversationResult result = multiModalConversation.call(param);
            
            if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null
                    && !result.getOutput().getChoices().isEmpty()) {
                List<Map<String, Object>> content = result.getOutput().getChoices().get(0).getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    for (Map<String, Object> item : content) {
                        if (item.containsKey("text")) {
                            String translatedText = item.get("text").toString().trim();
                            
                            // 清理可能的额外说明文字（如果 AI 返回了说明）
                            // 只保留第一行或第一个单词/短语
                            if (translatedText.contains("\n")) {
                                translatedText = translatedText.split("\n")[0].trim();
                            }
                            // 移除可能的引号
                            translatedText = translatedText.replaceAll("^[\"']|[\"']$", "");
                            
                            log.info("翻译结果: {} -> {}", trimmedText, translatedText);
                            return translatedText;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("翻译失败: {} - {}", trimmedText, e.getMessage(), e);
        }
        
        // 翻译失败时返回原文本（调用方需要检查是否仍包含中文）
        return trimmedText;
    }
    
    /**
     * 判断文本是否包含中文字符
     */
    private boolean containsChinese(String text) {
        if (text == null) {
            return false;
        }
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                return true; // 中文字符范围
            }
        }
        return false;
    }
}
