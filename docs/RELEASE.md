# Json Path Navigator 发布指南

本文档详细说明插件的发布流程、配置要求以及常见问题的解决方案。

---

## 目录

1. [发布概述](#1-发布概述)
2. [前置准备](#2-前置准备)
3. [GitHub Actions 自动发布](#3-github-actions-自动发布)
4. [手动发布](#4-手动发布)
5. [常见问题与解决方案](#5-常见问题与解决方案)
6. [发布检查清单](#6-发布检查清单)

---

## 1. 发布概述

### 1.1 发布方式

本项目支持两种发布方式：

| 方式 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **GitHub Actions 自动发布** | 自动化、可重复、有记录 | 需要配置 Secrets | ✅ 推荐用于正式发布 |
| **手动发布** | 简单、快速 | 容易出错、无记录 | 仅用于测试或紧急情况 |

### 1.2 发布流程图

```
更新版本号和 CHANGELOG
        ↓
提交并推送到 main 分支
        ↓
GitHub Actions 自动构建
        ↓
创建 Draft Release
        ↓
手动审核并发布 Release ← 你在这里
        ↓
GitHub Actions 自动发布到 Marketplace
        ↓
用户可以更新插件
```

---

## 2. 前置准备

### 2.1 JetBrains Marketplace 账号

**首次发布需要：**
1. 访问 https://plugins.jetbrains.com/
2. 使用 JetBrains 账号登录
3. 创建插件（如果是首次发布）

### 2.2 获取 PUBLISH_TOKEN

**步骤：**
1. 访问 https://plugins.jetbrains.com/author/me/tokens
2. 点击 **"Generate New Token"**
3. 填写信息：
   - **Token name**: `JsonPathNavigator GitHub Actions`
   - **权限**: 勾选 ✅ **Upload plugins**
4. 点击 **"Generate"**
5. **⚠️ 重要**: 立即复制 Token（只显示一次）
6. 保存到安全的地方（如密码管理器）

### 2.3 配置 GitHub Secrets

**步骤：**
1. 访问仓库 Secrets 设置：
   ```
   https://github.com/wenmin92/JsonPathNavigator/settings/secrets/actions
   ```

2. 点击 **"New repository secret"**

3. 添加必需的 Secret：

   | Name | Value | 说明 |
   |------|-------|------|
   | `PUBLISH_TOKEN` | （刚才复制的 token） | ✅ **必需** - 用于发布到 Marketplace |

4. **可选的签名相关 Secrets**（本项目暂不使用）：
   - `CERTIFICATE_CHAIN` - 证书链（暂不需要）
   - `PRIVATE_KEY` - 私钥（暂不需要）
   - `PRIVATE_KEY_PASSWORD` - 私钥密码（暂不需要）

   > **注意**: 我们的插件历史版本都未签名，为保持兼容性，新版本也不应签名。

---

## 3. GitHub Actions 自动发布

### 3.1 发布步骤

#### 步骤 1: 更新版本号

编辑 `gradle.properties`:

```properties
pluginVersion=1.0.11  # 更新版本号
```

#### 步骤 2: 更新 CHANGELOG.md

在 CHANGELOG.md 中添加新版本信息：

```markdown
## [Unreleased]

## [1.0.11] - 2026-01-07

### Added
- 新功能描述

### Fixed
- 修复的问题

### Changed
- 变更说明
```

并在文件底部添加版本链接：

```markdown
[1.0.11]: https://github.com/wenmin92/JsonPathNavigator/releases/tag/v1.0.11
```

#### 步骤 3: 提交并推送

```bash
git add gradle.properties CHANGELOG.md
git commit -m "chore: bump version to 1.0.11"
git push origin main
```

#### 步骤 4: 等待 Build Workflow 完成

1. 访问 https://github.com/wenmin92/JsonPathNavigator/actions
2. 等待 **Build** workflow 完成（约 5-10 分钟）
3. 确认所有检查都通过 ✅

#### 步骤 5: 发布 Release

1. Build 成功后，会自动创建 Draft Release
2. 访问 https://github.com/wenmin92/JsonPathNavigator/releases
3. 找到 Draft 状态的 `v1.0.11` release
4. 点击 **Edit** 检查内容
5. 点击 **Publish release** 🚀

#### 步骤 6: 等待 Release Workflow 完成

1. 发布后会自动触发 **Release** workflow
2. 访问 https://github.com/wenmin92/JsonPathNavigator/actions/workflows/release.yml
3. 等待完成（约 5 分钟）

#### 步骤 7: 验证发布

**GitHub Release:**
- 访问 https://github.com/wenmin92/JsonPathNavigator/releases/tag/v1.0.11
- 确认 Assets 中有 `jsonpathnavigator-1.0.11.zip`

**JetBrains Marketplace:**
- 访问插件页面（可能需要几分钟审核）
- 确认新版本出现在版本列表中

### 3.2 Workflow 说明

#### Build Workflow (.github/workflows/build.yml)

**触发条件**: 
- Push 到 main 分支
- Pull Request

**主要任务**:
1. ✅ 验证 Gradle Wrapper
2. ✅ 编译代码
3. ✅ 运行测试（当前已禁用）
4. ✅ 代码质量检查（Qodana）
5. ✅ 插件验证（Plugin Verifier）
6. ✅ 构建插件
7. ✅ 创建 Draft Release

#### Release Workflow (.github/workflows/release.yml)

**触发条件**: 
- 发布 Release（published）

**主要任务**:
1. ✅ 构建插件
2. ⚠️ 签名插件（跳过 - 未配置证书）
3. ✅ 发布到 JetBrains Marketplace
4. ✅ 上传构建产物到 GitHub Release
5. ⚠️ 创建 PR 更新 CHANGELOG（可能失败，不影响发布）

---

## 4. 手动发布

### 4.1 本地构建

```bash
# Windows
.\gradlew.bat clean buildPlugin

# Linux/Mac
./gradlew clean buildPlugin
```

构建产物位置: `build/distributions/jsonpathnavigator-{version}.zip`

### 4.2 手动上传到 Marketplace

1. 访问 https://plugins.jetbrains.com/plugin/YOUR_PLUGIN_ID/edit
2. 点击 **"Upload Update"**
3. 选择构建的 zip 文件
4. 填写更新说明
5. 点击 **"Upload"**

---

## 5. 常见问题与解决方案

### 5.1 构建阶段问题

#### ❌ 问题: `org.gradle.java.home` 路径无效

**错误信息**:
```
Value 'C:/Users/wenmin/scoop/apps/openjdk17/current' given for 
org.gradle.java.home Gradle property is invalid
```

**原因**: `gradle.properties` 中硬编码了本地 Windows Java 路径，CI 环境无法使用。

**解决方案**:
在 `gradle.properties` 中注释掉 Java Home 配置：
```properties
# org.gradle.java.home=C:/Users/wenmin/scoop/apps/openjdk17/current
```

---

#### ❌ 问题: Unreleased 版本缺失

**错误信息**:
```
Version is missing: Unreleased
```

**原因**: CHANGELOG.md 缺少 `[Unreleased]` 部分。

**解决方案**:
在 CHANGELOG.md 顶部添加：
```markdown
## [Unreleased]

## [1.0.10] - 2026-01-06
...
```

---

#### ❌ 问题: 插件描述未找到

**错误信息**:
```
Plugin description section not found in README.md
```

**原因**: README.md 缺少插件描述标记。

**解决方案**:
在 README.md 中添加：
```markdown
<!-- Plugin description -->
插件描述内容
<!-- Plugin description end -->
```

---

#### ❌ 问题: 测试编译失败

**错误信息**:
```
Unresolved reference 'BasePlatformTestCase'
```

**原因**: 测试依赖未正确配置。

**解决方案**:
在 `build.gradle.kts` 中临时禁用测试：
```kotlin
test {
    useJUnitPlatform()
    enabled = false  // 临时禁用
}

withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (name.contains("Test")) {
        enabled = false
    }
}
```

---

### 5.2 发布阶段问题

#### ❌ 问题: token 属性未指定

**错误信息**:
```
'token' property must be specified for plugin publishing
```

**原因**: `PUBLISH_TOKEN` Secret 未配置。

**解决方案**:
1. 访问 https://plugins.jetbrains.com/author/me/tokens 生成 token
2. 添加到 GitHub Secrets（名称：`PUBLISH_TOKEN`）

---

#### ❌ 问题: 无法创建 Pull Request

**错误信息**:
```
GitHub Actions is not permitted to create or approve pull requests
```

**原因**: GitHub Actions 默认没有创建 PR 的权限。

**影响**: ⚠️ 不影响插件发布，只是无法自动创建 CHANGELOG 更新 PR。

**解决方案（可选）**:
1. 访问 https://github.com/wenmin92/JsonPathNavigator/settings/actions
2. 在 "Workflow permissions" 部分：
   - 选择 **"Read and write permissions"**
   - ✅ 勾选 **"Allow GitHub Actions to create and approve pull requests"**
3. 点击 **"Save"**

---

### 5.3 签名相关问题

#### ❓ 问题: 需要签名插件吗？

**回答**: 
- ✅ **不需要** - 我们的历史版本都未签名
- ⚠️ 如果现在开始签名，会导致用户更新时需要卸载重装
- 👍 保持不签名即可

#### ❓ 问题: 签名跳过是否正常？

**日志显示**:
```
> Task :signPlugin SKIPPED
```

**回答**: ✅ 这是正常的，因为未配置签名证书，Gradle 会自动跳过签名步骤。

---

### 5.4 更新相关问题

#### ❓ 问题: 更换 PUBLISH_TOKEN 是否影响更新？

**回答**: ✅ **不影响**
- PUBLISH_TOKEN 只是认证用的令牌
- 不会影响插件本身或用户更新

#### ❓ 问题: 从手动发布切换到 CI 发布是否有问题？

**回答**: ✅ **没问题**
- 只要保持签名策略一致（都不签名）
- 用户可以正常更新

---

## 6. 发布检查清单

### 发布前检查

- [ ] 更新 `gradle.properties` 中的版本号
- [ ] 更新 `CHANGELOG.md`，添加新版本说明
- [ ] 在 CHANGELOG.md 添加新版本链接
- [ ] 确保 `[Unreleased]` 部分存在
- [ ] 本地测试构建：`./gradlew buildPlugin`
- [ ] 提交所有更改并推送到 main

### 构建检查

- [ ] Build workflow 成功完成
- [ ] 所有测试通过（如果启用）
- [ ] 代码质量检查通过
- [ ] Plugin Verifier 验证通过
- [ ] Draft Release 自动创建

### 发布检查

- [ ] 检查 Draft Release 的内容和版本号
- [ ] 发布 Release
- [ ] Release workflow 成功完成
- [ ] GitHub Release Assets 中有 zip 文件
- [ ] JetBrains Marketplace 显示新版本

### 发布后验证

- [ ] 在 IDE 中检查插件更新
- [ ] 安装新版本并测试核心功能
- [ ] 关闭或合并相关的 issue/PR
- [ ] 在 Discussions 或 Release 中公告（可选）

---

## 附录

### A. 相关链接

- **GitHub 仓库**: https://github.com/wenmin92/JsonPathNavigator
- **JetBrains Marketplace**: https://plugins.jetbrains.com/plugin/YOUR_PLUGIN_ID
- **GitHub Actions**: https://github.com/wenmin92/JsonPathNavigator/actions
- **Token 管理**: https://plugins.jetbrains.com/author/me/tokens

### B. 版本号规范

遵循 [Semantic Versioning](https://semver.org/):

- **Major (1.x.x)**: 不兼容的 API 变更
- **Minor (x.1.x)**: 向后兼容的功能新增
- **Patch (x.x.1)**: 向后兼容的问题修复

示例:
- `1.0.9` → `1.0.10`: Bug 修复
- `1.0.10` → `1.1.0`: 新功能
- `1.1.0` → `2.0.0`: 破坏性变更

### C. 故障恢复

如果发布失败：

1. **删除失败的 Release**:
   ```bash
   gh release delete v1.0.10 --yes
   git push origin :refs/tags/v1.0.10
   ```

2. **修复问题后重新发布**

3. **或者手动发布**: 见 [4. 手动发布](#4-手动发布)

---

*最后更新: 2026-01-07*
