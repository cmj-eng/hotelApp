# Hotel Guest Manager — Java Desktop App

Cross-platform desktop application for managing hotel guests.
Same features as the iOS app, fully compatible JSON export/import between both.

## Requirements

- **Java 17 or newer** — free download from https://adoptium.net  
  (Choose: "Temurin 21 LTS" → your platform → JRE)

## Running the App

### macOS
1. Right-click `run-macos.command` → **Open**  
   (first time only, macOS may ask to confirm — click Open)
2. Or from Terminal: `java -jar HotelGuestApp.jar`

### Windows 11
1. Double-click `run-windows.bat`
2. Or from Command Prompt: `java -jar HotelGuestApp.jar`

## Features

| Tab | What it does |
|-----|-------------|
| **Guests** | Searchable, filterable table. Double-click to edit. Purple rows = special request, green = Very Good, orange = Unreliable, red = Blacklisted. |
| **New Guest** | Full form: name, address, passport, DOB, dates, room type + rate, car hire + rate, status flag, comments |
| **Finance** | Summary tiles (revenue, profit, open bills, this week). Profit per guest. Outstanding bills table. 4-week bar chart. |
| **History** | Chronological log of every change across all guests. Searchable. |
| **Data** | Export all data to JSON. Import JSON from desktop or iOS app. |

## Data Storage

Guest records are saved automatically to:
- **macOS**: `~/.hotelguestapp/guests.json`
- **Windows**: `C:\Users\<you>\.hotelguestapp\guests.json`

## iOS Compatibility

The export/import JSON format is identical between this desktop app and the iOS Swift app.
Export from iOS → import on desktop, or vice versa.

## Building from Source

```bash
# Requires: Java 21 JDK + Maven 3.8+
cd HotelGuestApp-Java
mvn package
java -jar target/HotelGuestApp-1.0.jar
```
