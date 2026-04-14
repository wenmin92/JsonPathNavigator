# JsonPathNavigator 版本测试指南

本文档提供了在多个 IntelliJ IDEA 版本中测试 JsonPathNavigator 插件的详细方法。

---

## 📋 测试版本清单

### 🎯 **必须测试的核心版本**

| IDE 版本 | 构建号 | 测试优先级 | 测试重点 |
|---------|--------|-----------|----------|
| **IntelliJ IDEA 2024.1.7** | 241 | 🔴 **最高** | 最低支持版本，基础功能验证 |
| **IntelliJ IDEA 2024.3.5** | 243 | 🔴 **最高** | JSON模块分离，API兼容性关键 |
| **IntelliJ IDEA 2025.1.3** | 251 | 🟡 **高** | 2025年稳定版 |
| **IntelliJ IDEA 2025.3.3** | 253 | 🟡 **高** | 统一发行版开始 |
| **IntelliJ IDEA 2026.1.1** | 261 | 🟡 **高** | 最新稳定版 |

### 🌟 **扩展测试版本（推荐）**

| IDE 版本 | 构建号 | 测试优先级 | 测试重点 |
|---------|--------|-----------|----------|
| **IntelliJ IDEA 2024.2.6** | 242 | 🟢 **中** | JSON模块分离前的最后版本 |
| **IntelliJ IDEA 2025.2.3** | 252 | 🟢 **中** | 2025年中期版本 |

---

## 🔍 **自动化测试执行**

### 1. **运行所有测试**

```powershell
# 清理并运行所有测试（推荐）
.\gradlew.bat clean test

# 只运行测试，不清理（较快的迭代）
.\gradlew.bat test --rerun-tasks

# 生成测试覆盖率报告
.\gradlew.bat clean koverHtmlReport --rerun-tasks
```

### 2. **运行特定测试类别**

```powershell
# 运行核心服务测试
.\gradlew.bat test --tests "cc.wenmin92.jsonkeyfinder.service.JsonSearchServiceTest"

# 运行兼容性测试
.\gradlew.bat test --tests "cc.wenmin92.jsonkeyfinder.compatibility.VersionCompatibilityTest"

# 运行错误处理测试
.\gradlew.bat test --tests "cc.wenmin92.jsonkeyfinder.service.JsonSearchServiceErrorHandlingTest"

# 运行性能测试
.\gradlew.bat test --tests "cc.wenmin92.jsonkeyfinder.performance.JsonSearchPerformanceTest"

# 运行API兼容性测试
.\gradlew.bat test --tests "cc.wenmin92.jsonkeyfinder.service.JsonApiCompatibilityTest"
```

### 3. **查看测试结果**

```powershell
# HTML测试报告
start build/reports/tests/test/index.html

# 覆盖率报告
start build/reports/kover/html/index.html

# 验证报告（如果是verifyPlugin）
start build/reports/pluginVerifier/
```

### 4. **关键测试指标**

| 测试类别 | 预期测试数量 | 通过标准 |
|---------|-------------|----------|
| **基础功能测试** | 25+ | 全部通过 |
| **兼容性测试** | 10+ | 全部通过 |
| **错误处理测试** | 30+ | 全部通过 |
| **性能测试** | 10+ | < 2秒/搜索 |
| **API兼容性测试** | 15+ | 全部通过 |

---

## 🧪 **手动测试执行流程**

### 1. **自动化测试后手动验证**

```powershell
# 1. 构建插件
.\gradlew.bat clean buildPlugin

# 2. 在目标版本中测试
.\gradlew.bat runIde "-PplatformVersion=2024.3.5"

# 3. 测试功能
#   - 打开测试项目
#   - 创建JSON文件
#   - 测试快捷键 Ctrl+Alt+Shift+F
#   - 测试右键菜单
#   - 验证搜索功能
```

### 2. **手动测试检查清单**

#### ✅ **基础功能测试（每个版本必做）**

| 测试项目 | 操作步骤 | 预期结果 |
|---------|---------|----------|
| **插件加载** | 启动IDE → 插件管理 → 查看插件 | 插件显示"已安装" |
| **快捷键** | 打开任何文件 → 按 Ctrl+Alt+Shift+F | 打开搜索对话框 |
| **右键菜单** | 选中文本 → 右键 → Find Key in JSON | 菜单项可见 |
| **基本搜索** | 创建simple.json → 搜索"name" | 找到结果 |
| **嵌套搜索** | 创建nested.json → 搜索"a.b.c" | 找到结果 |
| **结果跳转** | 双击搜索结果 | 跳转到正确位置 |
| **无结果处理** | 搜索"nonexistent" | 显示"未找到" |

#### ✅ **版本特定测试**

| 版本 | 特殊测试项目 | 验证要点 |
|------|-------------|----------|
| **2024.1.x** | JSON模块内嵌 | 所有功能正常 |
| **2024.3.x** | JSON模块分离 | com.intellij.modules.json可用 |
| **2025.3.x+** | 统一发行版 | Community功能正常 |

### 3. **版本特定测试命令**

