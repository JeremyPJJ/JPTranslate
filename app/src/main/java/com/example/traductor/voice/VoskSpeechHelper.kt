package com.example.traductor.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.zip.ZipInputStream

class VoskSpeechHelper(private val context: Context) {
    private var speechService: SpeechService? = null
    private var modelEs: Model? = null
    private var modelEn: Model? = null
    private var isDownloading = false

    fun areModelsReady(): Boolean {
        val esDir = File(context.filesDir, "model-es")
        val enDir = File(context.filesDir, "model-en")
        
        val esValid = esDir.exists() && File(esDir, "am").exists() && File(esDir, "graph").exists()
        val enValid = enDir.exists() && File(enDir, "am").exists() && File(enDir, "graph").exists()
        
        return esValid && enValid
    }

    suspend fun ensureModelsReady(
        onProgress: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (isDownloading) return@withContext
        isDownloading = true

        try {
            val esDir = File(context.filesDir, "model-es")
            val enDir = File(context.filesDir, "model-en")

            if (areModelsReady()) {
                loadModels()
                isDownloading = false
                withContext(Dispatchers.Main) { onComplete() }
                return@withContext
            }

            if (esDir.exists()) esDir.deleteRecursively()
            if (enDir.exists()) enDir.deleteRecursively()

            withContext(Dispatchers.Main) { onProgress("Descargando modelo de voz en Español (Vosk)...") }
            downloadAndUnzip(
                "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip",
                context.filesDir,
                "vosk-model-small-es-0.42",
                "model-es"
            )

            withContext(Dispatchers.Main) { onProgress("Descargando modelo de voz en Inglés (Vosk)...") }
            downloadAndUnzip(
                "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
                context.filesDir,
                "vosk-model-small-en-us-0.15",
                "model-en"
            )

            loadModels()
            isDownloading = false
            withContext(Dispatchers.Main) { onComplete() }
        } catch (e: Exception) {
            isDownloading = false
            Log.e("VOSK", "Error downloading/unpacking models", e)
            withContext(Dispatchers.Main) { 
                onError("Error descargando modelos Vosk: ${e.localizedMessage}. Verifica tu conexión a internet.") 
            }
        }
    }

    private fun downloadAndUnzip(urlString: String, targetDir: File, expectedFolderName: String, finalFolderName: String) {
        val zipFile = File(context.cacheDir, "$finalFolderName.zip")
        
        val url = URL(urlString)
        val connection = url.openConnection()
        connection.connect()
        val inputStream: InputStream = connection.getInputStream()
        FileOutputStream(zipFile).use { output ->
            inputStream.copyTo(output)
        }

        val finalDir = File(targetDir, finalFolderName)
        if (finalDir.exists()) finalDir.deleteRecursively()
        finalDir.mkdirs()

        val tempExtractDir = File(context.cacheDir, "${finalFolderName}_extracted")
        if (tempExtractDir.exists()) tempExtractDir.deleteRecursively()
        tempExtractDir.mkdirs()

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                val newFile = File(tempExtractDir, zipEntry.name)
                if (zipEntry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zipEntry = zis.nextEntry
            }
        }

        val extractedRoot = File(tempExtractDir, expectedFolderName)
        if (extractedRoot.exists()) {
            extractedRoot.renameTo(finalDir)
        } else {
            tempExtractDir.listFiles()?.firstOrNull()?.renameTo(finalDir) ?: tempExtractDir.renameTo(finalDir)
        }

        zipFile.delete()
        tempExtractDir.deleteRecursively()
    }

    private fun loadModels() {
        if (modelEs == null) {
            modelEs = Model(File(context.filesDir, "model-es").absolutePath)
        }
        if (modelEn == null) {
            modelEn = Model(File(context.filesDir, "model-en").absolutePath)
        }
    }

    fun startListening(
        languageCode: String,
        onPartialResult: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopListening()
        val model = if (languageCode == "es") modelEs else modelEn
        if (model == null) {
            if (areModelsReady()) {
                loadModels()
                startListening(languageCode, onPartialResult, onResult, onError)
                return
            }
            onError("Modelos Vosk aún no están listos. Descárgalos desde el botón superior derecho ⬇️.")
            return
        }

        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f).apply {
                startListening(object : RecognitionListener {
                    override fun onPartialResult(hypothesis: String?) {
                        hypothesis?.let {
                            try {
                                val json = JSONObject(it)
                                val text = json.optString("partial", "")
                                if (text.isNotBlank()) {
                                    onPartialResult(text)
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    override fun onResult(hypothesis: String?) {
                        hypothesis?.let {
                            try {
                                val json = JSONObject(it)
                                val text = json.optString("text", "")
                                if (text.isNotBlank()) {
                                    onResult(text)
                                    stopListening()
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    override fun onFinalResult(hypothesis: String?) {
                        hypothesis?.let {
                            try {
                                val json = JSONObject(it)
                                val text = json.optString("text", "")
                                if (text.isNotBlank()) {
                                    onResult(text)
                                    stopListening()
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    override fun onError(exception: Exception?) {
                        onError("Error de reconocimiento Vosk: ${exception?.localizedMessage}")
                    }

                    override fun onTimeout() {
                        stopListening()
                    }
                })
            }
        } catch (e: Exception) {
            onError("No se pudo iniciar Vosk: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechService?.stop()
            speechService?.shutdown()
            speechService = null
        } catch (e: Exception) {
            Log.e("VOSK", "Error stopping speech service", e)
        }
    }

    fun shutdown() {
        stopListening()
        try {
            modelEs?.close()
            modelEn?.close()
        } catch (_: Exception) {}
    }
}
