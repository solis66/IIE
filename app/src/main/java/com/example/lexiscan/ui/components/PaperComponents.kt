package com.example.lexiscan.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.lexiscan.data.model.RecognitionRecord
import com.example.lexiscan.ui.theme.CaveatFamily
import com.example.lexiscan.ui.theme.inkBlue
import com.example.lexiscan.ui.theme.paperBg
import com.example.lexiscan.ui.theme.paperLine
import com.example.lexiscan.ui.theme.redPen

// ══════════════════════════════════════════════════════════════════
//  教授讲义风共享组件
//  ───────────────────────────────────────────────────────────────────
//  paperBackground  — 米色讲义纸 + 横线纹理（页面背景）
//  sketchBorder     — 手绘感虚线边框（不规则圆角）
//  SketchTextButton — 虚线手绘框按钮（返回讲义 / 再拍一个）
//  PinFramedImage   — 图钉双线框装裱的图片（白衬纸 + 双线 + 红图钉）
//  WordRow          — 生词表一行（缩略图 + 单词 + 音标 + 中文 + 发音）
// ══════════════════════════════════════════════════════════════════

/**
 * 米色讲义纸背景：纸色底 + 固定横向横线纹理
 * 横线固定在页面背景上（不随内容滚动错位），行距 33dp
 */
fun Modifier.paperBackground(): Modifier = this.drawBehind {
    drawRect(paperBg)
    val gap = 33.dp.toPx()
    val stroke = 1.dp.toPx()
    var y = gap
    while (y < size.height) {
        drawLine(paperLine, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
        y += gap
    }
}

/**
 * 手绘感虚线边框：不规则四角圆角 + 虚线描边，模拟钢笔手画的框
 */
fun Modifier.sketchBorder(
    color: Color,
    strokeWidth: Dp = 2.dp,
    dash: Dp = 9.dp,
    gap: Dp = 7.dp
): Modifier = this.drawBehind {
    val w = size.width
    val h = size.height
    // 四角半径各不相同，制造手绘的不规则感
    val rect = RoundRect(
        rect = Rect(0f, 0f, w, h),
        topLeft = CornerRadius(22.dp.toPx(), 10.dp.toPx()),
        topRight = CornerRadius(26.dp.toPx(), 14.dp.toPx()),
        bottomRight = CornerRadius(18.dp.toPx(), 24.dp.toPx()),
        bottomLeft = CornerRadius(12.dp.toPx(), 26.dp.toPx())
    )
    val path = Path().apply { addRoundRect(rect) }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))
        )
    )
}

/**
 * 虚线手绘框按钮（结果页"返回讲义 / 再拍一个"、主页空状态等）
 */
@Composable
fun SketchTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = inkBlue,
    fontSize: Int = 15
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(150),
        label = "sketch_btn_scale"
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .sketchBorder(textColor, strokeWidth = 2.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * 图钉双线框装裱的图片
 *
 * 视觉构成（从外到内）：
 * 1. 柔和投影
 * 2. 白色衬纸 + 外框 1.5dp 墨色实线
 * 3. 内框 1dp 墨色 55%（双线间距约 3dp）
 * 4. 图片内容（Fit，白底透出）
 * 5. 顶部中央一枚红图钉压在框沿上
 *
 * 入场：spring 弹出 + 落定后持续轻微悬浮
 */
@Composable
fun PinFramedImage(
    imageUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    frameWidth: Dp = 220.dp,
    frameHeight: Dp = 220.dp
) {
    // 弹出动画
    val scaleAnim = remember { Animatable(0f) }
    LaunchedEffect(imageUri) {
        scaleAnim.snapTo(0f)
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // 悬浮动画：落定后上下微浮 4dp
    val floatTransition = rememberInfiniteTransition(label = "pin_frame_float")
    val floatOffset by floatTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pin_frame_float_y"
    )

    Box(
        modifier = modifier
            .width(frameWidth)
            .height(frameHeight)
            .scale(scaleAnim.value)
            .offset(y = floatOffset.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(4.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            // 白衬纸
            .background(Color.White, RoundedCornerShape(4.dp))
            .drawBehind {
                val r = 4.dp.toPx()
                // 外框：1.5dp 墨色实线
                drawRoundRect(
                    color = inkBlue,
                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // 内框：1dp 墨色 55%
                drawRoundRect(
                    color = inkBlue.copy(alpha = 0.55f),
                    topLeft = Offset(5.5.dp.toPx(), 5.5.dp.toPx()),
                    size = Size(size.width - 11.dp.toPx(), size.height - 11.dp.toPx()),
                    cornerRadius = CornerRadius(r * 0.8f, r * 0.8f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(14.dp) // 框内留白，让图片落在双线内
    ) {
        // 图片（白底 Fit）
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(true)
                    .build()
            ),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp)),
            contentScale = ContentScale.Fit
        )

        // 顶部中央红图钉（压在框沿上）
        PinDot(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
        )
    }
}

/**
 * 红图钉：径向渐变圆 + 白色高光点 + 微投影
 */
@Composable
fun PinDot(modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(colors = listOf(Color(0xFFD65A4A), Color(0xFF8E2F22)))
            )
    ) {
        // 高光点
        Box(
            modifier = Modifier
                .padding(start = 3.dp, top = 3.dp)
                .size(size / 4)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.75f))
        )
    }
}

/**
 * 生词表一行：小照片（微倾斜白衬纸）+ 单词 + 音标 + 中文/时间 + 发音按钮
 * 主页"最近学的词"与全部历史列表共用
 *
 * @param index 用于缩略图交替倾斜方向
 */
@Composable
fun WordRow(
    record: RecognitionRecord,
    onClick: () -> Unit,
    onAudioClick: () -> Unit,
    index: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 缩略图：白衬纸 + 微投影 + 奇偶交替微倾斜
        Box(
            modifier = Modifier
                .size(42.dp)
                .rotate(if (index % 2 == 0) -1.5f else 1.5f)
                .shadow(2.dp, RoundedCornerShape(3.dp))
                .background(Color.White, RoundedCornerShape(3.dp))
                .padding(2.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(record.imageUri)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = record.englishName,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // 文字信息
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            // 第一行：英文单词（Caveat）+ 红笔音标
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = record.englishName,
                    fontFamily = CaveatFamily,
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    color = inkBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!record.phonetic.isNullOrBlank()) {
                    Text(
                        text = "  ${record.phonetic}",
                        fontFamily = CaveatFamily,
                        fontSize = 14.sp,
                        color = redPen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // 第二行：中文 + 相对时间
            Text(
                text = buildString {
                    append(record.chineseName.ifBlank { "—" })
                    append(" · ")
                    append(formatRelativeTime(record.timestamp))
                },
                fontSize = 12.sp,
                color = inkBlue.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        // 发音按钮（红笔，原地发音，不进详情）
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(redPen.copy(alpha = 0.08f))
                .clickable(onClick = onAudioClick),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🔊", fontSize = 15.sp)
        }
    }
}

/**
 * 相对时间格式化（刚刚 / n分钟前 / n小时前 / 昨天 / n天前）
 */
fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = (diff / 60000).toInt()
    val hours = (diff / 3600000).toInt()
    val days = (diff / 86400000).toInt()
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        hours < 24 -> "${hours}小时前"
        days == 1 -> "昨天"
        else -> "${days}天前"
    }
}
