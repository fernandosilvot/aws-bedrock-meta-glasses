# 🎙️ Integración ElevenLabs TTS

## 📋 Descripción

Meta-Rock ahora soporta **dos motores TTS**:

1. **Android TTS** (por defecto, gratis, offline)
2. **ElevenLabs** (calidad premium, tipo JARVIS, requiere internet)

El sistema detecta automáticamente si tienes credenciales de ElevenLabs configuradas y usa el motor correspondiente.

---

## 🚀 Configuración

### 1. Obtener Credenciales de ElevenLabs

#### API Key
1. Ve a https://elevenlabs.io/app/settings/api-keys
2. Crea una nueva API key
3. Copia el valor

#### Voice ID
Opción A - Usar voz existente:
```bash
# Listar tus voces
curl -X GET "https://api.elevenlabs.io/v1/voices" \
  -H "xi-api-key: TU_API_KEY"
```

Opción B - Crear voz personalizada:
1. Ve a https://elevenlabs.io/voice-lab
2. Sube audio de entrenamiento (mínimo 1 minuto)
3. Espera el procesamiento
4. Copia el Voice ID

### 2. Configurar local.properties

```properties
# Agregar al final de local.properties
elevenlabs_api_key=sk_xxxxxxxxxxxxxxxxxxxxx
elevenlabs_voice_id=xxxxxxxxxxxxxxxxxxxxxxxx
```

### 3. Recompilar

```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎛️ Perfiles de Voz

El sistema incluye 4 perfiles predefinidos (basados en `jarvis_advanced.py`):

| Perfil | Stability | Similarity | Style | Descripción |
|--------|-----------|------------|-------|-------------|
| **hybrid** (default) | 0.6 | 0.7 | 0.05 | Balanceado - estable pero natural |
| **classic** | 0.5 | 0.75 | 0.0 | Profesional y calmado |
| **conversational** | 0.4 | 0.65 | 0.1 | Natural y cercano |
| **robotic** | 0.8 | 0.8 | 0.0 | Muy estable y consistente |

---

## 🔧 Uso Programático

### Cambiar perfil de voz

```kotlin
// En NovaViewModel
novaViewModel.setVoiceProfile("classic")
```

### Toggle entre TTS engines

```kotlin
// Cambiar entre ElevenLabs y Android TTS
novaViewModel.toggleTtsMode()
```

---

## 💰 Costos

### Free Tier
- 10,000 caracteres/mes gratis
- ~200 interacciones (50 chars promedio)

### Paid Plans
- Starter: $5/mes - 30,000 caracteres
- Creator: $22/mes - 100,000 caracteres
- Pro: $99/mes - 500,000 caracteres

### Estimación de Uso
- Respuesta promedio: 50 caracteres
- 100 interacciones/día = 5,000 chars/día
- **Costo mensual**: ~$4.50 USD (150,000 chars)

---

## 🎯 Características Implementadas

### ✅ Cache Inteligente
- **Memory cache**: Últimas 50 respuestas en RAM
- **Disk cache**: Persistente entre sesiones
- **Auto-limpieza**: Elimina archivos antiguos

### ✅ Streaming (preparado)
```kotlin
// Usar streaming para menor latencia
elevenLabsManager?.speakStreaming(text) { onDone() }
```

### ✅ Fallback Automático
Si ElevenLabs falla o no está configurado, usa Android TTS automáticamente.

### ✅ Multiidioma
Soporta los 29 idiomas de ElevenLabs, incluyendo ES/EN.

---

## 🐛 Troubleshooting

### Error: "API error: 401"
- Verifica que tu API key sea correcta
- Revisa que no haya espacios extra en `local.properties`

### Error: "Voice not found"
- Verifica el Voice ID con:
```bash
curl -X GET "https://api.elevenlabs.io/v1/voices" \
  -H "xi-api-key: TU_API_KEY"
```

### Audio no se reproduce
- Verifica conexión a internet
- Revisa logs: `adb logcat | grep ElevenLabsManager`
- El sistema debería hacer fallback a Android TTS automáticamente

### Latencia alta
- Usa cache (activado por defecto)
- Considera usar `speakStreaming()` en lugar de `speak()`
- Reduce longitud de respuestas de Claude

---

## 📊 Comparación de Rendimiento

| Métrica | Android TTS | ElevenLabs (cache) | ElevenLabs (streaming) |
|---------|-------------|-------------------|----------------------|
| Primera vez | 50-100ms | 500-800ms | 300-500ms |
| Con cache | 50-100ms | 50-100ms | N/A |
| Calidad | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Offline | ✅ | ❌ | ❌ |

---

## 🔄 Migración desde Android TTS

El código es **100% compatible**. Si no configuras ElevenLabs, la app funciona exactamente igual que antes.

**Ventajas del sistema híbrido**:
- ✅ Sin breaking changes
- ✅ Fallback automático
- ✅ Configuración opcional
- ✅ Mismo callback `onDone()`

---

## 🎓 Próximos Pasos

### Fase 1: Básico (actual)
- [x] Integración con cache
- [x] Fallback a Android TTS
- [x] 4 perfiles de voz

### Fase 2: UI (opcional)
- [ ] Toggle en StreamScreen
- [ ] Selector de perfil de voz
- [ ] Indicador de TTS activo

### Fase 3: Optimización (opcional)
- [ ] Streaming real (PCM)
- [ ] Pre-caching de respuestas comunes
- [ ] Compresión de audio

---

## 📚 Referencias

- [ElevenLabs API Docs](https://elevenlabs.io/docs/api-reference)
- [Python SDK (referencia)](https://github.com/elevenlabs/elevenlabs-python)
- [Pricing](https://elevenlabs.io/pricing)
- [Voice Lab](https://elevenlabs.io/voice-lab)

---

## 🤝 Créditos

Basado en la configuración de `jarvis_advanced.py` con perfiles optimizados para asistente tipo JARVIS/Viernes.

**Creado con ❤️ para Meta-Rock**
