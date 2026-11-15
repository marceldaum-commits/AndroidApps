import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// Gemeinsame Oberfläche (Compose Multiplatform): dieselben Bildschirme, Spiele und Logik für Android-App und WebApp.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Eine Versionsnummer für beide: aus app/build.gradle.kts gelesen und als AppInfo in den Code geschrieben
val appGradle = rootProject.file("app/build.gradle.kts").readText()
val versionName = Regex("""versionName = "([^"]+)"""").find(appGradle)!!.groupValues[1]
val versionDate = Regex("""VERSION_DATE", "\\"([0-9.]+)\\""""").find(appGradle)!!.groupValues[1]
val genDir = layout.buildDirectory.dir("generated/appinfo/kotlin")
val genAppInfo by tasks.registering {
    inputs.property("v", versionName); inputs.property("d", versionDate)
    outputs.dir(genDir)
    doLast {
        val f = genDir.get().file("de/wissensgarten/app/AppInfo.kt").asFile
        f.parentFile.mkdirs()
        f.writeText("package de.wissensgarten.app\n\n/** Automatisch erzeugt aus app/build.gradle.kts – gleiche Version für App und WebApp. */\n" +
            "object AppInfo {\n    const val VERSION = \"$versionName\"\n    const val DATE = \"$versionDate\"\n}\n")
    }
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.addAll("androidx.compose.material3.ExperimentalMaterial3Api", "androidx.compose.foundation.layout.ExperimentalLayoutApi")
    }
    sourceSets {
        commonMain {
            kotlin.srcDir(genDir)
            dependencies {
                api(project(":core:data"))
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material3)
                api(compose.ui)
                api(compose.animation)
                api(compose.components.resources)
                api(compose.materialIconsExtended)
                api("org.jetbrains.androidx.navigation:navigation-compose:2.9.1")
                api("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.6")
                api("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.6")
                api("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.6")
            }
        }
        androidMain.dependencies {
            api("androidx.activity:activity-compose:1.11.0")
            implementation("com.google.zxing:core:3.5.4")
        }
        wasmJsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            implementation(npm("qrcode-generator", "1.4.4"))
        }
    }
}
tasks.matching { it.name.startsWith("compile") || it.name.contains("Sources") || it.name.contains("ksp", ignoreCase = true) }
    .configureEach { if (this != genAppInfo.get()) dependsOn(genAppInfo) }

android {
    namespace = "de.wissensgarten.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
