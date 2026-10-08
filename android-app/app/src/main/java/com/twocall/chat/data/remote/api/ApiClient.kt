package com.twocall.chat.data.remote.api

import com.twocall.chat.crypto.KeyStoreManager
import com.twocall.chat.data.remote.dto.RefreshTokenRequestDto
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ApiClient(private val keyStoreManager: KeyStoreManager) {

    // Production / Zero-configuration host injected from BuildConfig
    var baseUrl: String = com.twocall.chat.BuildConfig.BASE_URL
    private val refreshMutex = Mutex()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val pairId = original.header("X-Pair-Id") ?: keyStoreManager.getActivePairId()
        val token = pairId?.let { keyStoreManager.getAccessToken(it) }

        val requestBuilder = original.newBuilder().removeHeader("X-Pair-Id").tag(String::class.java, pairId)
        if (token != null && !original.url.encodedPath.contains("/api/v1/pair/") && !original.url.encodedPath.contains("/api/v1/auth/refresh")) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val tokenAuthenticator = Authenticator { _, response ->
        if (responseCount(response) >= 3) {
            return@Authenticator null // Prevent infinite loop
        }

        val pairId = response.request.tag(String::class.java) ?: return@Authenticator null
        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (kotlinx.coroutines.runBlocking { refreshPair(pairId, failedToken) }) {
            val token = keyStoreManager.getAccessToken(pairId) ?: return@Authenticator null
            return@Authenticator response.request.newBuilder().header("Authorization", "Bearer $token").build()
        }
        null
    }

    suspend fun forceRefreshTokens(): Boolean {
        val pairId = keyStoreManager.getActivePairId() ?: return false
        val token = keyStoreManager.getAccessToken(pairId)
        return refreshPair(pairId, token)
    }

    private suspend fun refreshPair(pairId: String, failedToken: String?): Boolean = refreshMutex.withLock {
        val current = keyStoreManager.getAccessToken(pairId)
        if (current != null && current != failedToken) return@withLock true
        val refreshToken = keyStoreManager.getRefreshToken(pairId) ?: return@withLock false
        val refreshClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val refreshRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val refreshService = refreshRetrofit.create(ChatApiService::class.java)

        try {
            val refreshResponse = refreshService.refreshToken(RefreshTokenRequestDto(refreshToken))
            if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                val newTokens = refreshResponse.body()!!
                keyStoreManager.updateTokens(pairId, newTokens.accessToken, newTokens.refreshToken)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val apiService: ChatApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApiService::class.java)
    }
}
