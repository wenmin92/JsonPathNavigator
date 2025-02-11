rootProject.name = "jsonpathnavigator"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.22"
        id("org.jetbrains.intellij") version "1.17.1"
        id("org.jetbrains.changelog") version "2.2.0"
        id("org.jetbrains.qodana") version "0.1.13"
        id("org.jetbrains.kotlinx.kover") version "0.7.5"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
