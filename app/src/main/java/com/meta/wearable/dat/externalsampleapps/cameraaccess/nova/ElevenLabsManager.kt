package com.meta.wearable.dat.externalsampleapps.cameraaccess.nova

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class ElevenLabsManager(
    private val context: Context,
    private val apiKey: String,
    private val voiceId: String
) {
    companion object {
        private const val TAG = "ElevenLabsManager"
        private const val BASE_URL = "https://api.elevenlabs.io/v1"
        private const val CACHE_DIR = "elevenlabs_cache"
        private const val MAX_CACHE_SIZE = 50 // Máximo de audios en cache
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val audioCache = ConcurrentHashMap<String, ByteArray>()
    private var audioTrack: AudioTrack? = null
    private var currentMediaPlayer: android.media.MediaPlayer? = null

    data class VoiceSettings(
        val stability: Float,
        val similarityBoost: Float,
        val style: Float
    )

    // Perfiles de jarvis_advanced.py
    private val profiles = mapOf(
        "classic" to VoiceSettings(0.5f, 0.75f, 0.0f),
        "hybrid" to VoiceSettings(0.6f, 0.7f, 0.05f),
        "conversational" to VoiceSettings(0.4f, 0.65f, 0.1f),
        "robotic" to VoiceSettings(0.8f, 0.8f, 0.0f)
    )
    
    var currentProfile = "hybrid"

    init {
        initCacheDir()
        // Pre-warm connection
        Thread {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/voices")
                    .addHeader("xi-api-key", apiKey)
                    .build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.d(TAG, "Pre-warm failed: ${e.message}")
            }
        }.start()
    }

    private fun initCacheDir() {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    suspend fun speak(text: String, useCache: Boolean = true, onDone: () -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Speaking: $text (profile: $currentProfile, cache: $useCache)")
                
                val audioBytes = if (useCache) {
                    getCachedOrGenerate(text)
                } else {
                    generateAudio(text)
                }
                
                playAudioBytes(audioBytes, onDone)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in speak", e)
                withContext(Dispatchers.Main) { onDone() }
            }
        }
    }

    suspend fun speakOptimized(text: String, onDone: () -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Optimized streaming: $text")
                
                val settings = profiles[currentProfile]!!
                val json = createJsonBody(text, settings)

                val request = Request.Builder()
                    .url("$BASE_URL/text-to-speech/$voiceId/stream?optimize_streaming_latency=4&output_format=mp3_22050_32")
                    .addHeader("xi-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Streaming error: ${response.code}")
                    }
                    
                    val audioBytes = response.body?.bytes() ?: throw IOException("Empty response")
                    Log.d(TAG, "Audio received: ${audioBytes.size} bytes")
                    
                    playAudioBytes(audioBytes, onDone)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in optimized streaming", e)
                withContext(Dispatchers.Main) { onDone() }
            }
        }
    }

    private suspend fun getCachedOrGenerate(text: String): ByteArray {
        val cacheKey = "${text}_${currentProfile}".hashCode().toString()
        
        // Check memory cache
        audioCache[cacheKey]?.let {
            Log.d(TAG, "Using memory cache")
            return it
        }
        
        // Check disk cache
        val cacheFile = File(context.cacheDir, "$CACHE_DIR/$cacheKey.mp3")
        if (cacheFile.exists()) {
            Log.d(TAG, "Using disk cache")
            val bytes = cacheFile.readBytes()
            audioCache[cacheKey] = bytes
            return bytes
        }
        
        // Generate new
        Log.d(TAG, "Generating new audio")
        val bytes = generateAudio(text)
        
        // Save to cache
        saveToCache(cacheKey, bytes)
        
        return bytes
    }

    private suspend fun generateAudio(text: String): ByteArray {
        val settings = profiles[currentProfile]!!
        val json = createJsonBody(text, settings)

        val request = Request.Builder()
            .url("$BASE_URL/text-to-speech/$voiceId")
            .addHeader("xi-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("API error: ${response.code} - ${response.message}")
                }
                response.body?.bytes() ?: throw IOException("Empty response")
            }
        }
    }

    private fun createJsonBody(text: String, settings: VoiceSettings): String {
        return JSONObject().apply {
            put("text", text)
            put("model_id", "eleven_multilingual_v2")
            put("voice_settings", JSONObject().apply {
                put("stability", settings.stability)
                put("similarity_boost", settings.similarityBoost)
                put("style", settings.style)
                put("use_speaker_boost", true)
            })
        }.toString()
    }

    private suspend fun playAudioBytes(audioBytes: ByteArray, onDone: () -> Unit) {
        withContext(Dispatchers.Main) {
            try {
                // Release previous player
                currentMediaPlayer?.release()
                
                val tempFile = File(context.cacheDir, "elevenlabs_temp_${System.currentTimeMillis()}.mp3")
                tempFile.writeBytes(audioBytes)
                
                Log.d(TAG, "Playing audio: ${tempFile.absolutePath} (${audioBytes.size} bytes)")

                currentMediaPlayer = android.media.MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)  // Usar altavoz multimedia
                            .build()
                    )
                    setDataSource(tempFile.absolutePath)
                    setVolume(1.0f, 1.0f)  // Volumen máximo
                    setOnCompletionListener { mp ->
                        Log.d(TAG, "Playback completed")
                        mp.release()
                        currentMediaPlayer = null
                        tempFile.delete()
                        onDone()
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                        mp.release()
                        currentMediaPlayer = null
                        tempFile.delete()
                        onDone()
                        true
                    }
                    prepare()
                    start()
                    Log.d(TAG, "MediaPlayer started, duration: ${duration}ms")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error playing audio", e)
                onDone()
            }
        }
    }

    private suspend fun playStreamingAudio(inputStream: java.io.InputStream, onDone: () -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                // Collect all bytes first (MP3 needs full file)
                val allBytes = inputStream.readBytes()
                playAudioBytes(allBytes, onDone)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in streaming playback", e)
                withContext(Dispatchers.Main) { onDone() }
            }
        }
    }

    private suspend fun playStreamingAudioOptimized(inputStream: java.io.InputStream, onDone: () -> Unit) {
        withContext(Dispatchers.Main) {
            try {
                // Save to temp file and play immediately
                val tempFile = File(context.cacheDir, "elevenlabs_stream_${System.currentTimeMillis()}.mp3")
                
                // Start writing in background
                val writeJob = withContext(Dispatchers.IO) {
                    tempFile.outputStream().use { output ->
                        inputStream.copyTo(output, bufferSize = 8192)
                    }
                }
                
                // Start playing as soon as we have some data
                delay(100) // Small delay to ensure file has started writing
                
                val mediaPlayer = android.media.MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .build()
                    )
                    setDataSource(tempFile.absolutePath)
                    setOnCompletionListener {
                        release()
                        tempFile.delete()
                        onDone()
                    }
                    setOnErrorListener { _, _, _ ->
                        release()
                        tempFile.delete()
                        onDone()
                        true
                    }
                    prepareAsync()
                    setOnPreparedListener { start() }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in optimized streaming playback", e)
                onDone()
            }
        }
    }

    private fun saveToCache(key: String, bytes: ByteArray) {
        try {
            // Memory cache
            if (audioCache.size >= MAX_CACHE_SIZE) {
                audioCache.clear()
            }
            audioCache[key] = bytes
            
            // Disk cache
            val cacheFile = File(context.cacheDir, "$CACHE_DIR/$key.mp3")
            cacheFile.writeBytes(bytes)
            
            // Clean old cache files
            cleanOldCache()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cache", e)
        }
    }

    private fun cleanOldCache() {
        try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            val files = cacheDir.listFiles() ?: return
            
            if (files.size > MAX_CACHE_SIZE) {
                files.sortedBy { it.lastModified() }
                    .take(files.size - MAX_CACHE_SIZE)
                    .forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning cache", e)
        }
    }

    fun clearCache() {
        audioCache.clear()
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun destroy() {
        currentMediaPlayer?.release()
        currentMediaPlayer = null
        audioTrack?.release()
        audioTrack = null
    }
}
