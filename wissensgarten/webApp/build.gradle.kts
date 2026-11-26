import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// WebApp: dieselbe Oberfläche wie die Android-App (Modul :shared), gebaut für den Browser (Kotlin/Wasm).
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("wissensgarten")
        // kein Import von WebAssembly.JSTag – Safari 26/27 verarbeitet Tag-Importe fehlerhaft (weiße Fläche)
        compilerOptions { freeCompilerArgs.add("-Xwasm-no-jstag") }
        browser { commonWebpackConfig { outputFileName = "wissensgarten.js" } }
        binaries.executable()
    }
    // Kotlin/JS-Fassung: dieselbe App ohne WasmGC – der Lader in index.html wählt sie, wenn Wasm nicht klappt (Safari 27)
    js {
        outputModuleName.set("wissensgarten-js")
        useCommonJs()
        browser { commonWebpackConfig { outputFileName = "wissensgarten-js.js" } }
        binaries.executable()
    }
    sourceSets {
        webMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
        }
    }
}
compose.resources { packageOfResClass = "de.wissensgarten.web.res"; generateResClass = always }

// Wissenspakete als Dateien mitliefern (gleiche JSON-Dateien wie in der Android-App) + Inhaltsverzeichnis
val packsDir = rootProject.file("content/src/main/assets/packs")
val genPacks = layout.buildDirectory.dir("generated/packs")
val copyPacks by tasks.registering {
    inputs.dir(packsDir); outputs.dir(genPacks)
    doLast {
        genPacks.get().asFile.deleteRecursively()   // auch Reste früherer Fassungen (z. B. stimme/ der früheren Offline-Stimme)
        val out = genPacks.get().dir("packs").asFile.apply { deleteRecursively(); mkdirs() }
        val files = packsDir.listFiles()!!.filter { it.extension == "json" }.sortedBy { it.name }
        files.forEach { it.copyTo(out.resolve(it.name), overwrite = true) }
        rootProject.file("content/src/main/assets/lizenzen.txt").copyTo(genPacks.get().file("lizenzen.txt").asFile, overwrite = true)
        // Vorschau-Katalog der Plus-Pakete (nur Titel, Erklärung, 3 Beispielkarten) – die vollen Pakete kommen vom Plus-Endpunkt
        rootProject.file("content/src/main/assets/plus_katalog.json").takeIf { it.exists() }?.copyTo(genPacks.get().file("plus_katalog.json").asFile, overwrite = true)
        out.resolve("index.json").writeText(files.joinToString(",", "[", "]") { "\"${it.name}\"" })
        // Für das Nachladen in der App: Datei → Paket-Version (nur Daten, kein Code)
        val slurper = groovy.json.JsonSlurper()
        out.resolve("versions.json").writeText(files.joinToString(",", "{", "}") { f ->
            val v = (slurper.parse(f) as Map<*, *>)["version"] as Number
            "\"${f.name}\":${v.toInt()}"
        })
    }
}
kotlin.sourceSets["webMain"].resources.srcDir(genPacks)
tasks.matching { it.name == "wasmJsProcessResources" || it.name == "jsProcessResources" }.configureEach { dependsOn(copyPacks) }

// Fertige WebApp nach wissensgarten/webapp/ legen (wird auf den Branch wissensgarten-test gepusht)
val appGradle = rootProject.file("app/build.gradle.kts").readText()
val versionName = Regex("""versionName = "([^"]+)"""").find(appGradle)!!.groupValues[1]
tasks.register<Sync>("exportWebApp") {
    dependsOn("wasmJsBrowserDistribution", "jsBrowserDistribution")
    val wasmDist = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
    val jsDist = layout.buildDirectory.dir("dist/js/productionExecutable")
    // Beide Fassungen nebeneinander (gemeinsame Dateien wie index.html, Pakete, Schriften nur einmal)
    from(wasmDist)
    from(jsDist)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    into(rootProject.file("webapp"))
    exclude("**/*.map")
    filesMatching(listOf("index.html", "sw.js", "manifest.webmanifest")) { filter { it.replace("@VERSION@", versionName) } }
    val versionDate = Regex("""VERSION_DATE", "\\"([0-9.]+)\\""""").find(appGradle)!!.groupValues[1]
    doLast {
        val out = rootProject.file("webapp")
        // Echter Ladebalken: Größe der großen Startdateien (entpackt) ins HTML schreiben
        val big = out.walkTopDown().filter { it.isFile && (it.extension == "wasm" || it.extension == "ttf" || it.name == "wissensgarten.js" || it.name == "wissensgarten-js.js") }.toList()
        // Dateien, die nur eine Fassung braucht: nicht vorab für alle speichern (das iPhone soll nicht 16 MB Wasm laden, die es nicht nutzt);
        // die Seite legt die Dateien ihrer Fassung selbst in den Zwischenspeicher
        fun top(d: File, ext: String) = d.listFiles()!!.filter { it.isFile && it.extension == ext }.map { it.name }
        val wasmFiles = top(wasmDist.get().asFile, "wasm") + "wissensgarten.js"
        // Die Grafik-Bibliothek (skiko, .wasm mit Hash im Namen) steckt in beiden Fassungen – die JS-Fassung braucht sie genauso (sonst startet sie offline nicht)
        val jsFiles = top(jsDist.get().asFile, "wasm") + "wissensgarten-js.js"
        val modeFiles = "{\"wasm\":" + wasmFiles.joinToString(",", "[", "]") { "\"$it\"" } + ",\"js\":" + jsFiles.joinToString(",", "[", "]") { "\"$it\"" } + "}"
        val sizes = big.joinToString(",", "{", "}") { "\"${it.name}\":${it.length()}" }
        // Service Worker: beim Installieren alles vorab speichern → zweiter Start sofort, auch offline
        // vorlesen/ (Liste der festen Texte für den Stimm-Server „Nova“) braucht die App selbst nicht
        val all = out.walkTopDown().filter { it.isFile && !it.name.endsWith(".LICENSE.txt") && it.name != "sw.js" && it.name != "version.json" &&
            !it.relativeTo(out).invariantSeparatorsPath.startsWith("vorlesen/") && it.name !in wasmFiles && it.name !in jsFiles }
            .map { it.relativeTo(out).invariantSeparatorsPath }.sorted().toList()
        val pre = (listOf("./") + all).joinToString(",", "[", "]") { "'$it'" }
        // Safari 27.2 (WebKit-Fehler 329421, weiße Fläche): In beiden Bündeln darf kein Aufruf der Grafik-Bibliothek als Endaufruf stehen
        for (b in listOf("wissensgarten.js", "wissensgarten-js.js")) {
            val js = out.resolve(b).readText()
            val tail = Regex("""=>\(\w+=\w+\._\.org_jetbrains_\w+\)\(""").findAll(js).count()
            val safe = Regex("""try\{return\(\w+=\w+\._\.org_jetbrains_\w+\)\(""").findAll(js).count()
            check(tail == 0 && safe > 900) { "Safari-Abhilfe fehlt in $b: $tail Endaufrufe übrig, $safe abgesichert" }
            println("Safari-27.2-Abhilfe $b: $safe abgesichert, $tail Endaufrufe übrig")
        }
        out.resolve("index.html").let { it.writeText(it.readText().replace("@SIZES@", sizes).replace("@MODEFILES@", modeFiles)) }
        out.resolve("sw.js").let { it.writeText(it.readText().replace("'@PRECACHE@'", pre)) }
        rootProject.file("webapp/version.json").writeText("{\n  \"version\": \"$versionName\",\n  \"date\": \"$versionDate\"\n}\n")
    }
}
