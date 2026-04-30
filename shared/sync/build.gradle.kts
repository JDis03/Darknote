plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    // Solo JVM por ahora - Android se agregará después
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core"))
            implementation(project(":shared:persistence"))

            // Dropbox
            implementation("com.dropbox.core:dropbox-core-sdk:6.0.0")

            // Serialization
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
