import java.util.Properties

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
    namespace = "de.wissensgarten.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.wissensgarten.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 27
        versionName = "0.16.1"
    }

    // Zwei Varianten aus demselben Code:
    //  github – APK für GitHub-Releases mit eingebautem Updater (🎁-Hinweis)
    //  play   – AAB für den Google Play Store: kein eigener Updater (Play-Richtlinie), Updates über Play,
    //           Wissens-Safari (Bilderkennung) aus, weil die Familien-Richtlinie nur freigegebene SDKs erlaubt
    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            buildConfigField("boolean", "SELF_UPDATE", "true")
            buildConfigField("boolean", "SAFARI", "true")
            manifestPlaceholders["mlkitDeps"] = "ica,barcode_ui"
        }
        create("play") {
            dimension = "store"
            buildConfigField("boolean", "SELF_UPDATE", "false")
            buildConfigField("boolean", "SAFARI", "false")
            manifestPlaceholders["mlkitDeps"] = "barcode_ui"
        }
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
                it.systemProperty("roborazzi.test.record", "true")
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
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation(project(":core:data"))
    implementation(project(":content"))
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    // Klassen-Code: QR erzeugen (zxing) und scannen (Google Code Scanner – ohne Kamera-Berechtigung)
    implementation("com.google.zxing:core:3.5.4")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    // Wissens-Safari: Dinge auf Fotos erkennen – auf dem Gerät, Modell über Play-Dienste
    implementation("com.google.android.gms:play-services-mlkit-image-labeling:16.0.8")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    // Geräte-Sync: Google-Anmeldung nur für den versteckten App-Ordner im Drive
    implementation("com.google.android.gms:play-services-auth:21.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    // Play-Store-Variante: Updates über Google Play (In-App-Updates)
    "playImplementation"("com.google.android.play:app-update-ktx:2.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.79.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.79.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Bildschirmfoto-Tests brauchen das Debug-Test-Manifest – in der Release-Variante überspringen.
tasks.withType<Test>().configureEach {
    // Tests laufen einmal (GitHub-Debug) – die Play-Variante teilt denselben Code
    if (name.contains("Release") || name.contains("Play")) enabled = false
}
