// Holds the convention plugin(s) shared by every implementation module. Kept as an included
// build so the plugins are precompiled Kotlin scripts with full type-safe accessors.

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Lets the convention plugin apply the Kotlin/JVM plugin (`plugins { kotlin("jvm") }`).
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")
}
