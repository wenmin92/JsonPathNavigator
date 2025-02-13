rootProject.name = "jsonpathnavigator"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.1.10"
        id("org.jetbrains.intellij") version "1.17.4"
        id("org.jetbrains.changelog") version "2.2.1"
        id("org.jetbrains.qodana") version "0.1.13"
        id("org.jetbrains.kotlinx.kover") version "0.9.1"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
