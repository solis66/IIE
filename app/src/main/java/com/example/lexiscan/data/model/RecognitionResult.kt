package com.example.lexiscan.data.model

data class RecognitionResult(
    val id: String,
    val englishWord: String,
    val phonetic: String,
    val chineseMeaning: String,
    val partOfSpeech: String,
    val exampleSentence: String,
    val exampleTranslation: String,
    val imageUrl: String,
    val timestamp: Long
)