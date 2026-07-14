package com.example.lexiscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// ── 暖色调色彩系统（第一阶段核心视觉升级）──
// 品牌关键词：温暖 · 智趣 · 陪伴式学习

// P0 主强调色 - 暖橙色（用于主按钮、核心行动）
val warmOrange = Color(0xFFFF7E5F)
val warmOrangeLight = Color(0xFFFEB47B)

// P1 次强调色 - 淡紫色（用于音标、收藏等功能标签）
val warmPurple = Color(0xFF9B89B3)

// P2 辅助色 - 薄荷绿（用于复数、成功状态）
val warmMint = Color(0xFF88D8B0)

// 背景色 - 暖米色（加深以提升状态栏文字可见性，保持暖色调）
val warmBg = Color(0xFFF3E9DD)

// 卡片背景 - 纯白
val warmCardBg = Color.White

// 文字颜色层级
val warmTextPrimary = Color(0xFF3E342F)  // 深咖啡色（代替纯黑，更柔和）
val warmTextSecondary = Color(0xFF8C7E74) // 暖灰文字
val warmTextTertiary = Color(0xFFBDB2AA)  // 浅暖灰（用于时间、置信度等）

// 阴影色 - 暖色阴影，更自然
val warmShadow = Color(0x1A3E342F)

// 玻璃拟态颜色
val glassLight = Color.White.copy(alpha = 0.15f)
val glassBorder = Color.White.copy(alpha = 0.25f)

// 暗色模式颜色
val darkBg = Color(0xFF2A2420)
val darkSurface = Color(0xFF3D3430)

// ── Material3 配色方案 ──
private val DarkColorScheme = darkColorScheme(
    primary = warmOrange,
    secondary = warmPurple,
    background = darkBg,
    surface = darkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = warmOrange,
    secondary = warmPurple,
    background = warmBg,
    surface = warmCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = warmTextPrimary,
    onSurface = warmTextPrimary
)

/**
 * LexiScan 主题入口
 * 使用暖色调色彩系统，统一全应用视觉风格
 */
@Composable
fun LexiScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // 状态栏图标颜色适配：浅色模式用深色图标，深色模式用浅色图标
    // 解决小米等机型在浅色背景上看不清状态栏文字的问题
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = !darkTheme
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}