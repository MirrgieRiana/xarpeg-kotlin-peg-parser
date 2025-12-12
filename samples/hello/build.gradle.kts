plugins {
    kotlin("multiplatform")
}

group = "mirrg.xarpite.samples"
version = "1.0.3"

kotlin {
    jvm {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("mirrg.xarpite.samples.hello.MainKt")
        }
    }

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

// Create a task to prepare distribution with all dependencies
tasks.register<Copy>("installDist") {
    dependsOn("jvmJar")
    
    val jvmJar = tasks.named<Jar>("jvmJar").get()
    val runtimeClasspath = configurations.named("jvmRuntimeClasspath").get()
    
    into(layout.buildDirectory.dir("install/hello"))
    
    from(jvmJar.archiveFile) {
        into("lib")
    }
    from(runtimeClasspath) {
        into("lib")
    }
    
    doLast {
        val binDir = layout.buildDirectory.dir("install/hello/bin").get().asFile
        binDir.mkdirs()
        
        val scriptFile = File(binDir, "hello")
        scriptFile.writeText("""
            #!/bin/sh
            SCRIPT_DIR=${'$'}(cd "${'$'}(dirname "${'$'}0")" && pwd)
            CLASSPATH="${'$'}SCRIPT_DIR/../lib/*"
            exec java -cp "${'$'}CLASSPATH" mirrg.xarpite.samples.hello.MainKt "${'$'}@"
        """.trimIndent())
        scriptFile.setExecutable(true)
        
        val batFile = File(binDir, "hello.bat")
        batFile.writeText("""
            @echo off
            set SCRIPT_DIR=%~dp0
            set CLASSPATH=%SCRIPT_DIR%..\lib\*
            java -cp "%CLASSPATH%" mirrg.xarpite.samples.hello.MainKt %*
        """.trimIndent())
    }
}
