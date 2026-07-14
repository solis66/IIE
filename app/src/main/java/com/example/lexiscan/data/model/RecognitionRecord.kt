package com.example.lexiscan.data.model

/**
 * 识别历史记录（含LLM标准化后的完整学习信息，用于持久化）
 */
data class RecognitionRecord(
    val id: String,
    val imageUri: String,
    val englishName: String,
    val chineseName: String,
    val confidence: Float,
    val phonetic: String? = null,
    val plural: String? = null,           // 复数形式
    val exampleSentence: String? = null,  // 少儿英语短句例句
    val category: String? = null,
    val recognitionMode: String = "SINGLE_OBJECT",
    val timestamp: Long = System.currentTimeMillis()
)
