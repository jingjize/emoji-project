package com.example.meme.model;

/**
 * 情绪类型枚举
 */
public enum EmotionType {
    HAPPY("高兴", "happy", "一个开心、快乐、笑容满面的表情"),
    SAD("伤心", "sad", "一个悲伤、难过、哭泣的表情"),
    ANGRY("生气", "angry", "一个愤怒、生气、瞪眼的表情"),
    SURPRISED("惊讶", "surprised", "一个惊讶、震惊、瞪大眼睛的表情"),
    CONFUSED("困惑", "confused", "一个困惑、迷茫、疑惑的表情"),
    EXCITED("兴奋", "excited", "一个兴奋、激动、手舞足蹈的表情"),
    CALM("平静", "calm", "一个平静、安详、放松的表情"),
    SHY("害羞", "shy", "一个害羞、腼腆、脸红的表情"),
    PLAYFUL("调皮", "playful", "一个调皮、搞怪、顽皮的表情");
    
    private final String chineseName;
    private final String englishName;
    private final String description;
    
    EmotionType(String chineseName, String englishName, String description) {
        this.chineseName = chineseName;
        this.englishName = englishName;
        this.description = description;
    }
    
    public String getChineseName() {
        return chineseName;
    }
    
    public String getEnglishName() {
        return englishName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据中文名称获取情绪类型
     */
    public static EmotionType fromChineseName(String chineseName) {
        for (EmotionType type : values()) {
            if (type.chineseName.equals(chineseName)) {
                return type;
            }
        }
        return HAPPY; // 默认返回高兴
    }
    
    /**
     * 根据英文名称获取情绪类型
     */
    public static EmotionType fromEnglishName(String englishName) {
        for (EmotionType type : values()) {
            if (type.englishName.equalsIgnoreCase(englishName)) {
                return type;
            }
        }
        return HAPPY; // 默认返回高兴
    }
}

