plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.2.21" apply false
    id("org.jetbrains.compose") version "1.9.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
    id("androidx.room") version "2.8.4" apply false
}

// Kotlin/Wasm: Die npm-Sperrdatei hängt davon ab, welche Module gebaut werden (nur Tests vs. WebApp) –
// sie wird dann nachgezogen statt den Build abzubrechen.
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension>().apply {
        packageLockMismatchReport.set(org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport.NONE)
        packageLockAutoReplace.set(true)
    }
}
