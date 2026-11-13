plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.monolith.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.monolith.tv"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.2-prealpha"
    }

    buildTypes {
        debug {
            // A separately installable preview, never an automatic HOME replacement.
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
        }
        release {
            // No production release is distributed until dedicated stable signing exists.
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.tv:tv-material:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    testImplementation("junit:junit:4.13.2")
}

