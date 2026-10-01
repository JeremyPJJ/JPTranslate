# JP Translator - Offline Spanish ⇄ English Translation App Implementation Plan

An Android application built with **Kotlin** and **Jetpack Compose**, implementing Clean Architecture, local offline translation (using ML Kit On-Device Translation with local model management), Text-to-Speech, Speech-to-Text, Translation History, and Favorites.

## User Review Required

> [!IMPORTANT]
> - **Translation Engine**: We will use **Google ML Kit On-Device Translation** (`com.google.mlkit:translate`), which downloads language models (Spanish & English) locally to the device and runs 100% offline without requiring internet during translation.
> - **Voice & TTS**: Android's built-in `TextToSpeech` and `SpeechRecognizer` will be used, with clear UI indicators for offline capabilities and limitations.
> - **Persistence**: Room database / lightweight local storage will be used for saving Translation History and Favorites.

## Open Questions

- None. The feature set matches all user specifications.

## Proposed Changes

### Dependencies & Setup
#### [MODIFY] [libs.versions.toml](file:///C:/Users/Paredes/StudioProjects/Traductor/gradle/libs.versions.toml)
- Add ML Kit translation, Room, and Material Icons Extended dependencies.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Paredes/StudioProjects/Traductor/app/build.gradle.kts)
- Include ML Kit translation and UI dependencies.

### Domain Layer (`domain/`)
#### [NEW] [Language.kt](file:///C:/Users/Paredes/StudioProjects/Traductor/app/src/main/java/com/example/traductor/domain/model/Language.kt)
- Define source and target languages (Spanish 🇪🇸, English 🇺🇸).
#### [NEW] [TranslationItem.kt](file:///C:/Users/Paredes/StudioProjects/Traductor/app/src/main/java/com/example/traductor/domain/model/TranslationItem.kt)
- Data model for translation entries, history, and favorites.

### Data & Translation Layer (`data/`, `translation/`, `voice/`)
#### [NEW] [TranslationDataSource.kt](file:///C:/Users/Paredes/StudioProjects/Traductor/app/src/main/java/com/example/traductor/data/translation/TranslationDataSource.kt)
- Manages ML Kit model downloading (`downloadModelIfNeeded`) and local offline translation execution.
#### [NEW] [HistoryRepository.kt](file:///C:/Users/Paredes/StudioProjects/Traductor/app/src/main/java/com/example/traductor/data/history/HistoryRepository.kt)
- Local storage for history and favorites (using Room or local JSON/Preferences).
#### [NEW] [TextToSpeechHelper.kt](file:///C:/Users/Paredes/StudioProjects/Traductor/app/src/main/java/com/example/traductor/voice/TextToSpeechHelper.kt)
- Manages offline text-to-speech playback for Spanish and English.
#### [NEW] [SpeechRecognizerHelper.kt](file:///C:/Users/Paredes/StudioProjects/Traductor/app/src/main/java/com/example/traductor/voice/SpeechRecognizerHelper.kt)
- Manages voice-to-text input.

### UI & ViewModel Layer (`ui/`)
#### [NEW] [TranslationViewModel.kt](file:///C:/Users/Paredes/StudioProjects/Traductor/app/src/main/java/com/example/traductor/ui/TranslationViewModel.kt)
- StateFlow-based ViewModel managing UI state, source/target languages, input text, translated text, model download status, history, and favorites.
#### [NEW] [TranslationScreen.kt](file:///C:/Users/Paredes/StudioProjects/Traductor/app/src/main/java/com/example/traductor/ui/TranslationScreen.kt)
- Main translation screen with language selector, input box, translate button, output box, speech, TTS, copy, clear, history, and favorites.
#### [MODIFY] [MainActivity.kt](file:///C:/Users/Paredes/StudioProjects/Traductor/app/src/main/java/com/example/traductor/MainActivity.kt)
- Wire up `TranslationScreen` with theme.

## Verification Plan

### Automated Tests
- Build project using Gradle `app:assembleDebug` to verify no compilation errors.

### Manual Verification
- Deploy app to device/emulator.
- Download offline models (Spanish/English).
- Test translation in Airplane Mode ✈️.
- Test Speech, Text-to-Speech, History, and Favorites.
