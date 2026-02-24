# 🤖 Meta-Rock — AWS Bedrock × Meta Glasses

<details>
<summary>🇬🇧 English</summary>

> **Voice-activated AI assistant integrating Meta Ray-Ban smart glasses with AWS Bedrock**

---

## 📖 Description

**Meta-Rock** is an Android app that integrates Meta Ray-Ban smart glasses with AWS Bedrock (Claude Sonnet 4.5) to create **Friday** — a voice-activated AI assistant inspired by F.R.I.D.A.Y. from Iron Man.

Say **"Hey Friday"**, ask a question, and the app sends text + camera frame to Claude for analysis. Then it displays and reads the response aloud.

---

## ✨ Features

### 🎙️ Voice Assistant — Friday
- Wake word: `"Hey Friday"` / `"Oye Viernes"`
- Real-time speech recognition
- Text-to-Speech (TTS) responses

### 📹 Camera Streaming
- Live stream from Meta Ray-Ban in **high quality (720p, 30fps)**
- 85% JPEG compression for maximum sharpness

### 🧠 AI Vision
- Always sends camera frame + text to Claude for full context
- Model: `us.anthropic.claude-sonnet-4-5-20250929-v1:0` (cross-region inference)

### 🎯 Two Activation Modes
- **Voice**: responds and goes back to passive (need to say "Hey Friday" again)
- **Mic button**: continuous conversation until pressed again

### 🌍 Bilingual
- Spanish (Latin American) / English
- Real-time language switching

### 🎨 Modern UI
- Animated code rain background
- Glassmorphism card design
- Adaptive launcher icon
- Material Design 3

---

## 🏗️ Architecture

```
┌─────────────────────┐   Bluetooth   ┌──────────────────────┐   HTTPS API   ┌──────────────────────┐
│  Meta Ray-Ban       │ ────────────► │   Meta-Rock          │ ────────────► │   AWS Bedrock        │
│  Smart Glasses      │               │   (Android App)      │               │   (Claude Sonnet 4.5)│
└─────────────────────┘               └──────────────────────┘               └──────────────────────┘
                                                  │
                                      ┌───────────┴───────────┐
                                      │  SpeechManager        │  Wake word + STT + TTS
                                      │  BedrockClient        │  Converse API
                                      │  NovaViewModel        │  Orchestrator
                                      │  StreamViewModel      │  Camera + frames
                                      └───────────────────────┘
```

---

## 🛠️ Stack

| Technology | Version | Purpose |
|---|---|---|
| Kotlin | 2.3.0 | Main language |
| Jetpack Compose | BOM latest | Reactive UI |
| Meta Wearables DAT SDK | 0.4.0 | Glasses integration |
| AWS SDK for Kotlin | bedrockruntime 1.6.12 | Bedrock client |
| Material 3 | Latest | Design system |

---

## 📋 Requirements

- **Meta Ray-Ban Smart Glasses** (firmware v20+)
- **Android 12+ phone** (API 31+)
- **Meta AI app** (v254+) installed and paired with glasses
- **AWS account** with Bedrock access (Claude Sonnet)
- **GitHub token** with `read:packages` scope (to download Meta SDK)

---

## 🚀 Setup

### 1. Clone

```bash
git clone https://github.com/fernandosilvot/aws-bedrock-meta-glasses.git
cd aws-bedrock-meta-glasses
```

### 2. Configure credentials

Create `local.properties` in the root:

```properties
sdk.dir=/Users/YOUR_USER/Library/Android/sdk
github_token=YOUR_GITHUB_TOKEN
aws_access_key=YOUR_AWS_ACCESS_KEY
aws_secret_key=YOUR_AWS_SECRET_KEY
aws_region=us-east-1
meta_application_id=YOUR_META_APP_ID
```

### 3. Build & install

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Usage

1. Open **Meta-Rock** on your phone
2. Tap **"Connect my glasses"** → authorize in Meta AI app
3. Tap **"Start streaming"** → authorize camera access
4. Say **"Hey Friday"** or tap the 🎤 button
5. Ask your question → get the AI response

### Example phrases

| English | Español |
|---|---|
| "What do you see?" | "¿Qué ves?" |
| "What time is it?" | "¿Qué hora es?" |
| "Describe what's in front of me" | "Describe lo que hay frente a mí" |
| "Tell me a joke" | "Cuéntame un chiste" |

---

## 📁 Structure

