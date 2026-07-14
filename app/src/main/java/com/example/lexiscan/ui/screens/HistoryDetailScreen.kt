package com.example.lexiscan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import com.example.lexiscan.data.model.RecognitionRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── 卡通学习卡片配色（与 RecognitionResultDialog 统一）──
private val cardBg = Color(0xFFFFF8F0)
private val cardAccent = Color(0xFFFF6B6B)
private val cardYellow = Color(0xFFFFD93D)
private val cardPurple = Color(0xFF6C5CE7)
private val cardGreen = Color(0xFF00B894)
private val textMain = Color(0xFF2D3436)
private val textSub = Color(0xFF636E72)
private val textLight = Color(0xFFB2BEC3)

/**
 * 历史识别记录详情页（卡通学习卡片风格）
 * 展示放大图片、英文单词、音标、复数、例句、识别时间
 */
@Composable
fun HistoryDetailScreen(
    record: RecognitionRecord,
    onBack: () -> Unit,
    onAudioClick: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { showContent = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部返回按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "\u2190 返回",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSub,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 日期显示
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatTimestamp(record.timestamp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textLight
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 物品图片展示区（卡通画框）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                cardYellow.copy(alpha = 0.2f),
                                cardAccent.copy(alpha = 0.1f),
                                cardBg
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .shadow(12.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(record.imageUri)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = record.englishName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp)
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
                        // 英文单词
                        Text(
                            text = record.englishName,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textMain,
                            letterSpacing = (-0.5).sp,
                            textAlign = TextAlign.Center
                        )

                        // 中文含义
                        if (record.chineseName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = record.chineseName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSub,
                                textAlign = TextAlign.Center
                            )
                        }

                        // 音标标签与发音按钮
                        if (!record.phonetic.isNullOrBlank()) {
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
                                        text = record.phonetic,
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

                        // 置信度
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cardAccent.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "置信 ${(record.confidence * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = cardAccent
                                )
                            }
                        }

                        // 复数形式
                        if (!record.plural.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
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
                                        text = record.plural,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cardGreen
                                    )
                                }
                            }
                        }

                        // 例句
                        if (!record.exampleSentence.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
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
                                        text = record.exampleSentence,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = textMain.copy(alpha = 0.85f),
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

/**
 * 全部历史记录列表页 —— 点击首页"查看全部"进入
 * 以列表形式展示所有识别记录，点击单项进入详情
 */
@Composable
fun HistoryListScreen(
    records: List<RecognitionRecord>,
    onBack: () -> Unit,
    onItemClick: (RecognitionRecord) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部返回按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "\u2190 返回",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSub,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 页面标题
            Text(
                text = "全部历史记录",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textMain,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // 空状态提示
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无识别记录",
                        fontSize = 16.sp,
                        color = textLight
                    )
                }
            } else {
                // 历史记录列表
                records.forEach { record ->
                    HistoryListItem(
                        record = record,
                        onClick = { onItemClick(record) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 历史记录列表项 —— 用于全部历史记录页面
 * 暖色调卡片设计，与首页历史卡片风格统一
 */
@Composable
private fun HistoryListItem(
    record: RecognitionRecord,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 物品缩略图
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(record.imageUri)
                    .crossfade(true)
                    .build()
            ),
            contentDescription = record.englishName,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF0EAE1)),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            // 英文单词
            Text(
                text = record.englishName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = textMain,
                maxLines = 1
            )
            // 中文翻译
            Text(
                text = record.chineseName,
                fontSize = 14.sp,
                color = textSub,
                modifier = Modifier.padding(top = 2.dp)
            )
            // 时间
            Text(
                text = formatTimestamp(record.timestamp),
                fontSize = 11.sp,
                color = textLight,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // 右箭头
        Text(
            text = "\u203A",
            fontSize = 24.sp,
            color = textLight
        )
    }
}

/**
 * 格式化时间戳为可读日期字符串
 */
private fun formatTimestamp(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINESE)
        sdf.format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}
