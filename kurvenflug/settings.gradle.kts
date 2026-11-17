pluginManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // Kotlin/Wasm holt Node.js und Binaryen über eigene Quellen – daher Projekt-Quellen erlauben
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        // Natürliche Stimme „Thorsten“ (sherpa-onnx, Apache-2.0) – nur diese eine Bibliothek von JitPack
        maven("https://jitpack.io") { content { includeGroup("com.github.k2-fsa.sherpa-onnx") } }
    }
}
rootProject.name = "Kurvenflug"

include(":app")            // Oberfläche: Weltenkarte, Spiele, Einstellungen
include(":core:model")     // reine Kotlin-Logik: Flugbahnen, Physik, Level (ohne Android)
include(":core:data")      // Fortschritt (Room) und Einstellungen auf Android
include(":shared")          // gemeinsame Oberfläche für Android und Browser
include(":web")             // WebApp (Kotlin/Wasm)
