package com.example.lexiscan.data.Service

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 单词标准化结果（LLM返回的完整结构化数据）
 */
data class StandardizedWord(
    val standardEnglish: String,   // 标准化英文单词（CET-4/6或少儿词汇）
    val chineseMeaning: String,    // 中文释义
    val phonetic: String,          // 音标（英式IPA，如 /ˈæp.əl/）
    val plural: String,            // 复数形式
    val exampleSentence: String    // 少儿英语短句例句
)

// ──── DeepSeek API 请求/响应模型 ────

data class DeepSeekChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<DeepSeekMessage>,
    val temperature: Double = 0.3,
    val max_tokens: Int = 512
)

data class DeepSeekMessage(
    val role: String,
    val content: String
)

data class DeepSeekChatResponse(
    val id: String?,
    val choices: List<DeepSeekChoice>?
)

data class DeepSeekChoice(
    val message: DeepSeekMessage?,
    val finish_reason: String?
)

/**
 * DeepSeek-v4-Pro LLM 服务
 * 功能：将识别到的物品俗称标准化为标准英文单词，补充音标、复数、短句例句
 *
 * API文档: https://api-docs.deepseek.com/
 * 模型: deepseek-chat (DeepSeek-V3/R1系列)
 */
class DeepSeekService {

    companion object {
        private const val TAG = "DeepSeekService"
        // 使用 val 而非 const val：避免跨文件 const val 字符串模板导致的字节码内联异常
        private val CHAT_URL = "${BaiduApiConfig.DEEPSEEK_BASE_URL}v1/chat/completions"

        // 单词标准化系统提示词
        // 功能：识别物品俗称 → 标准英文 + 中文释义 + UK IPA音标 + 复数 + 少儿例句
        private val SYSTEM_PROMPT = """
You are an English vocabulary tutor for Chinese children (ages 6-12). You will receive a recognized object name — possibly Chinese, English, a brand name, or a colloquial term. Your job is to transform it into a teachable English vocabulary card.

## Output Rules

1. **Standard English**: The most common and basic English word for the object. Prefer words from:
   - Children's vocabulary (animals, fruits, household items, body parts, etc.)
   - CET-4 vocabulary list (College English Test Band 4)
   - Cambridge YLE (Young Learners English) vocabulary
   If the input is already a correct English word, keep it but lowercase it.

2. **Chinese Meaning**: The Chinese translation as a NOUN (not a description). Examples:
   - Input "苹果" → chineseMeaning: "苹果"
   - Input "mango" → chineseMeaning: "芒果"
   - Input "笔记本电脑" → chineseMeaning: "笔记本电脑"

3. **Phonetic**: UK IPA pronunciation enclosed in `/ /` slashes. Use a standard reference (Cambridge Dictionary). Examples:
   - `/ˈæp.əl/` for apple
   - `/ˈbʌt.ə.flaɪ/` for butterfly
   - `/dɒɡ/` for dog
   - `/ˈmaɪ.krə.skəʊp/` for microscope

4. **Plural**: The standard plural form. Rules:
   - Most nouns: add -s (cat → cats)
   - Ending in s/x/sh/ch/o: add -es (bus → buses, tomato → tomatoes)
   - Ending in consonant+y: change y to ies (baby → babies)
   - Irregular: use the standard form (child → children, mouse → mice)
   - Uncountable nouns: same as singular (water, rice, milk)
   - Always-provided: even proper nouns get a plural (e.g. "bicycle" → "bicycles")

5. **Example Sentence**: A SHORT, simple sentence suitable for a 6-12 year old Chinese child learning English. Requirements:
   - Use the singular form of the word as the main subject or object
   - Keep it under 12 words
   - Use simple present tense or present continuous
   - Avoid complex grammar (no passive voice, no relative clauses)
   - Examples:
     - "I eat an apple every day."
     - "The dog is running in the park."
     - "She has a yellow school bag."
     - "Can you see the butterfly?"

## Output Format

Return ONLY a single JSON object. No markdown, no code fences, no explanation:
{"standardEnglish":"apple","chineseMeaning":"苹果","phonetic":"/ˈæp.əl/","plural":"apples","exampleSentence":"I eat an apple every day."}

## Examples

Input: 苹果
Output: {"standardEnglish":"apple","chineseMeaning":"苹果","phonetic":"/ˈæp.əl/","plural":"apples","exampleSentence":"I eat an apple every day."}

Input: 红富士
Output: {"standardEnglish":"apple","chineseMeaning":"苹果","phonetic":"/ˈæp.əl/","plural":"apples","exampleSentence":"I eat an apple every day."}

Input: kitty cat
Output: {"standardEnglish":"cat","chineseMeaning":"猫","phonetic":"/kæt/","plural":"cats","exampleSentence":"The cat is sleeping on the sofa."}

Input: 自行车
Output: {"standardEnglish":"bicycle","chineseMeaning":"自行车","phonetic":"/ˈbaɪ.sɪ.kəl/","plural":"bicycles","exampleSentence":"I ride my bicycle to school."}

Input: dog
Output: {"standardEnglish":"dog","chineseMeaning":"狗","phonetic":"/dɒɡ/","plural":"dogs","exampleSentence":"The dog is very friendly."}
""".trimIndent()
    }