```
app/src/main/java/.../cameraaccess/
├── nova/                        # 🤖 AI Assistant (Friday)
│   ├── NovaViewModel.kt        # Flow orchestrator
│   ├── SpeechManager.kt        # Wake word + STT + TTS
│   ├── BedrockClient.kt        # AWS Bedrock client (Converse API)
│   ├── IntentClassifier.kt     # Intent classification
│   ├── Prompts.kt              # System prompts EN/ES
│   └── NovaUiState.kt          # States: IDLE → LISTENING → PROCESSING → RESPONDING
├── stream/                      # 📹 Camera streaming
│   ├── StreamViewModel.kt      # Stream session + I420→NV21 conversion
│   └── StreamUiState.kt        # Stream state
├── wearables/                   # 🥽 Device management
│   ├── WearablesViewModel.kt   # Connection and registration
│   └── WearablesUiState.kt     # State + AppLanguage enum
├── ui/                          # 🎨 Interface
│   ├── NonStreamScreen.kt      # Main screen (glassmorphism + code rain)
│   ├── StreamScreen.kt         # Streaming screen + mic button
│   ├── NovaOverlay.kt          # Animated Friday overlay
│   ├── CameraAccessScaffold.kt # Scaffold with language picker
│   ├── LocalizedString.kt      # Localization helper
│   ├── LanguageButton.kt       # Language button
│   ├── SwitchButton.kt         # Main button
│   └── AppColor.kt             # Color palette
└── MainActivity.kt             # Entry point + permissions
```

---

## 📄 License

