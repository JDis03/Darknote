plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("app.cash.sqldelight")
}

android {
    namespace = "com.darknote.persistence"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
    jvm()
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core"))

            // SQLDelight
            implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
            implementation("app.cash.sqldelight:primitive-adapters:2.0.2")

            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        }
        
        jvmMain.dependencies {
            // SQLDelight JDBC Driver for JVM/Desktop
            implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
        }
        
        androidMain.dependencies {
            // SQLDelight Android Driver
            implementation("app.cash.sqldelight:android-driver:2.0.2")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

sqldelight {
    databases {
        create("DarkNoteDatabase") {
            packageName.set("com.darknote.persistence.database")
            srcDirs.setFrom("src/commonMain/sqldelight")
            version = 2
        }
    }
}
