package com.example.traductor.domain.model

data class TranslationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: Language,
    val targetLanguage: Language,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
