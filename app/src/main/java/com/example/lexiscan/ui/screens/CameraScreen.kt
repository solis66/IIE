package com.example.lexiscan.ui.screens

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.lexiscan.R
import com.example.lexiscan.data.camera.CameraController
import com.example.lexiscan.ui.components.paperBackground
import com.example.lexiscan.ui.theme.*

/**
 * 相机界面
 * 包含：相机预览、拟态玻璃扫描框、底部玻璃按钮组
 * 设计风格：深色背景 + 玻璃拟态控件
 */
@Composable
fun CameraScreen(
    isTorchOn: Boolean,
    isRecognizing: Boolean,
    capturedImageUri: Uri?,
    segmentedForegroundUri: Uri?,
    onBackClick: () -> Unit,
    onTorchToggle: () -> Unit,
    onCaptureClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraController = remember { CameraController(context, lifecycleOwner) }

    LaunchedEffect(cameraController) {
        cameraController.setOnImageCapturedListener { uri ->
            onPhotoCaptured(uri)
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraController.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 当处于识别加载状态时，显示拍摄或选择的静态图片，冻结画面
        if (isRecognizing && capturedImageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(capturedImageUri)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Captured Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 否则显示相机实时预览
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                update = { previewView ->
                    previewView.post {
                        cameraController.startCamera(previewView)
                    }
                }
            )
        }

        // 顶部栏：返回按钮（右侧占位，原小狐狸表情已移除）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮（玻璃拟态设计）
            GlassCircleButton(
                onClick = onBackClick,
                content = {
                    Text(text = "\u2039", fontSize = 26.sp, color = Color.White)
                }
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // 中间区域：拟态玻璃扫描框 + 提示文字
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 提示文字
                Text(
                    text = "请将需要识别的物品放置屏幕正中心",
                    modifier = Modifier.padding(bottom = 28.dp),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                // 拟态玻璃扫描框（带圆角矩形边框 + 四角装饰）
                GlassScanFrame()
            }
        }

        // 识别加载状态：撕拉扫描动画掩盖 API 等待时间（仿 CapWords）
        if (isRecognizing) {
            TearScanOverlay(
                originalImageUri = capturedImageUri,
                foregroundImageUri = segmentedForegroundUri
            )
        }

        // 底部操作区：玻璃拟态按钮组
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(160.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 闪光灯按钮（玻璃拟态）
                GlassCircleButton(
                    onClick = {
                        onTorchToggle()
                        cameraController.toggleTorch()
                    },
                    content = {
                        IconFlash(modifier = Modifier.size(22.dp), enabled = isTorchOn)
                    }
                )

                // 拍照按钮（突出设计：白色边框 + 实心圆）
                CaptureButton(onClick = {
                    onCaptureClick()
                    cameraController.takePhoto()
                })

                // 相册按钮（玻璃拟态）
                GlassCircleButton(
                    onClick = onGalleryClick,
                    content = {
                        IconGallery(modifier = Modifier.size(22.dp))
                    }
                )
            }
        }
    }
}

/**
 * 玻璃拟态圆形按钮
 * 半透明白色背景 + 白色描边 + 轻微阴影
 * 点击时加深半透明度，提供视觉反馈
 */
@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    // 使用 interactionSource 检测按下状态，实现按压视觉反馈
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // 按压缩放动画：按下时缩小到 0.95，松开回弹，配合按压视觉反馈
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "glass_scale"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isPressed) {
                    Color.White.copy(alpha = 0.25f)
                } else {
                    Color.White.copy(alpha = 0.15f)
                }
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * 拍照按钮（突出设计）
 * 白色边框 + 实心白色圆，与玻璃按钮形成对比
 */
