package com.minikano.f50_sms.utils

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * FM_U20 (MN901/UMS9621) compatibility layer.
 *
 * The stock MN901 firmware has no com.zte.web package or Samba startup hook.
 * On a Magisk-rooted device we therefore start the existing UFI-TOOLS socat
 * backend directly. SELinux remains enforcing and the socket is only readable
 * by this application's UID.
 */
object U20PlatformAdapter {
    private const val TAG = "UFI_TOOLS_U20"
    private const val SOCKET_NAME = "kano_root_shell.sock"
    private const val PID_NAME = "kano_root_shell.pid"
    private const val LOG_NAME = "u20_root_backend.log"
    private const val MAGISK_SERVICE_PATH = "/data/adb/service.d/ufi-tools-u20.sh"
    private const val CLOUD_CONTROL_DIR = "/data/adb/ufi-tools-u20"
    private const val CLOUD_CONTROL_MODE = "$CLOUD_CONTROL_DIR/cloud-control-mode"
    private const val WEB_ENTRY_MODE = "$CLOUD_CONTROL_DIR/web-entry-mode"
    private const val WEB_ENTRY_CHAIN = "UFI_TOOLS_WEB"

    @Volatile
    private var bootHookInstalled = false

    private fun rootProcess(command: String): java.lang.Process {
        val suBinary = listOf(
            "/debug_ramdisk/su",
            "/sbin/su",
            "/system/xbin/su",
            "/system/bin/su"
        ).firstOrNull { File(it).canExecute() } ?: "su"
        return ProcessBuilder(suBinary, "-c", command)
            .redirectErrorStream(true)
            .start()
    }

    fun isNewU20(): Boolean {
        val identifiers = listOf(
            Build.MODEL,
            Build.DEVICE,
            Build.PRODUCT,
            Build.BOARD
        ).joinToString(" ").uppercase()

        return identifiers.contains("FM_U20") ||
            identifiers.contains("MN901") ||
            identifiers.contains("MN901UFI")
    }

    /**
     * The MN901 Flymodem management server is local to the device. Do not let
     * the F50 gateway auto-detection replace it with the USB/Wi-Fi client IP.
     */
    fun configureLocalGateway(context: Context) {
        if (!isNewU20()) return

        context.getSharedPreferences("kano_ZTE_store", Context.MODE_PRIVATE)
            .edit()
            .putString("gateway_ip", "127.0.0.1:8080")
            .putString("auto_ip_enabled", false.toString())
            .apply()
    }

