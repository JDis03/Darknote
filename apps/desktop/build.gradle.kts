plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:persistence"))
    implementation(project(":shared:sync"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
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
