package com.example.traductor.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognizerHelper(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(
        languageCode: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onReadyForSpeech: () -> Unit = {}
    ) {
        mainHandler.post {
            val available = SpeechRecognizer.isRecognitionAvailable(context)
            Log.d("STT_DEBUG", "SpeechRecognizer.isRecognitionAvailable: $available")

            if (!available) {
                Log.e("STT_DEBUG", "Speech recognition not available on this device/emulator.")
                onError("Reconocimiento de voz no disponible en este dispositivo. (Nota: algunos emuladores no incluyen SpeechRecognizer offline).")
                return@post
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d("STT_DEBUG", "onReadyForSpeech")
                            onReadyForSpeech()
                        }
                        override fun onBeginningOfSpeech() {
                            Log.d("STT_DEBUG", "onBeginningOfSpeech")
                        }
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            Log.d("STT_DEBUG", "onEndOfSpeech")
                        }
                        override fun onError(error: Int) {
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Error de grabación de audio"
                                SpeechRecognizer.ERROR_CLIENT -> "Error de cliente de reconocimiento"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono denegado"
                                SpeechRecognizer.ERROR_NETWORK -> "Error de red (algunos motores de voz pueden requerir conexión o datos offline en Google App)"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red agotado"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó voz comprensible"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocimiento ocupado, intenta de nuevo"
                                SpeechRecognizer.ERROR_SERVER -> "Error del servidor de reconocimiento"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz (tiempo agotado)"
                                else -> "Error de reconocimiento desconocido ($error)"
                            }
                            Log.e("STT_DEBUG", "SpeechRecognizer error code: $error -> $message")
                            onError(message)
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            Log.d("STT_DEBUG", "onResults matches: $matches")
                            if (!matches.isNullOrEmpty()) {
                                onResult(matches[0])
                            } else {
                                onError("No se obtuvieron resultados de voz")
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            Log.d("STT_DEBUG", "onPartialResults: $partial")
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (languageCode == "es") "es-ES" else "en-US")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                Log.d("STT_DEBUG", "Starting listening for language: $languageCode")
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("STT_DEBUG", "Exception starting speech recognizer", e)
                onError("No se pudo iniciar el reconocimiento: ${e.localizedMessage}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                Log.d("STT_DEBUG", "Stopping SpeechRecognizer")
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e("STT_DEBUG", "Error stopping SpeechRecognizer", e)
            }
        }
    }
}
