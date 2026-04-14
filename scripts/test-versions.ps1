#!/usr/bin/env pwsh

# JsonPathNavigator 版本测试脚本
# 此脚本自动测试所有指定的 IntelliJ IDEA 版本

param(
    [string]$Version = "all",          # 测试版本：all, core, full
    [switch]$Quick = $false,           # 快速模式：只测试核心功能
    [switch]$Clean = $false,           # 清理构建
    [switch]$Report = $false,          # 生成报告
    [switch]$Manual = $false           # 是否需要手动操作
)

# 颜色定义
$red = "`e[91m"
$green = "`e[92m"
$yellow = "`e[93m"
$blue = "`e[94m"
$magenta = "`e[95m"
$cyan = "`e[96m"
$reset = "`e[0m"

# 版本配置
$coreVersions = @("2024.1.7", "2024.3.5", "2025.1.3", "2025.3.3", "2026.1")
$extendedVersions = @("2024.2.6", "2025.2.3")
$allVersions = $coreVersions + $extendedVersions

Write-Host "$cyan" "==================================================" $reset
Write-Host "$cyan" "JsonPathNavigator 版本测试脚本" $reset
Write-Host "$cyan" "==================================================" $reset

# 根据参数选择版本
if ($Version -eq "core") {
    $testVersions = $coreVersions
    Write-Host "$yellow" "执行核心版本测试" $reset
} elseif ($Version -eq "full") {
    $testVersions = $allVersions
    Write-Host "$yellow" "执行完整版本测试" $reset
} else {
    $testVersions = $allVersions
    Write-Host "$yellow" "执行所有版本测试" $reset
}

# 清理选项
if ($Clean) {
    Write-Host "$blue" "清理构建..." $reset
    & "./gradlew.bat" clean
}

# 预测试检查
function Test-Prerequisites {
    Write-Host "$yellow" "检查环境..." $reset

    # 检查 Gradle
    if (-not (Test-Path "./gradlew.bat")) {
        Write-Host "$red" "错误: 找不到 gradlew.bat" $reset
        exit 1
    }

    # 检查 JDK
    $javaVersion = & java -version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "$red" "错误: Java 未安装或不可用" $reset
        exit 1
    }

    Write-Host "$green" "环境检查通过" $reset
}

# 运行自动化测试
function Run-AutomatedTests {
    Write-Host "$yellow" "运行自动化测试..." $reset

    & "./gradlew.bat" test --rerun-tasks
    if ($LASTEXITCODE -ne 0) {
        Write-Host "$red" "自动化测试失败！" $reset
        exit 1
    }

    Write-Host "$green" "自动化测试通过" $reset
}

# 运行性能测试
function Run-PerformanceTests {
    Write-Host "$yellow" "运行性能测试..." $reset

    & "./gradlew.bat" test --tests "cc.wenmin92.jsonkeyfinder.performance.JsonSearchPerformanceTest" --rerun-tasks
    if ($LASTEXITCODE -ne 0) {
        Write-Host "$red" "性能测试失败！" $reset
        exit 1
    }

    Write-Host "$green" "性能测试通过" $reset
}

# 测试指定版本
function Test-Version {
    param([string]$Version)

    Write-Host $cyan "测试版本: $Version" $reset

    # 检查是否已安装该版本
    $localIdePath = Get-LocalIdePath $Version
    if ($localIdePath) {
        Write-Host $green "使用本地 IDE: $localIdePath" $reset
        $cmd = "./gradlew.bat runIde `"-PplatformVersion=$Version`" `"-DplatformLocalPath=$localIdePath`""
    } else {
        Write-Host $yellow "在线下载 IDE 版本..." $reset
        $cmd = "./gradlew.bat runIde `"-PplatformVersion=$Version`""
    }

    # 运行测试
    if ($Quick) {
        # 快速模式：只检查插件加载和基本功能
        $cmd += " -Dtest.quick=true"
    }

    Invoke-Expression $cmd

    if ($Manual) {
        Write-Host $magenta "请在打开的 IDE 中手动测试以下功能：" $reset
        Write-Host "1. 插件是否能正常加载"
        Write-Host "2. 快捷键 Ctrl+Alt+Shift+F 是否工作"
        Write-Host "3. 右键菜单是否显示"
        Write-Host "4. 基本搜索功能是否正常"
        Write-Host $magenta "按 Enter 继续..." $reset
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    }
}

# 获取本地 IDE 路径
function Get-LocalIdePath {
    param([string]$Version)

    $possiblePaths = @(
        "C:/Program Files/JetBrains/IntelliJ IDEA $Version",
        "C:/Program Files/JetBrains/IntelliJ IDEA $Version Ultimate",
        "C:/Program Files/JetBrains/IntelliJ IDEA Community Edition",
        "C:/Program Files/JetBrains/IntelliJ IDEA Ultimate",
        "C:/Program Files/JetBrains/IntelliJ IDEA 2026.1"
    )

    foreach ($path in $possiblePaths) {
        if (Test-Path $path -PathType Container) {
            # 验证是否是有效的 IDE 安装
            if (Test-Path "$path/bin") {
                return $path
            }
        }
    }

    return $null
}

# 生成测试报告
function Generate-Report {
    Write-Host "$yellow" "生成测试报告..." $reset

    & "./gradlew.bat" koverHtmlReport --rerun-tasks
    & "./gradlew.bat" verifyPluginStructure

    $testReport = Join-Path $PWD "build" "reports" "tests" "test" "index.html"
    $coverageReport = Join-Path $PWD "build" "reports" "kover" "html" "index.html"

    if (Test-Path $testReport) {
        Write-Host "$green" "测试报告: $testReport" $reset
        Start-Process $testReport
    }

    if (Test-Path $coverageReport) {
        Write-Host "$green" "覆盖率报告: $coverageReport" $reset
        Start-Process $coverageReport
    }
}

# 主流程
function Main {
    Test-Prerequisites

    # 自动化测试（所有版本都需要）
    Run-AutomatedTests

    if (-not $Quick) {
        Run-PerformanceTests
    }

    # IDE 版本测试
    foreach ($version in $testVersions) {
        Test-Version -Version $version
    }

    # 生成报告
    if ($Report) {
        Generate-Report
    }

    Write-Host $green "==================================================" $reset
    Write-Host $green "所有测试完成！" $reset
    Write-Host $green "==================================================" $reset

    # 显示结果摘要
    if ($Version -eq "core") {
        Write-Host $cyan "核心版本 ($($coreVersions.Length)) 测试完成" $reset
    } elseif ($Version -eq "full") {
        Write-Host $cyan "完整版本 ($($allVersions.Length)) 测试完成" $reset
    } else {
        Write-Host $cyan "所有版本 ($($allVersions.Length)) 测试完成" $reset
    }
}

# 执行主流程
Main