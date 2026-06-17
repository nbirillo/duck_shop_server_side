// The Ktor "comparison" port of duckShop (Module 9). A standalone Gradle build that lives
// next to the Spring `server/` module but is completely independent of it — building or running
// this never touches the Spring app. It implements a subset of /api/ducks (GET/PUT/POST) so the
// lecture can show the same shop on a lightweight stack: Ktor routing, kotlinx.serialization,
// Exposed, and Ktor Authentication.

val ktorVersion = "3.2.0"
val exposedVersion = "0.61.0"
val logbackVersion = "1.5.12"

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    application
}

group = "org.jetbrains.kotlin.course"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server + Netty engine
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    // JSON via kotlinx.serialization (compare: Spring uses Jackson)
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    // Auth, error mapping, CORS, request logging
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")

    // Exposed (compare: Spring Data JPA) + file-based H2
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.h2database:h2:2.3.232")

    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation(kotlin("test"))
}

application {
    mainClass = "org.jetbrains.kotlin.course.duck.ktor.ApplicationKt"
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
