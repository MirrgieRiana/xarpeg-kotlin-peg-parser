pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = providers.gradleProperty("repositoryName").get()
include("doc-test")
