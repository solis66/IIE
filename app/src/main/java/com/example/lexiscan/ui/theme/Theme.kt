package com.example.lexiscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.lexiscan.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// ── 教授讲义配色系统（蓝黑备课笔记）──
// 品牌关键词：米色讲义纸 · 蓝黑钢笔 · 红笔批注
// 正文用蓝黑墨，红色只留给音标与重点标注（教授的红笔）

// 米色讲义纸背景
val paperBg = Color(0xFFF6F0DF)

// 纸面横线（浅蓝灰，极淡）
val paperLine = Color(0xFF5A6E96).copy(alpha = 0.11f)

// 蓝黑钢笔墨（正文）
val inkBlue = Color(0xFF2C3E5D)

// 红笔（音标、重点、强调动作）
val redPen = Color(0xFFB03A2E)

// ── 旧暖色系常量的映射（最小改动兼容，新代码请直接用上面的新名称）──
val warmBg = paperBg
val warmOrange = redPen
val warmOrangeLight = redPen
val warmPurple = redPen
val warmMint = inkBlue
val warmCardBg = Color.White
val warmTextPrimary = inkBlue
val warmTextSecondary = inkBlue.copy(alpha = 0.65f)
val warmTextTertiary = inkBlue.copy(alpha = 0.45f)
val warmShadow = Color(0x1A2C3E5D)
val glassLight = Color.White.copy(alpha = 0.15f)
val glassBorder = Color.White.copy(alpha = 0.25f)

// ── 英文手写字体：Caveat（OFL 开源，约 400KB 可变字重）──
// 用于英文单词、音标、英文例句与英文 UI 文案；中文一律保持系统字体
val CaveatFamily = FontFamily(Font(R.font.caveat, FontWeight.Normal))

// ── Material3 配色方案（本轮强制浅色讲义纸，不做暗色适配）──
private val LightColorScheme = lightColorScheme(
    primary = redPen,
    secondary = inkBlue,
    background = paperBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = inkBlue,
    onSurface = inkBlue
)

/**
 * LexiScan 主题入口
 * 使用教授讲义配色系统；忽略系统暗色模式（各界面按浅色纸面硬编码）
 */
@Composable
fun LexiScanTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // 状态栏图标固定深色（浅色纸面背景）
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = true
        )
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
