#!/bin/sh
set -eu

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
    echo "Usage: $0 /path/to/UFI-TOOLS.apk [/path/to/output.zip]" >&2
    exit 2
fi

APK=$1
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OUTPUT=${2:-"$SCRIPT_DIR/U20_System_Provider_Magisk.zip"}
STAGE=$(mktemp -d "${TMPDIR:-/tmp}/u20-system-provider.XXXXXX")

cleanup() {
    rm -rf "$STAGE"
}
trap cleanup EXIT INT TERM

mkdir -p "$STAGE/system/app/UfiTools"
cp "$SCRIPT_DIR/module.prop" "$STAGE/module.prop"
cp "$SCRIPT_DIR/README.md" "$STAGE/README.md"
cp "$APK" "$STAGE/system/app/UfiTools/UfiTools.apk"

(cd "$STAGE" && zip -qr "$OUTPUT" module.prop README.md system)
echo "$OUTPUT"
