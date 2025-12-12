plugins {
    kotlin("multiplatform") version "2.2.20"
}

group = "mirrg.xarpite.samples"
version = "1.0.0-SNAPSHOT"

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("mirrg.xarpite:kotlin-peg-parser-jvm:1.0.0-SNAPSHOT")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
