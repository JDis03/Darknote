plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    androidTarget()

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
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
