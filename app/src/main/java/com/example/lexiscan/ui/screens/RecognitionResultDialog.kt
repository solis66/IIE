package com.example.lexiscan.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lexiscan.R
import com.example.lexiscan.ui.components.PinFramedImage
import com.example.lexiscan.ui.components.SketchTextButton
import com.example.lexiscan.ui.components.paperBackground
import com.example.lexiscan.ui.theme.CaveatFamily
import com.example.lexiscan.ui.theme.inkBlue
import com.example.lexiscan.ui.theme.redPen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 识别结果页 —— 教授讲义风
 * 布局：讲义头（新词收录·日期）→ 图钉双线框装裱物品 → 单词信息（手写排布）→ 例句 → 操作按钮
 * 页面可上下滚动，矮屏不被裁切
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
    wordClass: String?,
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

    // 讲义头日期
    val dateText = remember {
        val sdf = SimpleDateFormat("M月d日", Locale.CHINESE)
        "✎ 新词收录 · ${sdf.format(Date())}"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paperBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 讲义头 ──
            Text(
                text = dateText,
                fontSize = 14.sp,
                color = inkBlue.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 6.dp),
                textAlign = TextAlign.Center
            )

            // ── 图钉双线框装裱区 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    PinFramedImage(
                        imageUri = imageUri.toString(),
                        contentDescription = name,
                        frameWidth = 220.dp,
                        frameHeight = 230.dp
                    )
                } else {
                    // 无图兜底：占位白框
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    )
                }
            }

            // ── 信息区（入场动画保留）──
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(18.dp))

                    // 英文单词（Caveat 大字）；DeepSeek 失败时退显示中文
                    Text(
                        text = if (isEnglishValid) name else (chineseName ?: name),
                        fontFamily = CaveatFamily,
                        fontSize = if (isEnglishValid) 36.sp else 30.sp,
                        color = if (isEnglishValid) inkBlue else redPen,
                        textAlign = TextAlign.Center
                    )

                    // DeepSeek 离线提示
                    if (!isEnglishValid) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "英文翻译离线，展示原始识别结果",
                            fontSize = 11.sp,
                            color = inkBlue.copy(alpha = 0.45f)
                        )
                    }

                    // 音标 + 发音按钮（红笔一行）
                    if (!phonetic.isNullOrBlank() && isEnglishValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = phonetic,
                                fontFamily = CaveatFamily,
                                fontSize = 21.sp,
                                color = redPen,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🔊",
                                fontSize = 17.sp,
                                modifier = Modifier.clickable(onClick = onAudioClick)
                            )
                        }
                    }

                    // 中文 · 词性（词性来自 DeepSeek 新字段，为空时仅显示中文）
                    if (!chineseName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (wordClass.isNullOrBlank()) chineseName
                            else "$chineseName · $wordClass",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = inkBlue,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 复数 + 置信度 + 收藏（小字一行）
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val metaParts = buildList {
                            if (!plural.isNullOrBlank()) add("复数 $plural")
                            add("置信 ${(confidence * 100).toInt()}%")
                        }
                        Text(
                            text = metaParts.joinToString("  ·  "),
                            fontSize = 12.sp,
                            color = inkBlue.copy(alpha = 0.45f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        // 收藏星（红笔）
                        Text(
                            text = if (isCollected) "★" else "☆",
                            fontSize = 20.sp,
                            color = if (isCollected) redPen else inkBlue.copy(alpha = 0.45f),
                            modifier = Modifier.clickable(onClick = onCollectClick)
                        )
                    }

                    // ── 例句：白纸块 + 红笔下划线 ──
                    if (!exampleSentence.isNullOrBlank() && isEnglishValid) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.5f))
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // 英文例句（Caveat）+ 红笔下划线（随文字宽度）
                                Column(
                                    modifier = Modifier.width(IntrinsicSize.Max),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = exampleSentence,
                                        fontFamily = CaveatFamily,
                                        fontSize = 19.sp,
                                        lineHeight = 23.sp,
                                        color = inkBlue,
                                        textAlign = TextAlign.Center
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp)
                                            .height(2.dp)
                                            .background(redPen.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // ── 操作按钮：虚线手绘框 ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SketchTextButton(
                            text = stringResource(R.string.btn_back_home),
                            onClick = onBackHome,
                            modifier = Modifier.weight(1f)
                        )
                        SketchTextButton(
                            text = stringResource(R.string.btn_scan_again),
                            onClick = onContinue,
                            textColor = redPen,
                            modifier = Modifier.weight(1.4f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
