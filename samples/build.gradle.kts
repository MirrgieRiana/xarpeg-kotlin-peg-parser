allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/MirrgieRiana/kotlin-peg-parser/maven/maven") }
    }
}

// Convenience task to run the hello sample
tasks.register("run") {
    group = "application"
    description = "Runs the hello sample application"
    dependsOn(":hello:run")
}
