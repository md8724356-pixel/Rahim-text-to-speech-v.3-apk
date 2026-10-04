package com.example.data.remote

import android.util.Base64
import com.example.audio.AudioPlayerController
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @param:Json(name = "contents") val contents: List<GeminiContent>,
    @param:Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @param:Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @param:Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @param:Json(name = "text") val text: String? = null,
    @param:Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @param:Json(name = "mimeType") val mimeType: String? = null,
    @param:Json(name = "data") val data: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @param:Json(name = "temperature") val temperature: Float? = null,
    @param:Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @param:Json(name = "speechConfig") val speechConfig: GeminiSpeechConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSpeechConfig(
    @param:Json(name = "voiceConfig") val voiceConfig: GeminiVoiceConfig
)

@JsonClass(generateAdapter = true)
data class GeminiVoiceConfig(
    @param:Json(name = "prebuiltVoiceConfig") val prebuiltVoiceConfig: GeminiPrebuiltVoiceConfig
)

@JsonClass(generateAdapter = true)
data class GeminiPrebuiltVoiceConfig(
    @param:Json(name = "voiceName") val voiceName: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @param:Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @param:Json(name = "content") val content: GeminiContent? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash-preview-tts:generateContent")
    suspend fun generateSpeechContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): Response<GenerateContentResponse>

    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateTextContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): Response<GenerateContentResponse>
}

object GeminiNetworkClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(CartesiaNetworkModule.moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun mapToGeminiVoiceName(voiceIdOrName: String): String {
        val validGeminiVoices = setOf("Kore", "Puck", "Charon", "Fenrir", "Aoede", "Zephyr")
        if (voiceIdOrName in validGeminiVoices) return voiceIdOrName
        val lower = voiceIdOrName.lowercase()
        return when {
            lower.contains("kore") || lower.contains("friendly") -> "Kore"
            lower.contains("puck") || lower.contains("california") -> "Puck"
            lower.contains("charon") || lower.contains("barbershop") -> "Charon"
            lower.contains("fenrir") || lower.contains("newsman") -> "Fenrir"
            lower.contains("aoede") || lower.contains("british") -> "Aoede"
            lower.contains("zephyr") || lower.contains("wise") -> "Zephyr"
            else -> "Kore"
        }
    }

    suspend fun synthesizeSpeechToFile(
        apiKey: String,
        transcript: String,
        voiceIdOrName: String,
        outputFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API Key পাওয়া যায়নি।")
            )
        }

        val geminiVoiceName = mapToGeminiVoiceName(voiceIdOrName)
        val request = GenerateContentRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = transcript))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseModalities = listOf("AUDIO"),
                speechConfig = GeminiSpeechConfig(
                    voiceConfig = GeminiVoiceConfig(
                        prebuiltVoiceConfig = GeminiPrebuiltVoiceConfig(
                            voiceName = geminiVoiceName
                        )
                    )
                )
            )
        )

        try {
            val response = service.generateSpeechContent(cleanKey, request)
            if (!response.isSuccessful) {
                val errBody = response.errorBody()?.string()?.take(220).orEmpty()
                return@withContext Result.failure(
                    IllegalStateException("Gemini TTS HTTP ${response.code()}: $errBody")
                )
            }

            val inlineData = response.body()
                ?.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull { !it.inlineData?.data.isNullOrBlank() }
                ?.inlineData
                ?: return@withContext Result.failure(
                    IllegalStateException("Gemini TTS থেকে অডিও ডেটা পাওয়া যায়নি।")
                )

            val rawAudioBytes = Base64.decode(inlineData.data.orEmpty(), Base64.DEFAULT)
            if (rawAudioBytes.size <= 64) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini TTS অডিও ডেটা খালি বা অসম্পূর্ণ।")
                )
            }

            val rateMatch = Regex("rate=(\\d+)").find(inlineData.mimeType.orEmpty())
            val sampleRate = rateMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(8000, 48000) ?: 24000

            AudioPlayerController.writeRawPcmOrWavToStandardWavFile(
                inputBytes = rawAudioBytes,
                defaultSampleRate = sampleRate,
                outputFile = outputFile
            )

            if (outputFile.exists() && outputFile.length() > 100L) {
                Result.success("Gemini 2.5 TTS ($geminiVoiceName)")
            } else {
                Result.failure(IllegalStateException("WAV অডিও ফাইল তৈরি করা যায়নি।"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun enhanceOrGenerateScript(
        apiKey: String,
        currentText: String,
        languageCode: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API Key সেট করা নেই।")
            )
        }

        val isBangla = languageCode == "bn" || AudioPlayerController.containsBanglaCharacters(currentText)
        val prompt = if (currentText.isBlank()) {
            if (isBangla) {
                "একটি সুন্দর, স্পষ্ট ও অনুপ্রেরণামূলক ৩ বাক্যের বাংলা ভয়েসওভার স্ক্রিপ্ট লিখে দাও। শুধুমাত্র স্ক্রিপ্টের টেক্সটটুকু লিখবে, কোনো অতিরিক্ত ব্যাখ্যা বা কোটেশন মার্ক দেবে না।"
            } else {
                "Write an expressive, engaging 3-sentence voiceover narration script in English. Output only the spoken script text without quotes or extra commentary."
            }
        } else {
            if (isBangla) {
                "নিচের লেখাটিকে ভয়েসওভার বা অডিওবুকের জন্য আরও সুন্দর, প্রাঞ্জল ও শ্রুতিমধুর বাংলায় সাজিয়ে লিখে দাও (শুধুমাত্র মূল টেক্সটটুকু দেবে, কোনো ভূমিকা বা কোটেশন দেবে না):\n\n$currentText"
            } else {
                "Polish and enhance the following text into a natural, expressive voiceover script (return only the spoken script text without quotes or commentary):\n\n$currentText"
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f
            )
        )

        try {
            val response = service.generateTextContent(cleanKey, request)
            if (!response.isSuccessful) {
                val errBody = response.errorBody()?.string()?.take(200).orEmpty()
                return@withContext Result.failure(
                    IllegalStateException("Gemini HTTP ${response.code()}: $errBody")
                )
            }
            val generatedText = response.body()
                ?.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                .orEmpty()

            if (generatedText.isNotBlank()) {
                Result.success(generatedText)
            } else {
                Result.failure(IllegalStateException("Gemini থেকে কোনো টেক্সট পাওয়া যায়নি।"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