@Composable
fun CaptureButton(onClick: () -> Unit) {
    // 按压缩放动画：按下时缩小到 0.92
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(100),
        label = "capture_scale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(4.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.82f)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * 手绘虚线扫描框（教授讲义风取景框）
 *
 * 视觉设计：
 * - 墨白色虚线圆角矩形模拟手画的框（PathEffect.dashPathEffect）
 * - 四角叠加 L 形短实线加强（线长 40dp，线宽 3.5dp）
 * - 透明度呼吸动画（0.4~0.8），引导用户对焦
 */
@Composable
fun GlassScanFrame() {
    // 呼吸动画：边框透明度在 0.4~0.8 之间循环渐变
    val infiniteTransition = rememberInfiniteTransition(label = "scan_frame")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    // 角标参数：线长 40dp，线宽 3.5dp
    val cornerLength = 40.dp
    val cornerStroke = 3.5.dp

    // 容器尺寸与取景框一致
    Box(modifier = Modifier.size(256.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val len = cornerLength.toPx()
            val stroke = cornerStroke.toPx()
            val color = Color.White.copy(alpha = borderAlpha)

            // 1. 手绘感虚线矩形框（不规则四角圆角）
            val rect = androidx.compose.ui.geometry.RoundRect(
                rect = androidx.compose.ui.geometry.Rect(0f, 0f, w, h),
                topLeft = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx(), 12.dp.toPx()),
                topRight = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx(), 16.dp.toPx()),
                bottomRight = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx(), 24.dp.toPx()),
                bottomLeft = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 26.dp.toPx())
            )
            val framePath = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(rect)
            }
            drawPath(
                path = framePath,
                color = color.copy(alpha = color.alpha * 0.7f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(9.dp.toPx(), 7.dp.toPx())
                    )
                )
            )

            // 2. 四角 L 形短实线加强
            drawLine(color, Offset(0f, 0f), Offset(len, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(0f, 0f), Offset(0f, len), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w, 0f), Offset(w - len, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w, 0f), Offset(w, len), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(0f, h), Offset(len, h), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(0f, h), Offset(0f, h - len), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w, h), Offset(w - len, h), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w, h), Offset(w, h - len), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

/**
 * 撕拉扫描覆盖层（仿 CapWords 撕拉效果）
 *
 * 单次撕拉效果（从拍照立即开始，覆盖分割等待时间）：
 * - 撕拉中：扫描线从上到下单次移动，上方露出白色背景（原图被"撕掉"），
 *   下方保留原图，撕拉线带光晕 + 阴影增强立体感
 * - 撕拉完成 + 前景图就绪：全屏白底，前景物品 spring 弹出（贴纸效果）
 * - 撕拉完成 + 前景图未就绪：全屏白底 + "正在抠图…"提示，等待分割完成
 *
 * @param originalImageUri 拍照原图 Uri
 * @param foregroundImageUri 分割后的透明前景图 Uri，null 表示分割未完成
 */
