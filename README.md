# LexiScan · 撕拉识物

> 拍一拍，撕一撕，认识身边的英语单词 —— 基于 U2-Net 本地分割 + 大模型识图翻译的 Android 英语学习应用。

LexiScan 将「拍照识物」与「英语单词学习」结合：用户对准物品拍照后，应用以**撕拉动画**抠出前景物体，再通过视觉大模型识别物品并翻译为标准英文单词，附带音标、复数、例句与发音，帮助用户在真实场景中积累词汇。

---

## 核心特性

- **本地图像分割** —— U2-Net TFLite 模型完全离线运行，无需联网即可抠出前景物品为透明 PNG，不依赖 Google Play 服务。
- **撕拉扫描交互** —— 仿 CapWord 的自上而下撕拉动画，配合震动反馈与前景弹出动效，让抠图过程直观有趣。
- **大模型识图翻译** —— 硅基流动 Qwen3-VL 视觉模型识别物品（输出中文名），DeepSeek LLM 将其标准化为英文单词 + 音标 + 复数 + 例句。
- **容错回退** —— DeepSeek 失败时自动回退至 dictionaryapi.dev 查询音标，保证核心体验可用。
- **贴纸式结果展示** —— 抠出的物品以贴纸形式叠加于结果页背景，四角直角顶点选框，视觉干净。
- **TTS 发音** —— Android TextToSpeech 朗读英文单词，支持反复播放。
- **历史记录** —— Room 数据库持久化所有识别记录，可随时回顾复习。
- **学士狐吉祥物** —— 贯穿应用的「学士狐」形象，含招呼、思考、学习、开心等多种表情。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material 3 |
| 相机 | CameraX 1.3.2 |
| 本地分割 | TensorFlow Lite 2.16.1 + U2-Net（`u2netp.tflite`） |
| 网络请求 | Retrofit 2.9.0 + OkHttp 4.12.0 |
| 视觉识图 | 硅基流动 SiliconFlow · Qwen3-VL-30B-A3B-Thinking |
| 文本翻译 | DeepSeek LLM API |
| 语音 | Android TextToSpeech |
| 持久化 | Room + 文件存储 |
| 构建 | Gradle 8.7 (Kotlin DSL) |
| 最低 SDK | Android 12 (API 31) |
| 目标 SDK | Android 15 (API 35) |

---

## 识别工作流

```
拍照 / 选图
    │
    ▼
┌─────────────────────────────────┐
│  1. U2-Net TFLite 本地显著性分割  │  完全离线，输出透明 PNG 前景
│     ImageSegmenter.segment()    │
└───────────────┬─────────────────┘
                ▼
┌─────────────────────────────────┐
│  2. 前景合成白底图 + Base64 编码  │
└───────────────┬─────────────────┘
                ▼
┌─────────────────────────────────┐
│  3. Qwen3-VL 视觉识图 (SSE 流式)  │  输出中文物品名
│     SiliconFlow API             │
└───────────────┬─────────────────┘
                ▼
┌─────────────────────────────────┐
│  4. DeepSeek LLM 翻译标准化      │  英文单词 + 音标 + 复数 + 例句
└───────────────┬─────────────────┘
                ▼ (失败回退)
┌─────────────────────────────────┐
│  5. dictionaryapi.dev 音标查询   │  兜底方案
└───────────────┬─────────────────┘
                ▼
     保存至 Room 数据库 → 结果页展示
```

---

## 项目结构

