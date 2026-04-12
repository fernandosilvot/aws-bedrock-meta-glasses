package com.meta.wearable.dat.externalsampleapps.cameraaccess.nova

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class UserMemory(
    val preferences: MutableMap<String, String> = mutableMapOf(),
    val facts: MutableList<String> = mutableListOf(),
    val interactions: MutableList<Interaction> = mutableListOf()
)

@Serializable
data class Interaction(
    val timestamp: Long,
    val userSaid: String,
    val context: String = ""
)

class MemoryManager(private val context: Context) {
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val memoryFile = File(context.filesDir, "jarvis_memory.json")
    private var memory: UserMemory = loadMemory()
    
    companion object {
        private const val TAG = "MemoryManager"
        private const val MAX_INTERACTIONS = 50 // Últimas 50 interacciones
    }
    
    private fun loadMemory(): UserMemory {
        return try {
            if (memoryFile.exists()) {
                val jsonString = memoryFile.readText()
                json.decodeFromString<UserMemory>(jsonString)
            } else {
                UserMemory()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading memory", e)
            UserMemory()
        }
    }
    
    private fun saveMemory() {
        try {
            val jsonString = json.encodeToString(memory)
            memoryFile.writeText(jsonString)
            Log.d(TAG, "Memory saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving memory", e)
        }
    }
    
    fun addInteraction(userSaid: String, context: String = "") {
        val interaction = Interaction(
            timestamp = System.currentTimeMillis(),
            userSaid = userSaid,
            context = context
        )
        memory.interactions.add(interaction)
        
        // Mantener solo las últimas N interacciones
        if (memory.interactions.size > MAX_INTERACTIONS) {
            memory.interactions.removeAt(0)
        }
        
        saveMemory()
    }
    
    fun addFact(fact: String) {
        // Buscar si hay un hecho similar para reemplazarlo
        val factKey = fact.substringBefore(":", fact).trim()
        val existingIndex = memory.facts.indexOfFirst { 
            it.substringBefore(":", it).trim() == factKey 
        }
        
        if (existingIndex >= 0) {
            // Actualizar hecho existente
            memory.facts[existingIndex] = fact
            Log.d(TAG, "Fact updated: $fact")
        } else {
            // Agregar nuevo hecho
            memory.facts.add(fact)
            Log.d(TAG, "New fact learned: $fact")
        }
        saveMemory()
    }
    
    fun setPreference(key: String, value: String) {
        memory.preferences[key] = value
        saveMemory()
        Log.d(TAG, "Preference set: $key = $value")
    }
    
    fun getMemoryContext(): String {
        val sb = StringBuilder()
        
        if (memory.preferences.isNotEmpty()) {
            sb.append("\n\nPREFERENCIAS DEL USUARIO:\n")
            memory.preferences.forEach { (key, value) ->
                sb.append("- $key: $value\n")
            }
        }
        
        if (memory.facts.isNotEmpty()) {
            sb.append("\n\nHECHOS SOBRE EL USUARIO:\n")
            memory.facts.takeLast(10).forEach { fact ->
                sb.append("- $fact\n")
            }
        }
        
        if (memory.interactions.isNotEmpty()) {
            sb.append("\n\nÚLTIMAS INTERACCIONES:\n")
            memory.interactions.takeLast(5).forEach { interaction ->
                sb.append("- Usuario: ${interaction.userSaid}\n")
                if (interaction.context.isNotEmpty()) {
                    sb.append("  Contexto: ${interaction.context}\n")
                }
            }
        }
        
        return sb.toString()
    }
    
    fun extractLearnings(transcript: String, response: String) {
        val lowerTranscript = transcript.lowercase()
        
        // Detectar nombre - mejorado para capturar "llámame Silva"
        when {
            lowerTranscript.contains("mi nombre es") -> {
                val name = transcript.substringAfter("mi nombre es", "").trim().split(" ").firstOrNull()
                if (!name.isNullOrEmpty()) {
                    setPreference("nombre", name)
                    Log.d(TAG, "Nombre detectado: $name")
                }
            }
            lowerTranscript.contains("me llamo") -> {
                val name = transcript.substringAfter("me llamo", "").trim().split(" ").firstOrNull()
                if (!name.isNullOrEmpty()) {
                    setPreference("nombre", name)
                    Log.d(TAG, "Nombre detectado: $name")
                }
            }
            lowerTranscript.contains("llámame") || lowerTranscript.contains("llamame") -> {
                val afterLlamame = if (lowerTranscript.contains("llámame")) {
                    transcript.substringAfter("llámame", "")
                } else {
                    transcript.substringAfter("llamame", "")
                }
                val name = afterLlamame.trim().split(" ").firstOrNull()
                if (!name.isNullOrEmpty() && name.length > 1) {
                    setPreference("nombre", name)
                    Log.d(TAG, "Nombre detectado: $name")
                }
            }
        }
        
        // Detectar preferencias
        when {
            lowerTranscript.contains("me gusta") -> {
                val preference = transcript.substringAfter("me gusta", "").trim()
                if (preference.isNotEmpty()) {
                    addFact("Le gusta: $preference")
                }
            }
            lowerTranscript.contains("no me gusta") -> {
                val preference = transcript.substringAfter("no me gusta", "").trim()
                if (preference.isNotEmpty()) {
                    addFact("No le gusta: $preference")
                }
            }
            lowerTranscript.contains("recuerda que") -> {
                val fact = transcript.substringAfter("recuerda que", "").trim()
                if (fact.isNotEmpty()) {
                    addFact(fact)
                }
            }
        }
    }
    
    fun clearMemory() {
        memory = UserMemory()
        saveMemory()
        Log.d(TAG, "Memory cleared")
    }
}
