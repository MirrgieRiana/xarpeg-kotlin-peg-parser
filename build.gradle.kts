plugins {
    kotlin("multiplatform") version "2.2.20"
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
    }
}
