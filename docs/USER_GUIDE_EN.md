# Json Path Navigator User Guide

> A JetBrains IDE plugin to quickly find JSON keys in your project

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Installation](#2-installation)
3. [Quick Start](#3-quick-start)
4. [Features in Detail](#4-features-in-detail)
5. [Tips & Tricks](#5-tips--tricks)
6. [FAQ](#6-faq)

---

## 1. Introduction

### 1.1 What Does This Plugin Do?

Imagine you have multiple JSON configuration files in your project, and you want to find all locations containing the key `database.host`. Normally you would need to:

1. Open each JSON file
2. Manually search for "database"
3. Then find "host"
4. Note down the locations...

**With Json Path Navigator**, you just need to:

1. Press `Ctrl+Alt+Shift+F`
2. Type `database.host`
3. Instantly see all matching results!

### 1.2 Key Features

| Feature | Description |
|---------|-------------|
| 🔍 Quick Search | Quickly locate JSON keys by path |
| 💡 Smart Suggestions | Auto-suggest possible paths as you type |
| 👁️ Preview | Show value preview of matches |
| 📍 One-Click Navigation | Double-click to jump to file location |
| 🖱️ Context Menu | Right-click selected text to search |

### 1.3 Supported IDEs

- IntelliJ IDEA (Community & Ultimate)
- WebStorm
- PyCharm
- GoLand
- Other JetBrains IDEs (2024.1+)

---

## 2. Installation

### Method 1: Install from IDE Plugin Marketplace (Recommended)

1. Open IDE Settings
   - Windows/Linux: `File` → `Settings`
   - macOS: `IntelliJ IDEA` → `Preferences`

2. Go to Plugins
   - Click `Plugins` in the left sidebar

3. Search and Install
   - Click the `Marketplace` tab
   - Search for "Json Path Navigator"
   - Click the `Install` button

4. Restart IDE

### Method 2: Manual Installation

1. Download the Plugin
   - Download the latest `.zip` file from [GitHub Releases](https://github.com/wenmin92/JsonPathNavigator/releases)

2. Install the Plugin
   - Open IDE Settings → `Plugins`
   - Click the ⚙️ icon → `Install Plugin from Disk...`
   - Select the downloaded `.zip` file

3. Restart IDE

---

## 3. Quick Start

### 3.1 First Time Use

**Step 1**: Open any file (doesn't have to be a JSON file)

**Step 2**: Press the keyboard shortcut
- Windows/Linux: `Ctrl + Alt + Shift + F`
- macOS: `⌘ + ⇧ + ⌥ + F`

**Step 3**: Type the key path you want to search in the dialog

```
For example: config.database.host
```

**Step 4**: View search results, double-click to jump to the location

### 3.2 30-Second Demo

```
┌─────────────────────────────────────────────────────────────┐
│  1. Suppose your project has these JSON files:              │
│                                                             │
│     config/app.json:                                        │
│     {                                                       │
│       "database": {                                         │
│         "host": "localhost",                                │
│         "port": 5432                                        │
│       }                                                     │
│     }                                                       │
│                                                             │
│     config/prod.json:                                       │
│     {                                                       │
│       "database": {                                         │
│         "host": "prod.example.com",                         │
│         "port": 5432                                        │
│       }                                                     │
│     }                                                       │
├─────────────────────────────────────────────────────────────┤
│  2. Press Ctrl+Alt+Shift+F, type "database.host"            │
├─────────────────────────────────────────────────────────────┤
│  3. See search results:                                     │
│                                                             │
│     ┌────────────┬───────────────┬────────────────┬──────┐  │
│     │ File       │ Path          │ Preview        │ Line │  │
│     ├────────────┼───────────────┼────────────────┼──────┤  │
│     │ app.json   │ database.host │ host: "local.. │  3   │  │
│     │ prod.json  │ database.host │ host: "prod... │  3   │  │
│     └────────────┴───────────────┴────────────────┴──────┘  │
├─────────────────────────────────────────────────────────────┤
│  4. Double-click any result to jump directly to that        │
│     location in the file!                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Features in Detail

### 4.1 Search Dialog

After opening the search dialog, you'll see:

```
┌─────────────────────────────────────────────────────────────┐
│  Find JSON Key                                        [X]   │
├─────────────────────────────────────────────────────────────┤
│  Search: [database.host_______________] [Search]            │
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ File        │ Path            │ Preview         │ Line  ││
│  ├─────────────┼─────────────────┼─────────────────┼───────┤│
│  │ app.json    │ database.host   │ host: "local... │  3    ││
│  │ prod.json   │ database.host   │ host: "prod.e.. │  3    ││
│  │             │                 │                 │       ││
│  └─────────────────────────────────────────────────────────┘│
│                                                             │
│  Suggestions:                                               │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ database.host                                           ││
│  │ database.port                                           ││
│  │ database.name                                           ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

**Components Explained**:

| Area | Function |
|------|----------|
| Search Box | Enter the JSON key path to find |
| Search Button | Click or press Enter to search |
| Results Table | Shows all matching results |
| Suggestions List | Shows possible path suggestions while typing |

### 4.2 Search Path Syntax

Use `.` (dot) to separate nested key names:

```
Simple key:     name
Nested path:    config.database.host
Deep nesting:   app.settings.ui.theme.color
With underscore: my_config.db_host
```

**Example JSON and Corresponding Search Paths**:

```json
{
  "app": {
    "name": "MyApp",
    "settings": {
      "debug": true,
      "log_level": "info"
    }
  },
  "database": {
    "host": "localhost"
  }
}
```

| Search Path | Found Value |
|-------------|-------------|
| `app.name` | "MyApp" |
| `app.settings.debug` | true |
| `app.settings.log_level` | "info" |
| `database.host` | "localhost" |

### 4.3 Context Menu Search

1. Select some text in your code (that looks like a JSON path)
2. Right-click
3. Select "Find Key in JSON"

```
For example, in your Java/Kotlin/JS code:

String host = config.get("database.host");
                         └──────────────┘
                          Select this text, then right-click to search
```

### 4.4 Smart Suggestions

When you start typing, the plugin automatically provides suggestions:

```
Type: "data"
      ↓
Suggestions: database.host
             database.port
             database.name
             data.users.count
```

Click any suggestion to auto-fill and search.

---

## 5. Tips & Tricks

### 5.1 Keyboard Shortcuts

| Action | Windows/Linux | macOS |
|--------|---------------|-------|
| Open search dialog | `Ctrl+Alt+Shift+F` | `⌘+⇧+⌥+F` |
| Execute search | `Enter` | `Enter` |
| Close dialog | `Esc` | `Esc` |
| Jump to result | Double-click row | Double-click row |

### 5.2 Efficient Search Tips

**Tip 1**: Start from selected text
```
If you have a path string in your code, select it first, then open 
the search dialog - the path will be auto-filled.
```

**Tip 2**: Use suggestions to speed up input
```
Just type the first few characters, then select from suggestions - 
much faster than typing the full path.
```

**Tip 3**: Search parent paths
```
If you're unsure of the full path, search the parent path to see 
all child keys:
Type "database" instead of "database.host"
```

### 5.3 Sorting Results

Click table headers to sort:
- Sort by filename
- Sort by path
- Sort by line number

---

## 6. FAQ

### Q1: No search results?

**Possible causes and solutions**:

1. **Typo in path**
   - Check if the key name is correct
   - Note that JSON keys are case-sensitive

2. **Incomplete path**
   - Make sure to use the full path starting from the root key
   - For example: use `config.host` not just `host`

3. **JSON file not in project**
   - Make sure JSON files are within the project directory
   - Check if the file is excluded by .gitignore

4. **File extension is not .json**
   - The plugin only searches files with `.json` extension

### Q2: How to change the keyboard shortcut?

1. Open Settings → `Keymap`
2. Search for "Find Key in JSON"
3. Right-click → `Add Keyboard Shortcut`
4. Enter your new shortcut combination

### Q3: Search is slow?

**Optimization suggestions**:

1. Enter a more specific path (reduces matching scope)
2. Close unnecessary project folders
3. Wait for project indexing to complete before searching

### Q4: Does it support JSON5 or JSONC?

Currently only standard JSON format is supported. JSON with comments (JSONC) and JSON5 are not supported yet.

### Q5: Can it search elements in arrays?

Array index searching (like `items[0].name`) is not currently supported. Only object key path searching is supported.

---

## Feedback & Support

- **Bug Reports**: [GitHub Issues](https://github.com/wenmin92/JsonPathNavigator/issues)
- **Feature Requests**: [GitHub Discussions](https://github.com/wenmin92/JsonPathNavigator/discussions)

---

*Version: 1.0.9 | Last Updated: 2026-01-05*
