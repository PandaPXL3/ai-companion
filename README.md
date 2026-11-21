# Local Companion

Fully local Android AI companion — no cloud, no accounts.

- **Brain:** LFM2.5-1.2B-Instruct on-device via the LEAP SDK
- **Voice:** Kokoro TTS + Moonshine STT via sherpa-onnx (voice pack downloads after install)
- **Avatar:** importable Live2D models, TTS-driven lip-sync, expression tags
- **UI:** native Kotlin + Jetpack Compose, AIRI-style stage layout

The APK ships small; model packs download after install like game asset packs.

CI: GitHub Actions builds the debug APK and runs unit tests on every push.
