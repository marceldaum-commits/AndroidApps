import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// Daten: gemeinsame Lern-Logik (Repository, Einstellungen) für Android und Web.
// Android speichert in Room (Schema unverändert seit Version 3), das Web im Browser.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("androidx.room")
}
kotlin {
    androidTarget { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    // Kotlin/JS-Fassung für Browser ohne passendes WasmGC (Safari 27 auf iPad/iPhone)
    js { browser(); useCommonJs() }
    compilerOptions { optIn.add("kotlin.uuid.ExperimentalUuidApi") }
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        androidMain.dependencies {
            api("androidx.room:room-runtime:2.8.4")
            implementation("androidx.room:room-ktx:2.8.4")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
        }
        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.robolectric:robolectric:4.17")
                implementation("androidx.test:core:1.7.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
                implementation(project(":content"))
            }
        }
        webMain.dependencies { implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0") }
    }
}
android {
    namespace = "de.wissensgarten.core.data"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
room { schemaDirectory("$projectDir/schemas") }
dependencies { add("kspAndroid", "androidx.room:room-compiler:2.8.4") }
