package com.example.traductor.data.translation

import android.content.Context
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.common.model.DownloadConditions
import kotlinx.coroutines.tasks.await

class TranslationDataSource(private val context: Context) {
    private val activeTranslators = mutableMapOf<String, Translator>()

    private fun getTranslator(sourceLang: String, targetLang: String): Translator {
        val key = "$sourceLang->$targetLang"
        return activeTranslators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            Translation.getClient(options)
        }
    }

    suspend fun downloadModelIfNeeded(sourceLang: String, targetLang: String): Result<Unit> = runCatching {
        val conditions = DownloadConditions.Builder().build()
        val translatorSource = getTranslator(sourceLang, targetLang)
        translatorSource.downloadModelIfNeeded(conditions).await()
    }

    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> = runCatching {
        if (text.isBlank()) return@runCatching ""
        downloadModelIfNeeded(sourceLang, targetLang).getOrThrow()
        val translator = getTranslator(sourceLang, targetLang)
        translator.translate(text).await()
    }
}
