import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("org.jetbrains.kotlin.android")

    // APLICAMOS LOS PLUGINS DE FIREBASE PARA ANDROID:
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseAppDistribution)
}

kotlin {
    compilerOptions {
        // 🚀 ACTUALIZADO A JVM 17 PARA ESTAR EN SINTONÍA CON EL MÓDULO COMPARTIDO
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "dev.josearroyo.fitlog"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.josearroyo.fitlog"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        // Recuperamos la versión real de tu FitLog original:
        versionCode = 9
        versionName = "1.1.6"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        // 🚀 ACTUALIZADO A JAVA 17
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// CONFIGURACIÓN DE TU GRUPO PILOTO DE ATLETAS:
firebaseAppDistribution {
    artifactType = "APK"
    groups = "fitlog-prueba-piloto"
    releaseNotes = "Versión 1.1.6: Transición oficial de FitLog a arquitectura unificada Compose Multiplatform."
    serviceCredentialsFile = file("firebase-credentials.json").absolutePath
}