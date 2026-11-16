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
rootProject.name = "Experimentopia"

include(":app")            // Oberfläche: Inselkarte, Labore, Forscherbuch
include(":core:model")     // reine Kotlin-Logik: Physik- und Chemie-Modelle, Versuche (ohne Android)
include(":core:data")      // Fortschritt (Room), Forscherbuch, Einstellungen
