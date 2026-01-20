package com.example.meme.service;

import com.example.meme.client.AiClient;
import com.example.meme.client.SiliconFlowClient;
import com.example.meme.model.EmotionType;
import com.example.meme.model.ImageGenerateResult;
import com.example.meme.model.ImageStyle;
import com.example.meme.model.ImageUnderstandResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * 图像生成服务
 * 负责调用 AI 生成情绪表情图片并保存
 * 支持多个 AI 提供商：SiliconFlow（优先）、DashScope（备用）
 */
@Slf4j
@Service
public class ImageGenerateService {
    
    @Autowired
    private AiClient aiClient;
    
    @Autowired
    private SiliconFlowClient siliconFlowClient;
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    /**
     * 生成情绪表情图片
     * 
     * @param imageBytes 原始图片字节数组
     * @param emotionType 情绪类型
     * @return 生成的表情包图片 URL
     */
    public String generateEmotionImage(byte[] imageBytes, EmotionType emotionType) throws IOException {
        return generateEmotionImage(imageBytes, emotionType, ImageStyle.CHIBI);
    }
    
    /**
     * 生成情绪表情图片（支持风格）
     * 优先使用 SiliconFlow，如果未配置或失败则使用 DashScope
     * 
     * @param imageBytes 原始图片字节数组
     * @param emotionType 情绪类型
     * @param style 图片风格
     * @return 生成的表情包图片 URL
     */
    public String generateEmotionImage(byte[] imageBytes, EmotionType emotionType, ImageStyle style) throws IOException {
        // 将图片转换为 Base64
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        
        ImageGenerateResult result = null;
        
        // 优先尝试 SiliconFlow
        if (siliconFlowClient.isConfigured()) {
            try {
                log.info("使用 SiliconFlow 生成图片");
                
                // 先理解原图内容
                ImageUnderstandResult understandResult = aiClient.understandImage(imageBase64);
                String description = understandResult.getDescription();
                
                // 构建图像生成提示词
                String prompt = aiClient.buildImagePrompt(description, emotionType, style);
                
                // 使用上传的图片生成（传入图片字节数组）
                result = siliconFlowClient.generateImageWithImageBytes(prompt, emotionType, imageBytes);
                log.info("SiliconFlow 生成成功（已传入上传的图片）");
            } catch (Exception e) {
                log.warn("SiliconFlow 生成失败，尝试使用 DashScope: {}", e.getMessage());
            }
        }
        
        // 如果 SiliconFlow 未配置或失败，使用 DashScope
        if (result == null) {
            log.info("使用 DashScope 生成图片");
            result = aiClient.generateEmotionImage(imageBase64, emotionType, style);
        }
        
        String imageData = result.getImageUrl();
        if (imageData == null || imageData.isEmpty()) {
            throw new IOException("AI 未能生成图片，请稍后重试");
        }
        
        // 如果是 HTTP/HTTPS URL（如 OSS URL），直接返回，浏览器可以直接显示
        if (imageData.startsWith("http://") || imageData.startsWith("https://")) {
            log.info("返回 OSS URL: {}", imageData);
            return imageData;  // 直接返回 OSS URL，前端可以直接显示
        }
        
        // 处理 Base64 图片数据
        if (imageData.startsWith("data:image")) {
            // 提取 Base64 部分
            String base64Data = imageData.substring(imageData.indexOf(",") + 1);
            byte[] generatedImageBytes = Base64.getDecoder().decode(base64Data);
            
            // 保存图片到本地
            String fileName = "emotion_" + emotionType.getEnglishName() + "_" + System.currentTimeMillis() + ".png";
            
            // 如果 uploadDir 是相对路径，转换为绝对路径（项目根目录）
            Path outputDir;
            if (Paths.get(uploadDir).isAbsolute()) {
                outputDir = Paths.get(uploadDir);
            } else {
                String projectRoot = System.getProperty("user.dir");
                outputDir = Paths.get(projectRoot, uploadDir);
            }
            
            Path outputPath = outputDir.resolve(fileName);
            
            // 确保目录存在
            Files.createDirectories(outputPath.getParent());
            
            // 读取并保存图片
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(generatedImageBytes));
            if (image == null) {
                throw new IOException("无法读取生成的图片数据");
            }
            
            ImageIO.write(image, "png", outputPath.toFile());
            
            // 返回本地访问 URL
            return "/output/" + fileName;
        }
        
        throw new IOException("不支持的图片格式，期望 HTTP URL 或 Base64 格式");
    }
}

