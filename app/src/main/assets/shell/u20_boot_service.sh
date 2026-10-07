#!/system/bin/sh

# FM_U20/MN901 firmware blocks third-party BOOT_COMPLETED receivers.
# Magisk's --service process is an init exec service: init kills its original
# process group when it exits. Start a new session so the delayed worker is not
# killed together with Magisk's launcher.
LOG_FILE=/data/local/tmp/ufi-tools-u20-boot.log
SCRIPT_PATH=/data/adb/service.d/ufi-tools-u20.sh
BUSYBOX=/data/adb/magisk/busybox

if [ "${U20_DETACHED:-0}" != "1" ]; then
    CLOUD_CONTROL_MODE=/data/adb/ufi-tools-u20/cloud-control-mode
    if grep -q '^disabled$' "$CLOUD_CONTROL_MODE" 2>/dev/null; then
        stop fly_p2xclient >/dev/null 2>&1 || setprop ctl.stop fly_p2xclient
    fi

    echo "[$(date)] launching detached boot worker" >> "$LOG_FILE"
    if [ -x "$BUSYBOX" ]; then
        U20_DETACHED=1 "$BUSYBOX" setsid /system/bin/sh "$SCRIPT_PATH" \
            </dev/null >>"$LOG_FILE" 2>&1 &
    else
        U20_DETACHED=1 nohup /system/bin/sh "$SCRIPT_PATH" \
            </dev/null >>"$LOG_FILE" 2>&1 &
    fi
    exit 0
fi

PKG=com.minikano.f50_sms
echo "[$(date)] detached boot worker started" >> "$LOG_FILE"

start_root_backend() {
    APP_FILES=/data/user/0/$PKG/files
    [ -d "$APP_FILES" ] || APP_FILES=/data/data/$PKG/files
    SOCAT="$APP_FILES/socat"
    SOCK="$APP_FILES/kano_root_shell.sock"
    PID_FILE="$APP_FILES/kano_root_shell.pid"
    ROOT_LOG="$APP_FILES/u20_root_backend.log"
    [ -x "$SOCAT" ] || return 1

    # The daemon must be born from Magisk's service.d context.  A daemon
    # recreated later through an app-initiated `su` can read files and run tc,
    # but this firmware denies its netfilter socket after a cold boot.
    for old_pid in $(pidof socat 2>/dev/null); do
        old_cmdline=$(tr '\000' ' ' < "/proc/$old_pid/cmdline" 2>/dev/null)
        case "$old_cmdline" in
            *"$SOCK"*) kill "$old_pid" 2>/dev/null || true ;;
        esac
    done
    rm -f "$SOCK" "$PID_FILE"
    umask 077
    nohup "$SOCAT" UNIX-LISTEN:"$SOCK",fork,reuseaddr,unlink-early \
        EXEC:/system/bin/sh >"$ROOT_LOG" 2>&1 </dev/null &
    root_pid=$!
    echo "$root_pid" > "$PID_FILE"

    wait_socket=0
    while [ ! -S "$SOCK" ] && [ "$wait_socket" -lt 50 ]; do
        sleep 0.1
        wait_socket=$((wait_socket + 1))
    done
    [ -S "$SOCK" ] || return 1

    app_uid=$(stat -c %u "$APP_FILES" 2>/dev/null)
    [ -n "$app_uid" ] || return 1
    chown "$app_uid:$app_uid" "$SOCK" "$PID_FILE" "$ROOT_LOG"
    chmod 0600 "$SOCK" "$PID_FILE" "$ROOT_LOG"
    return 0
}

i=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ "$i" -lt 180 ]; do
    sleep 2
    i=$((i + 1))
done

sleep 5

# The stock image starts an ArmNN GPU HAL which is absent from its own VINTF
# manifest. Registration can never succeed, so init restarts it every five
# seconds. The hotspot exposes no NNAPI workload; stop only this unusable HAL.
setprop ctl.stop neuralnetworks_hal_service_armnn-gpu >/dev/null 2>&1 || true
echo "[$(date)] stopped unusable ArmNN GPU HAL restart loop" >> "$LOG_FILE"

    # 新版 U20 的 fly_p2xclient 会连接厂家 MQTT 云控。默认停止；
    # 用户在 UFI-TOOLS 中关闭“解除云控”后，mode 会切回 official。
