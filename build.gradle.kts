import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object Versions {

    const val jackson = "2.13.4"
    const val prometheus = "0.16.0"
    const val jda = "5.0.0-alpha.20"
    const val hibernate = "6.1.3.Final"
    const val postgres = "42.5.0"
    const val hoplite = "2.6.3"
}

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.cascadebot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}


dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlin.coreLibrariesVersion}")
    implementation("io.prometheus:simpleclient:${Versions.prometheus}")
    implementation("io.prometheus:simpleclient_logback:${Versions.prometheus}")
    implementation("io.prometheus:simpleclient_hotspot:${Versions.prometheus}")
    implementation("net.dv8tion:JDA:${Versions.jda}")
    implementation("com.github.minndevelopment:jda-ktx:fc7d7de")
    implementation("club.minnced:discord-webhooks:0.8.2")
    implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.2")
    implementation("org.hibernate:hibernate-core:${Versions.hibernate}")
    implementation("org.hibernate:hibernate-hikaricp:${Versions.hibernate}")
    implementation("com.vladmihalcea:hibernate-types-60:2.19.2")
    implementation("org.postgresql:postgresql:${Versions.postgres}")
    implementation("org.flywaydb:flyway-core:9.3.0")
    implementation("ch.qos.logback:logback-classic:1.4.1")
    implementation("io.sentry:sentry-logback:6.4.2")
    implementation("com.sksamuel.hoplite:hoplite-core:${Versions.hoplite}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${Versions.hoplite}")
    implementation("com.sksamuel.hoplite:hoplite-hikaricp:${Versions.hoplite}")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.rabbitmq:amqp-client:5.16.0")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:${Versions.jackson}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson}")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")
    implementation("com.auth0:java-jwt:4.0.0")

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