dependencies {
    api(project(":sedna-core"))
    api(project(":sedna-dna"))
    api(project(":sedna-registry"))
    api(project(":sedna-validation"))
    implementation("com.github.javaparser:javaparser-core:3.26.3")
    implementation("fr.inria.gforge.spoon:spoon-core:11.3.0")
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")

    testImplementation(project(":sedna-forward"))
    testImplementation(project(":sedna-dna"))
}
