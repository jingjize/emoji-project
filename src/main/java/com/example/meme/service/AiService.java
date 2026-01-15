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
    
    /**
     * 解释互联网黑话/隐喻词语
     * 
     * @param word 需要解释的词语
     * @return 解释结果（包含简短解释和详细说明）
     */
    public com.example.meme.client.AiClient.SlangExplanation explainInternetSlang(String word) {
        return aiClient.explainInternetSlang(word);
    }
}

