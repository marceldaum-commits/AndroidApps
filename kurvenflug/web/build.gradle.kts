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
        browser { commonWebpackConfig { outputFileName = "kurvenflug.js" } }
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies { implementation(compose.components.resources) }
        wasmJsMain {
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

/**
 * Fertige WebApp nach kurvenflug/webapp/ (index.html oben): ./gradlew :web:webapp
 * Dazu ein Service Worker mit genau diesen Dateien (offline spielbar) und version.json für den Leitstand.
 */
val webapp by tasks.registering(Sync::class) {
    dependsOn("wasmJsBrowserDistribution", ":core:model:vorlesenTexte")
    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    // Feste Vorlese-Texte für die KI-Stimme Nova (das Stimmen-Werk holt sie hier ab)
    from(project(":core:model").layout.buildDirectory.dir("vorlesen")) { into("vorlesen") }
    into(rootProject.file("webapp"))
    exclude("**/*.map")
    filesMatching("index.html") { filter { it.replace("{{VERSION}}", appVersion) } }
    doLast {
        // Große Dateien für das Schnell-Laden mit Fortschrittsbalken (kf.js) – Pfad, Größe, Typ
        val dir = rootProject.file("webapp")
        val big = dir.walkTopDown().filter { it.isFile && (it.extension == "wasm" || it.name == "symbols.ttf") }   // Emoji-Schrift kommt erst nach dem Start.sortedByDescending { it.length() }
        val json = big.joinToString(",", "[", "]") { f ->
            "{\"u\":\"${f.relativeTo(dir).invariantSeparatorsPath}\",\"s\":${f.length()},\"t\":\"${if (f.extension == "wasm") "application/wasm" else "font/ttf"}\"}"
        }
        val index = dir.resolve("index.html")
        index.writeText(index.readText().replace("{{FILES}}", json))
    }
    doLast {
        val dir = rootProject.file("webapp")
        // Die Liste der Vorlese-Texte (für unser Stimmen-Werk) gehört nicht ins Offline-Paket
        val files = dir.walkTopDown().filter { it.isFile }.map { it.relativeTo(dir).invariantSeparatorsPath }
            .filter { !it.startsWith("vorlesen/") }.sorted().toList()
        val list = (listOf("./") + files).joinToString(",\n  ") { "\"$it\"" }
        dir.resolve("sw.js").writeText("""
            |// Kurvenflug $appVersion – Service Worker (automatisch erzeugt). Macht die WebApp offline spielbar.
            |const CACHE = "kurvenflug-$appVersion";
            |const FILES = [
            |  $list
            |];
            |self.addEventListener("install", e => {
            |  e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES.map(f => new Request(f, { cache: "no-cache" })))).then(() => self.skipWaiting()));
            |});
            |self.addEventListener("activate", e => {
            |  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k.startsWith("kurvenflug-") && k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()));
            |});
            |self.addEventListener("fetch", e => {
            |  if (e.request.method !== "GET" || new URL(e.request.url).origin !== location.origin) return;
            |  e.respondWith(caches.match(e.request, { ignoreSearch: true }).then(r => r || fetch(e.request)));
            |});
            |""".trimMargin())
        dir.resolve("version.json").writeText("{\n  \"app\": \"kurvenflug\",\n  \"version\": \"$appVersion\"\n}\n")
    }
}
