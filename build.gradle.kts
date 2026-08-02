@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform") version "2.4.0"
    id("org.jetbrains.compose") version "1.12.0-beta01"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
}

kotlin {
    wasmJs {
        browser {
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
            implementation("com.materialkolor:material-kolor:5.0.0-alpha07")
        }
    }
}


