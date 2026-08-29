#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
WATCH_AVD="${HYDRA_WATCH_AVD:-Hydra_Wear_API36}"
PHONE_AVD="${HYDRA_PHONE_AVD:-Hydra_Phone_API36}"
WATCH_SYSTEM_IMAGE="${HYDRA_WATCH_SYSTEM_IMAGE:-}"
PHONE_SYSTEM_IMAGE="${HYDRA_PHONE_SYSTEM_IMAGE:-}"
ANDROID_PLATFORM="platforms;android-36"
ANDROID_BUILD_TOOLS="build-tools;36.0.0"
SETUP_ONLY=false
SKIP_BUILD=false

usage() {
    cat <<'EOF'
Usage: scripts/setup_emulator.sh [options]

Create or reuse Android emulators, install the debug applications, and launch
them. The default AVD names are Hydra_Wear_API36 and Hydra_Phone_API36.

Options:
  --setup-only  Create the AVDs without starting them or building the app
  --no-build    Reuse existing APKs instead of running the Gradle build
  -h, --help    Show this help

Environment:
  ANDROID_HOME / ANDROID_SDK_ROOT
  HYDRA_WATCH_AVD, HYDRA_PHONE_AVD
  HYDRA_WATCH_SYSTEM_IMAGE, HYDRA_PHONE_SYSTEM_IMAGE
EOF
}

while (($# > 0)); do
    case "$1" in
        --setup-only)
            SETUP_ONLY=true
            ;;
        --no-build)
            SKIP_BUILD=true
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            printf '不明なオプションです: %s\n\n' "$1" >&2
            usage >&2
            exit 2
            ;;
    esac
    shift
done

if [[ -z "$SDK_DIR" ]]; then
    case "$(uname -s)" in
        Darwin)
            [[ -d "$HOME/Library/Android/sdk" ]] && SDK_DIR="$HOME/Library/Android/sdk"
            ;;
        Linux)
            [[ -d "$HOME/Android/Sdk" ]] && SDK_DIR="$HOME/Android/Sdk"
            ;;
    esac
fi

if [[ -z "$SDK_DIR" || ! -d "$SDK_DIR" ]]; then
    printf 'Android SDK が見つかりません。ANDROID_HOME または ANDROID_SDK_ROOT を設定してください。\n' >&2
    exit 1
fi

# Android command-line tools require Java 17 or later. Prefer the Homebrew
# JDK when JAVA_HOME is not set, since macOS may otherwise select Java 8.
if [[ -z "${JAVA_HOME:-}" ]]; then
    for candidate in /opt/homebrew/opt/openjdk /usr/local/opt/openjdk; do
        if [[ -x "$candidate/bin/java" ]]; then
            export JAVA_HOME="$candidate"
            break
        fi
    done
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    JAVA_CMD="$(command -v java || true)"
fi

if [[ -z "$JAVA_CMD" || ! -x "$JAVA_CMD" ]]; then
    printf 'Java 17 以上が必要です。JAVA_HOME を設定するか PATH に追加してください。\n' >&2
    exit 1
fi

java_version="$("$JAVA_CMD" -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')"
java_major="${java_version%%.*}"
if [[ "$java_major" == "1" ]]; then
    java_major="${java_version#*.}"
    java_major="${java_major%%.*}"
fi
if [[ -z "$java_major" || "$java_major" -lt 17 ]]; then
    printf 'Java 17 以上が必要です（検出: %s）。\n' "${java_version:-unknown}" >&2
    exit 1
fi

SDKMANAGER="${SDK_DIR}/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="${SDK_DIR}/cmdline-tools/latest/bin/avdmanager"
EMULATOR="${SDK_DIR}/emulator/emulator"
ADB="${SDK_DIR}/platform-tools/adb"

for tool in "$SDKMANAGER" "$AVDMANAGER" "$EMULATOR" "$ADB"; do
    if [[ ! -x "$tool" ]]; then
        printf 'Android SDK のツールが見つかりません: %s\n' "$tool" >&2
        exit 1
    fi
done

ensure_sdk_package() {
    local package="$1"
    local directory="$2"

    if [[ -d "$directory" ]]; then
        return
    fi

    printf 'SDK パッケージをインストールします: %s\n' "$package"
    "$SDKMANAGER" "$package"
}

