package com.example.lexiscan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lexiscan.R
import com.example.lexiscan.ui.theme.*

// ══════════════════════════════════════════════════════════════════
//  LexiMascot —— 小狐狸IP形象封装组件
//  ───────────────────────────────────────────────────────────────────
//  代码结构说明（供后续 AI 功能接入参考）：
//
//  1. LexiExpression（枚举）：定义小狐狸所有表情状态
//     - 后续 AI 接入时，可根据对话状态切换表情
//
//  2. LexiMascot（Composable）：图片资源加载的小狐狸形象
//     - 参数：expression（表情）、size（尺寸）、enableFloat（浮动动画开关）
//     - 后续 AI 接入时直接调用此组件即可
//
//  3. LexiSpeechBubble（Composable）：对话气泡
//     - 用于展示小狐狸的语音/文字输出
//     - 后续 AI 接入时，将 AI 回复文本传入 text 参数
//
//  4. LexiOverlay（Composable）：全屏覆盖层（预留）
//     - 类似 Siri/小爱的全屏交互模式
//     - 后续 AI 接入时，在 LexiOverlay 中嵌入输入框和对话逻辑
//
//  5. LexiInteractionCallback（接口）：AI 交互回调（预留）
//     - 定义用户与小狐狸交互的回调接口
//     - 后续实现 AI 对话时实现此接口
// ══════════════════════════════════════════════════════════════════

/**
 * 小狐狸表情状态枚举
 * 后续 AI 接入时，可根据对话上下文切换表情
 */
enum class LexiExpression {
    /** 打招呼 — 首页欢迎区使用 */
    GREETING,

    /** 思考中 — 识别加载/等待AI回复时使用 */
    THINKING,

    /** 开心 — 识别成功/回答正确时使用 */
    HAPPY,

    /** 学习中 — 结果页展示时使用 */
    LEARNING,

    /** 空闲 — 默认状态，带眨眼动画 */
    IDLE
}

/**
 * AI 交互回调接口（预留，供后续 AI 功能接入时实现）
 */
interface LexiInteractionCallback {
    /** 用户点击小狐狸时触发 */
    fun onMascotClick()

    /** 用户提交问题时触发（后续 AI 对话接入点） */
    fun onUserSubmitQuestion(question: String)

    /** AI 回复完成时触发 */
    fun onAIResponseReceived(response: String)
}

/**
 * 小狐狸 IP 形象组件
 *
 * 通过图片资源加载戴学士帽的小狐狸，支持 5 种表情状态
 * 自带浮动动画，可通过 enableFloat 关闭
 *
 * @param expression 表情状态，默认 IDLE
 * @param modifier 布局修饰符
 * @param sizePx 狐狸尺寸（dp），默认 80dp
 * @param enableFloat 是否启用浮动动画，默认 true
 * @param onClick 点击回调（后续 AI 接入时用于唤醒对话）
 */
@Composable
fun LexiMascot(
    expression: LexiExpression = LexiExpression.IDLE,
    modifier: Modifier = Modifier,
    sizePx: Int = 80,
    enableFloat: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    // 表情 → 图片资源映射
    // 五种表情均有对应图片资源
    val mascotResId = when (expression) {
        LexiExpression.IDLE -> R.drawable.ic_lexi_idle
        LexiExpression.HAPPY -> R.drawable.ic_lexi_happy
        LexiExpression.GREETING -> R.drawable.ic_lexi_greeting
        LexiExpression.THINKING -> R.drawable.ic_lexi_thinking
        LexiExpression.LEARNING -> R.drawable.ic_lexi_learning
    }

    // 浮动动画：小狐狸上下轻微浮动，增加生动感
    val infiniteTransition = rememberInfiniteTransition(label = "lexi_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (enableFloat) 6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    // 思考动画：THINKING 状态时轻微缩放
    val thinkScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (expression == LexiExpression.THINKING) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "think_scale"
    )

    Box(
        modifier = modifier
            .size(sizePx.dp)
            .offset(y = floatOffset.dp)
            .scale(thinkScale)
            .let {
                if (onClick != null) it.clickable(onClick = onClick) else it
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = mascotResId),
            contentDescription = "Lexi 小狐狸",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * 小狐狸对话气泡组件
 *
 * 用于展示小狐狸的文字输出（学习提示、AI 回复等）
 *
 * @param text 气泡中的文字内容
 * @param modifier 布局修饰符
 * @param expression 关联的表情状态（影响气泡样式）
 */
@Composable
fun LexiSpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    expression: LexiExpression = LexiExpression.LEARNING
) {
    // 气泡背景色：根据表情状态微调
    val bubbleBg = when (expression) {
        LexiExpression.HAPPY -> Color(0xFFFFF4E6)       // 暖橙色背景
        LexiExpression.THINKING -> Color(0xFFF3F0FA)     // 淡紫背景
        else -> Color(0xFFFFF8F0)                         // 默认暖白
    }
    val bubbleAccent = when (expression) {
        LexiExpression.HAPPY -> warmOrange
        LexiExpression.THINKING -> warmPurple
        else -> warmTextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
            .background(bubbleBg)
            .border(1.dp, bubbleAccent.copy(alpha = 0.15f), RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = bubbleAccent,
            lineHeight = 20.sp
        )
    }
}

/**
 * 小狐狸 + 对话气泡组合组件
 *
 * 左侧小狐狸 + 右侧对话气泡的横向布局
 * 常用于结果页、首页欢迎区等场景
 *
 * @param expression 小狐狸表情
 * @param speechText 对话气泡文字
 * @param mascotSize 小狐狸尺寸
 */
@Composable
fun LexiWithBubble(
    expression: LexiExpression = LexiExpression.GREETING,
    speechText: String,
    mascotSize: Int = 64,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：小狐狸
        LexiMascot(
            expression = expression,
            sizePx = mascotSize,
            enableFloat = true
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 右侧：对话气泡
        LexiSpeechBubble(
            text = speechText,
            expression = expression,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 小狐狸全屏覆盖层（预留组件）
 *
 * 类似 Siri/小爱同学的全屏交互模式
 * 后续 AI 功能接入时，在此组件中嵌入：
 * 1. 底部文本输入框
 * 2. AI 回复展示区
 * 3. 对话历史滚动列表
 *
 * @param visible 是否显示
 * @param onDismiss 关闭回调
 * @param expression 小狐狸当前表情
 */
@Composable
fun LexiOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    expression: LexiExpression = LexiExpression.THINKING
) {
    if (!visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        // ── 底部交互区域（玻璃拟态效果）──
        // 后续 AI 接入时，在此处添加：
        // 1. 对话历史 LazyColumn
        // 2. 底部 TextField 输入框
        // 3. 发送按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(glassLight)
                .border(1.dp, glassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 小狐狸形象（大尺寸）
            LexiMascot(
                expression = expression,
                sizePx = 100,
                enableFloat = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 提示文字（后续替换为对话内容）
            Text(
                text = "我是 Lexi，有什么可以帮你？",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = warmTextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── 输入框占位（后续 AI 接入点）──
            // TODO: AI 接入时替换为 TextField + 发送按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.6f))
                    .border(1.dp, warmOrange.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "输入你的问题...",
                    fontSize = 14.sp,
                    color = warmTextTertiary,
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
        }
    }
}
