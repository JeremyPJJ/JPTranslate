package com.example.traductor.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechHelper(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingUtterances = mutableListOf<Pair<String, String>>()
    var onError: ((String?) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                onTtsSuccess()
            } else {
                Log.w("TTS", "Default TTS init failed ($status). Trying explicit system engines...")
                tryExplicitEngine()
            }
        }
    }

    private fun tryExplicitEngine() {
        try {
            val tempTts = TextToSpeech(context.applicationContext, null)
            val engines = tempTts.engines
            tempTts.shutdown()

            if (!engines.isNullOrEmpty()) {
                val engineName = engines[0].name
                Log.d("TTS", "Attempting explicit engine binding: $engineName")
                tts = TextToSpeech(context.applicationContext, { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        onTtsSuccess()
                    } else {
                        onTtsFailure(status)
                    }
                }, engineName)
            } else {
                onTtsFailure(-1)
            }
        } catch (e: Exception) {
            onTtsFailure(-1)
        }
    }

    private fun onTtsSuccess() {
        tts?.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        isInitialized = true
        synchronized(pendingUtterances) {
            for ((text, langCode) in pendingUtterances) {
                speak(text, langCode)
            }
            pendingUtterances.clear()
        }
    }

    private fun onTtsFailure(status: Int) {
        Log.e("TTS", "Motor TTS no disponible ($status)")
        onError?.invoke("No hay ningún motor de voz (TTS) activo en el sistema Android.")
    }

    fun openPlayStoreForTts() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openPlayStoreForTts()
        }
    }

    fun isSpeaking(): Boolean {
        return try {
            tts?.isSpeaking == true
        } catch (_: Exception) {
            false
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TTS", "Error stopping TTS", e)
        }
    }

    fun speakOrStop(text: String, languageCode: String): Boolean {
        if (isSpeaking()) {
            stop()
            return false
        } else {
            speak(text, languageCode)
            return true
        }
    }

    fun speak(text: String, languageCode: String) {
        if (text.isBlank()) {
            onError?.invoke("No hay texto para reproducir.")
            return
        }
        if (!isInitialized || tts == null) {
            onError?.invoke("El motor de voz (TTS) se está inicializando. Inténtalo de nuevo en un segundo.")
            initTts()
            return
        }

        val primaryLocale = if (languageCode == "es") Locale("es", "ES") else Locale("en", "US")
        val fallbackLocale = Locale(languageCode)

        var setResult = tts?.setLanguage(primaryLocale)
        if (setResult == TextToSpeech.LANG_MISSING_DATA || setResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("TTS", "Primary locale $primaryLocale missing data, trying fallback $fallbackLocale")
            setResult = tts?.setLanguage(fallbackLocale)
        }
        if (setResult == TextToSpeech.LANG_MISSING_DATA || setResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("TTS", "Fallback locale $fallbackLocale missing data, using system default locale")
            tts?.setLanguage(Locale.getDefault())
        }

        val speakResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        Log.d("TTS", "Speak result code for '$text': $speakResult")
        if (speakResult == TextToSpeech.ERROR) {
            onError?.invoke("Error al reproducir audio. Comprueba el volumen multimedia.")
        } else {
            onError?.invoke(null)
        }
    }

    fun checkCapabilities(): Map<String, Boolean> {
        if (!isInitialized || tts == null) {
            return mapOf(
                "tts_available" to false,
                "spanish_available" to false,
                "english_available" to false
            )
        }

        val spanishLocale = Locale("es", "ES")
        val englishLocale = Locale.US

        val spanishAvail = try {
            tts?.isLanguageAvailable(spanishLocale)?.let { it >= TextToSpeech.LANG_AVAILABLE } ?: false
        } catch (_: Exception) { false }

        val englishAvail = try {
            tts?.isLanguageAvailable(englishLocale)?.let { it >= TextToSpeech.LANG_AVAILABLE } ?: false
        } catch (_: Exception) { false }

        return mapOf(
            "tts_available" to true,
            "spanish_available" to spanishAvail,
            "english_available" to englishAvail
        )
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
    }
}
