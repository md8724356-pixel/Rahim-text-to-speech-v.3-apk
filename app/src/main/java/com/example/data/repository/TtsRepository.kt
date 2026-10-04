package com.example.data.repository

import android.content.Context
import com.example.audio.AudioPlayerController
import com.example.data.local.TtsDao
import com.example.data.local.TtsHistoryEntity
import com.example.data.model.TtsDefaults
import com.example.data.model.TtsEngineMode
import com.example.data.model.TtsVoice
import com.example.data.remote.CartesiaApiService
import com.example.data.remote.CartesiaNetworkModule
import com.example.data.remote.CartesiaOutputFormat
import com.example.data.remote.CartesiaRemoteVoiceDto
import com.example.data.remote.CartesiaTtsRequest
import com.example.data.remote.CartesiaVoiceSpec
import com.example.data.remote.CartesiaVoicesPageDto
import com.example.data.remote.GeminiNetworkClient
import com.squareup.moshi.Types
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class SynthesisResult(
    val historyItem: TtsHistoryEntity,
    val statusNotice: String? = null
)

class TtsRepository(
    private val context: Context,
    private val ttsDao: TtsDao,
    private val apiService: CartesiaApiService = CartesiaNetworkModule.apiService
) {
    val allHistory: Flow<List<TtsHistoryEntity>> = ttsDao.getAllHistory()

    private val audioDirectory: File by lazy {
        File(context.filesDir, "tts_audio").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun fetchCartesiaVoices(): Result<List<TtsVoice>> = withContext(Dispatchers.IO) {
        if (!CartesiaNetworkModule.isCartesiaKeyConfigured()) {
            return@withContext Result.success(TtsDefaults.defaultVoices)
        }

        try {
            val response = apiService.listVoices()
            if (response.isSuccessful) {
                val rawJson = response.body()?.string().orEmpty()
                val remoteList = parseVoicesListOrPage(rawJson)
                if (remoteList.isNotEmpty()) {
                    val mapped = remoteList.mapNotNull { dto ->
                        val id = dto.id?.trim()
                        val name = dto.name?.trim()
                        if (id.isNullOrEmpty() || name.isNullOrEmpty()) return@mapNotNull null
                        TtsVoice(
                            id = id,
                            name = name,
                            banglaTitle = "$name • ক্লাউড ভয়েস",
                            description = dto.description?.takeIf { it.isNotBlank() }
                                ?: "Cartesia Sonic multilingual neural voice.",
                            languageCode = dto.language ?: "multilingual",
                            styleBadge = if (dto.isPublic == true) "Cloud Public" else "Custom Voice",
                            isCustomFromApi = true
                        )
                    }.take(24)
                    val combined = (TtsDefaults.defaultVoices + mapped).distinctBy { it.id }
                    return@withContext Result.success(combined)
                }
            }
            Result.success(TtsDefaults.defaultVoices)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseVoicesListOrPage(rawJson: String): List<CartesiaRemoteVoiceDto> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            if (trimmed.startsWith("[")) {
                val listType = Types.newParameterizedType(List::class.java, CartesiaRemoteVoiceDto::class.java)
                CartesiaNetworkModule.moshi.adapter<List<CartesiaRemoteVoiceDto>>(listType).fromJson(trimmed).orEmpty()
            } else {
                CartesiaNetworkModule.moshi.adapter(CartesiaVoicesPageDto::class.java).fromJson(trimmed)?.data.orEmpty()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun synthesizeSpeech(
        transcript: String,
        voice: TtsVoice,
        modelId: String,
        languageCode: String,
        speed: Float,
        engineMode: TtsEngineMode,
        audioPlayerController: AudioPlayerController
    ): Result<SynthesisResult> = withContext(Dispatchers.IO) {
        val cleanText = transcript.trim()
        if (cleanText.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("অনুগ্রহ করে প্রথমে কিছু টেক্সট লিখুন।"))
        }

        val fileName = "sonic_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.wav"
        val targetFile = File(audioDirectory, fileName)

        var usedEngine = "Gemini 2.5 Flash TTS"
        var synthesisSucceeded = false
        var lastCloudError: String? = null
        var statusNotice: String? = null

        val hasGeminiKey = CartesiaNetworkModule.isGeminiKeyConfigured()
        val hasCartesiaKey = CartesiaNetworkModule.isCartesiaKeyConfigured()
        val preferGeminiFirst = engineMode == TtsEngineMode.GEMINI_CLOUD ||
            (engineMode == TtsEngineMode.AUTO_SMART &&
                (modelId.startsWith("gemini") || voice.styleBadge.contains("Gemini") || (hasGeminiKey && !hasCartesiaKey)))

        when (engineMode) {
            TtsEngineMode.GEMINI_CLOUD -> {
                if (!hasGeminiKey) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            "Gemini API Key সেট করা নেই। উপরের 'Set API Key' বাটনে ট্যাপ করে আপনার Gemini API Key সেভ করুন অথবা AI Studio-এর Secrets প্যানেলে GEMINI_API_KEY যুক্ত করুন।"
                        )
                    )
                }
                val geminiRes = GeminiNetworkClient.synthesizeSpeechToFile(
                    apiKey = CartesiaNetworkModule.resolveActiveGeminiApiKey(),
                    transcript = cleanText,
                    voiceIdOrName = voice.id,
                    outputFile = targetFile
                )
                geminiRes.onSuccess { label ->
                    synthesisSucceeded = true
                    usedEngine = label
                    statusNotice = "$label ভয়েস তৈরি হয়েছে!"
                }.onFailure { err ->
                    lastCloudError = err.message
                }
                if (!synthesisSucceeded) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            "Gemini TTS সংযোগ সফল হয়নি: ${lastCloudError ?: "API Key যাচাই করুন"}"
                        )
                    )
                }
            }

            TtsEngineMode.CARTESIA_CLOUD -> {
                if (!hasCartesiaKey) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            "Cartesia API Key সেট করা নেই। উপরের 'Set API Key' বাটনে ট্যাপ করে আপনার sk_car_... কী সেভ করুন।"
                        )
                    )
                }
                val cloudResult = tryCartesiaSynthesisVerified(
                    transcript = cleanText,
                    voiceId = mapVoiceToCartesiaUuid(voice.id),
                    preferredModelId = if (modelId.startsWith("sonic")) modelId else "sonic-3.6",
                    languageCode = languageCode,
                    outputFile = targetFile
                )
                cloudResult.onSuccess { resolvedModel ->
                    synthesisSucceeded = true
                    usedEngine = "Cartesia ($resolvedModel)"
                    statusNotice = "Cartesia ($resolvedModel) ভয়েস তৈরি হয়েছে!"
                }.onFailure { err ->
                    lastCloudError = err.message
                }
                if (!synthesisSucceeded) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            "Cartesia ক্লাউড ভয়েস তৈরি করা যায়নি: ${lastCloudError ?: "API Key বা ইন্টারনেট সংযোগ যাচাই করুন।"}"
                        )
                    )
                }
            }

            TtsEngineMode.AUTO_SMART -> {
                if (preferGeminiFirst && hasGeminiKey) {
                    val geminiRes = GeminiNetworkClient.synthesizeSpeechToFile(
                        apiKey = CartesiaNetworkModule.resolveActiveGeminiApiKey(),
                        transcript = cleanText,
                        voiceIdOrName = voice.id,
                        outputFile = targetFile
                    )
                    geminiRes.onSuccess { label ->
                        synthesisSucceeded = true
                        usedEngine = label
                        statusNotice = "$label ভয়েস তৈরি হয়েছে!"
                    }.onFailure { err ->
                        lastCloudError = err.message
                    }
                }

                if (!synthesisSucceeded && hasCartesiaKey) {
                    val cartesiaRes = tryCartesiaSynthesisVerified(
                        transcript = cleanText,
                        voiceId = mapVoiceToCartesiaUuid(voice.id),
                        preferredModelId = if (modelId.startsWith("sonic")) modelId else "sonic-3.6",
                        languageCode = languageCode,
                        outputFile = targetFile
                    )
                    cartesiaRes.onSuccess { resolvedModel ->
                        synthesisSucceeded = true
                        usedEngine = "Cartesia ($resolvedModel)"
                        statusNotice = "Cartesia ($resolvedModel) ভয়েস তৈরি হয়েছে!"
                    }.onFailure { err ->
                        lastCloudError = err.message
                    }
                }

                if (!synthesisSucceeded && !preferGeminiFirst && hasGeminiKey) {
                    val geminiRes = GeminiNetworkClient.synthesizeSpeechToFile(
                        apiKey = CartesiaNetworkModule.resolveActiveGeminiApiKey(),
                        transcript = cleanText,
                        voiceIdOrName = voice.id,
                        outputFile = targetFile
                    )
                    geminiRes.onSuccess { label ->
                        synthesisSucceeded = true
                        usedEngine = label
                        statusNotice = "$label ভয়েস তৈরি হয়েছে!"
                    }.onFailure { err ->
                        lastCloudError = err.message
                    }
                }

                if (!synthesisSucceeded) {
                    val fallbackOk = audioPlayerController.synthesizeWithDeviceTtsToFile(
                        text = cleanText,
                        languageCode = languageCode,
                        speed = speed,
                        outputFile = targetFile
                    )
                    if (fallbackOk && targetFile.exists() && targetFile.length() > 44L) {
                        synthesisSucceeded = true
                        usedEngine = "Device TTS Engine"
                        statusNotice = if (lastCloudError != null) {
                            "ক্লাউড সংযোগ ত্রুটি ($lastCloudError) — ডিভাইস ভয়েসে প্লে হচ্ছে।"
                        } else {
                            "ডিভাইস TTS ভয়েস তৈরি হয়েছে।"
                        }
                    }
                }
            }

            TtsEngineMode.DEVICE_NATIVE -> {
                val fallbackOk = audioPlayerController.synthesizeWithDeviceTtsToFile(
                    text = cleanText,
                    languageCode = languageCode,
                    speed = speed,
                    outputFile = targetFile
                )
                if (fallbackOk && targetFile.exists() && targetFile.length() > 44L) {
                    synthesisSucceeded = true
                    usedEngine = "Device TTS Engine"
                    statusNotice = "ডিভাইস TTS ভয়েস তৈরি হয়েছে।"
                }
            }
        }

        if (!synthesisSucceeded || !targetFile.exists() || targetFile.length() <= 44L) {
            return@withContext Result.failure(
                IllegalStateException(
                    lastCloudError ?: "অডিও সিন্থেসিস সম্পন্ন করা যায়নি। অনুগ্রহ করে আবার চেষ্টা করুন।"
                )
            )
        }

        val durationMs = AudioPlayerController.resolveAudioDurationMs(context, targetFile)
        val entity = TtsHistoryEntity(
            transcript = cleanText,
            voiceId = voice.id,
            voiceName = voice.name,
            modelId = modelId,
            languageCode = languageCode,
            engineUsed = usedEngine,
            playbackSpeed = speed,
            audioFilePath = targetFile.absolutePath,
            durationMs = durationMs,
            fileSizeBytes = targetFile.length(),
            createdAt = System.currentTimeMillis()
        )
        val insertedId = ttsDao.insertHistory(entity)
        val savedEntity = entity.copy(id = insertedId)

        Result.success(
            SynthesisResult(
                historyItem = savedEntity,
                statusNotice = statusNotice
            )
        )
    }

    private fun mapVoiceToCartesiaUuid(voiceId: String): String {
        if (voiceId.length >= 32 && voiceId.contains("-")) return voiceId
        return when (voiceId) {
            "Kore" -> "694f9389-aac1-45b6-b726-9d9369183238"
            "Puck" -> "b7d50908-b17c-442d-ad8d-810c63997ed9"
            "Charon" -> "a0e99841-438c-4a64-b679-ae501e7d6091"
            "Aoede" -> "79a125e8-cd45-4c13-8a67-188112f4dd22"
            else -> "694f9389-aac1-45b6-b726-9d9369183238"
        }
    }

    private suspend fun tryCartesiaSynthesisVerified(
        transcript: String,
        voiceId: String,
        preferredModelId: String,
        languageCode: String,
        outputFile: File
    ): Result<String> {
        val isBanglaText = AudioPlayerController.containsBanglaCharacters(transcript)
        val resolvedLang: String? = when {
            languageCode == "auto" && isBanglaText -> "bn"
            languageCode == "auto" -> null
            else -> languageCode
        }

        val modelsToTry = if (resolvedLang == "bn" || isBanglaText) {
            listOf("sonic-3.6", "sonic-3", preferredModelId).distinct()
        } else {
            listOf(preferredModelId, "sonic-3.6", "sonic-3").distinct()
        }

        var lastErrorMsg = "HTTP error"

        for (model in modelsToTry) {
            try {
                val langForModel = if (model.startsWith("sonic-2") && resolvedLang == "bn") {
                    null
                } else {
                    resolvedLang
                }

                val request = CartesiaTtsRequest(
                    modelId = model,
                    transcript = transcript,
                    voice = CartesiaVoiceSpec(mode = "id", id = voiceId),
                    outputFormat = CartesiaOutputFormat(
                        container = "raw",
                        encoding = "pcm_s16le",
                        sampleRate = 24000
                    ),
                    language = langForModel
                )
                val response = apiService.synthesizeSpeech(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val writtenSize = body.byteStream().use { inputStream ->
                            AudioPlayerController.streamRawPcmToStandardWavFile(
                                inputStream = inputStream,
                                sampleRate = 24000,
                                outputFile = outputFile
                            )
                        }
                        if (writtenSize > 100L && outputFile.exists()) {
                            return Result.success(model)
                        }
                    }
                } else {
                    val errBody = response.errorBody()?.string()?.take(180).orEmpty()
                    lastErrorMsg = "HTTP ${response.code()} ($model): $errBody"
                }
            } catch (e: Exception) {
                lastErrorMsg = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "Network error"}"
            }
        }
        return Result.failure(IllegalStateException(lastErrorMsg))
    }

    suspend fun toggleFavorite(item: TtsHistoryEntity) = withContext(Dispatchers.IO) {
        ttsDao.updateFavorite(item.id, !item.isFavorite)
    }

    suspend fun deleteHistoryItem(item: TtsHistoryEntity) = withContext(Dispatchers.IO) {
        try {
            File(item.audioFilePath).delete()
        } catch (_: Exception) {
        }
        ttsDao.deleteById(item.id)
    }

    suspend fun clearAllHistory(items: List<TtsHistoryEntity>) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            try {
                File(item.audioFilePath).delete()
            } catch (_: Exception) {
            }
        }
        ttsDao.clearAll()
    }
}
