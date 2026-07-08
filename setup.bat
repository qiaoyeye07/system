@echo off
setlocal
cd /d "%~dp0"

:: ================================================================
::  Flea Market - First-Run Setup (Windows)
::  Run once per machine. Creates .env with your MySQL password.
:: ================================================================

title Flea Market Setup
cls
echo.
echo  ============================================================
echo    Flea Market - First-Time Setup
echo  ============================================================
echo.
echo  This will create a .env file with your MySQL settings.
echo.

:: --- MySQL password ---------------------------------------------
set "DB_PASSWORD=MySql_123456"
set /p DB_PASSWORD="  MySQL root password [MySql_123456]: "

:: --- Write .env -------------------------------------------------
echo # Flea Market environment config > .env
echo # Created by setup.bat >> .env
echo DB_PASSWORD=%DB_PASSWORD% >> .env
echo DB_HOST=localhost >> .env
echo DB_PORT=3306 >> .env
echo DB_NAME=flea_market >> .env
echo DB_USER=root >> .env
echo JWT_SECRET=flea-market-jwt-secret-change-in-production >> .env

echo.
echo  [OK] .env created.
echo.
echo  Next: Initialize the database.
echo  Run in PowerShell:  powershell -ExecutionPolicy Bypass -File setup.ps1
echo  Or import manually:  mysql -u root -p ^< database\init.sql
echo.
echo  Then run: start.bat
echo.
pause
endlocal
