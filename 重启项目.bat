@echo off
chcp 65001 > nul
echo =======================================================
echo          ApiSensitivities 一键重启脚本
echo =======================================================

echo [1/5] 正在停止现有 Java 进程...
taskkill /F /IM java.exe /T >nul 2>&1
if %errorlevel% equ 0 (
    echo    - 已停止 Java 进程
) else (
    echo    - 未发现运行中的 Java 进程
)

echo [2/5] 正在停止现有 Node/Vite 进程...
taskkill /F /IM node.exe /T >nul 2>&1
if %errorlevel% equ 0 (
    echo    - 已停止 Node 进程
) else (
    echo    - 未发现运行中的 Node 进程
)

echo.
echo [3/5] 正在启动后端服务 (Spring Boot)...
start "Backend - Spring Boot" cmd /k "mvnw.cmd spring-boot:run"

echo.
echo [4/5] 正在启动前端服务 (Vite)...
cd front_end
start "Frontend - Vite" cmd /k "npm run dev"
cd ..

echo.
echo [5/5] 等待服务启动 (约10秒)...
timeout /t 10 > nul

echo.
echo 正在打开浏览器...
start http://localhost:5173

echo.
echo =======================================================
echo          项目已重启！
echo          后端端口: 8080
echo          前端端口: 5173
echo =======================================================
echo 按任意键退出此窗口...
pause > nul
