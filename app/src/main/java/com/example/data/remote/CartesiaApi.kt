package com.example.data.remote

import android.content.Context
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

@JsonClass(generateAdapter = true)
data class CartesiaVoiceSpec(
    @param:Json(name = "mode") val mode: String = "id",
    @param:Json(name = "id") val id: String
)

@JsonClass(generateAdapter = true)
data class CartesiaOutputFormat(
    @param:Json(name = "container") val container: String = "raw",
    @param:Json(name = "encoding") val encoding: String = "pcm_s16le",
    @param:Json(name = "sample_rate") val sampleRate: Int = 24000
)

@JsonClass(generateAdapter = true)
data class CartesiaTtsRequest(
    @param:Json(name = "model_id") val modelId: String,
    @param:Json(name = "transcript") val transcript: String,
    @param:Json(name = "voice") val voice: CartesiaVoiceSpec,
    @param:Json(name = "output_format") val outputFormat: CartesiaOutputFormat = CartesiaOutputFormat(),
    @param:Json(name = "language") val language: String? = null
)

@JsonClass(generateAdapter = true)
data class CartesiaRemoteVoiceDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "language") val language: String? = null,
    @param:Json(name = "is_public") val isPublic: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class CartesiaVoicesPageDto(
    @param:Json(name = "data") val data: List<CartesiaRemoteVoiceDto>? = null
)

interface CartesiaApiService {
    @POST("tts/bytes")
    @Streaming
    suspend fun synthesizeSpeech(
        @Body request: CartesiaTtsRequest
    ): Response<ResponseBody>

    @GET("voices")
    suspend fun listVoices(): Response<ResponseBody>
}

object CartesiaNetworkModule {
    private const val BASE_URL = "https://api.cartesia.ai/"
    private const val CARTESIA_VERSION = "2024-11-13"
    private const val PREFS_NAME = "sonic_voice_prefs"
    private const val KEY_CUSTOM_API_KEY = "custom_cartesia_api_key"
    private const val KEY_CUSTOM_GEMINI_API = "custom_gemini_api_key"

    @Volatile
    private var runtimeOverrideKey: String? = null

    @Volatile
    private var runtimeGeminiKeyOverride: String? = null

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCartesia = prefs.getString(KEY_CUSTOM_API_KEY, null)?.trim()
        if (!savedCartesia.isNullOrEmpty()) {
            runtimeOverrideKey = savedCartesia
        }
        val savedGemini = prefs.getString(KEY_CUSTOM_GEMINI_API, null)?.trim()
        if (!savedGemini.isNullOrEmpty()) {
            runtimeGeminiKeyOverride = savedGemini
        }
    }

    fun saveCustomApiKey(context: Context, newKey: String) {
        val clean = newKey.trim()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (clean.startsWith("sk_car_", ignoreCase = true)) {
            runtimeOverrideKey = clean.ifEmpty { null }
            prefs.edit().putString(KEY_CUSTOM_API_KEY, clean).apply()
        } else {
            runtimeGeminiKeyOverride = clean.ifEmpty { null }
            prefs.edit().putString(KEY_CUSTOM_GEMINI_API, clean).apply()
        }
    }

    fun saveBothApiKeys(context: Context, geminiKey: String, cartesiaKey: String) {
        val cleanGemini = geminiKey.trim()
        val cleanCartesia = cartesiaKey.trim()
        runtimeGeminiKeyOverride = cleanGemini.ifEmpty { null }
        runtimeOverrideKey = cleanCartesia.ifEmpty { null }
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_GEMINI_API, cleanGemini)
            .putString(KEY_CUSTOM_API_KEY, cleanCartesia)
            .apply()
    }

    fun resolveActiveGeminiApiKey(): String {
        val override = runtimeGeminiKeyOverride?.trim().orEmpty()
        if (override.isNotEmpty() &&
            override != "MY_GEMINI_API_KEY" &&
            override != "YOUR_GEMINI_API_KEY" &&
            !override.startsWith("YOUR_")
        ) {
            return override
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
        if (buildKey.isNotEmpty() &&
            buildKey != "MY_GEMINI_API_KEY" &&
            buildKey != "YOUR_GEMINI_API_KEY" &&
            !buildKey.startsWith("YOUR_")
        ) {
            return buildKey
        }
        return ""
    }

    fun resolveActiveApiKey(): String {
        val override = runtimeOverrideKey?.trim().orEmpty()
        if (override.isNotEmpty()) return override
        val buildKey = BuildConfig.CARTESIA_API_KEY.trim()
        if (buildKey.isNotEmpty() &&
            buildKey != "YOUR_CARTESIA_API_KEY" &&
            buildKey != "MY_CARTESIA_API_KEY" &&
            !buildKey.startsWith("YOUR_")
        ) {
            return buildKey
        }
        return ""
    }

    fun isGeminiKeyConfigured(): Boolean = resolveActiveGeminiApiKey().isNotEmpty()

    fun isCartesiaKeyConfigured(): Boolean = resolveActiveApiKey().isNotEmpty()

    fun isApiKeyConfigured(): Boolean {
        return isGeminiKeyConfigured() || isCartesiaKeyConfigured()
    }

    fun getMaskedKeyStatus(): String {
        val geminiKey = resolveActiveGeminiApiKey()
        val cartesiaKey = resolveActiveApiKey()
        return when {
            geminiKey.isNotEmpty() && cartesiaKey.isNotEmpty() -> "Gemini + Cartesia Ready"
            geminiKey.isNotEmpty() -> {
                if (geminiKey.length > 10) "Gemini (${geminiKey.take(6)}••${geminiKey.takeLast(4)})"
                else "Gemini Active"
            }
            cartesiaKey.isNotEmpty() -> {
                if (cartesiaKey.length > 10) "Cartesia (${cartesiaKey.take(7)}••${cartesiaKey.takeLast(4)})"
                else "Cartesia Active"
            }
            else -> "Not Set"
        }
    }

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val authInterceptor = Interceptor { chain ->
        val apiKey = resolveActiveApiKey()
        val requestBuilder = chain.request().newBuilder()
            .header("Cartesia-Version", CARTESIA_VERSION)
            .header("X-API-Key", apiKey)
            .header("Authorization", "Bearer $apiKey")
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val apiService: CartesiaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CartesiaApiService::class.java)
    }
}
