import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.spotbugs.snom.SpotBugsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    base
    id("com.diffplug.spotless") version "7.0.2" apply false
    id("com.github.spotbugs") version "6.1.2" apply false
}

allprojects {
    group = "io.sedna"
    version = property("sedna.version") as String

    repositories {
        mavenCentral()
    }
}

val libraryModules =
    setOf(
        "sedna-core",
        "sedna-dna",
        "sedna-registry",
        "sedna-validation",
        "sedna-forward",
        "sedna-reverse",
        "sedna-runtime",
        "sedna-mutation",
        "sedna-training",
        "sedna-persistence",
        "sedna-cli",
    )

subprojects {
    if (name in libraryModules) {
        apply(plugin = "java-library")
        apply(plugin = "com.diffplug.spotless")
        apply(plugin = "com.github.spotbugs")
        configureJavaDefaults()
        configureSpotless()
        configureSpotbugs()
    } else if (name == "tests") {
        apply(plugin = "java")
        apply(plugin = "com.diffplug.spotless")
        configureJavaDefaults()
        configureSpotless()
    } else if (name == "benchmarks") {
        // benchmarks/build.gradle.kts applies java + jmh plugins
    }
}

fun Project.configureJavaDefaults() {
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        if (plugins.hasPlugin("java-library")) {
            withSourcesJar()
            withJavadocJar()
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
    dependencies {
        add("testImplementation", platform("org.junit:junit-bom:5.11.4"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }
}

fun Project.configureSpotless() {
    extensions.configure<SpotlessExtension> {
        java {
            target("src/*/java/**/*.java")
            importOrder()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint()
        }
    }
    tasks.named("check") { dependsOn("spotlessCheck") }
}

fun Project.configureSpotbugs() {
    extensions.configure<SpotBugsExtension> {
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
        val excludeFile = rootProject.file("config/spotbugs/exclude.xml")
        if (excludeFile.exists()) {
            excludeFilter.set(excludeFile)
        }
    }
}
