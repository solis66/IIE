package com.example.lexiscan.data.Service.provider

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.lexiscan.data.Service.BaiduApiConfig
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Qwen3-VL 视觉识别提供者（通过硅基流动 SiliconFlow API）
 *
 * 完整工作流程（两步走）：
 *
 * 1. 视觉识别（Qwen3-VL-30B-A3B-Thinking）：
 *    将拍照图片转为 base64，发送给硅基流动平台的 Qwen3-VL 视觉模型，
 *    要求模型输出 JSON 固定格式的中文物品名，如 {"itemName": "苹果"}
 *
 * 2. 翻译标准化（DeepSeek LLM）：
 *    将中文物品名传给 DeepSeek LLM（DeepSeekService.standardizeWord），
 *    翻译标准化为标准英文单词 + 中文释义 + UK IPA 音标 + 复数形式 + 少儿例句
 *
 * API 平台：硅基流动 SiliconFlow（OpenAI 兼容格式）
 * 视觉模型：Qwen/Qwen3-VL-30B-A3B-Thinking
 *
 * @param context Android Context，用于读取图片文件
 */
class DeepSeekVisionProvider(context: Context) : BaseBaiduProvider(context) {

    companion object {
        private const val TAG = "Qwen3VLProvider"
        // 硅基流动 chat completions 端点（OpenAI 兼容）
        private const val CHAT_URL = "${BaiduApiConfig.SILICONFLOW_BASE_URL}v1/chat/completions"
        // 视觉模型名称：Qwen3-VL-30B-A3B-Thinking（Thinking 模型，先思考再回答）
        private const val MODEL = BaiduApiConfig.SILICONFLOW_VL_MODEL
    }

    private val gson = Gson()

    // HTTP 客户端：Thinking 模型推理时间较长（先思考再回答），超时设为 90s
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 识别图片中的物品（对外接口，由 ViewModel 调用）
     *
     * 流程：
     * 1. 图片 → base64 编码
     * 2. base64 → Qwen3-VL 视觉模型 → 中文物品名 JSON
     * 3. 中文物品名 → DeepSeek LLM → 英文+音标+复数+例句
     *
     * @param imageUri 拍照/选择的图片 Uri
     * @return 识别+翻译后的完整结果（英文单词、中文释义、音标、复数、例句）
     */
    override suspend fun recognize(imageUri: Uri): ProviderResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== Qwen3-VL 视觉识别流程开始 === imageUri=$imageUri")

        // === Step 1: 将图片转为 base64（复用基类方法，内含尺寸压缩+质量调整）===
        val imageBase64 = convertUriToBase64(imageUri)
        Log.d(TAG, "Step 1 完成: 图片 base64 编码 ${imageBase64.length / 1024}KB")

        // === Step 2: 调用硅基流动 Qwen3-VL 视觉模型，识别图片中的物品 ===
        // 要求模型输出 JSON 固定格式的中文物品名
        // 出错时（HTTP 错误、API Key 无效、额度不足等）会直接抛出 InvalidResponse 异常
        val rawName = recognizeWithVL(imageBase64)
        Log.d(TAG, "Step 2 完成: Qwen3-VL 识别结果 = $rawName")

        // === Step 3: 将中文物品名传给 DeepSeek LLM 进行翻译标准化 ===
        // standardizeWithLLM 继承自 BaseBaiduProvider，内部调用 DeepSeekService
        // 输入: 中文物品名（如"苹果"）
        // 输出: 标准英文(apple) + 中文释义 + 音标 + 复数 + 例句
        val std = standardizeWithLLM(rawName)
        Log.d(TAG, "Step 3 完成: en=${std.english}, cn=${std.chinese}, phonetic=${std.phonetic}, source=${std.source}")

