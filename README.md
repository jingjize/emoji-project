# AI 情绪表情生成器

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 项目介绍

这是一个基于 Spring Boot 的 AI 情绪表情生成服务。用户上传一张图片，系统通过阿里云百炼多模态模型理解图片内容，自动生成不同情绪的表情图片。

**支持平台**：
- 🌐 Web 版本（浏览器访问）
- 📱 微信小程序版本

**核心功能**：
- 📸 图片上传（支持 Web 和小程序）
- 🤖 AI 自动理解图片内容（多模态视觉模型）
- 🎭 8 种情绪类型选择（高兴、伤心、生气等）
- 🎨 AI 自动生成情绪表情图片
- 📥 返回生成的表情包 URL（支持 OSS 直链）

## 技术栈

### 后端
- **Java 17**
- **Spring Boot 3.2.0**
- **Maven**
- **阿里云 DashScope SDK**（多模态 AI）
- **Spring AI Alibaba**（AI 集成框架）

### 前端
- **Web**: HTML + CSS + JavaScript
- **小程序**: 微信小程序原生开发

### AI 服务
- **阿里云百炼**（DashScope）
  - 视觉理解：`qwen-vl-plus`
  - 图像生成：`qwen-image`

## 快速开始

### 1. 环境要求

- JDK 17 或更高版本
- Maven 3.6+
- 阿里云百炼 API Key（可选，如果 API 不可用会使用模拟数据）

### 2. 配置 API Key

编辑 `src/main/resources/application.yml`，将 `YOUR_API_KEY` 替换为你的阿里云百炼 API Key：

```yaml
spring:
  ai:
    dashscope:
      api-key: YOUR_API_KEY  # 替换为你的阿里云百炼 API Key
```

**获取 API Key**：
1. 访问 [阿里云百炼控制台](https://dashscope.console.aliyun.com/)
2. 创建 API Key
3. 将 Key 配置到 `application.yml`

**注意**：如果没有 API Key 或 API 调用失败，系统会自动使用模拟数据，不影响功能测试。

### 3. 启动项目

```bash
# 方式一：使用 Maven
mvn spring-boot:run

# 方式二：先编译再运行
mvn clean package
java -jar target/emoji-project-1.0.0.jar
```

启动成功后，控制台会显示：
```
=================================
表情包生成服务已启动！
访问地址: http://localhost:8080
API 文档: POST /api/meme/generate
=================================
```

### 4. 创建输出目录

确保输出目录存在（如果不存在会自动创建）：
```bash
mkdir -p src/main/resources/static/output
```

## API 使用示例

### 1. 生成表情包

**请求**：
```bash
curl -X POST http://localhost:8080/api/meme/generate \
  -F "image=@/path/to/your/image.jpg"
```

**Windows PowerShell**：
```powershell
$uri = "http://localhost:8080/api/meme/generate"
$filePath = "C:\path\to\your\image.jpg"
$form = @{
    image = Get-Item -Path $filePath
}
Invoke-RestMethod -Uri $uri -Method Post -Form $form
```

**响应**：
```json
{
  "success": true,
  "message": "表情包生成成功",
  "imageUrl": "/output/meme_1234567890.jpg"
}
```

### 2. 访问生成的表情包

生成的表情包可以通过以下 URL 访问：
```
http://localhost:8080/output/meme_1234567890.jpg
```

### 3. 健康检查

```bash
curl http://localhost:8080/api/meme/health
```

## 示例流程

1. **准备一张图片**（JPG 或 PNG 格式，建议小于 10MB）

2. **调用生成接口**：
   ```bash
   curl -X POST http://localhost:8080/api/meme/generate \
     -F "image=@test.jpg"
   ```

3. **获取返回的 URL**：
   ```json
   {
     "imageUrl": "/output/meme_1234567890.jpg"
   }
   ```

4. **在浏览器中访问**：
   ```
   http://localhost:8080/output/meme_1234567890.jpg
   ```

## 项目结构

```
emoji-project/
├── src/main/java/com/example/meme/
│   ├── MemeApplication.java          # 主启动类
│   ├── controller/
│   │   └── MemeController.java       # REST 控制器
│   ├── service/
│   │   ├── MemeService.java          # 业务逻辑服务
│   │   ├── AiService.java            # AI 服务封装
│   │   ├── ImageGenerateService.java # 图像生成服务
│   │   └── ImageComposeService.java # 图片合成服务（已废弃）
│   ├── model/
│   │   ├── EmotionType.java          # 情绪类型枚举
│   │   ├── ImageUnderstandResult.java # AI 理解结果模型
│   │   └── ImageGenerateResult.java  # 图像生成结果模型
│   ├── client/
│   │   └── AiClient.java             # AI 客户端（DashScope SDK）
│   └── config/
│       └── WebConfig.java             # Web 配置（静态资源映射）
├── src/main/resources/
│   ├── application.yml               # 配置文件
│   └── static/
│       ├── index.html                 # Web 前端页面
│       └── output/                   # 生成的表情包存储目录
├── miniprogram/                      # 微信小程序版本
│   ├── app.js                        # 小程序入口
│   ├── app.json                      # 小程序配置
│   ├── pages/index/                  # 主页面
│   └── README.md                     # 小程序说明
├── pom.xml                           # Maven 配置
├── design.md                         # 设计文档
└── README.md                         # 本文件
```

## 功能说明

### 图片处理
- 支持 JPG、PNG 格式
- 最大文件大小：10MB
- 自动在图片底部添加留白区域
- 文案自动换行

### 文字样式
- 字体：黑体（SimHei）
- 颜色：白色文字 + 黑色描边
- 位置：底部居中
- 大小：40px

### AI 图像生成
- 使用阿里云百炼 DashScope SDK
- 视觉理解：`qwen-vl-plus` 模型理解图片内容
- 图像生成：`qwen-image` 模型生成情绪表情图片
- 支持 8 种情绪类型：高兴、伤心、生气、惊讶、困惑、兴奋、平静、害羞
- 如果 API 不可用，自动使用模拟数据

## 常见问题

### 1. 字体显示问题
如果系统没有黑体（SimHei），Java 会自动使用默认字体。可以在 `ImageComposeService.java` 中修改字体设置。

### 2. API Key 配置
如果没有 OpenAI API Key，系统会自动使用模拟数据，不影响功能测试。

### 3. 端口冲突
如果 8080 端口被占用，可以在 `application.yml` 中修改 `server.port`。

### 4. 图片访问 404
确保 `src/main/resources/static/output/` 目录存在，并且 Spring Boot 的静态资源路径配置正确。

## 平台支持

### Web 版本
- 访问地址：`http://localhost:8080`
- 支持拖拽上传图片
- 响应式设计，支持移动端

### 微信小程序版本
- 项目路径：`miniprogram/`
- 详细说明：查看 [miniprogram/README.md](miniprogram/README.md)
- 支持相册选择和拍照
- 支持保存图片到相册

## 后续扩展方向

- 支持更多图片格式
- 自定义情绪类型
- 批量处理多张图片
- 集成其他 AI 模型
- 云存储集成（OSS、COS 等）
- 用户系统（可选）
- 历史记录功能

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## GitHub 仓库

项目地址：https://github.com/jingjize/emoji-project

