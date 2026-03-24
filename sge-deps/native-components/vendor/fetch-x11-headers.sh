#!/bin/bash
# Download X11 development headers for cross-compiling GLFW to Linux.
# Headers are arch-independent — only needed for compilation, not linking
# (GLFW loads libX11.so at runtime via dlopen).
#
# Uses xorgproto (protocol headers) + individual lib -dev packages.
# Downloads from Ubuntu mirrors via packages.ubuntu.com redirects.
#
# Usage: ./fetch-x11-headers.sh [target-dir]
# Default target: vendor/x11-include

set -euo pipefail

TARGET_DIR="${1:-$(dirname "$0")/x11-include}"
# Resolve to absolute path before cd-ing into temp dirs
mkdir -p "$TARGET_DIR"
TARGET_DIR="$(cd "$TARGET_DIR" && pwd)"
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT

if [ -f "$TARGET_DIR/X11/Xlib.h" ]; then
  echo "X11 headers already present in $TARGET_DIR"
  exit 0
fi

echo "Downloading X11 headers to $TARGET_DIR..."
mkdir -p "$TARGET_DIR"

# Direct URLs to Ubuntu deb packages containing X11 headers.
# Headers are arch-independent (only type definitions, no code).
# Using archive.ubuntu.com which is stable and reliable.
MIRROR="http://archive.ubuntu.com/ubuntu/pool/main"
PACKAGES=(
  "$MIRROR/x/xorgproto/x11proto-dev_2025.1-1_all.deb"
  "$MIRROR/libx/libx11/libx11-dev_1.8.7-1build1_amd64.deb"
  "$MIRROR/libx/libxrandr/libxrandr-dev_1.5.4-1build1_amd64.deb"
  "$MIRROR/libx/libxinerama/libxinerama-dev_1.1.4-3build2_amd64.deb"
  "$MIRROR/libx/libxcursor/libxcursor-dev_1.2.3-1build1_amd64.deb"
  "$MIRROR/libx/libxi/libxi-dev_1.8.2-2_amd64.deb"
  "$MIRROR/libx/libxext/libxext-dev_1.3.4-1build3_amd64.deb"
  "$MIRROR/libx/libxfixes/libxfixes-dev_6.0.0-2build2_amd64.deb"
  "$MIRROR/libx/libxrender/libxrender-dev_0.9.12-1build1_amd64.deb"
)

extract_deb() {
  local deb="$1"
  local work="$TEMP_DIR/extract"
  rm -rf "$work" && mkdir -p "$work"
  # ar x writes to cwd
  (
    cd "$work"
    ar x "$deb"
    # Decompress data archive
    if [ -f data.tar.zst ]; then
      zstd -d -f data.tar.zst -o data.tar
    elif [ -f data.tar.xz ]; then
      xz -d -f data.tar.xz
    elif [ -f data.tar.gz ]; then
      gzip -d -f data.tar.gz
    fi
    if [ -f data.tar ]; then
      tar xf data.tar --no-same-owner --no-same-permissions 2>/dev/null || tar xf data.tar 2>/dev/null || true
      if [ -d usr/include ]; then
        cp -r usr/include/* "$TARGET_DIR/"
      fi
    else
      echo "  WARNING: no data.tar found in $deb"
    fi
  )
}

for url in "${PACKAGES[@]}"; do
  pkg=$(basename "$url")
  echo "  Fetching $pkg..."
  curl -fsSL -o "$TEMP_DIR/$pkg" "$url" || {
    echo "  WARNING: failed to download $pkg"
    continue
  }
  extract_deb "$TEMP_DIR/$pkg"
done

if [ -f "$TARGET_DIR/X11/Xlib.h" ]; then
  count=$(find "$TARGET_DIR" -name '*.h' | wc -l)
  echo "X11 headers installed to $TARGET_DIR ($count headers)"
else
  echo "ERROR: X11/Xlib.h not found after download"
  exit 1
fi
