# Hotel Guest Manager — Java Desktop (Swing)

Cross-platform desktop app. Works on macOS (Intel & Apple Silicon) and Windows 11.

## Requirements
Java 17 or newer — free from https://adoptium.net
Choose: Temurin 21 LTS → your OS → your chip (aarch64 for M1/M2/M3/M4, x64 for Intel)

## Run
- **macOS**: right-click `run-macos.command` → Open (first launch only)
- **Windows**: double-click `run-windows.bat`
- **Any platform**: `java -jar HotelGuestApp.jar`

## Data saved to
- macOS: ~/.hotelguestapp/guests.json
- Windows: C:\Users\<you>\.hotelguestapp\guests.json

## iOS compatibility
Export/import JSON format is identical to the iOS Swift app.
