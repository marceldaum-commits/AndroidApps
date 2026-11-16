// Gemeinsame Oberfläche: alle Bildschirme von Raumlicht, einmal geschrieben für Android und für die WebApp (Compose Multiplatform).
// Was die Plattformen verschieden lösen (Ton, Vibration, Stimme, Teilen, Uhr, Einbettung der 3D-Bühne), steckt hinter kleinen Schnittstellen
// in ui/fx/Platform.kt und ui/stage/Stage.kt.
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
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(compose.runtime); implementation(compose.foundation); implementation(compose.material3); implementation(compose.ui)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        androidMain.dependencies { implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4") }
    }
}
android {
    namespace = "de.raumlicht.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
