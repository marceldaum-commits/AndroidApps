plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Reine Spiellogik – läuft gleich auf Android (JVM) und im Browser (Kotlin/Wasm).
kotlin {
    jvm { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser(); nodejs(); compilerOptions { freeCompilerArgs.add("-Xwasm-no-jstag") } }   // Safari 26/27: kann den Import „intrinsics.tag“ (WebAssembly.JSTag) nicht – Leitstand 18.11.
    // Kompatibilitäts-Fassung für Browser ohne WebAssembly-GC (ältere iPhones/iPads): Kotlin/JS
    js { browser { testTask { enabled = false } }; nodejs { testTask { useMocha { timeout = "60s" } } } }
    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmTest.dependencies { implementation("junit:junit:4.13.2") }
    }
}

/** Liste der festen Vorlese-Texte für die KI-Stimme Nova (build/vorlesen/texte.json) – die WebApp liefert sie unter vorlesen/texte.json aus. */
val vorlesenTexte by tasks.registering(JavaExec::class) {
    val jvmMain = kotlin.jvm().compilations.getByName("main")
    dependsOn(jvmMain.compileTaskProvider)
    classpath = files(jvmMain.output.allOutputs, jvmMain.runtimeDependencyFiles)
    mainClass.set("de.kurvenflug.core.model.VorlesenListeKt")
    val out = layout.buildDirectory.file("vorlesen-root/vorlesen/texte.json")
    val ui = rootProject.file("shared/src/commonMain/kotlin")
    inputs.dir(ui); outputs.file(out)
    args(out.get().asFile.path, ui.path)
}
