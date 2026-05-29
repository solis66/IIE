package com.example.lexiscan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.lexiscan.R
import com.example.lexiscan.data.model.RecognitionResult
import com.example.lexiscan.ui.theme.iosBlue
import com.example.lexiscan.ui.theme.iosGreen

@Composable
fun ResultScreen(
    result: RecognitionResult,
    isCollected: Boolean,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onScanAgain: () -> Unit,
    onCollectClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showContent = true
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(result.imageUrl)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }

        Box(
            modifier = Modifier.fillMaxSize().clickable(onClick = onBackClick)
        )

        AnimatedVisibility(
            visible = showContent,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        color = Color.White.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .height(4.dp)
                            .background(Color(0xFFA3A3A3).copy(alpha = 0.4f), shape = RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = result.englishWord,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1A1A1A),
                                letterSpacing = -0.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = result.phonetic,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF737373),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(iosBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp))
                                        .clickable(onClick = {
                                            isMuted = !isMuted
                                            onAudioClick()
                                        }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isMuted) {
                                        IconVolumeMute(modifier = Modifier.size(16.dp))
                                    } else {
                                        IconVolumeUp(modifier = Modifier.size(16.dp))
                                    }
                                }
                                LaunchedEffect(isMuted) {
                                    if (isMuted) {
                                        kotlinx.coroutines.delay(800)
                                        isMuted = false
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (isCollected) Color(0xFFFEFCE8) else Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .clickable(onClick = onCollectClick),
                            contentAlignment = Alignment.Center
                        ) {
                            IconStar(
                                modifier = Modifier.size(24.dp),
                                filled = isCollected
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.label_chinese).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosBlue,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${result.partOfSpeech} ${result.chineseMeaning}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.label_example).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosGreen,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.exampleSentence,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF404040),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.exampleTranslation,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF737373)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(iosBlue, shape = RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFD4D4D4), shape = RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFD4D4D4), shape = RoundedCornerShape(4.dp)))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .background(Color(0xFFE5E5E5).copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp))
                                .clickable(onClick = onHomeClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.btn_back_home),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .background(iosBlue, shape = RoundedCornerShape(16.dp))
                                .clickable(onClick = onScanAgain),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.btn_scan_again),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun IconVolumeUp(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val color = iosBlue
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width * 0.3f, height * 0.2f),
            end = androidx.compose.ui.geometry.Offset(width * 0.3f, height * 0.8f),
            strokeWidth = 2.dp.toPx()
        )
        
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.3f),
            end = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.7f),
            strokeWidth = 2.dp.toPx()
        )
        
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width * 0.7f, height * 0.4f),
            end = androidx.compose.ui.geometry.Offset(width * 0.7f, height * 0.6f),
            strokeWidth = 2.dp.toPx()
        )
        
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(width * 0.5f, width * 0.5f),
            style = stroke
        )
        
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(0f, height * 0.5f),
            size = androidx.compose.ui.geometry.Size(width * 0.5f, width * 0.5f),
            style = stroke
        )
    }
}

@Composable
fun IconVolumeMute(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val color = iosBlue
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width * 0.2f, height * 0.2f),
            end = androidx.compose.ui.geometry.Offset(width * 0.8f, height * 0.8f),
            strokeWidth = 2.dp.toPx()
        )
        
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width * 0.8f, height * 0.2f),
            end = androidx.compose.ui.geometry.Offset(width * 0.2f, height * 0.8f),
            strokeWidth = 2.dp.toPx()
        )
        
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(width * 0.5f, width * 0.5f),
            style = stroke
        )
        
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(0f, height * 0.5f),
            size = androidx.compose.ui.geometry.Size(width * 0.5f, width * 0.5f),
            style = stroke
        )
    }
}

@Composable
fun IconStar(modifier: Modifier = Modifier, filled: Boolean) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val color = if (filled) Color(0xFFEAB308) else Color(0xFFA3A3A3)
        
        val points = listOf(
            androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.1f),
            androidx.compose.ui.geometry.Offset(width * 0.61f, height * 0.38f),
            androidx.compose.ui.geometry.Offset(width * 0.9f, height * 0.38f),
            androidx.compose.ui.geometry.Offset(width * 0.68f, height * 0.57f),
            androidx.compose.ui.geometry.Offset(width * 0.79f, height * 0.85f),
            androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.7f),
            androidx.compose.ui.geometry.Offset(width * 0.21f, height * 0.85f),
            androidx.compose.ui.geometry.Offset(width * 0.32f, height * 0.57f),
            androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.38f),
            androidx.compose.ui.geometry.Offset(width * 0.39f, height * 0.38f)
        )
        
        if (filled) {
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.forEach { lineTo(it.x, it.y) }
                    close()
                },
                color = color
            )
        } else {
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.forEach { lineTo(it.x, it.y) }
                    close()
                },
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}