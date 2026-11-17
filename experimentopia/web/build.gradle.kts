import java.util.Properties
// WebApp: dieselbe Rechenlogik (core:model) und dieselben Bildschirme (shared) wie die Android-App.
// Bauen:  ./gradlew :web:webapp   → fertige Dateien in experimentopia/webapp/ (so holt sie der Server vom Branch experimentopia-test)
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
val proProps = Properties().also { p -> val f = rootProject.file("pro/pro.properties"); if (f.exists()) f.inputStream().use { p.load(it) } }
val proKey = proProps.getProperty("publicKey", "").trim()
val proCheckout = proProps.getProperty("checkoutUrl", "").trim()
val writeVersion by tasks.registering {
    inputs.property("v", versionName); inputs.property("d", versionDate); inputs.property("pk", proKey); inputs.property("pc", proCheckout); outputs.dir(versionSource)
    doLast { versionSource.get().file("Version.kt").asFile.apply { parentFile.mkdirs() }.writeText(
        "package de.experimentopia.web\n\n/** Aus app/build.gradle.kts erzeugt – nicht von Hand ändern. */\nobject Version { const val NAME = \"$versionName\"; const val DATE = \"$versionDate\"; const val PRO_KEY = \"$proKey\"; const val PRO_CHECKOUT = \"$proCheckout\" }\n") }
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("experimentopia")
        browser { commonWebpackConfig { outputFileName = "experimentopia.js" } }
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

// Schriften der WebApp automatisch auf die benutzten Zeichen kürzen: läuft vor jedem Bau, aber nur, wenn sich Texte im Quellcode geändert haben.
// Ein neues Emoji oder Sonderzeichen landet so von selbst in der Schrift (sonst erschiene im Browser ein Kästchen).
val schriften = tasks.register<Exec>("schriften") {
    val root = rootProject.projectDir
    inputs.files(fileTree(root) { include("core/model/src/commonMain/**/*.kt", "shared/src/commonMain/**/*.kt", "shared/src/wasmJsMain/**/*.kt", "web/src/wasmJsMain/**/*.kt", "tools/schrift/**") })
    outputs.files(file("src/wasmJsMain/resources/fonts/noto_color_emoji.ttf"), file("src/wasmJsMain/resources/fonts/dejavu_sans.ttf"))
    workingDir = root
    commandLine("python3", "tools/schrift/schriften.py")
    isIgnoreExitValue = true
    doLast { if (executionResult.get().exitValue != 0) throw GradleException("Die Schriften der WebApp ließen sich nicht erzeugen. Nötig: python3 und „pip install fonttools“ (siehe tools/schrift/schriften.py).") }
}

tasks.named<Copy>("wasmJsProcessResources") {
    dependsOn(schriften)
    // Versionsnummer in Seite, Manifest und Service Worker eintragen (der Service Worker lädt bei neuer Version alles frisch)
    filesMatching(listOf("index.html", "sw.js", "version.json")) { filter { it.replace("@VERSION@", versionName).replace("@DATUM@", versionDate) } }
    inputs.property("v", versionName); inputs.property("d", versionDate)
}

/** Fertige WebApp nach experimentopia/webapp/ legen – nur reine Dateien, wie der Server sie ausliefert. */
tasks.register<Sync>("webapp") {
    from(tasks.named("wasmJsBrowserDistribution"))
    into(rootProject.file("webapp"))
    exclude("**/*.LICENSE.txt")
    // Ladeplan eintragen: Größen der großen Dateien für den Fortschrittsbalken und die Namen der .wasm-Dateien für den Service Worker
    doLast {
        val out = rootProject.file("webapp")
        val wasm = out.listFiles { f -> f.name.endsWith(".wasm") }!!.sortedBy { it.name }
        val files = wasm.map { it.name to it.length() } + listOf("fonts/dejavu_sans.ttf", "fonts/noto_color_emoji.ttf", "experimentopia.js").map { it to out.resolve(it).length() }
        val plan = files.joinToString(", ", "{ ", " }") { (n, s) -> "\"$n\": $s" }
        out.resolve("index.html").let { it.writeText(it.readText().replace("@LADEPLAN@", plan)) }
        out.resolve("sw.js").let { it.writeText(it.readText().replace("@WASM@", wasm.joinToString(", ", "[", "]") { "'${it.name}'" })) }
    }
}
