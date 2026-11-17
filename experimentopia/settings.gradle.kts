pluginManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // Es gelten nur die Adressen hier. (Die Kotlin-Web-Werkzeuge melden zusätzlich eigene an – die werden übergangen statt den Bau abzubrechen.)
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        // Werkzeuge für die WebApp (Kotlin/Wasm): Node.js, Yarn und Binaryen – jeweils nur genau dieses eine Paket von dieser Adresse
        exclusiveContent {
            forRepository { ivy("https://nodejs.org/dist/") { name = "Node.js"; patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }; metadataSources { artifact() } } }
            filter { includeModule("org.nodejs", "node") }
        }
        exclusiveContent {
            forRepository { ivy("https://github.com/yarnpkg/yarn/releases/download") { name = "Yarn"; patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }; metadataSources { artifact() } } }
            filter { includeModule("com.yarnpkg", "yarn") }
        }
        exclusiveContent {
            forRepository { ivy("https://github.com/WebAssembly/binaryen/releases/download") { name = "Binaryen"; patternLayout { artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]") }; metadataSources { artifact() } } }
            filter { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}
rootProject.name = "Experimentopia"

include(":app")            // Android: Start, Ton, Vibration, Updates – die Bildschirme kommen aus :shared
include(":core:model")     // reine Kotlin-Logik: Physik- und Chemie-Modelle, Versuche (Android und Web)
include(":core:data")      // Android: Fortschritt (Room), Forscherbuch, Einstellungen
include(":shared")         // gemeinsame Oberfläche für Android und Web (Compose Multiplatform)
include(":web")            // WebApp (Kotlin/Wasm) – derselbe Code wie die Android-App
