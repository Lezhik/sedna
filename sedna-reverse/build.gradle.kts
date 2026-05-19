dependencies {
    api(project(":sedna-core"))
    api(project(":sedna-dna"))
    api(project(":sedna-registry"))
    api(project(":sedna-validation"))
    implementation("com.github.javaparser:javaparser-core:3.26.3")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")

    testImplementation(project(":sedna-forward"))
    testImplementation(project(":sedna-dna"))
}
