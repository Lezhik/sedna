pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "sedna"

include(
    "sedna-core",
    "sedna-dna",
    "sedna-registry",
    "sedna-validation",
    "sedna-forward",
    "sedna-reverse",
    "sedna-runtime",
    "sedna-mutation",
    "sedna-training",
    "sedna-persistence",
    "sedna-cli",
    "tests",
    "benchmarks",
)
