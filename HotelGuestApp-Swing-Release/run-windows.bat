@echo off
cd /d "%~dp0"
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo Java not found. Install Java 17+ from https://adoptium.net
    pause & exit /b 1
)
java -jar HotelGuestApp.jar
if %errorlevel% neq 0 ( pause )
