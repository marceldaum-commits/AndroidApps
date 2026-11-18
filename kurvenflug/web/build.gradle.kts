import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

// Version kommt aus der Android-App – APK und WebApp tragen immer dieselbe Nummer.
val appVersion: String = Regex("""versionName\s*=\s*"([^"]+)"""")
    .find(rootProject.file("app/build.gradle.kts").readText())?.groupValues?.get(1) ?: error("versionName nicht gefunden")

val buildInfoDir = layout.buildDirectory.dir("generated/buildinfo/kotlin")
val proProps = Properties().also { p ->
    val f = rootProject.file("pro/pro.properties")
    if (f.exists()) f.inputStream().use { p.load(it) }
}
val proKey = proProps.getProperty("publicKey", "").trim()
val proCheckout = proProps.getProperty("checkoutUrl", "").trim()

val genBuildInfo by tasks.registering {
    inputs.property("version", appVersion)
    inputs.property("proKey", proKey)
    inputs.property("proCheckout", proCheckout)
    outputs.dir(buildInfoDir)
    doLast {
        val f = buildInfoDir.get().file("BuildInfo.kt").asFile
        f.parentFile.mkdirs()
        f.writeText("/** Automatisch erzeugt aus app/build.gradle.kts */\nobject BuildInfo {\n    const val VERSION = \"$appVersion\"\n" +
            "    const val PRO_KEY = \"$proKey\"\n    const val PRO_CHECKOUT = \"$proCheckout\"\n}\n")
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("kurvenflug")
        compilerOptions { freeCompilerArgs.add("-Xwasm-no-jstag") }   // Safari 26/27: kann den Import „intrinsics.tag“ (WebAssembly.JSTag) nicht – Leitstand 18.11.
        browser { commonWebpackConfig { outputFileName = "kurvenflug.js" } }
        binaries.executable()
    }
    // Kompatibilitäts-Fassung (Kotlin/JS) für Browser ohne WebAssembly-GC/Ausnahmen – z. B. ältere iPhones/iPads.
    // index.html wählt: Wasm, wenn der Browser es kann, sonst kurvenflug-compat.js.
    js {
        outputModuleName.set("kurvenflug-compat")
        browser { commonWebpackConfig { outputFileName = "kurvenflug-compat.js" } }
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies { implementation(compose.components.resources) }
        webMain {
            kotlin.srcDir(genBuildInfo)
            dependencies { implementation(project(":shared")) }
        }
    }
}

compose.resources {
    packageOfResClass = "de.kurvenflug.web.resources"
    publicResClass = false
}

/** Neue Emojis/Sonderzeichen im Code, die die Web-Schriften noch nicht kennen? → web/fonts/make_fonts.py ausführen */
val checkWebFonts by tasks.registering {
    val sources = fileTree(rootDir) { include("**/src/*Main/**/*.kt", "app/src/main/**/*.kt"); exclude("**/build/**") }
    inputs.files(sources)
    inputs.file("fonts/chars.txt")
    doLast {
        val known = file("fonts/chars.txt").readText().toSet()
        val used = sources.files.flatMap { f -> f.readText().filter { it.code > 0xFF }.toList() }.toSet()
        val missing = used - known
        if (missing.isNotEmpty()) throw GradleException("Neue Zeichen ohne Web-Schrift: ${missing.joinToString("")} – bitte `python3 web/fonts/make_fonts.py` ausführen.")
    }
}
tasks.named("compileKotlinWasmJs") { dependsOn(checkWebFonts) }
tasks.named("compileKotlinJs") { dependsOn(checkWebFonts) }

/**
 * Fertige WebApp nach kurvenflug/webapp/ (index.html oben): ./gradlew :web:webapp
 * Dazu ein Service Worker mit genau diesen Dateien (offline spielbar) und version.json für den Leitstand.
 */
val webapp by tasks.registering(Sync::class) {
    dependsOn("wasmJsBrowserDistribution", "jsBrowserDistribution", ":core:model:vorlesenTexte")
    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    // Kompatibilitäts-Fassung: nur das Programm – die Grafik-Bibliothek (Skiko-wasm, ohne GC) ist dieselbe Datei wie oben
    from(layout.buildDirectory.dir("dist/js/productionExecutable")) { include("kurvenflug-compat.js") }
    // Feste Vorlese-Texte für die KI-Stimme Nova (das Stimmen-Werk holt sie hier ab)
    from(project(":core:model").layout.buildDirectory.dir("vorlesen-root"))
    into(rootProject.file("webapp"))
    exclude("**/*.map")
    filesMatching(listOf("index.html", "diag.html")) { filter { it.replace("{{VERSION}}", appVersion) } }
    doLast {
        val dir = rootProject.file("webapp")
        val jsDist = layout.buildDirectory.dir("dist/js/productionExecutable").get().asFile
        // Wasm-Dateien, die die JS-Fassung braucht (Skiko), müssen in beiden Ausgaben gleich heißen
        val jsWasm = jsDist.listFiles()!!.filter { it.extension == "wasm" && it.name != "skiko.wasm" }.map { it.name }.toSet()
        jsWasm.forEach { n -> check(dir.resolve(n).exists() && dir.resolve(n).length() == jsDist.resolve(n).length()) { "JS-Fassung braucht $n – fehlt in der Wasm-Ausgabe" } }
        check(jsWasm.isNotEmpty()) { "Skiko-wasm der JS-Fassung nicht gefunden" }
        // Große Dateien für das Schnell-Laden mit Fortschrittsbalken (kf.js) – je Fassung: Pfad, Größe, Typ
        fun entry(f: File) = "{\"u\":\"${f.relativeTo(dir).invariantSeparatorsPath}\",\"s\":${f.length()},\"t\":\"${if (f.extension == "wasm") "application/wasm" else "font/ttf"}\"}"
        val font = dir.walkTopDown().filter { it.isFile && it.name == "symbols.ttf" }.toList()
        val wasmFiles = dir.listFiles()!!.filter { it.extension == "wasm" }.sortedByDescending { it.length() }
        val jsFiles = wasmFiles.filter { it.name in jsWasm }
        val json = "{\"wasm\":" + (font + wasmFiles).joinToString(",", "[", "]") { entry(it) } +
            ",\"js\":" + (font + jsFiles).joinToString(",", "[", "]") { entry(it) } + "}"
        val index = dir.resolve("index.html")
        index.writeText(index.readText().replace("{{FILES}}", json))
        // Diagnose-Seite: die Wasm-Dateien der schnellen Fassung, kleinste zuerst
        val diag = dir.resolve("diag.html")
        diag.writeText(diag.readText().replace("{{FILES}}", wasmFiles.sortedBy { it.length() }.joinToString(",", "[", "]") { "{\"u\":\"${it.name}\",\"s\":${it.length()}}" }))

        // Service Worker: gemeinsame Dateien + nur die Dateien der Fassung, die das Gerät nutzt (sw.js?fassung=wasm|js)
        val all = dir.walkTopDown().filter { it.isFile }.map { it.relativeTo(dir).invariantSeparatorsPath }
            .filter { !it.startsWith("vorlesen/") && it != "sw.js" && it != "diag.html" }.sorted().toList()   // Diagnose immer frisch vom Server;   // Vorlese-Liste gehört nicht ins Offline-Paket
        val wasmOnly = all.filter { it == "kurvenflug.js" || (it.endsWith(".wasm") && it !in jsWasm) }
        val jsOnly = all.filter { it == "kurvenflug-compat.js" }
        val shared = listOf("./") + (all - wasmOnly.toSet() - jsOnly.toSet())
        fun list(l: List<String>) = l.joinToString(",\n  ") { "\"$it\"" }
        dir.resolve("sw.js").writeText("""
            |// Kurvenflug $appVersion – Service Worker (automatisch erzeugt). Macht die WebApp offline spielbar.
            |const CACHE = "kurvenflug-$appVersion";
            |const SHARED = [
            |  ${list(shared)}
            |];
            |const ONLY = {
            |  wasm: [${list(wasmOnly)}],
            |  js: [${list(jsOnly)}]
            |};
            |const MODE = new URL(location).searchParams.get("fassung") === "js" ? "js" : "wasm";
            |const FILES = SHARED.concat(ONLY[MODE]);
            |self.addEventListener("install", e => {
            |  e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES.map(f => new Request(f, { cache: "no-cache" })))).then(() => self.skipWaiting()));
            |});
            |self.addEventListener("activate", e => {
            |  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k.startsWith("kurvenflug-") && k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()));
            |});
            |self.addEventListener("fetch", e => {
            |  const url = new URL(e.request.url);
            |  if (e.request.method !== "GET" || url.origin !== location.origin) return;
            |  // version.json immer frisch vom Server (Update-Prüfung); ohne Netz die gespeicherte
            |  if (url.pathname.endsWith("/version.json")) { e.respondWith(fetch(e.request).catch(() => caches.match(e.request, { ignoreSearch: true }))); return; }
            |  e.respondWith(caches.match(e.request, { ignoreSearch: true }).then(r => r || fetch(e.request).then(res => {
            |    // Dateien der anderen Fassung (Wechsel per Knopf) beim ersten Laden ebenfalls offline merken
            |    const p = url.pathname.split("/").pop();
            |    if (res.ok && (ONLY.wasm.includes(p) || ONLY.js.includes(p))) { const copy = res.clone(); caches.open(CACHE).then(c => c.put(e.request, copy)); }
            |    return res;
            |  })));
            |});
            |""".trimMargin())
        // Safari 27.2 (WebKit-Fehler 329421, weiße Seite): Im fertigen Paket darf kein Aufruf der Grafik-Bibliothek mehr als
        // Endaufruf stehen (siehe web/webpack/skiko-no-tail-call.cjs, übernommen von Raumlicht) – für beide Fassungen
        listOf("kurvenflug.js", "kurvenflug-compat.js").forEach { name ->
            val js = dir.resolve(name).readText()
            val tail = Regex("""=>\(\w+=\w+\._\.org_jetbrains_\w+\)\(""").findAll(js).count()
            val safe = Regex("""try\{return\(\w+=\w+\._\.org_jetbrains_\w+\)\(""").findAll(js).count()
            check(tail == 0 && safe > 900) { "Safari-Abhilfe fehlt in $name: $tail Endaufrufe übrig, $safe abgesichert" }
            println("Safari-27.2-Abhilfe $name: $safe Aufrufe abgesichert, $tail Endaufrufe übrig")
        }
        dir.resolve("version.json").writeText("{\n  \"app\": \"kurvenflug\",\n  \"version\": \"$appVersion\"\n}\n")
    }
}