@Composable
fun TearScanOverlay(
    originalImageUri: Uri? = null,
    foregroundImageUri: Uri? = null
) {
    val context = LocalContext.current

    // 分割是否完成（前景图是否就绪）
    val showForeground = foregroundImageUri != null

    // 撕拉是否完成（扫描线到底）
    var tearComplete by remember { mutableStateOf(false) }

    // 单次撕拉进度（从 0 到 1，不循环）
    // 用 Animatable 手动控制，拍照后立即开始，覆盖分割等待时间
    val tearProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        tearProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
        )
        tearComplete = true
        // 撕拉完成时触觉反馈
        try {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (_: Exception) { }
    }

    // 撕拉过程中的周期性轻微震动（增强撕拉触感）
    LaunchedEffect(Unit) {
        while (!tearComplete) {
            delay(150)
            try {
                val vibrator = context.getSystemService(Vibrator::class.java) ?: break
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(8L, 60))
                }
            } catch (_: Exception) { }
        }
    }

    // 前景物品弹出动画（撕拉完成 + 前景图就绪后触发）
    val popInScale by animateFloatAsState(
        targetValue = if (tearComplete && showForeground) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 400f
        ),
        label = "pop_in_scale"
    )
    val popInAlpha by animateFloatAsState(
        targetValue = if (tearComplete && showForeground) 1f else 0f,
        animationSpec = tween(300, easing = LinearEasing),
        label = "pop_in_alpha"
    )

    // 加载原图 Painter
    val originalPainter = if (originalImageUri != null) {
        rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(originalImageUri)
                .crossfade(false)
                .build()
        )
    } else null

    // 加载前景图 Painter
    val foregroundPainter = if (foregroundImageUri != null) {
        rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(foregroundImageUri)
                .crossfade(false)
                .build()
        )
    } else null

    val progress = tearProgress.value

    Box(modifier = Modifier.fillMaxSize()) {
        // ================================================================
        // 背景层：撕拉中显示原图，撕拉完成后全白
        // ================================================================
        if (tearComplete) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White))
        } else if (originalPainter != null) {
            Image(
                painter = originalPainter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // ================================================================
        // 撕拉效果：扫描线上方覆盖白色（原图被"撕掉"）+ 阴影 + 光晕
        // ================================================================
        if (!tearComplete) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height
                val scanY = canvasH * progress

                // 1. 扫描线上方：白色覆盖（原图被"撕掉"）
                drawRect(
                    color = Color.White,
                    topLeft = Offset(0f, 0f),
                    size = Size(canvasW, scanY)
                )

                // 2. 撕拉线阴影（下方渐变到透明，增加撕开立体感）
                val shadowHeight = 24f
                if (scanY < canvasH) {
                    drawRect(
                        topLeft = Offset(0f, scanY),
                        size = Size(canvasW, shadowHeight.coerceAtMost(canvasH - scanY)),
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                }

                // 3. 扫描线上方光晕带（向上渐隐的暖橙色光晕）
                val glowHeight = 100f
                val glowStart = (scanY - glowHeight).coerceAtLeast(0f)
                drawRect(
                    topLeft = Offset(0f, glowStart),
                    size = Size(canvasW, scanY - glowStart),
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            warmOrange.copy(alpha = 0.0f),
                            warmOrange.copy(alpha = 0.15f),
                            warmOrangeLight.copy(alpha = 0.45f)
                        )
                    )
                )

                // 4. 主扫描线外层光晕（8px）
                drawLine(
                    color = warmOrange.copy(alpha = 0.5f),
                    start = Offset(0f, scanY),
                    end = Offset(canvasW, scanY),
                    strokeWidth = 8f
                )
                // 5. 主扫描线核心（4px）
                drawLine(
                    color = warmOrange,
                    start = Offset(0f, scanY),
                    end = Offset(canvasW, scanY),
                    strokeWidth = 4f
                )
                // 6. 白色高光中线（1.5px）
                drawLine(
                    color = Color.White,
                    start = Offset(0f, scanY),
                    end = Offset(canvasW, scanY),
                    strokeWidth = 1.5f
                )
            }
        }

        // ================================================================
        // 撕拉完成 + 前景图就绪：显示抠出的物品（贴纸效果）
        // ================================================================
        if (tearComplete && foregroundPainter != null) {
            Image(
                painter = foregroundPainter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(popInScale),
                contentScale = ContentScale.Fit,
                alpha = popInAlpha
            )
        }

        // ================================================================
        // 底部状态提示：纸墨手写风状态条（撕开讲义 / 裁剪标本 / 教授批改中）
        // ================================================================
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 200.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(paperBg.copy(alpha = 0.92f))
                    .border(
                        width = 1.dp,
                        color = inkBlue.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                val statusText = when {
                    tearComplete && showForeground -> "教授批改中…"
                    tearComplete && !showForeground -> "裁剪标本…"
                    else -> "撕开讲义…"
                }
                Text(
                    text = statusText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = inkBlue
                )
            }
        }
    }
}

/**
 * 闪光灯图标（Canvas 绘制）
 */
@Composable
fun IconFlash(modifier: Modifier = Modifier, enabled: Boolean) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val color = if (enabled) Color(0xFFFFD93D) else Color.White.copy(alpha = 0.5f)

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.55f, h * 0.05f)
            lineTo(w * 0.2f, h * 0.52f)
            lineTo(w * 0.48f, h * 0.45f)
            lineTo(w * 0.42f, h * 0.95f)
            lineTo(w * 0.85f, h * 0.38f)
            lineTo(w * 0.52f, h * 0.45f)
            close()
        }
        drawPath(path = path, color = color)

        if (enabled) {
            drawPath(
                path = path,
                color = Color(0xFFFFD93D).copy(alpha = 0.4f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 4.dp.toPx()
                )
            )
        }
    }
}

/**
 * 相册图标（Canvas 绘制）
 */
@Composable
fun IconGallery(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val color = Color.White.copy(alpha = 0.8f)
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())

        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(width * 0.9f, height * 0.9f),
            cornerRadius = cornerRadius
        )
        drawRoundRect(
            color = color.copy(alpha = 0.5f),
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.45f, height * 0.45f),
            size = androidx.compose.ui.geometry.Size(width * 0.45f, height * 0.45f),
            cornerRadius = cornerRadius
        )
        drawCircle(
            color = color,
            center = androidx.compose.ui.geometry.Offset(width * 0.25f, height * 0.65f),
            radius = width * 0.1f
        )
    }
}