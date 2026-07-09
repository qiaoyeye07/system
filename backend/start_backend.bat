@echo off
cd /d "%~dp0backend"
set "DB_PASSWORD=%~1"
mvnw.cmd spring-boot:run -DskipTests
pause
