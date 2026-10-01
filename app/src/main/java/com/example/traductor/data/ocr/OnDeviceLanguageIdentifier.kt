package com.example.traductor.data.ocr

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import kotlinx.coroutines.tasks.await

class OnDeviceLanguageIdentifier {
    private val identifier: LanguageIdentifier = LanguageIdentification.getClient()

    suspend fun detectLanguageCode(text: String): String {
        val cleanText = text.lowercase()
        val englishWords = setOf("the", "is", "are", "you", "and", "to", "for", "with", "have", "has", "loan", "house", "what", "where", "how", "home", "buyer", "rate")
        val spanishWords = setOf("el", "la", "los", "las", "es", "son", "un", "una", "de", "que", "como", "para", "por", "con", "casa", "prestamo", "casa", "hipoteca")

        val words = cleanText.split("\\s+".toRegex())
        var enCount = 0
        var esCount = 0
        for (word in words) {
            val w = word.trim(',', '.', '?', '!', '"', '\'', ':', ';')
            if (englishWords.contains(w)) enCount++
            if (spanishWords.contains(w)) esCount++
        }

        if (enCount > esCount) return "en"
        if (esCount > enCount) return "es"

        return try {
            val code = identifier.identifyLanguage(text).await()
            if (code == "es") "es" else "en"
        } catch (_: Exception) {
            "en"
        }
    }
}
