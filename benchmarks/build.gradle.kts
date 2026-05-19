plugins {
    java
    id("me.champeau.jmh") version "0.7.2"
}

dependencies {
    jmh(project(":sedna-core"))
    jmh(project(":sedna-dna"))
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
}
