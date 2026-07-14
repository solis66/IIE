package com.example.lexiscan.data.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * 图像分割模块 — 基于 U2-Net TFLite 本地模型
 *
 * 功能：将拍照的物品从背景中分割出来，生成透明背景的前景图，
 *       用于结果页贴纸展示，效果接近 Apple VisionKit。
 *
 * 模型：U2-Net (U2NETP 轻量版) TFLite
 *  - 专为显著性目标分割（Salient Object Detection）设计
 *  - 输入: 1×320×320×3 (NHWC, float32, ImageNet 归一化)
 *  - 输出: 7 个 sigmoid mask，d0 为最终融合的显著性 mask
 *  - 模型大小: 4.44 MB，打包在 APK 中（assets/u2netp.tflite）
 *  - 完全离线运行，无需联网，无需 Google Play 服务
 *
 * 优势（相比 ML Kit Subject Segmentation）：
 *  - 不依赖 Google Play 服务，无需下载模型
 *  - 在无网络环境下也能正常工作
 *  - 模型固定大小，不占用额外存储空间
 */
class ImageSegmenter(private val context: Context) {

    companion object {
        private const val TAG = "ImageSegmenter"
        private const val MODEL_FILE = "u2netp.tflite"
        private const val INPUT_SIZE = 320
        // 输入图最大边长，避免大图 OOM
        private const val MAX_INPUT_SIZE = 1024
        // U2-Net 使用 ImageNet 归一化
        private val NORM_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val NORM_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }

    /**
     * TFLite Interpreter（懒加载）
     * - 使用单线程避免内存压力
     * - 模型从 assets 加载，完全离线
     */
    private val interpreter: Interpreter by lazy {
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        Interpreter(loadModelFile(), options)
    }

    /**
     * 对外接口：分割图片中的物品（挂起函数）
     *
     * @param imageUri 原始图片 Uri
     * @return 分割后的前景 Bitmap（背景透明），失败返回 null
     *
     * 流程：
     * 1. 从 Uri 加载原图（限制最大边 1024，校正 EXIF 方向）
     * 2. 将原图缩放到 320×320，ImageNet 归一化
     * 3. TFLite 推理获取显著性 mask（d0 输出）
     * 4. 将 mask 缩放回原图尺寸，作为 alpha 通道合成前景图
     */
    suspend fun segment(imageUri: Uri): Bitmap? {
        return try {
            Log.d(TAG, "=== 开始 U2-Net 分割流程 === imageUri=$imageUri")

            // Step 1: 加载原图
            val originalBitmap = loadBitmapFromUri(imageUri) ?: run {
                Log.e(TAG, "无法从 Uri 加载图片: $imageUri")
                return null
            }
            Log.d(TAG, "原图加载成功: ${originalBitmap.width}x${originalBitmap.height}")

            // Step 2: 预处理 — 缩放到 320×320 并归一化
            val resizedBitmap = Bitmap.createScaledBitmap(
                originalBitmap, INPUT_SIZE, INPUT_SIZE, true
            )
            val inputData = bitmapToNormalizedFloatArray(resizedBitmap)
            Log.d(TAG, "预处理完成: 320x320, ImageNet 归一化")

            // Step 3: TFLite 推理
            // 模型输出 7 个 mask (d0-d6)，每个张量 shape 为 [1, 320, 320, 1] float32
            // 用 ByteBuffer 接收输出，避免 Java 数组维度与张量 shape 不匹配的问题
            val outputSize = INPUT_SIZE * INPUT_SIZE * 4  // 每个输出: 320×320 个 float32 = 409600 字节
            val outputBuffers = Array(7) {
                ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder())
            }

            val inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
                .order(ByteOrder.nativeOrder())
            inputBuffer.rewind()
            for (h in 0 until INPUT_SIZE) {
                for (w in 0 until INPUT_SIZE) {
                    for (c in 0 until 3) {
                        inputBuffer.putFloat(inputData[h][w][c])
                    }
                }
            }

            val outputMap = HashMap<Int, Any>()
            for (i in outputBuffers.indices) {
                outputMap[i] = outputBuffers[i]
            }

            interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)
            Log.d(TAG, "TFLite 推理完成，获取 d0 显著性 mask")

