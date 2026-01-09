package com.example.meme.model;

import lombok.Data;

/**
 * 图库图片模型
 */
@Data
public class GalleryImage {
    /**
     * 图片ID
     */
    private String id;
    
    /**
     * 图片URL（原始尺寸）
     */
    private String originalUrl;
    
    /**
     * 图片URL（中等尺寸，用于预览）
     */
    private String mediumUrl;
    
    /**
     * 图片URL（小尺寸，用于缩略图）
     */
    private String smallUrl;
    
    /**
     * 图片宽度
     */
    private Integer width;
    
    /**
     * 图片高度
     */
    private Integer height;
    
    /**
     * 摄影师名称
     */
    private String photographer;
    
    /**
     * 图片来源
     */
    private String source;
    
    public GalleryImage() {
    }
    
    public GalleryImage(String id, String originalUrl, String mediumUrl, String smallUrl, 
                       Integer width, Integer height, String photographer, String source) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.mediumUrl = mediumUrl;
        this.smallUrl = smallUrl;
        this.width = width;
        this.height = height;
        this.photographer = photographer;
        this.source = source;
    }
}

