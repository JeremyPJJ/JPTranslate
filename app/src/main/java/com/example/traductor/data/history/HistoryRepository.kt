package com.example.traductor.data.history

import android.content.Context
import android.content.SharedPreferences
import com.example.traductor.domain.model.TranslationItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HistoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jp_translator_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val historyKey = "translation_history"

    private val _history = MutableStateFlow<List<TranslationItem>>(loadHistory())
    val history: StateFlow<List<TranslationItem>> = _history.asStateFlow()

    private fun loadHistory(): List<TranslationItem> {
        val json = prefs.getString(historyKey, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TranslationItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(items: List<TranslationItem>) {
        val json = gson.toJson(items)
        prefs.edit().putString(historyKey, json).apply()
        _history.value = items
    }

    fun addTranslation(item: TranslationItem) {
        val current = _history.value.toMutableList()
        current.removeAll { it.sourceText == item.sourceText && it.sourceLanguage == item.sourceLanguage && it.targetLanguage == item.targetLanguage }
        current.add(0, item)
        if (current.size > 100) {
            current.removeAt(current.size - 1)
        }
        saveHistory(current)
    }

    fun toggleFavorite(id: String) {
        val current = _history.value.map {
            if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
        }
        saveHistory(current)
    }

    fun deleteItem(id: String) {
        val current = _history.value.filter { it.id != id }
        saveHistory(current)
    }

    fun clearHistory() {
        saveHistory(emptyList())
    }
}
