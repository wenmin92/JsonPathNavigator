# Json Path Navigator - 构建与发布完整指南

本文档详细说明如何从源码构建插件产物，并将其发布到 JetBrains Marketplace 的完整流程。

---

## 目录

1. [概述](#1-概述)
2. [准备工作](#2-准备工作)
3. [本地构建](#3-本地构建)
4. [自动发布（推荐）](#4-自动发布推荐)
5. [手动发布](#5-手动发布)
6. [发布验证](#6-发布验证)
7. [故障排除](#7-故障排除)

---

## 1. 概述

### 1.1 发布方式对比

| 方式 | 复杂度 | 可靠性 | 适用场景 |
|------|--------|--------|----------|
| **GitHub Actions 自动发布** | 低 | 高 | ✅ **推荐用于正式版本发布** |
| **本地构建 + 手动上传** | 中 | 中 | 紧急修复、测试版本 |
| **本地构建 + 命令行发布** | 高 | 低 | 高级用户、自动化脚本 |

### 1.2 发布流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                     准备阶段                                      │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐ │
│  │ 更新版本号       │ →  │ 更新 CHANGELOG  │ →  │ 提交代码     │ │
│  │ gradle.properties│    │ CHANGELOG.md    │    │ git push    │ │
│  └─────────────────┘    └─────────────────┘    └──────┬──────┘ │
│                                                       │         │
├───────────────────────────────────────────────────────┼─────────┤
│                     构建阶段                           │         │
│  ┌────────────────────────────────────────────────────┘         │
│  ↓                                                               │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐  │
│  │ GitHub Actions  │ →  │ 运行测试         │ →  │ 验证插件     │  │
│  │ 自动触发         │    │ 代码检查         │    │ 兼容性       │  │
│  └────────┬────────┘    └─────────────────┘    └──────┬──────┘  │
│           │                                           │         │
│           └───────────────────────────────────────────┘         │
│                           ↓                                     │
│                  ┌─────────────────┐                           │
│                  │ 创建 Draft Release│                          │
│                  │ (等待手动发布)     │                          │
│                  └────────┬────────┘                           │
│                           ↓                                     │
├─────────────────────────────────────────────────────────────────┤
│                     发布阶段                                      │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐  │
│  │ 点击发布 Release│ →  │ 自动发布到       │ →  │ 用户可更新   │  │
│  │ (GitHub)        │    │ JetBrains        │    │ 插件         │  │
│  │                 │    │ Marketplace      │    │              │  │
│  └─────────────────┘    └─────────────────┘    └─────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 准备工作

### 2.1 环境要求

**必需软件：**
- **JDK 17+** - 用于编译 Kotlin 代码
- **Git** - 版本控制
- **IntelliJ IDEA** - 推荐用于开发和验证

**验证环境：**
```powershell
# 检查 Java 版本
java -version
# 应显示: openjdk version "17" 或更高

# 检查 Git
git --version
```

### 2.2 项目配置检查

在构建前，请检查以下配置文件：

#### 2.2.1 gradle.properties - 版本配置

```properties
# 文件位置: E:\GitHub\JsonPathNavigator\gradle.properties

# 插件版本号 - 每次发布必须更新
pluginVersion=1.0.11

# 支持的最低 IDE 版本
pluginSinceBuild=241

# 支持的最高 IDE 版本（可选，留空表示无上限）
# pluginUntilBuild=251

# 开发使用的平台版本
platformVersion=2024.1.7
```

**重要提示：**
- `pluginVersion` 必须遵循语义化版本规范（如 1.0.11）
- 每次发布前必须递增版本号
- `pluginSinceBuild` 对应 IDE 的内部版本号（241 = 2024.1）

#### 2.2.2 CHANGELOG.md - 更新日志

```markdown
# 文件位置: E:\GitHub\JsonPathNavigator\CHANGELOG.md

## [Unreleased]

## [1.0.11] - 2026-04-13

### Added
- 新增功能描述

### Fixed
- 修复的问题描述

### Changed
- 变更说明
```

**必填内容：**
- `[Unreleased]` 部分必须存在（可以为空）
- 新版本必须有日期
- 使用标准分类：Added, Fixed, Changed, Removed, Deprecated

#### 2.2.3 README.md - 插件描述

确保 README.md 包含插件描述标记：

```markdown
<!-- Plugin description -->
## Json Path Navigator

A JetBrains IDE plugin for quickly finding JSON keys in your project.

**Features:**
- Quick search for JSON keys
- Smart suggestions
- Value preview
...
<!-- Plugin description end -->
```

### 2.3 JetBrains Marketplace 配置

**首次发布前必须完成：**

1. **获取 PUBLISH_TOKEN**
   - 访问: https://plugins.jetbrains.com/author/me/tokens
   - 点击 **"Generate New Token"**
   - 名称: `JsonPathNavigator CI`
   - 权限: 勾选 ✅ **Upload plugins**
   - 点击 **Generate** 并复制 Token（只显示一次！）

2. **配置 GitHub Secrets**
   - 访问: https://github.com/wenmin92/JsonPathNavigator/settings/secrets/actions
   - 点击 **"New repository secret"**
   - Name: `PUBLISH_TOKEN`
   - Value: 刚才复制的 Token
   - 点击 **Add secret**

---

## 3. 本地构建

### 3.1 构建命令

**Windows:**
```powershell
# 进入项目目录
cd E:\GitHub\JsonPathNavigator

# 清理并构建插件
.\gradlew.bat clean buildPlugin
```

**Linux/Mac:**
```bash
./gradlew clean buildPlugin
```

### 3.2 构建输出

构建成功后，产物位置：
```
E:\GitHub\JsonPathNavigator\build\distributions\jsonpathnavigator-{version}.zip
```

示例：
```
build\distributions\jsonpathnavigator-1.0.11.zip
```

### 3.3 构建过程详解

```
构建任务流程:
├── :clean                          # 清理之前的构建
├── :initializeIntellijPlatformPlugin  # 初始化平台插件
├── :patchPluginXml                 # 更新 plugin.xml（插入版本、描述、更新日志）
├── :compileKotlin                  # 编译 Kotlin 代码
├── :processResources               # 处理资源文件
├── :instrumentCode                 # 代码插桩（用于 IDE 调试）
├── :jar                           # 打包 JAR
├── :buildSearchableOptions        # 构建可搜索选项（已禁用）
├── :prepareSandbox                # 准备沙箱环境
└── :buildPlugin                    # 最终构建插件 ZIP
```

### 3.4 本地安装测试

构建完成后，可以在本地 IDE 中测试：

1. **打开 IntelliJ IDEA**
2. **Settings** → **Plugins** → **⚙️ (齿轮图标)** → **Install from Disk...**
3. 选择 `build\distributions\jsonpathnavigator-{version}.zip`
4. 点击 **OK** → **Restart IDE**
5. 测试插件功能

### 3.5 高级构建选项

#### 3.5.1 跳过测试构建

```powershell
# 如果测试有问题，可以跳过
.\gradlew.bat clean buildPlugin -x test
```

#### 3.5.2 使用特定 JDK

```powershell
# 在 gradle.properties 中指定
org.gradle.java.home=C:/Program Files/Java/jdk-17
```

#### 3.5.3 清理 Gradle 缓存

```powershell
# 如果遇到奇怪的问题，清理缓存
.\gradlew.bat clean
rm -rf .gradle
```

---

## 4. 自动发布（推荐）

GitHub Actions 自动发布是最推荐的方式，可以确保构建环境一致性，并有完整的发布记录。

### 4.1 自动发布流程

```
代码推送 → Build Workflow → Draft Release → 手动发布 → Release Workflow → Marketplace
```

### 4.2 详细步骤

#### 步骤 1: 更新版本号

编辑 `gradle.properties`：
```properties
# 从旧版本
pluginVersion=1.0.10

# 更新为新版本
pluginVersion=1.0.11
```

版本号规则：
- **Patch** (1.0.10 → 1.0.11): Bug 修复
- **Minor** (1.0.x → 1.1.0): 新功能
- **Major** (1.x.x → 2.0.0): 破坏性变更

#### 步骤 2: 更新 CHANGELOG.md

在文件顶部添加新版本：

```markdown
## [Unreleased]

## [1.0.11] - 2026-04-13

### Added
- 支持 YAML 文件搜索
- 添加搜索结果导出功能

### Fixed
- 修复大文件搜索时的性能问题
- 修复中文路径显示乱码问题

### Changed
- 优化搜索算法，提升 50% 速度
- 更新 UI 样式
```

在文件底部添加版本链接：
```markdown
[1.0.11]: https://github.com/wenmin92/JsonPathNavigator/releases/tag/v1.0.11
```

#### 步骤 3: 提交并推送

```bash
# 添加更改
git add gradle.properties CHANGELOG.md

# 提交
git commit -m "chore: bump version to 1.0.11

Changes:
- Add YAML file support
- Fix performance issue with large files
- Optimize search algorithm"

# 推送到 main 分支
git push origin main
```

#### 步骤 4: 等待 Build Workflow

1. 访问: https://github.com/wenmin92/JsonPathNavigator/actions
2. 找到正在运行的 **Build** workflow
3. 等待完成（约 5-10 分钟）

Build Workflow 会执行：
- ✅ 验证 Gradle Wrapper
- ✅ 编译代码
- ✅ 代码质量检查（Qodana）
- ✅ 插件兼容性验证（Plugin Verifier）
- ✅ 构建插件 ZIP
- ✅ 创建 Draft Release

#### 步骤 5: 发布 Release

Build 成功后：

1. 访问: https://github.com/wenmin92/JsonPathNavigator/releases
2. 找到 Draft 状态的 Release（如 `v1.0.11`）
3. 点击 **Edit** 检查内容：
   - 版本号是否正确
   - 更新日志是否完整
   - 附件中是否有 ZIP 文件
4. 点击 **Publish release** 🚀

#### 步骤 6: 等待 Release Workflow

发布 Release 后会自动触发 **Release** workflow：

1. 访问: https://github.com/wenmin92/JsonPathNavigator/actions/workflows/release.yml
2. 等待完成（约 5 分钟）

Release Workflow 会执行：
- ✅ 重新构建插件
- ⚠️ 签名插件（跳过，未配置证书）
- ✅ 发布到 JetBrains Marketplace
- ✅ 上传产物到 GitHub Release

#### 步骤 7: 验证发布

**GitHub Release:**
- 访问: https://github.com/wenmin92/JsonPathNavigator/releases/tag/v1.0.11
- 确认 Assets 中有 `jsonpathnavigator-1.0.11.zip`

**JetBrains Marketplace:**
- 访问: https://plugins.jetbrains.com/plugin/cc.wenmin92.jsonpathnavigator
- 等待 5-15 分钟审核
- 确认新版本出现在版本列表中

**IDE 内验证:**
1. 打开 IntelliJ IDEA
2. Settings → Plugins → Marketplace
3. 搜索 "Json Path Navigator"
4. 检查是否显示新版本可用

### 4.3 GitHub Actions Workflow 配置

#### Build Workflow (.github/workflows/build.yml)

**触发条件:**
- Push 到 main 分支
- Pull Request

**主要任务:**
```yaml
name: Build
on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'corretto'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      - name: Build Plugin
        run: ./gradlew buildPlugin
      - name: Upload Artifact
        uses: actions/upload-artifact@v4
        with:
          name: plugin-artifact
          path: build/distributions/*.zip
```

#### Release Workflow (.github/workflows/release.yml)

**触发条件:**
- Release published

**主要任务:**
```yaml
name: Release
on:
  release:
    types: [published]

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'corretto'
      - name: Build Plugin
        run: ./gradlew buildPlugin
      - name: Publish Plugin
        env:
          PUBLISH_TOKEN: ${{ secrets.PUBLISH_TOKEN }}
        run: ./gradlew publishPlugin
      - name: Upload to Release
        uses: actions/upload-release-asset@v1
        with:
          upload_url: ${{ github.event.release.upload_url }}
          asset_path: build/distributions/*.zip
```

---

## 5. 手动发布

如果自动发布不可用，可以使用手动发布方式。

### 5.1 方式一：网页手动上传

**适用场景:** 快速测试、网络问题导致自动发布失败

**步骤:**

1. **本地构建插件**
   ```powershell
   .\gradlew.bat clean buildPlugin
   ```

2. **登录 JetBrains Marketplace**
   - 访问: https://plugins.jetbrains.com/author/me
   - 找到 Json Path Navigator 插件

3. **上传新版本**
   - 点击 **"Upload Update"**
   - 选择 `build\distributions\jsonpathnavigator-{version}.zip`
   - 填写更新说明（从 CHANGELOG.md 复制）
   - 选择兼容性版本范围
   - 点击 **"Upload"**

4. **等待审核**
   - 通常需要 5-15 分钟
   - 审核通过后会收到邮件通知

### 5.2 方式二：命令行发布

**适用场景:** 自动化脚本、CI/CD 集成

**步骤:**

1. **设置环境变量**
   ```powershell
   # Windows PowerShell
   $env:PUBLISH_TOKEN="your_token_here"
   ```

   ```bash
   # Linux/Mac
   export PUBLISH_TOKEN="your_token_here"
   ```

2. **执行发布命令**
   ```powershell
   .\gradlew.bat publishPlugin
   ```

**命令输出示例:**
```
> Task :publishPlugin
Publishing plugin Json Path Navigator v1.0.11
Uploading plugin...
Plugin uploaded successfully
Plugin ID: cc.wenmin92.jsonpathnavigator
Version: 1.0.11
```

### 5.3 手动发布注意事项

⚠️ **重要提示:**

1. **版本号一致性**
   - 本地构建的版本号必须与 Marketplace 上已发布的版本不同
   - 重复上传相同版本会失败

2. **签名问题**
   - 历史版本未签名，保持不签名策略
   - 如果从签名切换到不签名（或反之），用户需要卸载重装

3. **兼容性声明**
   - 手动上传时需要正确选择支持的 IDE 版本范围
   - 错误的声明会导致用户无法安装

---

## 6. 发布验证

### 6.1 构建产物验证

**检查 ZIP 文件结构:**
```powershell
# 解压检查
Expand-Archive -Path "build\distributions\jsonpathnavigator-1.0.11.zip" -DestinationPath "temp_check"

# 检查内容
tree temp_check /F
```

**预期结构:**
```
jsonpathnavigator-1.0.11/
├── lib/
│   ├── jsonpathnavigator-1.0.11.jar    # 主 JAR
│   ├── kotlin-stdlib-2.1.10.jar        # Kotlin 依赖
│   └── ...
├── META-INF/
│   └── plugin.xml                       # 插件配置
└── ...
```

**检查 plugin.xml:**
```xml
<idea-plugin>
    <version>1.0.11</version>
    <idea-version since-build="241"/>
    <description><![CDATA[...]]></description>
    <change-notes><![CDATA[...]]></change-notes>
</idea-plugin>
```

### 6.2 功能验证清单

**安装前验证:**
- [ ] ZIP 文件大小合理（通常 1-10 MB）
- [ ] plugin.xml 中版本号正确
- [ ] plugin.xml 中描述和更新日志完整
- [ ] lib 目录包含所有必要依赖

**安装后验证:**
- [ ] 插件成功加载（无启动错误）
- [ ] 快捷键可用（Ctrl+Alt+Shift+F）
- [ ] 搜索功能正常
- [ ] 结果跳转正常
- [ ] 设置面板正常（如有）

### 6.3 兼容性验证

**Plugin Verifier 检查:**

```powershell
# 本地运行兼容性验证
.\gradlew.bat verifyPlugin
```

此命令会检查插件与多个 IDE 版本的兼容性：
- IntelliJ IDEA 2024.1
- IntelliJ IDEA 2024.3
- IntelliJ IDEA 2025.1
- IntelliJ IDEA 2025.3
- IntelliJ IDEA 2026.1

**查看验证报告:**
```
build\reports\pluginVerifier\verification-report.html
```

---

## 7. 故障排除

### 7.1 构建阶段问题

#### ❌ 问题: `JAVA_HOME 未设置`

**错误信息:**
```
ERROR: JAVA_HOME is not set and no 'java' command could be found
```

**解决方案:**
```powershell
# 设置 JAVA_HOME 环境变量
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-17", "User")

# 重启 PowerShell 后验证
$env:JAVA_HOME
```

#### ❌ 问题: `org.gradle.java.home 路径无效`

**错误信息:**
```
Value 'C:/Users/xxx/scoop/apps/openjdk17/current' given for 
org.gradle.java.home Gradle property is invalid
```

**原因:** `gradle.properties` 中硬编码了本地 Windows 路径，CI 环境无法使用。

**解决方案:**
```properties
# gradle.properties - 注释掉或删除这一行
# org.gradle.java.home=C:/Users/xxx/scoop/apps/openjdk17/current
```

#### ❌ 问题: `Unreleased 版本缺失`

**错误信息:**
```
Version is missing: Unreleased
```

**解决方案:**
```markdown
# CHANGELOG.md 顶部必须存在
## [Unreleased]

## [1.0.11] - 2026-04-13
...
```

#### ❌ 问题: `Plugin description section not found`

**错误信息:**
```
Plugin description section not found in README.md:
```

**解决方案:**
```markdown
# README.md 中必须包含
<!-- Plugin description -->
这里是插件描述...
<!-- Plugin description end -->
```

#### ❌ 问题: `buildPlugin 任务失败`

**排查步骤:**
```powershell
# 1. 清理并重新构建
.\gradlew.bat clean buildPlugin

# 2. 查看详细错误
.\gradlew.bat buildPlugin --stacktrace

# 3. 检查 Gradle 版本
.\gradlew.bat --version

# 4. 更新 Gradle Wrapper
.\gradlew.bat wrapper --gradle-version 8.10.2
```

### 7.2 发布阶段问题

#### ❌ 问题: `token property must be specified`

**错误信息:**
```
'token' property must be specified for plugin publishing
```

**原因:** `PUBLISH_TOKEN` 环境变量未设置。

**解决方案:**
```powershell
# 设置环境变量
$env:PUBLISH_TOKEN="your_token_here"

# 验证
.\gradlew.bat publishPlugin
```

#### ❌ 问题: `Plugin version already exists`

**错误信息:**
```
Plugin version 1.0.11 already exists
```

**解决方案:**
1. 递增版本号（如 1.0.12）
2. 更新 `gradle.properties`
3. 重新构建并发布

#### ❌ 问题: `Marketplace 审核失败`

**常见原因:**
- 插件描述不完整
- 缺少必要的元数据
- 包含敏感信息

**解决方案:**
1. 检查 plugin.xml 中的描述
2. 确保 README.md 包含完整的插件说明
3. 重新提交

### 7.3 运行时问题

#### ❌ 问题: 插件安装后无法使用

**排查步骤:**
1. **检查插件是否启用**
   - Settings → Plugins → Installed
   - 确保 Json Path Navigator 已勾选启用

2. **检查快捷键冲突**
   - Settings → Keymap
   - 搜索 "Find Key in JSON"
   - 检查是否有快捷键冲突

3. **查看 IDE 日志**
   - Help → Show Log in Explorer/Finder
   - 查找 `JsonPathNavigator` 相关错误

#### ❌ 问题: 搜索结果为空

**排查步骤:**
1. 确认项目中有 JSON 文件
2. 检查文件是否在索引范围内
3. 尝试重建索引：File → Invalidate Caches → Invalidate and Restart

### 7.4 故障恢复

#### 删除失败的 Release

如果发布失败，需要清理后重新发布：

```bash
# 删除 GitHub Release
gh release delete v1.0.11 --yes

# 删除本地标签
git tag -d v1.0.11

# 删除远程标签
git push origin :refs/tags/v1.0.11

# 修复问题后重新推送
git add .
git commit --amend
git push origin main --force-with-lease
```

#### 回滚发布

如果发布了有问题的版本：

1. **在 Marketplace 隐藏版本**
   - 访问插件管理页面
   - 找到有问题的版本
   - 点击 **Hide** 或 **Delete**

2. **重新发布修复版本**
   - 递增版本号
   - 修复问题
   - 按正常流程重新发布

---

## 附录

### A. 常用 Gradle 任务速查

| 任务 | 命令 | 用途 |
|------|------|------|
| 清理 | `.\gradlew.bat clean` | 删除构建输出 |
| 构建插件 | `.\gradlew.bat buildPlugin` | 生成 ZIP 文件 |
| 运行 IDE | `.\gradlew.bat runIde` | 启动带插件的 IDE |
| 运行测试 | `.\gradlew.bat test` | 执行单元测试 |
| 验证插件 | `.\gradlew.bat verifyPlugin` | 检查兼容性 |
| 发布插件 | `.\gradlew.bat publishPlugin` | 发布到 Marketplace |
| 查看任务 | `.\gradlew.bat tasks` | 列出所有可用任务 |

### B. 版本号对应表

| IDE 版本 | Build 号 | since-build |
|----------|----------|-------------|
| 2024.1 | 241 | 241 |
| 2024.2 | 242 | 242 |
| 2024.3 | 243 | 243 |
| 2025.1 | 251 | 251 |
| 2025.3 | 253 | 253 |
| 2026.1 | 261 | 261 |

### C. 相关链接

- **GitHub 仓库**: https://github.com/wenmin92/JsonPathNavigator
- **JetBrains Marketplace**: https://plugins.jetbrains.com/plugin/cc.wenmin92.jsonpathnavigator
- **Token 管理**: https://plugins.jetbrains.com/author/me/tokens
- **Plugin Dev Docs**: https://plugins.jetbrains.com/docs/intellij/

### D. 发布检查清单

#### 发布前
- [ ] 版本号已更新（gradle.properties）
- [ ] CHANGELOG.md 已更新
- [ ] README.md 插件描述完整
- [ ] 本地构建成功
- [ ] 本地测试通过

#### 发布中
- [ ] Build Workflow 成功
- [ ] Draft Release 内容正确
- [ ] 已点击 Publish release
- [ ] Release Workflow 成功

#### 发布后
- [ ] GitHub Release 有 ZIP 附件
- [ ] Marketplace 显示新版本
- [ ] IDE 内可以检测到更新
- [ ] 更新后功能正常

---

*最后更新: 2026-04-13*
