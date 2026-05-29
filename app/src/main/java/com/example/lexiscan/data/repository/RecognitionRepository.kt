package com.example.lexiscan.data.repository

import com.example.lexiscan.data.model.RecognitionResult
import kotlinx.coroutines.delay

class RecognitionRepository {
    private val mockHistory = listOf(
        RecognitionResult(
            id = "1",
            englishWord = "Hamburger",
            phonetic = "/ˈhæmbɜːɡə(r)/",
            chineseMeaning = "汉堡包",
            partOfSpeech = "n.",
            exampleSentence = "\"I had a hamburger for lunch.\"",
            exampleTranslation = "我午餐吃了一个汉堡包。",
            imageUrl = "https://images.unsplash.com/photo-1512152272829-e3139592d56f?w=150&auto=format&fit=crop&q=60",
            timestamp = System.currentTimeMillis() - 600000
        ),
        RecognitionResult(
            id = "2",
            englishWord = "Laptop",
            phonetic = "/ˈlæptɒp/",
            chineseMeaning = "笔记本电脑",
            partOfSpeech = "n.",
            exampleSentence = "\"She works on her laptop every day.\"",
            exampleTranslation = "她每天都在笔记本电脑上工作。",
            imageUrl = "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?w=150&auto=format&fit=crop&q=60",
            timestamp = System.currentTimeMillis() - 7200000
        ),
        RecognitionResult(
            id = "3",
            englishWord = "Book",
            phonetic = "/bʊk/",
            chineseMeaning = "书，书籍",
            partOfSpeech = "n.",
            exampleSentence = "\"He is reading a book.\"",
            exampleTranslation = "他正在看书。",
            imageUrl = "https://images.unsplash.com/photo-1544816155-12df9643f363?w=150&auto=format&fit=crop&q=60",
            timestamp = System.currentTimeMillis() - 86400000
        )
    )

    private val recognitionResults = mutableListOf<RecognitionResult>().apply {
        addAll(mockHistory)
    }

    suspend fun getHistory(): List<RecognitionResult> {
        delay(200)
        return recognitionResults.sortedByDescending { it.timestamp }
    }

    suspend fun recognize(imageData: ByteArray): RecognitionResult {
        delay(2000)
        val result = RecognitionResult(
            id = System.currentTimeMillis().toString(),
            englishWord = "Coffee Cup",
            phonetic = "/ˈkɒfi kʌp/",
            chineseMeaning = "咖啡杯",
            partOfSpeech = "n.",
            exampleSentence = "\"He filled his coffee cup to the brim.\"",
            exampleTranslation = "他把咖啡杯倒得满满的。",
            imageUrl = "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600&auto=format&fit=crop&q=80",
            timestamp = System.currentTimeMillis()
        )
        recognitionResults.add(result)
        return result
    }
}