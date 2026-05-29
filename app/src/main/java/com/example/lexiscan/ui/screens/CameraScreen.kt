package com.example.lexiscan.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lexiscan.R
import com.example.lexiscan.ui.theme.iosBlue
import com.example.lexiscan.ui.theme.iosYellow

@Composable
fun CameraScreen(
    isFlashEnabled: Boolean,
    isRecognizing: Boolean,
    onBackClick: () -> Unit,
    onFlashClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = ColorPainter(Color.Transparent),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\u2039", fontSize = 24.sp, color = Color.White)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClick = onFlashClick),
                contentAlignment = Alignment.Center
            ) {
                IconFlash(
                    modifier = Modifier.size(24.dp),
                    enabled = isFlashEnabled
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(top = 120.dp), contentAlignment = Alignment.Center) {
            ScanFrame(
                modifier = Modifier.size(256.dp),
                isRecognizing = isRecognizing
            )
        }

        if (isRecognizing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingToast()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp).padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable(onClick = onGalleryClick),
                    contentAlignment = Alignment.Center
                ) {
                    IconGallery(modifier = Modifier.size(24.dp))
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable(onClick = onCaptureClick),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(0.85f).clip(CircleShape).background(Color.White)
                    )
                }

                Box(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun ScanFrame(modifier: Modifier = Modifier, isRecognizing: Boolean) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxSize().border(
                width = 2.dp,
                color = iosBlue.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            )
        )

        val cornerSize = 24.dp
        val cornerStroke = 4.dp
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            Box(
                modifier = Modifier
                    .size(cornerSize)
                    .border(
                        width = cornerStroke,
                        color = iosBlue,
                        shape = RoundedCornerShape(topStart = 12.dp)
                    )
            )
        }
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            Box(
                modifier = Modifier
                    .size(cornerSize)
                    .border(
                        width = cornerStroke,
                        color = iosBlue,
                        shape = RoundedCornerShape(topEnd = 12.dp)
                    )
            )
        }
        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            Box(
                modifier = Modifier
                    .size(cornerSize)
                    .border(
                        width = cornerStroke,
                        color = iosBlue,
                        shape = RoundedCornerShape(bottomStart = 12.dp)
                    )
            )
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            Box(
                modifier = Modifier
                    .size(cornerSize)
                    .border(
                        width = cornerStroke,
                        color = iosBlue,
                        shape = RoundedCornerShape(bottomEnd = 12.dp)
                    )
            )
        }

        if (isRecognizing) {
            ScanLine(modifier = Modifier.padding(8.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(iosBlue.copy(alpha = 0.8f), shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBrain(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "检测到目标物体",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ScanLine(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val topOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, iosBlue, Color.Transparent)
                    )
                )
                .shadow(elevation = 8.dp)
                .offset(y = 256.dp * topOffset)
        )
    }
}

@Composable
fun LoadingToast() {
    Box(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = iosBlue,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.loading_recognizing),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun IconFlash(modifier: Modifier = Modifier, enabled: Boolean) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        
        drawLine(
            color = if (enabled) iosYellow else Color.White.copy(alpha = 0.6f),
            start = androidx.compose.ui.geometry.Offset(centerX, height * 0.1f),
            end = androidx.compose.ui.geometry.Offset(centerX, height * 0.7f),
            strokeWidth = 3.dp.toPx()
        )
        
        drawLine(
            color = if (enabled) iosYellow else Color.White.copy(alpha = 0.6f),
            start = androidx.compose.ui.geometry.Offset(width * 0.3f, height * 0.4f),
            end = androidx.compose.ui.geometry.Offset(width * 0.7f, height * 0.4f),
            strokeWidth = 3.dp.toPx()
        )
        
        drawLine(
            color = if (enabled) iosYellow else Color.White.copy(alpha = 0.6f),
            start = androidx.compose.ui.geometry.Offset(width * 0.35f, height * 0.55f),
            end = androidx.compose.ui.geometry.Offset(width * 0.65f, height * 0.55f),
            strokeWidth = 3.dp.toPx()
        )
        
        drawLine(
            color = if (enabled) iosYellow else Color.White.copy(alpha = 0.6f),
            start = androidx.compose.ui.geometry.Offset(width * 0.15f, height * 0.3f),
            end = androidx.compose.ui.geometry.Offset(width * 0.25f, height * 0.35f),
            strokeWidth = 2.dp.toPx()
        )
        
        drawLine(
            color = if (enabled) iosYellow else Color.White.copy(alpha = 0.6f),
            start = androidx.compose.ui.geometry.Offset(width * 0.75f, height * 0.35f),
            end = androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.3f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun IconGallery(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val color = Color.White
        
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(width * 0.9f, height * 0.9f),
            cornerRadius = cornerRadius
        )
        
        drawRoundRect(
            color = color.copy(alpha = 0.6f),
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.45f, height * 0.45f),
            size = androidx.compose.ui.geometry.Size(width * 0.45f, height * 0.45f),
            cornerRadius = cornerRadius
        )
        
        drawCircle(
            color = color,
            center = androidx.compose.ui.geometry.Offset(width * 0.25f, height * 0.65f),
            radius = width * 0.12f
        )
    }
}

@Composable
fun IconBrain(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val color = Color.White
        
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.1f),
            size = androidx.compose.ui.geometry.Size(width * 0.4f, height * 0.8f)
        )
        
        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.1f),
            size = androidx.compose.ui.geometry.Size(width * 0.4f, height * 0.8f)
        )
        
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.3f),
            end = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.7f),
            strokeWidth = 2.dp.toPx()
        )
    }
}