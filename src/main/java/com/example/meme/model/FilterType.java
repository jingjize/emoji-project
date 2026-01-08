package com.example.meme.model;

/**
 * 滤镜类型枚举
 */
public enum FilterType {
    NONE("none", "无滤镜", "原图"),
    GRAYSCALE("grayscale", "黑白", "经典黑白效果"),
    VINTAGE("vintage", "复古", "怀旧复古风格"),
    BRIGHT("bright", "明亮", "增强亮度"),
    DARK("dark", "暗调", "降低亮度"),
    WARM("warm", "暖色", "暖色调滤镜"),
    COOL("cool", "冷色", "冷色调滤镜"),
    SEPIA("sepia", "怀旧", "棕褐色怀旧效果"),
    CONTRAST("contrast", "高对比", "增强对比度"),
    SATURATE("saturate", "高饱和", "增强饱和度");
    
    private final String code;
    private final String name;
    private final String description;
    
    FilterType(String code, String name, String description) {
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
     * 根据代码获取滤镜类型
     */
    public static FilterType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return NONE;
        }
        for (FilterType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return NONE;
    }
}

