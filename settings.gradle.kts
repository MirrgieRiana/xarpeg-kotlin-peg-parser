pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = providers.gradleProperty("projectName").get()
include("doc-test")
