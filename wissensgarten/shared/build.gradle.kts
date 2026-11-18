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
// Wissensgarten Plus: öffentlicher Schlüssel und Kassen-URLs (leer = noch keine Sperren)
val plusFile = rootProject.file("pro/plus.properties")
val plusProps: Map<String, String> = if (!plusFile.exists()) emptyMap() else plusFile.readLines()
    .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") && "=" in it }
    .associate { it.substringBefore("=").trim() to it.substringAfter("=").trim() }
fun plusProp(k: String) = plusProps[k] ?: ""
val genDir = layout.buildDirectory.dir("generated/appinfo/kotlin")
val genAppInfo by tasks.registering {
    inputs.property("v", versionName); inputs.property("d", versionDate)
    inputs.property("pk", plusProp("publicKey")); inputs.property("cy", plusProp("checkoutUrlYear")); inputs.property("cf", plusProp("checkoutUrlForever"))
    outputs.dir(genDir)
    doLast {
        val f = genDir.get().file("de/wissensgarten/app/AppInfo.kt").asFile
        f.parentFile.mkdirs()
        f.writeText("package de.wissensgarten.app\n\n/** Automatisch erzeugt aus app/build.gradle.kts – gleiche Version für App und WebApp. */\n" +
            "object AppInfo {\n    const val VERSION = \"$versionName\"\n    const val DATE = \"$versionDate\"\n" +
            "    const val PLUS_KEY = \"${plusProp("publicKey")}\"\n    const val PLUS_CHECKOUT_YEAR = \"${plusProp("checkoutUrlYear")}\"\n" +
            "    const val PLUS_CHECKOUT_FOREVER = \"${plusProp("checkoutUrlForever")}\"\n}\n")
    }
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    // Kotlin/JS-Fassung für Browser ohne passendes WasmGC (Safari 27 auf iPad/iPhone)
    js { browser(); useCommonJs() }
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
        webMain.dependencies {
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
