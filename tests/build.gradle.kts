dependencies {
    testImplementation(project(":sedna-core"))
    testImplementation(project(":sedna-dna"))
    testImplementation(project(":sedna-registry"))
    testImplementation(project(":sedna-validation"))
    testImplementation(project(":sedna-forward"))
    testImplementation(project(":sedna-reverse"))
    testImplementation(project(":sedna-runtime"))
    testImplementation(project(":sedna-mutation"))
    testImplementation(project(":sedna-training"))
    testImplementation(project(":sedna-persistence"))
    testImplementation(project(":sedna-cli"))
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("e2e")
    }
}

tasks.register<Test>("e2e") {
    description = "End-to-end integration tests"
    group = "verification"
    useJUnitPlatform {
        includeTags("e2e")
    }
    environment("SEDNA_LLM_ENABLED", "false")
}
