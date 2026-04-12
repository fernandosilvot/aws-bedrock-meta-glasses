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
import kotlinx.coroutines.delay
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
    private var elevenLabsManager: ElevenLabsManager? = null
    private var currentFrameProvider: (() -> Bitmap?)? = null
    private var language: AppLanguage = AppLanguage.ENGLISH
    private var processingJob: Job? = null
    
    val tools = JarvisTools()
    private val memoryManager = MemoryManager(application)
    private val bluetoothAudioManager = BluetoothAudioManager(application)
    
    // Tavily API - Obtén tu key gratis en https://app.tavily.com
    private val tavilyApiKey = "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"
    private val tavilySearch = if (tavilyApiKey.isNotEmpty()) {
        TavilySearchService(tavilyApiKey)
    } else null

    private val mainHandler = Handler(Looper.getMainLooper())

    private var activatedByVoice = true
    private var useElevenLabs = true  // Mantener ElevenLabs

    fun initialize(frameProvider: () -> Bitmap?, lang: AppLanguage) {
        currentFrameProvider = frameProvider
        language = lang
        
        // Opción A: Micrófono del celular + ElevenLabs alta calidad en lentes
        // Esta es la configuración óptima para mejor experiencia
        Log.d(TAG, "🎤 Mic: Phone | 🔊 Audio: Meta Ray-Ban (ElevenLabs)")
        
        // Initialize ElevenLabs if credentials available
        val apiKey = com.meta.wearable.dat.externalsampleapps.cameraaccess.BuildConfig.ELEVENLABS_API_KEY
        val voiceId = com.meta.wearable.dat.externalsampleapps.cameraaccess.BuildConfig.ELEVENLABS_VOICE_ID
        
        if (apiKey.isNotEmpty() && voiceId.isNotEmpty()) {
            elevenLabsManager = ElevenLabsManager(
                context = getApplication(),
                apiKey = apiKey,
                voiceId = voiceId
            )
            Log.d(TAG, "ElevenLabs initialized")
            
            // Pre-cache common phrases in background
            viewModelScope.launch(Dispatchers.IO) {
                delay(2000) // Wait 2 seconds after init
                preCacheCommonPhrases()
            }
        } else {
            useElevenLabs = false
            Log.w(TAG, "ElevenLabs credentials not found, using Android TTS")
        }
        
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
    
    private suspend fun preCacheCommonPhrases() {
        val commonPhrases = if (language == AppLanguage.SPANISH) {
            listOf(
                "A sus órdenes, señor.",
                "Todos los sistemas operativos, señor.",
                "Procesando su solicitud.",
                "Tarea completada, señor.",
                "Entendido perfectamente, señor."
            )
        } else {
            listOf(
                "At your service, sir.",
                "All systems operational, sir.",
                "Processing your request.",
                "Task completed, sir.",
                "Understood perfectly, sir."
            )
        }
        
        commonPhrases.forEach { phrase ->
            try {
                elevenLabsManager?.speak(phrase, useCache = true) { }
                delay(500)
            } catch (e: Exception) {
                Log.e(TAG, "Pre-cache error: ${e.message}")
            }
        }
        Log.d(TAG, "Pre-caching completed")
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
                // Tavily web search if enabled and needed
                var searchContext = ""
                if (tavilySearch != null && tavilySearch.shouldSearch(transcript)) {
                    Log.d(TAG, "Performing Tavily web search...")
                    val searchResult = tavilySearch.search(transcript)
                    if (searchResult != null) {
                        searchContext = buildSearchContext(searchResult)
                        Log.d(TAG, "Tavily search completed: ${searchResult.results.size} results")
                    }
                }
                
                val systemPrompt = Prompts.system(language.locale.language, tools.getToolsDescription())
                val memoryContext = memoryManager.getMemoryContext()
                val fullPrompt = buildString {
                    append(systemPrompt)
                    if (memoryContext.isNotEmpty()) append(memoryContext)
                    if (searchContext.isNotEmpty()) append(searchContext)
                }
                
                val response = if (frame != null) {
                    bedrockClient.askWithImage(transcript, frame, fullPrompt)
                } else {
                    bedrockClient.askTextOnly(transcript, fullPrompt)
                }
                
                Log.d(TAG, "Claude response: $response")
                
                // Guardar interacción y aprender
                memoryManager.addInteraction(transcript, if (frame != null) "con imagen" else "sin imagen")
                memoryManager.extractLearnings(transcript, response)
                
                // Parse tool calls
                when {
                    response.contains("[TOOL:activate_camera]") -> {
                        tools.requestActivateCamera()
                        val cleanResponse = response.replace("[TOOL:activate_camera]", "").replace(Regex("\\[TOOL:.*?\\]"), "").trim()
                        _uiState.update { it.copy(state = NovaState.RESPONDING, response = cleanResponse) }
                        speakResponse(cleanResponse)
                    }
                    response.contains("[TOOL:deactivate_camera]") -> {
                        tools.requestDeactivateCamera()
                        val cleanResponse = response.replace("[TOOL:deactivate_camera]", "").replace(Regex("\\[TOOL:.*?\\]"), "").trim()
                        _uiState.update { it.copy(state = NovaState.RESPONDING, response = cleanResponse) }
                        speakResponse(cleanResponse)
                    }
                    response.contains("[TOOL:capture_and_analyze]") -> {
                        tools.requestCaptureAndAnalyze(transcript)
                        val cleanResponse = response.replace("[TOOL:capture_and_analyze]", "").replace(Regex("\\[TOOL:.*?\\]"), "").trim()
                        _uiState.update { it.copy(state = NovaState.RESPONDING, response = cleanResponse) }
                        speakResponse(cleanResponse)
                    }
                    else -> {
                        val cleanResponse = response.replace(Regex("\\[TOOL:.*?\\]"), "").trim()
                        _uiState.update { it.copy(state = NovaState.RESPONDING, response = cleanResponse) }
                        speakResponse(cleanResponse)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bedrock error", e)
                _uiState.update { it.copy(state = NovaState.ERROR, error = e.message) }
                mainHandler.post { speechManager?.startPassiveListening() }
            }
        }
    }
    
    private fun speakResponse(text: String) {
        if (useElevenLabs && elevenLabsManager != null) {
            viewModelScope.launch {
                elevenLabsManager?.speakOptimized(text) {
                    handleSpeechComplete()
                }
            }
        } else {
            speechManager?.speak(text) {
                handleSpeechComplete()
            }
        }
    }
    
    private fun buildSearchContext(searchResult: SearchResult): String {
        return buildString {
            append("\n\nINFORMACIÓN ACTUALIZADA DE LA WEB:\n")
            
            if (searchResult.answer != null) {
                append("Respuesta: ${searchResult.answer}\n\n")
            }
            
            if (searchResult.results.isNotEmpty()) {
                append("Fuentes:\n")
                searchResult.results.take(3).forEach { item ->
                    append("- ${item.title}: ${item.content.take(200)}...\n")
                }
            }
        }
    }

    private fun handleSpeechComplete() {
        if (activatedByVoice) {
            _uiState.update { NovaUiState() }
            mainHandler.post { speechManager?.startPassiveListening() }
        } else {
            _uiState.update { it.copy(state = NovaState.LISTENING, transcript = null, response = null) }
            mainHandler.post { speechManager?.startActiveListening() }
        }
    }
    
    fun toggleTtsMode() {
        useElevenLabs = !useElevenLabs
        Log.d(TAG, "TTS mode: ${if (useElevenLabs) "ElevenLabs" else "Android TTS"}")
    }
    
    fun setVoiceProfile(profile: String) {
        elevenLabsManager?.currentProfile = profile
        Log.d(TAG, "Voice profile: $profile")
    }
    
    fun analyzePhoto(photo: Bitmap, originalTranscript: String) {
        Log.d(TAG, "analyzePhoto called with transcript: $originalTranscript")
        _uiState.update { it.copy(state = NovaState.PROCESSING) }
        
        processingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Analyzing photo with Claude...")
                val systemPrompt = Prompts.system(language.locale.language, tools.getToolsDescription())
                val response = bedrockClient.askWithImage(originalTranscript, photo, systemPrompt)
                
                Log.d(TAG, "Analysis response: $response")
                
                _uiState.update { it.copy(
                    state = NovaState.RESPONDING, 
                    response = response,
                    sentWithImage = true
                ) }
                
                // Speak response
                if (useElevenLabs && elevenLabsManager != null) {
                    elevenLabsManager?.speakOptimized(response) {
                        handleSpeechComplete()
                    }
                } else {
                    speechManager?.speak(response) {
                        handleSpeechComplete()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Photo analysis error", e)
                _uiState.update { it.copy(state = NovaState.ERROR, error = e.message) }
                mainHandler.post { speechManager?.startPassiveListening() }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        processingJob?.cancel()
        mainHandler.post { speechManager?.destroy() }
        elevenLabsManager?.destroy()
        bluetoothAudioManager.clearBluetoothAudio()
        bedrockClient.close()
    }
}
