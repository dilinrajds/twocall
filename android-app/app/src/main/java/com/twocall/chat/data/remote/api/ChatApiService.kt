package com.twocall.chat.data.remote.api

import com.twocall.chat.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApiService {

    @POST("api/v1/pair/create")
    suspend fun createPair(@Body request: CreatePairRequestDto): Response<CreatePairResponseDto>

    @POST("api/v1/pair/join")
    suspend fun joinPair(@Body request: JoinPairRequestDto): Response<JoinPairResponseDto>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequestDto): Response<TokenResponseDto>

    @GET("api/v1/auth/pair-info")
    suspend fun getPairInfo(): Response<DevicePairInfoResponseDto>

    @POST("api/v1/auth/disconnect")
    suspend fun disconnect(): Response<Map<String, String>>

    @DELETE("api/v1/auth/pair")
    suspend fun deletePair(): Response<Map<String, String>>

    @POST("api/v1/messages/send")
    suspend fun sendMessage(@Body request: SendMessageRequestDto): Response<MessageResponseDto>

    @GET("api/v1/messages/sync")
    suspend fun syncMessages(@Query("after") after: String?): Response<List<MessageResponseDto>>

    @POST("api/v1/messages/receipt")
    suspend fun updateReceipt(@Body request: ReceiptUpdateRequestDto): Response<Map<String, String>>

    @POST("api/v1/messages/reaction")
    suspend fun sendReaction(@Body request: ReactionRequestDto): Response<Map<String, String>>

    @DELETE("api/v1/messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): Response<Map<String, String>>

    @Multipart
    @POST("api/v1/media/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("encryptedKey") encryptedKey: String?
    ): Response<AttachmentUploadResponseDto>

    @Streaming
    @GET("api/v1/media/{attachmentId}")
    suspend fun downloadMedia(@Path("attachmentId") attachmentId: String): Response<ResponseBody>

    @GET("api/v1/webrtc/turn-credentials")
    suspend fun getTurnCredentials(): Response<TurnCredentialsResponseDto>

    @POST("api/v1/device/push-token")
    suspend fun registerPushToken(@Body request: RegisterPushTokenRequestDto): Response<Map<String, String>>
}
