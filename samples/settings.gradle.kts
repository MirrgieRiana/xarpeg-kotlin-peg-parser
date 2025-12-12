pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    val kotlinVersion: String by settings
    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("jvm") version kotlinVersion
    }
}

rootProject.name = "samples"
include("hello")
