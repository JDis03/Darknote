plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    kotlin("plugin.serialization")
    application
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:persistence"))
    implementation(project(":shared:sync"))

    // Compose Desktop - Versiones específicas
    implementation("org.jetbrains.compose.desktop:desktop:1.6.11")
    implementation("org.jetbrains.compose.material3:material3-desktop:1.6.11")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.6.11")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
}

application {
    mainClass.set("com.darknote.desktop.MainKt")
}
