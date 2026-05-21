plugins {
    java
    id("me.champeau.jmh") version "0.7.2"
}

dependencies {
    jmh(project(":sedna-core"))
    jmh(project(":sedna-dna"))
    jmh(project(":sedna-registry"))
    jmh(project(":sedna-forward"))
    jmh(project(":sedna-reverse"))
    jmh(project(":sedna-runtime"))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

jmh {
    warmupIterations.set(2)
    iterations.set(5)
    fork.set(1)
    resultFormat.set("JSON")
    resultsFile.set(layout.buildDirectory.file("results/jmh/results.json"))
}

tasks.register<Exec>("verifyJmhThresholds") {
    group = "verification"
    description = "Check JMH JSON results against E2E performance targets (Linux/macOS)"
    dependsOn("jmh")
    val results = layout.buildDirectory.file("results/jmh/results.json")
    commandLine(
        "bash",
        rootProject.file("benchmarks/verify-thresholds.sh"),
        results.get().asFile.absolutePath,
    )
}