CLOUD_CONTROL_DIR=/data/adb/ufi-tools-u20
CLOUD_CONTROL_MODE="$CLOUD_CONTROL_DIR/cloud-control-mode"
mkdir -p "$CLOUD_CONTROL_DIR"
if [ ! -f "$CLOUD_CONTROL_MODE" ]; then
    echo disabled > "$CLOUD_CONTROL_MODE"
fi
if grep -q '^disabled$' "$CLOUD_CONTROL_MODE"; then
    stop fly_p2xclient >/dev/null 2>&1 || setprop ctl.stop fly_p2xclient
    echo "[$(date)] fly_p2xclient cloud control disabled" >> "$LOG_FILE"
else
    start fly_p2xclient >/dev/null 2>&1 || setprop ctl.start fly_p2xclient
    echo "[$(date)] fly_p2xclient official cloud control restored" >> "$LOG_FILE"
fi

if pm path "$PKG" >/dev/null 2>&1; then
    for permission in \
        android.permission.READ_SMS \
        android.permission.RECEIVE_SMS \
        android.permission.SEND_SMS \
        android.permission.READ_PHONE_STATE
    do
        pm grant "$PKG" "$permission" >/dev/null 2>&1 || true
    done
    echo "[$(date)] starting UFI-TOOLS services" >> "$LOG_FILE"
    am start-foreground-service -n "$PKG/.WebService" >> "$LOG_FILE" 2>&1
    am start-foreground-service -n "$PKG/.ADBService" >> "$LOG_FILE" 2>&1

    # On a fresh install WebService first extracts socat into its private
    # directory.  Wait for that file, then replace any app-created daemon with
    # the service.d-owned daemon carrying the boot network privileges.
    root_wait=0
    while [ ! -x "/data/user/0/$PKG/files/socat" ] && \
          [ ! -x "/data/data/$PKG/files/socat" ] && \
          [ "$root_wait" -lt 20 ]; do
        sleep 1
        root_wait=$((root_wait + 1))
    done
    if start_root_backend; then
        echo "[$(date)] privileged UFI-TOOLS root backend ready" >> "$LOG_FILE"
    else
        echo "[$(date)] privileged UFI-TOOLS root backend unavailable" >> "$LOG_FILE"
    fi

    # Default device management entry (port 80) opens UFI-TOOLS (2333).
    # Only packets addressed to the U20 itself are redirected.
    WEB_ENTRY_MODE="$CLOUD_CONTROL_DIR/web-entry-mode"
    WEB_ENTRY_CHAIN=UFI_TOOLS_WEB
    if [ ! -f "$WEB_ENTRY_MODE" ]; then
        echo enabled > "$WEB_ENTRY_MODE"
    fi
    while iptables -t nat -C PREROUTING -j "$WEB_ENTRY_CHAIN" >/dev/null 2>&1; do
        iptables -t nat -D PREROUTING -j "$WEB_ENTRY_CHAIN" >/dev/null 2>&1 || break
    done
    iptables -t nat -N "$WEB_ENTRY_CHAIN" >/dev/null 2>&1 || true
    iptables -t nat -F "$WEB_ENTRY_CHAIN" >/dev/null 2>&1 || true
    if grep -q '^enabled$' "$WEB_ENTRY_MODE"; then
        iptables -t nat -A "$WEB_ENTRY_CHAIN" -d 192.168.88.1/32 -p tcp --dport 80 -j REDIRECT --to-ports 2333
        iptables -t nat -A "$WEB_ENTRY_CHAIN" -d 192.168.99.1/32 -p tcp --dport 80 -j REDIRECT --to-ports 2333
        iptables -t nat -I PREROUTING 1 -j "$WEB_ENTRY_CHAIN"
        echo "[$(date)] default web entry redirected to UFI-TOOLS :2333" >> "$LOG_FILE"
    else
        iptables -t nat -X "$WEB_ENTRY_CHAIN" >/dev/null 2>&1 || true
        echo "[$(date)] official web entry restored" >> "$LOG_FILE"
    fi
else
    echo "[$(date)] UFI-TOOLS package not installed" >> "$LOG_FILE"
fi

echo "[$(date)] detached boot worker finished" >> "$LOG_FILE"
