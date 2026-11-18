// Die Rechenlogik gehört beiden Welten: Android (JVM) und der WebApp (Kotlin/Wasm). Hier steht nichts, was nur auf einer Plattform läuft.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}
kotlin {
    jvm { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    js { browser() }   // Ersatz für Browser, die Kotlin/Wasm nicht verarbeiten (Safari 27)
    sourceSets {
        commonMain.dependencies { api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0"); api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") }
        jvmTest.dependencies { implementation("junit:junit:4.13.2") }
    }
}

