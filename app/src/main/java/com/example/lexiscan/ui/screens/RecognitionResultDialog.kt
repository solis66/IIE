package com.example.lexiscan.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
// ── 动画相关导入（按压缩放动画用）──
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
// ── 交互状态导入（按钮按下检测用）──
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale   // 按压缩放动画用
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
// ── Lexi 小狐狸 IP 组件导入 ──
import com.example.lexiscan.ui.components.LexiExpression
import com.example.lexiscan.ui.components.LexiMascot
import com.example.lexiscan.ui.components.LexiSpeechBubble
// ── 全局暖色调主题变量导入 ──
import com.example.lexiscan.ui.theme.*
// ── 卡通学习卡片配色（统一引用全局暖色调系统 Theme.kt，不再使用硬编码色值）──
private val cardBg = warmBg                  // 暖白底色
private val cardAccent = warmOrange          // 暖橙强调色
private val cardYellow = warmOrangeLight     // 活泼暖橙
private val cardPurple = warmPurple          // 童趣紫
private val cardGreen = warmMint             // 清新薄荷绿
private val textMain = warmTextPrimary       // 主文字色
private val textSub = warmTextSecondary      // 副文字色
private val textLight = warmTextTertiary     // 淡文字色

/**
 * 卡通学习卡片 — 识别结果展示页
 * 布局：顶部暖色背景 → 物品卡通图 → 白色学习卡片(单词+音标+复数+例句) → 操作按钮
 */
@Composable
fun RecognitionResultDialog(
    imageUri: Uri?,
    name: String,
    confidence: Float,
    phonetic: String?,
    chineseName: String?,
    plural: String?,
    exampleSentence: String?,
    isEnglishValid: Boolean = true,
    isSegmented: Boolean = false,
    isCollected: Boolean,
    onBackHome: () -> Unit,
    onContinue: () -> Unit,
    onCollectClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { showContent = true }

    // 操作按钮按压缩放动画
    val btnInteractionSource = remember { MutableInteractionSource() }
    val isBtnPressed by btnInteractionSource.collectIsPressedAsState()
    val btnScale by animateFloatAsState(
        targetValue = if (isBtnPressed) 0.96f else 1f,
        animationSpec = tween(150),
        label = "btn_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cardBg)
    ) {
        // 顶部卡通渐变背景区域（装饰性 + 物品图）
        Column(modifier = Modifier.fillMaxSize()) {
            // ── IP 对话气泡：Lexi 小狐狸学习小贴士 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 小狐狸形象（LEARNING 表情）
                LexiMascot(
                    expression = LexiExpression.LEARNING,
                    sizePx = 48,
                    enableFloat = true
                )
                Spacer(modifier = Modifier.width(10.dp))
                // 对话气泡：学习小贴士
                LexiSpeechBubble(
                    text = "这个词很常用哦！一起来学习吧~",
                    expression = LexiExpression.LEARNING,
                    modifier = Modifier.weight(1f)
                )
            }
            // 物品图片展示区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                cardYellow.copy(alpha = 0.3f),
                                cardAccent.copy(alpha = 0.15f),
                                cardBg
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    if (isSegmented) {
                        // === 分割成功：贴纸式展示（弹出动画 + 白色描边 + 悬浮浮动）===
                        StickerImage(
                            imageUri = imageUri,
                            contentDescription = name
                        )
                    } else {
                        // === 分割失败：普通图片展示（圆角 + 淡入动画）===
                        PlainImageDisplay(
                            imageUri = imageUri,
                            contentDescription = name
                        )
                    }
                }
            }

            // 学习卡片区域
            AnimatedVisibility(
                visible = showContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                        // === 单词卡片 ===
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 英文单词（大字醒目）
                        // DeepSeek 翻译失败时 name 可能仍是中文，退显示原始名称
                            Text(
                                text = if (isEnglishValid) name else (chineseName ?: name),
                                fontSize = if (isEnglishValid) 32.sp else 28.sp,
                                fontWeight = if (isEnglishValid) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isEnglishValid) textMain else cardAccent,
                                letterSpacing = (-0.5).sp,
                                textAlign = TextAlign.Center
                            )

                        // 中文含义 / 翻译失败提示
                            if (!chineseName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isEnglishValid) chineseName else name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textSub,
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            // DeepSeek 离线提示
                            if (!isEnglishValid) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cardYellow.copy(alpha = 0.2f))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "英文翻译离线，展示原始识别结果",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = textLight
                                    )
                                }
                            }

                            // 音标标签与发音按钮
                            if (!phonetic.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cardPurple.copy(alpha = 0.08f))
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = phonetic,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = cardPurple,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // 喇叭发音按钮
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(cardPurple.copy(alpha = 0.1f))
                                            .clickable(onClick = onAudioClick),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🔊",
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }

                            // 分隔线
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.3f)
                                    .height(1.dp)
                                    .background(textLight.copy(alpha = 0.3f))
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // 复数形式
                            if (!plural.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "复数:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textSub
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cardGreen.copy(alpha = 0.12f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = plural,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cardGreen
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // 例句（对话气泡风格）
                            if (!exampleSentence.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(cardYellow.copy(alpha = 0.15f))
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "例句",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cardAccent.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = exampleSentence,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = textMain.copy(alpha = 0.85f),
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // === 置信度 + 收藏按钮行 ===
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 置信度小标签
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cardAccent.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "置信 ${(confidence * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = cardAccent
                                )
                            }

                            // 收藏按钮（圆形）
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCollected) cardYellow else Color.White
                                    )
                                    .clickable(onClick = onCollectClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isCollected) "\u2605" else "\u2606",
                                    fontSize = 24.sp,
                                    color = if (isCollected) cardAccent else textLight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // === 操作按钮 ===
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 返回首页
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .shadow(4.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .clickable(onClick = onBackHome),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "返回首页",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSub
                                )
                            }

                            // 继续拍照（带按压缩放动画）
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .scale(btnScale)   // 按压缩放反馈
                                    .shadow(4.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(cardAccent, cardPurple)
                                        )
                                    )
                                    .clickable(
                                        // 使用自定义 interactionSource 驱动缩放动画，缩放即按压反馈
                                        interactionSource = btnInteractionSource,
                                        indication = null,
                                        onClick = onContinue
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "继续拍照",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
        }
    }
}

