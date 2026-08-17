#!/bin/bash
# ============================================================
#  build-macos-app.sh
#  Creates HotelGuestApp.app — a proper macOS application bundle
#  that shows your icon in the Dock and Finder.
#
#  Prerequisites (all free, built into macOS):
#    • Java 17+ installed
#    • Your icon as icon.png (512×512) in the same folder as this script
#
#  Usage:
#    1. Put this script, HotelGuestApp.jar and icon.png in the same folder
#    2. Open Terminal, cd to that folder
#    3. chmod +x build-macos-app.sh
#    4. ./build-macos-app.sh
#    5. Double-click HotelGuestApp.app in Finder
# ============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="HotelGuestApp"
APP="$SCRIPT_DIR/$APP_NAME.app"

echo "→ Building $APP_NAME.app ..."

# ── 1. Check requirements ────────────────────────────────────
if [ ! -f "$SCRIPT_DIR/HotelGuestApp.jar" ]; then
  echo "✗  HotelGuestApp.jar not found in $SCRIPT_DIR"
  exit 1
fi
if ! command -v java &>/dev/null; then
  echo "✗  Java not found. Install from https://adoptium.net"
  exit 1
fi

# ── 2. Create .app folder structure ─────────────────────────
rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS"
mkdir -p "$APP/Contents/Resources"
mkdir -p "$APP/Contents/Java"

# ── 3. Copy the JAR ─────────────────────────────────────────
cp "$SCRIPT_DIR/HotelGuestApp.jar" "$APP/Contents/Java/"

# ── 4. Convert icon.png → .icns (if icon.png exists) ────────
if [ -f "$SCRIPT_DIR/icon.png" ]; then
  echo "→ Converting icon.png to .icns ..."
  ICONSET="$SCRIPT_DIR/HotelGuestApp.iconset"
  mkdir -p "$ICONSET"

  # Generate all required sizes
  sips -z 16   16   "$SCRIPT_DIR/icon.png" --out "$ICONSET/icon_16x16.png"      &>/dev/null
  sips -z 32   32   "$SCRIPT_DIR/icon.png" --out "$ICONSET/icon_16x16@2x.png"   &>/dev/null
  sips -z 32   32   "$SCRIPT_DIR/icon.png" --out "$ICONSET/icon_32x32.png"      &>/dev/null
  sips -z 64   64   "$SCRIPT_DIR/icon.png" --out "$ICONSET/icon_32x32@2x.png"   &>/dev/null
  sips -z 128  128  "$SCRIPT_DIR/icon.png" --out "$ICONSET/icon_128x128.png"    &>/dev/null
  sips -z 256  256  "$SCRIPT_DIR/icon.png" --out "$ICONSET/icon_128x128@2x.png" &>/dev/null
  sips -z 256  256  "$SCRIPT_DIR/icon.png" --out "$ICONSET/icon_256x256.png"    &>/dev/null
  sips -z 512  512  "$SCRIPT_DIR/icon.png" --out "$ICONSET/icon_256x256@2x.png" &>/dev/null
  sips -z 512  512  "$SCRIPT_DIR/icon.png" --out "$ICONSET/icon_512x512.png"    &>/dev/null
  sips -z 1024 1024 "$SCRIPT_DIR/icon.png" --out "$ICONSET/icon_512x512@2x.png" &>/dev/null

  iconutil -c icns "$ICONSET" -o "$APP/Contents/Resources/HotelGuestApp.icns"
  rm -rf "$ICONSET"
  ICON_LINE="<key>CFBundleIconFile</key><string>HotelGuestApp</string>"
  echo "   ✓ Icon converted"
else
  echo "   ℹ  icon.png not found — app will use default Java icon."
  echo "      Place icon.png (512×512) next to this script and re-run to add your icon."
  ICON_LINE=""
fi

# ── 5. Find Java executable ──────────────────────────────────
JAVA_BIN="$(which java)"
JAVA_HOME_REAL="$(dirname "$(dirname "$(readlink -f "$JAVA_BIN" 2>/dev/null || echo "$JAVA_BIN")")")"

# ── 6. Write the launcher shell script ──────────────────────
cat > "$APP/Contents/MacOS/$APP_NAME" << LAUNCHER
#!/bin/bash
# Launcher — finds Java and starts the app
SCRIPT="\$(cd "\$(dirname "\$0")" && pwd)"
JAR="\$SCRIPT/../Java/HotelGuestApp.jar"

# Try bundled Java first, then system Java
for JAVA_TRY in \\
  "\$SCRIPT/../Resources/jre/bin/java" \\
  "$JAVA_BIN" \\
  "/usr/bin/java" \\
  "/usr/local/bin/java"; do
  if [ -x "\$JAVA_TRY" ]; then
    exec "\$JAVA_TRY" -jar "\$JAR"
    break
  fi
done

osascript -e 'display alert "Java not found" message "Install Java 17+ from https://adoptium.net" as critical'
exit 1
LAUNCHER
chmod +x "$APP/Contents/MacOS/$APP_NAME"

# ── 7. Write Info.plist ──────────────────────────────────────
cat > "$APP/Contents/Info.plist" << PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleName</key>
  <string>Hotel Guest Manager</string>
  <key>CFBundleDisplayName</key>
  <string>Hotel Guest Manager</string>
  <key>CFBundleIdentifier</key>
  <string>com.hotelguest.app</string>
  <key>CFBundleVersion</key>
  <string>1.0</string>
  <key>CFBundleShortVersionString</key>
  <string>1.0</string>
  <key>CFBundleExecutable</key>
  <string>$APP_NAME</string>
  <key>CFBundlePackageType</key>
  <string>APPL</string>
  <key>CFBundleSignature</key>
  <string>????</string>
  <key>NSHighResolutionCapable</key>
  <true/>
  <key>NSRequiresAquaSystemAppearance</key>
  <false/>
  $ICON_LINE
</dict>
</plist>
PLIST

# ── 8. Remove macOS quarantine flag ─────────────────────────
xattr -cr "$APP" 2>/dev/null || true

echo ""
echo "✓  Done!  HotelGuestApp.app created in:"
echo "   $SCRIPT_DIR"
echo ""
echo "   Double-click it in Finder to launch."
echo "   Tip: drag it to your Applications folder or the Dock."
