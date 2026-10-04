package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class TtsVoice(
    val id: String,
    val name: String,
    val banglaTitle: String,
    val description: String,
    val languageCode: String,
    val styleBadge: String,
    val isCustomFromApi: Boolean = false
)

@Immutable
data class TtsModelOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String
)

@Immutable
data class TtsLanguageOption(
    val code: String,
    val labelBangla: String,
    val labelEnglish: String,
    val flagEmoji: String
)

enum class TtsEngineMode(val titleBangla: String, val titleEnglish: String) {
    AUTO_SMART("অটো AI (Gemini / Cartesia)", "Auto Smart AI"),
    GEMINI_CLOUD("Gemini 2.5 Flash TTS", "Gemini 2.5 TTS"),
    CARTESIA_CLOUD("Cartesia Sonic 3.6", "Cartesia Sonic AI"),
    DEVICE_NATIVE("অন-ডিভাইস ভয়েস (Device TTS)", "On-Device TTS")
}

@Immutable
data class PresetScript(
    val id: String,
    val titleBangla: String,
    val category: String,
    val languageCode: String,
    val transcript: String
)

object TtsDefaults {
    val defaultVoices = listOf(
        TtsVoice(
            id = "Kore",
            name = "Kore (Gemini AI)",
            banglaTitle = "কোর • Gemini ন্যাচারাল ভয়েস (বাংলা ও ইংরেজি)",
            description = "Warm, crystal-clear multilingual Gemini 2.5 Flash TTS voice for natural Bangla and English speech.",
            languageCode = "multilingual",
            styleBadge = "Gemini • Bangla"
        ),
        TtsVoice(
            id = "Puck",
            name = "Puck (Gemini AI)",
            banglaTitle = "পাক • Gemini প্রাণবন্ত পুরুষ কণ্ঠ",
            description = "Upbeat, energetic Gemini 2.5 Flash TTS voice ideal for storytelling, reels, and expressive narration.",
            languageCode = "multilingual",
            styleBadge = "Gemini • Lively"
        ),
        TtsVoice(
            id = "Charon",
            name = "Charon (Gemini AI)",
            banglaTitle = "শ্যারন • Gemini গম্ভীর ন্যারেটর",
            description = "Deep, authoritative Gemini studio voice tailored for documentaries, news, and audiobooks.",
            languageCode = "multilingual",
            styleBadge = "Gemini • Deep"
        ),
        TtsVoice(
            id = "Aoede",
            name = "Aoede (Gemini AI)",
            banglaTitle = "আয়েদে • Gemini সুরেলা নারী কণ্ঠ",
            description = "Melodic, expressive female Gemini voice suited for poetry, literature, and presentations.",
            languageCode = "multilingual",
            styleBadge = "Gemini • Expressive"
        ),
        TtsVoice(
            id = "694f9389-aac1-45b6-b726-9d9369183238",
            name = "Friendly Narrator",
            banglaTitle = "ফ্রেন্ডলি ন্যারেটর (Cartesia Sonic)",
            description = "Expressive Cartesia Sonic 3.6 conversational tone for Bangla & English narration.",
            languageCode = "multilingual",
            styleBadge = "Cartesia • Bangla"
        ),
        TtsVoice(
            id = "a0e99841-438c-4a64-b679-ae501e7d6091",
            name = "Barbershop Man",
            banglaTitle = "বার্বারশপ ম্যান (গভীর পুরুষ কণ্ঠ)",
            description = "Warm, resonant male studio voice ideal for narration, podcasts, and announcements.",
            languageCode = "multilingual",
            styleBadge = "Studio Male"
        ),
        TtsVoice(
            id = "79a125e8-cd45-4c13-8a67-188112f4dd22",
            name = "British Lady",
            banglaTitle = "ব্রিটিশ লেডি (মার্জিত নারী কণ্ঠ)",
            description = "Crisp, articulate female voice suited for audiobooks, news reading, and tutorials.",
            languageCode = "multilingual",
            styleBadge = "Articulate Female"
        ),
        TtsVoice(
            id = "d46abd1d-2d02-43e8-819f-51fb652c1c61",
            name = "Newsman",
            banglaTitle = "নিউজম্যান (সংবাদ উপস্থাপক)",
            description = "Authoritative broadcast cadence designed for breaking news and documentary scripts.",
            languageCode = "multilingual",
            styleBadge = "Broadcast"
        )
    )

