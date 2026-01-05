package com.example.meme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
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
     * 在图片上绘制文案并保存
     * 
     * @param imageBytes 原始图片字节数组
     * @param text 要绘制的文案
     * @return 生成的表情包图片 URL
     */
    public String composeImage(byte[] imageBytes, String text) throws IOException {
        // 读取原始图片
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            throw new IOException("无法读取图片，请确保图片格式正确");
        }
        
        // 创建新的图片，增加底部空间用于文字
        int padding = 80; // 底部留白
        int width = originalImage.getWidth();
        int height = originalImage.getHeight() + padding;
        
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = newImage.createGraphics();
        
        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 绘制原始图片
        g2d.drawImage(originalImage, 0, 0, null);
        
        // 绘制文字
        drawText(g2d, text, width, height, padding);
        
        g2d.dispose();
        
        // 保存图片
        String fileName = "meme_" + System.currentTimeMillis() + ".jpg";
        
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
        
        // 保存文件
        ImageIO.write(newImage, "jpg", outputPath.toFile());
        
        // 返回访问 URL
        return "/output/" + fileName;
    }
    
    /**
     * 在图片上绘制文字
     * 
     * @param g2d Graphics2D 对象
     * @param text 要绘制的文字
     * @param width 图片宽度
     * @param height 图片高度
     * @param padding 底部留白
     */
    private void drawText(Graphics2D g2d, String text, int width, int height, int padding) {
        // 设置字体（黑体）
        Font font = new Font("SimHei", Font.BOLD, 40); // 如果系统没有黑体，会使用默认字体
        g2d.setFont(font);
        
        FontMetrics fm = g2d.getFontMetrics();
        int textHeight = fm.getHeight();
        int maxWidth = width - 40; // 左右各留 20px 边距
        
        // 计算文字换行
        List<String> lines = wrapText(text, fm, maxWidth);
        
        // 计算文字起始 Y 坐标（底部居中）
        int totalTextHeight = lines.size() * textHeight;
        int startY = height - padding + (padding - totalTextHeight) / 2 + textHeight;
        
        // 绘制每一行文字
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int textWidth = fm.stringWidth(line);
            int x = (width - textWidth) / 2; // 居中
            int y = startY + i * textHeight;
            
            // 绘制黑色描边
            g2d.setStroke(new BasicStroke(3.0f));
            g2d.setColor(Color.BLACK);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (dx != 0 || dy != 0) {
                        g2d.drawString(line, x + dx, y + dy);
                    }
                }
            }
            
            // 绘制白色文字
            g2d.setColor(Color.WHITE);
            g2d.drawString(line, x, y);
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

