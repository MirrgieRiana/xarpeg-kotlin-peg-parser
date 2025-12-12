allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("../build/maven") }
    }
}
