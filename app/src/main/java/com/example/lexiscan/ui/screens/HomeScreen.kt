package com.example.lexiscan.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lexiscan.R
import com.example.lexiscan.data.model.RecognitionRecord
import com.example.lexiscan.ui.components.SketchTextButton
import com.example.lexiscan.ui.components.WordRow
import com.example.lexiscan.ui.components.paperBackground
import com.example.lexiscan.ui.components.sketchBorder
import com.example.lexiscan.ui.theme.CaveatFamily
import com.example.lexiscan.ui.theme.inkBlue
import com.example.lexiscan.ui.theme.redPen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 首页 —— 教授讲义风
 * 讲义头（标题+日期）→ 手绘虚线"拍一拍"入口 → 最近学的词（生词表，最近5条）
 */
@Composable
fun HomeScreen(
    history: List<RecognitionRecord>,
    isLoading: Boolean,
    onCameraClick: () -> Unit,
    onHistoryItemClick: (RecognitionRecord) -> Unit = {},
    onViewAllHistory: () -> Unit = {},
    onAudioClick: (RecognitionRecord) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paperBackground()
    ) {
        if (isLoading) {
            // 加载状态：居中墨蓝色进度圈
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = inkBlue
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
            ) {
                HeaderSection()
                CameraEntryButton(onClick = onCameraClick)
                RecentWordsSection(
                    records = history,
                    onItemClick = onHistoryItemClick,
                    onViewAllClick = onViewAllHistory,
                    onAudioClick = onAudioClick,
                    onEmptyClick = onCameraClick
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * 讲义头：LexiScan（Caveat 手写）+ 右侧日期 + 副标题 + 分隔线
 */
@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "LexiScan",
                fontFamily = CaveatFamily,
                fontSize = 40.sp,
                lineHeight = 42.sp,
                color = inkBlue
            )
            // 日期：词汇讲义 · 10月24日
            val dateText = remember {
                val sdf = SimpleDateFormat("M月d日", Locale.CHINESE)
                "词汇讲义 · ${sdf.format(Date())}"
            }
            Text(
                text = dateText,
                fontSize = 13.sp,
                color = inkBlue.copy(alpha = 0.65f)
            )
        }
        Text(
            text = stringResource(R.string.app_slogan),
            fontSize = 13.sp,
            color = inkBlue.copy(alpha = 0.65f),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        // 讲义头分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(1.dp)
                .background(inkBlue.copy(alpha = 0.25f))
        )
    }
}

/**
 * 拍照入口：手绘虚线框 + "拍一拍 · 记个新词"（红笔）+ 英文提示
 * 保留按压缩放反馈
 */
@Composable
private fun CameraEntryButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150),
        label = "camera_entry_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .scale(scale)
            .sketchBorder(inkBlue.copy(alpha = 0.55f), strokeWidth = 2.dp)
            .background(Color.White.copy(alpha = 0.45f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.btn_camera),
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                color = redPen
            )
            Text(
                text = stringResource(R.string.btn_camera_hint),
                fontFamily = CaveatFamily,
                fontSize = 15.sp,
                color = inkBlue.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * 最近学的词（生词表）：区块标题行 + 最近5条 WordRow / 空状态
 */
@Composable
private fun RecentWordsSection(
    records: List<RecognitionRecord>,
    onItemClick: (RecognitionRecord) -> Unit,
    onViewAllClick: () -> Unit,
    onAudioClick: (RecognitionRecord) -> Unit,
    onEmptyClick: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        // 标题行：左"最近学的词" + 右红笔"全部 →"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = inkBlue
            )
            Text(
                text = stringResource(R.string.history_view_all),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = redPen,
                modifier = Modifier.clickable(onClick = onViewAllClick)
            )
        }

        if (records.isEmpty()) {
            // 空状态：红笔手写提示，点击去拍照
            SketchTextButton(
                text = "还没有标本，拍第一个试试吧 →",
                onClick = onEmptyClick,
                textColor = redPen,
                fontSize = 15,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            )
        } else {
            // 生词表：最近 5 条，一行一词
            records.take(5).forEachIndexed { index, record ->
                WordRow(
                    record = record,
                    onClick = { onItemClick(record) },
                    onAudioClick = { onAudioClick(record) },
                    index = index
                )
            }
        }
    }
}
