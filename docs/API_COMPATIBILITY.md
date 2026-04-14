# API 兼容性验证指南

本文档说明如何在本地复现 JetBrains Marketplace 的 API 兼容性验证，理解验证报告的含义，以及如何修复常见问题。

---

## 目录

1. [背景：什么是 Plugin Verifier](#1-背景什么是-plugin-verifier)
2. [验证范围：三类问题](#2-验证范围三类问题)
3. [本地运行验证](#3-本地运行验证)
4. [解读验证报告](#4-解读验证报告)
5. [build.gradle.kts 中的版本配置](#5-buildgradlekts-中的版本配置)
6. [常见问题与修复方法](#6-常见问题与修复方法)
7. [典型案例：ReadAction API 演进问题](#7-典型案例readaction-api-演进问题)
8. [CI/CD 集成](#8-cicd-集成)

---

## 1. 背景：什么是 Plugin Verifier

JetBrains Marketplace 在每次上传插件时，会用 **IntelliJ Plugin Verifier** 工具对插件 JAR 进行静态字节码分析，检查插件在各目标 IDE 版本中是否存在 API 使用问题。

本项目通过 Gradle 任务 `verifyPlugin` 集成了同一个工具，使你可以在提交前本地复现官方验证，避免上传后才发现问题。

```
插件 JAR
   │
   ▼
IntelliJ Plugin Verifier（字节码分析）
   │
   ├── 针对每个目标 IDE 版本：
   │       2024.1.7 / 2024.2.6 / 2024.3.7
   │       2025.1.7 / 2025.2.6.1 / 2025.3.4 / 2026.1.1
   │
   └── 输出：兼容性报告（build/reports/pluginVerifier/）
```

---

## 2. 验证范围：三类问题

Plugin Verifier 识别以下三类 API 使用问题，严重程度依次递减：

### 2.1 Compatibility Problems（兼容性问题）❌ 必须修复

插件在该 IDE 版本运行时会**直接崩溃**或**无法加载**。

常见原因：
- 调用的方法/类在该版本中**不存在**（被移除或尚未引入）
- 方法签名发生了不兼容变更

示例报告：
```
Invocation of unresolved method 'com.example.SomeClass.someMethod()'
Access to unresolved class 'com.example.RemovedClass'
```

### 2.2 Deprecated API Usages（废弃 API 使用）⚠️ 建议修复

调用了标注 `@Deprecated` 的方法。该方法**目前仍可用**，但可能在未来版本中被移除，届时升级为 Compatibility Problem。

示例报告：
```
Deprecated method usage: ReadAction.compute(ThrowableComputable) (1)
Deprecated method usage: ActionsKt.runReadAction(Function0) (1)
```

### 2.3 Experimental API Usages（实验性 API 使用）⚠️ 建议修复

调用了标注 `@ApiStatus.Experimental` 的方法。JetBrains **不保证** API 稳定性，可能在任何版本改变签名或行为。

示例报告：
```
Experimental method usage: ReadAction.computeCancellable(ThrowableComputable) (1)
```

---

## 3. 本地运行验证

### 3.1 完整验证（推荐发布前执行）

```powershell
# 首次运行会下载多个 IDE 版本（共约 5~10 GB，耗时较长）
.\gradlew.bat verifyPlugin
```

验证结果在：
```
build/reports/pluginVerifier/
  ├── 2024.1.7/   # 每个版本独立子目录
  ├── 2024.2.6/
  ├── ...
  └── 2026.1.1/
```

### 3.2 只验证插件结构（快速）

```powershell
# 不需要下载 IDE，只检查 plugin.xml 和插件结构
.\gradlew.bat verifyPluginStructure
```

### 3.3 强制重新运行

```powershell
.\gradlew.bat verifyPlugin --rerun-tasks
```

### 3.4 查看报告

```powershell
# Windows：在文件管理器中打开报告目录
start build\reports\pluginVerifier

# 或直接查看文本报告
Get-Content build\reports\pluginVerifier\2024.1.7\verification-report.txt
```

---

## 4. 解读验证报告

报告示例（节选）：

```
Plugin Json Path Navigator 1.0.13 against IC-2024.2.6

1 experimental API usage
  Json Path Navigator 1.0.13 uses experimental API, which may be changed
  in future releases leading to binary and source code incompatibilities
    Experimental method usage (1)
      ReadAction.computeCancellable(ThrowableComputable) (1)
        cc.wenmin92.jsonkeyfinder.service.JsonSearchService.isValidRootProperty(String) (1)
```

阅读要点：

| 字段 | 含义 |
|------|------|
| `against IC-2024.2.6` | 此条问题出现在哪个 IDE 版本 |
| `Experimental method usage` | 问题类型（实验性/废弃/兼容性） |
| `ReadAction.computeCancellable(...)` | 有问题的 API |
| `...JsonSearchService.isValidRootProperty(...)` | 调用该 API 的具体位置 |

### 判断优先级

同一个问题**可能只在部分版本中出现**，需要看报告覆盖哪些版本：

- 若仅在旧版本出现 Experimental → 该 API 在旧版本是实验性的，新版本已毕业
- 若仅在新版本出现 Deprecated → 该 API 在新版本被废弃，旧版本还是稳定的
- 若同一 API 在旧版本是 Experimental、在新版本是 Deprecated → **存在版本交叉问题**，需要寻找跨版本稳定的替代方案（见 [第 7 节](#7-典型案例readaction-api-演进问题)）

---

## 5. build.gradle.kts 中的版本配置

`pluginVerification` 块配置了验证使用的 IDE 版本，与 JetBrains Marketplace 官方验证版本保持一致：

```kotlin
intellijPlatform {
    pluginVerification {
        ides {
            // 与 JetBrains Marketplace 官方验证版本保持一致（正式发布版本，可自动下载）
            // 2025.3+ 使用统一发行版 (IU)，不再有独立的 Community Edition
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1.7")   // 插件最低支持版本
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.2.6")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3.7")   // JSON 模块分离起始版本
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1.7")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.2.6.1")
            ide(IntelliJPlatformType.IntellijIdeaUltimate,  "2025.3.4")   // IU 统一发行版起点
            // 2026.1.1 为 EAP 版本，不在标准仓库，见下方说明
        }
    }
}
```

### EAP 版本的处理

Marketplace 使用的 `2026.1.1` 是 **EAP（Early Access Preview）** 版本，不发布在 JetBrains 的 Maven 仓库中，Plugin Verifier 无法自动下载。

**本地验证选项**：若本机已安装 2026.1.1 EAP，在 `gradle.properties` 中添加：

```properties
verifyLocalIdePath2026=C:/Program Files/JetBrains/IntelliJ IDEA 2026.1
```

然后在 `build.gradle.kts` 中取消对应注释：

```kotlin
local((project.findProperty("verifyLocalIdePath2026") as? String) ?: "")
```

**若未安装**：跳过本地 2026.1.1 验证，上传到 Marketplace 后观察官方检测结果即可。6 个正式发布版本的验证已能覆盖绝大多数兼容性问题。

**为什么 2025.3+ 用 IU？**

从 2025.3 开始，JetBrains 不再单独发布 Community Edition 的独立安装包，改为统一的 IntelliJ IDEA（内含社区功能）。Plugin Verifier 需要对应版本的 IDE 才能验证。

**版本更新时机**

当 JetBrains 发布新版本并更新 Marketplace 验证列表时，同步更新此处的版本列表，确保本地验证与官方保持一致。

---

## 6. 常见问题与修复方法

### 6.1 Deprecated API

**排查步骤：**

1. 从报告中找到被废弃的 API 全名，例如 `ReadAction.compute(ThrowableComputable)`
2. 在 IntelliJ Platform 源码中查找该 API 的 `@Deprecated` 注释，通常包含推荐替代方案：
   - 官方源码：`https://github.com/JetBrains/intellij-community/tree/idea/<build>/platform/`
3. 确认替代 API 在插件最低支持版本（`pluginSinceBuild`）中是否也存在
4. 替换后重新运行 `verifyPlugin`

**注意事项：**

废弃 API 的替代方案本身也可能在旧版本中是实验性的（见 [第 7 节](#7-典型案例readaction-api-演进问题)），需要检查跨版本兼容性。

### 6.2 Experimental API

**排查步骤：**

1. 从报告中找到实验性 API，例如 `ReadAction.computeCancellable(ThrowableComputable)`
2. 确认该 API 是否在新版本中已"毕业"（移除了 `@ApiStatus.Experimental` 注解）
3. 如果已毕业，说明旧版本标注实验性、新版本稳定，问题只影响旧版本
4. 寻找在所有支持版本中均稳定的底层 API

### 6.3 Compatibility Problems

**排查步骤：**

1. 确认哪个版本引入/移除了该 API
2. 如果 API 在 `pluginSinceBuild` 之后才引入，需要提高 `pluginSinceBuild`
3. 如果 API 在某版本被移除，需要寻找替代方案或降低使用

---

## 7. 典型案例：ReadAction API 演进问题

### 背景

本项目在 1.0.12~1.0.13 版本期间遭遇了一个 IntelliJ Platform API 演进导致的"两难困境"，记录于此作为经典参考。

### 问题时间线

| 版本 | 现象 | 原因 |
|------|------|------|
| 1.0.11 | Marketplace 报告 **Deprecated API**：`ReadAction.compute`、`ActionsKt.runReadAction` | 2026.1 中这两个方法被标注 `@Deprecated` |
| 1.0.12 | 改为 `ReadAction.computeCancellable` | 2026.1 中该方法稳定，不再是实验性 |
| 1.0.13 | Marketplace 报告 **Experimental API**：`ReadAction.computeCancellable` | 在 2024.1~2025.3.4 中该方法标注 `@ApiStatus.Experimental` |

### API 在各版本的状态矩阵

| API | 2024.1~2025.3.4 | 2026.1+ |
|-----|-----------------|---------|
| `ReadAction.compute(ThrowableComputable)` | ✅ 稳定 | ❌ `@Deprecated` |
| `ReadAction.computeCancellable(ThrowableComputable)` | ❌ `@Experimental` | ✅ 稳定 |
| `Application.runReadAction(ThrowableComputable)` | ✅ 稳定 | ✅ 稳定 |

JetBrains 在 2026.1 将高层静态包装器（`ReadAction.compute`）废弃，推荐改用 `computeCancellable`，但后者在更早版本仍是实验性的——形成了交叉约束。

### 解决方案

跳过两个有问题的静态包装器，直接调用它们共同的底层实现：`Application.runReadAction(ThrowableComputable)`。

该方法是 `Application` 接口的核心方法，在两个版本中均无任何 `@Deprecated` 或 `@Experimental` 注解。查阅源码可以验证：

- 2024.1 中 `ReadAction.compute` 的实现体就是：
  ```java
  return ApplicationManager.getApplication().runReadAction(action);
  ```
- 2026.1 中 `ReadAction.computeBlocking`（`compute` 的继任者）同样：
  ```java
  return ApplicationManager.getApplication().runReadAction(action);
  ```

**最终代码（JsonSearchService.kt）：**

```kotlin
fun isValidRootProperty(propertyName: String): Boolean {
    return if (ApplicationManager.getApplication().isReadAccessAllowed) {
        // 已在 read action 上下文中（EDT / 测试），直接执行
        computeIsValidRootProperty(propertyName)
    } else {
        // 使用 Application 接口的核心方法，在 2024.1~2026.1+ 均稳定
        ApplicationManager.getApplication().runReadAction(
            ThrowableComputable<Boolean, Throwable> {
                computeIsValidRootProperty(propertyName)
            }
        )
    }
}
```

### 经验总结

遇到"旧版本实验性、新版本废弃"的交叉问题时：

1. **查阅官方源码**，找到高层 API 的实现体
2. **识别最底层的稳定方法**（通常是接口方法，而非静态工具方法）
3. **直接调用底层方法**，绕开随版本变化的包装层
4. 源码查阅地址：`https://github.com/JetBrains/intellij-community/tree/idea/<build>/`

---

## 8. CI/CD 集成

### GitHub Actions 中的验证

`.github/workflows/build.yml` 中的 `verify` job 在每次 push/PR 时自动运行：

```yaml
- name: Run Plugin Verification tasks
  run: ./gradlew verifyPlugin -Dplugin.verifier.home.dir=${{ ... }}
```

验证报告会作为 Artifact 上传，可在 Actions 页面下载查看。

### 本地 vs CI 的差异

| 项目 | 本地 | CI (GitHub Actions) |
|------|------|---------------------|
| IDE 下载位置 | `~/.pluginVerifier/ides/` | Actions Cache |
| 首次运行耗时 | 较长（需下载） | 较长（首次无缓存） |
| 后续运行 | 使用缓存，较快 | 使用 Cache，较快 |
| 报告位置 | `build/reports/pluginVerifier/` | Artifact: `pluginVerifier-result` |

### 推荐工作流

```
本地开发
   ↓
修改 API 调用
   ↓
./gradlew verifyPlugin          ← 本地验证，与官方相同
   ↓
无问题 → git push
   ↓
GitHub Actions 自动验证          ← 二次确认
   ↓
全绿 → 发布
```

---

*最后更新：2026-04-14*
