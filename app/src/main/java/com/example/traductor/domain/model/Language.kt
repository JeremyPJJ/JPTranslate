package com.example.traductor.domain.model

enum class Language(
    val code: String,
    val displayName: String,
    val flag: String,
    val mlKitCode: String
) {
    SPANISH("es", "Español", "🇪🇸", com.google.mlkit.nl.translate.TranslateLanguage.SPANISH),
    ENGLISH("en", "English", "🇺🇸", com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH)
}