/**
 * 贴纸式图片展示组件（仿 CapWords 物品展示效果）
 *
 * 仅在分割成功（isSegmented=true）时使用，展示透明背景的抠图物品。
 *
 * 视觉构成：
 * 1. 弹出动画：scale 从 0 → 1（spring 带 overshoot，模拟贴纸"贴上"的弹性）
 * 2. 悬浮浮动：落定后持续上下微浮，制造"悬浮在背景中"的效果
 * 3. 透明背景：抠图物品直接展示在结果界面上，透明区域透出界面背景色
 *    （与 CapWords 一致：物品背景与结果界面背景自然融合，无白色描边）
 * 4. 轻柔阴影：微弱投影增强层次感，不喧宾夺主
 *
 * @param imageUri 分割后透明背景的物品图片
 * @param contentDescription 无障碍描述
 */
@Composable
private fun StickerImage(
    imageUri: Uri,
    contentDescription: String
) {
    // 弹出动画：spring 中等弹力，stiffness 偏低让弹跳更明显
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

    // 悬浮浮动动画：落定后持续上下微浮 6dp，制造悬浮感
    val floatTransition = rememberInfiniteTransition(label = "sticker_float")
    val floatOffset by floatTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(imageUri)
            .crossfade(true)
            .build()
    )

    // 抠图物品直接展示在结果界面背景上（透明区域透出界面背景色）
    Box(
        modifier = Modifier
            .size(300.dp)
            .scale(scaleAnim.value)
            .offset(y = floatOffset.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            ),
        contentAlignment = Alignment.Center
    ) {
        // 原图层：分割后的物品（透明背景，透明区域透出结果界面背景）
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * 普通图片展示（分割失败时的降级方案）
 * 圆角裁剪 + 淡入动画，不加描边和贴纸效果
 */
@Composable
private fun PlainImageDisplay(
    imageUri: Uri,
    contentDescription: String
) {
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(imageUri) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(400)
        )
    }

    Image(
        painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .crossfade(true)
                .build()
        ),
        contentDescription = contentDescription,
        modifier = Modifier
            .size(260.dp)
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .alpha(alphaAnim.value),
        contentScale = ContentScale.Crop
    )
}
