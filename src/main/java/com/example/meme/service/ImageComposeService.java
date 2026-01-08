package com.example.meme.service;

import com.example.meme.model.TextStyle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 图片合成服务
 * 负责将文案绘制到图片上
 */
@Service
public class ImageComposeService {
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    @Value("${file.base-url}")
    private String baseUrl;
    
    /**
     * 在图片上绘制文案并保存（使用默认样式）
     * 
     * @param imageBytes 原始图片字节数组
     * @param text 要绘制的文案
     * @return 生成的表情包图片 URL
     */
    public String composeImage(byte[] imageBytes, String text) throws IOException {
        TextStyle defaultStyle = new TextStyle();
        return composeImage(imageBytes, text, defaultStyle);
    }
    
    /**
     * 在图片上绘制文案并保存（支持自定义样式）
     * 文字背景透明，显示在图片中间
     * 
     * @param imageBytes 原始图片字节数组
     * @param text 要绘制的文案
     * @param textStyle 文字样式配置
     * @return 生成的表情包图片 URL
     */
    public String composeImage(byte[] imageBytes, String text, TextStyle textStyle) throws IOException {
        // 读取原始图片
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            throw new IOException("无法读取图片，请确保图片格式正确");
        }
        
        // 保持原图尺寸，不增加高度
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        // 创建支持透明度的图片（ARGB 格式）
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = newImage.createGraphics();
        
        // 设置抗锯齿和高质量渲染
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        // 绘制原始图片（作为背景）
        g2d.drawImage(originalImage, 0, 0, null);
        
        // 绘制文字（背景透明，直接叠加在原图上）
        drawText(g2d, text, width, height, textStyle);
        
        g2d.dispose();
        
        // 保存图片（使用 PNG 格式以支持透明度）
        String fileName = "meme_" + System.currentTimeMillis() + ".png";
        
        // 如果 uploadDir 是相对路径，转换为绝对路径（项目根目录）
        Path outputDir;
        if (Paths.get(uploadDir).isAbsolute()) {
            outputDir = Paths.get(uploadDir);
        } else {
            // 相对路径，使用项目根目录
            String projectRoot = System.getProperty("user.dir");
            outputDir = Paths.get(projectRoot, uploadDir);
        }
        
        Path outputPath = outputDir.resolve(fileName);
        
        // 确保目录存在
        Files.createDirectories(outputPath.getParent());
        
        // 保存文件（PNG 格式支持透明度）
        ImageIO.write(newImage, "png", outputPath.toFile());
        
        // 返回访问 URL
        return "/output/" + fileName;
    }
    
    /**
     * 在图片上绘制文字（支持自定义样式）
     * 
     * @param g2d Graphics2D 对象
     * @param text 要绘制的文字
     * @param width 图片宽度
     * @param height 图片高度
     * @param textStyle 文字样式配置
     */
    private void drawText(Graphics2D g2d, String text, int width, int height, TextStyle textStyle) {
        if (textStyle == null) {
            textStyle = new TextStyle();
        }
        
        // 设置字体
        int fontSize = textStyle.getFontSize() != null ? textStyle.getFontSize() : Math.max(30, Math.min(60, width / 20));
        Font font = new Font(textStyle.getFontName(), Font.BOLD, fontSize);
        g2d.setFont(font);
        
        FontMetrics fm = g2d.getFontMetrics();
        int textHeight = fm.getHeight();
        int lineSpacing = (int) (textHeight * 0.2);
        int maxWidth = width - 80; // 左右各留 40px 边距
        
        // 计算文字换行
        List<String> lines = wrapText(text, fm, maxWidth);
        
        // 计算文字总高度
        int totalTextHeight = lines.size() * textHeight + (lines.size() - 1) * lineSpacing;
        
        // 根据位置计算起始 Y 坐标
        int startY;
        String position = textStyle.getPosition() != null ? textStyle.getPosition() : "center";
        switch (position.toLowerCase()) {
            case "top":
                startY = textHeight + 40; // 顶部留 40px
                break;
            case "bottom":
                startY = height - totalTextHeight - 40; // 底部留 40px
                break;
            case "center":
            default:
                startY = (height - totalTextHeight) / 2 + textHeight; // 垂直居中
                break;
        }
        
        // 设置透明度
        float opacity = textStyle.getOpacity() != null ? textStyle.getOpacity().floatValue() : 1.0f;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        
        // 绘制每一行文字
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int textWidth = fm.stringWidth(line);
            int x = (width - textWidth) / 2; // 水平居中
            int y = startY + i * (textHeight + lineSpacing);
            
            // 保存当前变换
            AffineTransform originalTransform = g2d.getTransform();
            
            // 应用旋转（如果需要）
            if (textStyle.getRotation() != null && textStyle.getRotation() != 0) {
                double centerX = x + textWidth / 2.0;
                double centerY = y;
                AffineTransform rotation = AffineTransform.getRotateInstance(
                    Math.toRadians(textStyle.getRotation()), centerX, centerY);
                g2d.transform(rotation);
            }
            
            // 绘制描边
            Color strokeColor = textStyle.getStrokeColorAsColor();
            int strokeWidth = textStyle.getStrokeWidth() != null ? textStyle.getStrokeWidth() : 3;
            g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(strokeColor);
            
            // 绘制描边（8方向）
            int strokeRadius = Math.max(2, strokeWidth / 2);
            for (int dx = -strokeRadius; dx <= strokeRadius; dx++) {
                for (int dy = -strokeRadius; dy <= strokeRadius; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) <= strokeRadius && (dx != 0 || dy != 0)) {
                        g2d.drawString(line, x + dx, y + dy);
                    }
                }
            }
            
            // 绘制阴影（如果需要）
            if (textStyle.getEnableShadow() != null && textStyle.getEnableShadow()) {
                Color shadowColor = textStyle.getShadowColorAsColor();
                int shadowOffsetX = textStyle.getShadowOffsetX() != null ? textStyle.getShadowOffsetX() : 2;
                int shadowOffsetY = textStyle.getShadowOffsetY() != null ? textStyle.getShadowOffsetY() : 2;
                g2d.setColor(new Color(shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(), 128));
                g2d.drawString(line, x + shadowOffsetX, y + shadowOffsetY);
            }
            
            // 绘制主文字
            Color textColor = textStyle.getTextColorAsColor();
            g2d.setColor(textColor);
            g2d.drawString(line, x, y);
            
            // 恢复变换
            g2d.setTransform(originalTransform);
        }
    }
    
    /**
     * 文字自动换行
     * 
     * @param text 原始文字
     * @param fm FontMetrics 对象
     * @param maxWidth 最大宽度
     * @return 换行后的文字列表
     */
    private List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new java.util.ArrayList<String>();
        
        // 如果文字宽度小于最大宽度，直接返回
        if (fm.stringWidth(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }
        
        // 按字符分割并组合
        StringBuilder currentLine = new StringBuilder();
        for (char c : text.toCharArray()) {
            String testLine = currentLine.toString() + c;
            if (fm.stringWidth(testLine) <= maxWidth) {
                currentLine.append(c);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(String.valueOf(c));
                } else {
                    // 单个字符就超宽，强制添加
                    currentLine.append(c);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
}

