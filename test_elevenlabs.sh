#!/bin/bash

# Script de prueba rápida para ElevenLabs
# Uso: ./test_elevenlabs.sh

echo "🧪 Test de ElevenLabs API"
echo "=========================="
echo ""

# Leer credenciales de local.properties
if [ ! -f "local.properties" ]; then
    echo "❌ Error: local.properties no encontrado"
    exit 1
fi

API_KEY=$(grep "elevenlabs_api_key" local.properties | cut -d'=' -f2)
VOICE_ID=$(grep "elevenlabs_voice_id" local.properties | cut -d'=' -f2)

if [ -z "$API_KEY" ] || [ -z "$VOICE_ID" ]; then
    echo "❌ Error: Credenciales no configuradas en local.properties"
    echo ""
    echo "Agrega estas líneas:"
    echo "  elevenlabs_api_key=tu_api_key"
    echo "  elevenlabs_voice_id=tu_voice_id"
    exit 1
fi

echo "✅ Credenciales encontradas"
echo "   API Key: ${API_KEY:0:10}..."
echo "   Voice ID: ${VOICE_ID:0:10}..."
echo ""

# Test 1: Listar voces
echo "📋 Test 1: Listar voces disponibles"
echo "-----------------------------------"
curl -s -X GET "https://api.elevenlabs.io/v1/voices" \
  -H "xi-api-key: $API_KEY" | jq -r '.voices[] | "\(.name) - \(.voice_id)"' 2>/dev/null

if [ $? -ne 0 ]; then
    echo "⚠️  jq no instalado, mostrando respuesta raw:"
    curl -s -X GET "https://api.elevenlabs.io/v1/voices" \
      -H "xi-api-key: $API_KEY"
fi
echo ""

# Test 2: Generar audio de prueba
echo "🎙️  Test 2: Generar audio de prueba"
echo "-----------------------------------"
echo "Texto: 'Buenas tardes, señor. Todos los sistemas están operativos.'"

curl -s -X POST "https://api.elevenlabs.io/v1/text-to-speech/$VOICE_ID" \
  -H "xi-api-key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Buenas tardes, señor. Todos los sistemas están operativos.",
    "model_id": "eleven_multilingual_v2",
    "voice_settings": {
      "stability": 0.6,
      "similarity_boost": 0.7,
      "style": 0.05,
      "use_speaker_boost": true
    }
  }' \
  --output test_elevenlabs_output.mp3

if [ -f "test_elevenlabs_output.mp3" ]; then
    SIZE=$(ls -lh test_elevenlabs_output.mp3 | awk '{print $5}')
    echo "✅ Audio generado: test_elevenlabs_output.mp3 ($SIZE)"
    echo ""
    echo "🔊 Reproducir audio:"
    echo "   macOS: afplay test_elevenlabs_output.mp3"
    echo "   Linux: mpg123 test_elevenlabs_output.mp3"
    echo "   Windows: start test_elevenlabs_output.mp3"
else
    echo "❌ Error al generar audio"
fi

echo ""
echo "✅ Tests completados"
