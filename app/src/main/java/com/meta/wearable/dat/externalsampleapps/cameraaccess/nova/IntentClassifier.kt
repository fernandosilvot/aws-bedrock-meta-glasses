package com.meta.wearable.dat.externalsampleapps.cameraaccess.nova

object IntentClassifier {
    private val imageKeywords = listOf(
        // Spanish
        "veo", "viendo", "miro", "mirando", "esto", "eso", "lee", "leer", "cartel",
        "describe", "qué hay", "qué es", "frente", "muestra", "foto", "imagen",
        "texto", "letrero", "pantalla", "objeto", "color", "persona", "escena",
        // English
        "see", "seeing", "look", "looking", "this", "that", "read", "sign",
        "describe", "what is", "what's", "show", "photo", "image", "text",
        "screen", "object", "color", "person", "scene", "front",
    )

    fun needsImage(transcript: String): Boolean {
        val lower = transcript.lowercase()
        return imageKeywords.any { lower.contains(it) }
    }
}
