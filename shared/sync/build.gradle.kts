plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

android {
    namespace = "com.darknote.sync"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    jvm()
    androidTarget()

    // Suppress KMP expect/actual beta warnings (KT-61573)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared:core"))
                implementation(project(":shared:persistence"))

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }

        val jvmMain by getting {
            dependencies {
                // Dropbox SDK (JVM/Desktop only) - v7.0.0+ required for SSL
                implementation("com.dropbox.core:dropbox-core-sdk:7.0.0")
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
            }
        }

        val androidMain by getting {
            dependencies {
                // Dropbox SDK for Android
                implementation("com.dropbox.core:dropbox-core-sdk:7.0.0")
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                // Android specific dependencies
                implementation("androidx.browser:browser:1.8.0") // For OAuth in browser
                // Encrypted SharedPreferences for secure token storage
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
