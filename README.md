# 表情包生成 MVP 项目

## 项目介绍

这是一个基于 Spring Boot 的图片表情包自动生成服务。用户上传一张图片，系统通过 AI 多模态模型理解图片内容，自动生成适合的中文文案，并将文案绘制到图片上生成表情包。

**核心功能**：
- 📸 图片上传
- 🤖 AI 自动生成情绪表情图片（高兴、伤心、生气等）
- ✍️ **自定义文字**：可选择在生成的图片上添加自定义文字
- 🎨 **文字样式自定义**：支持自定义颜色、字体大小、位置、描边等
- 🖼️ **滤镜特效**：10+ 种滤镜效果（黑白、复古、明亮、暗调、暖色、冷色等）
- 📥 返回生成的表情包 URL
- 📱 支持 Web 端和微信小程序

## 技术栈

- **Java 17**
- **Spring Boot 3.2.0**
- **Maven**
- **Java Graphics2D**（图片处理）
- **OpenAI API**（多模态模型，可替换）

## 快速开始

### 1. 环境要求

- JDK 17 或更高版本
- Maven 3.6+
- OpenAI API Key（可选，如果 API 不可用会使用模拟数据）

### 2. 配置 API Key

编辑 `src/main/resources/application.yml`，将 `YOUR_API_KEY` 替换为你的 OpenAI API Key：

```yaml
ai:
  openai:
    api-key: YOUR_API_KEY  # 替换为你的 API Key
```

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
访问地址: http://localhost:8443
API 文档: POST /api/meme/generate
=================================
```

### 4. 创建输出目录

确保输出目录存在（如果不存在会自动创建）：
```bash
mkdir -p src/main/resources/static/output
```

## API 使用示例

### 1. 生成情绪表情图片（不带自定义文字）

**请求**：
```bash
curl -X POST http://localhost:8443/api/meme/generate \
  -F "image=@/path/to/your/image.jpg" \
  -F "emotion=happy"
```

**Windows PowerShell**：
```powershell
$uri = "http://localhost:8443/api/meme/generate"
$filePath = "C:\path\to\your\image.jpg"
$form = @{
    image = Get-Item -Path $filePath
    emotion = "happy"
}
Invoke-RestMethod -Uri $uri -Method Post -Form $form
```

**响应**：
```json
{
  "success": true,
  "message": "高兴表情图片生成成功",
  "imageUrl": "https://dashscope-result-xxx.oss-cn-xxx.aliyuncs.com/xxx.png",
  "emotion": "高兴"
}
```

### 2. 生成情绪表情图片（带自定义文字和样式）

**请求**：
```bash
curl -X POST http://localhost:8443/api/meme/generate \
  -F "image=@/path/to/your/image.jpg" \
  -F "emotion=happy" \
  -F "text=今天真开心！" \
  -F "textStyle={\"textColor\":\"255,255,255\",\"strokeColor\":\"0,0,0\",\"fontSize\":40,\"position\":\"center\"}" \
  -F "filter=vintage"
```

**响应**：
```json
{
  "success": true,
  "message": "高兴表情图片生成成功",
  "imageUrl": "/output/meme_1234567890.png",
  "emotion": "高兴"
}
```

**参数说明**：
- `image`（必需）：上传的图片文件
- `emotion`（可选，默认：happy）：情绪类型
  - `happy` - 高兴
  - `sad` - 伤心
  - `angry` - 生气
  - `surprised` - 惊讶
  - `confused` - 困惑
  - `excited` - 兴奋
  - `calm` - 平静
  - `shy` - 害羞
  - `playful` - 调皮
- `text`（可选）：自定义文字，如果提供，会将文字绘制到生成的图片上
- `textStyle`（可选）：文字样式JSON，格式：
  ```json
  {
    "textColor": "255,255,255",      // 文字颜色（RGB）
    "strokeColor": "0,0,0",          // 描边颜色（RGB）
    "strokeWidth": 3,                // 描边宽度（1-10）
    "fontSize": 40,                  // 字体大小（20-100）
    "position": "center",            // 位置：top/center/bottom
    "fontName": "SimHei",            // 字体名称
    "opacity": 1.0,                  // 透明度（0.0-1.0）
    "rotation": 0,                   // 旋转角度（度）
    "enableShadow": false            // 是否启用阴影
  }
  ```
- `filter`（可选）：滤镜类型
  - `none` - 无滤镜
  - `grayscale` - 黑白
  - `vintage` - 复古
  - `bright` - 明亮
  - `dark` - 暗调
  - `warm` - 暖色
  - `cool` - 冷色
  - `sepia` - 怀旧
  - `contrast` - 高对比
  - `saturate` - 高饱和

### 3. 获取支持的滤镜类型

```bash
curl http://localhost:8443/api/meme/filters
```

**响应**：
```json
{
  "success": true,
  "filters": {
    "none": {"name": "无滤镜", "description": "原图"},
    "grayscale": {"name": "黑白", "description": "经典黑白效果"},
    "vintage": {"name": "复古", "description": "怀旧复古风格"},
    ...
  }
}
```

### 4. 获取支持的情绪类型

```bash
curl http://localhost:8443/api/meme/emotions
```

**响应**：
```json
{
  "success": true,
  "emotions": {
    "happy": "高兴",
    "sad": "伤心",
    "angry": "生气",
    "surprised": "惊讶",
    "confused": "困惑",
    "excited": "兴奋",
    "calm": "平静",
    "shy": "害羞"
  }
}
```

### 5. 访问生成的表情包

如果返回的是本地路径（如 `/output/meme_1234567890.jpg`），可以通过以下 URL 访问：
```
http://localhost:8443/output/meme_1234567890.jpg
```

如果返回的是 OSS URL（如 `https://dashscope-result-xxx.oss-cn-xxx.aliyuncs.com/xxx.png`），可以直接在浏览器中打开。

