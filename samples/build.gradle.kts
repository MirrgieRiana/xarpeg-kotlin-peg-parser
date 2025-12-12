allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("../build/maven") }
    }
}

// Convenience task to run the hello sample
tasks.register("run") {
    group = "application"
    description = "Runs the hello sample application"
    dependsOn(":hello:jvmRun")
}
