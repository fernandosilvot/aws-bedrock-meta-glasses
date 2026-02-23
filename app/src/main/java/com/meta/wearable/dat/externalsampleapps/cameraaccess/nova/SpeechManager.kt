package com.meta.wearable.dat.externalsampleapps.cameraaccess.nova

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onWakeWord: () -> Unit,
    private val onTranscript: (String) -> Unit,
    private val onListeningStarted: () -> Unit,
) {
    companion object {
        private const val TAG = "SpeechManager"
        private val WAKE_WORDS = listOf("oye nova", "hey nova", "oi nova", "oye nu va", "hey nu va")
    }

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isActiveListening = false
    private var currentLocale: Locale = Locale.getDefault()
    private val handler = Handler(Looper.getMainLooper())

    fun initialize(locale: Locale) {
        currentLocale = locale
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = locale
            }
        }
    }

    fun startPassiveListening() {
        isActiveListening = false
        restartRecognizer()
    }

    fun startActiveListening() {
        isActiveListening = true
        onListeningStarted()
        restartRecognizer()
    }

    fun stop() {
        destroyRecognizer()
    }

    fun speak(text: String, onDone: () -> Unit = {}) {
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onDone() }
            @Deprecated("Deprecated") override fun onError(utteranceId: String?) { onDone() }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nova_response")
    }

    fun destroy() {
        destroyRecognizer()
        tts?.shutdown()
        tts = null
    }

    private fun destroyRecognizer() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
    }

    private fun restartRecognizer() {
        destroyRecognizer()
        // Small delay to let the old recognizer fully release
        handler.postDelayed({
            try {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                    startListening(createIntent())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recognizer", e)
            }
        }, 300)
    }

    private fun createListener() = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            Log.d(TAG, "Result: $text (active=$isActiveListening)")
            handleResult(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!isActiveListening) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.lowercase() ?: return
                if (WAKE_WORDS.any { text.contains(it) }) {
                    Log.d(TAG, "Wake word in partial: $text")
                    destroyRecognizer()
                    onWakeWord()
                }
            }
        }

        override fun onError(error: Int) {
            Log.d(TAG, "Speech error: $error (active=$isActiveListening)")
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    if (isActiveListening) {
                        // User didn't say anything after wake word, go back to passive
                        isActiveListening = false
                    }
                    restartRecognizer()
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    // Wait and retry
                    handler.postDelayed({ restartRecognizer() }, 500)
                }
                else -> restartRecognizer()
            }
        }

        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech (active=$isActiveListening)")
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleResult(text: String) {
        if (isActiveListening) {
            var clean = text
            WAKE_WORDS.forEach { clean = clean.lowercase().replace(it, "").trim() }
            if (clean.isNotBlank()) {
                Log.d(TAG, "Transcript ready: $clean")
                onTranscript(clean)
            } else {
                startActiveListening()
            }
        } else {
            val lower = text.lowercase()
            if (WAKE_WORDS.any { lower.contains(it) }) {
                destroyRecognizer()
                onWakeWord()
            } else {
                restartRecognizer()
            }
        }
    }

    private fun createIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLocale.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, !isActiveListening)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
}
