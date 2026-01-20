package com.example.meme.controller;

import com.example.meme.annotation.LogRequest;
import com.example.meme.model.EmotionType;
import com.example.meme.model.FilterType;
import com.example.meme.model.GalleryImage;
import com.example.meme.model.ImageStyle;
import com.example.meme.service.AiService;
import com.example.meme.service.HotSlangService;
import com.example.meme.service.ImageGalleryService;
import com.example.meme.service.MemeService;
import com.example.meme.service.RateLimitService;
import com.example.meme.util.ByteArrayMultipartFile;
import com.example.meme.util.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
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
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Autowired
    private AiService aiService;
    
    @Autowired
    private HotSlangService hotSlangService;
    
    /**
     * 生成情绪表情图片接口
     * 
     * @param image 上传的图片文件
     * @param emotion 情绪类型（happy, sad, angry, surprised, confused, excited, calm, shy）
     * @param text 自定义文字（可选），如果提供，会将文字绘制到生成的图片上
     * @param textStyle 文字样式JSON（可选），格式：{"textColor":"255,255,255","strokeColor":"0,0,0","fontSize":40,"position":"center",...}
     * @param filter 滤镜类型（可选），none, grayscale, vintage, bright, dark, warm, cool, sepia, contrast, saturate
     * @param style 图片风格（可选），cartoon, pixel, tough, original, realistic
     * @return 生成结果，包含图片 URL
     */
    @PostMapping("/generate")
    @LogRequest("生成表情包")
    public ResponseEntity<Map<String, Object>> generateEmotionImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "emotion", defaultValue = "happy") String emotion,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "textStyle", required = false) String textStyle,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "style", required = false) String style,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取客户端IP并检查限流
            String clientIp = IpUtils.getClientIp(request);
            if (!rateLimitService.canGenerate(clientIp)) {
                int remaining = rateLimitService.getRemainingCount(clientIp);
                response.put("success", false);
                response.put("message", "今日生成次数已达上限（20次/天），请明天再试");
                response.put("imageUrl", null);
                response.put("remaining", remaining);
                return ResponseEntity.status(429).body(response); // 429 Too Many Requests
            }
            
            // 解析情绪类型
            EmotionType emotionType;
            try {
                emotionType = EmotionType.fromEnglishName(emotion);
            } catch (Exception e) {
                emotionType = EmotionType.HAPPY; // 默认使用高兴
            }
            
            // 解析滤镜类型
            FilterType filterType = FilterType.fromCode(filter);
            
            // 解析图片风格
            ImageStyle imageStyle = ImageStyle.fromCode(style);
            
            // 调用服务生成情绪表情图片
            String imageUrl = memeService.generateEmotionImage(image, emotionType, text, textStyle, filterType, imageStyle);
            
            // 生成成功，增加计数
            rateLimitService.incrementCount(clientIp);
            int remaining = rateLimitService.getRemainingCount(clientIp);
            
            response.put("success", true);
            response.put("message", emotionType.getChineseName() + "表情图片生成成功");
            response.put("imageUrl", imageUrl);
            response.put("emotion", emotionType.getChineseName());
            response.put("remaining", remaining);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // 参数验证错误
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("imageUrl", null);
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            // 其他错误
            log.error("生成表情包失败: {}", e.getMessage(), e);
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
    @LogRequest("获取情绪类型")
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
    @LogRequest("获取滤镜类型")
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
     * 获取所有支持的图片风格类型
     */
    @GetMapping("/styles")
    @LogRequest("获取图片风格类型")
    public ResponseEntity<Map<String, Object>> getStyles() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Map<String, String>> styles = new HashMap<>();
        
        for (ImageStyle type : ImageStyle.values()) {
            Map<String, String> styleInfo = new HashMap<>();
            styleInfo.put("name", type.getName());
            styleInfo.put("description", type.getDescription());
            styles.put(type.getCode(), styleInfo);
        }
        
        response.put("success", true);
        response.put("styles", styles);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.info("=== 健康检查请求 ===");
        log.info("请求接口: GET /api/meme/health");
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "meme-generator");
        
        log.info("=== 健康检查响应 ===");
        log.info("响应状态: 200 OK, status=ok");
        
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
    @LogRequest("搜索图库图片")
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
            log.error("搜索图库图片失败: {}", e.getMessage(), e);
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
    @LogRequest("获取热门图片")
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
            log.error("获取热门图片失败: {}", e.getMessage(), e);
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
    @LogRequest("获取分类图片")
    public ResponseEntity<Map<String, Object>> getCategoryImages(
            @RequestParam("category") String category,
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<GalleryImage> images;

            // “项目图片”标签：返回项目内置图片（放在 src/main/resources/static/project-gallery/）
            if ("local".equalsIgnoreCase(category) || "project".equalsIgnoreCase(category)) {
                String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
                images = imageGalleryService.getProjectImages(baseUrl, page, 15);
            } else {
                images = imageGalleryService.getCategoryImages(category, page);
            }
            
            response.put("success", true);
            response.put("images", images);
            response.put("category", category);
            response.put("page", page);
            response.put("total", images.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取分类图片失败: {}", e.getMessage(), e);
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
    @LogRequest("从图库生成表情包")
    public ResponseEntity<Map<String, Object>> generateFromGallery(
            @RequestParam("imageUrl") String imageUrl,
            @RequestParam(value = "emotion", defaultValue = "happy") String emotion,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "textStyle", required = false) String textStyle,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "style", required = false) String style,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取客户端IP并检查限流
            String clientIp = IpUtils.getClientIp(request);
            if (!rateLimitService.canGenerate(clientIp)) {
                int remaining = rateLimitService.getRemainingCount(clientIp);
                response.put("success", false);
                response.put("message", "今日生成次数已达上限（20次/天），请明天再试");
                response.put("imageUrl", null);
                response.put("remaining", remaining);
                return ResponseEntity.status(429).body(response); // 429 Too Many Requests
            }
            
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
            
            // 解析图片风格
            ImageStyle imageStyle = ImageStyle.fromCode(style);
            
            // 调用生成服务
            String resultUrl = memeService.generateEmotionImage(
                    multipartFile, emotionType, text, textStyle, filterType, imageStyle);
            
            // 生成成功，增加计数
            rateLimitService.incrementCount(clientIp);
            int remaining = rateLimitService.getRemainingCount(clientIp);
            
            response.put("success", true);
            response.put("message", emotionType.getChineseName() + "表情图片生成成功");
            response.put("imageUrl", resultUrl);
            response.put("emotion", emotionType.getChineseName());
            response.put("remaining", remaining);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("从图库生成表情包失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "生成失败: " + e.getMessage());
            response.put("imageUrl", null);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取热门词语列表
     * 每次调用都重新生成，确保返回最新、不同的词语
     * 
     * @return 热门词语列表
     */
    @GetMapping("/slang/hot-words")
    @LogRequest("获取热门词语")
    public ResponseEntity<Map<String, Object>> getHotSlangs(
            @RequestParam(value = "industry", required = false) String industry) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> hotSlangs = hotSlangService.getTodayHotSlangs(industry);
            
            response.put("success", true);
            response.put("hotSlangs", hotSlangs);
            response.put("message", "获取成功");
            if (industry != null && !industry.trim().isEmpty()) {
                response.put("industry", industry);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取热门词语失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            response.put("hotSlangs", new java.util.ArrayList<>());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 手动刷新热门词语列表
     * 重新从AI获取最新热门词语（与获取接口效果相同，但语义更清晰）
     * 
     * @return 刷新后的热门词语列表
     */
    @PostMapping("/slang/hot-words/refresh")
    @LogRequest("刷新热门词语")
    public ResponseEntity<Map<String, Object>> refreshHotSlangs(
            @RequestParam(value = "industry", required = false) String industry) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 重新生成热门词语（每次调用都会重新生成，可以指定行业）
            List<String> hotSlangs = hotSlangService.getTodayHotSlangs(industry);
            
            response.put("success", true);
            response.put("hotSlangs", hotSlangs);
            response.put("message", "刷新成功");
            if (industry != null && !industry.trim().isEmpty()) {
                response.put("industry", industry);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("刷新热门词语失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "刷新失败: " + e.getMessage());
            response.put("hotSlangs", new java.util.ArrayList<>());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 黑话盒子 - 解释互联网黑话/隐喻词语
     * 使用 AI 分析词语的含义、来源、使用场景等
     * 返回简短解释和详细说明
     * 
     * @param word 需要解释的词语
     * @return 解释结果（包含简短解释和详细说明）
     */
    @GetMapping("/slang/explain")
    @LogRequest("解释互联网黑话")
    public ResponseEntity<Map<String, Object>> explainSlang(
            @RequestParam("word") String word) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (word == null || word.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "请输入需要解释的词语");
                response.put("shortExplanation", "");
                response.put("detailedExplanation", "");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 调用 AI 服务解释词语
            com.example.meme.client.AiClient.SlangExplanation explanation = aiService.explainInternetSlang(word.trim());
            
            response.put("success", true);
            response.put("word", word.trim());
            response.put("shortExplanation", explanation.getShortExplanation());
            response.put("detailedExplanation", explanation.getDetailedExplanation());
            response.put("message", "解释成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("解释词语失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "解释失败: " + e.getMessage());
            response.put("shortExplanation", "");
            response.put("detailedExplanation", "");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

