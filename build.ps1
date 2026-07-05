<#
.SYNOPSIS
    车架号查询 App - 一键构建脚本
.DESCRIPTION
    自动检测/安装所需环境并编译 APK
#>

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   车架号查询 App - 一键构建脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ========== 检测 JDK ==========
Write-Host "[1/4] 检测 Java 开发工具包 (JDK)..." -ForegroundColor Yellow
$java = Get-Command "java" -ErrorAction SilentlyContinue
if ($java) {
    $javaVersion = & java -version 2>&1
    Write-Host "  ✓ 已安装: $($javaVersion[0])" -ForegroundColor Green
} else {
    Write-Host "  ✗ 未检测到 Java，正在通过 winget 安装 JDK 17..." -ForegroundColor Yellow
    try {
        winget install "Eclipse Temurin JDK with Hotspot 17" --accept-package-agreements --accept-source-agreements
        Write-Host "  ✓ JDK 17 安装完成" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ winget 安装失败，请手动安装 JDK 17:" -ForegroundColor Red
        Write-Host "    下载地址: https://adoptium.net/temurin/releases/?version=17" -ForegroundColor White
        Write-Host "    安装后重新运行此脚本" -ForegroundColor White
        exit 1
    }
}

# ========== 检测/安装 Android SDK ==========
Write-Host "[2/4] 检测 Android SDK..." -ForegroundColor Yellow
$androidHome = $env:ANDROID_HOME
if (-not $androidHome) {
    $androidHome = Join-Path $env:USERPROFILE "AppData\Local\Android\Sdk"
}

if (Test-Path $androidHome) {
    Write-Host "  ✓ Android SDK 已找到: $androidHome" -ForegroundColor Green
} else {
    Write-Host "  ✗ 未检测到 Android SDK" -ForegroundColor Yellow
    Write-Host "  正在通过 winget 安装 Android Studio (包含 SDK)..." -ForegroundColor Yellow
    try {
        winget install "Google.AndroidStudio" --accept-package-agreements --accept-source-agreements
        Write-Host "  ✓ Android Studio 安装完成" -ForegroundColor Green
        Write-Host "  请先手动打开 Android Studio 完成初始设置 (它会自动安装 SDK)" -ForegroundColor Yellow
        Write-Host "  然后重新运行此脚本" -ForegroundColor Yellow
        pause
        exit 0
    } catch {
        Write-Host "  ✗ 安装失败，请手动安装 Android Studio:" -ForegroundColor Red
        Write-Host "    下载地址: https://developer.android.com/studio" -ForegroundColor White
        Write-Host "    安装后打开一次，它会自动下载 SDK" -ForegroundColor White
        pause
        exit 1
    }
}

# 设置环境变量
$env:ANDROID_HOME = $androidHome
$env:JAVA_HOME = & "$env:ProgramFiles\Eclipse Adoptium\jdk-17.0.10.7-hotspot" -ErrorAction SilentlyContinue
if (-not $env:JAVA_HOME -and (Test-Path "C:\Program Files\Eclipse Adoptium")) {
    $jdkDir = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory | Select-Object -First 1
    if ($jdkDir) { $env:JAVA_HOME = $jdkDir.FullName }
}
if (-not $env:JAVA_HOME) {
    # Try other common JDK locations
    $candidates = @(
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "$env:ProgramFiles\Java"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) {
            $dir = Get-ChildItem $c -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($dir) { $env:JAVA_HOME = $dir.FullName; break }
        }
    }
}

# ========== 构建 APK ==========
Write-Host "[3/4] 准备构建..." -ForegroundColor Yellow
Set-Location $ProjectRoot

# 检测 Gradle Wrapper
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "  正在下载 Gradle Wrapper..." -ForegroundColor Yellow
    # 创建基本的 gradlew.bat
    @"
@rem Gradle wrapper
@if "%DEBUG%"=="" @echo off
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%%~n0
set APP_HOME=%%DIRNAME%%

set DEFAULT_JVM_OPTS="-Xmx64m"

set CLASSPATH=%%APP_HOME%%gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
%%JAVA_HOME%%\bin\java.exe %%DEFAULT_JVM_OPTS%% -classpath "%%CLASSPATH%%" org.gradle.wrapper.GradleWrapperMain %%*

:end
@rem End local scope
"%@" > gradlew.bat

    # 下载 gradle-wrapper.jar
    $wrapperUrl = "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar"
    $wrapperJar = Join-Path $ProjectRoot "gradle\wrapper\gradle-wrapper.jar"
    try {
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $wrapperUrl -OutFile $wrapperJar -UseBasicParsing
        Write-Host "  ✓ Gradle Wrapper 就绪" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ 下载失败: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "  请手动下载 https://services.gradle.org/distributions/gradle-8.5-bin.zip" -ForegroundColor White
        Write-Host "  并解压到系统的 PATH 中，然后运行: gradle assembleDebug" -ForegroundColor White
        pause
        exit 1
    }
}

Write-Host "  正在构建 (首次需要下载依赖，可能需要 5-10 分钟)..." -ForegroundColor Yellow
Write-Host "  请耐心等待..." -ForegroundColor Gray

try {
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  ✅ APK 构建成功!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "APK 位置:" -ForegroundColor Cyan
        $apkPath = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
        if (Test-Path $apkPath) {
            Write-Host "  $apkPath" -ForegroundColor White
            Write-Host ""
            Write-Host "桌面快捷方式:" -ForegroundColor Cyan
            $desktop = [Environment]::GetFolderPath("Desktop")
            Copy-Item $apkPath $desktop -Force
            Write-Host "  已复制到 $desktop\app-debug.apk" -ForegroundColor White
        }
    } else {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Red
        Write-Host "  ❌ 构建失败 (错误码: $LASTEXITCODE)" -ForegroundColor Red
        Write-Host "========================================" -ForegroundColor Red
        Write-Host ""
        Write-Host "请检查错误信息，常见原因:" -ForegroundColor Yellow
        Write-Host "  1. Android SDK 版本不匹配 - 检查 local.properties" -ForegroundColor White
        Write-Host "  2. 网络连接问题 - 确保能访问 maven.google.com" -ForegroundColor White
        Write-Host "  3. 内存不足 - 关闭其他程序" -ForegroundColor White
    }
} catch {
    Write-Host "  ✗ 构建出错: $($_.Exception.Message)" -ForegroundColor Red
}

# ========== 完成 ==========
Write-Host ""
Write-Host "[4/4] 完成!" -ForegroundColor Yellow
Write-Host ""
Write-Host "如果构建成功，APK 文件已复制到桌面" -ForegroundColor Cyan
Write-Host "直接传到手机即可安装使用!" -ForegroundColor Cyan
Write-Host ""

pause
