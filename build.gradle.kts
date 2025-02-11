import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.tasks.BuildSearchableOptionsTask
import org.jetbrains.intellij.tasks.RunIdeTask

plugins {
    id("java") // Java support
    id("org.jetbrains.kotlin.jvm") // Kotlin support
    id("org.jetbrains.intellij") // IntelliJ Platform Gradle Plugin
    id("org.jetbrains.changelog") // Gradle Changelog Plugin
    id("org.jetbrains.qodana") // Gradle Qodana Plugin
    id("org.jetbrains.kotlinx.kover") // Gradle Kover Plugin
}

group = "cc.wenmin92.jsonpathnavigator"
version = "1.0.5"

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Configure IntelliJ Plugin
intellij {
    version.set(providers.gradleProperty("platformVersion"))
    type.set(providers.gradleProperty("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(providers.gradleProperty("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) })
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    withType<BuildSearchableOptionsTask>().configureEach {
        enabled = false
    }

    patchPluginXml {
        version.set(providers.gradleProperty("pluginVersion"))
        sinceBuild.set(providers.gradleProperty("pluginSinceBuild"))
        untilBuild.set(providers.gradleProperty("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        })

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes.set(providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        })
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    test {
        useJUnitPlatform()
    }

    withType<RunIdeTask> {
        autoReloadPlugins.set(true)
        jvmArgs("-Xmx2g")
        // IDE VM options
        jvmArgs(
            "-Drobot-server.port=8082",
            "-Dide.mac.message.dialogs.as.sheets=false",
            "-Djb.privacy.policy.text=<!--999.999-->",
            "-Djb.consents.confirmation.enabled=false"
        )
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl.set(providers.gradleProperty("pluginRepositoryUrl"))
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}
