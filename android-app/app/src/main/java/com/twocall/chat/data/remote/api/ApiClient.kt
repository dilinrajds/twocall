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

class ApiClient(private val keyStoreManager: KeyStoreManager) {

    // Production / Zero-configuration host injected from BuildConfig
    var baseUrl: String = com.twocall.chat.BuildConfig.BASE_URL

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = keyStoreManager.getAccessToken()

        val requestBuilder = original.newBuilder()
        if (token != null && !original.url.encodedPath.contains("/api/v1/pair/") && !original.url.encodedPath.contains("/api/v1/auth/refresh")) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val tokenAuthenticator = Authenticator { _, response ->
        if (responseCount(response) >= 3) {
            return@Authenticator null // Prevent infinite loop
        }

        val refreshToken = keyStoreManager.getRefreshToken() ?: return@Authenticator null

        // Synchronously call refresh endpoint
        val refreshClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val refreshRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val refreshService = refreshRetrofit.create(ChatApiService::class.java)

        try {
            val refreshResponse = kotlinx.coroutines.runBlocking {
                refreshService.refreshToken(RefreshTokenRequestDto(refreshToken))
            }

            if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                val newTokens = refreshResponse.body()!!
                keyStoreManager.updateTokens(newTokens.accessToken, newTokens.refreshToken)

                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            }
        } catch (e: Exception) {
            // Refresh failed
        }

        null
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
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
