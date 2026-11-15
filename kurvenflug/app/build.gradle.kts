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
    namespace = "de.kurvenflug.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.kurvenflug.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.5.1"
        // Update-Knopf (lädt neue Versionen aus GitHub Releases). Für den Play Store auf false setzen –
        // dort kommen Updates über den Store, und die Installations-Berechtigung ist nicht erlaubt.
        buildConfigField("boolean", "SELF_UPDATE", "true")
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
    implementation(project(":core:data"))
    implementation("androidx.navigation:navigation-compose:2.9.6")

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
