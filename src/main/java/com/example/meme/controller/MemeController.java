package com.example.meme.controller;

import com.example.meme.model.EmotionType;
import com.example.meme.model.FilterType;
import com.example.meme.model.GalleryImage;
import com.example.meme.service.ImageGalleryService;
import com.example.meme.service.MemeService;
import com.example.meme.util.ByteArrayMultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表情包生成控制器
 * 提供 REST API 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/meme")
@CrossOrigin(origins = "*") // 允许跨域访问
public class MemeController {
    
    @Autowired
    private MemeService memeService;
    
    @Autowired
    private ImageGalleryService imageGalleryService;
    
    /**
     * 生成情绪表情图片接口
     * 
     * @param image 上传的图片文件
     * @param emotion 情绪类型（happy, sad, angry, surprised, confused, excited, calm, shy）
     * @param text 自定义文字（可选），如果提供，会将文字绘制到生成的图片上
     * @param textStyle 文字样式JSON（可选），格式：{"textColor":"255,255,255","strokeColor":"0,0,0","fontSize":40,"position":"center",...}
     * @param filter 滤镜类型（可选），none, grayscale, vintage, bright, dark, warm, cool, sepia, contrast, saturate
     * @return 生成结果，包含图片 URL
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateEmotionImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "emotion", defaultValue = "happy") String emotion,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "textStyle", required = false) String textStyle,
            @RequestParam(value = "filter", required = false) String filter) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 解析情绪类型
            EmotionType emotionType;
            try {
                emotionType = EmotionType.fromEnglishName(emotion);
            } catch (Exception e) {
                emotionType = EmotionType.HAPPY; // 默认使用高兴
            }
            
            // 解析滤镜类型
            FilterType filterType = FilterType.fromCode(filter);
            
            // 调用服务生成情绪表情图片
            String imageUrl = memeService.generateEmotionImage(image, emotionType, text, textStyle, filterType);
            
            response.put("success", true);
            response.put("message", emotionType.getChineseName() + "表情图片生成成功");
            response.put("imageUrl", imageUrl);
            response.put("emotion", emotionType.getChineseName());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // 参数验证错误
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("imageUrl", null);
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            // 其他错误
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "生成表情图片失败: " + e.getMessage());
            response.put("imageUrl", null);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取所有支持的情绪类型
     */
    @GetMapping("/emotions")
    public ResponseEntity<Map<String, Object>> getEmotions() {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> emotions = new HashMap<>();
        
        for (EmotionType type : EmotionType.values()) {
            emotions.put(type.getEnglishName(), type.getChineseName());
        }
        
        response.put("success", true);
        response.put("emotions", emotions);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取所有支持的滤镜类型
     */
    @GetMapping("/filters")
    public ResponseEntity<Map<String, Object>> getFilters() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Map<String, String>> filters = new HashMap<>();
        
        for (FilterType type : FilterType.values()) {
            Map<String, String> filterInfo = new HashMap<>();
            filterInfo.put("name", type.getName());
            filterInfo.put("description", type.getDescription());
            filters.put(type.getCode(), filterInfo);
        }
        
        response.put("success", true);
        response.put("filters", filters);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "meme-generator");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 搜索图库图片
     * 
     * @param query 搜索关键词
     * @param page 页码（默认1）
     * @param perPage 每页数量（默认15）
     * @return 图片列表
     */
    @GetMapping("/gallery/search")
    public ResponseEntity<Map<String, Object>> searchGalleryImages(
            @RequestParam("query") String query,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "perPage", defaultValue = "15") Integer perPage) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<GalleryImage> images = imageGalleryService.searchImages(query, page, perPage);
            
            response.put("success", true);
            response.put("images", images);
            response.put("page", page);
            response.put("perPage", perPage);
            response.put("total", images.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("搜索图库图片失败", e);
            response.put("success", false);
            response.put("message", "搜索失败: " + e.getMessage());
            response.put("images", new java.util.ArrayList<>());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取热门/精选图片
     * 
     * @param page 页码（默认1）
     * @param perPage 每页数量（默认15）
     * @return 图片列表
     */
    @GetMapping("/gallery/curated")
    public ResponseEntity<Map<String, Object>> getCuratedImages(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "perPage", defaultValue = "15") Integer perPage) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<GalleryImage> images = imageGalleryService.getCuratedImages(page, perPage);
            
            response.put("success", true);
            response.put("images", images);
            response.put("page", page);
            response.put("perPage", perPage);
            response.put("total", images.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取热门图片失败", e);
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            response.put("images", new java.util.ArrayList<>());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取分类图片
     * 
     * @param category 分类名称（animals, nature, people, food, travel, emotion, funny, cute）
     * @param page 页码（默认1）
     * @return 图片列表
     */
    @GetMapping("/gallery/category")
    public ResponseEntity<Map<String, Object>> getCategoryImages(
            @RequestParam("category") String category,
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<GalleryImage> images = imageGalleryService.getCategoryImages(category, page);
            
            response.put("success", true);
            response.put("images", images);
            response.put("category", category);
            response.put("page", page);
            response.put("total", images.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取分类图片失败", e);
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            response.put("images", new java.util.ArrayList<>());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 从图库选择图片生成表情包
     * 接收图片URL，下载后生成
     * 
     * @param imageUrl 图库图片URL
     * @param emotion 情绪类型
     * @param text 自定义文字
     * @param textStyle 文字样式
     * @param filter 滤镜类型
     * @return 生成结果
     */
    @PostMapping("/generate-from-gallery")
    public ResponseEntity<Map<String, Object>> generateFromGallery(
            @RequestParam("imageUrl") String imageUrl,
            @RequestParam(value = "emotion", defaultValue = "happy") String emotion,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "textStyle", required = false) String textStyle,
            @RequestParam(value = "filter", required = false) String filter) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 下载图库图片
            byte[] imageBytes = imageGalleryService.downloadImage(imageUrl);
            
            // 创建临时MultipartFile
            MultipartFile multipartFile = new ByteArrayMultipartFile(
                    imageBytes,
                    "image",
                    "gallery-image.jpg",
                    "image/jpeg"
            );
            
            // 解析情绪类型
            EmotionType emotionType;
            try {
                emotionType = EmotionType.fromEnglishName(emotion);
            } catch (Exception e) {
                emotionType = EmotionType.HAPPY;
            }
            
            // 解析滤镜类型
            FilterType filterType = FilterType.fromCode(filter);
            
            // 调用生成服务
            String resultUrl = memeService.generateEmotionImage(
                    multipartFile, emotionType, text, textStyle, filterType);
            
            response.put("success", true);
            response.put("message", emotionType.getChineseName() + "表情图片生成成功");
            response.put("imageUrl", resultUrl);
            response.put("emotion", emotionType.getChineseName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("从图库生成表情包失败", e);
            response.put("success", false);
            response.put("message", "生成失败: " + e.getMessage());
            response.put("imageUrl", null);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

