plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.monolith.tv"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "dev.monolith.tv"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.1.3-prealpha"
    }

    buildTypes {
        debug {
            // Existing user preview: never wipe it to resolve signing conflicts.
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
        }
        create("review") {
            // Parallel BOXY UI acceptance build. Leaves the existing preview and
            // its local favorites/preferences untouched if CI signatures differ.
            initWith(getByName("debug"))
            applicationIdSuffix = ".review"
            versionNameSuffix = "-review"
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
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.tv:tv-material:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    testImplementation("junit:junit:4.13.2")
}

