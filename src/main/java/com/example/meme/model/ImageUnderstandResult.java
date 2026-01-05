package com.example.meme.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 图片理解结果模型
 * 包含图片描述和生成的表情包文案
 */
public class ImageUnderstandResult {
    
    /**
     * 图片内容描述
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * 生成的表情包文案
     */
    @JsonProperty("text")
    private String text;
    
    public ImageUnderstandResult() {
    }
    
    public ImageUnderstandResult(String description, String text) {
        this.description = description;
        this.text = text;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return "ImageUnderstandResult{" +
                "description='" + description + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}

