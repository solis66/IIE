package com.example.lexiscan.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.lexiscan.R
import com.example.lexiscan.data.model.RecognitionResult
import com.example.lexiscan.ui.theme.iosBlue
import com.example.lexiscan.ui.theme.iosBg

@Composable
fun HomeScreen(
    history: List<RecognitionResult>,
    isLoading: Boolean,
    onCameraClick: () -> Unit,
    onHistoryItemClick: (RecognitionResult) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().background(iosBg)) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = iosBlue
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 56.dp, bottom = 16.dp)
            ) {
                item {
                    HeaderSection()
                }
                
                item {
                    CameraButton(onClick = onCameraClick)
                }
                
                item {
                    HistorySection(history, onHistoryItemClick)
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            text = "LexiScan",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )
        Text(
            text = stringResource(R.string.app_slogan),
            fontSize = 14.sp,
            color = Color(0xFF737373),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun CameraButton(onClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(iosBlue, Color(0xFF0066E6))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconCamera(modifier = Modifier.size(24.dp))
                }
                
                Text(
                    text = "\u2039",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Text(
                    text = stringResource(R.string.btn_camera),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.btn_camera_hint),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun IconCamera(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        
        drawCircle(
            color = Color.White,
            center = center,
            radius = width * 0.35f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )
        
        drawCircle(
            color = Color.White,
            center = center,
            radius = width * 0.15f
        )
        
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.6f, height * 0.55f),
            size = androidx.compose.ui.geometry.Size(width * 0.25f, height * 0.25f),
            cornerRadius = cornerRadius
        )
    }
}

@Composable
fun HistorySection(history: List<RecognitionResult>, onItemClick: (RecognitionResult) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_title),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF737373),
                letterSpacing = 1.sp
            )
            Text(
                text = stringResource(R.string.history_view_all),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = iosBlue
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            history.forEach { item ->
                HistoryItem(result = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
fun HistoryItem(result: RecognitionResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White, shape = RoundedCornerShape(16.dp))
            .padding(14.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(result.imageUrl)
                    .crossfade(true)
                    .build()
            ),
            contentDescription = result.englishWord,
            modifier = Modifier.size(56.dp).background(Color(0xFFF5F5F5), shape = RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.englishWord,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = formatTime(result.timestamp),
                    fontSize = 11.sp,
                    color = Color(0xFFA3A3A3)
                )
            }
            Text(
                text = result.phonetic,
                fontSize = 12.sp,
                color = Color(0xFFA3A3A3),
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "${result.partOfSpeech} ${result.chineseMeaning}",
                fontSize = 12.sp,
                color = Color(0xFF525252),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

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