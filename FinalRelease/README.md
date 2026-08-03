# Hotel Guest Manager — Icon & Packaging Guide

## Quick summary

| Goal | File to use | Platform |
|------|-------------|----------|
| Proper Dock icon + double-click .app | `build-macos-app.sh` | macOS |
| Custom taskbar icon + .exe | `launch4j-config.xml` | Windows |

---

## macOS — Create HotelGuestApp.app

**What you need:**
- `HotelGuestApp.jar` (your existing JAR)
- `icon.png` — your logo, 512×512 pixels, square
- `build-macos-app.sh` (this folder)

**Steps:**
1. Put all three files in the same folder
2. Open **Terminal**, `cd` to that folder:
   ```
   cd ~/Downloads/HotelGuestApp-Extras
   ```
3. Make the script executable (first time only):
   ```
   chmod +x build-macos-app.sh
   ```
4. Run it:
   ```
   ./build-macos-app.sh
   ```
5. `HotelGuestApp.app` appears in the same folder
6. Double-click it in Finder — or drag it to `/Applications` or your Dock

The script converts `icon.png` to `.icns` automatically using macOS built-in tools (`sips` + `iconutil`). No extra software needed.

---

## Windows — Create HotelGuestApp.exe with Launch4j

**What you need:**
- `HotelGuestApp.jar`
- `icon.ico` — convert your PNG at https://convertico.com (free, upload PNG → download ICO)
- `launch4j-config.xml` (this folder)
- Launch4j — download free at http://launch4j.sourceforge.net

**Steps:**
1. Install Launch4j
2. Convert `icon.png` → `icon.ico` at convertico.com
3. Put `HotelGuestApp.jar`, `icon.ico`, and `launch4j-config.xml` in the same folder
4. Open Launch4j → **File → Open config** → select `launch4j-config.xml`
5. Click the **gear icon** (Build wrapper)
6. `HotelGuestApp.exe` is created with your icon

**Note:** keep `HotelGuestApp.jar` next to the `.exe` — the exe is a launcher, not a standalone file.

---

## Setting the window title bar icon (in-app)

Upload your `icon.png` directly in the Claude chat and I'll embed it in the JAR automatically. This sets the icon in the window title bar on both macOS and Windows.