    val models = listOf(
        TtsModelOption(
            id = "gemini-2.5-flash-preview-tts",
            title = "Gemini 2.5 Flash TTS",
            subtitle = "Google Gemini native audio synthesis for Bangla & multilingual speech",
            badge = "Gemini AI • Bangla"
        ),
        TtsModelOption(
            id = "sonic-3.6",
            title = "Cartesia Sonic 3.6",
            subtitle = "Cartesia flagship model with native Bangla (বাংলা) & 44-language support",
            badge = "Cartesia • Bangla"
        ),
        TtsModelOption(
            id = "sonic-3",
            title = "Cartesia Sonic 3.0",
            subtitle = "High-emotion expressive synthesis with native multilingual support",
            badge = "Expressive"
        ),
        TtsModelOption(
            id = "sonic-turbo",
            title = "Cartesia Sonic Turbo",
            subtitle = "Ultra-low latency voice generation for instant responses",
            badge = "Ultra Fast"
        )
    )

    val languages = listOf(
        TtsLanguageOption("bn", "বাংলা (Bangla)", "Bangla", "🇧🇩"),
        TtsLanguageOption("auto", "অটো ডিটেক্ট (Auto)", "Auto-Detect", "🌐"),
        TtsLanguageOption("en", "ইংরেজি (English)", "English", "🇺🇸"),
        TtsLanguageOption("hi", "হিন্দি (Hindi)", "Hindi", "🇮🇳"),
        TtsLanguageOption("ar", "আরবি (Arabic)", "Arabic", "🇸🇦"),
        TtsLanguageOption("es", "স্প্যানিশ (Spanish)", "Spanish", "🇪🇸"),
        TtsLanguageOption("fr", "ফরাসি (French)", "French", "🇫🇷"),
        TtsLanguageOption("ja", "জাপানি (Japanese)", "Japanese", "🇯🇵")
    )

    val presetScripts = listOf(
        PresetScript(
            id = "bn_welcome",
            titleBangla = "স্বাগতম বার্তা (বাংলা)",
            category = "বাংলা • Greeting",
            languageCode = "bn",
            transcript = "নমস্কার এবং স্বাগতম! সনিক ভয়েস টেক্সট টু স্পিচ স্টুডিওতে আপনাকে স্বাগত জানাই। এখানে আপনি যেকোনো লেখা মুহূর্তের মধ্যেই স্পষ্ট ও প্রাণবন্ত কণ্ঠে রূপান্তর করতে পারবেন।"
        ),
        PresetScript(
            id = "bn_news",
            titleBangla = "সংবাদ পাঠ (News)",
            category = "বাংলা • Broadcast",
            languageCode = "bn",
            transcript = "সন্ধ্যা সাতটার বিশেষ সংবাদে স্বাগতম। আজ প্রযুক্তি বিশ্বে কৃত্রিম বুদ্ধিমত্তা এক নতুন দিগন্তের সূচনা করেছে। এখন মানুষ নিজের ভাষায় আরও নিখুঁত ও স্বাভাবিক কণ্ঠে অডিও তৈরি করতে পারছে।"
        ),
        PresetScript(
            id = "bn_story",
            titleBangla = "গল্পের আসর (Story)",
            category = "বাংলা • Storytelling",
            languageCode = "bn",
            transcript = "অনেক দিন আগের কথা। নির্জন পাহাড়ের পাদদেশে এক ছোট্ট গ্রামে বাস করত এক তরুণ অভিযাত্রী। প্রতিদিন ভোরে সে নতুন কোনো রহস্যের খোঁজে বেরিয়ে পড়ত।"
        ),
        PresetScript(
            id = "en_studio",
            titleBangla = "AI Studio Intro (EN)",
            category = "English • Podcast",
            languageCode = "en",
            transcript = "Welcome to SonicVoice Studio, powered by Gemini 2.5 Flash TTS and Cartesia Sonic AI. Experience ultra-fast, deeply expressive text-to-speech synthesis with studio-grade audio clarity."
        ),
        PresetScript(
            id = "en_story",
            titleBangla = "Sci-Fi Narration (EN)",
            category = "English • Audiobook",
            languageCode = "en",
            transcript = "Beyond the horizon of the neon skyline, a quiet signal echoed through the starlight. Every word carried the warmth of human emotion across lightyears of silence."
        )
    )
}
