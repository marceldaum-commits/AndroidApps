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
        browser { commonWebpackConfig { outputFileName = "wissensgarten.js" } }
        binaries.executable()
    }
    sourceSets {
        wasmJsMain.dependencies {
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
        val out = genPacks.get().dir("packs").asFile.apply { deleteRecursively(); mkdirs() }
        val files = packsDir.listFiles()!!.filter { it.extension == "json" }.sortedBy { it.name }
        files.forEach { it.copyTo(out.resolve(it.name), overwrite = true) }
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
kotlin.sourceSets["wasmJsMain"].resources.srcDir(genPacks)
tasks.matching { it.name == "wasmJsProcessResources" }.configureEach { dependsOn(copyPacks) }

// Fertige WebApp nach wissensgarten/webapp/ legen (wird auf den Branch wissensgarten-test gepusht)
val appGradle = rootProject.file("app/build.gradle.kts").readText()
val versionName = Regex("""versionName = "([^"]+)"""").find(appGradle)!!.groupValues[1]
tasks.register<Sync>("exportWebApp") {
    dependsOn("wasmJsBrowserDistribution")
    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    into(rootProject.file("webapp"))
    exclude("**/*.map")
    filesMatching(listOf("index.html", "sw.js", "manifest.webmanifest")) { filter { it.replace("@VERSION@", versionName) } }
    val versionDate = Regex("""VERSION_DATE", "\\"([0-9.]+)\\""""").find(appGradle)!!.groupValues[1]
    doLast {
        val out = rootProject.file("webapp")
        // Echter Ladebalken: Größe der großen Startdateien (entpackt) ins HTML schreiben
        val big = out.walkTopDown().filter { it.isFile && (it.extension == "wasm" || it.extension == "ttf" || it.name == "wissensgarten.js") }.toList()
        val sizes = big.joinToString(",", "{", "}") { "\"${it.name}\":${it.length()}" }
        // Service Worker: beim Installieren alles vorab speichern → zweiter Start sofort, auch offline
        val all = out.walkTopDown().filter { it.isFile && !it.name.endsWith(".LICENSE.txt") && it.name != "sw.js" && it.name != "version.json" }
            .map { it.relativeTo(out).invariantSeparatorsPath }.sorted().toList()
        val pre = (listOf("./") + all).joinToString(",", "[", "]") { "'$it'" }
        out.resolve("index.html").let { it.writeText(it.readText().replace("@SIZES@", sizes)) }
        out.resolve("sw.js").let { it.writeText(it.readText().replace("'@PRECACHE@'", pre)) }
        rootProject.file("webapp/version.json").writeText("{\n  \"version\": \"$versionName\",\n  \"date\": \"$versionDate\"\n}\n")
    }
}
