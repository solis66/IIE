package com.example.lexiscan.data.Service

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

/**
 * 百度图片识别API服务接口
 */
interface BaiduApiService {

    /**
     * 获取访问令牌
     * API文档：https://ai.baidu.com/ai-doc/REFERENCE/Ck3dwjhhu
     */
    @FormUrlEncoded
    @POST("oauth/2.0/token")
    suspend fun getAccessToken(
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): Response<AccessTokenResponse>
}

/**
 * 访问令牌响应
 */
data class AccessTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("error_description")
    val errorDescription: String? = null
)
