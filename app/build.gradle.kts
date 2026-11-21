plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ai.localcompanion.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.localcompanion.app"
        minSdk = 31 // LEAP SDK requires API 31+
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    // tar.bz2 extraction for model packs (pure Java, no native deps)
    implementation("org.apache.commons:commons-compress:1.27.1")

    // On-device LLM: Liquid AI LEAP SDK (Maven Central, no token needed)
    implementation("ai.liquid.leap:leap-sdk:0.10.9")
    implementation("ai.liquid.leap:leap-model-downloader:0.10.9")

    // Local TTS (Kokoro) + STT (Moonshine) + VAD: sherpa-onnx via JitPack
    implementation("com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.5")

    testImplementation("junit:junit:4.13.2")
}
