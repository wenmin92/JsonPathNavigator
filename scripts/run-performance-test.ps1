#!/usr/bin/env pwsh

# JsonPathNavigator 性能测试脚本

param(
    [switch]$Quick = $false  # 快速模式：只运行部分性能测试
)

Write-Host "=================================================="
Write-Host "JsonPathNavigator 性能测试脚本"
Write-Host "=================================================="

if ($Quick) {
    Write-Host "执行快速性能测试..."
    & "./gradlew.bat" test --tests "cc.wenmin92.jsonkeyfinder.performance.JsonSearchPerformanceTest.01_test*" --rerun-tasks
    if ($LASTEXITCODE -eq 0) {
        Write-Host "快速性能测试通过！"
    } else {
        Write-Host "快速性能测试失败！"
        exit 1
    }
} else {
    Write-Host "执行完整性能测试..."
    & "./gradlew.bat" test --tests "cc.wenmin92.jsonkeyfinder.performance.JsonSearchPerformanceTest" --rerun-tasks
    if ($LASTEXITCODE -eq 0) {
        Write-Host "完整性能测试通过！"
    } else {
        Write-Host "完整性能测试失败！"
        exit 1
    }
}

Write-Host "=================================================="
Write-Host "性能测试完成！"
Write-Host "=================================================="
