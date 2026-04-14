import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.BuildSearchableOptionsTask
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.intellij.platform.gradle.tasks.PatchPluginXmlTask

fun resolveIdeType(version: String): IntelliJPlatformType {
    val major = version.substringBefore(".").toIntOrNull() ?: 0
    val minor = version.substringAfter(".").substringBefore(".").toIntOrNull() ?: 0
    return if (major > 2025 || (major == 2025 && minor >= 3)) {
        IntelliJPlatformType.IntellijIdeaUltimate
    } else {
        IntelliJPlatformType.IntellijIdeaCommunity
    }
}

/**
 * 自 2024.3 起 JSON 语言支持从平台核心拆成独立 bundled plugin，编译时必须把 [com.intellij.modules.json]
 * 加入 Gradle 的 bundledPlugins，否则 com.intellij.json.* 不在 classpath（见 API Changes 2024.3）。
 * 2024.1/2024.2 仍内嵌在平台 SDK 中，显式添加会解析失败，故按版本区分。
 */
fun needsExplicitJsonBundledPlugin(platformVersion: String): Boolean {
    val parts = platformVersion.trim().split(".")
    if (parts.size < 2) return false
    val year = parts[0].toIntOrNull() ?: return false
    val minor = parts[1].toIntOrNull() ?: return false
    if (year > 2024) return true
    return year == 2024 && minor >= 3
}

plugins {
    id("java") // Java support
    id("org.jetbrains.kotlin.jvm") // Kotlin support
    id("org.jetbrains.intellij.platform") version "2.6.0" // IntelliJ Platform Gradle Plugin 2.x
    id("org.jetbrains.changelog") // Gradle Changelog Plugin
    id("org.jetbrains.qodana") // Gradle Qodana Plugin
    id("org.jetbrains.kotlinx.kover") // Gradle Kover Plugin
}

group = "cc.wenmin92.jsonpathnavigator"
version = project.property("pluginVersion") as String

// Set the JVM language level used to build the project.
// Uses Gradle Toolchain to automatically download JDK 17 if not available locally
val javaVersion = (project.findProperty("javaToolchainVersion") as? String)?.toIntOrNull() ?: 17

kotlin {
    jvmToolchain(javaVersion)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

// Configure project's dependencies
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // JUnit 4 for IntelliJ Platform Test Framework (BasePlatformTestCase uses JUnit 4)
    testImplementation("junit:junit:4.13.2")
    // JUnit 5 for standalone unit tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // JUnit Vintage for running JUnit 4 tests with JUnit 5 runner
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.4")
    
    intellijPlatform {
        val localIdePath = (project.findProperty("platformLocalPath") as? String)?.trim().orEmpty()
        val pv = project.property("platformVersion") as String
        if (localIdePath.isNotEmpty()) {
            // 使用本机已安装的 IDE，避免 Gradle 从 CDN 下载大体积安装包（参见 docs/TESTING.md）
            local(localIdePath)
        } else {
            create(resolveIdeType(pv), pv)
        }
        val baseBundled = (project.property("platformBundledPlugins") as String)
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
        val bundled = if (localIdePath.isEmpty() && needsExplicitJsonBundledPlugin(pv)) {
            (baseBundled + "com.intellij.modules.json").distinct()
        } else {
            baseBundled
        }
        bundledPlugins(bundled)

        
        pluginVerifier()
        zipSigner()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

// Configure IntelliJ Platform Plugin Verification
intellijPlatform {
    pluginVerification {
        ides {
            // 与 JetBrains Marketplace 官方验证版本保持一致（正式发布版本，可自动下载）
            // 2025.3+ 使用统一发行版 (IU)，不再有独立的 Community Edition
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1.7")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.2.6")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3.7")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1.7")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.2.6.1")
            ide(IntelliJPlatformType.IntellijIdeaUltimate, "2025.3.4")
            // 2026.1.1 为 EAP 版本，不在标准仓库中，无法自动下载。
            // 若本机已安装该版本，可在 gradle.properties 中设置 verifyLocalIdePath2026
            // 并取消下方注释来启用本地验证：
            // local((project.findProperty("verifyLocalIdePath2026") as? String) ?: "")
        }
    }
}

tasks {
    wrapper {
        gradleVersion = project.property("gradleVersion") as String
    }

    withType<BuildSearchableOptionsTask>().configureEach {
        enabled = false
    }

    patchPluginXml {
        version = project.property("pluginVersion") as String
        sinceBuild = project.property("pluginSinceBuild") as String
        if (project.hasProperty("pluginUntilBuild")) {
            untilBuild = project.property("pluginUntilBuild") as String
        }

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = with(changelog) {
            renderItem(
                (getOrNull(project.property("pluginVersion") as String) ?: getUnreleased())
                    .withHeader(false)
                    .withEmptySections(false),
                Changelog.OutputType.HTML,
            )
        }
    }

    runIde {
        // Disable all instrumentation agents to prevent ClassNotFoundException
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Dkotlinx.coroutines.debug=off",
                "-Djava.instrument.debug=false",
                "-Xnoagent"
            )
        }
    }

    signPlugin {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        token = System.getenv("PUBLISH_TOKEN")
    }

    test {
        useJUnitPlatform()
    }

    withType<RunIdeTask> {
        jvmArgs("-Xmx2g")
        // IDE VM options
        jvmArgs(
            "-Drobot-server.port=8082",
            "-Dide.mac.message.dialogs.as.sheets=false",
            "-Djb.privacy.policy.text=<!--999.999-->",
            "-Djb.consents.confirmation.enabled=false"
        )
    }
    
    // 创建一个任务来验证最新版本
    register("verifyPluginLatest") {
        group = "verification"
        description = "验证插件与最新 IDE 版本的兼容性"
        
        doLast {
            println("=".repeat(60))
            println("验证最新 IDE 版本的说明:")
            println("=".repeat(60))
            println("1. 在 build.gradle.kts 中取消注释以下行:")
            println("   // ide(IntelliJPlatformType.IntellijIdeaCommunity, \"2024.2.6\")")
            println("2. 或者添加其他版本，如:")
            println("   ide(IntelliJPlatformType.IntellijIdeaCommunity, \"2024.3.5\")")
            println("3. 然后运行: ./gradlew verifyPlugin")
            println("=".repeat(60))
            println("注意: 首次下载新版本 IDE 可能需要较长时间")
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = project.property("pluginRepositoryUrl") as String
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
