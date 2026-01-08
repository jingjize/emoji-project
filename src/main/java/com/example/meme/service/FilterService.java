package com.example.meme.service;

import com.example.meme.model.FilterType;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 滤镜服务
 * 提供各种图片滤镜效果
 */
@Service
public class FilterService {
    
    /**
     * 应用滤镜效果
     * 
     * @param image 原始图片
     * @param filterType 滤镜类型
     * @return 处理后的图片
     */
    public BufferedImage applyFilter(BufferedImage image, FilterType filterType) {
        if (filterType == null || filterType == FilterType.NONE) {
            return image;
        }
        
        switch (filterType) {
            case GRAYSCALE:
                return applyGrayscale(image);
            case VINTAGE:
                return applyVintage(image);
            case BRIGHT:
                return applyBright(image);
            case DARK:
                return applyDark(image);
            case WARM:
                return applyWarm(image);
            case COOL:
                return applyCool(image);
            case SEPIA:
                return applySepia(image);
            case CONTRAST:
                return applyContrast(image);
            case SATURATE:
                return applySaturate(image);
            default:
                return image;
        }
    }
    
    /**
     * 黑白滤镜
     */
    private BufferedImage applyGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);
                
                // 灰度公式：0.299*R + 0.587*G + 0.114*B
                int gray = (int) (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
                Color grayColor = new Color(gray, gray, gray, color.getAlpha());
                result.setRGB(x, y, grayColor.getRGB());
            }
        }
        return result;
    }
    
    /**
     * 复古滤镜
     */
    private BufferedImage applyVintage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);
                
                // 复古效果：降低饱和度，增加暖色调
                int r = Math.min(255, (int) (color.getRed() * 1.1));
                int g = (int) (color.getGreen() * 0.95);
                int b = (int) (color.getBlue() * 0.9);
                
                // 添加轻微黄色调
                r = Math.min(255, r + 10);
                g = Math.min(255, g + 5);
                
                Color vintageColor = new Color(r, g, b, color.getAlpha());
                result.setRGB(x, y, vintageColor.getRGB());
            }
        }
        return result;
    }
    
    /**
     * 明亮滤镜
     */
    private BufferedImage applyBright(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);
                
                int r = Math.min(255, (int) (color.getRed() * 1.3));
                int g = Math.min(255, (int) (color.getGreen() * 1.3));
                int b = Math.min(255, (int) (color.getBlue() * 1.3));
                
                Color brightColor = new Color(r, g, b, color.getAlpha());
                result.setRGB(x, y, brightColor.getRGB());
            }
        }
        return result;
    }
    
    /**
     * 暗调滤镜
     */
    private BufferedImage applyDark(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);
                
                int r = (int) (color.getRed() * 0.7);
                int g = (int) (color.getGreen() * 0.7);
                int b = (int) (color.getBlue() * 0.7);
                
                Color darkColor = new Color(r, g, b, color.getAlpha());
                result.setRGB(x, y, darkColor.getRGB());
            }
        }
        return result;
    }
    
    /**
     * 暖色滤镜
     */
    private BufferedImage applyWarm(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);
                
                // 增强红色和黄色
                int r = Math.min(255, (int) (color.getRed() * 1.2));
                int g = Math.min(255, (int) (color.getGreen() * 1.1));
                int b = (int) (color.getBlue() * 0.95);
                
                Color warmColor = new Color(r, g, b, color.getAlpha());
                result.setRGB(x, y, warmColor.getRGB());
            }
        }
        return result;
    }
    
    /**
     * 冷色滤镜
     */
    private BufferedImage applyCool(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);
                
                // 增强蓝色和青色
                int r = (int) (color.getRed() * 0.95);
                int g = Math.min(255, (int) (color.getGreen() * 1.05));
                int b = Math.min(255, (int) (color.getBlue() * 1.2));
                
                Color coolColor = new Color(r, g, b, color.getAlpha());
                result.setRGB(x, y, coolColor.getRGB());
            }
        }
        return result;
    }
    
    /**
     * 怀旧（棕褐色）滤镜
     */
    private BufferedImage applySepia(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);
                
                // 转换为灰度
                int gray = (int) (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
                
                // 应用棕褐色调
                int r = Math.min(255, (int) (gray * 1.2));
                int g = Math.min(255, (int) (gray * 1.0));
                int b = Math.min(255, (int) (gray * 0.8));
                
                Color sepiaColor = new Color(r, g, b, color.getAlpha());
                result.setRGB(x, y, sepiaColor.getRGB());
            }
        }
        return result;
    }
    
    /**
     * 高对比度滤镜
     */
    private BufferedImage applyContrast(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        double contrast = 1.5; // 对比度因子
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);
                
                // 应用对比度调整
                int r = Math.max(0, Math.min(255, (int) ((color.getRed() - 128) * contrast + 128)));
                int g = Math.max(0, Math.min(255, (int) ((color.getGreen() - 128) * contrast + 128)));
                int b = Math.max(0, Math.min(255, (int) ((color.getBlue() - 128) * contrast + 128)));
                
                Color contrastColor = new Color(r, g, b, color.getAlpha());
                result.setRGB(x, y, contrastColor.getRGB());
            }
        }
        return result;
    }
    
    /**
     * 高饱和度滤镜
     */
    private BufferedImage applySaturate(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        double saturation = 1.5; // 饱和度因子
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);
                
                // 转换为HSL，增加饱和度，再转回RGB
                float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                float s = Math.min(1.0f, (float) (hsb[1] * saturation));
                Color saturatedColor = new Color(Color.HSBtoRGB(hsb[0], s, hsb[2]));
                
                // 保持透明度
                Color finalColor = new Color(
                    saturatedColor.getRed(),
                    saturatedColor.getGreen(),
                    saturatedColor.getBlue(),
                    color.getAlpha()
                );
                result.setRGB(x, y, finalColor.getRGB());
            }
        }
        return result;
    }
}

