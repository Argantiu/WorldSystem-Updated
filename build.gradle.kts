val plugingroup = "de.cycodly"
val pluginname = "WorldSystem"
val pluginauthors = "[Butzlabben, Trainerlord, Cycodly]"
val pluginversion = "2.4.40"
val plugindescription = "Worldsystem - Let players create thier own worlds"
val pluginapiversion = "1.16"
val pluginminecraft = "1.21.4"
val plugindepend = "[WorldEdit]"
val pluginsoftdepend = "[PlaceholderAPI, Vault, Chunky]"

plugins {
    id("com.gradleup.shadow") version "9.0.2"
    id("io.freefair.lombok") version "8.14"
    id("java")
    id("jacoco")
    id("base")
}

base {
    archivesName = pluginname
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    annotationProcessor(libs.lombok)
    implementation(libs.commonsio)
    implementation(libs.minimessage)
    implementation(libs.bstatsBukkit) { isTransitive = false }
    implementation(libs.bstatsBase) { isTransitive = false }
    compileOnly(libs.spigotapi)
    compileOnly(libs.lombok)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.vaultapi)
    compileOnly(libs.authlib)
    compileOnly(libs.worldedit)
    compileOnly(libs.fawe)
    compileOnly(libs.chunky)
    testImplementation(libs.jupiter)
    testImplementation(libs.mockito)
    testImplementation(libs.assertj)
    testImplementation(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

configurations.all {
    resolutionStrategy {
        force(libs.gson)
        force(libs.guava)
    }
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(
            "pluginname" to pluginname,
            "group" to plugingroup,
            "version" to pluginversion,
            "authors" to pluginauthors,
            "description" to plugindescription,
            "apiversion" to pluginapiversion,
            "depend" to plugindepend,
            "softdepend" to pluginsoftdepend
        )
    }
    from(sourceSets.main.get().resources.srcDirs) {
        include("plugin.yml")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

tasks.shadowJar {
    minimize()
    archiveClassifier.set("")
    archiveFileName.set("$pluginname-$pluginversion.jar")
    dependencies {
        exclude(dependency("commons-io:commons-io"))
        exclude(dependency("net.kyori:adventure-text-minimessage"))
        relocate("org.bstats", "de.cycodly.worldsystem.bstats") {
            include(dependency("org.bstats:"))
        }
    }
    
}

tasks.withType<Javadoc> {
    source = sourceSets.main.get().allJava
    destinationDir = file("build/javadocs")
    include("**/api/*")
    options {
        (this as? StandardJavadocDocletOptions)?.apply {
            links(
                "https://javadoc.io/static/org.jetbrains/annotations/20.1.0/",
                "https://docs.oracle.com/javase/21/docs/api/",
                "https://papermc.io/javadocs/paper/$pluginminecraft/"
            )
        }
    }
}

tasks.withType<JavaCompile> {
    options.isDeprecation = false
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
    options.isFork = true
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    ignoreFailures = false
}

tasks.jar {
    archiveFileName.set("$pluginname-$pluginversion-noShade.jar")  
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

project.defaultTasks("build")