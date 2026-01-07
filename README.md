# Json Path Navigator

![Build](https://github.com/wenmin92/JsonPathNavigator/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/cc.wenmin92.jsonpathnavigator.svg)](https://plugins.jetbrains.com/plugin/cc.wenmin92.jsonpathnavigator)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/cc.wenmin92.jsonpathnavigator.svg)](https://plugins.jetbrains.com/plugin/cc.wenmin92.jsonpathnavigator)

> 🔍 一个帮助你在项目中快速查找 JSON 键的 JetBrains IDE 插件
>
> A JetBrains IDE plugin for quickly finding JSON keys in your project

<!-- Plugin description -->
A JetBrains IDE plugin for quickly finding JSON keys in your project.

**Features:**
- 🔍 **Quick Search** - Locate JSON keys by path instantly
- 💡 **Smart Suggestions** - Auto-suggest paths as you type
- 👁️ **Value Preview** - Show value preview of matches
- 📍 **One-Click Navigation** - Double-click to jump to file location
- 🖱️ **Context Menu** - Right-click selected text to search
- ⌨️ **Smart Selection** - Auto-detect JSON path at cursor position

**Usage:** Press `Ctrl+Alt+Shift+F` (Windows/Linux) or `⌘+⇧+⌥+F` (macOS) to open the search dialog.
<!-- Plugin description end -->

---

## ✨ 功能特性 / Features

| 功能 | Feature | 说明 / Description |
|------|---------|-------------------|
| 🔍 快速搜索 | Quick Search | 通过键路径快速定位 JSON 键 / Quickly locate JSON keys by path |
| 💡 智能建议 | Smart Suggestions | 输入时自动提示可能的路径 / Auto-suggest paths as you type |
| 👁️ 值预览 | Value Preview | 显示匹配项的值预览 / Show value preview of matches |
| 📍 一键跳转 | One-Click Navigation | 双击结果直接跳转到文件位置 / Double-click to jump to file |
| 🖱️ 右键菜单 | Context Menu | 选中文本后右键即可搜索 / Right-click selected text to search |
| ⌨️ 智能取词 | Smart Selection | 自动识别光标处的 JSON 路径 / Auto-detect JSON path at cursor |

---

## ⚡ 快速开始 / Quick Start

### 🎯 3 种方式打开搜索对话框 / 3 Ways to Open Search Dialog

| 方式 / Method | 操作 / Action |
|--------------|--------------|
| **⌨️ 快捷键** | Windows/Linux: `Ctrl + Alt + Shift + F`<br>macOS: `⌘ + ⇧ + ⌥ + F` |
| **🖱️ 右键菜单** | 选中文本 → 右键 → **Find Key in JSON** |
| **📋 Edit 菜单** | 菜单栏 → Edit → **Find Key in JSON** |

### 📖 基本使用流程 / Basic Usage

```
1️⃣ 按下 Ctrl+Alt+Shift+F 打开搜索对话框
   Press Ctrl+Alt+Shift+F to open search dialog

2️⃣ 输入要搜索的 JSON 键路径，如：database.host
   Enter JSON key path, e.g.: database.host

3️⃣ 查看搜索结果，双击跳转到对应文件位置
   View results, double-click to navigate to file
```

### 🎹 对话框内快捷键 / Dialog Shortcuts

| 操作 / Action | 快捷键 / Shortcut |
|--------------|------------------|
| 执行搜索 / Search | `Enter` |
| 关闭对话框 / Close | `Esc` |
| 跳转到结果 / Navigate | 双击结果行 / Double-click |

---

## 📝 搜索语法 / Search Syntax

使用 `.` (点) 分隔嵌套的键名 / Use `.` (dot) to separate nested keys:

```
简单键 / Simple key:        name
嵌套路径 / Nested path:     config.database.host
多层嵌套 / Deep nesting:    app.settings.ui.theme
```

### 示例 / Example

对于以下 JSON / For this JSON:
```json
{
  "database": {
    "host": "localhost",
    "port": 5432
  }
}
```

| 搜索路径 / Search Path | 找到 / Found |
|----------------------|--------------|
| `database.host` | `"localhost"` |
| `database.port` | `5432` |

---

## 💻 支持的 IDE / Supported IDEs

- IntelliJ IDEA (Community & Ultimate)
- WebStorm
- PyCharm
- GoLand
- PhpStorm
- Rider
- CLion
- 其他 JetBrains IDE (2024.1+) / Other JetBrains IDEs (2024.1+)

---

## 📦 安装方法 / Installation

### 方法一：从 IDE 插件市场安装（推荐）/ Method 1: IDE Marketplace (Recommended)

1. 打开设置 / Open Settings
   - Windows/Linux: `File` → `Settings` → `Plugins`
   - macOS: `IntelliJ IDEA` → `Preferences` → `Plugins`

2. 点击 `Marketplace` 标签，搜索 **"Json Path Navigator"**
   Click `Marketplace` tab, search for **"Json Path Navigator"**

3. 点击 `Install`，然后重启 IDE
   Click `Install`, then restart IDE

### 方法二：手动安装 / Method 2: Manual Installation

1. 从 [GitHub Releases](https://github.com/wenmin92/JsonPathNavigator/releases) 下载 `.zip` 文件
   Download `.zip` from [GitHub Releases](https://github.com/wenmin92/JsonPathNavigator/releases)

2. 打开 IDE 设置 → `Plugins` → ⚙️ → `Install Plugin from Disk...`
   Open IDE Settings → `Plugins` → ⚙️ → `Install Plugin from Disk...`

3. 选择下载的文件，重启 IDE
   Select downloaded file, restart IDE

---

## ⚙️ 自定义快捷键 / Customize Shortcuts

如果默认快捷键与其他插件冲突，可以自定义：

If the default shortcut conflicts with other plugins:

1. 打开设置 → `Keymap`
   Open Settings → `Keymap`

2. 搜索 **"Find Key in JSON"**
   Search for **"Find Key in JSON"**

3. 右键 → `Add Keyboard Shortcut` → 输入新快捷键
   Right-click → `Add Keyboard Shortcut` → Enter new shortcut

---

## 📚 详细文档 / Documentation

- [中文用户指南](docs/USER_GUIDE_CN.md) - 完整的功能说明和使用技巧
- [English User Guide](docs/USER_GUIDE_EN.md) - Complete features and tips
- [开发文档](docs/DEVELOPMENT.md) - 开发者指南

---

## ❓ 常见问题 / FAQ

<details>
<summary><b>Q: 搜索没有结果？/ No search results?</b></summary>

1. 检查键名拼写（大小写敏感）/ Check spelling (case-sensitive)
2. 使用完整路径，如 `config.host` 而非 `host` / Use full path
3. 确保文件扩展名是 `.json` / Ensure file extension is `.json`
4. 确保文件在项目目录内 / Ensure file is in project

</details>

<details>
<summary><b>Q: 支持 JSON5/JSONC 吗？/ Support JSON5/JSONC?</b></summary>

目前只支持标准 JSON 格式。/ Currently only standard JSON is supported.

</details>

<details>
<summary><b>Q: 能搜索数组元素吗？/ Search array elements?</b></summary>

暂不支持数组索引，如 `items[0].name`。/ Array indexing like `items[0].name` is not supported yet.

</details>

---

## 🤝 反馈与贡献 / Feedback & Contributing

- **🐛 问题反馈 / Bug Reports**: [GitHub Issues](https://github.com/wenmin92/JsonPathNavigator/issues)
- **💡 功能建议 / Feature Requests**: [GitHub Discussions](https://github.com/wenmin92/JsonPathNavigator/discussions)
- **⭐ 如果觉得有用，欢迎 Star！/ Star if you find it useful!**

---

## 📄 License

Apache License 2.0

---

*Made with ❤️ by [wenmin92](https://github.com/wenmin92)*
