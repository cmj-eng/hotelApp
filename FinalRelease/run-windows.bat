@echo off
cd /d "%~dp0"
java -jar HotelGuestApp.jar
if %errorlevel% neq 0 ( pause )
