# 🔍 Tavily Web Search Integration

## Configuración

1. **Obtén tu API Key gratuita:**
   - Ve a https://app.tavily.com
   - Crea una cuenta
   - Copia tu API key

2. **Agrega la key en el código:**
   ```kotlin
   // En NovaViewModel.kt línea ~38
   private val tavilyApiKey = "tvly-TU_API_KEY_AQUI"
   ```

3. **Límites gratuitos:**
   - 1,000 créditos/mes gratis
   - 1 búsqueda básica = 1 crédito
   - Suficiente para ~30 búsquedas/día

## Uso

J.A.R.V.I.S. automáticamente buscará en web cuando detecte:
- "busca..."
- "qué es..."
- "quién es..."
- "información sobre..."
- "dime sobre..."

## Ejemplos

```
"Jarvis, busca información sobre AWS Bedrock"
"Jarvis, qué es Meta Ray-Ban"
"Jarvis, quién es el CEO de Meta"
"Jarvis, dime sobre inteligencia artificial"
```

## Cómo Funciona

1. Usuario hace pregunta con palabra clave
2. Tavily busca en web (3 resultados)
3. Claude recibe contexto actualizado
4. Responde con información web + su conocimiento
5. ElevenLabs habla la respuesta

## Logs

Para verificar que funciona:
```bash
adb logcat | grep -E "(TavilySearch|Tavily)"
```

Deberías ver:
```
TavilySearch: Searching Tavily for: ...
TavilySearch: Response received: ...
NovaViewModel: Tavily search completed: 3 results
```

## Alternativas

Si no quieres usar Tavily, simplemente deja `tavilyApiKey = ""` vacío.
J.A.R.V.I.S. funcionará solo con el conocimiento de Claude.