### 6. 健康检查

```bash
curl http://localhost:8443/api/meme/health
```

## 示例流程

### Web 端使用

1. **访问前端页面**：
   ```
   http://localhost:8443/
   ```

2. **上传图片**：点击或拖拽图片到上传区域

3. **选择情绪**：点击情绪按钮选择想要的情绪（如：高兴、伤心等）

4. **输入自定义文字**（可选）：在文字输入框中输入你想添加到图片上的文字

5. **生成表情**：点击「生成情绪表情」按钮

6. **查看结果**：生成完成后会显示结果图片，可以下载保存

### API 调用流程

1. **准备一张图片**（JPG 或 PNG 格式，建议小于 10MB）

2. **调用生成接口**（不带文字）：
   ```bash
   curl -X POST http://localhost:8443/api/meme/generate \
     -F "image=@test.jpg" \
     -F "emotion=happy"
   ```

3. **调用生成接口**（带自定义文字）：
   ```bash
   curl -X POST http://localhost:8443/api/meme/generate \
     -F "image=@test.jpg" \
     -F "emotion=happy" \
     -F "text=今天真开心！"
   ```

4. **获取返回的 URL**：
   ```json
   {
     "success": true,
     "imageUrl": "/output/meme_1234567890.jpg",
     "emotion": "高兴"
   }
   ```

5. **在浏览器中访问**：
   ```
   http://localhost:8443/output/meme_1234567890.jpg
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
│   │   └── ImageComposeService.java  # 图片合成服务
│   ├── model/
│   │   └── ImageUnderstandResult.java # AI 返回结果模型
│   └── client/
│       └── AiClient.java             # AI 客户端
├── src/main/resources/
│   ├── application.yml               # 配置文件
│   └── static/output/                # 生成的表情包存储目录
├── pom.xml                           # Maven 配置
├── design.md                         # 设计文档
└── README.md                         # 本文件
```

## 功能说明

### 图片处理
- 支持 JPG、PNG 格式
- 最大文件大小：10MB
- AI 生成情绪表情图片（基于上传的图片和选择的情绪）
- 如果提供了自定义文字，会在生成的图片底部添加留白区域并绘制文字

### 情绪类型
支持以下 9 种情绪类型：
- 😊 **高兴** (happy)
- 😢 **伤心** (sad)
- 😠 **生气** (angry)
- 😲 **惊讶** (surprised)
- 😕 **困惑** (confused)
- 🤩 **兴奋** (excited)
- 😌 **平静** (calm)
- 😳 **害羞** (shy)
- 😜 **调皮** (playful)

### 自定义文字功能
- **可选功能**：用户可以选择是否在生成的图片上添加自定义文字
- **文字样式自定义**：
  - **位置**：顶部、中间、底部（可自定义）
  - **颜色**：自定义文字颜色（RGB格式）
  - **描边**：自定义描边颜色和宽度
  - **字体大小**：20-100px 可调
  - **字体**：黑体（SimHei）
  - **背景透明**：文字直接叠加在原图上，不遮挡图片内容
  - **自动换行**：文字过长时自动换行
- **使用场景**：
  - 添加表情包文案
  - 添加说明文字
  - 添加个性化标签

### 滤镜特效功能
- **10+ 种滤镜效果**：
  - **无滤镜**：保持原图
  - **黑白**：经典黑白效果
  - **复古**：怀旧复古风格
  - **明亮**：增强亮度
  - **暗调**：降低亮度
  - **暖色**：暖色调滤镜
  - **冷色**：冷色调滤镜
  - **怀旧**：棕褐色怀旧效果
  - **高对比**：增强对比度
  - **高饱和**：增强饱和度
- **使用方式**：在生成前选择滤镜，滤镜会应用到 AI 生成的图片上

### AI 图片生成
- 使用阿里云 DashScope 的 `qwen-image` 模型
- 基于上传的图片和选择的情绪生成对应的表情图片
- 支持返回 OSS URL 或本地保存

## 常见问题

### 1. 字体显示问题
如果系统没有黑体（SimHei），Java 会自动使用默认字体。可以在 `ImageComposeService.java` 中修改字体设置。

### 2. API Key 配置
如果没有 OpenAI API Key，系统会自动使用模拟数据，不影响功能测试。

### 3. 端口冲突
如果 8080 端口被占用，可以在 `application.yml` 中修改 `server.port`。

### 4. 图片访问 404
确保 `output/` 目录（项目根目录）存在，并且 Spring Boot 的静态资源路径配置正确。

### 5. 自定义文字不显示
- 确保输入了文字（不能为空）
- 文字长度建议不超过 50 个字符
- 如果生成的图片是 OSS URL，系统会自动下载后再添加文字

## 后续扩展方向

- ✅ 自定义文字功能（已完成）
- 自定义文字样式（颜色、大小、位置、字体）
- 批量处理多张图片
- 集成其他 AI 模型
- 云存储集成
- 文字位置自定义（顶部、中部、底部）
- 多行文字布局优化

## 许可证

MIT License

