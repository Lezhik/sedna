dependencies {
    api(project(":sedna-core"))
    api(project(":sedna-dna"))
    api(project(":sedna-registry"))
    api(project(":sedna-validation"))
    api(project(":sedna-persistence"))
    implementation("io.projectreactor:reactor-core:3.7.2")

    testImplementation(project(":sedna-dna"))
}
