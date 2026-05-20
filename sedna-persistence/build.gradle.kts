dependencies {
    api(project(":sedna-core"))
    implementation("org.postgresql:postgresql:42.7.4")

    testImplementation(project(":sedna-dna"))
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
}
