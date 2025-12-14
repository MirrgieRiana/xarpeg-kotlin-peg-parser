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
version = libs.versions.xarpeg.get()

application {
    mainClass.set("io.github.mirrgieriana.xarpite.xarpeg.samples.interpreter.MainKt")
}

dependencies {
    implementation(libs.xarpeg)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    // Ensure the distribution is built before running tests
    dependsOn(tasks.installDist)
}
