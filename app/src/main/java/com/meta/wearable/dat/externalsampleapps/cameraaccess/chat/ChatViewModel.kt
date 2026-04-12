package com.meta.wearable.dat.externalsampleapps.cameraaccess.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.nova.BedrockClient
import com.meta.wearable.dat.externalsampleapps.cameraaccess.nova.ElevenLabsManager
import com.meta.wearable.dat.externalsampleapps.cameraaccess.nova.Prompts
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val bedrockClient = BedrockClient()
    private var elevenLabsManager: ElevenLabsManager? = null
    private var language: AppLanguage = AppLanguage.SPANISH

    init {
        // Initialize ElevenLabs
        val apiKey = com.meta.wearable.dat.externalsampleapps.cameraaccess.BuildConfig.ELEVENLABS_API_KEY
        val voiceId = com.meta.wearable.dat.externalsampleapps.cameraaccess.BuildConfig.ELEVENLABS_VOICE_ID
        
        if (apiKey.isNotEmpty() && voiceId.isNotEmpty()) {
            elevenLabsManager = ElevenLabsManager(
                context = getApplication(),
                apiKey = apiKey,
                voiceId = voiceId
            )
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isProcessing.value) return

        // Add user message
        val userMessage = ChatMessage(text = text, isUser = true)
        _messages.update { it + userMessage }
        _isProcessing.update { true }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val systemPrompt = Prompts.system(language.locale.language, "")
                val response = bedrockClient.askTextOnly(text, systemPrompt)

                // Add assistant response
                val assistantMessage = ChatMessage(text = response, isUser = false)
                _messages.update { it + assistantMessage }

                // Speak response
                elevenLabsManager?.speakOptimized(response) {
                    Log.d(TAG, "Speech completed")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
                val errorMessage = ChatMessage(
                    text = "Error: ${e.message}",
                    isUser = false
                )
                _messages.update { it + errorMessage }
            } finally {
                _isProcessing.update { false }
            }
        }
    }

    fun clearHistory() {
        _messages.update { emptyList() }
    }

    fun setLanguage(lang: AppLanguage) {
        language = lang
    }

    override fun onCleared() {
        super.onCleared()
        elevenLabsManager?.destroy()
        bedrockClient.close()
    }
}
