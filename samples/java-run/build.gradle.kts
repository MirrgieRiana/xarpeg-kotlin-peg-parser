plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://raw.githubusercontent.com/MirrgieRiana/xarpeg-kotlin-peg-parser/maven/maven") }
}

group = "mirrg.xarpite.samples"
version = "1.0.3"

application {
    mainClass.set("mirrg.xarpite.samples.java_run.MainKt")
}

dependencies {
    implementation("io.github.mirrgieriana.xarpite:xarpeg-kotlin-peg-parser:1.0.3")
    testImplementation(kotlin("test"))
}