```powershell
# 测试所有关键版本
.\gradlew.bat runIde "-PplatformVersion=2024.1.7"
.\gradlew.bat runIde "-PplatformVersion=2024.3.5"
.\gradlew.bat runIde "-PplatformVersion=2025.1.3"
.\gradlew.bat runIde "-PplatformVersion=2025.3.3"
.\gradlew.bat runIde "-PplatformVersion=2026.1.1"

# 或使用本地已安装的IDE（更快）
# 在gradle.properties中设置platformLocalPath，然后运行
```

---

## 🚨 **兼容性验证**

### 1. **运行兼容性检查**

```powershell
# 运行完整兼容性验证（会下载多个IDE版本）
.\gradlew.bat verifyPlugin

# 只验证插件结构（快速）
.\gradlew.bat verifyPluginStructure

# 强制重新验证
.\gradlew.bat verifyPlugin --rerun-tasks
```

### 2. **验证重点关注**

| 问题类型 | 严重程度 | 预期结果 |
|---------|----------|----------|
| **Compatibility problems** | ❌ **必须修复** | 无此类问题 |
| **Deprecated API usage** | ⚠️ **建议修复** | 无此类问题 |
| **Internal API usage** | ⚠️ **了解风险** | 可接受 |
| **Plugin structure warnings** | ⚠️ **建议修复** | 无此类问题 |

### 3. **处理常见验证问题**

```text
1. 方法不存在
   - 错误：Invocation of unresolved method
   - 解决：调整pluginSinceBuild版本或使用条件代码

2. 类不存在
   - 错误：Access to unresolved class
   - 解决：检查目标版本中是否支持该类

3. 已废弃API
   - 错误：Deprecated API usage
   - 解决：更新为推荐的API（已修复）
```

---

## 📊 **测试报告分析**

### 1. **测试报告位置**

```
# 单元测试报告
build/reports/tests/test/index.html

# 覆盖率报告
build/reports/kover/html/index.html

# 兼容性验证报告
build/reports/pluginVerifier/
```

### 2. **覆盖率目标**

| 模块 | 当前覆盖率 | 目标覆盖率 |
|------|-----------|-----------|
| JsonSearchService | ~85% | > 80% |
| FindJsonKeyAction | ~70% | > 60% |
| 错误处理 | ~75% | > 70% |
| 性能测试 | N/A | > 90%通过 |

### 3. **性能基准测试**

| 测试场景 | 可接受时间 | 优秀时间 |
|---------|-----------|---------|
| 单个文件搜索 | < 1秒 | < 0.5秒 |
| 多文件搜索 | < 2秒 | < 1秒 |
| 大文件搜索 | < 3秒 | < 2秒 |
| 深层嵌套搜索 | < 5秒 | < 3秒 |

---

## 🔄 **测试流程建议**

### 1. **回归测试流程**

```powershell
# 1. 代码修改后运行
.\gradlew.bat clean test

# 2. 运行核心功能测试
.\gradlew.bat test --tests "cc.wenmin92.jsonkeyfinder.service.JsonSearchServiceTest"

# 3. 验证覆盖率和性能
.\gradlew.bat koverHtmlReport

# 4. 手动快速测试
.\gradlew.bat runIde
```

### 2. **发布前完整测试流程**

```powershell
# 1. 完整构建和测试
.\gradlew.bat clean build

# 2. 所有自动化测试
.\gradlew.bat test --rerun-tasks

# 3. 兼容性验证
.\gradlew.bat verifyPlugin

# 4. 覆盖率检查
.\gradlew.bat koverHtmlReport

# 5. 手动测试所有版本
# 5.1 本地测试（使用本地IDE）
# 5.2 CI测试（使用gradle参数）

# 6. 性能测试
.\gradlew.bat test --tests "cc.wenmin92.jsonkeyfinder.performance.JsonSearchPerformanceTest"
```

### 3. **CI/CD 集成测试**

```yaml
# GitHub Actions示例
jobs:
  test:
    strategy:
      matrix:
        version: ['241.14494.304', '243.20346.77', '251.1087.41', '253.6341.17']
    steps:
      - uses: actions/checkout@v3
      - name: Test on ${{ matrix.version }}
        run: |
          ./gradlew test -PplatformVersion=${{ matrix.version }}
```

---

## 🚀 **测试优化建议**

### 1. **自动化测试增强**

```powershell
# 添加性能监控到测试中
# 在性能测试类中添加：

@Test
fun `test search performance is acceptable`() {
    val results = measurePerformance("Simple search") {
        searchService.findKey("test")
    }
    assertTrue("Performance should be acceptable", results < 1000) // 1秒
}
```

### 2. **并行测试优化**

```powershell
# 并行运行测试以提高速度
.\gradlew.bat test --parallel --max-workers=4
```

### 3. **测试数据管理**

```powershell
# 创建测试数据辅助类
object TestDataGenerator {
    fun generateLargeJson(size: Int): String {
        // 生成大型测试数据
    }
    
    fun generateNestedJson(depth: Int): String {
        // 生成嵌套测试数据
    }
}
```

---

## 📞 **获取帮助**

如果在测试过程中遇到问题：

1. **查看日志**：`--info` 或 `--debug` 参数获取详细日志
2. **检查依赖**：确认所有依赖版本正确
3. **验证环境**：确保JDK版本正确（需要JDK 17）
4. **查看文档**：参考 [TESTING.md](../TESTING.md) 获取更多帮助

```powershell
# 获取详细帮助
.\gradlew.bat test --info
```

---

*最后更新：2026-04-08*