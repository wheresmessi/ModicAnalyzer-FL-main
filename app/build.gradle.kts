plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"
}

// Apply the Google services plugin to make google-services.json values available to Firebase SDKs.
// This plugin is declared in the project-level `build.gradle.kts` (id("com.google.gms.google-services") version "4.4.4" apply false).
// If you don't want to enable Firebase in your environment, you can remove or comment this line.
apply(plugin = "com.google.gms.google-services")

android {
    namespace = "com.example.modicanalyzer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.modicanlyzer"
        minSdk = 26  // Updated to Android O (API 26) to support MethodHandle APIs
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true  // Enable desugaring for better API compatibility
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    
    // Prevent Android from compressing TF Lite model files (modern Kotlin DSL syntax)
    androidResources {
        noCompress += "tflite"
    }
}

// LiteRT dependency resolution (no conflicts)
configurations.all {
    resolutionStrategy {
        // Force LiteRT version for consistency
        force("com.google.ai.edge.litert:litert:1.0.1")
    }
}

dependencies {
    // LiteRT 2025 - Google's future-proof solution (eliminates conflicts)
    implementation("com.google.ai.edge.litert:litert:1.0.1")
    
    // Task API for official async pattern
    implementation("com.google.android.gms:play-services-tasks:18.0.2")
    
    // HTTP client for federated learning and remote inference
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON serialization for API communication
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Core library desugaring for better API compatibility
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Firebase BoM - ensures compatible versions for all Firebase libraries
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    // Firebase libraries - Explicitly versioned to resolve dependency issue
    implementation("com.google.firebase:firebase-auth-ktx:22.4.0")
    implementation("com.google.firebase:firebase-analytics-ktx:22.0.0")
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Compatibility: some CI or tooling may request ':app:testClasses' which isn't present
// in Android modules. Register a safe no-op task to avoid failures.
tasks.register("testClasses") {
    // This task intentionally left blank. It prevents external tooling from failing
    // when they try to query or execute ':app:testClasses'.
}