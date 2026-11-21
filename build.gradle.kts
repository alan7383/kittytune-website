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
            implementation("io.github.alexzhirkevich:compottie:2.3.3")
        }
    }
}



// Same optimisation pipeline as the Kotlin default, except the final pass: Binaryen 125's -Oz
// segfaults on this module, while -Os produces practically the same output (within ~0.3%).
tasks.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExec>().configureEach {
    binaryenArguments.set(
        listOf(
            "--enable-gc", "--enable-reference-types", "--enable-exception-handling",
            "--enable-bulk-memory", "--enable-nontrapping-float-to-int",
            "--no-inline=kotlin.wasm.internal.throwValue",
            "--no-inline=kotlin.wasm.internal.getKotlinException",
            "--no-inline=kotlin.wasm.internal.jsToKotlinStringAdapter",
            "--inline-functions-with-loops", "--traps-never-happen", "--fast-math", "--closed-world",
            "--type-ssa", "-O3", "-O3", "--gufa", "-O3", "--type-merging", "-O3", "-Os",
        )
    )
}
