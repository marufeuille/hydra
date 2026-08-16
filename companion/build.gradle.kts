plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val resolvedVersionName: String = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "0.1.0"

fun versionCodeFrom(name: String, offset: Int): Int {
    val parts = name.substringBefore("-").split(".")
    val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
    return (major * 10000 + minor * 100 + patch) * 10 + offset
}

android {
    namespace = "dev.marufeuille.hydra.companion"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.marufeuille.hydra"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeFrom(resolvedVersionName, 1)
        versionName = resolvedVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)

    implementation(libs.play.services.wearable)
    implementation(libs.health.connect.client)
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
}
