plugins {
    kotlin("jvm")
    application
}

apply(from = "../repoPath.gradle.kts")

val repoPath: String by extra

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://raw.githubusercontent.com/$repoPath/maven/maven") }
}

group = "mirrg.xarpite.samples"
version = libs.versions.xarpeg.get()

application {
    mainClass.set("io.github.mirrgieriana.xarpite.xarpeg.samples.java_run.MainKt")
}

dependencies {
    implementation(libs.xarpeg)
    testImplementation(kotlin("test"))
}
