@echo off
setlocal
cd /d "%~dp0"

:: ============================================================
::  Flea Market - Start (double-click or terminal)
::  Prerequisites: JDK 17+, Node.js 18+, MySQL running
:: ============================================================

title Flea Market

:: --- .env required -------------------------------------------
if not exist ".env" (
    echo [ERROR] .env file not found.
    echo Run setup.bat first, or copy .env.template to .env
    pause & exit /b 1
)

:: --- Read DB_PASSWORD from .env -------------------------------
set "DB_PASSWORD=MySql_123456"
for /f "usebackq eol=# tokens=1,2 delims==" %%a in (".env") do (
    if "%%a"=="DB_PASSWORD" set "DB_PASSWORD=%%b"
)

:: --- Ensure Node.js in PATH ----------------------------------
where node >nul 2>&1
if errorlevel 1 (
    if exist "C:\Program Files\nodejs\node.exe" (
        set "PATH=C:\Program Files\nodejs;%PATH%"
    ) else if exist "D:\Program Files\nodejs\node.exe" (
        set "PATH=D:\Program Files\nodejs;%PATH%"
    ) else (
        echo [ERROR] Node.js not found
        pause & exit /b 1
    )
)

:: --- Ensure Java in PATH -------------------------------------
where java >nul 2>&1
if errorlevel 1 (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
        set "PATH=%JAVA_HOME%\bin;%PATH%"
    ) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-17"
        set "PATH=C:\Program Files\Java\jdk-17\bin;%PATH%"
    )
    where java >nul 2>&1 || (
        echo [ERROR] JDK 17+ not found
        pause & exit /b 1
    )
)

cls
echo.
echo  ============================================================
echo    Flea Market
echo    Backend : http://localhost:8081
echo    Frontend: http://localhost:5173
echo  ============================================================
echo.

:: --- Kill old instances --------------------------------------
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING" 2^>nul') do taskkill /f /pid %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING" 2^>nul') do taskkill /f /pid %%a >nul 2>&1

:: --- Install frontend deps if missing ------------------------
if not exist "frontend\node_modules\" (
    echo  [1/3] Installing frontend dependencies...
    cd frontend
    call npm install
    if errorlevel 1 (
        echo  [FAIL] npm install failed
        cd ..
        pause & exit /b 1
    )
    cd ..
    echo  [OK] Done
    echo.
)

:: --- Start backend -------------------------------------------
echo  [2/3] Starting backend (port 8081)...
set "BE_CMD=cd /d "%~dp0backend" && set DB_PASSWORD=%DB_PASSWORD% && mvnw.cmd spring-boot:run -DskipTests && pause"
start "Backend-8081" cmd /c %BE_CMD%

:: --- Start frontend -------------------------------------------
echo  [3/3] Starting frontend (port 5173)...
timeout /t 10 /nobreak >nul
set "FE_CMD=cd /d "%~dp0frontend" && npx vite --host 0.0.0.0 && pause"
start "Frontend-5173" cmd /c %FE_CMD%

echo.
echo  ============================================================
echo    Backend  : http://localhost:8081/api   (may take 30s)
echo    Frontend : http://localhost:5173
echo    Login    : admin / 12345678
echo  ============================================================
echo.
pause
endlocal
