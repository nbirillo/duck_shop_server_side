plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    // Build a GraalVM native image with ./gradlew nativeCompile (requires a GraalVM JDK).
    id("org.graalvm.buildtools.native") version "0.10.3"
}

group = "org.jetbrains.kotlin.course"
version = "0.0.1-SNAPSHOT"

// Generates META-INF/build-info.properties so /actuator/info reports the app version.
springBoot {
    buildInfo()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus") // /actuator/prometheus

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.postgresql:postgresql")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ---- Frontend integration ----
// Builds the React frontend and bundles its output into the server jar under /static,
// so the single artifact serves both the API and the UI. Runs automatically before
// processResources (used by bootJar, assemble and bootRun).
// Skip with: ./gradlew assemble -PskipFrontend  (jar will then contain no UI)
val frontendDir = file("../frontend")
val frontendBuildDir = frontendDir.resolve("build")
val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
fun npm(vararg args: String) = listOf(if (isWindows) "npm.cmd" else "npm", *args)

val installFrontend = tasks.register<Exec>("installFrontend") {
    group = "frontend"
    description = "Installs npm dependencies (only when node_modules is missing)."
    workingDir = frontendDir
    commandLine(npm("install"))
    onlyIf { !frontendDir.resolve("node_modules").exists() }
}

val buildFrontend = tasks.register<Exec>("buildFrontend") {
    group = "frontend"
    description = "Builds the React frontend into frontend/build."
    dependsOn(installFrontend)
    workingDir = frontendDir
    environment("CI", "false") // treat CRA warnings as warnings, not errors
    inputs.dir(frontendDir.resolve("src"))
    inputs.dir(frontendDir.resolve("public"))
    inputs.files(
        frontendDir.resolve("package.json"),
        frontendDir.resolve("package-lock.json"),
        frontendDir.resolve("tsconfig.json"),
    )
    outputs.dir(frontendBuildDir)
    commandLine(npm("run", "build"))
}

if (!providers.gradleProperty("skipFrontend").isPresent) {
    tasks.processResources {
        dependsOn(buildFrontend)
        from(frontendBuildDir) { into("static") }
    }
}
