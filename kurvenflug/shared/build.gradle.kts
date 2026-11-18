plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

// Gemeinsame Oberfläche: dieselben Bildschirme auf Android und im Browser (Compose Multiplatform).
kotlin {
    androidTarget { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    js { browser { testTask { enabled = false } } }   // Kompatibilitäts-Fassung (Kotlin/JS)
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api("org.jetbrains.compose.material:material-icons-core:1.7.3")
            api("org.jetbrains.androidx.navigation:navigation-compose:2.9.1")
            api("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.6")
        }
        androidMain.dependencies {
            api(project(":core:data"))
            implementation("androidx.activity:activity-compose:1.11.0")
            implementation("androidx.core:core-ktx:1.17.0")
        }
    }
}

android {
    namespace = "de.kurvenflug.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
