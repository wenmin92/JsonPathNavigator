# scripts/

此目录包含开发辅助脚本，**不参与项目构建和 CI**，仅用于本地开发时快捷执行常用任务。

---

## 目录

- [test-versions.ps1 — 多版本 IDE 兼容性测试](#test-versionsps1--多版本-ide-兼容性测试)
  - [版本列表](#版本列表)
  - [执行流程](#执行流程)
  - [参数说明](#参数说明)
  - [使用示例](#使用示例)
  - [测试单个特定版本](#测试单个特定版本)
  - [使用本地已安装的 IDE](#使用本地已安装的-ide)
- [run-performance-test.ps1 — 性能测试](#run-performance-testps1--性能测试)
- [与直接使用 Gradle 的区别](#与直接使用-gradle-的区别)
- [常见问题排查](#常见问题排查)

---

## `test-versions.ps1` — 多版本 IDE 兼容性测试

对多个 IntelliJ IDEA 版本依次运行自动化测试和手动验证 (`runIde`)。

### 版本列表

脚本内置两组版本：

| 组别 | 版本 | 说明 |
|------|------|------|
| **核心版本** (`core`) | 2024.1.7, 2024.3.5, 2025.1.3, 2025.3.3, 2026.1 | 每个大版本系列的代表版本 |
| **扩展版本** | 2024.2.6, 2025.2.3 | 额外的补丁版本 |
| **全部版本** (`full` / `all`) | 以上全部共 7 个 | 核心 + 扩展 |

### 执行流程

脚本分为**两个阶段**，部分步骤需要人工介入：

**阶段一：自动化测试（无需干预）**
1. 运行 `gradlew test` — 单元测试，全自动
2. 运行 `gradlew test --tests JsonSearchPerformanceTest` — 性能测试，全自动（`-Quick` 模式下跳过）

**阶段二：IDE 兼容性验证（需要手动操作）**
- 对每个目标版本依次执行 `gradlew runIde`，**会弹出一个 IntelliJ IDEA 窗口**
- 你需要在 IDE 中手动验证插件功能，**验证完毕后手动关闭 IDE 窗口**，脚本才会继续测试下一个版本
- 使用 `-Manual` 参数时，终端会额外显示测试检查清单，关闭 IDE 后还需按 Enter 确认

### 参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `-Version` | `string` | `all` | 测试的版本组：`core`（5个）、`full`（7个）、`all`（7个） |
| `-Quick` | `switch` | 关闭 | 跳过性能测试，加速验证流程 |
| `-Clean` | `switch` | 关闭 | 测试前先执行 `gradlew clean` |
| `-Report` | `switch` | 关闭 | 测试完成后生成覆盖率报告并在浏览器中打开 |
| `-Manual` | `switch` | 关闭 | 每个 IDE 启动后显示检查清单，关闭 IDE 后需按 Enter 才继续 |

参数可以任意组合使用。

### 使用示例

```powershell
# 测试所有版本（默认，7 个版本）
.\scripts\test-versions.ps1

# 只测试核心版本（5 个：2024.1 / 2024.3 / 2025.1 / 2025.3 / 2026.1）
.\scripts\test-versions.ps1 -Version core

# 完整版本（核心 + 扩展，共 7 个）
.\scripts\test-versions.ps1 -Version full

# 快速模式：跳过性能测试，直接进入 IDE 验证阶段
.\scripts\test-versions.ps1 -Quick

# 清理构建后再测试
.\scripts\test-versions.ps1 -Clean

# 生成覆盖率报告并在浏览器中打开
.\scripts\test-versions.ps1 -Report

# 手动验证模式：每个 IDE 启动后显示检查清单，关闭 IDE 后按 Enter 继续
.\scripts\test-versions.ps1 -Manual

# 组合示例：清理构建 + 只测核心版本 + 跳过性能测试 + 手动验证模式
.\scripts\test-versions.ps1 -Version core -Clean -Quick -Manual

# 组合示例：核心版本 + 测试完生成报告
.\scripts\test-versions.ps1 -Version core -Report
```

### 测试单个特定版本

脚本本身**不支持**直接指定单个版本号（如 `2025.1.3`）。若只想验证某一个版本，有以下两种方式：

**方式一：直接使用 Gradle（推荐）**

```powershell
# 测试指定版本（会弹出 IDE 窗口，手动关闭后结束）
.\gradlew.bat runIde "-PplatformVersion=2025.1.3"

# 测试其他版本示例
.\gradlew.bat runIde "-PplatformVersion=2024.3.5"
.\gradlew.bat runIde "-PplatformVersion=2026.1"
```

> 注意：PowerShell 中含点号的 `-P` 参数需要加引号，否则 Gradle 会解析错误。

**方式二：使用本地 IDE（跳过下载）**

若本机已安装目标版本的 IDE，在 `gradle.properties` 中设置路径后直接运行：

```powershell
# gradle.properties 中：
# platformLocalPath=C:/Program Files/JetBrains/IntelliJ IDEA 2025.1

.\gradlew.bat runIde
```

### 使用本地已安装的 IDE

首次运行会从网络下载各版本 IDE（每个约 500MB～2GB），耗时较长。若本机已安装目标版本，可配置为直接使用本地安装，**跳过下载**。

**步骤：编辑 `gradle.properties`**

```properties
# 取消注释并填写本机 IDE 安装路径
platformLocalPath=C:/Program Files/JetBrains/IntelliJ IDEA 2025.3
```

路径须指向 IDE 根目录（包含 `bin`、`lib` 等子目录），不需要包含版本号。

配置后，无论 `-PplatformVersion` 传入什么值，都会优先使用本地路径。

---

## `run-performance-test.ps1` — 性能测试

单独运行 `JsonSearchPerformanceTest`，无需启动完整测试套件。

```powershell
# 完整性能测试
.\scripts\run-performance-test.ps1

# 快速模式：只运行第一组性能测试（01_test*）
.\scripts\run-performance-test.ps1 -Quick
```

---

## 与直接使用 Gradle 的区别

这两个脚本本质上是 `gradlew` 命令的封装，提供参数化和彩色输出。
如果你更习惯直接使用 Gradle，完全可以跳过这些脚本：

```powershell
# 单元测试
.\gradlew.bat test

# 性能测试（等价于 run-performance-test.ps1）
.\gradlew.bat test --tests "cc.wenmin92.jsonkeyfinder.performance.JsonSearchPerformanceTest" --rerun-tasks

# 测试指定版本（等价于 test-versions.ps1 单版本）
.\gradlew.bat runIde "-PplatformVersion=2025.3.3"

# 清理并测试
.\gradlew.bat clean runIde "-PplatformVersion=2025.1.3"

# 生成覆盖率报告
.\gradlew.bat koverHtmlReport

# 验证插件结构
.\gradlew.bat verifyPluginStructure
```

更多测试说明参见 `docs/TESTING.md`。

---

## 常见问题排查

### 下载时出现 404 错误

版本号不存在于 JetBrains 仓库。请确认使用的是完整的修订号（如 `2025.1.3`，而非 `2025.1`），或查阅 [Build Number Ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html) 确认可用版本。

解决方法：配置 `platformLocalPath` 使用本地已安装的 IDE。

### 出现 `OutOfMemoryError: Java heap space`

Gradle 内存不足，无法处理大型 IDE 插件（如 `kotlin-plugin.jar`）。

在 `gradle.properties` 中增加内存限制：

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
org.gradle.workers.max=2
```

### Kotlin 版本不兼容

错误信息类似：`binary version of its metadata is 2.3.0, expected version is 2.1.0`

原因是项目的 Kotlin 编译器版本低于目标 IDE 内置的 Kotlin 版本。需要同步更新 `gradle/libs.versions.toml` 和 `settings.gradle.kts` 中的 Kotlin 版本。

### IDE 窗口未出现 / 进程已退出

- 确认 `JAVA_HOME` 或 `org.gradle.java.home` 指向有效的 JDK 17～21
- 检查 `build/idea-sandbox/` 目录是否存在残留锁文件，可执行 `.\gradlew.bat clean` 清理后重试
