package com.example.meme.model;

import java.awt.Color;

/**
 * 文字样式配置
 */
public class TextStyle {
    
    /**
     * 文字颜色（RGB，格式：255,255,255）
     */
    private String textColor = "255,255,255";
    
    /**
     * 描边颜色（RGB，格式：0,0,0）
     */
    private String strokeColor = "0,0,0";
    
    /**
     * 描边宽度（像素）
     */
    private Integer strokeWidth = 3;
    
    /**
     * 字体大小（像素）
     */
    private Integer fontSize = 40;
    
    /**
     * 字体名称（SimHei, SimSun, Arial等）
     */
    private String fontName = "SimHei";
    
    /**
     * 文字位置（top, center, bottom）
     */
    private String position = "center";
    
    /**
     * 文字透明度（0.0-1.0）
     */
    private Double opacity = 1.0;
    
    /**
     * 文字旋转角度（度）
     */
    private Integer rotation = 0;
    
    /**
     * 是否启用阴影
     */
    private Boolean enableShadow = false;
    
    /**
     * 阴影颜色（RGB）
     */
    private String shadowColor = "0,0,0";
    
    /**
     * 阴影偏移X
     */
    private Integer shadowOffsetX = 2;
    
    /**
     * 阴影偏移Y
     */
    private Integer shadowOffsetY = 2;
    
    // 默认构造函数
    public TextStyle() {
    }
    
    // Getters and Setters
    public String getTextColor() {
        return textColor;
    }
    
    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }
    
    public String getStrokeColor() {
        return strokeColor;
    }
    
    public void setStrokeColor(String strokeColor) {
        this.strokeColor = strokeColor;
    }
    
    public Integer getStrokeWidth() {
        return strokeWidth;
    }
    
    public void setStrokeWidth(Integer strokeWidth) {
        this.strokeWidth = strokeWidth;
    }
    
    public Integer getFontSize() {
        return fontSize;
    }
    
    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }
    
    public String getFontName() {
        return fontName;
    }
    
    public void setFontName(String fontName) {
        this.fontName = fontName;
    }
    
    public String getPosition() {
        return position;
    }
    
    public void setPosition(String position) {
        this.position = position;
    }
    
    public Double getOpacity() {
        return opacity;
    }
    
    public void setOpacity(Double opacity) {
        this.opacity = opacity;
    }
    
    public Integer getRotation() {
        return rotation;
    }
    
    public void setRotation(Integer rotation) {
        this.rotation = rotation;
    }
    
    public Boolean getEnableShadow() {
        return enableShadow;
    }
    
    public void setEnableShadow(Boolean enableShadow) {
        this.enableShadow = enableShadow;
    }
    
    public String getShadowColor() {
        return shadowColor;
    }
    
    public void setShadowColor(String shadowColor) {
        this.shadowColor = shadowColor;
    }
    
    public Integer getShadowOffsetX() {
        return shadowOffsetX;
    }
    
    public void setShadowOffsetX(Integer shadowOffsetX) {
        this.shadowOffsetX = shadowOffsetX;
    }
    
    public Integer getShadowOffsetY() {
        return shadowOffsetY;
    }
    
    public void setShadowOffsetY(Integer shadowOffsetY) {
        this.shadowOffsetY = shadowOffsetY;
    }
    
    /**
     * 解析RGB颜色字符串为Color对象
     */
    public Color parseColor(String rgbString) {
        String[] parts = rgbString.split(",");
        if (parts.length == 3) {
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return new Color(r, g, b);
            } catch (NumberFormatException e) {
                return Color.WHITE; // 默认白色
            }
        }
        return Color.WHITE;
    }
    
    /**
     * 获取文字颜色
     */
    public Color getTextColorAsColor() {
        return parseColor(textColor);
    }
    
    /**
     * 获取描边颜色
     */
    public Color getStrokeColorAsColor() {
        return parseColor(strokeColor);
    }
    
    /**
     * 获取阴影颜色
     */
    public Color getShadowColorAsColor() {
        return parseColor(shadowColor);
    }
}

