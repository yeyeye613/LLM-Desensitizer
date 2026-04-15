@echo off
chcp 65001 > nul
title ApiSensitivities 启动脚本

echo =======================================================
echo          ApiSensitivities 一键重启脚本
echo =======================================================

echo [1/5] 正在停止现有 Java 进程...
taskkill /F /IM java.exe /T >nul 2>&1
echo    完成

echo [2/5] 正在停止现有 Node/Vite 进程...
taskkill /F /IM node.exe /T >nul 2>&1
echo    完成

echo.
echo [3/5] 正在启动后端服务...
start "Backend - Spring Boot" cmd /k "mvnw.cmd spring-boot:run"
echo    后端已启动

echo.
echo [4/5] 检查前端目录...
if not exist "front_end\" (
    echo [错误] 找不到 front_end 目录
    pause
    exit /b 1
)
cd front_end
echo    当前目录: %cd%

echo.
echo [5/5] 安装前端依赖...
if not exist "node_modules\" (
    echo    正在执行 npm install...
    npm install
    if %errorlevel% neq 0 (
        echo.
        echo ========== npm install 失败 ==========
        echo 请手动执行以下命令：
        echo   cd front_end
        echo   npm install
        echo ======================================
        pause
        exit /b 1
    )
) else (
    echo    依赖已存在
)

echo.
echo 启动前端服务...
start "Frontend - Vite" cmd /k "npm run dev"
cd ..

echo.
echo 等待 5 秒...
timeout /t 5 > nul

start http://localhost:5173

echo.
echo =======================================================
echo 启动完成！
echo 后端: http://localhost:8080
echo 前端: http://localhost:5173
echo =======================================================
echo 按任意键关闭此窗口...
pause > nul