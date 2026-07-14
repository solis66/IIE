package com.example.lexiscan.data.model

import com.google.gson.annotations.SerializedName

/**
 * 百度API错误类型
 */
sealed class BaiduApiError : Exception() {
    data class NetworkError(override val cause: Throwable) : BaiduApiError() {
        override val message: String
            get() = "网络错误: ${cause.message ?: cause.javaClass.simpleName}"
    }
    data class ApiError(val errorCode: Int, val errorMsg: String) : BaiduApiError() {
        override val message: String
            get() = "API错误 [code=$errorCode]: $errorMsg"
    }
    data class InvalidResponse(override val message: String) : BaiduApiError()
    data class ImageProcessingError(override val message: String) : BaiduApiError()
}