ensure_sdk_package "$ANDROID_PLATFORM" "$SDK_DIR/platforms/android-36"
ensure_sdk_package "$ANDROID_BUILD_TOOLS" "$SDK_DIR/build-tools/36.0.0"

if [[ -z "$WATCH_SYSTEM_IMAGE" || -z "$PHONE_SYSTEM_IMAGE" ]]; then
    case "$(uname -m)" in
        arm64|aarch64)
            default_arch="arm64-v8a"
            ;;
        *)
            default_arch="x86_64"
            ;;
    esac
    WATCH_SYSTEM_IMAGE="${WATCH_SYSTEM_IMAGE:-system-images;android-36;android-wear-signed;${default_arch}}"
    PHONE_SYSTEM_IMAGE="${PHONE_SYSTEM_IMAGE:-system-images;android-36;google_apis;${default_arch}}"
fi

avd_exists() {
    "$AVDMANAGER" list avd 2>/dev/null |
        awk -v expected="$1" '$1 == "Name:" && $2 == expected { found = 1 } END { exit !found }'
}

system_image_dir() {
    printf '%s/system-images/%s\n' "$SDK_DIR" "${1#system-images;}" | tr ';' '/'
}

ensure_system_image() {
    local package="$1"
    local directory
    directory="$(system_image_dir "$package")"
    if [[ -d "$directory" ]]; then
        return
    fi

    printf 'システムイメージをインストールします: %s\n' "$package"
    "$SDKMANAGER" "$package"
}

ensure_avd() {
    local name="$1"
    local package="$2"
    local device="$3"

    if avd_exists "$name"; then
        printf 'AVD を再利用します: %s\n' "$name"
        return
    fi

    printf 'AVD を作成します: %s\n' "$name"
    printf 'no\n' | "$AVDMANAGER" create avd \
        --name "$name" \
        --package "$package" \
        --device "$device"
}

ensure_system_image "$WATCH_SYSTEM_IMAGE"
ensure_system_image "$PHONE_SYSTEM_IMAGE"
ensure_avd "$WATCH_AVD" "$WATCH_SYSTEM_IMAGE" "wearos_large_round"
ensure_avd "$PHONE_AVD" "$PHONE_SYSTEM_IMAGE" "pixel_9"

if [[ "$SETUP_ONLY" == "true" ]]; then
    printf 'エミュレータの準備が完了しました。\n'
    printf 'ウォッチ: %s\nスマホ: %s\n' "$WATCH_AVD" "$PHONE_AVD"
    exit 0
fi

if [[ "$SKIP_BUILD" == "false" ]]; then
    "$ROOT_DIR/gradlew" :app:assembleDebug :companion:assembleDebug --configuration-cache
fi

if ! command -v lsof >/dev/null 2>&1 && ! command -v nc >/dev/null 2>&1; then
    printf 'ポート確認に lsof または nc が必要です。\n' >&2
    exit 1
fi

"$ADB" start-server >/dev/null
USED_PORTS=("")
STARTED_SERIAL=""
NEXT_PORT=""
STARTED_PID=""
STARTED_PIDS=()

cleanup_on_failure() {
    local status="$?"
    local pid

    if [[ "$status" -eq 0 ]]; then
        return
    fi

    for pid in "${STARTED_PIDS[@]}"; do
        [[ -n "$pid" ]] && kill "$pid" 2>/dev/null || true
    done
}

trap cleanup_on_failure EXIT

running_serial() {
    local avd="$1"
    local serial
    local name

    while read -r serial; do
        [[ -z "$serial" ]] && continue
        name="$("$ADB" -s "$serial" emu avd name 2>/dev/null | awk 'NR == 1 { print; exit }' || true)"
        if [[ "$name" == "$avd" ]]; then
            printf '%s\n' "$serial"
            return 0
        fi
    done < <("$ADB" devices | awk '$1 ~ /^emulator-/ { print $1 }')

    return 1
}

port_available() {
    local port="$1"

    if command -v lsof >/dev/null 2>&1; then
        if lsof -nP -iTCP:"$port" -sTCP:LISTEN -t >/dev/null 2>&1; then
            return 1
        fi
        return 0
    fi

    if nc -z 127.0.0.1 "$port" >/dev/null 2>&1; then
        return 1
    fi
    return 0
}

