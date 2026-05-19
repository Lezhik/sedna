dependencies {
    api(project(":sedna-core"))
    api(project(":sedna-dna"))
    api(project(":sedna-registry"))
    api(project(":sedna-validation"))
    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.github.spullara.mustache.java:compiler:0.9.11")
}
