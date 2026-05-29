package com.example.lexiscan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lexiscan.data.model.RecognitionResult
import com.example.lexiscan.data.repository.RecognitionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScreenState {
    object Loading : ScreenState()
    data class Success(val results: List<RecognitionResult>) : ScreenState()
    data class Error(val message: String) : ScreenState()
}

sealed class RecognitionState {
    object Idle : RecognitionState()
    object Recognizing : RecognitionState()
    data class Success(val result: RecognitionResult) : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}

class RecognitionViewModel(
    private val repository: RecognitionRepository = RecognitionRepository()
) : ViewModel() {

    private val _historyState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val historyState: StateFlow<ScreenState> = _historyState

    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState

    private val _flashEnabled = MutableStateFlow(true)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled

    private val _isCollected = MutableStateFlow(false)
    val isCollected: StateFlow<Boolean> = _isCollected

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _historyState.value = ScreenState.Loading
            try {
                val results = repository.getHistory()
                _historyState.value = ScreenState.Success(results)
            } catch (e: Exception) {
                _historyState.value = ScreenState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun recognize(imageData: ByteArray = ByteArray(0)) {
        viewModelScope.launch {
            _recognitionState.value = RecognitionState.Recognizing
            try {
                val result = repository.recognize(imageData)
                _recognitionState.value = RecognitionState.Success(result)
                loadHistory()
            } catch (e: Exception) {
                _recognitionState.value = RecognitionState.Error(e.message ?: "识别失败")
            }
        }
    }

    fun toggleFlash() {
        _flashEnabled.value = !_flashEnabled.value
    }

    fun toggleCollection() {
        _isCollected.value = !_isCollected.value
    }

    fun resetRecognitionState() {
        _recognitionState.value = RecognitionState.Idle
        _isCollected.value = false
    }
}