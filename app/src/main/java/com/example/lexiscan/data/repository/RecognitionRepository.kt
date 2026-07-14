package com.example.lexiscan.data.repository

import android.content.Context
import com.example.lexiscan.data.model.RecognitionRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RecognitionRepository(private val context: Context) {

    private val gson = Gson()
    private val recordsFile: File
        get() = File(context.filesDir, "recognition_records.json")

    private var records = mutableListOf<RecognitionRecord>()

    init {
        loadFromFile()
    }

    private fun loadFromFile() {
        try {
            if (recordsFile.exists()) {
                val json = recordsFile.readText()
                val type = object : TypeToken<List<RecognitionRecord>>() {}.type
                val loaded: List<RecognitionRecord> = gson.fromJson(json, type)
                records.clear()
                records.addAll(loaded)
            }
        } catch (_: Exception) {
            records.clear()
        }
    }

    private fun saveToFile() {
        try {
            val json = gson.toJson(records.toList())
            recordsFile.writeText(json)
        } catch (_: Exception) {
        }
    }

    suspend fun addRecord(record: RecognitionRecord) = withContext(Dispatchers.IO) {
        records.add(0, record)
        saveToFile()
    }

    suspend fun getHistory(): List<RecognitionRecord> = withContext(Dispatchers.IO) {
        records.toList()
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        records.clear()
        saveToFile()
    }
}
