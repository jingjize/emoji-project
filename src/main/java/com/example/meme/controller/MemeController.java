package com.example.meme.controller;

import com.example.meme.model.EmotionType;
import com.example.meme.service.MemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 表情包生成控制器
 * 提供 REST API 接口
 */
@RestController
@RequestMapping("/api/meme")
@CrossOrigin(origins = "*") // 允许跨域访问
public class MemeController {
    
    @Autowired
    private MemeService memeService;
    
    /**
     * 生成情绪表情图片接口
     * 
     * @param image 上传的图片文件
     * @param emotion 情绪类型（happy, sad, angry, surprised, confused, excited, calm, shy）
     * @return 生成结果，包含图片 URL
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateEmotionImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "emotion", defaultValue = "happy") String emotion) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 解析情绪类型
            EmotionType emotionType;
            try {
                emotionType = EmotionType.fromEnglishName(emotion);
            } catch (Exception e) {
                emotionType = EmotionType.HAPPY; // 默认使用高兴
            }
            
            // 调用服务生成情绪表情图片
            String imageUrl = memeService.generateEmotionImage(image, emotionType);
            
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
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "meme-generator");
        return ResponseEntity.ok(response);
    }
}

