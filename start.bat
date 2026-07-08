@echo off
setlocal
cd /d "%~dp0"

:: ================================================================
::  Flea Market - Windows Startup
::  Run: double-click, or start.bat from terminal
::  Prerequisites: JDK 17+, Node.js 18+, MySQL running
:: ================================================================

title Flea Market

:: --- .env check --------------------------------------------------
if not exist ".env" (
    echo.
    echo  [SETUP NEEDED] No .env file found.
    echo.
    echo  Please run: setup.bat
    echo  Or copy .env.template to .env and set DB_PASSWORD
    echo.
    pause
    exit /b 1
)

:: --- Load DB_PASSWORD --------------------------------------------
set "DB_PASSWORD=MySql_123456"
for /f "usebackq eol=# tokens=1,2 delims==" %%a in (".env") do (
    if "%%a"=="DB_PASSWORD" set "DB_PASSWORD=%%b"
)

:: --- Prerequisites -----------------------------------------------
where java >nul 2>&1 || (echo [FAIL] JDK 17+ required & pause & exit /b 1)
where node >nul 2>&1 || (
    :: Maybe Node is at C:\Program Files\nodejs but not in PATH
    if exist "C:\Program Files\nodejs\node.exe" (
        set "PATH=C:\Program Files\nodejs;%PATH%"
    ) else (
        echo [FAIL] Node.js 18+ required & pause & exit /b 1
    )
)
if not exist "backend\mvnw.cmd" (
    echo [FAIL] backend\mvnw.cmd missing & pause & exit /b 1
)

cls
echo.
echo  ============================================================
echo    Flea Market
echo    Backend : http://localhost:8081
echo    Frontend: http://localhost:5173
echo  ============================================================
echo.

:: --- Kill old instances ------------------------------------------
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING" 2^>nul') do taskkill /f /pid %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING" 2^>nul') do taskkill /f /pid %%a >nul 2>&1

:: --- Install frontend if needed ----------------------------------
if not exist "frontend\node_modules\" (
    echo  Installing frontend dependencies...
    cd frontend
    call npm install
    if errorlevel 1 (
        echo  [FAIL] npm install failed
        cd ..
        pause & exit /b 1
    )
    cd ..
    echo  Done.
    echo.
)

:: --- Start backend (mvnw wrapper) --------------------------------
echo  Starting backend...
start "FleaMarket Backend" /D "%~dp0backend" cmd /c "set DB_PASSWORD=%DB_PASSWORD% && mvnw.cmd spring-boot:run -DskipTests && pause"

:: --- Start frontend ----------------------------------------------
timeout /t 10 /nobreak >nul
echo  Starting frontend...
start "FleaMarket Frontend" /D "%~dp0frontend" cmd /c "npx vite --host 0.0.0.0 && pause"

echo.
echo  ============================================================
echo    Backend  http://localhost:8081/api
echo    Frontend http://localhost:5173
echo    Login    admin / 12345678
echo  ============================================================
echo.
pause
endlocal