    private val gson = Gson()

    /**
     * 规范化 API Key：自动剥离网关前缀（如 "IIE:"、"openai:" 等）
     * 格式 "IIE:sk-xxx" → "sk-xxx"，直接调用官方 API 时不需要前缀
     */
    private fun normalizeApiKey(rawKey: String): String {
        val parts = rawKey.split(":", limit = 2)
        return if (parts.size == 2 && parts[1].startsWith("sk-")) {
            Log.d(TAG, "  API Key 归一化: ${parts[0]}:****${parts[1].takeLast(4)} → ****${parts[1].takeLast(4)}")
            parts[1]
        } else {
            rawKey
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * 将识别到的物品名称标准化为教学级英文单词
     *
     * @param rawName 原始识别结果（可能是中文俗称，如"苹果"、"红富士"）
     * @return 标准化后的单词信息，失败返回null（调用方应降级到原始结果）
     */
    suspend fun standardizeWord(rawName: String): StandardizedWord? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== DeepSeek 标准化请求开始 ===")
            Log.d(TAG, "  rawName = $rawName")
            Log.d(TAG, "  url = $CHAT_URL")
            Log.d(TAG, "  model = deepseek-chat")

            val userPrompt = "Standardize this object name: \"$rawName\""

            val request = DeepSeekChatRequest(
                messages = listOf(
                    DeepSeekMessage(role = "system", content = SYSTEM_PROMPT),
                    DeepSeekMessage(role = "user", content = userPrompt)
                )
            )

            val jsonBody = gson.toJson(request)
            // 记录请求体摘要（不记录完整 SYSTEM_PROMPT 以免日志过长）
            Log.d(TAG, "  reqBody size = ${jsonBody.length} chars")

            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val apiKey = normalizeApiKey(BaiduApiConfig.DEEPSEEK_API_KEY)

            val httpRequest = Request.Builder()
                .url(CHAT_URL)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .build()

            Log.d(TAG, "  发送HTTP请求...")
            val response = httpClient.newCall(httpRequest).execute()

            val httpCode = response.code
            // 无论成功失败，记录响应状态
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "(empty)"
                response.close()
                Log.e(TAG, "  HTTP $httpCode: ${response.message}")
                Log.e(TAG, "  errorBody = ${errorBody.take(500)}")
                return@withContext null
            }

            val body = response.body?.string()
            response.close()

            if (body == null) {
                Log.e(TAG, "  response body is null")
                return@withContext null
            }

            Log.d(TAG, "  HTTP 200, body = ${body.take(300)}")

            val chatResponse = gson.fromJson(body, DeepSeekChatResponse::class.java)
            if (chatResponse.choices.isNullOrEmpty()) {
                Log.e(TAG, "  choices is null/empty, full body = ${body.take(500)}")
                return@withContext null
            }

            val content = chatResponse.choices.first().message?.content
            if (content == null) {
                Log.e(TAG, "  message.content is null")
                return@withContext null
            }

            val word = parseWordJson(content)
            if (word != null) {
                Log.d(TAG, "  SUCCESS: en=${word.standardEnglish}, cn=${word.chineseMeaning}, ph=${word.phonetic}")
            } else {
                Log.e(TAG, "  parseWordJson failed, content=${content.take(300)}")
            }
            word
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "  DNS解析失败（无网络或域名错误）: ${e.message}")
            null
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "  连接/读取超时: ${e.message}")
            null
        } catch (e: javax.net.ssl.SSLHandshakeException) {
            Log.e(TAG, "  SSL握手失败（证书问题或代理拦截）: ${e.message}")
            null
        } catch (e: java.io.IOException) {
            Log.e(TAG, "  IO异常: ${e.javaClass.simpleName}: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "  未知异常: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    /**
     * 解析LLM返回的JSON内容为StandardizedWord
     * 做容错处理：清理markdown代码块标记
     */
    private fun parseWordJson(raw: String): StandardizedWord? {
        return try {
            // 清理可能的markdown代码块包裹 ```json ... ```
            val cleanJson = raw
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            gson.fromJson(cleanJson, StandardizedWord::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "解析LLM返回JSON失败: ${e.message}, raw=$raw")
            null
        }
    }
}
