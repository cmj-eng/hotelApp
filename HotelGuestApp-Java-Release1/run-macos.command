#!/bin/bash
# Hotel Guest Manager — macOS launcher
# Double-click this file in Finder to run the app.
cd "$(dirname "$0")"

# Check for Java 11+
if ! command -v java &>/dev/null; then
  osascript -e 'display alert "Java not found" message "Please install Java 17 or newer from https://adoptium.net" as critical'
  exit 1
fi

java -jar HotelGuestApp.jar
