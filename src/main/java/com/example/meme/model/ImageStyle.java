package com.example.meme.model;

/**
 * 图片生成风格枚举
 */
public enum ImageStyle {
    CARTOON("cartoon", "卡通风", "卡通风格，色彩鲜艳，线条简洁，适合轻松愉快的场景"),
    PIXEL("pixel", "像素风", "像素艺术风格，复古游戏风格，8-bit或16-bit像素效果"),
    TOUGH("tough", "硬汉风", "硬朗、粗犷的风格，强烈的对比，适合表现力量感和男性化特征"),
    ORIGINAL("original", "原样", "保持原图的风格和特点，不做风格化处理"),
    REALISTIC("realistic", "写实风", "写实主义风格，细节丰富，真实感强，接近照片效果");
    
    private final String code;
    private final String name;
    private final String description;
    
    ImageStyle(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取风格类型
     */
    public static ImageStyle fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return ORIGINAL; // 默认原样
        }
        
        for (ImageStyle style : values()) {
            if (style.code.equalsIgnoreCase(code.trim())) {
                return style;
            }
        }
        
        return ORIGINAL; // 默认原样
    }
}

