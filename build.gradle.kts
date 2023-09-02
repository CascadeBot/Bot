import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object Versions {

    const val jackson = "2.15.2"
    const val prometheus = "0.16.0"
    const val jda = "5.0.0-beta.13"
    const val hibernate = "6.2.7.Final"
    const val postgres = "42.6.0"
    const val hoplite = "2.7.4"
}

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    idea
}

group = "org.cascadebot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlin.coreLibrariesVersion}")
    implementation("io.prometheus:simpleclient:${Versions.prometheus}")
    implementation("io.prometheus:simpleclient_logback:${Versions.prometheus}")
    implementation("io.prometheus:simpleclient_hotspot:${Versions.prometheus}")
    implementation("net.dv8tion:JDA:${Versions.jda}")
    implementation("com.github.minndevelopment:jda-ktx:0.10.0-beta.1")
    implementation("club.minnced:discord-webhooks:0.8.4")
    implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4")
    implementation("org.hibernate:hibernate-core:${Versions.hibernate}")
    implementation("org.hibernate:hibernate-hikaricp:${Versions.hibernate}")
    implementation("com.vladmihalcea:hibernate-types-60:2.21.1")
    implementation("org.postgresql:postgresql:${Versions.postgres}")
    implementation("org.flywaydb:flyway-core:9.21.1")
    implementation("ch.qos.logback:logback-classic:1.4.9")
    implementation("io.sentry:sentry-logback:6.28.0")
    implementation("com.sksamuel.hoplite:hoplite-core:${Versions.hoplite}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${Versions.hoplite}")
    implementation("com.sksamuel.hoplite:hoplite-hikaricp:${Versions.hoplite}")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.rabbitmq:amqp-client:5.18.0")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:${Versions.jackson}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.jackson}")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.7")
    implementation("com.auth0:java-jwt:4.4.0")

}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.jar {
    archiveFileName.set("${project.name}.${archiveExtension.get()}")

    // Set the main class to be a runnable jar
    manifest {
        attributes["Main-Class"] = "${project.group}.${project.name}.Main"
    }
}

tasks.shadowJar {
    archiveBaseName.set("${project.name}-shadow")
    archiveClassifier.set("")
    archiveVersion.set("")

    mergeServiceFiles()

    doLast {
        File(buildDir, "/libs/version.txt").writeText(project.version.toString())
    }
}