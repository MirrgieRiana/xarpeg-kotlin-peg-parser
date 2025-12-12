plugins {
    kotlin("multiplatform")
}

group = "mirrg.xarpite.samples"
version = "1.0.3"

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.github.mirrgieriana.xarpite:kotlin-peg-parser:1.0.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
