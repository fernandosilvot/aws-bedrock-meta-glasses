package com.meta.wearable.dat.externalsampleapps.cameraaccess.nova

import android.app.Application
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NovaViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "NovaViewModel"
    }

    private val _uiState = MutableStateFlow(NovaUiState())
    val uiState: StateFlow<NovaUiState> = _uiState.asStateFlow()

    private val bedrockClient = BedrockClient()
    private var speechManager: SpeechManager? = null
    private var currentFrameProvider: (() -> Bitmap?)? = null
    private var language: AppLanguage = AppLanguage.ENGLISH
    private var processingJob: Job? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private var activatedByVoice = true

    fun initialize(frameProvider: () -> Bitmap?, lang: AppLanguage) {
        currentFrameProvider = frameProvider
        language = lang
        mainHandler.post {
            speechManager = SpeechManager(
                context = getApplication(),
                onWakeWord = { onWakeWordDetected() },
                onTranscript = { onTranscriptReady(it) },
                onListeningStarted = {
                    _uiState.update { it.copy(state = NovaState.LISTENING, transcript = null, response = null) }
                },
            ).apply { initialize(lang.locale) }
        }
    }

    fun startListening() {
        mainHandler.post { speechManager?.startPassiveListening() }
    }

    fun stopListening() {
        mainHandler.post { speechManager?.stop() }
        _uiState.update { NovaUiState() }
    }

    /** Manual trigger — stays active until button pressed again */
    fun activateManually() {
        activatedByVoice = false
        _uiState.update { it.copy(state = NovaState.LISTENING, transcript = null, response = null) }
        mainHandler.post { speechManager?.startActiveListening() }
    }

    fun dismiss() {
        processingJob?.cancel()
        _uiState.update { NovaUiState() }
        mainHandler.post { speechManager?.startPassiveListening() }
    }

    fun updateLanguage(lang: AppLanguage) {
        language = lang
        mainHandler.post { speechManager?.initialize(lang.locale) }
    }

    private fun onWakeWordDetected() {
        Log.d(TAG, "Wake word detected")
        activatedByVoice = true
        _uiState.update { it.copy(state = NovaState.LISTENING, transcript = null, response = null) }
        mainHandler.post { speechManager?.startActiveListening() }
    }

    private fun onTranscriptReady(transcript: String) {
        Log.d(TAG, "Transcript: $transcript")
        val frame = currentFrameProvider?.invoke()

        _uiState.update { it.copy(state = NovaState.PROCESSING, transcript = transcript, sentWithImage = frame != null) }

        processingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val systemPrompt = Prompts.system(language.locale.language)
                val response = if (frame != null) {
                    bedrockClient.askWithImage(transcript, frame, systemPrompt)
                } else {
                    bedrockClient.askTextOnly(transcript, systemPrompt)
                }
                _uiState.update { it.copy(state = NovaState.RESPONDING, response = response) }
                speechManager?.speak(response) {
                    if (activatedByVoice) {
                        // Voice-activated: go back to passive, need "Oye Nova" again
                        _uiState.update { NovaUiState() }
                        mainHandler.post { speechManager?.startPassiveListening() }
                    } else {
                        // Button-activated: keep listening for follow-up
                        _uiState.update { it.copy(state = NovaState.LISTENING, transcript = null, response = null) }
                        mainHandler.post { speechManager?.startActiveListening() }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bedrock error", e)
                _uiState.update { it.copy(state = NovaState.ERROR, error = e.message) }
                mainHandler.post { speechManager?.startPassiveListening() }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        processingJob?.cancel()
        mainHandler.post { speechManager?.destroy() }
        bedrockClient.close()
    }
}
