plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.chicagoist.justgerman"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.chicagoist.justgerman"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // Release signing comes from environment variables (see tools/build-release.sh)
        // so keystore secrets never end up in the repo. Without them the release is unsigned.
        val keystorePath = System.getenv("KEYSTORE_PATH")
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Fail loud ONLY when a release build is actually requested and
            // no signing config exists: AGP would otherwise silently produce
            // `app-release-unsigned.apk`, which a CI glob path could
            // mis-attribute as a signed artifact. The check is gated on the
            // requested task names so `assembleDebug`, tests, lint and IDE
            // sync never require the release signing env vars. Env vars
            // match what tools/build-release.sh and the CI workflows export.
            val releaseSigning = signingConfigs.findByName("release")
            signingConfig = releaseSigning
            // Fire only for explicit release tasks (assembleRelease,
            // bundleRelease, installRelease, ...), not for bare `assemble`
            // or unit-test-only task names that merely contain "release".
            val releaseRequested = gradle.startParameter.taskNames.any {
                it.substringAfterLast(':').endsWith("Release", ignoreCase = true)
            }
            if (releaseRequested && releaseSigning == null) {
                error("Release signing keystore not configured. " +
                        "Set KEYSTORE_PATH (path to release.jks), KEYSTORE_PASSWORD, " +
                        "and KEY_ALIAS env vars; see tools/build-release.sh.")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // App Bundle: Play Store serves only the resources each device needs.
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    androidResources {
        noCompress += listOf("mp3", "pdf", "m4a", "ogg")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Media3 ExoPlayer (audio playback)
    implementation("androidx.media3:media3-exoplayer:1.2.1")

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // DataStore (lesson completion progress)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Kotlinx Coroutines (used directly by screens)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Debug (using BOM versions)
    debugImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
