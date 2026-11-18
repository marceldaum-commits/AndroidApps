// Gemeinsame Oberfläche: alle Bildschirme und Labore von Experimentopia, einmal geschrieben für Android und für die WebApp (Compose Multiplatform).
// Was die Plattformen verschieden lösen (Ton, Vibration, Speicher, Updates), steckt hinter kleinen Schnittstellen in ui/fx/Platform.kt und core/model/Store.kt.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}
kotlin {
    androidTarget { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    js { browser() }   // Ersatz für Browser, die Kotlin/Wasm nicht verarbeiten (Safari 27)
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(compose.runtime); implementation(compose.foundation); implementation(compose.material3); implementation(compose.ui)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
    }
}
android {
    namespace = "de.experimentopia.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
