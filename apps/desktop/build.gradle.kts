plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    kotlin("plugin.serialization")
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared:core"))
                implementation(project(":shared:persistence"))
                implementation(project(":shared:sync"))

                // Compose Desktop
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
                
                // Logging (SLF4J implementation for Dropbox SDK)
                implementation("org.slf4j:slf4j-simple:2.0.9")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.darknote.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "darknote"
            packageVersion = "1.0.0"
        }
    }
}
