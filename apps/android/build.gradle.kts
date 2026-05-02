plugins {
    kotlin("android")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    kotlin("plugin.serialization")
}

android {
    namespace = "com.darknote.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.darknote.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:persistence"))

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Pull-to-refresh (material)
    implementation("androidx.compose.material:material:1.6.8")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Activity
    implementation("androidx.activity:activity-compose:1.9.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

    // Tests
    testImplementation(kotlin("test"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}