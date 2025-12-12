plugins {
    kotlin("jvm")
    application
}

group = "mirrg.xarpite.samples"
version = "1.0.3"

application {
    mainClass.set("mirrg.xarpite.samples.hello.MainKt")
}

dependencies {
    implementation("io.github.mirrgieriana.xarpite:kotlin-peg-parser:1.0.3")
    testImplementation(kotlin("test"))
}
