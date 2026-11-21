pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // sherpa-onnx publishes its Android AAR through JitPack, not Maven Central
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "LocalCompanion"
include(":app")
