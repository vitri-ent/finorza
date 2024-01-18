@file:Suppress("UnstableApiUsage")

import com.google.protobuf.gradle.*

plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.protobuf)
    `java-library`
}

group = "io.pyke.vitri"
version = "1.0.1-SNAPSHOT"
description = "A Minecraft mod for simulating autonomous agents."

repositories {
    mavenCentral()
    maven("https://maven.parchmentmc.org")
    maven("https://maven.terraformersmc.com/releases")
}

val includeImplementation by configurations.registering
val includeRuntimeOnly by configurations.registering

configurations {
    implementation.get().extendsFrom(includeImplementation.get())
    runtimeOnly.get().extendsFrom(includeRuntimeOnly.get())

    include.get().extendsFrom(includeImplementation.get(), includeRuntimeOnly.get())
}

dependencies {
    minecraft(libs.fabric.minecraft)
    mappings(loom.layered {
        officialMojangMappings()
        parchment(
            libs.fabric.parchment.map { parchment ->
                parchment.apply {
                    artifact {
                        type = "zip" // Parchment is dumb and needs to use a non-standard artifact extension
                    }
                }
            }
        )
    })

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.modmenu)

    // gRPC's Netty and Guava are not included, since Minecraft has them out of the box
    // they are older than the dependency expects, but ABI should be ok for what we need

    compileOnly(libs.tomcat.annotations)
    includeImplementation(libs.grpc.api)
    includeImplementation(libs.grpc.core)
    includeImplementation(libs.grpc.context)
    includeImplementation(libs.grpc.stub)
    includeImplementation(libs.grpc.services)
    includeImplementation(libs.grpc.protobuf)
    includeImplementation(libs.grpc.protobuf.lite)
    includeImplementation(libs.grpc.netty)
    includeImplementation(libs.protobuf.java)
    includeRuntimeOnly(libs.perfmark.api)
}

loom {
    accessWidenerPath = file("src/main/resources/finorza.accesswidener")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
            }
            task.builtins {
                id("python")
            }
        }
    }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        val props = mapOf(
            "name" to project.name,
            "version" to project.version,
            "description" to project.description
        )

        inputs.properties(props)
        filesMatching("fabric.mod.json") {
            expand(props)
        }
    }

    build {
        dependsOn(processResources)
    }
}
