# Json Path Navigator 测试指南

本文档详细介绍了项目的测试体系，帮助开发者理解、运行和扩展测试。

---

## 目录

1. [测试概述](#1-测试概述)
2. [快速开始](#2-快速开始)
3. [测试结构](#3-测试结构)
4. [运行测试](#4-运行测试)
5. [IDE 手动测试 (runIde)](#5-ide-手动测试-runide)
   - [5.6.1 IDE 构件与下载地址（进阶）](#advanced-ide-artifact-download)
6. [插件兼容性验证 (verifyPlugin)](#6-插件兼容性验证-verifyplugin)
7. [编写新测试](#7-编写新测试)
8. [测试覆盖率](#8-测试覆盖率)
9. [测试最佳实践](#9-测试最佳实践)
10. [Gradle 缓存机制](#10-gradle-缓存机制)
11. [常见问题](#11-常见问题)

---

## 1. 测试概述

### 1.1 测试框架

本项目使用以下测试框架和工具：

| 工具 | 版本 | 用途 |
|------|------|------|
| JUnit 4 | 4.13.2 | IntelliJ Platform 测试基类依赖 |
| JUnit 5 | 5.11.4 | 现代测试运行器 |
| IntelliJ Platform Test Framework | - | 提供 `BasePlatformTestCase` 等测试基类 |
| Kover | 0.9.1 | 代码覆盖率分析 |

### 1.2 测试类型

```
┌─────────────────────────────────────────────────────────────┐
│                      测试金字塔                               │
├─────────────────────────────────────────────────────────────┤
│                    ┌─────────┐                              │
│                    │ UI 测试  │  ← 手动测试 (runIde)          │
│                   ─┴─────────┴─                             │
│                 ┌───────────────┐                           │
│                 │   集成测试     │  ← 多组件协作               │
│               ──┴───────────────┴──                         │
│             ┌───────────────────────┐                       │
│             │       单元测试         │  ← 单个类/方法          │
│           ──┴───────────────────────┴──                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 快速开始

### 2.1 环境要求

- **JDK 17** (必需，Gradle 构建和测试运行。不支持更高版本的 JDK)
- **Gradle 8.10.2** (已包含在项目中，使用 gradlew)

> **Windows 用户注意**：如果系统默认 JDK 不是 17，需要在运行命令前设置 `JAVA_HOME`：
> ```powershell
> $env:JAVA_HOME = "C:\path\to\jdk17"
> ```
> 或者取消 `gradle.properties` 中 `org.gradle.java.home` 行的注释并设置正确路径。

### 2.2 运行所有测试

```powershell
# Windows
.\gradlew.bat test

# macOS/Linux
./gradlew test
```

### 2.3 查看测试报告

测试完成后，打开以下文件查看报告：

```
build/reports/tests/test/index.html
```

---

## 3. 测试结构

### 3.1 目录结构

```
src/test/
├── kotlin/
│   └── cc/wenmin92/jsonkeyfinder/
│       ├── JsonPathTestBase.kt              # 测试基类，提供辅助方法
│       ├── JsonPathNavigatorIntegrationTest.kt  # 集成测试
│       ├── actions/
│       │   └── FindJsonKeyActionTest.kt     # Action 行为测试
│       └── service/
│           ├── JsonSearchServiceTest.kt     # 核心服务完整测试
│           └── JsonSearchServiceSimpleTest.kt   # 使用基类的简化测试
└── resources/
    └── testData/
        ├── simple.json                      # 简单 JSON 测试数据
        ├── nested.json                      # 嵌套 JSON 测试数据
        └── complex.json                     # 复杂 JSON 测试数据
```

### 3.2 测试类说明

| 测试类 | 测试数量 | 描述 |
|--------|----------|------|
| `JsonSearchServiceTest` | 25 | 核心搜索服务的完整单元测试 |
| `JsonSearchServiceSimpleTest` | 11 | 使用测试基类的简化测试示例 |
| `FindJsonKeyActionTest` | 11 | Action 启用/禁用、路径匹配等测试 |
| `JsonPathNavigatorIntegrationTest` | 10 | 端到端集成测试 |

---

## 4. 运行测试

### 4.1 基本命令

```powershell
# 运行所有测试
.\gradlew.bat test

# 运行测试并显示详细输出
.\gradlew.bat test --info

# 运行单个测试类
.\gradlew.bat test --tests "cc.wenmin92.jsonkeyfinder.service.JsonSearchServiceTest"

# 运行单个测试方法
.\gradlew.bat test --tests "cc.wenmin92.jsonkeyfinder.service.JsonSearchServiceTest.test findKey with simple key"

# 运行匹配模式的测试
.\gradlew.bat test --tests "*ServiceTest"
```

### 4.2 跳过测试

```powershell
# 构建但跳过测试
.\gradlew.bat build -x test
```

### 4.3 强制重新运行测试

Gradle 有智能缓存机制：如果代码没有变化，会跳过任务并使用缓存结果。这意味着**报告时间不会更新**。

```powershell
# 方法 1：清除所有缓存后重新运行（最彻底）
.\gradlew.bat clean test

# 方法 2：强制重新运行所有任务（不清除构建产物）
.\gradlew.bat test --rerun-tasks

# 方法 3：只强制重新运行测试任务
.\gradlew.bat test --rerun
```

**常见场景**：
- 查看报告发现时间是昨天的 → 使用 `--rerun-tasks` 重新生成
- 怀疑缓存有问题 → 使用 `clean` 彻底清理

### 4.4 在 IDE 中运行测试

1. 打开任意测试类
2. 点击测试方法或类旁边的绿色运行按钮
3. 或右键选择 "Run 'TestName'"

---

## 5. IDE 手动测试 (runIde)

`runIde` 命令启动一个**独立的 IntelliJ IDEA 实例**，其中已经加载了你的插件。这让你可以像真实用户一样测试插件功能。

### 5.1 启动测试 IDE

```powershell
.\gradlew.bat runIde
```

**运行后会发生什么？**

```
┌─────────────────────────────────────────────────────────────────┐
│  1. Gradle 构建并打包插件                                         │
│       ↓                                                         │
│  2. 下载 IntelliJ IDEA Community Edition (首次运行，约 500MB)      │
│       ↓                                                         │
│  3. 在临时沙盒目录创建 IDE 配置                                     │
│       ↓                                                         │
│  4. 启动 IDE，插件已自动安装                                       │
│       ↓                                                         │
│  5. 一个新的 IntelliJ IDEA 窗口打开！                              │
└─────────────────────────────────────────────────────────────────┘
```

**首次运行需要下载 IDE（约 500MB），请耐心等待。**

### 5.2 在测试 IDE 中进行手动测试

IDE 启动后，你需要**创建或打开一个测试项目**来测试插件功能。

#### 步骤 1：创建测试项目

```
1. File → New → Project
2. 选择 "Empty Project" 或任意项目类型
3. 设置项目名称（如 "test-plugin"）和位置
4. 点击 Create
```

#### 步骤 2：创建测试 JSON 文件

在项目中创建一些 JSON 文件用于测试：

```
test-plugin/
├── config/
│   ├── app.json
│   └── database.json
└── data/
    └── settings.json
```

**app.json** 示例内容：
```json
{
  "app": {
    "name": "TestApp",
    "version": "1.0.0",
    "settings": {
      "debug": true,
      "logLevel": "info"
    }
  }
}
```

**database.json** 示例内容：
```json
{
  "database": {
    "host": "localhost",
    "port": 5432,
    "credentials": {
      "username": "admin",
      "password": "secret"
    }
  }
}
```

#### 步骤 3：测试插件功能

**测试 1：快捷键搜索**
```
1. 打开任意文件
2. 按 Ctrl+Alt+Shift+F (Windows) 或 ⌘+⇧+⌥+F (Mac)
3. 输入 "database.host"
4. 验证：应该显示搜索结果
5. 双击结果，验证：应该跳转到 database.json 文件的对应行
```

**测试 2：右键菜单搜索**
```
1. 在任意代码文件中输入文本 "app.settings.debug"
2. 选中这段文本
3. 右键 → 选择 "Find Key in JSON"
4. 验证：搜索对话框应该打开，且搜索框已填入选中的文本
```

**测试 3：搜索建议**
```
1. 打开搜索对话框
2. 输入 "data"
3. 验证：下方应该显示建议列表（如 database.host, database.port 等）
4. 点击任意建议
5. 验证：应该自动执行搜索
```

**测试 4：多文件搜索**
```
1. 在 app.json 和 settings.json 中都添加相同的键路径
2. 搜索这个路径
3. 验证：结果应该显示来自多个文件的匹配项
```

**测试 5：无结果情况**
```
1. 搜索一个不存在的路径，如 "nonexistent.key"
2. 验证：应该显示 "No results found" 提示
```

### 5.3 测试检查清单

在发布新版本前，建议完成以下测试：

| 测试项 | 操作 | 预期结果 | ✓ |
|--------|------|----------|---|
| 快捷键 | Ctrl+Alt+Shift+F | 打开搜索对话框 | ☐ |
| 右键菜单 | 选中文本 → 右键 | 显示 "Find Key in JSON" | ☐ |
| 简单搜索 | 搜索 "name" | 找到匹配结果 | ☐ |
| 嵌套搜索 | 搜索 "a.b.c" | 找到嵌套键 | ☐ |
| 无结果 | 搜索不存在的键 | 显示无结果提示 | ☐ |
| 建议功能 | 输入部分路径 | 显示建议列表 | ☐ |
| 结果跳转 | 双击搜索结果 | 跳转到文件对应行 | ☐ |
| 多文件 | 搜索存在于多文件的键 | 显示所有文件的结果 | ☐ |
| 空搜索 | 不输入任何内容点搜索 | 显示错误提示 | ☐ |
| ESC 关闭 | 按 ESC 键 | 关闭对话框 | ☐ |

### 5.4 测试 IDE 的配置

测试 IDE 使用独立的配置目录（沙盒），不会影响你的日常 IDE 设置：

```
配置位置: build/idea-sandbox/
├── config/     # IDE 配置
├── plugins/    # 插件（你的插件在这里）
└── system/     # 系统缓存
```

**清除沙盒重新开始：**
```powershell
.\gradlew.bat clean
```

### 5.5 调试模式运行

如果需要调试插件代码：

```powershell
# 1. 启动测试 IDE（会在特定端口监听调试连接）
.\gradlew.bat runIde

# 2. 在你的开发 IDE 中：
#    Run → Attach to Process → 选择测试 IDE 进程

# 3. 在代码中设置断点，然后在测试 IDE 中触发功能
```

### 5.6 指定 IDE 版本进行手动测试

默认情况下，`runIde` 使用 `gradle.properties` 中 `platformVersion` 指定的 IDE 版本（当前为 2024.1.7）。要测试其他版本，有两种方式：

**方式 1：命令行参数覆盖（推荐，不修改文件）**

```powershell
# 使用 IntelliJ IDEA 2024.3 进行测试
.\gradlew.bat runIde "-PplatformVersion=2024.3"

# 使用 IntelliJ IDEA 2025.1 进行测试
.\gradlew.bat runIde "-PplatformVersion=2025.1"

# 使用 IntelliJ IDEA 2025.3 进行测试（2025.3 起使用统一发行版）
.\gradlew.bat runIde "-PplatformVersion=2025.3"

# 使用 IntelliJ IDEA 2026.1 进行测试
.\gradlew.bat runIde "-PplatformVersion=2026.1"
```

> **PowerShell 注意**：参数必须加双引号（如 `"-PplatformVersion=2026.1"`），否则 PowerShell 会把版本号中的 `.1` 解析为单独的参数导致报错。

> **2025.3+ 统一发行版**：从 2025.3 开始，IntelliJ IDEA 不再提供独立的 Community Edition 构件，改为统一发行版（Ultimate，但免费功能等同于 Community）。构建脚本已自动处理此切换，无需额外配置。

> **2024.3+ 与 JSON API**：自 2024.3 起，`com.intellij.json.*` 等类来自独立的 bundled plugin，不再随平台 SDK 自动进编译 classpath。本仓库的 `build.gradle.kts` 会在 `platformVersion` 为 2024.3 及更高时**自动**把 `com.intellij.modules.json` 加入 `bundledPlugins`；若你仍看到大量 `Unresolved reference 'json'`，请确认未覆盖掉该逻辑，并检查 `-PplatformVersion` 是否带引号。

> **首次下载**：首次使用新版本时，Gradle 需要下载对应的 IDE（约 500MB-2GB），请耐心等待。下载的 IDE 会被缓存在 `~/.gradle` 中，后续启动会快很多。如果下载卡住，检查 `gradle.properties` 中代理配置是否正确。若要查看**实际下载 URL**、构件命名含义、Gradle 诊断命令与官方文档链接，见 [5.6.1 IDE 构件与下载地址（进阶）](#advanced-ide-artifact-download)。

**下载失败时常见报错（不完整下载）**

若出现类似下面的错误：

```text
Premature end of Content-Length delimited message body
(expected: 1,579,722,173; received: 1,206,648,832)
```

含义是：**HTTP 连接在传完整个文件之前就被断开了**。服务器声明文件大小约 1.58GB，但只收到了约 1.21GB，因此这次下载**无效**，Gradle 不会把不完整的 zip 当作可用缓存。

- **是否下载成功**：没有。必须完整下载后才会解压并缓存。
- **再次运行为何还会下**：因为上次没有成功写入完整缓存，Gradle 会重新尝试下载；若网络仍不稳定，可能反复失败。

**缓解办法**：配置 HTTP 代理（见本仓库 `gradle.properties` 中 `systemProp.http.proxyHost` 示例）、换网络、或使用下面「本机 IDE 目录」方式完全跳过该次下载。

**方式 1b：使用本机已安装的 IDE（推荐网络差时）**

若你已通过 [JetBrains 官网](https://www.jetbrains.com/idea/download/)、Toolbox 或其他方式**完整安装**了某一版本的 IntelliJ IDEA，可以让 Gradle **直接使用该安装目录**，不再下载 `ideaIU-*.zip`。

1. 在 `gradle.properties` 中设置 `platformLocalPath`，值为 IDE **根目录**（其下应有 `bin`、`lib` 等文件夹）：

   ```properties
   # Windows 示例（路径按你本机安装位置修改）
   platformLocalPath=C:/Program Files/JetBrains/IntelliJ IDEA 2026.1

   # macOS 示例
   # platformLocalPath=/Applications/IntelliJ IDEA.app
   ```

2. 保存后执行 `.\gradlew.bat runIde`（仍会加载当前工程里的插件到沙盒）。

3. 测完其他版本时，**删除或注释** `platformLocalPath` 一行，即可恢复为按 `platformVersion` 在线解析（或再改 `platformVersion`）。

> **`platformLocalPath` 是 zip 还是解压后的目录？**  
> 必须是**解压后的 IDE 根目录**（与官网安装版、Toolbox 安装后的目录结构相同：其下有 `bin`、`lib` 等），**不能**填 `.zip` 文件路径。  
> **Gradle 在线下载**时：由 IntelliJ Platform Gradle Plugin 负责下载并在 Gradle 缓存中处理，**不需要**你手动解压任何 zip。  
> **若你自行从官网下载了 `ideaIU-*.win.zip`**：需要先**解压到任意目录**，再把 `platformLocalPath` 指向该目录下解压出来的那一层（即能看到 `bin` 的那一层）。

> **说明**：`platformLocalPath` 与 `-PplatformVersion=...` 同时存在时，以本地目录为准（不再为该任务下载对应 zip）。`verifyPlugin` 仍会按 `build.gradle.kts` 里配置的各版本 IDE 做校验，与本地路径无关。

**方式 2：临时修改 `gradle.properties`**

```properties
# 修改 platformVersion 为目标版本
platformVersion = 2025.3
```

修改后运行 `.\gradlew.bat runIde`，测试完成后记得改回原来的版本。

<a id="advanced-ide-artifact-download"></a>

#### 5.6.1 进阶：IDE 构件从哪来、如何查看实际下载地址

新人常在终端里看到类似 `Resolve ... ideaIU-2025.3.win` 的进度，但不清楚**具体从哪个 URL 下载**、**和官网安装包是什么关系**。下面把这些概念串起来，便于自行排查网络问题或对照官方发布物。

**终端里的 `ideaIU-2025.3.win` 表示什么**

- `ideaIU`：IntelliJ IDEA **Ultimate** 产品线对应的平台构件前缀（本仓库在 2025.3+ 使用统一发行版时由 `build.gradle.kts` 中的 `resolveIdeType` 选择 IU 类型）。
- `2025.3`：你传入的 **发布线版本**（与 `platformVersion` / `-PplatformVersion` 一致）。
- `.win`：Windows 平台变体。Gradle 与 IntelliJ Platform Gradle Plugin 会据此从 JetBrains 配置的仓库里解析**一个具体的 zip 构件**（完整坐标与构建号由插件与仓库元数据决定，不一定等于你在官网看到的「安装包」文件名）。

**如何得知本次构建实际使用的下载 URL**

Gradle 在默认日志级别下**往往不打印**完整 HTTP 地址，可以用下面方式查看：

1. **加 `--info`（首选）或 `--debug`（更啰嗦）**  
   在下载阶段日志中查找 **`Downloading https://...`**（或重定向后的最终地址）。示例：

   ```powershell
   .\gradlew.bat runIde "-PplatformVersion=2025.3" --info
   ```

2. **用 `dependencyInsight` 看解析结果**（看构件名与版本，辅助对照）：

   ```powershell
   .\gradlew.bat dependencyInsight --dependency ideaIU --configuration intellijPlatformDependency "-PplatformVersion=2025.3"
   ```

**与「猜官网直链」的关系**

- 很多 Windows zip 的形态类似 `https://download.jetbrains.com/idea/ideaIU-<版本>.win.zip`，但若 JetBrains 实际发布的是 **补丁线**（如 `2025.3.1`），文件名或路径可能带第三段版本号。
- **不要仅凭命名规律当作唯一依据**；以 **`--info` 里打印的 URL**、[JetBrains 下载页](https://www.jetbrains.com/idea/download/) 上给出的链接，或仓库元数据为准。

**进一步阅读（官方文档）**

| 主题 | 链接 |
|------|------|
| 平台构件与仓库约定 | [IntelliJ Platform Artifacts Repositories](https://plugins.jetbrains.com/docs/intellij/intellij-artifacts.html) |
| IntelliJ Platform Gradle Plugin 2.x（依赖、`create`、本地 IDE） | [Tools: IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html) |
| `dependencies { intellijPlatform { ... } }` 扩展 | [Dependencies Extension](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html) |

**缓存位置**

- 首次下载成功后，对应 IDE 包会进入 **Gradle 用户目录下的缓存**（Windows 常见为 `C:\Users\<用户名>\.gradle\caches\` 下由插件管理的条目），之后同一 `platformVersion` 会复用，启动会快很多。
- 若下载**不完整**（例如中途断线），缓存不会被视为可用，下次仍会重新拉取；这与「缓解办法」见本节上文「下载失败时常见报错」。

**完全避免 Gradle 下载 zip**

- 使用上文 **方式 1b：`platformLocalPath`**，指向本机已完整安装的 IDE **解压/安装后的根目录**（不是 `.zip` 文件）；若只有官网下载的 zip，须先解压再填路径。详见方式 1b 下的说明块。

**附：JDK 与 Gradle 的 native 访问警告**

若使用较新的 JDK 运行 Gradle，可能出现 `Restricted method in java.lang.System` / `System::load` 之类**与 native 库加载相关**的警告（例如来自 Gradle 自带的 native 组件）。这通常**与 IDE zip 的下载地址无关**；若需消除警告，可按提示为 JVM 增加 `--enable-native-access=ALL-UNNAMED`，或查阅当前 Gradle / JDK 发行说明。团队规范以公司环境为准。

### 5.7 手动测试的 IDE 版本矩阵

发布新版本前，建议在以下关键版本上进行手动测试：

| IDE 版本 | 构建号前缀 | 关注点 | 优先级 |
|----------|-----------|--------|--------|
| 2024.1.x | 241 | 最低支持版本，基础功能验证 | 高 |
| 2024.3.x | 243 | JSON 模块从平台核心分离为独立 bundled plugin，需确保 JSON 相关功能正常 | 高 |
| 2025.3.x | 253 | 2025 年最新稳定版 | 中 |
| 2026.1 | 261 | 最新发布版本，验证前沿兼容性 | 中 |

**每个版本上至少需要验证的核心功能**：

1. 插件能正常加载（不报错）
2. 快捷键 `Ctrl+Alt+Shift+F` 能打开搜索对话框
3. JSON 键搜索能返回正确结果
4. 双击搜索结果能跳转到目标文件
5. 右键菜单中显示 "Find Key in JSON"

**特别注意**：2024.3+ 版本是一个重要的分界线，因为 JSON 模块在此版本从平台核心中提取为独立的 bundled plugin。如果测试时间有限，至少要在 2024.1.x（旧架构）和 2024.3+（新架构）各测一个版本。

### 5.8 终止测试 IDE

- **正常关闭**: 关闭 IDE 窗口，或 File → Exit
- **强制终止**: 在运行 Gradle 的终端中按 `Ctrl+C`，然后输入 `Y` 确认

---

## 6. 插件兼容性验证 (verifyPlugin)

`verifyPlugin` 命令检查你的插件是否与不同版本的 IntelliJ 平台兼容。这对于确保插件能在用户的各种 IDE 版本上正常工作非常重要。

### 6.1 运行兼容性验证

```powershell
.\gradlew.bat verifyPlugin
```

### 6.2 验证过程详解

```
┌─────────────────────────────────────────────────────────────────┐
│  verifyPlugin 执行过程                                           │
├─────────────────────────────────────────────────────────────────┤
│  1. 构建插件 ZIP 包                                               │
│       ↓                                                         │
│  2. 下载 IntelliJ Plugin Verifier 工具                           │
│       ↓                                                         │
│  3. 下载配置的目标 IDE 版本（可能多个）                              │
│       ↓                                                         │
│  4. 对每个 IDE 版本执行验证：                                      │
│      - 检查 API 兼容性                                           │
│      - 检查已废弃 API 的使用                                      │
│      - 检查内部 API 的使用                                        │
│      - 检查依赖问题                                               │
│       ↓                                                         │
│  5. 生成验证报告                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**首次运行需要下载验证工具和目标 IDE（每个约 500MB），非常耗时！**

### 6.3 配置验证的目标 IDE 版本

在 `build.gradle.kts` 中配置：

```kotlin
intellijPlatform {
    pluginVerification {
        ides {
            // 当前开发基准版本
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1.7")
            // JSON 模块分离的关键版本
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3")
            // 各主要版本
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.3")
            // 最新发布版本
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2026.1")
        }
    }
}
```

`verifyPlugin` 与手动测试（`runIde`）的关系：
- `verifyPlugin`：**静态分析**，自动检查 API 兼容性、弃用 API 使用、依赖解析等，不需要人工操作
- `runIde`：**运行时测试**，启动真实 IDE 实例，需要人工操作验证功能是否正常
- 两者互补：`verifyPlugin` 能发现编译级别的兼容问题，但无法测试运行时行为；`runIde` 可以验证实际体验

### 6.4 理解验证报告

验证完成后，报告位于：

```
build/reports/pluginVerifier/
```

**报告类型：**

| 类型 | 说明 | 处理方式 |
|------|------|----------|
| **Compatibility problems** | 严重的 API 兼容问题 | ❌ 必须修复 |
| **Deprecated API usage** | 使用了已废弃的 API | ⚠️ 建议修复 |
| **Experimental API usage** | 使用了实验性 API | ⚠️ 了解风险 |
| **Internal API usage** | 使用了内部 API | ⚠️ 可能在未来版本中断 |
| **Plugin structure warnings** | 插件结构问题 | ⚠️ 建议修复 |

### 6.5 常见兼容性问题及解决方案

**问题 1: 方法不存在**
```
Invocation of unresolved method com.intellij.xxx.SomeClass.someMethod()
```
**解决**: 这个方法在目标 IDE 版本中不存在，需要：
- 调整 `pluginSinceBuild` 版本
- 或使用条件代码处理不同版本

**问题 2: 类不存在**
```
Access to unresolved class com.intellij.xxx.SomeClass
```
**解决**: 该类在目标版本中不存在或被移动

**问题 3: 已废弃 API**
```
Deprecated API usage: com.intellij.xxx.deprecatedMethod()
```
**解决**: 查看 IDE 源码或文档，找到替代 API

### 6.6 只验证结构（快速检查）

如果只想检查插件结构而不做完整兼容性验证：

```powershell
.\gradlew.bat verifyPluginStructure
```

这个命令很快，检查：
- plugin.xml 格式是否正确
- 依赖是否声明
- 必需字段是否存在

### 6.7 验证插件签名

如果插件需要签名：

```powershell
.\gradlew.bat verifyPluginSignature
```

---

## 7. 编写新测试

### 7.1 基本测试模板

```kotlin
package cc.wenmin92.jsonkeyfinder.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MyNewTest : BasePlatformTestCase() {

    private lateinit var searchService: JsonSearchService

    override fun setUp() {
        super.setUp()
        searchService = JsonSearchService(project)
    }

    fun `test my feature works correctly`() {
        // Arrange - 准备测试数据
        val jsonContent = """
            {
                "key": "value"
            }
        """.trimIndent()
        myFixture.configureByText("test.json", jsonContent)

        // Act - 执行被测方法
        val results = searchService.findKey("key")

        // Assert - 验证结果
        assertEquals(1, results.size)
        assertEquals("key", results[0].path)
    }
}
```

### 7.2 使用测试基类（推荐）

```kotlin
package cc.wenmin92.jsonkeyfinder.service

import cc.wenmin92.jsonkeyfinder.JsonPathTestBase

class MySimplifiedTest : JsonPathTestBase() {

    fun `test simple search`() {
        // 使用辅助方法创建 JSON 文件
        createSimpleJsonFile("test.json", "name", "TestValue")
        
        // 使用辅助断言方法
        assertSingleResult("name")
        assertPreviewContains("name", "TestValue")
    }

    fun `test nested path`() {
        createNestedJsonFile("config.json", "app.settings.theme", "dark")
        assertSingleResult("app.settings.theme")
    }

    fun `test no results`() {
        createSimpleJsonFile("test.json", "exists", "value")
        assertNoResults("notExists")
    }
}
```

### 7.3 测试基类提供的辅助方法

```kotlin
// 创建 JSON 文件
createJsonFile(fileName: String, content: String): VirtualFile
createSimpleJsonFile(fileName: String, key: String, value: Any): VirtualFile
createNestedJsonFile(fileName: String, path: String, value: Any): VirtualFile

// 断言方法
assertSingleResult(searchPath: String, expectedPath: String = searchPath)
assertNoResults(searchPath: String)
assertResultCount(searchPath: String, expectedCount: Int)
assertPreviewContains(searchPath: String, expectedText: String)

// 工具方法
refreshCache()  // 刷新搜索服务缓存
```

### 7.4 测试命名规范

使用反引号语法编写描述性测试名称：

```kotlin
// ✅ 好的命名
fun `test findKey returns empty list when key does not exist`()
fun `test search with unicode characters in value`()
fun `test cache is invalidated after file change`()

// ❌ 避免的命名
fun testFindKey1()
fun test_search()
```

---

## 8. 测试覆盖率

### 8.1 生成覆盖率报告

```powershell
# 生成 HTML 报告
.\gradlew.bat koverHtmlReport

# 生成 XML 报告（用于 CI 集成）
.\gradlew.bat koverXmlReport

# 强制重新生成报告（避免使用缓存的旧数据）
.\gradlew.bat koverHtmlReport --rerun-tasks

# 清理后重新生成（最彻底）
.\gradlew.bat clean koverHtmlReport
```

> ⚠️ **注意**：Gradle 会缓存任务结果。如果代码没有变化，覆盖率报告不会重新生成，
> 报告中的时间戳也不会更新。使用 `--rerun-tasks` 或 `clean` 可以强制重新生成。

### 8.2 查看报告

```
HTML 报告: build/reports/kover/html/index.html
XML 报告:  build/reports/kover/report.xml
```

### 8.3 当前覆盖率

| 模块 | 覆盖率 | 说明 |
|------|--------|------|
| JsonSearchService | ~80% | 核心业务逻辑，覆盖率高 |
| FindJsonKeyAction | ~60% | Action 逻辑 |
| JsonKeyFinderDialog | ~10% | UI 代码，需要 UI 测试框架 |

### 8.4 提高覆盖率

要提高覆盖率，可以：

1. **为边界条件添加测试**
   ```kotlin
   fun `test findKey with empty project`()
   fun `test findKey with malformed JSON`()
   ```

2. **为异常情况添加测试**
   ```kotlin
   fun `test search handles IOException gracefully`()
   ```

3. **添加 UI 测试**（使用 Remote-Robot）
   ```kotlin
   // 需要额外配置，参见 JetBrains Remote-Robot 文档
   ```

---

## 9. 测试最佳实践

### 9.1 测试设计原则

```
┌────────────────────────────────────────────────────────────┐
│                    FIRST 原则                               │
├────────────────────────────────────────────────────────────┤
│  F - Fast (快速)       测试应该快速运行                       │
│  I - Independent (独立) 测试之间不应相互依赖                   │
│  R - Repeatable (可重复) 每次运行结果应该一致                  │
│  S - Self-validating (自验证) 测试应自动判断通过/失败          │
│  T - Timely (及时)     测试应该及时编写                       │
└────────────────────────────────────────────────────────────┘
```

### 9.2 AAA 模式

每个测试方法应遵循 AAA 模式：

```kotlin
fun `test example using AAA pattern`() {
    // Arrange - 准备
    val jsonContent = """{"key": "value"}"""
    createJsonFile("test.json", jsonContent)

    // Act - 执行
    val results = searchService.findKey("key")

    // Assert - 断言
    assertEquals(1, results.size)
}
```

### 9.3 避免的反模式

```kotlin
// ❌ 测试太多东西
fun `test everything`() {
    // 测试 A 功能
    // 测试 B 功能
    // 测试 C 功能
}

// ✅ 每个测试专注一个功能
fun `test feature A`() { ... }
fun `test feature B`() { ... }
fun `test feature C`() { ... }

// ❌ 硬编码的魔法数字
assertEquals(42, results.size)

// ✅ 使用有意义的常量或变量
val expectedCount = files.size
assertEquals(expectedCount, results.size)
```

---

## 10. Gradle 缓存机制

Gradle 为了提高构建速度，会智能缓存任务的执行结果。理解这个机制对于正确使用测试命令非常重要。

### 10.1 什么是任务缓存？

```
┌─────────────────────────────────────────────────────────────────┐
│  Gradle 任务执行逻辑                                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  运行 .\gradlew.bat test                                         │
│       ↓                                                         │
│  检查：输入文件是否有变化？                                         │
│       ↓                                                         │
│  ┌─────────┐              ┌─────────┐                           │
│  │ 有变化   │              │ 无变化   │                           │
│  └────┬────┘              └────┬────┘                           │
│       ↓                        ↓                                │
│  执行测试任务              跳过任务，显示 "UP-TO-DATE"              │
│  生成新报告                使用缓存的报告（时间不变）                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 10.2 如何识别缓存行为

运行命令时，观察任务状态：

```
> Task :test UP-TO-DATE      ← 使用缓存，没有重新运行
> Task :test                 ← 重新运行了
> Task :test FROM-CACHE      ← 从构建缓存恢复
```

### 10.3 强制重新运行的方法

| 方法 | 命令 | 说明 |
|------|------|------|
| 重新运行所有任务 | `--rerun-tasks` | 忽略所有缓存，重新执行所有任务 |
| 重新运行单个任务 | `--rerun` | 只重新运行指定的任务 |
| 清理后运行 | `clean <task>` | 删除构建输出后重新运行 |
| 禁用构建缓存 | `--no-build-cache` | 本次运行不使用构建缓存 |

**各命令的强制重新运行方式：**

```powershell
# 测试
.\gradlew.bat test --rerun-tasks
.\gradlew.bat clean test

# 覆盖率
.\gradlew.bat koverHtmlReport --rerun-tasks
.\gradlew.bat clean koverHtmlReport

# 手动测试 IDE（通常不需要，因为每次都会启动新进程）
.\gradlew.bat runIde

# 兼容性验证
.\gradlew.bat verifyPlugin --rerun-tasks
.\gradlew.bat clean verifyPlugin
```

### 10.4 报告时间不正确的解决方案

**症状**：运行测试后，查看报告发现生成时间是昨天或更早

**原因**：Gradle 认为没有代码变化，跳过了任务

**解决**：
```powershell
# 强制重新运行
.\gradlew.bat test --rerun-tasks

# 或清理后重新运行
.\gradlew.bat clean test
```

### 10.5 何时应该使用强制重新运行

| 场景 | 建议 |
|------|------|
| 日常开发，代码有修改 | 直接运行，Gradle 会自动检测变化 |
| 想要最新的报告时间 | 使用 `--rerun-tasks` |
| 怀疑缓存有问题 | 使用 `clean` |
| CI/CD 环境 | 通常使用 `clean build` |
| 切换分支后 | 建议使用 `clean` |

---

## 11. 常见问题

### 11.1 测试运行失败："Cannot access junit.framework.TestCase"

**原因**: 缺少 JUnit 4 依赖

**解决**: 确保 `build.gradle.kts` 包含：
```kotlin
testImplementation("junit:junit:4.13.2")
```

### 11.2 测试运行失败："25.0.1" 错误

**原因**: Java 版本过高，IntelliJ Platform Plugin 不支持

**解决**: 在 `gradle.properties` 中配置 JDK 17：
```properties
org.gradle.java.home=C:/Users/你的用户名/scoop/apps/openjdk17/current
```

### 11.3 测试通过但有警告信息

常见警告及其含义：

| 警告 | 含义 | 是否需要处理 |
|------|------|-------------|
| `test-log.properties does not exist` | 日志配置文件缺失 | ❌ 可忽略 |
| `ActionUpdater - ms total to grab EDT` | IDE 内部性能警告 | ❌ 可忽略 |
| `Deprecated Gradle features` | Gradle 版本兼容性提示 | ⚠️ 未来版本需关注 |

### 11.4 如何调试测试

1. **在 IDE 中调试**
   - 在测试方法中设置断点
   - 右键测试方法 → "Debug 'TestName'"

2. **添加日志输出**
   ```kotlin
   fun `test with debug output`() {
       val results = searchService.findKey("key")
       println("Found ${results.size} results")  // 会显示在测试输出中
       results.forEach { println("  - ${it.path}") }
   }
   ```

### 11.5 测试数据在哪里

测试数据文件位于：
```
src/test/resources/testData/
```

您也可以在测试中动态创建测试数据：
```kotlin
myFixture.configureByText("test.json", """{"key": "value"}""")
```

---

## 附录 A：完整命令参考

### A.1 单元测试命令

| 命令 | 说明 | 耗时 |
|------|------|------|
| `.\gradlew.bat test` | 运行所有单元测试 | ~1-2 分钟 |
| `.\gradlew.bat test --info` | 运行测试并显示详细日志 | ~1-2 分钟 |
| `.\gradlew.bat test --tests "ClassName"` | 只运行指定测试类 | ~30 秒 |
| `.\gradlew.bat test --tests "*Test.test*"` | 运行匹配模式的测试 | 视情况 |
| `.\gradlew.bat test --rerun-tasks` | 强制重新运行测试（忽略缓存） | ~1-2 分钟 |
| `.\gradlew.bat clean test` | 清理缓存后重新测试 | ~2-3 分钟 |

### A.2 覆盖率命令

| 命令 | 说明 | 输出位置 |
|------|------|----------|
| `.\gradlew.bat koverHtmlReport` | 生成 HTML 覆盖率报告 | `build/reports/kover/html/index.html` |
| `.\gradlew.bat koverXmlReport` | 生成 XML 覆盖率报告（用于 CI） | `build/reports/kover/report.xml` |
| `.\gradlew.bat koverHtmlReport --rerun-tasks` | 强制重新生成报告 | 同上 |
| `.\gradlew.bat clean koverHtmlReport` | 清理后重新生成报告 | 同上 |

### A.3 手动测试命令

| 命令 | 说明 | 注意事项 |
|------|------|----------|
| `.\gradlew.bat runIde` | 启动带插件的测试 IDE | 首次运行需下载 IDE (~500MB) |
| `.\gradlew.bat runIde "-PplatformVersion=2024.3"` | 使用指定 IDE 版本启动 | 首次需下载对应版本 |
| `.\gradlew.bat runIde "-PplatformVersion=2025.3"` | 使用 2025.3 版本启动 | 2025.3+ 自动使用统一发行版 |
| `.\gradlew.bat runIde "-PplatformVersion=2026.1"` | 使用最新版本启动 | 首次需下载对应版本 |
| `.\gradlew.bat runIde --debug-jvm` | 以调试模式启动测试 IDE | 可附加调试器 |

### A.4 兼容性验证命令

| 命令 | 说明 | 耗时 |
|------|------|------|
| `.\gradlew.bat verifyPlugin` | 完整兼容性验证 | ~5-10 分钟（需下载多个 IDE） |
| `.\gradlew.bat verifyPlugin --rerun-tasks` | 强制重新验证 | ~5-10 分钟 |
| `.\gradlew.bat verifyPluginStructure` | 只验证插件结构 | ~10 秒 |
| `.\gradlew.bat verifyPluginSignature` | 验证插件签名 | ~10 秒 |
| `.\gradlew.bat verifyPluginProjectConfiguration` | 验证项目配置 | ~10 秒 |

### A.5 构建命令

| 命令 | 说明 | 输出位置 |
|------|------|----------|
| `.\gradlew.bat build` | 完整构建（含测试） | - |
| `.\gradlew.bat build -x test` | 构建但跳过测试 | - |
| `.\gradlew.bat buildPlugin` | 构建插件 ZIP 包 | `build/distributions/*.zip` |
| `.\gradlew.bat clean` | 清理所有构建输出 | - |

### A.6 缓存控制命令

| 命令/选项 | 说明 |
|-----------|------|
| `--rerun-tasks` | 强制重新运行所有任务，忽略缓存 |
| `--rerun` | 强制重新运行指定任务 |
| `--no-build-cache` | 本次运行禁用构建缓存 |
| `clean` | 删除 build 目录下的所有输出 |
| `.\gradlew.bat --stop` | 停止所有 Gradle 守护进程（清理内存缓存） |

### A.7 其他有用命令

| 命令 | 说明 |
|------|------|
| `.\gradlew.bat tasks` | 列出所有可用任务 |
| `.\gradlew.bat tasks --group="intellij platform"` | 列出 IntelliJ 相关任务 |
| `.\gradlew.bat dependencies` | 显示项目依赖树 |
| `.\gradlew.bat --status` | 查看 Gradle 守护进程状态 |

---

## 附录 B：测试输出说明

### B.1 测试报告结构

```
build/reports/tests/test/
├── index.html              # 主报告页面
├── css/                    # 样式文件
├── js/                     # JavaScript 文件
├── classes/                # 每个测试类的详细报告
│   ├── ...Test.html
│   └── ...
└── packages/               # 按包分组的报告
    └── ...
```

### B.2 覆盖率报告结构

```
build/reports/kover/
├── html/
│   ├── index.html          # HTML 覆盖率报告主页
│   └── ...
└── report.xml              # XML 格式报告（用于 CI 工具）
```

### B.3 验证报告结构

```
build/reports/pluginVerifier/
├── YYYY-MM-DD-HH-MM-SS/    # 按时间戳分组
│   ├── IC-2024.1.7/        # 每个 IDE 版本的结果
│   │   ├── verification-verdict.txt    # 验证结论
│   │   ├── compatibility-problems.txt  # 兼容性问题
│   │   └── ...
│   └── IC-2025.1/
│       └── ...
└── ...
```

---

*最后更新: 2026-04-02*
