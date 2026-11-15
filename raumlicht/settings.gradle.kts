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
rootProject.name = "Raumlicht"

include(":app")            // Oberfläche: Tourplan, 3D-Bühne, Einstellungen
include(":core:model")     // reine Kotlin-Logik: analytische Geometrie, Level (ohne Android)
include(":core:data")      // Fortschritt (Room), Einstellungen