Based on the [Meta Wearables DAT SDK Sample](https://github.com/facebook/meta-wearables-dat-android).
Subject to [Meta Wearables Developer Terms](https://wearables.developer.meta.com/terms).

---

## 👨‍💻 Credits

**Created by** [Fernando Silva T.](https://fernandosilvot.cl/en)

Made with ❤️ in Chile 🇨🇱

</details>

<details open>
<summary>🇪🇸 Español</summary>

> **Asistente de IA activado por voz que integra lentes inteligentes Meta Ray-Ban con AWS Bedrock**

---

## 📖 Descripción

**Meta-Rock** es una app Android que integra los lentes inteligentes Meta Ray-Ban con AWS Bedrock (Claude Sonnet 4.5) para crear **Viernes** — un asistente de IA activado por voz inspirado en F.R.I.D.A.Y. de Iron Man.

Dices **"Oye Viernes"**, haces una pregunta, y la app envía texto + imagen de la cámara a Claude para análisis. Luego muestra y lee la respuesta en voz alta.

---

## ✨ Características

### 🎙️ Asistente de Voz — Viernes
- Palabra de activación: `"Oye Viernes"` / `"Hey Friday"`
- Reconocimiento de voz en tiempo real
- Respuestas por Text-to-Speech (TTS)

### 📹 Streaming de Cámara
- Transmisión en vivo desde Meta Ray-Ban en **alta calidad (720p, 30fps)**
- Compresión JPEG al 85% para máxima nitidez

### 🧠 IA con Visión
- Siempre envía frame de la cámara + texto a Claude para contexto completo
- Modelo: `us.anthropic.claude-sonnet-4-5-20250929-v1:0` (cross-region inference)

### 🎯 Dos Modos de Activación
- **Voz**: responde y vuelve a pasivo (hay que decir "Oye Viernes" de nuevo)
- **Botón micrófono**: conversación continua hasta que se pulse de nuevo

### 🌍 Bilingüe
- Español (Latinoamericano) / Inglés
- Cambio de idioma en tiempo real

### 🎨 UI Moderna
- Fondo animado con lluvia de código
- Tarjeta central con efecto glassmorphism
- Ícono adaptativo
- Material Design 3

---

## 🏗️ Arquitectura

```
┌─────────────────────┐   Bluetooth   ┌──────────────────────┐   HTTPS API   ┌──────────────────────┐
│  Meta Ray-Ban       │ ────────────► │   Meta-Rock          │ ────────────► │   AWS Bedrock        │
│  Smart Glasses      │               │   (Android App)      │               │   (Claude Sonnet 4.5)│
└─────────────────────┘               └──────────────────────┘               └──────────────────────┘
                                                  │
                                      ┌───────────┴───────────┐
                                      │  SpeechManager        │  Wake word + STT + TTS
                                      │  BedrockClient        │  Converse API
                                      │  NovaViewModel        │  Orquestador
                                      │  StreamViewModel      │  Cámara + frames
                                      └───────────────────────┘
```

---

## 🛠️ Stack

| Tecnología | Versión | Uso |
|---|---|---|
| Kotlin | 2.3.0 | Lenguaje principal |
| Jetpack Compose | BOM latest | UI reactiva |
| Meta Wearables DAT SDK | 0.4.0 | Integración con lentes |
| AWS SDK for Kotlin | bedrockruntime 1.6.12 | Cliente Bedrock |
| Material 3 | Latest | Sistema de diseño |

---

## 📋 Requisitos

- **Meta Ray-Ban Smart Glasses** (firmware v20+)
- **Teléfono Android 12+** (API 31+)
- **Meta AI app** (v254+) instalada y con lentes emparejados
- **Cuenta AWS** con acceso a Bedrock (Claude Sonnet)
- **Token GitHub** con scope `read:packages` (para descargar Meta SDK)

---

## 🚀 Instalación

### 1. Clonar

```bash
git clone https://github.com/fernandosilvot/aws-bedrock-meta-glasses.git
cd aws-bedrock-meta-glasses
```

### 2. Configurar credenciales

Crear `local.properties` en la raíz:

```properties
sdk.dir=/Users/TU_USUARIO/Library/Android/sdk
github_token=TU_TOKEN_GITHUB
aws_access_key=TU_AWS_ACCESS_KEY
aws_secret_key=TU_AWS_SECRET_KEY
aws_region=us-east-1
meta_application_id=TU_META_APP_ID
```

### 3. Compilar e instalar

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Uso

1. Abre **Meta-Rock** en tu teléfono
2. Toca **"Conectar mis lentes"** → autoriza en Meta AI app
3. Toca **"Iniciar streaming"** → autoriza acceso a cámara
4. Di **"Oye Viernes"** o toca el botón 🎤
5. Haz tu pregunta → recibe la respuesta de la IA

### Frases de ejemplo

| Español | English |
|---|---|
| "¿Qué ves?" | "What do you see?" |
| "¿Qué hora es?" | "What time is it?" |
| "Describe lo que hay frente a mí" | "Describe what's in front of me" |
| "Cuéntame un chiste" | "Tell me a joke" |

---

## 📁 Estructura

```
app/src/main/java/.../cameraaccess/
├── nova/                        # 🤖 Asistente de IA (Viernes)
│   ├── NovaViewModel.kt        # Orquestador del flujo
│   ├── SpeechManager.kt        # Wake word + STT + TTS
│   ├── BedrockClient.kt        # Cliente AWS Bedrock (Converse API)
│   ├── IntentClassifier.kt     # Clasificación de intenciones
│   ├── Prompts.kt              # System prompts ES/EN
│   └── NovaUiState.kt          # Estados: IDLE → LISTENING → PROCESSING → RESPONDING
├── stream/                      # 📹 Streaming de cámara
│   ├── StreamViewModel.kt      # Sesión de streaming + conversión I420→NV21
│   └── StreamUiState.kt        # Estado del stream
├── wearables/                   # 🥽 Gestión de dispositivos
│   ├── WearablesViewModel.kt   # Conexión y registro de lentes
│   └── WearablesUiState.kt     # Estado + AppLanguage enum
├── ui/                          # 🎨 Interfaz
│   ├── NonStreamScreen.kt      # Pantalla principal (glassmorphism + code rain)
│   ├── StreamScreen.kt         # Pantalla de streaming + botón mic
│   ├── NovaOverlay.kt          # Overlay animado de Viernes
│   ├── CameraAccessScaffold.kt # Scaffold con language picker
│   ├── LocalizedString.kt      # Helper de localización
│   ├── LanguageButton.kt       # Botón de idioma
│   ├── SwitchButton.kt         # Botón principal
│   └── AppColor.kt             # Paleta de colores
└── MainActivity.kt             # Entry point + permisos
```

---

## 📄 Licencia

Basado en el [Meta Wearables DAT SDK Sample](https://github.com/facebook/meta-wearables-dat-android).
Sujeto a los [Meta Wearables Developer Terms](https://wearables.developer.meta.com/terms).

---

## 👨‍💻 Créditos

**Creado por** [Fernando Silva T.](https://fernandosilvot.cl)

Made with ❤️ in Chile 🇨🇱

</details>
