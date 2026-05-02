plugins {
    kotlin("multiplatform")
    id("app.cash.sqldelight")
}

kotlin {
    jvm()

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
        }
    }
}