    @Synchronized
    fun ensureRootBackend(context: Context): Result<String> {
        if (!isNewU20()) {
            return Result.failure(IllegalStateException("当前设备不是新版 FM_U20/MN901"))
        }

        return runCatching {
            ensureBootHook(context)

            val socket = File(context.filesDir, SOCKET_NAME)
            if (socket.exists()) {
                val probe = RootShell.sendCommandToSocket("id -u", socket.absolutePath, 2_000)
                if (probe?.trim() == "0") {
                    return@runCatching "root backend already running"
                }
            }

            val socat = KanoUtils.copyFileToFilesDir(context, "shell/socat", false)
                ?: error("复制 socat 失败")
            val sendAt = KanoUtils.copyFileToFilesDir(context, "shell/sendat", false)
                ?: error("复制 32 位 sendat 失败")

            socat.setExecutable(true, true)
            sendAt.setExecutable(true, true)

            val pidFile = File(context.filesDir, PID_NAME)
            val logFile = File(context.filesDir, LOG_NAME)
            val appUid = Process.myUid()
            val script = """
                SOCK='${socket.absolutePath}'
                PID_FILE='${pidFile.absolutePath}'
                LOG_FILE='${logFile.absolutePath}'
                SOCAT='${socat.absolutePath}'
                for PID in ${'$'}(pidof socat 2>/dev/null); do
                    CMDLINE=${'$'}(tr '\000' ' ' < "/proc/${'$'}PID/cmdline" 2>/dev/null)
                    case "${'$'}CMDLINE" in
                        *"${'$'}SOCK"*) kill "${'$'}PID" 2>/dev/null || true ;;
                    esac
                done
                rm -f "${'$'}SOCK" "${'$'}PID_FILE"
                umask 077
                nohup "${'$'}SOCAT" UNIX-LISTEN:"${'$'}SOCK",fork,reuseaddr,unlink-early EXEC:/system/bin/sh >"${'$'}LOG_FILE" 2>&1 </dev/null &
                NEW_PID=${'$'}!
                echo "${'$'}NEW_PID" > "${'$'}PID_FILE"
                i=0
                while [ ! -S "${'$'}SOCK" ] && [ "${'$'}i" -lt 50 ]; do
                    sleep 0.1
                    i=${'$'}((i + 1))
                done
                [ -S "${'$'}SOCK" ] || exit 20
                chown $appUid:$appUid "${'$'}SOCK" "${'$'}PID_FILE" "${'$'}LOG_FILE"
                chmod 0600 "${'$'}SOCK" "${'$'}PID_FILE" "${'$'}LOG_FILE"
            """.trimIndent()

            val process = rootProcess(script)
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                error("Magisk root 后端启动超时")
            }
            if (process.exitValue() != 0) {
                val backendLog = logFile.takeIf { it.exists() }?.readText().orEmpty()
                error("Magisk root 后端启动失败(${process.exitValue()}): $output $backendLog")
            }

            val probe = RootShell.sendCommandToSocket("id -u", socket.absolutePath, 3_000)
            if (probe?.trim() != "0") {
                error("root socket 校验失败: $probe")
            }

            Log.i(TAG, "FM_U20 Magisk root backend ready, SELinux=${KanoUtils.getSELinuxStatus()}")
            "FM_U20 Magisk root backend ready"
        }
    }

    private fun ensureBootHook(context: Context) {
        if (bootHookInstalled) return

        val source = KanoUtils.copyFileToFilesDir(context, "shell/u20_boot_service.sh", false)
            ?: error("复制 U20 Magisk 开机脚本失败")
        source.setReadable(true, true)

        val script = """
            SRC='${source.absolutePath}'
            DST='$MAGISK_SERVICE_PATH'
            mkdir -p /data/adb/service.d
            cp -f "${'$'}SRC" "${'$'}DST.tmp"
            chown 0:0 "${'$'}DST.tmp"
            chmod 0755 "${'$'}DST.tmp"
            mv -f "${'$'}DST.tmp" "${'$'}DST"
            setprop ctl.stop neuralnetworks_hal_service_armnn-gpu >/dev/null 2>&1 || true
            mkdir -p '$CLOUD_CONTROL_DIR'
            if [ ! -f '$CLOUD_CONTROL_MODE' ]; then
                echo disabled > '$CLOUD_CONTROL_MODE'
            fi
            if grep -q '^disabled${'$'}' '$CLOUD_CONTROL_MODE'; then
                stop fly_p2xclient 2>/dev/null || setprop ctl.stop fly_p2xclient
            fi
            if [ ! -f '$WEB_ENTRY_MODE' ]; then
                echo enabled > '$WEB_ENTRY_MODE'
            fi
        """.trimIndent()

        val process = rootProcess(script)
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("U20 Magisk 开机脚本安装超时")
        }
        if (process.exitValue() != 0) {
            error("U20 Magisk 开机脚本安装失败(${process.exitValue()}): $output")
        }
        bootHookInstalled = true
        Log.i(TAG, "Magisk boot hook installed: $MAGISK_SERVICE_PATH")
    }

    fun stopRootBackend(context: Context): Result<String> = runCatching {
        val socket = File(context.filesDir, SOCKET_NAME)
        val pidFile = File(context.filesDir, PID_NAME)
        val socat = File(context.filesDir, "socat")
        val script = """
            PID_FILE='${pidFile.absolutePath}'
            SOCK='${socket.absolutePath}'
            SOCAT='${socat.absolutePath}'
            for PID in ${'$'}(pidof socat 2>/dev/null); do
                CMDLINE=${'$'}(tr '\000' ' ' < "/proc/${'$'}PID/cmdline" 2>/dev/null)
                case "${'$'}CMDLINE" in
                    *"${'$'}SOCK"*) kill "${'$'}PID" 2>/dev/null || true ;;
                esac
            done
            rm -f "${'$'}SOCK" "${'$'}PID_FILE"
        """.trimIndent()

        val process = rootProcess(script)
        if (!process.waitFor(5, TimeUnit.SECONDS) || process.exitValue() != 0) {
            error("停止 FM_U20 root 后端失败")
        }
        "FM_U20 Magisk root backend stopped"
    }

    fun runRootCommand(context: Context, command: String, timeoutMs: Int = 5_000): Result<String> =
        runCatching {
            ensureRootBackend(context).getOrThrow()
            val socket = File(context.filesDir, SOCKET_NAME)
            RootShell.sendCommandToSocket(command, socket.absolutePath, timeoutMs)
                ?: error("FM_U20 root socket 无响应")
        }

    fun getCloudControlState(context: Context): Result<U20CloudControlState> = runCatching {
        val output = runRootCommand(
            context,
            "MODE=disabled; [ -f '$CLOUD_CONTROL_MODE' ] && MODE=${'$'}(cat '$CLOUD_CONTROL_MODE'); " +
                "echo mode=${'$'}MODE; echo service=${'$'}(getprop init.svc.fly_p2xclient)"
        ).getOrThrow()
        val values = output.lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1).trim()
            }
            .toMap()
        U20CloudControlState(
            released = values["mode"] != "official" && values["service"] != "running",
            serviceState = values["service"].orEmpty().ifBlank { "unknown" }
        )
    }

    fun setCloudControlReleased(context: Context, released: Boolean): Result<U20CloudControlState> = runCatching {
        val mode = if (released) "disabled" else "official"
        val serviceCommand = if (released) {
            "stop fly_p2xclient 2>/dev/null || setprop ctl.stop fly_p2xclient"
        } else {
            "start fly_p2xclient 2>/dev/null || setprop ctl.start fly_p2xclient"
        }
        runRootCommand(
            context,
            "mkdir -p '$CLOUD_CONTROL_DIR'; echo '$mode' > '$CLOUD_CONTROL_MODE'; " +
                "$serviceCommand; sleep 1; sync"
        ).getOrThrow()
        getCloudControlState(context).getOrThrow()
    }

    fun getWebEntryState(context: Context): Result<U20WebEntryState> = runCatching {
        val output = runRootCommand(
            context,
            "MODE=enabled; [ -f '$WEB_ENTRY_MODE' ] && MODE=${'$'}(cat '$WEB_ENTRY_MODE'); " +
                "ACTIVE=0; iptables -t nat -C PREROUTING -j '$WEB_ENTRY_CHAIN' >/dev/null 2>&1 && ACTIVE=1; " +
                "echo mode=${'$'}MODE; echo active=${'$'}ACTIVE"
        ).getOrThrow()
        val values = output.lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1).trim()
            }
            .toMap()
        U20WebEntryState(
            enabled = values["mode"] != "official",
            active = values["active"] == "1"
        )
    }

    fun setWebEntryEnabled(context: Context, enabled: Boolean): Result<U20WebEntryState> = runCatching {
        val mode = if (enabled) "enabled" else "official"
        val enableRules = if (enabled) {
            "iptables -t nat -N '$WEB_ENTRY_CHAIN'; " +
                "iptables -t nat -A '$WEB_ENTRY_CHAIN' -d 192.168.88.1/32 -p tcp --dport 80 -j REDIRECT --to-ports 2333; " +
                "iptables -t nat -A '$WEB_ENTRY_CHAIN' -d 192.168.99.1/32 -p tcp --dport 80 -j REDIRECT --to-ports 2333; " +
                "iptables -t nat -I PREROUTING 1 -j '$WEB_ENTRY_CHAIN'; "
        } else {
            ""
        }
        runRootCommand(
            context,
            "mkdir -p '$CLOUD_CONTROL_DIR'; echo '$mode' > '$WEB_ENTRY_MODE'; " +
                "while iptables -t nat -C PREROUTING -j '$WEB_ENTRY_CHAIN' >/dev/null 2>&1; do " +
                "iptables -t nat -D PREROUTING -j '$WEB_ENTRY_CHAIN' >/dev/null 2>&1 || break; done; " +
                "iptables -t nat -F '$WEB_ENTRY_CHAIN' >/dev/null 2>&1 || true; " +
                "iptables -t nat -X '$WEB_ENTRY_CHAIN' >/dev/null 2>&1 || true; " +
                enableRules +
                "sync"
        ).getOrThrow()
        getWebEntryState(context).getOrThrow()
    }

    fun schedulePowerAction(context: Context, action: String): Result<String> = runCatching {
        val powerCommand = when (action) {
            "reboot" -> "svc power reboot"
            "shutdown" -> "svc power shutdown"
            else -> error("不支持的电源操作")
        }
        runRootCommand(
            context,
            "nohup /system/bin/sh -c 'sleep 2; $powerCommand' >/dev/null 2>&1 & echo scheduled",
            3_000
        ).getOrThrow().also { output ->
            if (!output.contains("scheduled")) error("电源命令未成功排队: $output")
        }
    }

    fun getUsbMode(context: Context): Result<U20UsbModeState> = runCatching {
        val output = runRootCommand(
            context,
            "echo current=${'$'}(getprop sys.usb.config); " +
                "echo state=${'$'}(getprop sys.usb.state); " +
                "echo default=${'$'}(getprop persist.sys.usb.config)"
        ).getOrThrow()
        val values = output.lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1).trim()
            }
            .toMap()
        U20UsbModeState(
            current = values["current"].orEmpty().ifBlank { "charging,adb" },
            state = values["state"].orEmpty().ifBlank { "unknown" },
            defaultMode = values["default"].orEmpty().ifBlank { "charging,adb" }
        )
    }

    /**
     * Switch only the running USB gadget. The persistent vendor default is
     * deliberately left untouched, so a reboot always returns to charging+ADB.
     */
    fun scheduleUsbMode(context: Context, mode: String): Result<String> = runCatching {
        if (mode != "charging,adb" && mode != "rndis,adb") {
            error("不支持的 USB 模式")
        }
        // Let Android's UsbDeviceManager own the transition. Writing
        // sys.usb.config directly leaves tethering marked active and the
        // framework can immediately force RNDIS back on. ADB is appended by
        // the framework because persistent ADB is already enabled on U20.
        val usbCommand = if (mode == "rndis,adb") {
            "/system/bin/svc usb setFunctions rndis"
        } else {
            "/system/bin/svc usb setFunctions"
        }
        runRootCommand(
            context,
            "nohup /system/bin/sh -c 'sleep 2; $usbCommand' " +
                ">/dev/null 2>&1 & echo scheduled",
            3_000
        ).getOrThrow().also { output ->
            if (!output.contains("scheduled")) error("USB 切换命令未成功排队: $output")
        }
    }
}

data class U20CloudControlState(
    val released: Boolean,
    val serviceState: String
)

data class U20WebEntryState(
    val enabled: Boolean,
    val active: Boolean
)

data class U20UsbModeState(
    val current: String,
    val state: String,
    val defaultMode: String
)
