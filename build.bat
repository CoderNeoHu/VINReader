@echo off
echo ========================================
echo   车架号查询 App - 一键构建
echo ========================================
echo.
echo 请选择构建方式:
echo.
echo   [1] PowerShell 自动安装 + 构建
echo   [2] 仅构建（假设环境已就绪）
echo   [3] 打开 GitHub Actions 在线构建指南
echo.
set /p choice="请输入选项 (1/2/3): "

if "%choice%"=="1" (
    echo.
    echo 正在启动自动安装 + 构建脚本...
    powershell -ExecutionPolicy Bypass -File "%~dp0build.ps1"
    goto :end
)

if "%choice%"=="2" (
    echo.
    echo 正在直接构建...
    if exist "%~dp0gradlew.bat" (
        call "%~dp0gradlew.bat" assembleDebug
        if %errorlevel% equ 0 (
            echo.
            echo APK 构建成功!
            echo 位置: %~dp0app\build\outputs\apk\debug\app-debug.apk
            copy "%~dp0app\build\outputs\apk\debug\app-debug.apk" "%USERPROFILE%\Desktop\" >nul
            echo 已复制到桌面
        ) else (
            echo.
            echo 构建失败，请检查错误信息
        )
    ) else (
        echo.
        echo 未找到 gradlew.bat，请先运行: gradle wrapper
    )
    goto :end
)

if "%choice%"=="3" (
    start https://github.com/new
    echo.
    echo 已打开 GitHub 新建仓库页面
    echo 请按 README.md 中的"方案1"操作
    goto :end
)

echo 无效选项
:end
echo.
pause
