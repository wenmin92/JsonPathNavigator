# Json Path Navigator 开发指南

本文档帮助新手开发者理解项目代码结构，并能够轻松上手进行开发。

---

## 目录

1. [项目概述](#1-项目概述)
2. [环境搭建](#2-环境搭建)
3. [项目结构](#3-项目结构)
4. [核心模块详解](#4-核心模块详解)
5. [开发工作流](#5-开发工作流)
6. [常见开发任务](#6-常见开发任务)
7. [调试技巧](#7-调试技巧)
8. [发布流程](#8-发布流程)

---

## 1. 项目概述

### 1.1 插件功能

**Json Path Navigator** 是一个 JetBrains IDE 插件，用于在项目中快速搜索 JSON 文件中的键路径。

```
用户输入: "config.database.host"
        ↓
┌─────────────────────────────────────────┐
│  搜索所有 JSON 文件                       │
│         ↓                               │
│  匹配路径 config → database → host       │
│         ↓                               │
│  返回: 文件名、路径、预览、行号              │
└─────────────────────────────────────────┘
```

### 1.2 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.1.10 | 主要开发语言 |
| Gradle | 8.10.2 | 构建工具 |
| IntelliJ Platform SDK | 2024.1.7 | IDE 插件开发框架 |
| IntelliJ Platform Gradle Plugin | 2.6.0 | Gradle 集成插件 |

---

## 2. 环境搭建

### 2.1 必需软件

1. **JDK 17**
   ```powershell
   # 使用 Scoop 安装 (Windows)
   scoop bucket add java
   scoop install openjdk17
   ```

2. **IntelliJ IDEA** (Community 或 Ultimate)
   - 推荐使用最新版本
   - 安装 Kotlin 插件（通常已内置）

### 2.2 克隆项目

```powershell
git clone https://github.com/wenmin92/JsonPathNavigator.git
cd JsonPathNavigator
```

### 2.3 导入 IDE

1. 打开 IntelliJ IDEA
2. File → Open → 选择项目根目录
3. 等待 Gradle 同步完成

### 2.4 验证环境

```powershell
# 运行测试验证环境
.\gradlew.bat test

# 启动开发 IDE 实例
.\gradlew.bat runIde
```

---

## 3. 项目结构

### 3.1 目录结构详解

```
JsonPathNavigator/
├── build.gradle.kts          # Gradle 构建配置（重要）
├── settings.gradle.kts       # Gradle 设置
├── gradle.properties         # 项目属性配置
├── gradle/
│   └── libs.versions.toml    # 依赖版本管理
│
├── src/
│   ├── main/
│   │   ├── kotlin/           # Kotlin 源代码
│   │   │   └── cc/wenmin92/jsonkeyfinder/
│   │   │       ├── actions/          # 动作处理
│   │   │       │   └── FindJsonKeyAction.kt
│   │   │       ├── service/          # 核心服务
│   │   │       │   └── JsonSearchService.kt
│   │   │       ├── ui/               # 用户界面
│   │   │       │   └── JsonKeyFinderDialog.kt
│   │   │       └── util/             # 工具类
│   │   │           └── LogUtil.kt
│   │   │
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   ├── plugin.xml        # 插件配置（重要）
│   │       │   ├── pluginIcon.svg    # 插件图标
│   │       │   └── json-support.xml  # JSON 支持配置
│   │       └── messages/
│   │           └── MyBundle.properties   # 国际化字符串
│   │
│   └── test/                 # 测试代码
│       ├── kotlin/
│       └── resources/testData/
│
├── docs/                     # 文档目录
│   ├── TESTING.md           # 测试指南
│   ├── DEVELOPMENT.md       # 开发指南（本文档）
│   └── USER_GUIDE*.md       # 用户指南
│
└── build/                    # 构建输出（自动生成）
    ├── reports/              # 测试和覆盖率报告
    └── distributions/        # 插件 ZIP 包
```

### 3.2 关键配置文件

#### `plugin.xml` - 插件核心配置

```xml
<idea-plugin>
    <id>cc.wenmin92.jsonpathnavigator</id>    <!-- 唯一标识 -->
    <name>Json Path Navigator</name>           <!-- 显示名称 -->
    
    <!-- 依赖的模块 -->
    <depends>com.intellij.modules.platform</depends>
    <depends>com.intellij.modules.json</depends>
    
    <!-- 注册的动作 -->
    <actions>
        <action id="JsonPathNavigator.Find"
            class="cc.wenmin92.jsonkeyfinder.actions.FindJsonKeyAction"
            text="Find Key in JSON">
            <keyboard-shortcut keymap="$default" first-keystroke="ctrl alt shift F"/>
            <add-to-group group-id="EditorPopupMenu" anchor="first"/>
        </action>
    </actions>
</idea-plugin>
```

#### `gradle.properties` - 项目属性

```properties
# 插件版本
pluginVersion=1.0.9

# 支持的 IDE 版本范围
pluginSinceBuild=241        # 最低支持 2024.1
platformVersion=2024.1.7    # 开发使用的版本

# JDK 配置
org.gradle.java.home=C:/Users/xxx/scoop/apps/openjdk17/current
```

---

## 4. 核心模块详解

### 4.1 模块关系图

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户交互层                                 │
│  ┌─────────────────────┐    ┌─────────────────────┐             │
│  │  FindJsonKeyAction  │───→│ JsonKeyFinderDialog │             │
│  │    (入口点)          │    │    (搜索对话框)       │             │
│  └─────────────────────┘    └──────────┬──────────┘             │
│                                        │                        │
├────────────────────────────────────────┼────────────────────────┤
│                        业务逻辑层        │                        │
│                             ┌──────────▼──────────┐             │
│                             │  JsonSearchService  │             │
│                             │   (核心搜索引擎)      │             │
│                             └──────────┬──────────┘             │
│                                        │                        │
├────────────────────────────────────────┼────────────────────────┤
│                        基础设施层        │                        │
│  ┌───────────────┐   ┌─────────────────▼─────────────────┐      │
│  │    LogUtil    │   │      IntelliJ Platform API        │      │
│  │   (日志工具)   │   │  (PSI, VFS, FileTypeIndex, etc.)  │       │
│  └───────────────┘   └───────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 FindJsonKeyAction - 动作入口

**文件**: `src/main/kotlin/cc/wenmin92/jsonkeyfinder/actions/FindJsonKeyAction.kt`

```kotlin
class FindJsonKeyAction : AnAction() {
    
    // 用户触发动作时调用
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // 获取初始搜索文本（选中的文本或光标处的路径）
        val initialSearchText = getInitialSearchText(editor, project)
        
        // 显示搜索对话框
        showSearchDialog(project, initialSearchText)
    }
    
    // 更新动作状态（启用/禁用）
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null && e.getData(CommonDataKeys.EDITOR) != null
    }
    
    // 判断文本是否像 JSON 路径
    private fun isLikelyJsonPath(text: String): Boolean {
        return text.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+"))
    }
}
```

**扩展点**:
- 修改 `isLikelyJsonPath()` 可改变路径识别规则
- 修改 `getInitialSearchText()` 可改变初始文本获取逻辑

### 4.3 JsonSearchService - 核心搜索引擎

**文件**: `src/main/kotlin/cc/wenmin92/jsonkeyfinder/service/JsonSearchService.kt`

```kotlin
class JsonSearchService(private val project: Project) {
    
    // 缓存：根属性名称集合
    private var rootPropertiesCache: Set<String>? = null
    
    // 主搜索方法
    fun findKey(searchText: String): List<SearchResult> {
        // 1. 解析搜索路径
        val searchParts = searchText.split(".")
        
        // 2. 快速检查：第一部分是否存在于任何 JSON 文件
        if (!rootPropertiesCache.contains(searchParts[0])) {
            return emptyList()
        }
        
        // 3. 遍历所有 JSON 文件进行搜索
        val jsonFiles = FileTypeIndex.getFiles(JsonFileType.INSTANCE, projectScope)
        for (file in jsonFiles) {
            searchInFile(file, searchParts, results)
        }
        
        return results
    }
    
    // 在单个文件中搜索
    private fun searchInFile(jsonFile: JsonFile, searchParts: List<String>, results: MutableList<SearchResult>) {
        // 逐层遍历 JSON 对象，匹配路径
        var currentObject = jsonFile.topLevelValue as? JsonObject ?: return
        
        for (part in searchParts.dropLast(1)) {
            val property = currentObject.findProperty(part) ?: return
            currentObject = property.value as? JsonObject ?: return
        }
        
        // 找到目标属性
        val targetProperty = currentObject.findProperty(searchParts.last())
        if (targetProperty != null) {
            results.add(SearchResult(file, path, preview, lineNumber))
        }
    }
    
    // 获取搜索建议
    fun getSuggestions(partialKey: String): List<String>
    
    // 验证根属性
    fun isValidRootProperty(propertyName: String): Boolean
    
    // 清除缓存
    fun invalidateCache()
}

// 搜索结果数据类
data class SearchResult(
    val file: VirtualFile,    // 文件
    val path: String,         // JSON 路径
    val preview: String,      // 预览文本
    val lineNumber: Int       // 行号
)
```

**关键概念**:

| 概念 | 说明 |
|------|------|
| `FileTypeIndex` | IntelliJ 的文件类型索引，用于快速获取特定类型的文件 |
| `JsonFile` | JSON 文件的 PSI 表示 |
| `JsonObject` | JSON 对象的 PSI 表示 |
| `JsonProperty` | JSON 属性的 PSI 表示 |
| `PSI` | Program Structure Interface，IntelliJ 的代码模型 |

### 4.4 JsonKeyFinderDialog - 用户界面

**文件**: `src/main/kotlin/cc/wenmin92/jsonkeyfinder/ui/JsonKeyFinderDialog.kt`

```kotlin
class JsonKeyFinderDialog(
    private val project: Project,
    private val initialSearchText: String? = null
) : DialogWrapper(project, false) {
    
    // UI 组件
    private val searchField = JBTextField()          // 搜索输入框
    private val resultsTable = JBTable()             // 结果表格
    private val suggestionList = JBList<String>()    // 建议列表
    
    // 搜索服务
    private val searchService = JsonSearchService(project)
    
    // 执行搜索
    private fun performSearch() {
        // 在后台线程执行搜索
        ApplicationManager.getApplication().executeOnPooledThread {
            val results = searchService.findKey(searchField.text)
            
            // 在 UI 线程更新结果
            SwingUtilities.invokeLater {
                updateSearchResults(results)
            }
        }
    }
    
    // 更新搜索建议
    private fun updateSuggestions() {
        // 防抖处理
        suggestionAlarm.cancelAllRequests()
        suggestionAlarm.addRequest({
            val suggestions = searchService.getSuggestions(searchField.text)
            // 更新 UI...
        }, 200)
    }
    
    // 打开选中的文件
    private fun openSelectedFile() {
        val result = selectedResult
        FileEditorManager.getInstance(project).openTextEditor(
            OpenFileDescriptor(project, result.file, result.lineNumber - 1, 0),
            true
        )
    }
}
```

**UI 结构**:

```
┌─────────────────────────────────────────────────────────────┐
│  Search: [________________________] [Search]                │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐│
│  │  File     │  Path              │  Preview      │  Line  ││
│  ├───────────┼────────────────────┼───────────────┼────────┤│
│  │ config.js │ db.host            │ db.host: "lo..│  15    ││
│  │ app.json  │ db.host            │ db.host: "12..│  23    ││
│  │           │                    │               │        ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│  Suggestions:                                               │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ db.host                                                 ││
│  │ db.port                                                 ││
│  │ db.name                                                 ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 4.5 LogUtil - 日志工具

**文件**: `src/main/kotlin/cc/wenmin92/jsonkeyfinder/util/LogUtil.kt`

```kotlin
class LogUtil(private val ideLogger: Logger, private val className: String) {
    
    fun info(message: String) {
        ideLogger.info(message)
        println("[INFO] [$className] $message")
    }
    
    fun warn(message: String) { ... }
    fun error(message: String, e: Throwable? = null) { ... }
    
    companion object {
        // 使用方式
        inline fun <reified T : Any> getLogger(): LogUtil {
            return LogUtil(logger<T>(), T::class.java.simpleName)
        }
    }
}

// 在其他类中使用
class MyClass {
    private val LOG = LogUtil.getLogger<MyClass>()
    
    fun doSomething() {
        LOG.info("Doing something...")
    }
}
```

---

## 5. 开发工作流

### 5.1 日常开发流程

```
┌─────────────────────────────────────────────────────────────┐
│  1. 修改代码                                                 │
│       ↓                                                     │
│  2. 运行测试: .\gradlew.bat test                             │
│       ↓                                                     │
│  3. 启动开发 IDE: .\gradlew.bat runIde                       │
│       ↓                                                     │
│  4. 手动测试功能                                              │
│       ↓                                                     │
│  5. 提交代码                                                 │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 常用 Gradle 命令

```powershell
# 开发相关
.\gradlew.bat runIde              # 启动带插件的 IDE 实例
.\gradlew.bat buildPlugin         # 构建插件 ZIP 包

# 测试相关
.\gradlew.bat test                # 运行测试
.\gradlew.bat koverHtmlReport     # 生成覆盖率报告

# 构建相关
.\gradlew.bat build               # 完整构建
.\gradlew.bat clean               # 清理构建

# 验证相关
.\gradlew.bat verifyPlugin        # 验证插件兼容性
```

---

## 6. 常见开发任务

### 6.1 添加新的 Action

1. **创建 Action 类**

```kotlin
// src/main/kotlin/cc/wenmin92/jsonkeyfinder/actions/MyNewAction.kt
package cc.wenmin92.jsonkeyfinder.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class MyNewAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // 实现动作逻辑
    }
}
```

2. **在 plugin.xml 中注册**

```xml
<actions>
    <action id="JsonPathNavigator.MyNew"
            class="cc.wenmin92.jsonkeyfinder.actions.MyNewAction"
            text="My New Action"
            description="Description of the action">
        <keyboard-shortcut keymap="$default" first-keystroke="ctrl alt N"/>
        <add-to-group group-id="ToolsMenu" anchor="last"/>
    </action>
</actions>
```

### 6.2 修改搜索逻辑

修改 `JsonSearchService.kt`:

```kotlin
// 示例：添加模糊搜索支持
fun findKeyFuzzy(searchText: String): List<SearchResult> {
    // 实现模糊匹配逻辑
}
```

### 6.3 添加新的 UI 组件

在 `JsonKeyFinderDialog.kt` 中:

```kotlin
// 添加新按钮
private val myNewButton = JButton("New Feature").apply {
    addActionListener {
        // 按钮点击逻辑
    }
}

// 在 createMainPanel() 中添加到界面
panel.add(myNewButton, BorderLayout.SOUTH)
```

### 6.4 添加配置项

1. **创建配置类**

```kotlin
// src/main/kotlin/cc/wenmin92/jsonkeyfinder/settings/PluginSettings.kt
@State(name = "JsonPathNavigatorSettings", storages = [Storage("jsonPathNavigator.xml")])
class PluginSettings : PersistentStateComponent<PluginSettings.State> {
    
    data class State(
        var maxResults: Int = 100,
        var searchDelay: Int = 200
    )
    
    private var state = State()
    
    override fun getState() = state
    override fun loadState(state: State) { this.state = state }
    
    companion object {
        fun getInstance(): PluginSettings = service()
    }
}
```

2. **在 plugin.xml 中注册**

```xml
<extensions defaultExtensionNs="com.intellij">
    <applicationService serviceImplementation="cc.wenmin92.jsonkeyfinder.settings.PluginSettings"/>
</extensions>
```

---

## 7. 调试技巧

### 7.1 日志调试

```kotlin
private val LOG = LogUtil.getLogger<MyClass>()

fun myMethod() {
    LOG.info("Method called with params: $params")
    LOG.debug("Detailed debug info: $details")
}
```

查看日志：
- IDE 日志: Help → Show Log in Explorer
- 控制台输出: 运行 `.\gradlew.bat runIde` 时查看终端

### 7.2 断点调试

1. 运行 `.\gradlew.bat runIde`
2. 在 IDEA 中: Run → Attach to Process
3. 选择运行的 IDE 进程
4. 设置断点，在开发 IDE 中触发功能

### 7.3 PSI 调试

使用 PSI Viewer 插件查看代码结构：

1. 在开发 IDE 中安装 "PsiViewer" 插件
2. Tools → View PSI Structure

---

## 8. 发布流程

> **📖 完整发布指南**: 请查看 [RELEASE.md](RELEASE.md) 获取详细的发布流程、配置说明和问题解决方案。

### 8.1 快速发布步骤

1. **更新版本**:
   ```properties
   # gradle.properties
   pluginVersion=1.0.11
   ```

2. **更新 CHANGELOG.md**:
   ```markdown
   ## [1.0.11] - 2026-01-07
   ### Added
   - 新功能
   ```

3. **提交并推送**:
   ```bash
   git add gradle.properties CHANGELOG.md
   git commit -m "chore: bump version to 1.0.11"
   git push origin main
   ```

4. **等待 GitHub Actions** 构建完成

5. **发布 Release**: 
   - 访问 https://github.com/wenmin92/JsonPathNavigator/releases
   - 找到 Draft release 并点击 **Publish**

6. **验证**: 检查 JetBrains Marketplace 更新

### 8.2 手动构建（本地测试）

```bash
# 构建插件
.\gradlew.bat buildPlugin

# 输出: build/distributions/jsonpathnavigator-{version}.zip
```

**详细说明**: 参见 [RELEASE.md](RELEASE.md)

---

## 附录：IntelliJ Platform API 快速参考

### 常用类

| 类 | 用途 |
|-----|------|
| `AnAction` | 动作基类 |
| `AnActionEvent` | 动作事件 |
| `Project` | 项目对象 |
| `VirtualFile` | 虚拟文件 |
| `PsiFile` | PSI 文件 |
| `DialogWrapper` | 对话框基类 |
| `FileEditorManager` | 文件编辑器管理 |
| `ApplicationManager` | 应用程序管理器 |

### 常用操作

```kotlin
// 获取当前项目
val project = e.project

// 获取当前编辑器
val editor = e.getData(CommonDataKeys.EDITOR)

// 在后台线程执行
ApplicationManager.getApplication().executeOnPooledThread { }

// 在 UI 线程执行
ApplicationManager.getApplication().invokeLater { }

// 读操作（访问 PSI）
ApplicationManager.getApplication().runReadAction { }

// 打开文件
FileEditorManager.getInstance(project).openFile(virtualFile, true)
```

---

*最后更新: 2026-01-05*
