package com.example.lexiscan

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.lexiscan.ui.screens.CameraScreen
import com.example.lexiscan.ui.screens.HomeScreen
import com.example.lexiscan.ui.screens.ResultScreen
import com.example.lexiscan.ui.theme.LexiScanTheme
import com.example.lexiscan.viewmodel.RecognitionViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: RecognitionViewModel

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[RecognitionViewModel::class.java]

        setContent {
            LexiScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: RecognitionViewModel) {
    val historyState = viewModel.historyState.collectAsState()
    val recognitionState = viewModel.recognitionState.collectAsState()
    val flashEnabled = viewModel.flashEnabled.collectAsState()
    val isCollected = viewModel.isCollected.collectAsState()

    val history = when (val s = historyState.value) {
        is com.example.lexiscan.viewmodel.ScreenState.Success -> s.results
        else -> emptyList()
    }
    val isLoading = historyState.value is com.example.lexiscan.viewmodel.ScreenState.Loading
    val isRecognizing = recognitionState.value is com.example.lexiscan.viewmodel.RecognitionState.Recognizing
    val showResult = recognitionState.value is com.example.lexiscan.viewmodel.RecognitionState.Success
    val showCamera = remember { mutableStateOf(false) }

    when {
        showResult -> {
            val recState = recognitionState.value as com.example.lexiscan.viewmodel.RecognitionState.Success
            ResultScreen(
                result = recState.result,
                isCollected = isCollected.value,
                onBackClick = {
                    viewModel.resetRecognitionState()
                    showCamera.value = true
                },
                onHomeClick = {
                    viewModel.resetRecognitionState()
                    showCamera.value = false
                },
                onScanAgain = {
                    viewModel.resetRecognitionState()
                    showCamera.value = true
                },
                onCollectClick = { viewModel.toggleCollection() },
                onAudioClick = {}
            )
        }
        showCamera.value -> {
            CameraScreen(
                isFlashEnabled = flashEnabled.value,
                isRecognizing = isRecognizing,
                onBackClick = { showCamera.value = false },
                onFlashClick = { viewModel.toggleFlash() },
                onCaptureClick = { viewModel.recognize() },
                onGalleryClick = {}
            )
        }
        else -> {
            HomeScreen(
                history = history,
                isLoading = isLoading,
                onCameraClick = { showCamera.value = true }
            )
        }
    }
}
