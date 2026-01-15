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
        return generateEmotionImage(imageBase64, emotionType, com.example.meme.model.ImageStyle.ORIGINAL);
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
    private String buildImagePrompt(String originalDescription, EmotionType emotionType, com.example.meme.model.ImageStyle style) {
        String styleDescription = getStyleDescription(style);
        
        if (style == com.example.meme.model.ImageStyle.ORIGINAL) {
            // 原样风格：保持原图风格
            return String.format(
                "一个表情包风格的图片，基于以下描述：%s。要求：1. 表现出%s的情绪（%s）2. 表情夸张、生动 3. 适合作为表情包使用 4. 简洁的背景 5. 高质量、清晰的图像 6. 保持原图的风格特点",
                originalDescription,
                emotionType.getChineseName(),
                emotionType.getDescription()
            );
        } else {
            // 其他风格：应用指定风格
            return String.format(
                "一个表情包风格的图片，基于以下描述：%s。要求：1. 表现出%s的情绪（%s）2. 表情夸张、生动 3. 适合作为表情包使用 4. 简洁的背景 5. 高质量、清晰的图像 6. 必须使用%s风格：%s",
                originalDescription,
                emotionType.getChineseName(),
                emotionType.getDescription(),
                style.getName(),
                styleDescription
            );
        }
    }
    
    /**
     * 获取风格描述
     */
    private String getStyleDescription(com.example.meme.model.ImageStyle style) {
        switch (style) {
            case CARTOON:
                return "卡通风格，色彩鲜艳明快，线条简洁流畅，造型可爱夸张，适合轻松愉快的场景，类似迪士尼或日式动漫风格";
            case PIXEL:
                return "像素艺术风格，8-bit或16-bit复古游戏风格，低分辨率像素化效果，色彩有限但鲜明，具有强烈的复古游戏感";
            case TOUGH:
                return "硬汉风格，硬朗粗犷的线条，强烈的明暗对比，肌肉感强，适合表现力量感和男性化特征，类似美式漫画或硬派插画风格";
            case REALISTIC:
                return "写实主义风格，细节丰富逼真，真实感强，接近照片效果，光影自然，质感真实";
            default:
                return "";
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