```
IIE/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── assets/
│       │   └── u2netp.tflite              # U2-Net 分割模型（离线打包）
│       ├── java/com/example/lexiscan/
│       │   ├── MainActivity.kt            # 入口 + Compose 导航
│       │   ├── data/
│       │   │   ├── Service/
│       │   │   │   ├── BaiduApiConfig.kt  # API 密钥集中配置
│       │   │   │   ├── BaiduApiService.kt # 百度识图接口（已弃用，备用）
│       │   │   │   ├── DeepSeekService.kt # DeepSeek LLM 接口
│       │   │   │   ├── RetrofitClient.kt  # Retrofit 实例
│       │   │   │   └── provider/          # 识图策略提供者
│       │   │   ├── camera/
│       │   │   │   └── CameraController.kt
│       │   │   ├── model/                 # 数据模型
│       │   │   ├── repository/
│       │   │   │   └── RecognitionRepository.kt
│       │   │   └── segmentation/
│       │   │       └── ImageSegmenter.kt  # U2-Net TFLite 分割核心
│       │   ├── ui/
│       │   │   ├── components/LexiMascot.kt
│       │   │   ├── screens/
│       │   │   │   ├── CameraScreen.kt            # 相机 + 撕拉动画
│       │   │   │   ├── HomeScreen.kt              # 首页 + 历史
│       │   │   │   ├── HistoryDetailScreen.kt     # 历史详情
│       │   │   │   └── RecognitionResultDialog.kt # 结果展示
│       │   │   └── theme/
│       │   ├── util/TtsManager.kt
│       │   └── viewmodel/
│       │       └── RecognitionViewModel.kt        # 工作流编排
│       └── res/                          # 资源（图标、吉祥物表情等）
├── sam2_server/                          # SAM2 Python 分割服务端（备选）
│   ├── server.py
│   ├── requirements.txt
│   ├── start.bat / stop.bat
│   └── update_url.py
└── 学士狐-*.png                          # 吉祥物设计源文件
```

---

## 快速开始

### 环境要求

- Android Studio (Hedgehog 或更高)
- JDK 17
- Android SDK 35
- 一台 Android 12+ 真机（模拟器相机功能有限）

### 配置 API 密钥

编辑 [BaiduApiConfig.kt](app/src/main/java/com/example/lexiscan/data/Service/BaiduApiConfig.kt)，填入你自己的密钥：

```kotlin
object BaiduApiConfig {
    // 硅基流动 SiliconFlow（Qwen3-VL 视觉识图）
    const val SILICONFLOW_API_KEY = "你的 SiliconFlow 密钥"
    const val SILICONFLOW_VL_MODEL = "Qwen/Qwen3-VL-30B-A3B-Thinking"

    // DeepSeek LLM（翻译标准化）
    const val DEEPSEEK_API_KEY = "你的 DeepSeek 密钥"
}
```

> **安全提示**：切勿将真实 API 密钥提交到公开仓库。本仓库已将密钥替换为占位符。

### 构建运行

```bash
# Windows
.\gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

### （可选）启动 SAM2 服务端

如需使用更高精度的云端分割替代本地 U2-Net：

```bash
cd sam2_server
pip install -r requirements.txt
# Windows
start.bat
# 或手动启动
uvicorn server:app --host 0.0.0.0 --port 8800
```

首次运行会自动下载 `facebook/sam2.1-hiera-small` 模型。

---

## 关键技术说明

- **U2-Net 输出处理**：TFLite 输出张量形状为 `[1, 320, 320, 1]`，使用 `ByteBuffer` 读取以避免维度错配；掩膜采用 `ARGB_8888` 格式，alpha 值移至最高 8 位（`0xAA000000`）以保留透明通道。
- **TFLite 不压缩**：`build.gradle.kts` 中配置 `noCompress += listOf("tflite")`，确保模型可被 `ByteBuffer` 内存映射加载。
- **SSE 流式响应**：Qwen3-VL 识图采用 Server-Sent Events 流式传输，在提取到物品名后即终止连接以节省配额。
- **撕拉动画**：`TearScanOverlay` 使用 `Animatable.animateTo` 实现 2000ms 自上而下单次撕拉，白底覆盖撕拉线上方，三层撕拉线（阴影 + 主线 + 光晕），150ms 间隔震动反馈，前景弹出采用 spring 弹簧动画。

---

## 版本标签

| 标签 | 说明 |
|------|------|
| `v0.1.0-cleanup` | 仓库清理：配置 .gitignore，移除构建产物跟踪 |
| `v0.2.0-lexiscan-app` | LexiScan 物品识别应用完整实现 |
| `v0.3.0-sam2-server` | SAM2 Python 图像分割服务端 |
| `v1.0.0-readme` | 完整 README 项目文档 |

---

## 许可

本项目仅供学习交流使用。
