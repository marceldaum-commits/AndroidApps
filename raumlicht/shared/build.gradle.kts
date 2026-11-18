// Gemeinsame Oberfläche: alle Bildschirme von Raumlicht, einmal geschrieben für Android und für die WebApp (Compose Multiplatform).
// Was die Plattformen verschieden lösen (Ton, Vibration, Stimme, Teilen, Uhr, Einbettung der 3D-Bühne), steckt hinter kleinen Schnittstellen
// in ui/fx/Platform.kt und ui/stage/Stage.kt.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}
// Quelltexte der JavaScript-Fassung: aus src/wasmJsMain erzeugt (eingebettete Browser-Schnipsel umgesetzt, sonst unverändert) – siehe tools/webjs/gen.py
val jsQuellen = tasks.register<Exec>("jsQuellen") {
    val out = layout.buildDirectory.dir("generated/jsMain/kotlin")
    inputs.dir("src/wasmJsMain/kotlin"); inputs.file(rootProject.file("tools/webjs/gen.py")); outputs.dir(out)
    commandLine("python3", rootProject.file("tools/webjs/gen.py").path, file("src/wasmJsMain/kotlin").path, out.get().asFile.path)
}
kotlin {
    androidTarget { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser()
        // Safari 26/27 verarbeitet den Import „WebAssembly.JSTag“ fehlerhaft (weiße Seite) – mit diesem Schalter legt Kotlin ein eigenes Ausnahme-Kennzeichen an
        compilerOptions { freeCompilerArgs.add("-Xwasm-no-jstag") } }
    // Zweite Web-Fassung als JavaScript: benutzt dieselben Quelltexte wie die Wasm-Fassung (src/wasmJsMain)
    js { browser() }
    sourceSets {
        val jsMain by getting { kotlin.srcDir(jsQuellen) }
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
