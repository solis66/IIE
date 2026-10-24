package com.example.lexiscan.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.example.lexiscan.data.model.RecognitionRecord
import com.example.lexiscan.ui.components.PinFramedImage
import com.example.lexiscan.ui.components.WordRow
import com.example.lexiscan.ui.components.paperBackground
import com.example.lexiscan.ui.theme.CaveatFamily
import com.example.lexiscan.ui.theme.inkBlue
import com.example.lexiscan.ui.theme.redPen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 历史识别记录详情页 —— 教授讲义风
 * 返回 → 日期 → 图钉双线框装裱 → 单词信息（含词性）→ 例句
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
            .paperBackground()
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
                    text = "← 返回讲义",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = inkBlue.copy(alpha = 0.65f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
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
                    color = inkBlue.copy(alpha = 0.45f)
                )
            }

            // 图钉双线框装裱
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                PinFramedImage(
                    imageUri = record.imageUri,
                    contentDescription = record.englishName,
                    frameWidth = 220.dp,
                    frameHeight = 230.dp
                )
            }

            if (showContent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // 英文单词（Caveat）
                    Text(
                        text = record.englishName,
                        fontFamily = CaveatFamily,
                        fontSize = 36.sp,
                        color = inkBlue,
                        textAlign = TextAlign.Center
                    )

                    // 音标 + 发音按钮（红笔）
                    if (!record.phonetic.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = record.phonetic,
                                fontFamily = CaveatFamily,
                                fontSize = 21.sp,
                                color = redPen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(redPen.copy(alpha = 0.08f))
                                    .clickable(onClick = onAudioClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🔊", fontSize = 15.sp)
                            }
                        }
                    }

                    // 中文 · 词性（旧记录词性为空时仅显示中文）
                    if (record.chineseName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (record.wordClass.isNullOrBlank()) record.chineseName
                            else "${record.chineseName} · ${record.wordClass}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = inkBlue,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 复数 + 置信度（小字一行）
                    Spacer(modifier = Modifier.height(10.dp))
                    val metaParts = buildList {
                        if (!record.plural.isNullOrBlank()) add("复数 ${record.plural}")
                        add("置信 ${(record.confidence * 100).toInt()}%")
                    }
                    Text(
                        text = metaParts.joinToString("  ·  "),
                        fontSize = 12.sp,
                        color = inkBlue.copy(alpha = 0.45f)
                    )

                    // 例句：白纸块 + 红笔下划线
                    if (!record.exampleSentence.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.5f))
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Column(
                                modifier = Modifier.width(IntrinsicSize.Max),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = record.exampleSentence,
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

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

/**
 * 全部历史记录列表页 —— 点击首页"全部 →"进入
 * 讲义纸风列表，一行一词（与主页生词表同组件），点击进详情
 */
@Composable
fun HistoryListScreen(
    records: List<RecognitionRecord>,
    onBack: () -> Unit,
    onItemClick: (RecognitionRecord) -> Unit,
    onAudioClick: (RecognitionRecord) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paperBackground()
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
                    text = "← 返回讲义",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = inkBlue.copy(alpha = 0.65f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 页面标题
            Text(
                text = "全部历史记录",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = inkBlue,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
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
                        color = inkBlue.copy(alpha = 0.45f)
                    )
                }
            } else {
                // 历史记录列表（与主页生词表同款行组件）
                records.forEachIndexed { index, record ->
                    WordRow(
                        record = record,
                        onClick = { onItemClick(record) },
                        onAudioClick = { onAudioClick(record) },
                        index = index
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
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
