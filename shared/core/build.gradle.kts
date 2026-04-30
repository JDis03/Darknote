plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    // Solo JVM por ahora - Android se agregará después
    jvm()

    sourceSets {
        commonMain.dependencies {
            // Serialization
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

            // UUID
            implementation("com.benasher44:uuid:0.8.4")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
