plugins {
    kotlin("multiplatform") version "2.2.20"
    id("maven-publish")
    id("org.jetbrains.dokka") version "2.0.0"
}

group = "io.github.mirrgieriana.xarpite"
version = System.getenv("VERSION") ?: "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    // JVM target
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    // JS target with module kind
    js(IR) {
        binaries.executable()
        nodejs()
    }

    // Native target for Linux x64
    linuxX64 {
        binaries {
            executable {
                entryPoint = "mirrg.xarpite.peg.main"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("imported/src/commonMain/kotlin")
        }

        val commonTest by getting {
            kotlin.srcDir("imported/src/commonTest/kotlin")
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }

        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
        val linuxX64Main by getting
        val linuxX64Test by getting
    }
}

publishing {
    repositories {
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("maven"))
        }
    }
}

// Dokka configuration for KDoc generation
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    moduleName.set("xarpeg-kotlin-peg-parser")
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    
    // Suppress linuxX64 source set to avoid Kotlin/Native download issues
    dokkaSourceSets.configureEach {
        if (name.contains("linuxX64", ignoreCase = true)) {
            suppress.set(true)
        }
    }
}