        // 组装最终结果返回给 ViewModel
        ProviderResult(
            name = std.english,
            confidence = 0.95f,
            phonetic = std.phonetic,
            chineseName = std.chinese,
            plural = std.plural,
            exampleSentence = std.exampleSentence
        )
    }

    /**
     * 调用硅基流动 Qwen3-VL 视觉模型，识别图片中的核心物品
     *
     * 请求格式（OpenAI 兼容）：
     * - content 数组包含 image_url（base64 data URI）+ text（识别指令）
     * - 要求模型输出 JSON：{"itemName": "物品中文名"}
     * - temperature=0.1 确保结果稳定可复现
     *
     * 注意：Qwen3-VL-30B-A3B-Thinking 是 Thinking 模型，会先思考再回答。
     * 思考过程通常在 reasoning_content 字段，最终回答在 content 字段。
     * 但为安全起见，parseItemName 也会清理可能残留的 <think> 标签。
     *
     * @param imageBase64 图片的 base64 编码字符串（JPEG 格式，无 data URI 前缀）
     * @return 中文物品名称
     * @throws com.example.lexiscan.data.model.BaiduApiError.InvalidResponse 当 API 调用失败时抛出，包含具体错误信息
     */
    private suspend fun recognizeWithVL(imageBase64: String): String = withContext(Dispatchers.IO) {
        try {
            // 构建 OpenAI 兼容的请求体（硅基流动完全兼容此格式）
            // content 数组：先传图片，再传文字指令
            val requestJson = gson.toJson(mapOf(
                "model" to MODEL,
                "stream" to false,          // 非流式，等待完整响应
                "max_tokens" to 2048,       // Thinking 模型思考+回答都需要 token，设大一些
                "temperature" to 0.1,       // 低温度确保结果稳定
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to listOf(
                            // 图片输入：data URI 格式（base64 内嵌）
                            mapOf(
                                "type" to "image_url",
                                "image_url" to mapOf(
                                    "url" to "data:image/jpeg;base64,$imageBase64"
                                )
                            ),
                            // 文字指令：要求输出 JSON 固定格式的中文物品名
                            // 规则：去品牌/型号/颜色，取通用名，组合物品取上位概念
                            mapOf(
                                "type" to "text",
                                "text" to "识别图中的核心物品。严格遵循以下规则：\n" +
                                    "1. 仅输出一个 JSON 对象，不输出任何其他文字、解释或标记。\n" +
                                    "2. JSON 格式：{\"itemName\": \"物品中文名\"}\n" +
                                    "3. 物品名取最通用的中文日常名称，去掉品牌、型号、品种、颜色、材质等修饰词。\n" +
                                    "4. 组合物品取其上位概念。\n" +
                                    "示例：\n" +
                                    "图中是红富士苹果 → {\"itemName\": \"苹果\"}\n" +
                                    "图中是戴尔笔记本 → {\"itemName\": \"笔记本电脑\"}\n" +
                                    "图中是星巴克拿铁 → {\"itemName\": \"咖啡\"}\n" +
                                    "图中是iPhone充电线 → {\"itemName\": \"数据线\"}"
                            )
                        )
                    )
                )
            ))

            Log.d(TAG, "发送 Qwen3-VL 请求, model=$MODEL, bodyLength=${requestJson.length}")

            val requestBody = requestJson.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(CHAT_URL)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer ${BaiduApiConfig.SILICONFLOW_API_KEY}")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                // HTTP 错误：透传具体状态码和错误响应体，便于排查
                // 常见：401=API Key 无效，404=模型名错误，429=额度不足
                val errorBody = response.body?.string() ?: "(empty)"
                val code = response.code
                response.close()
                Log.e(TAG, "Qwen3-VL 请求失败: HTTP $code, body=${errorBody.take(500)}")
                throw com.example.lexiscan.data.model.BaiduApiError.InvalidResponse(
                    "Qwen3-VL 请求失败 (HTTP $code): ${errorBody.take(200)}"
                )
            }

            val body = response.body?.string()
            response.close()

            if (body == null) {
                throw com.example.lexiscan.data.model.BaiduApiError.InvalidResponse(
                    "Qwen3-VL 响应体为空"
                )
            }

            Log.d(TAG, "Qwen3-VL 响应: ${body.take(500)}")

            // 解析 OpenAI 兼容的响应格式：choices[0].message.content
            val json = JsonParser.parseString(body).asJsonObject
            val choices = json.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) {
                // choices 为空通常是 API 返回了错误信息，提取 message 字段
                val errorMsg = json.get("message")?.asString ?: body.take(200)
                throw com.example.lexiscan.data.model.BaiduApiError.InvalidResponse(
                    "Qwen3-VL 返回错误: $errorMsg"
                )
            }

            val content = choices[0].asJsonObject
                .getAsJsonObject("message")
                .get("content").asString

            Log.d(TAG, "Qwen3-VL content: ${content.take(300)}")

            // 从模型返回内容中提取物品名称
            // Thinking 模型的思考过程在 reasoning_content 字段，content 是最终回答
            // 但为安全起见，parseItemName 也会清理可能残留的 <think> 标签
            parseItemName(content) ?: throw com.example.lexiscan.data.model.BaiduApiError.InvalidResponse(
                "Qwen3-VL 未能识别物品 (content=${content.take(200)})"
            )
        } catch (e: com.example.lexiscan.data.model.BaiduApiError) {
            // 已知业务异常，直接向上抛出
            throw e
        } catch (e: Exception) {
            // 未知异常（网络超时、JSON 解析失败等），包装后抛出
            Log.e(TAG, "Qwen3-VL 调用异常: ${e.javaClass.simpleName}: ${e.message}", e)
            throw com.example.lexiscan.data.model.BaiduApiError.InvalidResponse(
                "Qwen3-VL 调用异常: ${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    /**
     * 从模型返回的内容中提取物品名称
     *
     * 做容错处理，支持多种返回格式：
     * - Thinking 模型思考过程：<think>思考...</think>回答 → 提取 </think> 之后的内容
     * - 标准 JSON：{"itemName": "苹果"}
     * - 带 markdown 代码块：```json\n{"itemName": "苹果"}\n```
     * - 纯文本物品名：苹果（模型未遵守 JSON 格式时降级）
     *
     * @param content 模型返回的原始内容
     * @return 物品名称字符串，失败返回 null
     */
    private fun parseItemName(content: String): String? {
        // Step 1: 清理 Thinking 模型的思考过程
        // Qwen3-VL-Thinking 可能在 content 中包含 <think>...</think> 标签
        // 提取 </think> 之后的内容作为实际回答
        var cleaned = content
        if (cleaned.contains("</think>")) {
            cleaned = cleaned.substringAfter("</think>", "").trim()
            Log.d(TAG, "已清理 Thinking 思考过程，提取最终回答: ${cleaned.take(100)}")
        }

        return try {
            // Step 2: 清理可能的 markdown 代码块标记
            val cleanJson = cleaned
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            // Step 3: 尝试解析为 JSON 并提取 itemName 字段
            val json = JsonParser.parseString(cleanJson).asJsonObject
            val itemName = json.get("itemName")?.asString

            if (itemName.isNullOrEmpty()) {
                // JSON 解析成功但没有 itemName 字段，降级使用清理后的原始内容
                Log.w(TAG, "JSON 中无 itemName 字段, content=$cleaned")
                cleanJson.takeIf { it.isNotBlank() }
            } else {
                itemName
            }
        } catch (e: Exception) {
            // JSON 解析失败，降级：直接用清理后的内容作为物品名
            Log.w(TAG, "JSON 解析失败，降级使用原始内容: $cleaned")
            cleaned.trim().takeIf { it.isNotBlank() }
        }
    }
}
