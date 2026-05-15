import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("app.cash.sqldelight")
}

group = "com.darknote"
version = "1.0.0"

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    
    // Shared modules
    implementation(project(":shared:core"))
    implementation(project(":shared:persistence"))
    implementation(project(":shared:sync"))
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    
    // SQLDelight JVM driver
    implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
    
    // Koin DI
    implementation("io.insert-koin:koin-core:3.5.3")
    implementation("io.insert-koin:koin-compose:1.1.2")
    
    // Dropbox SDK
    implementation("com.dropbox.core:dropbox-core-sdk:5.4.5")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
}

compose.desktop {
    application {
        mainClass = "com.darknote.desktop.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
            packageName = "DarkNote"
            packageVersion = "1.0.0"
            description = "Modern snippet manager with Dropbox sync"
            vendor = "DarkNote"
            licenseFile.set(project.file("../../LICENSE"))
            
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
                menuGroup = "Utility"
                appCategory = "Utility"
            }
        }
        
        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}

sqldelight {
    databases {
        create("DarkNoteDatabase") {
            packageName.set("com.darknote.persistence.database")
        }
    }
}
