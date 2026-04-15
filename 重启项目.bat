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
echo [4/5] 检查前端目录...
if not exist "front_end\" (
    echo    [错误] 找不到 front_end 目录！
    echo    请确保脚本在项目根目录下运行
    echo    当前目录: %cd%
    pause
    exit /b 1
)

cd front_end
echo    当前前端目录: %cd%

echo.
echo [5/5] 检查并安装前端依赖...
if not exist "node_modules\" (
    echo    - 首次运行，正在安装依赖 (npm install)...
    echo    这可能需要几分钟时间，请耐心等待...
    call npm install
    if %errorlevel% neq 0 (
        echo.
        echo    [错误] npm install 失败！
        echo    可能的原因：
        echo    1. 网络连接问题
        echo    2. Node.js 未安装或版本过低
        echo    3. 需要切换 npm 镜像源（如：npm config set registry https://registry.npmmirror.com）
        echo.
        pause
        exit /b 1
    )
    echo    - 依赖安装成功
) else (
    echo    - 依赖已存在，跳过安装
)

echo.
echo [6/6] 正在启动前端服务 (Vite)...
start "Frontend - Vite" cmd /k "npm run dev"
cd ..

echo.
echo 等待服务启动 (约10秒)...
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
echo.
echo 提示：如果前端未正常启动，请手动检查 front_end 目录
echo 并按任意键退出此窗口...
pause > nul