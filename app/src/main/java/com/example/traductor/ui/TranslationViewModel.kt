package com.example.traductor.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.traductor.data.history.HistoryRepository
import com.example.traductor.data.translation.TranslationDataSource
import com.example.traductor.domain.model.Language
import com.example.traductor.domain.model.TranslationItem
import com.example.traductor.voice.TextToSpeechHelper
import com.example.traductor.voice.VoskSpeechHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceCapabilities(
    val ttsAvailable: Boolean = false,
    val spanishTts: Boolean = false,
    val englishTts: Boolean = false,
    val micPermission: Boolean = false,
    val speechRecognizerAvailable: Boolean = true
)

data class TranslationUiState(
    val sourceText: String = "",
    val translatedText: String = "",
    val sourceLanguage: Language = Language.SPANISH,
    val targetLanguage: Language = Language.ENGLISH,
    val isLoading: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val downloadProgressMessage: String = "",
    val errorMessage: String? = null,
    val isListening: Boolean = false,
    val selectedTab: Int = 0, // 0: Translate, 1: Favorites
    val isModelsDownloaded: Boolean = false,
    val capabilities: DeviceCapabilities = DeviceCapabilities()
)

class TranslationViewModel(application: Application) : AndroidViewModel(application) {
    private val translationDataSource = TranslationDataSource(application)
    private val historyRepository = HistoryRepository(application)
    private val ttsHelper = TextToSpeechHelper(application)
    private val voskHelper = VoskSpeechHelper(application)

    private val _uiState = MutableStateFlow(TranslationUiState())
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    val favorites: StateFlow<List<TranslationItem>> = historyRepository.history

    init {
        ttsHelper.onError = { errorMsg ->
            _uiState.update { it.copy(errorMessage = errorMsg) }
        }
        refreshCapabilities(application)
        checkModelsStatus()
    }

    fun openTtsSettings() {
        ttsHelper.openTtsSettings()
    }

    fun openPlayStoreForTts() {
        ttsHelper.openPlayStoreForTts()
    }

    fun checkModelsStatus() {
        val ready = voskHelper.areModelsReady()
        _uiState.update { it.copy(isModelsDownloaded = ready) }
    }

    fun downloadModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingModel = true, downloadProgressMessage = "Iniciando descarga de complementos de voz (Vosk)...", errorMessage = null) }
            voskHelper.ensureModelsReady(
                onProgress = { msg ->
                    _uiState.update { it.copy(downloadProgressMessage = msg) }
                },
                onComplete = {
                    _uiState.update { it.copy(isDownloadingModel = false, downloadProgressMessage = "", isModelsDownloaded = true, errorMessage = "¡Complementos de voz descargados con éxito!") }
                },
                onError = { err ->
                    _uiState.update { it.copy(isDownloadingModel = false, errorMessage = err) }
                }
            )
        }
    }

    fun refreshCapabilities(context: Context) {
        val ttsCaps = ttsHelper.checkCapabilities()
        val permGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        checkModelsStatus()

        _uiState.update { state ->
            state.copy(
                capabilities = DeviceCapabilities(
                    ttsAvailable = ttsCaps["tts_available"] ?: false,
                    spanishTts = ttsCaps["spanish_available"] ?: false,
                    englishTts = ttsCaps["english_available"] ?: false,
                    micPermission = permGranted,
                    speechRecognizerAvailable = true
                )
            )
        }
    }

    fun updateSourceText(text: String) {
        _uiState.update { it.copy(sourceText = text) }
        if (text.isBlank()) {
            _uiState.update { it.copy(translatedText = "") }
        }
    }

    fun swapLanguages() {
        _uiState.update { state ->
            val newSource = state.targetLanguage
            val newTarget = state.sourceLanguage
            state.copy(
                sourceLanguage = newSource,
                targetLanguage = newTarget,
                sourceText = state.translatedText,
                translatedText = state.sourceText
            )
        }
    }

    fun setSourceLanguage(lang: Language) {
        if (_uiState.value.sourceLanguage == lang) return
        _uiState.update { it.copy(sourceLanguage = lang) }
    }

    fun setTargetLanguage(lang: Language) {
        if (_uiState.value.targetLanguage == lang) return
        _uiState.update { it.copy(targetLanguage = lang) }
    }

    fun translate() {
        val state = _uiState.value
        val text = state.sourceText.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(translatedText = "", errorMessage = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isDownloadingModel = true, downloadProgressMessage = "Verificando modelo offline...") }
            
            val downloadResult = translationDataSource.downloadModelIfNeeded(
                state.sourceLanguage.mlKitCode,
                state.targetLanguage.mlKitCode
            )

            if (downloadResult.isFailure) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isDownloadingModel = false,
                        errorMessage = "Error descargando modelo offline: ${downloadResult.exceptionOrNull()?.localizedMessage}. Verifica conexión para la primera descarga."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(downloadProgressMessage = "Traduciendo...") }

            val result = translationDataSource.translate(
                text,
                state.sourceLanguage.mlKitCode,
                state.targetLanguage.mlKitCode
            )

            result.fold(
                onSuccess = { translated ->
                    _uiState.update { 
                        it.copy(
                            translatedText = translated,
                            isLoading = false,
                            isDownloadingModel = false,
                            errorMessage = null
                        )
                    }
                    historyRepository.addTranslation(
                        TranslationItem(
                            sourceText = text,
                            translatedText = translated,
                            sourceLanguage = state.sourceLanguage,
                            targetLanguage = state.targetLanguage
                        )
                    )
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isDownloadingModel = false,
                            errorMessage = "Error en traducción: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun speakTranslation() {
        val text = _uiState.value.translatedText
        val lang = _uiState.value.targetLanguage.code
        ttsHelper.speak(text, lang)
    }

    fun speakSource() {
        val text = _uiState.value.sourceText
        val lang = _uiState.value.sourceLanguage.code
        ttsHelper.speak(text, lang)
    }

    fun startVoiceInput() {
        val state = _uiState.value
        if (!state.isModelsDownloaded) {
            _uiState.update { it.copy(errorMessage = "Primero debes descargar los complementos de voz usando el botón ⬇️ en la esquina superior derecha.") }
            return
        }

        val langCode = state.sourceLanguage.code
        _uiState.update { it.copy(isListening = true, errorMessage = null, downloadProgressMessage = "Escuchando (Vosk)...") }
        
        voskHelper.startListening(
            languageCode = langCode,
            onPartialResult = { partial ->
                _uiState.update { it.copy(sourceText = partial) }
            },
            onResult = { finalResult ->
                _uiState.update { it.copy(sourceText = finalResult, isListening = false, downloadProgressMessage = "") }
                translate()
            },
            onError = { error ->
                _uiState.update { it.copy(isListening = false, errorMessage = error, downloadProgressMessage = "") }
            }
        )
    }

    fun stopVoiceInput() {
        voskHelper.stopListening()
        _uiState.update { it.copy(isListening = false, downloadProgressMessage = "") }
    }

    fun toggleFavorite(id: String) {
        historyRepository.toggleFavorite(id)
    }

    fun deleteFavoriteItem(id: String) {
        historyRepository.deleteItem(id)
    }

    fun setSelectedTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setErrorMessage(message: String?) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun clearText() {
        _uiState.update { it.copy(sourceText = "", translatedText = "", errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
        voskHelper.shutdown()
    }
}