next_emulator_port() {
    local port=5554
    local reserved_port
    local is_reserved

    while true; do
        is_reserved=false
        for reserved_port in "${USED_PORTS[@]}"; do
            [[ -z "$reserved_port" ]] && continue
            if [[ "$reserved_port" == "$port" ]]; then
                is_reserved=true
                break
            fi
        done
        if [[ "$is_reserved" == "false" ]] &&
            ! "$ADB" devices | awk -v serial="emulator-${port}" '$1 == serial { found = 1 } END { exit found ? 0 : 1 }' &&
            port_available "$port" &&
            port_available "$((port + 1))"; then
            USED_PORTS+=("$port")
            NEXT_PORT="$port"
            return
        fi
        port=$((port + 2))
    done
}

start_emulator() {
    local avd="$1"
    local log_file="${TMPDIR:-/tmp}/hydra-${avd}.log"
    local serial
    local port

    if serial="$(running_serial "$avd")"; then
        printf '起動済みの AVD を再利用します: %s (%s)\n' "$avd" "$serial" >&2
        STARTED_PID=""
        STARTED_SERIAL="$serial"
        return
    fi

    next_emulator_port
    port="$NEXT_PORT"
    printf 'エミュレータを起動します: %s (ログ: %s)\n' "$avd" "$log_file" >&2
    nohup "$EMULATOR" -avd "$avd" -port "$port" -no-boot-anim \
        >"$log_file" 2>&1 </dev/null &
    STARTED_PID="$!"
    STARTED_PIDS+=("$STARTED_PID")
    STARTED_SERIAL="emulator-$port"
}

wait_for_boot() {
    local serial="$1"
    local pid="$2"
    local i
    local state
    local completed

    for ((i = 0; i < 180; i++)); do
        if [[ -n "$pid" ]] && ! kill -0 "$pid" 2>/dev/null; then
            printf 'エミュレータが起動に失敗しました: %s（ログ: %s）\n' \
                "$serial" "${TMPDIR:-/tmp}/hydra-${serial#emulator-}.log" >&2
            exit 1
        fi
        state="$("$ADB" -s "$serial" get-state 2>/dev/null || true)"
        completed="$("$ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
        if [[ "$state" == "device" && "$completed" == "1" ]]; then
            return
        fi
        sleep 1
    done

    printf 'エミュレータの起動を待機できませんでした: %s\n' "$serial" >&2
    exit 1
}

start_emulator "$WATCH_AVD"
watch_serial="$STARTED_SERIAL"
watch_pid="$STARTED_PID"
start_emulator "$PHONE_AVD"
phone_serial="$STARTED_SERIAL"
phone_pid="$STARTED_PID"
wait_for_boot "$watch_serial" "$watch_pid"
wait_for_boot "$phone_serial" "$phone_pid"

watch_apk="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
phone_apk="$ROOT_DIR/companion/build/outputs/apk/debug/companion-debug.apk"
for apk in "$watch_apk" "$phone_apk"; do
    if [[ ! -f "$apk" ]]; then
        printf 'APK が見つかりません: %s（--no-build を外して実行してください）\n' "$apk" >&2
        exit 1
    fi
done

"$ADB" -s "$watch_serial" install -r -d "$watch_apk" >/dev/null
"$ADB" -s "$phone_serial" install -r -d "$phone_apk" >/dev/null
"$ADB" -s "$watch_serial" shell am start \
    -n dev.marufeuille.hydra.debug/dev.marufeuille.hydra.MainActivity >/dev/null
"$ADB" -s "$phone_serial" shell am start \
    -n dev.marufeuille.hydra.debug/dev.marufeuille.hydra.companion.MainActivity >/dev/null

cat <<EOF
エミュレータで Hydra を起動しました。
ウォッチ: $watch_serial ($WATCH_AVD)
スマホ:   $phone_serial ($PHONE_AVD)

スマホ側で Hydra を開き、Health Connect の水分の読み取り・書き込みを許可してください。
ウォッチとスマホの Data Layer 連携を確認する場合は、Android Studio の Wear OS
Pairing Assistant でこの 2 台をペアリングしてください。
終了する場合は次を実行してください:
  $ADB -s $watch_serial emu kill
  $ADB -s $phone_serial emu kill
EOF
