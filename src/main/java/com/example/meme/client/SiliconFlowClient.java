package com.example.meme.client;

import com.example.meme.model.EmotionType;
import com.example.meme.model.ImageGenerateResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SiliconFlow AI 客户端
 * 调用 SiliconFlow 的图像生成 API
 */
@Slf4j
@Component
public class SiliconFlowClient {
    
    @Value("${siliconflow.api-key:}")
    private String apiKey;
    
    @Value("${siliconflow.api-url:https://api.siliconflow.cn/v1/images/generations}")
    private String apiUrl;
    
    @Value("${siliconflow.model:Kwai-Kolors/Kolors}")
    private String model;
    
    @Value("${siliconflow.image-size:1024x1024}")
    private String imageSize;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public SiliconFlowClient() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 检查是否已配置 SiliconFlow
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
    
    /**
     * 生成图像
     * 
     * @param prompt 提示词
     * @param emotionType 情绪类型
     * @return 生成的图片结果
     */
    public ImageGenerateResult generateImage(String prompt, EmotionType emotionType) throws Exception {
        return generateImageWithImages(prompt, emotionType, null, null, null);
    }
    
    /**
     * 生成图像（支持上传图片）
     * 根据 SiliconFlow API 文档：https://docs.siliconflow.cn/cn/api-reference/images/images-generations
     * 
     * @param prompt 提示词
     * @param emotionType 情绪类型
     * @param image 第一张图片，支持 base64 格式（data:image/png;base64, XXX）或 URL
     * @param image2 第二张图片（可选），仅适用于 Qwen/Qwen-Image-Edit-2509
     * @param image3 第三张图片（可选），仅适用于 Qwen/Qwen-Image-Edit-2509
     * @return 生成的图片结果
     */
    public ImageGenerateResult generateImageWithImages(String prompt, EmotionType emotionType, 
                                                       String image, String image2, String image3) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("SiliconFlow API Key 未配置");
        }
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("image_size", imageSize);
        requestBody.put("batch_size", 1);
        requestBody.put("num_inference_steps", 25);
        requestBody.put("guidance_scale", 7.5);
        requestBody.put("seed", 42);  // 固定seed保持稳定性
        
        // 添加图片参数（如果提供）
        if (image != null && !image.trim().isEmpty()) {
            requestBody.put("image", image);
            log.debug("添加 image 参数: {}", image.length() > 100 ? image.substring(0, 100) + "..." : image);
        }
        
        if (image2 != null && !image2.trim().isEmpty()) {
            requestBody.put("image2", image2);
            log.debug("添加 image2 参数: {}", image2.length() > 100 ? image2.substring(0, 100) + "..." : image2);
        }
        
        if (image3 != null && !image3.trim().isEmpty()) {
            requestBody.put("image3", image3);
            log.debug("添加 image3 参数: {}", image3.length() > 100 ? image3.substring(0, 100) + "..." : image3);
        }
        
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        
        log.info("调用 SiliconFlow API: {}, model: {}", apiUrl, model);
        
        // 发送请求
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBodyJson)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                String errorMsg = String.format("SiliconFlow API 调用失败: status=%d, body=%s", 
                                        response.statusCode().value(), body);
                                log.error(errorMsg);
                                return Mono.error(new Exception(errorMsg));
                            });
                })
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(60))
                .block();
        
        if (responseMap == null) {
            throw new Exception("SiliconFlow API 返回的响应为空");
        }
        
        // 获取图片URL
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> images = (List<Map<String, Object>>) responseMap.get("images");
        
        if (images == null || images.isEmpty()) {
            throw new Exception("SiliconFlow API 返回的图片列表为空");
        }
        
        String imageUrl = (String) images.get(0).get("url");
        
        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new Exception("SiliconFlow API 返回的图片URL为空");
        }
        
        log.info("SiliconFlow 图片生成成功: {}", imageUrl);
        
        return new ImageGenerateResult(imageUrl, prompt, emotionType.getChineseName());
    }
    
    /**
     * 生成图像（支持上传单张图片）
     * 
     * @param prompt 提示词
     * @param emotionType 情绪类型
     * @param image 图片，支持 base64 格式（data:image/png;base64, XXX）或 URL
     * @return 生成的图片结果
     */
    public ImageGenerateResult generateImageWithImage(String prompt, EmotionType emotionType, String image) throws Exception {
        return generateImageWithImages(prompt, emotionType, image, null, null);
    }
    
    /**
     * 生成图像（支持上传图片字节数组）
     * 自动将字节数组转换为 base64 格式
     * 
     * @param prompt 提示词
     * @param emotionType 情绪类型
     * @param imageBytes 图片字节数组
     * @return 生成的图片结果
     */
    public ImageGenerateResult generateImageWithImageBytes(String prompt, EmotionType emotionType, byte[] imageBytes) throws Exception {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("图片字节数组不能为空");
        }
        
        // 将字节数组转换为 base64 格式（假设是 PNG 格式）
        String imageBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        return generateImageWithImages(prompt, emotionType, imageBase64, null, null);
    }
    
    /**
     * 将 base64 字符串转换为 API 所需的格式
     * 如果已经是 data:image 格式，直接返回；否则添加前缀
     * 
     * @param base64String base64 字符串
     * @param imageType 图片类型，如 "png", "jpg", "jpeg" 等，默认为 "png"
     * @return 格式化后的 base64 字符串
     */
    public String formatImageBase64(String base64String, String imageType) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return null;
        }
        
        // 如果已经是 data:image 格式，直接返回
        if (base64String.startsWith("data:image")) {
            return base64String;
        }
        
        // 否则添加前缀
        String type = (imageType != null && !imageType.trim().isEmpty()) ? imageType : "png";
        return "data:image/" + type + ";base64," + base64String;
    }
}
