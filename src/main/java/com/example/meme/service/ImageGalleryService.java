package com.example.meme.service;

import com.example.meme.model.GalleryImage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 图库服务
 * 使用 Pixabay API，支持中文搜索（直接使用中文关键词 + lang=zh）
 */
@Slf4j
@Service
public class ImageGalleryService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${gallery.pixabay.api-key:}")
    private String pixabayApiKey;
    
    // Pixabay API 基础URL
    private static final String PIXABAY_API_BASE = "https://pixabay.com/api";
    
    public ImageGalleryService() {
        // 配置 WebClient 以支持大文件下载（增加缓冲区大小到 10MB）
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
        
        this.webClient = WebClient.builder()
                .defaultHeader("User-Agent", "MemeGenerator/1.0")
                .exchangeStrategies(strategies)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 搜索图片（使用 Pixabay API，支持中文搜索）
     * 根据实际测试，Pixabay API 支持直接使用中文关键词搜索，只需设置 lang=zh
     * 策略：检测到中文时，直接使用中文关键词 + lang=zh；英文关键词使用 lang=en
     * 
     * @param query 搜索关键词（支持中文和英文）
     * @param page 页码（从1开始）
     * @param perPage 每页数量（1-20，默认15）
     * @return 图片列表
     */
    public List<GalleryImage> searchImages(String query, Integer page, Integer perPage) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (perPage == null || perPage < 1 || perPage > 20) {
            perPage = 15;
        }
        
        if (query == null || query.trim().isEmpty()) {
            query = "nature"; // 默认搜索词
        }
        
        // 检查 API Key
        if (pixabayApiKey == null || pixabayApiKey.trim().isEmpty()) {
            log.error("Pixabay API Key 未配置");
            return getDefaultImages();
        }
        
        String searchQuery = query.trim();
        String lang = "en"; // 默认使用英文
        
        // 判断是否包含中文字符，如果包含中文则使用 lang=zh
        if (containsChinese(searchQuery)) {
            lang = "zh"; // 使用中文语言参数，支持中文关键词搜索
        }
        
        try {
            // 直接使用原始关键词搜索（支持中文和英文），根据语言设置 lang 参数
            List<GalleryImage> images = searchWithLang(searchQuery, lang, page, perPage);
            
            if (images != null && !images.isEmpty()) {
                return images;
            }
            
            log.warn("Pixabay API 未搜索到图片: {}", searchQuery);
            return getDefaultImages();
            
        } catch (Exception e) {
            log.error("Pixabay API 搜索失败: {}", e.getMessage(), e);
            return getDefaultImages();
        }
    }
    
    /**
     * 使用指定语言参数调用 Pixabay API
     */
    private List<GalleryImage> searchWithLang(String query, String lang, Integer page, Integer perPage) {
        try {
            // URL编码查询关键词
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            
            // 构建请求URL，参数顺序：key, q, image_type, page, per_page, lang, safesearch
            String url = String.format("%s/?key=%s&q=%s&image_type=photo&page=%d&per_page=%d&lang=%s&safesearch=true", 
                    PIXABAY_API_BASE,
                    pixabayApiKey,
                    encodedQuery,
                    page,
                    perPage,
                    lang);
            
            String response = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                        log.error("Pixabay API 返回错误状态码: {}", clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                                .doOnNext(body -> log.error("错误响应内容: {}", body))
                                .then(Mono.error(new RuntimeException("API 返回错误状态码: " + clientResponse.statusCode())));
                    })
                    .bodyToMono(String.class)
                    .block();
            
            return parsePixabayResponse(response);
        } catch (Exception e) {
            log.error("Pixabay API 调用失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 判断文本是否包含中文字符
     */
    private boolean containsChinese(String text) {
        if (text == null) {
            return false;
        }
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                return true; // 中文字符范围
            }
        }
        return false;
    }
    
    /**
     * 获取热门图片（精选）
     * 使用 Pixabay API，搜索热门关键词
     * 
     * @param page 页码
     * @param perPage 每页数量
     * @return 图片列表
     */
    public List<GalleryImage> getCuratedImages(Integer page, Integer perPage) {
        // 使用中文关键词搜索热门图片，设置 lang=zh
        return searchImages("热门", page, perPage);
    }
    
    /**
     * 获取分类图片（预设分类）
     * 使用中文关键词搜索，设置 lang=zh
     * 
     * @param category 分类代码（如 "animals", "nature" 等，或中文分类名）
     * @param page 页码
     * @return 图片列表
     */
    public List<GalleryImage> getCategoryImages(String category, Integer page) {
        // 预设分类关键词映射（使用中文关键词，直接支持中文搜索）
        java.util.Map<String, String> categoryMap = new java.util.HashMap<>();
        categoryMap.put("animals", "动物");
        categoryMap.put("nature", "自然");
        categoryMap.put("people", "人物");
        categoryMap.put("food", "食物");
        categoryMap.put("travel", "旅行");
        categoryMap.put("emotion", "表情");
        categoryMap.put("funny", "搞笑");
        categoryMap.put("cute", "可爱");
        
        // 获取中文关键词，如果分类代码不在映射中，直接使用原值
        String query = categoryMap.getOrDefault(category, category);
        
        // 调用 searchImages，它会自动检测中文并设置 lang=zh
        return searchImages(query, page, 15);
    }
    
    /**
     * 解析 Pixabay API 响应
     */
    private List<GalleryImage> parsePixabayResponse(String response) {
        List<GalleryImage> images = new ArrayList<>();
        
        if (response == null || response.trim().isEmpty()) {
            log.warn("Pixabay API 响应为空");
            return images;
        }
        
        try {
            JsonNode root = objectMapper.readTree(response);
            
            // 检查是否有错误
            if (root.has("error")) {
                String errorMsg = root.get("error").asText();
                log.error("Pixabay API 返回错误: {}", errorMsg);
                return images;
            }
            
            JsonNode hits = root.get("hits");
            
            if (hits != null && hits.isArray()) {
                for (JsonNode hit : hits) {
                    try {
                        String id = String.valueOf(hit.get("id").asLong());
                        String photographer = hit.has("user") ? hit.get("user").asText() : "";
                        
                        // Pixabay 提供多种尺寸
                        String original = hit.has("largeImageURL") ? hit.get("largeImageURL").asText() : 
                                         (hit.has("webformatURL") ? hit.get("webformatURL").asText() : "");
                        String medium = hit.has("webformatURL") ? hit.get("webformatURL").asText() : original;
                        String small = hit.has("previewURL") ? hit.get("previewURL").asText() : medium;
                        
                        int width = hit.has("imageWidth") ? hit.get("imageWidth").asInt() : 0;
                        int height = hit.has("imageHeight") ? hit.get("imageHeight").asInt() : 0;
                        
                        images.add(new GalleryImage(
                                id,
                                original,
                                medium,
                                small,
                                width,
                                height,
                                photographer,
                                "Pixabay"
                        ));
                    } catch (Exception e) {
                        log.warn("解析单张图片失败，跳过: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析Pixabay响应失败: {}", e.getMessage(), e);
            log.error("响应内容: {}", response.length() > 1000 ? response.substring(0, 1000) + "..." : response);
        }
        
        return images;
    }
    
    /**
     * 获取默认图片（当API调用失败时）
     */
    private List<GalleryImage> getDefaultImages() {
        List<GalleryImage> defaultImages = new ArrayList<>();
        
        // 返回一些占位图片URL（可以使用免费的占位图服务）
        // 这里返回空列表，前端可以显示提示信息
        log.warn("返回空图片列表，API调用失败");
        
        return defaultImages;
    }
    
    /**
     * 下载图片并转换为字节数组
     * 支持大文件下载（最大 10MB）
     * 
     * @param imageUrl 图片URL
     * @return 图片字节数组
     */
    public byte[] downloadImage(String imageUrl) throws Exception {
        return webClient.get()
                .uri(URI.create(imageUrl))
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }
}



