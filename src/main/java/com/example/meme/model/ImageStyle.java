package com.example.meme.model;

/**
 * 图片生成风格枚举
 */
public enum ImageStyle {
    CHIBI("chibi", "大头小人", "可爱的Q版大头小人风格，头身比约3:1，适合制作聊天表情包");
    
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
        // 只支持 CHIBI 风格
        return CHIBI;
    }
}

