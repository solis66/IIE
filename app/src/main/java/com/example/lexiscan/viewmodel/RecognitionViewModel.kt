package com.example.lexiscan.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lexiscan.data.Service.provider.DeepSeekVisionProvider
import com.example.lexiscan.data.Service.provider.ProviderResult
import com.example.lexiscan.data.Service.provider.RecognitionProvider
import com.example.lexiscan.data.model.*
import com.example.lexiscan.data.repository.RecognitionRepository
// ── 图像分割模块（U2-Net TFLite 本地模型）──
import com.example.lexiscan.data.segmentation.ImageSegmenter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScreenState {
    object Loading : ScreenState()
    data class Success(val results: List<RecognitionRecord>) : ScreenState()
    data class Error(val message: String) : ScreenState()
}

sealed class RecognitionState {
    object Idle : RecognitionState()
    object Recognizing : RecognitionState()
    data class Success(val result: ObjectRecognitionResult) : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}

class RecognitionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecognitionRepository = RecognitionRepository(application)

    // 识别服务：DeepSeek-VL2 视觉识别 + DeepSeek LLM 翻译（两步走）
    // Step1: 图片 → 硅基流动 DeepSeek-VL2 → 中文物品名 JSON
    // Step2: 中文物品名 → DeepSeek LLM → 英文+音标+复数+例句
    private val recognitionProvider: RecognitionProvider = DeepSeekVisionProvider(application)

    // 图像分割器：U2-Net TFLite 本地模型，将物品从背景中分割出来
    private val imageSegmenter: ImageSegmenter = ImageSegmenter(application)

    private val _historyState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val historyState: StateFlow<ScreenState> = _historyState

    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn

    private val _isCollected = MutableStateFlow(false)
    val isCollected: StateFlow<Boolean> = _isCollected

    private val _currentImageUri = MutableStateFlow<Uri?>(null)
    val currentImageUri: StateFlow<Uri?> = _currentImageUri

    private val _animeImageUri = MutableStateFlow<Uri?>(null)
    val animeImageUri: StateFlow<Uri?> = _animeImageUri

    private val _phonetic = MutableStateFlow<String?>(null)
    val phonetic: StateFlow<String?> = _phonetic

    private val _chineseName = MutableStateFlow<String?>(null)
    val chineseName: StateFlow<String?> = _chineseName

    private val _plural = MutableStateFlow<String?>(null)
    val plural: StateFlow<String?> = _plural

    private val _exampleSentence = MutableStateFlow<String?>(null)
    val exampleSentence: StateFlow<String?> = _exampleSentence

    private val _isEnglishValid = MutableStateFlow(true)
    val isEnglishValid: StateFlow<Boolean> = _isEnglishValid

    // 标记图片是否成功分割（抠图），UI 据此决定是否展示贴纸效果
    private val _isSegmented = MutableStateFlow(false)
    val isSegmented: StateFlow<Boolean> = _isSegmented

    // 分割完成后的前景图 Uri（透明背景），用于撕拉动画中"露出"抠图结果
    // 在分割完成时立即设置，CameraScreen 的 TearScanOverlay 据此切换为"露出物品"模式
    private val _segmentedForegroundUri = MutableStateFlow<Uri?>(null)
    val segmentedForegroundUri: StateFlow<Uri?> = _segmentedForegroundUri

    companion object {
        private const val TAG = "RecognitionVM"
    }

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _historyState.value = ScreenState.Loading
            try {
                val results = repository.getHistory()
                _historyState.value = ScreenState.Success(results)
            } catch (e: Exception) {
                _historyState.value = ScreenState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun recognizeObject(imageUri: Uri) {
        viewModelScope.launch {
            _currentImageUri.value = imageUri
            _recognitionState.value = RecognitionState.Recognizing
            // 重置分割状态（新一轮识别）
            _segmentedForegroundUri.value = null
            _isSegmented.value = false
            try {
                // ===================================================================
                // 串行流程（仿 CapWords）：先分割 → 再传前景图给 API
                //
                // 原因：百度 API 收到"背景干扰消除"的前景图后，识别准确率大幅提升
                // 撕拉动画在 Recognizing 全程播放，覆盖分割+API 两步等待时间
                // ===================================================================

                // === Step 1: 先分割（拿到透明前景 Bitmap）===
                Log.d(TAG, "Step 1: 开始图像分割...")
                val segmentedBitmap = imageSegmenter.segment(imageUri)

                // === Step 2: 处理分割结果，决定传给 API 的图片 ===
                val foregroundUri: Uri?      // 透明前景图 Uri（用于撕拉动画 + 结果页贴纸）
                val apiImageUri: Uri         // 传给百度 API 的图片 Uri

                if (segmentedBitmap != null) {
                    // 分割成功：保存透明前景图（用于撕拉动画 + 贴纸展示）
                    foregroundUri = saveBitmapToTempFile(segmentedBitmap)
                    if (foregroundUri != null) {
                        Log.d(TAG, "分割成功，已保存透明前景图: $foregroundUri")
                        _isSegmented.value = true
                        // 立即通知 UI：前景图已就绪，撕拉动画切换为"露出物品"模式
                        _segmentedForegroundUri.value = foregroundUri

                        // 合成白底前景图 + 裁剪到内容边界，提升 API 识别准确率
                        val whiteBgBitmap = cropAndCompositeOnWhite(segmentedBitmap)
                        apiImageUri = saveBitmapToTempFile(whiteBgBitmap) ?: imageUri
                        Log.d(TAG, "已生成白底前景图供 API 识别: $apiImageUri")
                    } else {
                        Log.w(TAG, "前景图保存失败，API 降级使用原图")
                        // foregroundUri 已是 null（saveBitmapToTempFile 返回 null），无需重复赋值
                        apiImageUri = imageUri
                    }
                } else {
                    Log.w(TAG, "分割失败，API 降级使用原图")
                    foregroundUri = null
                    apiImageUri = imageUri
                }

                // === Step 3: 调用百度 API 识别（用前景图或原图）===
                Log.d(TAG, "Step 3: 调用 API 识别, apiImageUri=$apiImageUri")
                val result = recognitionProvider.recognize(apiImageUri)

                _phonetic.value = result.phonetic
                _chineseName.value = result.chineseName
                _plural.value = result.plural
                _exampleSentence.value = result.exampleSentence

                val itemName = result.name

                // DeepSeek 失败时原始名称可能是中文，UI 展示需做区分
                val isEnglishResult = !itemName.any { it in '\u4e00'..'\u9fff' }
                _isEnglishValid.value = isEnglishResult

                // === Step 4: 设置最终展示图（透明前景图 > 原图）===
                val finalImageUri = foregroundUri ?: imageUri
                _animeImageUri.value = finalImageUri

                // === Step 5: 进入成功状态 ===
                val objResult = ObjectRecognitionResult(
                    itemName = ItemName(
                        english = itemName,
                        chinese = result.chineseName ?: "",
                        phonetic = result.phonetic,
                        plural = result.plural,
                        exampleSentence = result.exampleSentence
                    ),
                    boundingBox = BoundingBox(0, 0, 0, 0),
                    confidence = result.confidence,
                    category = null,
                    description = null
                )
                _recognitionState.value = RecognitionState.Success(objResult)

                // === Step 6: 保存记录 ===
                repository.addRecord(
                    RecognitionRecord(
                        id = System.currentTimeMillis().toString(),
                        imageUri = finalImageUri.toString(),
                        englishName = itemName,
                        chineseName = result.chineseName ?: "",
                        phonetic = result.phonetic,
                        plural = result.plural,
                        exampleSentence = result.exampleSentence,
                        confidence = result.confidence,
                        category = null,
                        recognitionMode = "SINGLE_OBJECT",
                        timestamp = System.currentTimeMillis()
                    )
                )

                loadHistory()
            } catch (e: BaiduApiError) {
                Log.e(TAG, "识别失败[${e.javaClass.simpleName}]: ${e.message}", e)
                _recognitionState.value = RecognitionState.Error(e.message ?: "识别失败")
            } catch (e: Exception) {
                Log.e(TAG, "识别异常: ${e.message}", e)
                _recognitionState.value = RecognitionState.Error("异常: ${e.message ?: "未知错误"}")
            }
        }
    }

    fun setTorchOn(on: Boolean) {
        _isTorchOn.value = on
    }

    fun toggleCollection() {
        _isCollected.value = !_isCollected.value
    }

    fun resetRecognitionState() {
        _recognitionState.value = RecognitionState.Idle
        _isCollected.value = false
        _animeImageUri.value = null
        _phonetic.value = null
        _chineseName.value = null
        _plural.value = null
        _exampleSentence.value = null
        _isEnglishValid.value = true
        _isSegmented.value = false
        _segmentedForegroundUri.value = null
    }

    /**
     * 将透明前景图裁剪到内容边界并合成到白色背景上
     *
     * 目的：
     * 1. 裁剪掉周围大量透明区域，让物品占据图片主体 → API 识别更准
     * 2. 合成白色背景 → 消除背景干扰，API 只关注物品本身
     *
     * @param source 分割后的透明前景 Bitmap
     * @return 白底前景 Bitmap（已裁剪到内容边界 + 留 20px 边距）
     */
    private fun cropAndCompositeOnWhite(source: Bitmap): Bitmap {
        // === 1. 找到非透明像素的边界（bounding box）===
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixels[y * width + x] ushr 24 > 10) { // alpha > 10 视为前景
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        // 全透明或无效 → 直接合成白底返回
        if (maxX < minX || maxY < minY) {
            return compositeOnWhite(source, 0, 0, width, height)
        }

        // 留 20px 边距，避免物品贴边
        val padding = 20
        val left = (minX - padding).coerceAtLeast(0)
        val top = (minY - padding).coerceAtLeast(0)
        val right = (maxX + padding).coerceAtMost(width)
        val bottom = (maxY + padding).coerceAtMost(height)

        return compositeOnWhite(source, left, top, right, bottom)
    }

    /**
     * 将 Bitmap 指定区域合成到白色背景上
     */
    private fun compositeOnWhite(source: Bitmap, left: Int, top: Int, right: Int, bottom: Int): Bitmap {
        val cropW = right - left
        val cropH = bottom - top
        if (cropW <= 0 || cropH <= 0) {
            // 退化：全图合成白底
            val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(source, 0f, 0f, null)
            return result
        }
        // 裁剪到前景区域
        val cropped = Bitmap.createBitmap(source, left, top, cropW, cropH)
        // 合成白底
        val result = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(cropped, 0f, 0f, null)
        return result
    }

    /**
     * 将分割后的 Bitmap 保存为应用缓存目录下的临时 PNG 文件
     * - PNG 格式保留 alpha 透明通道
     * - 文件名含时间戳避免冲突
     * - 返回 Uri 供 Coil 加载展示
     *
     * @param bitmap 分割后的前景 Bitmap（透明背景）
     * @return 临时文件 Uri，失败返回 null
     */
    private fun saveBitmapToTempFile(bitmap: Bitmap): Uri? {
        return try {
            // AndroidViewModel.application 是 private，需通过 getApplication() 访问
            val cacheDir = getApplication<Application>().cacheDir
            val tempFile = File(cacheDir, "segmented_${System.currentTimeMillis()}.png")
            val outStream = FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
            outStream.close()
            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            Log.e(TAG, "保存分割图失败: ${e.message}", e)
            null
        }
    }

    /**
     * ViewModel 销毁时释放分割器资源
     * - 关闭 TFLite Interpreter，释放本地模型内存
     */
    override fun onCleared() {
        super.onCleared()
        imageSegmenter.close()
    }
}
