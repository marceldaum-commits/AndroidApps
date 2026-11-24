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
    // Kotlin/JS-Fassung: gleicher Code, für Browser, die Kotlin/Wasm nicht verarbeiten (Safari 27 auf iPad/iPhone). Der Lader in index.html wählt.
    js {
        outputModuleName.set("experimentopia-js")
        browser { commonWebpackConfig { outputFileName = "experimentopia-js.js" } }
        binaries.executable()
    }
    sourceSets {
        // Gemeinsamer Web-Code (src/webShared) wird für beide Fassungen übersetzt; nur die Browser-Brücke (Browser.kt) gibt es je Fassung.
        listOf(getByName("wasmJsMain"), getByName("jsMain")).forEach { set ->
            set.kotlin.srcDir("src/webShared/kotlin")
            set.kotlin.srcDir(writeVersion)
            set.dependencies {
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
    from(tasks.named("jsBrowserDistribution")) { include("experimentopia-js.js") }   // JS-Fassung (Safari 27); nutzt dieselbe Skiko-Datei
    into(rootProject.file("webapp"))
    exclude("**/*.LICENSE.txt")
    // Ladeplan eintragen: Größen der großen Dateien für den Fortschrittsbalken und die Namen der .wasm-Dateien für den Service Worker
    doLast {
        val out = rootProject.file("webapp")
        val wasm = out.listFiles { f -> f.name.endsWith(".wasm") }!!.sortedBy { it.name }
        val files = wasm.map { it.name to it.length() } + listOf("fonts/dejavu_sans.ttf", "fonts/noto_color_emoji.ttf", "experimentopia.js", "experimentopia-js.js").map { it to out.resolve(it).length() }
        // Skiko-Grafikkern: die .wasm-Datei, die auch die JS-Fassung lädt
        val jsBundle = out.resolve("experimentopia-js.js").readText()
        val skiko = wasm.map { it.name }.firstOrNull { jsBundle.contains(it) } ?: error("Skiko-Datei der JS-Fassung nicht gefunden")
        val plan = files.joinToString(", ", "{ ", " }") { (n, s) -> "\"$n\": $s" }
        out.resolve("index.html").let { it.writeText(it.readText().replace("@LADEPLAN@", plan).replace("@SKIKO@", skiko)) }
        // Safari 27.2 (weiße Seite, WebKit-Fehler 329421): im fertigen Wasm-Paket darf kein Aufruf der Grafik-Bibliothek mehr als Endaufruf stehen
        // (umgeschrieben von web/webpack/skiko-no-tail-call.cjs – übernommen von Raumlicht 2.18.19)
        for (bundle in listOf("experimentopia.js", "experimentopia-js.js")) {
            val js = out.resolve(bundle).readText()
            val tail = Regex("""=>\(\w+=\w+\._\.org_jetbrains_\w+\)\(""").findAll(js).count()
            val safe = Regex("""try\{return\(\w+=\w+\._\.org_jetbrains_\w+\)\(""").findAll(js).count()
            check(tail == 0) { "Safari-Abhilfe fehlt in $bundle: $tail Endaufrufe übrig, $safe abgesichert" }
            println("Safari-27.2-Abhilfe $bundle: $safe Aufrufe abgesichert, $tail Endaufrufe übrig")
        }
        out.resolve("sw.js").let { it.writeText(it.readText().replace("@WASM@", wasm.joinToString(", ", "[", "]") { "'${it.name}'" })) }
    }
}
