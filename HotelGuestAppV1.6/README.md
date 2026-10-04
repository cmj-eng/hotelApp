# Hotel Guest Manager — JavaFX/Maven Desktop App

## Requirements
Java 21 JDK from https://adoptium.net
  → Choose: Temurin 21 LTS → macOS → JDK → aarch64 (M-chip) or x64 (Intel)

## Run
- macOS: right-click run-macos.command → Open
- Windows: double-click run-windows.bat
- Or: java -jar HotelGuestApp.jar

## Build from source (on your Mac)
  cd HotelGuestApp-Java  (your project folder)
  mvn package
  java -jar target/HotelGuestApp-1.0.jar

## Create .app bundle with Dock icon
  chmod +x build-macos-app.sh && ./build-macos-app.sh

## Changes in this version
- Country field separated from Address in guest form
- Outstanding bills can now be paid: Finance tab shows individual
  bill items with "Mark as Paid" and "Pay ALL for Guest" buttons
- Hotel icon embedded (window title bar, Dock via build-macos-app.sh)
