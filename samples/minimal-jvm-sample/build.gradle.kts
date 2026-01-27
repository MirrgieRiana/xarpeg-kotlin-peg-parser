plugins {
    kotlin("jvm")
    application
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

repositories {
    mavenCentral()
}

// ktlint configuration
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.3.1")
    android.set(false)
    outputColorName.set("RED")
}

group = "mirrg.xarpite.samples"
version = libs.versions.xarpeg.get()

application {
    mainClass.set("io.github.mirrgieriana.xarpeg.samples.java_run.MainKt")
}

dependencies {
    implementation(libs.xarpeg)
    testImplementation(kotlin("test"))
}

// Make build task depend on ktlintFormat
tasks.named("build") {
    dependsOn("ktlintFormat")
}

// Make ktlint check tasks run after format tasks
tasks.matching { it.name.startsWith("runKtlintCheck") }.configureEach {
    mustRunAfter(tasks.matching { it.name.startsWith("runKtlintFormat") })
}
