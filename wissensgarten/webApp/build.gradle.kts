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
        out.resolve("index.json").writeText(files.joinToString(",", "[", "]") { "\"${it.name}\"" })
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
}
