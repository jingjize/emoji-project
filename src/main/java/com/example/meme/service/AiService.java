package com.example.meme.service;

import com.example.meme.client.AiClient;
import com.example.meme.model.ImageUnderstandResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * AI 服务层
 * 负责调用 AI 客户端进行图片理解
 */
@Service
public class AiService {
    
    @Autowired
    private AiClient aiClient;
    
    /**
     * 理解图片并生成表情包文案
     * 
     * @param imageBytes 图片字节数组
     * @return 图片理解结果
     */
    public ImageUnderstandResult generateMemeText(byte[] imageBytes) {
        // 将图片转换为 Base64
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        
        // 调用 AI 客户端
        return aiClient.understandImage(imageBase64);
    }
}

