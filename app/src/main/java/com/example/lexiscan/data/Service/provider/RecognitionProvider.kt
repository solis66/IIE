package com.example.lexiscan.data.Service.provider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.lexiscan.data.Service.BaiduApiConfig
import com.example.lexiscan.data.Service.DeepSeekService
import com.example.lexiscan.data.Service.RetrofitClient
import com.example.lexiscan.data.model.BaiduApiError
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.ByteArrayOutputStream

/**
 * 识别提供者返回的统一结果（含LLM标准化后的完整学习信息）
 */
data class ProviderResult(
    val name: String,
    val confidence: Float,
    val phonetic: String? = null,    // 英文音标，如 ˈæp.əl
    val chineseName: String? = null, // 中文名，如 苹果
    val plural: String? = null,      // 复数形式
    val exampleSentence: String? = null  // 少儿英语短句例句
)

/**
 * 识别提供者接口
 * 切换API只需修改 ViewModel 中一行代码
 */
interface RecognitionProvider {
    suspend fun recognize(imageUri: Uri): ProviderResult
}

/**
 * 百度 API 共享基类：access_token 获取、图片 base64 编码
 */
abstract class BaseBaiduProvider(protected val context: Context) : RecognitionProvider {

    protected val apiService = RetrofitClient.baiduApiService

    // DeepSeek LLM 服务（懒加载，节省内存）
    private val deepSeekService: DeepSeekService by lazy { DeepSeekService() }

    /** 截断长文本，只保留核心名称 */
    protected fun trimName(raw: String): String {
        // 去除常见冗余描述词，保留核心名称
        val trimmed = raw
            .replace(Regex("这是一[个张幅].*?[，,。]?|图中[是的有]?|可以看到|包含|里面有|这是一|这是|图中|图片中|图像中|画面中"), "")
            .replace(Regex("[，,。、！!？?；;：:].*$"), "")  // 截断标点后的内容
            .replace(Regex("\\(.*?\\)|（.*?）|<.*?>|「.*?」|【.*?】"), "")  // 去除括号内容
            .replace(Regex("\\s+"), " ")  // 合并多余空格
            .trim()
        // 如果仍然太长且包含中文，只取前10个字符
        return if (trimmed.length > 30) {
            // 尝试取第一个空格前的词
            val firstWord = trimmed.split(" ").firstOrNull()?.trim() ?: trimmed
            if (firstWord.length > 15) trimmed.take(15) else firstWord
        } else trimmed
    }

    companion object {
        private const val TAG = "BaiduProvider"
        // 降低最大尺寸防止真机 OOM：原图 4000x3000 → 下采样 ~3x → ~1333x1000（约 5MB 内存）
        private const val MAX_DIMENSION = 1920
        private const val MIN_DIMENSION = 64
        private const val MAX_BASE64_SIZE = 10 * 1024 * 1024
    }

