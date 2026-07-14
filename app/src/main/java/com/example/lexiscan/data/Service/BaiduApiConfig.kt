package com.example.lexiscan.data.Service

/**
 * API 密钥与端点集中配置
 *
 * 当前识别流程（两步走）：
 * 1. 图片 → 硅基流动 Qwen3-VL 视觉模型 → 输出中文物品名 JSON
 * 2. 中文物品名 → DeepSeek LLM → 翻译标准化为英文 + 音标 + 复数 + 例句
 *
 * 百度图像识别 API 已弃用，保留密钥备用
 */
object BaiduApiConfig {

    // ── 硅基流动 SiliconFlow API ──
    // 用于调用 Qwen3-VL 视觉模型，识别图片中的物品（输出中文物品名）
    // 平台地址：https://cloud.siliconflow.cn/
    const val SILICONFLOW_API_KEY = "-"
    const val SILICONFLOW_BASE_URL = "https://api.siliconflow.cn/"
    const val SILICONFLOW_VL_MODEL = "Qwen/Qwen3-VL-30B-A3B-Thinking"

    // ── DeepSeek LLM API ──
    // 用于将中文物品名翻译标准化为英文单词 + 音标 + 复数 + 例句
    // 注意：请在此处填入你自己的 DeepSeek API 密钥，切勿将真实密钥提交到公开仓库
    const val DEEPSEEK_API_KEY = "YOUR_DEEPSEEK_API_KEY"
    const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/"

    // ── 百度图像识别密钥（已弃用，保留备用）──
    const val API_KEY = "-"
    const val SECRET_KEY = "-"
    const val BASE_URL = "https://aip.baidubce.com"
}