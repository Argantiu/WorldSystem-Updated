plugins {
    id("com.gradleup.shadow") version "8.3.8"
    id("io.freefair.lombok") version "8.14"
    id("java")
    id("jacoco")
    id("base")
    `java-library`
    `maven-publish`
    id("io.github.0ffz.github-packages") version "1.2.1"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.13"
}

repositories {
    gradlePluginPortal()
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    annotationProcessor("org.jetbrains:annotations-java5:24.1.0")
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    compileOnly("it.unimi.dsi:fastutil:8.5.11")
    implementation("xyz.xenondevs.invui:invui:1.45")
    annotationProcessor(libs.lombok)
    implementation(libs.commonsio)
    implementation(libs.minimessage)
    implementation(libs.bstatsBukkit) { isTransitive = false }
    implementation(libs.bstatsBase) { isTransitive = false }
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

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

group = "de.dertoaster"
version = "3.0.0-TTE"
description = "WorldSystem-TTE"
java.toolchain.languageVersion = JavaLanguageVersion.of(21)


tasks.jar {
    archiveBaseName.set("WorldSystem-TTE")
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.processResources {
    from(rootProject.file("LICENSE.md"))
    filesMatching("*.yml") {
        expand(mapOf("projectVersion" to project.version))
    }
}
