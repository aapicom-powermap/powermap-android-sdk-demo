import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.powermap.demo"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.powermap.demo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Read API keys from local.properties (never hardcode in source)
        buildConfigField("String", "MAP_API_KEY",    "\"${localProps["POWERMAP_MAP_API_KEY"] ?: ""}\"")
        buildConfigField("String", "CLIENT_ID",       "\"${localProps["POWERMAP_CLIENT_ID"] ?: ""}\"")
        buildConfigField("String", "CLIENT_SECRET",   "\"${localProps["POWERMAP_CLIENT_SECRET"] ?: ""}\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
}

dependencies {
    // PowerMap SDK — consumed as local AAR
    implementation(files("libs/powermap-sdk-release.aar"))

    // Transitive dependencies required by powermap-sdk-release.aar
    api("org.maplibre.gl:android-sdk:11.0.0")
    api("org.maplibre.gl:android-plugin-annotation-v9:3.0.0")
    api("com.google.android.gms:play-services-location:21.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // App dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.code.gson:gson:2.10.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
