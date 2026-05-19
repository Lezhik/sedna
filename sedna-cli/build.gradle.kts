plugins {
    application
}

application {
    mainClass.set("io.sedna.cli.SednaCli")
}

dependencies {
    implementation(project(":sedna-core"))
    implementation(project(":sedna-dna"))
    implementation(project(":sedna-registry"))
    implementation(project(":sedna-validation"))
    implementation(project(":sedna-forward"))
}
