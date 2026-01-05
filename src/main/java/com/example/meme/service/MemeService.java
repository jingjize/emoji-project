package com.example.meme.service;

import com.example.meme.model.EmotionType;
import com.example.meme.model.ImageUnderstandResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 表情包生成服务
 * 核心业务逻辑：协调 AI 服务和图片生成服务
 */
@Service
public class MemeService {
    
    @Autowired
    private AiService aiService;
    
    @Autowired
    private ImageComposeService imageComposeService;
    
    @Autowired
    private ImageGenerateService imageGenerateService;
    
    /**
     * 生成情绪表情图片
     * 
     * @param imageFile 上传的图片文件
     * @param emotionType 情绪类型
     * @return 生成的表情包图片 URL
     * @throws IOException 文件处理异常
     */
    public String generateEmotionImage(MultipartFile imageFile, EmotionType emotionType) throws IOException {
        // 1. 验证文件
        validateImageFile(imageFile);
        
        // 2. 读取图片字节
        byte[] imageBytes = imageFile.getBytes();
        
        // 3. 调用 AI 生成情绪表情图片
        String imageUrl = imageGenerateService.generateEmotionImage(imageBytes, emotionType);
        
        System.out.println("AI 生成的" + emotionType.getChineseName() + "表情图片: " + imageUrl);
        
        return imageUrl;
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
        
        System.out.println("AI 生成的文案: " + memeText);
        
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

