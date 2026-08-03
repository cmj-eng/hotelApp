@echo off
:: Hotel Guest Manager — Windows launcher
:: Double-click this file to run the app.

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo Java not found. Please install Java 17+ from https://adoptium.net
    pause
    exit /b 1
)

java -jar HotelGuestApp.jar
if %errorlevel% neq 0 (
    echo.
    echo Error launching app. Make sure Java 17+ is installed.
    pause
)
