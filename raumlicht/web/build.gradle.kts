// WebApp: dieselbe Rechenlogik (core:model) und dieselbe 3D-Bühne (scene.js) wie die Android-App.
// Bauen:  ./gradlew :web:webapp   → fertige Dateien in raumlicht/webapp/ (so holt sie der Server vom Branch raumlicht-test)
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

// Eine Quelle für die Version: app/build.gradle.kts (dort liest sie auch der Release-Workflow) – Web und Android können nicht auseinanderlaufen
val appGradle = rootProject.file("app/build.gradle.kts").readText()
val versionName = Regex("versionName = \"([^\"]+)\"").find(appGradle)!!.groupValues[1]
val versionDate = Regex("VERSION_DATE.*?(\\d{2}\\.\\d{2}\\.\\d{4})").find(appGradle)!!.groupValues[1]

val versionSource = layout.buildDirectory.dir("generated/version")
val writeVersion by tasks.registering {
    inputs.property("v", versionName); inputs.property("d", versionDate); outputs.dir(versionSource)
    doLast { versionSource.get().file("Version.kt").asFile.apply { parentFile.mkdirs() }.writeText(
        "package de.raumlicht.web\n\n/** Aus app/build.gradle.kts erzeugt – nicht von Hand ändern. */\nobject Version { const val NAME = \"$versionName\"; const val DATE = \"$versionDate\" }\n") }
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("raumlicht")
        browser { commonWebpackConfig { outputFileName = "raumlicht.js" } }
        binaries.executable()
    }
    sourceSets {
        val wasmJsMain by getting {
            kotlin.srcDir(writeVersion)
            dependencies {
                implementation(project(":core:model"))
                implementation(project(":shared"))   // alle Bildschirme und der App-Rahmen – derselbe Code wie in der Android-App
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation(compose.runtime); implementation(compose.foundation); implementation(compose.material3); implementation(compose.ui)
            }
        }
    }
}

// Die 3D-Bühne der Android-App unverändert mitliefern (eine Datei, zwei Welten)
tasks.named<Copy>("wasmJsProcessResources") {
    from(rootProject.file("app/src/main/assets/scene")) { into("scene") }
    // Versionsnummer in Seite, Manifest und Service Worker eintragen (der Service Worker lädt bei neuer Version alles frisch)
    filesMatching(listOf("index.html", "sw.js", "version.json", "diagnose.html")) { filter { it.replace("@VERSION@", versionName).replace("@DATUM@", versionDate) } }
    inputs.property("v", versionName); inputs.property("d", versionDate)
}

/** Fertige WebApp nach raumlicht/webapp/ legen – nur reine Dateien, wie der Server sie ausliefert. */
tasks.register<Sync>("webapp") {
    from(tasks.named("wasmJsBrowserDistribution"))
    into(rootProject.file("webapp"))
    exclude("**/*.LICENSE.txt")
    // Ladeplan eintragen: Größen der großen Dateien für den Fortschrittsbalken und die Namen der .wasm-Dateien für den Service Worker
    doLast {
        val out = rootProject.file("webapp")
        val wasm = out.listFiles { f -> f.name.endsWith(".wasm") }!!.sortedBy { it.name }
        val files = wasm.map { it.name to it.length() } + listOf("fonts/dejavu_sans.ttf", "fonts/noto_color_emoji.ttf", "raumlicht.js").map { it to out.resolve(it).length() }
        val plan = files.joinToString(", ", "{ ", " }") { (n, s) -> "\"$n\": $s" }
        out.resolve("index.html").let { it.writeText(it.readText().replace("@LADEPLAN@", plan)) }
        out.resolve("sw.js").let { it.writeText(it.readText().replace("@WASM@", wasm.joinToString(", ", "[", "]") { "'${it.name}'" })) }
    }
}
