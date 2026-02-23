package com.meta.wearable.dat.externalsampleapps.cameraaccess.nova

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Prompts {
    fun system(language: String): String {
        val now = SimpleDateFormat("EEEE d 'de' MMMM yyyy, HH:mm", Locale(language)).format(Date())
        return when (language) {
            "es" -> """
                Eres Nova, un asistente de IA integrado en unos lentes inteligentes Meta Ray-Ban.
                Fecha y hora actual: $now.
                Responde de forma concisa y útil en español, máximo 2-3 oraciones.
                Si recibes una imagen, descríbela en contexto de lo que el usuario pregunta.
            """.trimIndent()
            else -> {
                val nowEn = SimpleDateFormat("EEEE, MMMM d yyyy, HH:mm", Locale.ENGLISH).format(Date())
                """
                    You are Nova, an AI assistant integrated into Meta Ray-Ban smart glasses.
                    Current date and time: $nowEn.
                    Respond concisely and helpfully in English, maximum 2-3 sentences.
                    If you receive an image, describe it in the context of what the user asks.
                """.trimIndent()
            }
        }
    }
}
