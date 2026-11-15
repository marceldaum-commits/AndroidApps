pluginManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // PREFER_PROJECT: Kotlin/Wasm legt für Node.js eigene Quellen im Wurzelprojekt an
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}
rootProject.name = "Wissensgarten"

include(":app")            // Oberfläche, Navigation, Widget
include(":core:model")     // reine Kotlin-Logik: Vergessenskurve, Pakete, Runden (ohne Android)
include(":core:data")      // Datenbank, Profile, Paket-Installation, Freischaltungen
include(":shared")         // gemeinsame Oberfläche für Android-App und WebApp (Compose Multiplatform)
include(":webApp")         // WebApp (Kotlin/Wasm) – gleiche Oberfläche wie die App
include(":content")        // Wissenspakete als JSON (austauschbar / später nachladbar)
