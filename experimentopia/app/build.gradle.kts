import java.util.Properties

// Forscher-Pass: Schlüssel und Kasse aus pro/pro.properties (GitHub-APK; die Play-Fassung bekommt keine Codes)
val proProps = Properties().apply { val f = rootProject.file("pro/pro.properties"); if (f.exists()) f.inputStream().use { load(it) } }

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signatur-Schlüssel aus keystore.properties (nicht im Repo). Fehlt die Datei, wird mit dem Test-Schlüssel gebaut.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "de.experimentopia.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.experimentopia.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 41
        versionName = "2.9.0"
        // Datum dieser Version – bei jeder Auslieferung zusammen mit versionName anpassen (steht auch im CHANGELOG)
        // nur echte Handys (Maschinencode der Sprachsynthese) – hält die APK klein
        buildConfigField("String", "VERSION_DATE", "\"18.11.2026\"")
    }

    // Zwei Fassungen derselben App (gleicher Paketname, gleicher Schlüssel):
    //   github – APK zum direkten Installieren; bringt den eigenen Update-Knopf mit (lädt neue Versionen aus GitHub Releases)
    //   play   – für den Play Store; Updates kommen über den Store, keine Installations-Berechtigung nötig
    flavorDimensions += "vertrieb"
    productFlavors {
        create("github") { dimension = "vertrieb"; isDefault = true; buildConfigField("boolean", "SELF_UPDATE", "true")
            buildConfigField("String", "PRO_KEY", "\"${proProps.getProperty("publicKey", "").trim()}\"")
            buildConfigField("String", "PRO_CHECKOUT", "\"${proProps.getProperty("checkoutUrl", "").trim()}\"") }
        // Play: Käufe dort nur über Google Play (noch nicht angebunden) – darum ohne Website-Kasse, alles frei
        create("play") { dimension = "vertrieb"; buildConfigField("boolean", "SELF_UPDATE", "false")
            buildConfigField("String", "PRO_KEY", "\"\""); buildConfigField("String", "PRO_CHECKOUT", "\"\"") }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) create("upload") {
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2")
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                it.maxHeapSize = "2g"
                // Robolectric-Bildschirmfotos sammeln Speicher an – alle 25 Tests eine frische Java-Instanz
                it.setForkEvery(25)
                it.systemProperty("roborazzi.test.record", "true")
                // Schmal-Prüfung (320 dp) nur auf Wunsch: ./gradlew … -Pschmal
                if (project.hasProperty("schmal")) it.systemProperty("schmal", "1")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation(project(":core:data"))
    implementation(project(":shared"))   // alle Bildschirme – derselbe Code wie die WebApp
    // Nur Play-Fassung: Updates über den Store anbieten (Play In-App Updates)
    "playImplementation"("com.google.android.play:app-update:2.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.79.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.79.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Bildschirmfoto-Tests brauchen das Debug-Test-Manifest – in der Release-Variante überspringen.
tasks.withType<Test>().configureEach {
    if (name.contains("Release")) enabled = false
}