            // Step 4: 后处理 — 使用 d0 mask 合成前景图
            // d0 是 outputBuffers[0]，值域 [0, 1]（sigmoid 输出）
            // 从 ByteBuffer 中读取 320×320 个 float 值
            val mask = Array(INPUT_SIZE) { FloatArray(INPUT_SIZE) }
            outputBuffers[0].rewind()
            for (h in 0 until INPUT_SIZE) {
                for (w in 0 until INPUT_SIZE) {
                    mask[h][w] = outputBuffers[0].float
                }
            }
            Log.d(TAG, "d0 mask 读取完成, range=[${mask.minOf { it.minOf { v -> v } }}, ${mask.maxOf { it.maxOf { v -> v } }}]")

            // 归一化 mask 到 [0, 1]（U2-Net 原始代码的做法）
            val normalizedMask = normalizeMask(mask)

            // 将 mask 缩放回原图尺寸并合成前景图
            val foreground = applyMaskToBitmap(originalBitmap, normalizedMask)

            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle()
            }

            Log.d(TAG, "=== U2-Net 分割流程完成 === foreground=${foreground.width}x${foreground.height}")
            foreground
        } catch (e: Exception) {
            Log.e(TAG, "分割异常: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    /**
     * 从 assets 加载 TFLite 模型到 ByteBuffer
     */
    private fun loadModelFile(): ByteBuffer {
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val buffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
        fileDescriptor.close()
        inputStream.close()
        Log.d(TAG, "TFLite 模型加载成功: $MODEL_FILE (${declaredLength / 1024 / 1024}MB)")
        return buffer
    }

    /**
     * 将 Bitmap 转换为归一化的 float 数组 [H][W][C]
     * - RGB 通道顺序
     * - ImageNet 归一化: (pixel/255 - mean) / std
     */
    private fun bitmapToNormalizedFloatArray(bitmap: Bitmap): Array<Array<FloatArray>> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val result = Array(height) { Array(width) { FloatArray(3) } }
        for (h in 0 until height) {
            for (w in 0 until width) {
                val pixel = pixels[h * width + w]
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                // ImageNet 归一化
                result[h][w][0] = (r - NORM_MEAN[0]) / NORM_STD[0]
                result[h][w][1] = (g - NORM_MEAN[1]) / NORM_STD[1]
                result[h][w][2] = (b - NORM_MEAN[2]) / NORM_STD[2]
            }
        }
        return result
    }

    /**
     * 归一化 mask 到 [0, 1] 范围
     * U2-Net 原始代码使用 min-max 归一化
     */
    private fun normalizeMask(mask: Array<FloatArray>): Array<FloatArray> {
        val height = mask.size
        val width = mask[0].size
        var minVal = Float.MAX_VALUE
        var maxVal = Float.MIN_VALUE

        for (h in 0 until height) {
            for (w in 0 until width) {
                val v = mask[h][w]
                if (v < minVal) minVal = v
                if (v > maxVal) maxVal = v
            }
        }

        val range = maxVal - minVal
        val result = Array(height) { FloatArray(width) }
        if (range < 1e-6f) {
            // 极端情况：mask 值全部相同
            for (h in 0 until height) {
                for (w in 0 until width) {
                    result[h][w] = 0.5f
                }
            }
        } else {
            for (h in 0 until height) {
                for (w in 0 until width) {
                    result[h][w] = (mask[h][w] - minVal) / range
                }
            }
        }
        return result
    }

    /**
     * 将 mask 缩放回原图尺寸并作为 alpha 通道合成前景图
     * - mask 值 > 阈值 → 前景（alpha=255）
     * - mask 值 < 阈值 → 背景（alpha=0）
     * - 中间值 → 平滑过渡
     *
     * @param originalBitmap 原始图片（不含 alpha 通道）
     * @param mask 320×320 的显著性 mask，值域 [0, 1]
     * @return 带透明背景的前景图 (ARGB_8888)
     */
    private fun applyMaskToBitmap(
        originalBitmap: Bitmap,
        mask: Array<FloatArray>
    ): Bitmap {
        val origWidth = originalBitmap.width
        val origHeight = originalBitmap.height

        // 创建 320×320 的 mask Bitmap（使用 ARGB_8888 格式，避免 ALPHA_8 的格式歧义）
        // ALPHA_8 的 setPixels 期望 ARGB int（alpha 在最高8位），直接传 0-255 值会全部变成 0
        val maskBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val maskPixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        for (h in 0 until INPUT_SIZE) {
            for (w in 0 until INPUT_SIZE) {
                val alpha = (mask[h][w] * 255).toInt().coerceIn(0, 255)
                // ARGB 格式：alpha 值必须放到 int 的最高 8 位（0xAA000000）
                // RGB 设为 0（mask 只需要 alpha 通道）
                maskPixels[h * INPUT_SIZE + w] = alpha shl 24
            }
        }
        maskBitmap.setPixels(maskPixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        // 缩放 mask 到原图尺寸（双线性插值，边缘更平滑）
        val scaledMask = Bitmap.createScaledBitmap(maskBitmap, origWidth, origHeight, true)
        maskBitmap.recycle()

        // 读取缩放后的 mask 像素值（ARGB int 格式）
        val scaledMaskPixels = IntArray(origWidth * origHeight)
        scaledMask.getPixels(scaledMaskPixels, 0, origWidth, 0, 0, origWidth, origHeight)
        scaledMask.recycle()

        // 读取原图像素
        val origPixels = IntArray(origWidth * origHeight)
        originalBitmap.getPixels(origPixels, 0, origWidth, 0, 0, origWidth, origHeight)

        // 合成前景图：使用 mask 作为 alpha 通道
        val resultPixels = IntArray(origWidth * origHeight)
        for (i in resultPixels.indices) {
            val pixel = origPixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // 从 ARGB int 中提取 alpha 通道（最高 8 位）
            val rawAlpha = (scaledMaskPixels[i] ushr 24) and 0xFF
            // 应用阈值平滑，消除边缘锯齿
            val alpha = smoothAlpha(rawAlpha)
            resultPixels[i] = (alpha shl 24) or (r shl 16) or (g shl 8) or b
        }

        val result = Bitmap.createBitmap(origWidth, origHeight, Bitmap.Config.ARGB_8888)
        result.setPixels(resultPixels, 0, origWidth, 0, 0, origWidth, origHeight)

        Log.d(TAG, "前景图合成完成: ${origWidth}x${origHeight}, hasAlpha=${result.hasAlpha()}")
        return result
    }

    /**
     * 平滑 alpha 值
     * - mask < 0.3 (alpha < 76) → 完全透明 (0)
     * - mask > 0.7 (alpha > 178) → 完全不透明 (255)
     * - 中间值 → 线性过渡
     * 这样可以避免边缘锯齿，同时保证主体清晰
     */
    private fun smoothAlpha(rawAlpha: Int): Int {
        return when {
            rawAlpha < 76 -> 0
            rawAlpha > 178 -> 255
            else -> {
                // 线性映射 [76, 178] → [0, 255]
                ((rawAlpha - 76) * 255 / 102).coerceIn(0, 255)
            }
        }
    }

    /**
     * 从 Uri 加载 Bitmap
     * - 限制最大边长，避免大图 OOM
     * - 读取 EXIF 方向标记并旋转到正向（CameraX 拍照默认是传感器原始方向）
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Log.e(TAG, "BitmapFactory.decodeStream 返回 null")
                return null
            }

            // 读取 EXIF 方向标记并旋转 Bitmap
            val rotatedBitmap = applyExifOrientation(uri, bitmap)

            // 限制最大尺寸
            val scaledBitmap = if (rotatedBitmap.width > MAX_INPUT_SIZE || rotatedBitmap.height > MAX_INPUT_SIZE) {
                val scale = MAX_INPUT_SIZE.toFloat() / maxOf(rotatedBitmap.width, rotatedBitmap.height)
                Bitmap.createScaledBitmap(
                    rotatedBitmap,
                    (rotatedBitmap.width * scale).toInt(),
                    (rotatedBitmap.height * scale).toInt(),
                    true
                ).also { if (it != rotatedBitmap) rotatedBitmap.recycle() }
            } else {
                rotatedBitmap
            }

            Log.d(TAG, "加载图片完成: ${scaledBitmap.width}x${scaledBitmap.height}")
            scaledBitmap
        } catch (e: Exception) {
            Log.e(TAG, "加载图片失败: ${e.message}", e)
            null
        }
    }

    /**
     * 读取 EXIF orientation 并将 Bitmap 旋转到正确方向
     */
    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val exifInputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(exifInputStream)
            exifInputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            Log.d(TAG, "EXIF orientation=$orientation, 已旋转 Bitmap")
            rotated
        } catch (e: Exception) {
            Log.w(TAG, "读取 EXIF 失败，使用原始方向: ${e.message}")
            bitmap
        }
    }

    /**
     * 释放资源
     * - 关闭 TFLite Interpreter
     */
    fun close() {
        try {
            interpreter.close()
            Log.d(TAG, "TFLite Interpreter 已关闭")
        } catch (e: Exception) {
            Log.w(TAG, "关闭 Interpreter 时出错: ${e.message}")
        }
    }
}
