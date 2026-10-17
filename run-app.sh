#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
sdk_adb="$sdk_dir/platform-tools/adb"

if [[ -x "$sdk_adb" ]]; then
    adb_cmd="$sdk_adb"
elif command -v adb >/dev/null 2>&1; then
    adb_cmd="$(command -v adb)"
else
    echo "ADB não encontrado. Instale Android SDK Platform-Tools ou configure ANDROID_HOME." >&2
    exit 1
fi


mapfile -t connected_devices < <("$adb_cmd" devices | awk '$2 == "device" { print $1 }')

if [[ ${#connected_devices[@]} -ne 1 ]]; then
    echo "Conecte e autorize exatamente um celular por USB ou Wi-Fi." >&2
    "$adb_cmd" devices >&2
    exit 1
fi

device_serial="${connected_devices[0]}"

./gradlew :app:assembleDevDebug
"$adb_cmd" -s "$device_serial" install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
"$adb_cmd" -s "$device_serial" shell am start -n com.example.gymtimer.dev/com.example.gymtimer2.MainActivity
