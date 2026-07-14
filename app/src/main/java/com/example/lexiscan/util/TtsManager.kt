package com.example.lexiscan.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

/**
 * 封装在线词典音频播报功能（替代不稳定的系统原生 TTS）
 * 使用有道词典公开 API: https://dict.youdao.com/dictvoice?audio={word}&type=2
 */
class TtsManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * 朗读指定的英文单词
     */
    fun speak(text: String) {
        if (text.isBlank()) return
        Log.d("TtsManager", "Speaking via online API: $text")

        try {
            // 每次发音前先释放之前的 MediaPlayer 实例
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()

            // 设置音频属性为媒体音量，使用户能用音量键控制
            mediaPlayer?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )

            // 构造有道词典的在线发音 URL (type=2 表示美式发音，type=1 表示英式发音)
            val url = "https://dict.youdao.com/dictvoice?audio=${android.net.Uri.encode(text)}&type=2"
            
            mediaPlayer?.setDataSource(url)
            mediaPlayer?.setOnPreparedListener { mp ->
                // 准备好后直接播放
                mp.start()
            }
            mediaPlayer?.setOnErrorListener { _, what, extra ->
                Log.e("TtsManager", "MediaPlayer error: what=$what, extra=$extra")
                true
            }
            
            // 异步准备在线音频，避免阻塞主线程
            mediaPlayer?.prepareAsync()

        } catch (e: Exception) {
            Log.e("TtsManager", "Failed to play audio: ${e.message}", e)
        }
    }

    /**
     * 释放资源
     */
    fun shutdown() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("TtsManager", "Error during shutdown: ${e.message}")
        }
    }
}
