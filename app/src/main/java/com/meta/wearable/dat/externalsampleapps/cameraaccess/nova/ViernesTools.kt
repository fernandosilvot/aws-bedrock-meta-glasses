package com.meta.wearable.dat.externalsampleapps.cameraaccess.nova

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class ToolAction {
    object ActivateCamera : ToolAction()
    object DeactivateCamera : ToolAction()
    data class CaptureAndAnalyze(val transcript: String) : ToolAction()
}

class JarvisTools {
    private val _pendingAction = MutableStateFlow<ToolAction?>(null)
    val pendingAction: StateFlow<ToolAction?> = _pendingAction
    
    fun requestActivateCamera() {
        _pendingAction.value = ToolAction.ActivateCamera
    }
    
    fun requestDeactivateCamera() {
        _pendingAction.value = ToolAction.DeactivateCamera
    }
    
    fun requestCaptureAndAnalyze(transcript: String) {
        _pendingAction.value = ToolAction.CaptureAndAnalyze(transcript)
    }
    
    fun clearAction() {
        _pendingAction.value = null
    }
    
    fun getToolsDescription(): String = """
        Tienes acceso a las siguientes herramientas:
        
        1. activate_camera: Activa la cámara para ver en tiempo real lo que el usuario está mirando
           - Usar cuando: "muéstrame", "activa cámara", "quiero ver", "déjame ver"
        
        2. deactivate_camera: Desactiva la cámara
           - Usar cuando: "apaga cámara", "desactiva", "deja de ver", "cierra visión"
        
        3. capture_and_analyze: Captura una foto de alta calidad y la analiza en detalle
           - Usar cuando: "analiza", "examina", "describe en detalle", "qué es esto", "identifica"
           - IMPORTANTE: Esta herramienta captura foto, NO solo activa cámara
        
        Para usar una herramienta, responde EXACTAMENTE con el formato:
        [TOOL:nombre_herramienta] Tu mensaje al usuario
        
        Ejemplos:
        - Usuario: "muéstrame lo que hay" → [TOOL:activate_camera] Activando cámara, señor.
        - Usuario: "analiza esto" → [TOOL:capture_and_analyze] Capturando y analizando imagen, señor.
        - Usuario: "qué estoy mirando" → [TOOL:capture_and_analyze] Analizando lo que está observando, señor.
    """.trimIndent()
}
