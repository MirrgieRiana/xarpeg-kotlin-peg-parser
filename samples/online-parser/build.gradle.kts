import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://raw.githubusercontent.com/MirrgieRiana/kotlin-peg-parser/maven/maven") }
}

group = "mirrg.xarpite.samples"
version = "1.0.3"

kotlin {
    js(IR) {
        compilerOptions {
            moduleKind.set(JsModuleKind.MODULE_ES)
        }
        browser {
            commonWebpackConfig {
                outputFileName = "online-parser.js"
                mode = KotlinWebpackConfig.Mode.PRODUCTION
            }
        }
        binaries.library()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation("io.github.mirrgieriana.xarpite:kotlin-peg-parser:1.0.3")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

val bundleRelease by tasks.registering(Sync::class) {
    group = "build"
    description = "Bundles the production JS output and resources into build/site."

    dependsOn("compileProductionLibraryKotlinJs", "jsProcessResources", "jsProductionLibraryCompileSync")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(layout.buildDirectory.dir("processedResources/js/main"))
    from(layout.buildDirectory.dir("js/packages/${project.name}/kotlin"))
    into(layout.buildDirectory.dir("site"))
}

tasks.named("build") {
    dependsOn(bundleRelease)
}
