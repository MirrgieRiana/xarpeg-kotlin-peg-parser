val defaultRepoPath = "MirrgieRiana/xarpeg-kotlin-peg-parser"

val repoPath = providers.gradleProperty("repoPath").orElse(
    providers.provider {
        val propertiesFile = rootDir.resolve("../../gradle.properties")
        if (propertiesFile.isFile) {
            java.util.Properties().apply {
                propertiesFile.inputStream().use(::load)
            }.getProperty("repoPath") ?: defaultRepoPath
        } else {
            defaultRepoPath
        }
    }
).get()

extra["repoPath"] = repoPath
