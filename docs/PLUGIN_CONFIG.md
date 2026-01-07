# 插件配置与发布指南

本文档详细说明 Json Path Navigator 插件的配置、兼容性和发布到 JetBrains Marketplace 的完整流程。

---

## 目录

1. [IDE 兼容性](#1-ide-兼容性)
2. [版本配置详解](#2-版本配置详解)
3. [插件依赖配置](#3-插件依赖配置)
4. [Gradle 配置详解](#4-gradle-配置详解)
5. [插件签名](#5-插件签名)
6. [发布到 Marketplace](#6-发布到-marketplace)
7. [版本验证](#7-版本验证)
8. [CI/CD 配置](#8-cicd-配置)
9. [常见问题](#9-常见问题)

---

## 1. IDE 兼容性

### 1.1 支持的 IDE

本插件基于 IntelliJ Platform，通过以下模块依赖声明兼容性：

| 依赖模块 | 说明 | 必需 |
|---------|------|------|
| `com.intellij.modules.platform` | 核心平台模块 | ✅ |
| `com.intellij.modules.lang` | 语言支持模块 | ✅ |
| `com.intellij.modules.json` | JSON 支持模块 | ✅ |
| `org.jetbrains.plugins.json` | JSON 插件（可选） | ❌ |

### 1.2 兼容的 IDE 列表

由于依赖 `com.intellij.modules.platform`，本插件兼容所有基于 IntelliJ Platform 的 IDE：

| IDE | 产品代码 | 支持状态 |
|-----|---------|---------|
| IntelliJ IDEA Community | IC | ✅ 完全支持 |
| IntelliJ IDEA Ultimate | IU | ✅ 完全支持 |
| WebStorm | WS | ✅ 支持 |
| PyCharm Professional | PY | ✅ 支持 |
| PyCharm Community | PC | ✅ 支持 |
| GoLand | GO | ✅ 支持 |
| PhpStorm | PS | ✅ 支持 |
| RubyMine | RM | ✅ 支持 |
| CLion | CL | ✅ 支持 |
| DataGrip | DB | ✅ 支持 |
| Rider | RD | ✅ 支持 |
| Android Studio | AI | ✅ 支持 |
| AppCode | OC | ✅ 支持 |
| Aqua | QA | ✅ 支持 |
| RustRover | RR | ✅ 支持 |
| Fleet | FL | ❌ 不支持（架构不同） |

> **注意**: 不同 IDE 的 JSON 支持可能略有差异，核心功能在所有支持的 IDE 中均可用。

### 1.3 版本兼容范围

| 配置项 | 当前值 | 说明 |
|-------|-------|------|
| `pluginSinceBuild` | 241 | 最低支持版本 2024.1.x |
| `pluginUntilBuild` | 未设置 | 无上限，支持所有新版本 |

#### Build Number 与版本对照表

| Build Number | IDE 版本 | 发布日期 |
|-------------|----------|----------|
| 241.x | 2024.1.x | 2024年4月 |
| 242.x | 2024.2.x | 2024年8月 |
| 243.x | 2024.3.x | 2024年12月 |
| 251.x | 2025.1.x | 2025年4月 |

> 完整版本对照：https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html

---

## 2. 版本配置详解

### 2.1 gradle.properties 完整配置说明

```properties
# ========== 插件基本信息 ==========
pluginGroup=cc.wenmin92.jsonpathnavigator    # 插件包名/组织ID
pluginName=Json Path Navigator               # 插件显示名称
pluginVersion=1.0.9                          # 插件版本号
pluginRepositoryUrl=https://github.com/wenmin92/JsonPathNavigator  # 仓库地址

# ========== IDE 版本兼容性 ==========
pluginSinceBuild=241              # 最低支持的 Build Number (2024.1)
# pluginUntilBuild=251.*          # 最高支持版本，不设置则无上限
platformType=IC                   # 开发使用的 IDE 类型
platformVersion=2024.1.7          # 开发使用的 IDE 版本

# ========== 插件依赖 ==========
platformPlugins=                  # 第三方插件依赖（逗号分隔）
platformBundledPlugins=           # 内置插件依赖（逗号分隔）

# ========== 构建工具配置 ==========
gradleVersion=8.10.2              # Gradle 版本
org.gradle.java.home=C:/path/to/jdk17  # JDK 路径

# ========== Gradle 性能优化 ==========
org.gradle.daemon=true            # 启用 Gradle 守护进程
org.gradle.parallel=true          # 启用并行构建
org.gradle.configuration-cache=false  # 配置缓存（实验性）
org.gradle.caching=true           # 启用构建缓存

# ========== Kotlin 配置 ==========
kotlin.stdlib.default.dependency=false  # 不捆绑 Kotlin 标准库

# ========== 网络代理（可选） ==========
# systemProp.http.proxyHost=127.0.0.1
# systemProp.http.proxyPort=7890
```

### 2.2 plugin.xml 配置说明

```xml
<idea-plugin>
    <!-- 插件唯一标识符（发布后不可更改） -->
    <id>cc.wenmin92.jsonpathnavigator</id>
    
    <!-- 插件显示名称 -->
    <name>Json Path Navigator</name>
    
    <!-- 开发者/组织信息 -->
    <vendor email="your@email.com" url="https://github.com/wenmin92">
        wenmin92
    </vendor>
    
    <!-- 插件描述（HTML格式） -->
    <description><![CDATA[
        A plugin to find JSON keys in your project files.
    ]]></description>
    
    <!-- 更新日志（可选，通常由 Gradle 自动生成） -->
    <change-notes><![CDATA[
        <ul>
            <li>1.0.9: Bug fixes</li>
        </ul>
    ]]></change-notes>
    
    <!-- 依赖声明 -->
    <depends>com.intellij.modules.platform</depends>
    <depends>com.intellij.modules.lang</depends>
    <depends>com.intellij.modules.json</depends>
    <depends optional="true" config-file="json-support.xml">org.jetbrains.plugins.json</depends>
    
    <!-- 国际化资源 -->
    <resource-bundle>messages.MyBundle</resource-bundle>
    
    <!-- 扩展点注册 -->
    <extensions defaultExtensionNs="com.intellij">
        <!-- 启动时预热索引 -->
        <postStartupActivity implementation="cc.wenmin92.jsonkeyfinder.service.JsonIndexWarmupActivity"/>
    </extensions>
    
    <!-- 动作注册 -->
    <actions>
        <action id="JsonPathNavigator.Find"
                class="cc.wenmin92.jsonkeyfinder.actions.FindJsonKeyAction"
                text="Find Key in JSON"
                description="Search for JSON keys in project files">
            <!-- 快捷键 -->
            <keyboard-shortcut keymap="$default" first-keystroke="ctrl alt shift F"/>
            <keyboard-shortcut keymap="Mac OS X" first-keystroke="meta shift alt F"/>
            <keyboard-shortcut keymap="Mac OS X 10.5+" first-keystroke="meta shift alt F"/>
            <!-- 菜单位置 -->
            <add-to-group group-id="EditorPopupMenu" anchor="first"/>
            <add-to-group group-id="EditMenu" anchor="last"/>
        </action>
    </actions>
</idea-plugin>
```

---

## 3. 插件依赖配置

### 3.1 依赖类型说明

| 类型 | 说明 | 示例 |
|------|------|------|
| 模块依赖 | IDE 内置模块 | `com.intellij.modules.platform` |
| 内置插件依赖 | IDE 自带插件 | `com.intellij.java` |
| 第三方插件依赖 | Marketplace 插件 | `org.jetbrains.kotlin` |

### 3.2 当前插件依赖

```xml
<!-- 必需依赖 -->
<depends>com.intellij.modules.platform</depends>  <!-- 核心平台 -->
<depends>com.intellij.modules.lang</depends>      <!-- 语言支持 -->
<depends>com.intellij.modules.json</depends>      <!-- JSON 模块 -->

<!-- 可选依赖 -->
<depends optional="true" config-file="json-support.xml">org.jetbrains.plugins.json</depends>
```

### 3.3 添加新依赖

**添加内置插件依赖**:

1. 在 `gradle.properties` 中添加:
```properties
platformBundledPlugins=com.intellij.java
```

2. 在 `plugin.xml` 中添加:
```xml
<depends>com.intellij.modules.java</depends>
```

**添加第三方插件依赖**:

1. 在 `gradle.properties` 中添加:
```properties
platformPlugins=org.jetbrains.plugins.yaml:241.14494.150
```

2. 在 `plugin.xml` 中添加:
```xml
<depends>org.jetbrains.plugins.yaml</depends>
```

---

## 4. Gradle 配置详解

### 4.1 build.gradle.kts 关键配置

```kotlin
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform") version "2.6.0"  // IntelliJ Platform 插件
    id("org.jetbrains.changelog")  // 更新日志管理
    id("org.jetbrains.qodana")     // 代码质量检查
    id("org.jetbrains.kotlinx.kover")  // 测试覆盖率
}

// Java 工具链配置
kotlin {
    jvmToolchain(17)  // 使用 JDK 17
}

// IntelliJ Platform 依赖
dependencies {
    intellijPlatform {
        // 开发目标 IDE
        create(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1.7")
        
        // 内置插件依赖
        bundledPlugins(listOf("com.intellij.java"))
        
        // 验证工具
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

// 插件验证配置
intellijPlatform {
    pluginVerification {
        ides {
            // 验证的 IDE 版本
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1.7")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")
        }
    }
}

// 插件 XML 配置
tasks {
    patchPluginXml {
        version = "1.0.9"
        sinceBuild = "241"
        // untilBuild = "251.*"  // 不设置则无上限
    }
    
    // 签名配置
    signPlugin {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }
    
    // 发布配置
    publishPlugin {
        token = System.getenv("PUBLISH_TOKEN")
    }
}
```

### 4.2 开发环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17-21 | IntelliJ Platform 2024.1+ 要求 |
| Gradle | 8.5+ | 推荐 8.10.2 |
| IntelliJ Platform Plugin | 2.x | 当前使用 2.6.0 |
| Kotlin | 1.9+ | 自动管理 |

---

## 5. 插件签名

### 5.1 为什么需要签名

从 2021.2 开始，JetBrains 要求所有插件必须签名才能发布到 Marketplace。签名用于：
- 验证插件来源
- 确保插件未被篡改
- 提升用户信任度

### 5.2 生成签名证书

**方式一：使用 JetBrains 工具生成**

```powershell
# 1. 下载证书生成工具
# 访问 https://github.com/JetBrains/marketplace-zip-signer

# 2. 生成私钥和证书链
java -jar marketplace-zip-signer-cli.jar generate-keys `
    --private-key-out private.pem `
    --public-key-out public.pem `
    --private-key-password your_password
```

**方式二：使用 OpenSSL**

```powershell
# 1. 生成私钥
openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:4096

# 2. 生成自签名证书
openssl req -new -x509 -key private.pem -out chain.crt -days 365

# 3. 转换为证书链格式（如需要）
openssl pkcs12 -export -out certificate.p12 -inkey private.pem -in chain.crt
```

### 5.3 配置签名环境变量

**Windows PowerShell**:

```powershell
# 临时设置
$env:CERTIFICATE_CHAIN = Get-Content chain.crt -Raw
$env:PRIVATE_KEY = Get-Content private.pem -Raw
$env:PRIVATE_KEY_PASSWORD = "your_password"

# 永久设置（系统环境变量）
[Environment]::SetEnvironmentVariable("CERTIFICATE_CHAIN", (Get-Content chain.crt -Raw), "User")
[Environment]::SetEnvironmentVariable("PRIVATE_KEY", (Get-Content private.pem -Raw), "User")
[Environment]::SetEnvironmentVariable("PRIVATE_KEY_PASSWORD", "your_password", "User")
```

**Linux/macOS**:

```bash
export CERTIFICATE_CHAIN=$(cat chain.crt)
export PRIVATE_KEY=$(cat private.pem)
export PRIVATE_KEY_PASSWORD="your_password"
```

### 5.4 签名插件

```powershell
# 构建并签名
.\gradlew.bat signPlugin

# 输出位置: build/distributions/jsonpathnavigator-1.0.9-signed.zip
```

---

## 6. 发布到 Marketplace

### 6.1 准备工作

#### 1. 注册 JetBrains 账号

访问 https://plugins.jetbrains.com 并注册/登录

#### 2. 创建插件页面（首次发布）

1. 登录 Marketplace
2. 点击 "Upload plugin"
3. 上传插件 ZIP 包
4. 填写插件信息：
   - **名称**: Json Path Navigator
   - **描述**: 详细功能说明
   - **标签**: json, navigation, search
   - **许可证**: 选择开源许可证
   - **图标**: 上传插件图标 (40x40, 80x80, 128x128)
   - **截图**: 上传功能截图

#### 3. 获取发布 Token

1. 访问 https://plugins.jetbrains.com/author/me/tokens
2. 点击 "Generate Token"
3. 设置 Token 名称和过期时间
4. 保存生成的 Token

### 6.2 发布流程

#### 手动发布

```powershell
# 1. 设置发布 Token
$env:PUBLISH_TOKEN = "your_token_here"

# 2. 构建并签名
.\gradlew.bat buildPlugin signPlugin

# 3. 发布到 Marketplace
.\gradlew.bat publishPlugin
```

#### 配置发布渠道

```kotlin
// build.gradle.kts
tasks {
    publishPlugin {
        token = System.getenv("PUBLISH_TOKEN")
        // 发布渠道：default（正式）, eap（早期访问）, beta
        channels = listOf("default")
    }
}
```

### 6.3 更新插件

1. 更新 `gradle.properties` 中的版本号:
```properties
pluginVersion=1.1.0
```

2. 更新 `CHANGELOG.md`:
```markdown
## [1.1.0] - 2026-01-06

### Added
- 新功能描述

### Fixed
- 修复问题描述
```

3. 发布:
```powershell
.\gradlew.bat publishPlugin
```

### 6.4 发布检查清单

| 检查项 | 状态 |
|--------|------|
| 版本号已更新 | ☐ |
| CHANGELOG.md 已更新 | ☐ |
| 所有测试通过 | ☐ |
| 插件验证通过 | ☐ |
| 签名证书有效 | ☐ |
| PUBLISH_TOKEN 已设置 | ☐ |

---

## 7. 版本验证

### 7.1 运行插件验证

```powershell
# 验证插件兼容性
.\gradlew.bat verifyPlugin
```

验证器会检查：
- API 兼容性
- 废弃 API 使用
- 缺失的依赖
- 插件描述符问题

### 7.2 配置验证的 IDE 版本

```kotlin
// build.gradle.kts
intellijPlatform {
    pluginVerification {
        ides {
            // 验证特定版本
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1.7")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")
            
            // 验证推荐版本（需要网络）
            // recommended()
            
            // 验证不同 IDE 类型
            // ide(IntelliJPlatformType.WebStorm, "2024.1")
            // ide(IntelliJPlatformType.PyCharmCommunity, "2024.1")
        }
    }
}
```

### 7.3 验证报告

验证完成后，报告位于:
```
build/reports/pluginVerifier/
```

报告包含：
- 兼容性问题
- 废弃 API 警告
- 实验性 API 使用
- 内部 API 使用

---

## 8. CI/CD 配置

### 8.1 GitHub Actions 配置

项目使用 GitHub Actions 自动构建和发布。配置文件位于 `.github/workflows/`。

#### 构建工作流 (build.yml)

```yaml
name: Build

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Build
        run: ./gradlew build
        
      - name: Verify Plugin
        run: ./gradlew verifyPlugin
```

#### 发布工作流 (release.yml)

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
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Publish Plugin
        env:
          PUBLISH_TOKEN: ${{ secrets.PUBLISH_TOKEN }}
          CERTIFICATE_CHAIN: ${{ secrets.CERTIFICATE_CHAIN }}
          PRIVATE_KEY: ${{ secrets.PRIVATE_KEY }}
          PRIVATE_KEY_PASSWORD: ${{ secrets.PRIVATE_KEY_PASSWORD }}
        run: ./gradlew publishPlugin
```

### 8.2 配置 GitHub Secrets

在 GitHub 仓库设置中添加以下 Secrets:

| Secret 名称 | 说明 |
|------------|------|
| `PUBLISH_TOKEN` | Marketplace 发布 Token |
| `CERTIFICATE_CHAIN` | 签名证书链内容 |
| `PRIVATE_KEY` | 私钥内容 |
| `PRIVATE_KEY_PASSWORD` | 私钥密码 |

设置路径: Repository → Settings → Secrets and variables → Actions

---

## 9. 常见问题

### 9.1 构建问题

**Q: Gradle 同步失败**

```
A: 检查以下项目：
1. JDK 版本是否为 17-21
2. gradle.properties 中的 org.gradle.java.home 路径是否正确
3. 网络是否正常（可能需要代理）
```

**Q: 找不到 IntelliJ Platform 依赖**

```
A: 确保 repositories 中包含 intellijPlatform.defaultRepositories()
```

### 9.2 兼容性问题

**Q: 插件在某个 IDE 版本无法运行**

```
A: 
1. 检查 pluginSinceBuild 是否正确
2. 运行 verifyPlugin 检查兼容性
3. 检查是否使用了特定版本才有的 API
```

**Q: JSON 模块找不到**

```
A: 从 2024.3 开始，JSON 模块需要显式声明。确保：
1. plugin.xml 中有 <depends>com.intellij.modules.json</depends>
2. 如果需要 JSON 插件特定功能，添加可选依赖
```

### 9.3 发布问题

**Q: 发布时提示签名无效**

```
A:
1. 检查证书是否过期
2. 检查环境变量是否正确设置
3. 确保私钥密码正确
```

**Q: 发布成功但 Marketplace 显示"审核中"**

```
A: 首次发布需要 JetBrains 团队人工审核，通常需要 1-3 个工作日。
```

### 9.4 开发问题

**Q: runIde 启动很慢**

```
A:
1. 增加内存: jvmArgs("-Xmx2g")
2. 禁用不需要的插件
3. 使用 SSD
```

**Q: 测试失败**

```
A:
1. 确保测试资源文件存在于 src/test/resources/testData/
2. 检查测试是否正确使用 BasePlatformTestCase
3. 查看 build/reports/tests/ 中的详细报告
```

---

## 附录

### A. 有用的链接

| 资源 | 链接 |
|------|------|
| IntelliJ Platform SDK 文档 | https://plugins.jetbrains.com/docs/intellij/ |
| Plugin Template | https://github.com/JetBrains/intellij-platform-plugin-template |
| Gradle IntelliJ Plugin | https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html |
| Build Number 范围 | https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html |
| Plugin Signing | https://plugins.jetbrains.com/docs/intellij/plugin-signing.html |
| Marketplace 文档 | https://plugins.jetbrains.com/docs/marketplace/ |

### B. 版本历史

| 插件版本 | 支持的 IDE | 发布日期 |
|---------|-----------|----------|
| 1.0.9 | 2024.1+ | 2026-01-06 |
| 1.0.0 | 2024.1+ | 初始版本 |

---

*最后更新: 2026-01-06*
