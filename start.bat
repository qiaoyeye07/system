@echo off
setlocal
cd /d "%~dp0"

title Flea Market

:: --- Read .env -------------------------------------------------
if not exist ".env" (
    echo [ERROR] .env file not found. Run setup.bat first.
    pause & exit /b 1
)
for /f "usebackq eol=# tokens=1,2 delims==" %%a in (".env") do (
    if "%%a"=="DB_PASSWORD" set "DB_PASSWORD=%%b"
)
if not defined DB_PASSWORD set "DB_PASSWORD=WangQi1029."

cls
echo.
echo  ============================================================
echo    Flea Market
echo    Backend : http://localhost:8081
echo    Frontend: http://localhost:5173
echo  ============================================================
echo.

:: --- Kill old processes ----------------------------------------
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING" 2^>nul') do taskkill /f /pid %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING" 2^>nul') do taskkill /f /pid %%a >nul 2>&1

:: --- Install frontend deps -------------------------------------
if not exist "frontend\node_modules\" (
    echo  [1/3] Installing frontend dependencies...
    cd frontend
    call npm install
    cd ..
    echo  [OK] Done.
    echo.
)

:: --- Generate and launch backend -------------------------------
echo  [2/3] Starting backend (port 8081)...
set "BACKEND_BAT=%TEMP%\fleamarket_backend.bat"
> "%BACKEND_BAT%" echo @echo off
>> "%BACKEND_BAT%" echo cd /d "%~dp0backend"
>> "%BACKEND_BAT%" echo set "DB_PASSWORD=%DB_PASSWORD%"
>> "%BACKEND_BAT%" echo :: Auto-detect JAVA_HOME
>> "%BACKEND_BAT%" echo if not defined JAVA_HOME (
>> "%BACKEND_BAT%" echo   for %%%%d in ^("D:\Program Files\Java\jdk-17" "C:\Program Files\Java\jdk-17"^) do ^(
>> "%BACKEND_BAT%" echo     if exist "%%%%~d\bin\java.exe" set "JAVA_HOME=%%%%~d"
>> "%BACKEND_BAT%" echo   ^)
>> "%BACKEND_BAT%" echo ^)
>> "%BACKEND_BAT%" echo if defined JAVA_HOME set "PATH=%%JAVA_HOME%%\bin;%%PATH%%"
>> "%BACKEND_BAT%" echo mvnw.cmd spring-boot:run -DskipTests
>> "%BACKEND_BAT%" echo pause
start "Backend-8081" "%BACKEND_BAT%"

:: --- Generate and launch frontend ------------------------------
echo  [3/3] Starting frontend (port 5173)...
timeout /t 8 /nobreak >nul
set "FRONTEND_BAT=%TEMP%\fleamarket_frontend.bat"
> "%FRONTEND_BAT%" echo @echo off
>> "%FRONTEND_BAT%" echo cd /d "%~dp0frontend"
>> "%FRONTEND_BAT%" echo npx vite --host 0.0.0.0
>> "%FRONTEND_BAT%" echo pause
start "Frontend-5173" "%FRONTEND_BAT%"

echo.
echo  ============================================================
echo    Backend  : http://localhost:8081/api   (takes ~30s)
echo    Frontend : http://localhost:5173
echo    Login    : admin / 12345678
echo  ============================================================
echo.
pause
endlocal
