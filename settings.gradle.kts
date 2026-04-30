pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "darknote"

// Shared modules
include(":shared:core")
include(":shared:persistence")
include(":shared:sync")

// Apps - Desktop primero, Android después
include(":apps:desktop")
// include(":apps:android") // Descomentar cuando configuremos Android
