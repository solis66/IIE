package com.example.lexiscan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.lexiscan.data.model.RecognitionRecord
import com.example.lexiscan.ui.screens.CameraScreen
import com.example.lexiscan.ui.screens.HistoryDetailScreen
import com.example.lexiscan.ui.screens.HistoryListScreen
import com.example.lexiscan.ui.screens.HomeScreen
import com.example.lexiscan.ui.screens.RecognitionResultDialog
import com.example.lexiscan.ui.theme.LexiScanTheme
import com.example.lexiscan.ui.theme.warmOrange
import com.example.lexiscan.viewmodel.RecognitionViewModel
import com.example.lexiscan.util.TtsManager

class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: TtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 TTS 管理器
        ttsManager = TtsManager(this)
        
        setContent {
            LexiScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: RecognitionViewModel = viewModel()
                    AppNavigation(viewModel = viewModel, ttsManager = ttsManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}

/**
 * 应用导航组件
 * 管理首页、相机页、结果页、历史详情页之间的切换逻辑
 */
@Composable
fun AppNavigation(viewModel: RecognitionViewModel, ttsManager: TtsManager) {
    val context = LocalContext.current
    val historyState = viewModel.historyState.collectAsState()
    val recognitionState = viewModel.recognitionState.collectAsState()
    val isTorchOn = viewModel.isTorchOn.collectAsState()
    val isCollected = viewModel.isCollected.collectAsState()
    val currentImageUri = viewModel.currentImageUri.collectAsState()
    val animeImageUri = viewModel.animeImageUri.collectAsState()
    val phonetic = viewModel.phonetic.collectAsState()
    val chineseName = viewModel.chineseName.collectAsState()
    val plural = viewModel.plural.collectAsState()
    val exampleSentence = viewModel.exampleSentence.collectAsState()
    val isEnglishValid = viewModel.isEnglishValid.collectAsState()
    val isSegmented = viewModel.isSegmented.collectAsState()
    val segmentedForegroundUri = viewModel.segmentedForegroundUri.collectAsState()

    // 解析历史记录列表
    val history = when (val s = historyState.value) {
        is com.example.lexiscan.viewmodel.ScreenState.Success -> s.results
        else -> emptyList()
    }
    val isLoading = historyState.value is com.example.lexiscan.viewmodel.ScreenState.Loading
    val isRecognizing = recognitionState.value is com.example.lexiscan.viewmodel.RecognitionState.Recognizing
    val showResult = recognitionState.value is com.example.lexiscan.viewmodel.RecognitionState.Success

    // 导航状态
    var showCamera by remember { mutableStateOf(false) }
    
    // 历史记录详情页状态
    var selectedHistoryRecord by remember { mutableStateOf<RecognitionRecord?>(null) }
    
    // 是否显示全部历史记录
    var showAllHistory by remember { mutableStateOf(false) }

    // 相机权限状态
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 相机权限请求器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            showCamera = true
        } else {
            Toast.makeText(context, "需要相机权限才能拍照识别", Toast.LENGTH_SHORT).show()
        }
    }

    // 相册选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.recognizeObject(uri)
        }
    }

    // 监听识别错误状态
    LaunchedEffect(recognitionState.value) {
        if (recognitionState.value is com.example.lexiscan.viewmodel.RecognitionState.Error) {
            val errorState = recognitionState.value as com.example.lexiscan.viewmodel.RecognitionState.Error
            Toast.makeText(context, "识别失败: ${errorState.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 导航逻辑
    when {
        showResult -> {
            // 显示识别结果页
            val recState = recognitionState.value as com.example.lexiscan.viewmodel.RecognitionState.Success
            val objResult = recState.result
            RecognitionResultDialog(
                imageUri = animeImageUri.value,
                name = objResult.itemName.english,
                confidence = objResult.confidence,
                phonetic = phonetic.value,
                chineseName = chineseName.value,
                plural = plural.value,
                exampleSentence = exampleSentence.value,
                isEnglishValid = isEnglishValid.value,
                isSegmented = isSegmented.value,
                isCollected = isCollected.value,
                onBackHome = {
                    viewModel.resetRecognitionState()
                    showCamera = false
                },
                onContinue = {
                    viewModel.resetRecognitionState()
                },
                onCollectClick = { viewModel.toggleCollection() },
                onAudioClick = {
                    // 点击朗读英文单词
                    ttsManager.speak(objResult.itemName.english)
                }
            )
        }
        showAllHistory -> {
            // 显示全部历史记录列表页（点击"查看更多"进入）
            HistoryListScreen(
                records = history,
                onBack = { showAllHistory = false },
                onItemClick = { record ->
                    selectedHistoryRecord = record
                    showAllHistory = false
                }
            )
        }
        selectedHistoryRecord != null -> {
            // 显示单条历史记录详情页
            HistoryDetailScreen(
                record = selectedHistoryRecord!!,
                onBack = { selectedHistoryRecord = null },
                onAudioClick = {
                    // 点击朗读历史记录中的英文单词
                    ttsManager.speak(selectedHistoryRecord!!.englishName)
                }
            )
        }
        showCamera -> {
            // 显示相机页
            if (hasCameraPermission) {
                CameraScreen(
                    isTorchOn = isTorchOn.value,
                    isRecognizing = isRecognizing,
                    capturedImageUri = currentImageUri.value,
                    segmentedForegroundUri = segmentedForegroundUri.value,
                    onBackClick = { showCamera = false },
                    onTorchToggle = { viewModel.setTorchOn(!isTorchOn.value) },
                    onCaptureClick = {},
                    onGalleryClick = { galleryLauncher.launch("image/*") },
                    onPhotoCaptured = { uri -> viewModel.recognizeObject(uri) }
                )
            } else {
                // 显示权限请求页
                CameraPermissionRequest(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onBackClick = { showCamera = false }
                )
            }
        }
        else -> {
            // 显示首页
            HomeScreen(
                history = history,
                isLoading = isLoading,
                onCameraClick = {
                    if (hasCameraPermission) {
                        showCamera = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onHistoryItemClick = { record -> selectedHistoryRecord = record },
                onViewAllHistory = { showAllHistory = true }
            )
        }
    }
}

/**
 * 相机权限请求页面
 * 当用户未授予相机权限时显示
 */
@Composable
fun CameraPermissionRequest(
    onRequestPermission: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\uD83D\uDCF7",
                fontSize = 48.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "需要相机权限",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "请授予相机权限以使用拍照识别功能",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = warmOrange)
            ) {
                Text("授予权限")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "返回",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .clickable(onClick = onBackClick)
                    .padding(vertical = 8.dp, horizontal = 16.dp)
            )
        }
    }
}