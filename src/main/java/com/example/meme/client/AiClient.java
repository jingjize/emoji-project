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
@Component
public class AiClient {
    
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.dashscope.chat.options.model:qwen-vl-plus}")
    private String chatModel;
    
    @Value("${spring.ai.dashscope.image.options.model:wanx-v1}")
    private String imageModel;
    
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
            System.err.println("AI API 调用失败，使用模拟数据: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("解析 AI 响应失败: " + e.getMessage());
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
     * 
     * @param imageBase64 原图的 Base64 编码
     * @param emotionType 情绪类型
     * @return 生成的图片结果
     */
    public ImageGenerateResult generateEmotionImage(String imageBase64, EmotionType emotionType) {
        try {
            // 先理解原图内容
            ImageUnderstandResult understandResult = understandImage(imageBase64);
            String description = understandResult.getDescription();
            
            // 构建图像生成提示词
            String prompt = buildImagePrompt(description, emotionType);
            
            // 调用 DashScope ImageSynthesis API
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(apiKey)
                    .model(imageModel)
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
                }
            }
            
        } catch (Exception e) {
            System.err.println("AI 图像生成失败，使用模拟数据: " + e.getMessage());
            e.printStackTrace();
        }
        
        return getMockImageResult(emotionType);
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
}
