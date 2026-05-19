dependencies {
    api(project(":sedna-core"))
    api(project(":sedna-dna"))
    api(project(":sedna-registry"))
    api(project(":sedna-reverse"))
    api(project(":sedna-mutation"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")

    testImplementation(project(":sedna-dna"))
}
