#!/bin/bash
cd "$(dirname "$0")"
if ! command -v java &>/dev/null; then
  osascript -e 'display alert "Java not found" message "Install Java 17+ from https://adoptium.net" as critical'
  exit 1
fi
java -jar HotelGuestApp.jar
