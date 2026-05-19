plugins {
    java
}

dependencies {
    testImplementation(project(":sedna-core"))
    testImplementation(project(":sedna-dna"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
