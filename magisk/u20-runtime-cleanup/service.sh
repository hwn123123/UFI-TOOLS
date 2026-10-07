#!/system/bin/sh

MODDIR=${0%/*}
LOG_FILE="$MODDIR/runtime-cleanup.log"

log_line() {
    echo "$(date '+%F %T') $*" >> "$LOG_FILE"
}

wait_count=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ "$wait_count" -lt 120 ]; do
    sleep 1
    wait_count=$((wait_count + 1))
done

resetprop ro.hardware.audio.primary default
/system/bin/stop vendor.audio-hal 2>/dev/null
sleep 1
/system/bin/start vendor.audio-hal 2>/dev/null

for service_name in \
    vendor.cplog_svc-default \
    cplogserver \
    slogmodem \
    vendor.slogmodem \
    neuralnetworks_hal_service_armnn-gpu
do
    /system/bin/stop "$service_name" 2>/dev/null
done

sleep 8
for service_name in \
    vendor.cplog_svc-default \
    cplogserver \
    slogmodem \
    vendor.slogmodem \
    neuralnetworks_hal_service_armnn-gpu
do
    /system/bin/stop "$service_name" 2>/dev/null
done

log_line "audio=$(getprop ro.hardware.audio.primary) cplog=$(getprop init.svc.cplogserver) slog=$(getprop init.svc.slogmodem) armnn=$(getprop init.svc.neuralnetworks_hal_service_armnn-gpu)"