    protected suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val response = apiService.getAccessToken(
            clientId = BaiduApiConfig.API_KEY,
            clientSecret = BaiduApiConfig.SECRET_KEY
        )
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.error != null) {
                throw BaiduApiError.ApiError(-1, body.errorDescription ?: "获取令牌失败")
            }
            body?.accessToken ?: throw BaiduApiError.InvalidResponse("访问令牌为空")
        } else {
            throw BaiduApiError.NetworkError(
                java.io.IOException("获取令牌失败: ${response.code()}")
            )
        }
    }

    protected suspend fun convertUriToBase64(uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw BaiduApiError.ImageProcessingError("无法打开图片: $uri")

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val srcW = options.outWidth
        val srcH = options.outHeight
        Log.d(TAG, "原始尺寸: ${srcW}x${srcH}")

        if (srcW <= 0 || srcH <= 0) {
            throw BaiduApiError.ImageProcessingError("无效尺寸: ${srcW}x${srcH}")
        }
        if (srcW < MIN_DIMENSION || srcH < MIN_DIMENSION) {
            throw BaiduApiError.ImageProcessingError("图片过小: ${srcW}x${srcH}")
        }

        val maxSide = maxOf(srcW, srcH)
        val sampleSize = if (maxSide > MAX_DIMENSION) {
            (maxSide.toFloat() / MAX_DIMENSION).toInt().coerceAtLeast(1)
        } else 1

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val stream2 = context.contentResolver.openInputStream(uri)
            ?: throw BaiduApiError.ImageProcessingError("无法二次读取")
        var bitmap = BitmapFactory.decodeStream(stream2, null, decodeOpts)
        stream2.close()
        if (bitmap == null) throw BaiduApiError.ImageProcessingError("图片解码失败")

        val origW = bitmap.width
        val origH = bitmap.height
        if (maxOf(origW, origH) > MAX_DIMENSION) {
            val scale = MAX_DIMENSION.toFloat() / maxOf(origW, origH)
            val fw = (origW * scale).toInt().coerceAtLeast(MIN_DIMENSION)
            val fh = (origH * scale).toInt().coerceAtLeast(MIN_DIMENSION)
            val m = Matrix().apply { postScale(fw.toFloat() / origW, fh.toFloat() / origH) }
            val scaled = Bitmap.createBitmap(bitmap, 0, 0, origW, origH, m, true)
            if (scaled != bitmap) { bitmap.recycle(); bitmap = scaled }
            Log.d(TAG, "缩放后: ${bitmap.width}x${bitmap.height}")
        }

        var quality = 85
        var base64: String
        while (true) {
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            val bytes = bos.toByteArray()
            bos.close()
            base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (base64.length <= MAX_BASE64_SIZE || quality <= 20) break
            quality -= 15
            Log.d(TAG, "base64 ${base64.length} 过大，降质到 $quality")
        }

        bitmap.recycle()
        if (base64.length > MAX_BASE64_SIZE) {
            throw BaiduApiError.ImageProcessingError("图片过大，base64=${base64.length / 1024}KB")
        }
        Log.d(TAG, "base64: ${base64.length / 1024}KB, q=$quality")
        base64
    }

    // ──────────────── 翻译 + 音标（已移除百度 MT，改用 DeepSeek 统一处理）───────────────

    protected suspend fun getEnglishPhonetic(word: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.dictionaryapi.dev/api/v2/entries/en/$word")
                .get()
                .build()
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val resp = client.newCall(request).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null
            resp.close()
            val json = JsonParser.parseString(body).asJsonArray
            if (json.size() > 0) {
                json[0].asJsonObject.get("phonetic")?.asString
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * DeepSeek LLM 单词标准化（核心流程）
     * 流程：识别出的俗称 → LLM修正为标准四六级/少儿英文单词 + 音标 + 复数 + 例句
     *
     * 优先使用 DeepSeek 标准化，失败时自动降级到百度翻译+词典音标方案
     *
     * @param rawName 原始识别名称（可能是中文俗称或非标准英文名）
     * @return 标准化后的 ProviderResult 字段（standardEnglish, chineseMeaning, phonetic, plural, exampleSentence）
     */
    protected suspend fun standardizeWithLLM(rawName: String): StandardizationResult {
        return try {
            val word = deepSeekService.standardizeWord(rawName)
            if (word != null) {
                Log.d(TAG, "DeepSeek 标准化成功: ${word.standardEnglish}")
                StandardizationResult(
                    english = word.standardEnglish,
                    chinese = word.chineseMeaning,
                    phonetic = word.phonetic,
                    plural = word.plural,
                    exampleSentence = word.exampleSentence,
                    source = "DeepSeek"
                )
            } else {
                // DeepSeek 返回 null（查看 DeepSeekService 日志获取具体原因）
                Log.w(TAG, "DeepSeek 标准化返回 null，降级到 fallback。rawName=$rawName")
                fallbackStandardize(rawName)
            }
        } catch (e: Exception) {
            Log.w(TAG, "DeepSeek 异常: ${e.javaClass.simpleName}: ${e.message}，降级到 fallback")
            fallbackStandardize(rawName)
        }
    }

    /**
     * 降级方案：DeepSeek 调用失败时，仅通过词典API获取音标
     * 不包含翻译、复数、例句（这些只能由LLM提供）
     */
    private suspend fun fallbackStandardize(rawName: String): StandardizationResult {
        val isChinese = rawName.any { it in '\u4e00'..'\u9fff' }
        Log.w(TAG, "fallbackStandardize: rawName=$rawName, isChinese=$isChinese")
        if (isChinese) {
            Log.w(TAG, "中文名称无法翻译（已移除百度MT且DeepSeek不可用），返回原始中文")
            return StandardizationResult(
                english = rawName,
                chinese = rawName,
                phonetic = null,
                plural = null,
                exampleSentence = null,
                source = "RawName"
            )
        }
        val ph = try { getEnglishPhonetic(rawName) } catch (_: Exception) { null }
        Log.d(TAG, "fallback 词典音标: rawName=$rawName, phonetic=$ph")
        return StandardizationResult(
            english = rawName,
            chinese = "",
            phonetic = ph,
            plural = null,
            exampleSentence = null,
            source = "Dictionary"
        )
    }
}

/**
 * 单词标准化结果（内部数据结构，用于组合 ProviderResult）
 */
data class StandardizationResult(
    val english: String,
    val chinese: String,
    val phonetic: String?,
    val plural: String?,
    val exampleSentence: String?,
    val source: String   // "DeepSeek" | "BaiduTranslate" | "Dictionary"
)
