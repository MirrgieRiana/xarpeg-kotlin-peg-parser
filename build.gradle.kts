plugins {
    kotlin("multiplatform") version "2.2.20"
    id("maven-publish")
}

group = "mirrg.xarpite"
version = "1.0.0-SNAPSHOT"

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
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
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
            url = uri(layout.buildDirectory.dir("maven"))
        }
    }
}
