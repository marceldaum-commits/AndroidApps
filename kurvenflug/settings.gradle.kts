pluginManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}
rootProject.name = "Kurvenflug"

include(":app")            // Oberfläche: Weltenkarte, Spiele, Einstellungen
include(":core:model")     // reine Kotlin-Logik: Flugbahnen, Physik, Level (ohne Android)
include(":core:data")      // Fortschritt (Room), Einstellungen
