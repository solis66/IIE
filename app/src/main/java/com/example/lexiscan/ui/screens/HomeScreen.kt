package com.example.lexiscan.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.lexiscan.R
import com.example.lexiscan.data.model.RecognitionRecord
import com.example.lexiscan.ui.components.LexiExpression
import com.example.lexiscan.ui.components.LexiMascot
import com.example.lexiscan.ui.components.LexiWithBubble
import com.example.lexiscan.ui.theme.*

/**
 * 首页主界面
 * 包含：头部标题区、拍照按钮、历史识别记录滚动式卡片
 */
@Composable
fun HomeScreen(
    history: List<RecognitionRecord>,
    isLoading: Boolean,
    onCameraClick: () -> Unit,
    onHistoryItemClick: (RecognitionRecord) -> Unit = {},
    onViewAllHistory: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(warmBg) // 暖米色背景，营造温暖感
    ) {
        if (isLoading) {
            // 加载状态：居中显示进度圈
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = warmOrange // 暖橙色进度圈，与主色调统一
            )
        } else {
            // 主内容区域：垂直滚动布局
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding() // 避开状态栏，让内容下移
            ) {
                // 头部标题区
                HeaderSection()

                // IP 欢迎区：小狐狸 Lexi + 对话气泡
                LexiWelcomeSection()

                // 拍照识别按钮卡片（拟物化设计）
                CameraButton(onClick = onCameraClick)

                // 历史记录区域（滚动式卡片设计）
                if (history.isNotEmpty()) {
                    HistorySection(
                        records = history,
                        onItemClick = onHistoryItemClick,
                        onViewAllClick = onViewAllHistory
                    )
                }

                // 底部留白
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * 头部标题区
 * LexiScan 标题 + 动态问候语 + "看见世界 · 解锁英语" 副标题
 */
@Composable
fun HeaderSection() {
    Column(
        modifier = Modifier.padding(
            horizontal = 24.dp,
            vertical = 36.dp // 增加顶部留白，让标题位置更协调
        )
    ) {
        // 主标题：LexiScan
        Text(
            text = "LexiScan",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = warmTextPrimary, // 深咖啡色标题
            letterSpacing = (-0.5).sp
        )
        // 动态问候语（根据时间变化）
        Text(
            text = getGreeting(),
            fontSize = 14.sp,
            color = warmOrange, // 暖橙色问候语，增加活力
            modifier = Modifier.padding(top = 6.dp)
        )
        // 副标题：看见世界 · 解锁英语
        Text(
            text = stringResource(R.string.app_slogan),
            fontSize = 14.sp,
            color = warmTextSecondary, // 暖灰副标题
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * IP 欢迎区 —— 小狐狸 Lexi 形象 + 对话气泡
 * 根据时间动态显示不同问候语
 */
@Composable
fun LexiWelcomeSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 小狐狸 Lexi 形象（GREETING 表情 + 浮动动画）
        LexiMascot(
            expression = LexiExpression.GREETING,
            sizePx = 56,
            enableFloat = true
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 对话气泡：根据时间显示不同问候语
        val welcomeText = when (getGreeting()) {
            "早上好" -> "早上好！今天也要元气满满地学习哦~"
            "下午好" -> "下午好！来识别几个新单词吧~"
            "晚上好" -> "晚上好！今天学了什么呢？"
            "中午好" -> "中午好！午休后学几个单词吧~"
            else -> "今天想学什么词呢？"
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .background(Color(0xFFFFF4E6))
                .border(
                    1.dp,
                    warmOrange.copy(alpha = 0.15f),
                    RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = welcomeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = warmOrange,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * 根据当前时间获取动态问候语
 */
private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 6 -> "夜深了，注意休息"
        hour < 12 -> "早上好"
        hour < 14 -> "中午好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }
}

/**
 * 拍照识别按钮卡片 —— 拟物化设计
 * 日落橙渐变背景 + 微高光顶部 + 双层阴影 + 相机图标
 * 模拟实体按键的立体感和质感
 */
@Composable
fun CameraButton(onClick: () -> Unit) {
    // 按压缩放动画：按下时缩小到 0.96，松开恢复，模拟实体按键反馈
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(150),
        label = "camera_btn_scale"
    )
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 双层阴影：外层柔和阴影 + 内层深色投影，营造立体悬浮感
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = warmOrange.copy(alpha = 0.25f),
                    spotColor = warmOrange.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(28.dp))
                // 日落橙渐变背景
                .background(
                    Brush.linearGradient(
                        colors = listOf(warmOrange, warmOrangeLight),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(0f, 100f)
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .scale(scale) // 按压缩放微动效
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(32.dp)
        ) {
            Column {
                // 顶部区域：相机图标 + 右箭头
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 相机图标容器（半透明白色毛玻璃效果）
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconCamera(modifier = Modifier.size(24.dp))
                    }
                    // 右箭头图标（引导用户点击）
                    Text(
                        text = "\u203A",
                        fontSize = 32.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 文字区域：主标题 + 副标题
                Column {
                    // 主标题："拍照识别物体"
                    Text(
                        text = stringResource(R.string.btn_camera),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    // 副标题："Tap to Scan & Learn"
                    Text(
                        text = stringResource(R.string.btn_camera_hint),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 相机图标（Canvas 绘制，白色风格）
 */
@Composable
fun IconCamera(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // 外圈（描边）
        drawCircle(
            color = Color.White,
            center = center,
            radius = width * 0.35f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
        )
        // 内圈（实心）
        drawCircle(
            color = Color.White,
            center = center,
            radius = width * 0.13f
        )
        // 角落小方块（镜头）
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.6f, height * 0.55f),
            size = androidx.compose.ui.geometry.Size(width * 0.22f, height * 0.22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
    }
}

/**
 * 历史记录区域 —— 滚动式卡片设计
 * 使用 VerticalPager 实现垂直滑动，用户上下滑动切换卡片
 * 滑动时上一个卡片从下面出现，下一个卡片从上面出现
 * 最多展示3张卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistorySection(
    records: List<RecognitionRecord>,
    onItemClick: (RecognitionRecord) -> Unit,
    onViewAllClick: () -> Unit
) {
    // 最多展示3张卡片
    val displayRecords = records.take(3)

    Column(modifier = Modifier.padding(top = 16.dp)) {
        // 标题行：左侧"历史识别记录" + 右侧"查看全部"
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
                color = warmTextPrimary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = stringResource(R.string.history_view_all),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = warmOrange, // 暖橙色"查看全部"链接
                modifier = Modifier.clickable(onClick = onViewAllClick)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 滚动式卡片区域：使用 VerticalPager 实现垂直分页滑动
        // 用户上下滑动切换卡片，滑动时上一张从下方出现，下一张从上方出现
        val pagerState = rememberPagerState(pageCount = { displayRecords.size })

        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp), // 固定高度，确保圆柱体卡片+悬空阴影完整展示
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp
        ) { page ->
            val record = displayRecords[page]
            // 当前页和相邻页的缩放/透明度效果，增强层次感
            val pageOffset = (pagerState.currentPage - page).coerceIn(-1, 1)
            val alpha = 1f - kotlin.math.abs(pageOffset) * 0.3f
            val scale = 1f - kotlin.math.abs(pageOffset) * 0.05f

            ScrollableHistoryCard(
                record = record,
                onClick = { onItemClick(record) },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsAlpha(alpha)
                    .graphicsScale(scale)
            )
        }

        // 页面指示器（小圆点）
        if (displayRecords.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(displayRecords.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) warmOrange else warmTextTertiary.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * 可滑动的历史记录卡片 —— 圆柱体造型 + 悬空效果
 * 使用水平渐变模拟圆柱体表面光照，大圆角模拟圆柱截面
 * 多层阴影实现悬浮在空中的视觉效果
 */
@Composable
fun ScrollableHistoryCard(
    record: RecognitionRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 圆柱体形状：大圆角模拟圆柱截面弧度
    val cylinderShape = RoundedCornerShape(36.dp)

    Box(
        modifier = modifier
            // 悬空效果：大投影 + 偏移，模拟卡片漂浮在空中
            .shadow(
                elevation = 20.dp,
                shape = cylinderShape,
                ambientColor = warmShadow.copy(alpha = 0.12f),
                spotColor = warmShadow.copy(alpha = 0.35f)
            )
            .clip(cylinderShape)
            // 圆柱体表面：水平渐变（左暗→中亮→右暗），模拟圆柱曲面的光照
            .background(
                Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFFF5F0EA),  // 左侧暗（曲面阴影）
                        0.3f to Color(0xFFFCFAF7),  // 左过渡
                        0.5f to Color(0xFFFFFFFF),  // 中间亮（曲面高光）
                        0.7f to Color(0xFFFCFAF7),  // 右过渡
                        1.0f to Color(0xFFF5F0EA)   // 右侧暗（曲面阴影）
                    )
                ),
                shape = cylinderShape
            )
            .clickable(onClick = onClick)
    ) {
        // 顶部高光线条：模拟圆柱体顶部的反光带
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.7f),
                            Color.White.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 底部暗影线条：模拟圆柱体底部的阴影
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            warmShadow.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 卡片内容区域
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 物品缩略图（圆柱体内的画框效果）
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(
                        6.dp,
                        RoundedCornerShape(18.dp),
                        ambientColor = warmShadow.copy(alpha = 0.2f)
                    )
                    .clip(RoundedCornerShape(18.dp))
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
                        .background(Color(0xFFF0EAE1)),
                    contentScale = ContentScale.Crop
                )
            }

            // 文字信息区域（聚集式设计）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp)
            ) {
                // 第一行：英文单词 + 时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.englishName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = warmTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatTime(record.timestamp),
                        fontSize = 11.sp,
                        color = warmTextTertiary
                    )
                }

                // 第二行：中文翻译 + 音标
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.chineseName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = warmTextSecondary
                    )
                    if (!record.phonetic.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(warmPurple.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = record.phonetic,
                                fontSize = 12.sp,
                                color = warmPurple
                            )
                        }
                    }
                }

                // 第三行：置信度标签
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(warmOrange.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "置信 ${(record.confidence * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = warmOrange
                    )
                }
            }
        }
    }
}

// 辅助扩展函数：避免与 Modifier.alpha 命名冲突
private fun Modifier.graphicsAlpha(value: Float): Modifier =
    this.then(Modifier.alpha(value))

private fun Modifier.graphicsScale(value: Float): Modifier =
    this.then(Modifier.scale(value))

/**
 * 格式化时间戳为相对时间字符串
 */
private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

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