package com.example.meme.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 图像生成结果模型
 */
public class ImageGenerateResult {
    
    /**
     * 生成的图片 URL（Base64 或 URL）
     */
    @JsonProperty("image_url")
    private String imageUrl;
    
    /**
     * 图片描述
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * 情绪类型
     */
    @JsonProperty("emotion")
    private String emotion;
    
    public ImageGenerateResult() {
    }
    
    public ImageGenerateResult(String imageUrl, String description, String emotion) {
        this.imageUrl = imageUrl;
        this.description = description;
        this.emotion = emotion;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getEmotion() {
        return emotion;
    }
    
    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }
}

