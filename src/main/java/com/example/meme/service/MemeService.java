package com.example.meme.service;

import com.example.meme.model.EmotionType;
import com.example.meme.model.FilterType;
import com.example.meme.model.ImageUnderstandResult;
import com.example.meme.model.TextStyle;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 表情包生成服务
 * 核心业务逻辑：协调 AI 服务和图片生成服务
 */
@Slf4j
@Service
public class MemeService {
    
    @Autowired
    private AiService aiService;
    
    @Autowired
    private ImageComposeService imageComposeService;
    
    @Autowired
    private ImageGenerateService imageGenerateService;
    
    @Autowired
    private FilterService filterService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 生成情绪表情图片（支持样式和滤镜）
     * 
     * @param imageFile 上传的图片文件
     * @param emotionType 情绪类型
     * @param customText 自定义文字（可选）
     * @param textStyleJson 文字样式JSON（可选）
     * @param filterType 滤镜类型（可选）
     * @return 生成的表情包图片 URL
     * @throws IOException 文件处理异常
     */
    public String generateEmotionImage(
            MultipartFile imageFile, 
            EmotionType emotionType, 
            String customText,
            String textStyleJson,
            FilterType filterType) throws IOException {
        // 1. 验证文件
        validateImageFile(imageFile);
        
        // 2. 读取图片字节
        byte[] imageBytes = imageFile.getBytes();
        
        // 3. 调用 AI 生成情绪表情图片
        String imageUrl = imageGenerateService.generateEmotionImage(imageBytes, emotionType);
        
        log.info("AI 生成的{}表情图片: {}", emotionType.getChineseName(), imageUrl);
        
        // 4. 下载生成的图片（如果是 OSS URL）
        byte[] generatedImageBytes = downloadImage(imageUrl);
        
        // 5. 应用滤镜（如果需要）
        if (filterType != null && filterType != FilterType.NONE) {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(generatedImageBytes));
            BufferedImage filteredImage = filterService.applyFilter(image, filterType);
            
            // 将处理后的图片转换为字节数组
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(filteredImage, "png", baos);
            generatedImageBytes = baos.toByteArray();
            log.info("已应用滤镜: {}", filterType.getName());
        }
        
        // 6. 如果提供了自定义文字，将文字绘制到生成的图片上
        if (customText != null && !customText.trim().isEmpty()) {
            // 解析文字样式
            TextStyle textStyle = parseTextStyle(textStyleJson);
            
            // 将文字绘制到图片上
            imageUrl = imageComposeService.composeImage(generatedImageBytes, customText.trim(), textStyle);
            log.info("已添加自定义文字: {}", customText);
        }
        
        return imageUrl;
    }
    
    /**
     * 生成情绪表情图片（兼容旧版本）
     */
    public String generateEmotionImage(MultipartFile imageFile, EmotionType emotionType, String customText) throws IOException {
        return generateEmotionImage(imageFile, emotionType, customText, null, null);
    }
    
    /**
     * 解析文字样式JSON
     */
    private TextStyle parseTextStyle(String textStyleJson) {
        if (textStyleJson == null || textStyleJson.trim().isEmpty()) {
            return new TextStyle(); // 返回默认样式
        }
        
        try {
            return objectMapper.readValue(textStyleJson, TextStyle.class);
        } catch (Exception e) {
            log.warn("解析文字样式失败，使用默认样式: {}", e.getMessage());
            return new TextStyle();
        }
    }
    
    /**
     * 下载图片（支持 HTTP/HTTPS URL 和本地路径）
     * 
     * @param imageUrl 图片 URL
     * @return 图片字节数组
     * @throws IOException 下载失败
     */
    private byte[] downloadImage(String imageUrl) throws IOException {
        // 如果是 HTTP/HTTPS URL，需要下载
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            try {
                java.net.URL url = new java.net.URL(imageUrl);
                try (java.io.InputStream in = url.openStream()) {
                    return in.readAllBytes();
                }
            } catch (Exception e) {
                throw new IOException("下载图片失败: " + e.getMessage(), e);
            }
        }
        
        // 如果是本地路径，直接读取
        if (imageUrl.startsWith("/output/")) {
            String fileName = imageUrl.substring("/output/".length());
            String projectRoot = System.getProperty("user.dir");
            java.nio.file.Path filePath = java.nio.file.Paths.get(projectRoot, "output", fileName);
            return java.nio.file.Files.readAllBytes(filePath);
        }
        
        throw new IOException("不支持的图片 URL 格式: " + imageUrl);
    }
    
    /**
     * 生成表情包（旧方法，保留兼容性）
     * 
     * @param imageFile 上传的图片文件
     * @return 生成的表情包图片 URL
     * @throws IOException 文件处理异常
     */
    @Deprecated
    public String generateMeme(MultipartFile imageFile) throws IOException {
        // 1. 验证文件
        validateImageFile(imageFile);
        
        // 2. 读取图片字节
        byte[] imageBytes = imageFile.getBytes();
        
        // 3. 调用 AI 服务生成文案
        ImageUnderstandResult result = aiService.generateMemeText(imageBytes);
        String memeText = result.getText();
        
        log.info("AI 生成的文案: {}", memeText);
        
        // 4. 将文案绘制到图片上
        String imageUrl = imageComposeService.composeImage(imageBytes, memeText);
        
        return imageUrl;
    }
    
    /**
     * 验证图片文件
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("图片文件不能为空");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || 
            (!contentType.startsWith("image/jpeg") && 
             !contentType.startsWith("image/jpg") && 
             !contentType.startsWith("image/png"))) {
            throw new IllegalArgumentException("只支持 JPG 和 PNG 格式的图片");
        }
        
        long size = file.getSize();
        if (size > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException("图片大小不能超过 10MB");
        }
    }
}

