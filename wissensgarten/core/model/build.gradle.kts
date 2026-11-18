import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// Reine Lern-Logik – gemeinsam für Android-App (jvm) und WebApp (wasmJs)
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}
kotlin {
    jvm { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    @OptIn(ExperimentalWasmDsl::class)
    // -Xwasm-no-jstag: kein Import von WebAssembly.JSTag – Safari 26/27 verarbeitet Tag-Importe fehlerhaft (weiße Fläche)
    wasmJs { browser(); compilerOptions { freeCompilerArgs.add("-Xwasm-no-jstag") } }
    // Kotlin/JS-Fassung für Browser ohne passendes WasmGC (Safari 27 auf iPad/iPhone)
    js { browser(); useCommonJs() }
    compilerOptions {
        optIn.add("kotlin.io.encoding.ExperimentalEncodingApi")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            api("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            // „Alle Nova-Texte offline laden“: mehrere Downloads gleichzeitig, abbrechbar
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        // Nova: Sprechtext und Dateiname müssen auf JVM und im Browser (Wasm) gleich sein
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmTest.dependencies { implementation("junit:junit:4.13.2") }
        webMain.dependencies {
            implementation(npm("pako", "2.1.0"))
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
        }
    }
}
