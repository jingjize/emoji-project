package com.example.meme.client;

import com.alibaba.dashscope.aigc.imagegeneration.ImageGeneration;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationParam;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationResult;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationMessage;
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
    
    private final ImageGeneration imageGeneration;
    private final MultiModalConversation multiModalConversation;
    private final ObjectMapper objectMapper;
    
    public AiClient() {
        this.imageGeneration = new ImageGeneration();
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
            // 构建提示词（直接要求英文描述，限制400字符，参考特定格式）
            String promptText = "Describe this image in English following this format example: " +
                    "'Young anime female. Long black hair with pink gradient. Large blue eyes, blush. " +
                    "Same face, hairstyle, hair color, eye color. Visible upper body keeps original outfit: " +
                    "black T-shirt with smiley faces. Simplified, recognizable, no new outfit.' " +
                    "\n\nFocus on: " +
                    "1. Character basic info (age, gender, anime style) " +
                    "2. Hair (style, color, any gradients or highlights) " +
                    "3. Eyes (color, features) " +
                    "4. Expression/emotion (blush, smile, etc.) " +
                    "5. Upper body clothing ONLY (type, color, patterns) " +
                    "\nDO NOT describe: background, full body, arms, hands, legs, lower body. " +
                    "IMPORTANT: Total under 400 characters. Use short sentences. " +
                    "Return JSON: {\"description\": \"English description (max 400 chars)\", \"text\": \"Short summary\"}";
            
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
                    .model(chatModel)  // 使用配置的模型,如 qwen-vl-plus
                    .temperature(0.1f)  // 降低温度值保持输出稳定
                    .seed(1)  // 固定seed保持描述稳定
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
        return generateEmotionImage(imageBase64, emotionType, com.example.meme.model.ImageStyle.CHIBI);
    }
    
    public ImageGenerateResult generateEmotionImage(String imageBase64, EmotionType emotionType, com.example.meme.model.ImageStyle style) {
        // 先理解原图内容
        ImageUnderstandResult understandResult = understandImage(imageBase64);
        String description = understandResult.getDescription();
        
        // 构建图像生成提示词
        String prompt = buildImagePrompt(description, emotionType, style);
        
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
        // 构建消息（新版 API 要求）
        ImageGenerationMessage message = ImageGenerationMessage.builder()
                .role("user")
                .content(Collections.singletonList(
                        Collections.singletonMap("text", prompt)
                )).build();
        
        // 调用 DashScope ImageGeneration API(新版)
        ImageGenerationParam param = ImageGenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .n(1)
                .size(imageSize)  // 使用配置的尺寸
                .seed(1)  // 固定seed保持生成结果稳定
                .messages(Collections.singletonList(message))
                .build();
        
        ImageGenerationResult result = imageGeneration.call(param);
        
        // 新版 SDK API 结构：使用 JsonUtils 解析结果
        if (result != null && result.getOutput() != null) {
            try {
                // 尝试从 Output 中获取图片 URL
                String outputJson = com.alibaba.dashscope.utils.JsonUtils.toJson(result.getOutput());
                log.info("图片生成结果: {}", outputJson);
                
                // 解析 JSON 获取 URL
                @SuppressWarnings("unchecked")
                Map<String, Object> outputMap = objectMapper.readValue(outputJson, Map.class);
                
                String imageUrl = null;
                
                // 尝试多种可能的结构
                
                // 结构1: choices[0].message.content[0].image
                if (outputMap.containsKey("choices")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) outputMap.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        if (choice.containsKey("message")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
                            if (messageObj.containsKey("content")) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> content = (List<Map<String, Object>>) messageObj.get("content");
                                if (content != null && !content.isEmpty()) {
                                    Map<String, Object> contentItem = content.get(0);
                                    if (contentItem.containsKey("image")) {
                                        imageUrl = contentItem.get("image").toString();
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 结构2: results[0].url (旧版兼容)
                if (imageUrl == null && outputMap.containsKey("results")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> results = (List<Map<String, Object>>) outputMap.get("results");
                    if (results != null && !results.isEmpty()) {
                        Map<String, Object> firstResult = results.get(0);
                        if (firstResult.containsKey("url")) {
                            imageUrl = firstResult.get("url").toString();
                        }
                    }
                }
                
                // 结构3: 直接在 output 中
                if (imageUrl == null && outputMap.containsKey("url")) {
                    imageUrl = outputMap.get("url").toString();
                }
                
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    return new ImageGenerateResult(imageUrl, description, emotionType.getChineseName());
                } else {
                    throw new Exception("模型 " + model + " 返回的图片数据为空: " + outputJson);
                }
            } catch (Exception e) {
                log.error("解析图片生成结果失败", e);
                throw new Exception("模型 " + model + " 返回结果解析失败: " + e.getMessage());
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
     * 构建图像生成提示词（只支持 CHIBI 风格）
     * public 方法，供 SiliconFlowClient 调用
     */
    public String buildImagePrompt(String originalDescription, EmotionType emotionType, com.example.meme.model.ImageStyle style) {
        // 只支持 CHIBI 风格
        return buildChibiStylePrompt(originalDescription, emotionType);
    }
    
    /**
     * 构建 CHIBI 风格提示词，使用模板格式并替换占位符
     */
    private String buildChibiStylePrompt(String originalDescription, EmotionType emotionType) {
        // 使用 AI 返回的英文描述（已经是英文，AI已限制在200字符以内）
        String characterDesc = originalDescription != null && !originalDescription.isEmpty() 
            ? originalDescription 
            : "anime character";
        
        // 根据情绪类型生成表情描述
        String expressionDesc = getExpressionDescription(emotionType);
        
        // 使用模板格式构建完整提示词（强化不要背景、不要全身）
        return String.format(
            "Cute anime-style chibi emoji character. " +
            "Big head, tiny upper body (head ~80%%). " +
            "Only head, neck, shoulders, small upper torso visible. " +
            "Bust-up emoji portrait ONLY. " +
            "Character: %s. " +
            "Expression: %s. " +
            "Exaggerated, suitable for emoji. " +
            "Japanese chibi style, clean lines, soft shading, pastel colors. " +
            "High quality, transparent pure white background. " +
            "Negative: full body, legs, lower body, background scene, realistic, 3D, complex background.",
            characterDesc,
            expressionDesc
        );
    }
    
    /**
     * 根据情绪类型生成表情描述（包含面部表情、头部姿势、手部动作）
     */
    private String getExpressionDescription(EmotionType emotionType) {
        switch (emotionType) {
            case HAPPY:
                return "Happy smile, curved eyes, closed mouth. Head upright. Hands forming V-sign near face";
            case SAD:
                return "Big teary eyes, downturned brows, trembling mouth, tears. Head slightly down. Hands wiping tears";
            case ANGRY:
                return "Puffed cheeks, furrowed brows, pouting mouth. Head tilted forward. Fists clenched near cheeks";
            case SURPRISED:
                return "Wide eyes, open O-mouth, raised brows. Head slightly back. Hands on cheeks";
            case CONFUSED:
                return "Tilted head, squinting eye, puzzled look. Head tilted to side. Hand near chin, thinking pose";
            case EXCITED:
                return "Sparkling eyes with stars, wide smile. Head upright. Hands raised in cheer";
            case CALM:
                return "Peaceful eyes, serene smile, relaxed. Head upright. Hands in relaxed pose";
            case SHY:
                return "Blushing cheeks, looking away, shy smile. Head turned slightly. Hands covering mouth";
            case PLAYFUL:
                return "Winking eye, tongue out, cheeky smile. Head tilted playfully. Peace sign or pointing finger";
            default:
                return "Happy smile, curved eyes, closed mouth. Head upright. Hands forming V-sign near face";
        }
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
    
    /**
     * 解释互联网黑话/隐喻词语
     * 使用 AI 分析词语的含义、来源、使用场景等
     * 返回简短解释和详细说明
     * 
     * @param word 需要解释的词语
     * @return 解释结果，包含简短解释和详细说明
     */
    public SlangExplanation explainInternetSlang(String word) {
        if (word == null || word.trim().isEmpty()) {
            return new SlangExplanation("请输入需要解释的词语", "");
        }
        
        String trimmedWord = word.trim();
        
        try {
            // 构建解释提示词，要求返回简短解释和详细说明
            String promptText = String.format(
                "请解释以下互联网黑话/网络用语的含义。要求：\n" +
                "1. 首先用一句话（不超过30字）简洁地解释词语的基本含义\n" +
                "2. 然后提供详细说明，包括：\n" +
                "   - 词语的来源或出处（如果知道）\n" +
                "   - 使用场景和语境\n" +
                "   - 1-2个使用示例\n" +
                "   - 如果该词语有多种含义，请分别说明\n" +
                "3. 用通俗易懂的语言解释，适合普通用户理解\n" +
                "4. 如果这不是一个网络用语，请说明这是一个普通词语的含义\n\n" +
                "请用以下格式回答：\n" +
                "【简短解释】一句话解释（不超过30字）\n" +
                "【详细说明】详细解释内容\n\n" +
                "词语：%s",
                trimmedWord
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
                            String fullExplanation = item.get("text").toString().trim();
                            return parseExplanation(fullExplanation);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("解释词语失败: {} - {}", trimmedWord, e.getMessage(), e);
        }
        
        // 如果AI调用失败，返回默认提示
        return new SlangExplanation("抱歉，暂时无法解释该词语，请稍后重试。", "");
    }
    
    /**
     * 解析AI返回的解释文本，提取简短解释和详细说明
     */
    private SlangExplanation parseExplanation(String fullText) {
        String shortExplanation = "";
        String detailedExplanation = "";
        
        // 尝试解析格式化的回答
        if (fullText.contains("【简短解释】") || fullText.contains("简短解释")) {
            String[] parts = fullText.split("【详细说明】|详细说明");
            if (parts.length >= 1) {
                String shortPart = parts[0];
                // 提取简短解释
                if (shortPart.contains("【简短解释】")) {
                    shortExplanation = shortPart.split("【简短解释】")[1].trim();
                } else if (shortPart.contains("简短解释")) {
                    shortExplanation = shortPart.split("简短解释")[1].trim();
                } else {
                    // 如果没有标记，取第一行作为简短解释
                    String[] lines = shortPart.split("\n");
                    shortExplanation = lines.length > 0 ? lines[0].trim() : shortPart.trim();
                }
                
                // 限制简短解释长度
                if (shortExplanation.length() > 50) {
                    shortExplanation = shortExplanation.substring(0, 47) + "...";
                }
            }
            
            if (parts.length >= 2) {
                detailedExplanation = cleanTextAlignment(parts[1].trim());
            }
        } else {
            // 如果没有格式化，取第一句作为简短解释，其余作为详细说明
            String[] sentences = fullText.split("[。！？\n]");
            if (sentences.length > 0) {
                shortExplanation = sentences[0].trim();
                if (shortExplanation.length() > 50) {
                    shortExplanation = shortExplanation.substring(0, 47) + "...";
                }
                
                if (sentences.length > 1) {
                    detailedExplanation = String.join("。", Arrays.copyOfRange(sentences, 1, sentences.length)).trim();
                }
            } else {
                // 如果只有一段，取前50字作为简短解释
                if (fullText.length() > 50) {
                    shortExplanation = fullText.substring(0, 47) + "...";
                    detailedExplanation = fullText;
                } else {
                    shortExplanation = fullText;
                }
            }
        }
        
        // 如果简短解释为空，使用详细说明的前50字
        if (shortExplanation.isEmpty() && !detailedExplanation.isEmpty()) {
            shortExplanation = detailedExplanation.length() > 50 
                ? detailedExplanation.substring(0, 47) + "..." 
                : detailedExplanation;
        }
        
        // 如果详细说明为空，使用完整文本
        if (detailedExplanation.isEmpty()) {
            detailedExplanation = cleanTextAlignment(fullText);
        } else {
            // 清理详细说明的文本对齐
            detailedExplanation = cleanTextAlignment(detailedExplanation);
        }
        
        return new SlangExplanation(shortExplanation, detailedExplanation);
    }
    
    /**
     * 清理文本对齐，移除每行的前导空格，确保左对齐
     */
    private String cleanTextAlignment(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 按行分割
        String[] lines = text.split("\n");
        StringBuilder cleaned = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim(); // 移除每行的前导和尾随空格
            if (!line.isEmpty()) {
                cleaned.append(line);
                // 如果不是最后一行，添加换行符
                if (i < lines.length - 1) {
                    cleaned.append("\n");
                }
            } else if (i < lines.length - 1) {
                // 保留空行（用于段落分隔）
                cleaned.append("\n");
            }
        }
        
        return cleaned.toString();
    }
    
    /**
     * 解释结果类
     */
    public static class SlangExplanation {
        private final String shortExplanation;
        private final String detailedExplanation;
        
        public SlangExplanation(String shortExplanation, String detailedExplanation) {
            this.shortExplanation = shortExplanation;
            this.detailedExplanation = detailedExplanation;
        }
        
        public String getShortExplanation() {
            return shortExplanation;
        }
        
        public String getDetailedExplanation() {
            return detailedExplanation;
        }
    }
    
    /**
     * 生成文本内容（用于生成热门词语等）
     * 
     * @param prompt 提示词
     * @return 生成的文本
     */
    public String generateText(String prompt) {
        return generateText(prompt, false);
    }
    
    /**
     * 生成文本内容（支持联网搜索）
     * 
     * @param prompt 提示词
     * @param enableWebSearch 是否启用联网搜索
     * @return 生成的文本
     */
    public String generateText(String prompt, boolean enableWebSearch) {
        try {
            // 构建文本消息
            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(Arrays.asList(
                            Collections.singletonMap("text", prompt)
                    ))
                    .build();
            
            // 构建参数
            var paramBuilder = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model(chatModel)
                    .messages(Arrays.asList(userMessage));
            
            // 启用联网搜索功能
            if (enableWebSearch) {
                paramBuilder.enableSearch(true);
            }
            
            MultiModalConversationParam param = paramBuilder.build();
            
            MultiModalConversationResult result = multiModalConversation.call(param);
            
            if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null
                    && !result.getOutput().getChoices().isEmpty()) {
                List<Map<String, Object>> content = result.getOutput().getChoices().get(0).getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    for (Map<String, Object> item : content) {
                        if (item.containsKey("text")) {
                            String text = item.get("text").toString().trim();
                            log.info("AI生成文本: {}", text.substring(0, Math.min(50, text.length())) + "...");
                            return text;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("AI生成文本失败: {}", e.getMessage(), e);
        }
        
        return null;
    }
}
