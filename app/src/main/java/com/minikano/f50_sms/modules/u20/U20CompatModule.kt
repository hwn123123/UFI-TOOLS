package com.minikano.f50_sms.modules.u20

import android.content.Context
import android.graphics.Bitmap
import android.telephony.TelephonyManager
import android.util.Base64
import com.minikano.f50_sms.utils.KanoUtils
import com.minikano.f50_sms.utils.U20PlatformAdapter
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Adapter for the Flymodem API shipped by the FM_U20/MN901 firmware.
 *
 * UFI-TOOLS was built around ZTE's goform API. The new U20 instead exposes a
 * JSON RPC-like endpoint at /MF08_Pages. This module keeps the existing web UI
 * usable while native U20 screens are added incrementally.
 */
fun Route.u20CompatModule(context: Context) {
    route("/api/goform") {
        get("/goform_get_cmd_process") {
            val commands = call.request.queryParameters["cmd"]
                .orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val pageSize = call.request.queryParameters["data_per_page"]?.toIntOrNull() ?: 500
            val result = U20GoformBridge.get(context, commands, page, pageSize)
            call.respondText(result.toString(), ContentType.Application.Json)
        }

        post("/goform_set_cmd_process") {
            val form = call.receiveParameters()
            val goformId = form["goformId"].orEmpty()
            val result = U20GoformBridge.set(context, goformId, form.entries().associate {
                it.key to it.value.firstOrNull().orEmpty()
            })

            if (goformId == "LOGIN" || goformId == "LOGIN_MULTI_USER") {
                call.response.headers.append("kano-cookie", "u20_session=local; Path=/")
            }
            call.respondText(result.toString(), ContentType.Application.Json)
        }
    }

    // Native bridge used by U20-specific pages. It remains behind UFI-TOOLS'
    // normal authentication middleware and never exposes the Flymodem token.
    post("/api/u20/request") {
        val body = JSONObject(call.receiveText())
        val request = body.optString("Request")
        if (request.isBlank()) {
            call.respondText(
                JSONObject().put("error", "Request 不能为空").toString(),
                ContentType.Application.Json,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val parameters = body.opt("Parameters")
        val result = FlymodemApi.request(context, request, parameters)
        call.respondText(result.toString(), ContentType.Application.Json)
    }

    get("/api/u20/sms/status") {
        runCatching { U20SmsAdapter.status(context) }.fold(
            onSuccess = { call.respondText(it.toString(), ContentType.Application.Json) },
            onFailure = {
                call.respondText(
                    JSONObject().put("error", it.message ?: "读取短信状态失败").toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        )
    }

    post("/api/u20/sms/settings") {
        runCatching {
            val body = JSONObject(call.receiveText())
            U20SmsAdapter.updateSettings(
                context,
                body.optString("smsc").takeIf { it.isNotBlank() },
                body.optString("read_store").takeIf { it.isNotBlank() },
                body.optString("write_store").takeIf { it.isNotBlank() },
                body.optString("receive_store").takeIf { it.isNotBlank() }
            )
        }.fold(
            onSuccess = { call.respondText(it.toString(), ContentType.Application.Json) },
            onFailure = {
                call.respondText(
                    JSONObject().put("error", it.message ?: "保存短信设置失败").toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.BadRequest
                )
            }
        )
    }

    get("/api/u20/wifi_qr") {
        val requestedBand = call.request.queryParameters["band"]?.toIntOrNull()
        val wireless = FlymodemApi.request(context, "GetWirelessSetting")
            .optJSONObject("Parameters")
            ?: error("无法读取 U20 Wi-Fi 设置")
        val ap = when (requestedBand) {
            0 -> wireless.optJSONObject("Wireless_24G")
            1 -> wireless.optJSONObject("Wireless_5G")
            else -> null
        } ?: listOf("Wireless_5G", "Wireless_24G")
            .mapNotNull(wireless::optJSONObject)
            .firstOrNull { it.optBoolean("Enable") }
            ?: error("U20 Wi-Fi 未启用")

        call.respondBytes(
            generateWifiQrPng(
                ap.optString("SSID"),
                ap.optString("Password"),
                ap.optString("EncryptType"),
                ap.optBoolean("Hidden")
            ),
            ContentType.Image.PNG
        )
    }

    get("/api/u20/wifi/clients") {
        runCatching {
            val wireless = FlymodemApi.request(context, "GetWirelessSetting")
                .optJSONObject("Parameters")
            U20WifiClientAdapter.list(context, wireless)
        }.fold(
            onSuccess = { call.respondText(it.toString(), ContentType.Application.Json) },
            onFailure = { error ->
                call.respondText(
                    JSONObject().put("error", error.message ?: "读取 Wi-Fi 客户端失败").toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        )
    }

    post("/api/u20/wifi/client/action") {
        runCatching {
            U20WifiClientAdapter.applyAction(context, JSONObject(call.receiveText()))
        }.fold(
            onSuccess = { call.respondText(it.toString(), ContentType.Application.Json) },
            onFailure = { error ->
                call.respondText(
                    JSONObject().put("error", error.message ?: "Wi-Fi 客户端操作失败").toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.BadRequest
                )
            }
        )
    }

    get("/api/u20/cloud_control") {
        U20PlatformAdapter.getCloudControlState(context).fold(
            onSuccess = { state ->
                call.respondText(
                    JSONObject()
                        .put("released", state.released)
                        .put("service", state.serviceState)
                        .toString(),
                    ContentType.Application.Json
                )
            },
            onFailure = { error ->
                call.respondText(
                    JSONObject().put("error", error.message).toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        )
    }

    post("/api/u20/cloud_control") {
        val released = runCatching {
            JSONObject(call.receiveText()).optBoolean("released", true)
        }.getOrDefault(true)
        U20PlatformAdapter.setCloudControlReleased(context, released).fold(
            onSuccess = { state ->
                call.respondText(
                    JSONObject()
                        .put("released", state.released)
                        .put("service", state.serviceState)
                        .toString(),
                    ContentType.Application.Json
                )
            },
            onFailure = { error ->
                call.respondText(
                    JSONObject().put("error", error.message).toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        )
    }

    get("/api/u20/web_entry") {
        U20PlatformAdapter.getWebEntryState(context).fold(
            onSuccess = { state ->
                call.respondText(
                    JSONObject()
                        .put("enabled", state.enabled)
                        .put("active", state.active)
                        .put("target", if (state.enabled) "2333" else "official")
                        .toString(),
                    ContentType.Application.Json
                )
            },
            onFailure = { error ->
                call.respondText(
                    JSONObject().put("error", error.message).toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        )
    }

    post("/api/u20/web_entry") {
        val enabled = runCatching {
            JSONObject(call.receiveText()).optBoolean("enabled", true)
        }.getOrDefault(true)
        U20PlatformAdapter.setWebEntryEnabled(context, enabled).fold(
            onSuccess = { state ->
                call.respondText(
                    JSONObject()
                        .put("enabled", state.enabled)
                        .put("active", state.active)
                        .put("target", if (state.enabled) "2333" else "official")
                        .toString(),
                    ContentType.Application.Json
                )
            },
            onFailure = { error ->
                call.respondText(
                    JSONObject().put("error", error.message).toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        )
    }

    post("/api/u20/power") {
        val action = runCatching { JSONObject(call.receiveText()).optString("action") }
            .getOrDefault("")
        if (action != "reboot" && action != "shutdown") {
            call.respondText(
                JSONObject().put("error", "action 必须是 reboot 或 shutdown").toString(),
                ContentType.Application.Json,
                HttpStatusCode.BadRequest
            )
            return@post
        }
        val operation = if (action == "reboot") {
            runCatching {
                val response = FlymodemApi.request(context, "DeviceReset")
                if (response.optString("Message") != "OK") {
                    error("原厂重启接口返回异常: $response")
                }
                "scheduled"
            }
        } else {
            U20PlatformAdapter.schedulePowerAction(context, action)
        }
        operation.fold(
            onSuccess = {
                call.respondText(
                    JSONObject().put("result", "scheduled").put("action", action).toString(),
                    ContentType.Application.Json
                )
            },
            onFailure = { error ->
                call.respondText(
                    JSONObject().put("error", error.message).toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        )
    }

    get("/api/u20/usb_mode") {
        U20PlatformAdapter.getUsbMode(context).fold(
            onSuccess = { state ->
                call.respondText(
                    JSONObject()
                        .put("current", state.current)
                        .put("state", state.state)
                        .put("default", state.defaultMode)
                        .put("temporary", true)
                        .toString(),
                    ContentType.Application.Json
                )
            },
            onFailure = { error ->
                call.respondText(
                    JSONObject().put("error", error.message).toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        )
    }

    post("/api/u20/usb_mode") {
        val mode = runCatching { JSONObject(call.receiveText()).optString("mode") }
            .getOrDefault("")
        if (mode != "charging,adb" && mode != "rndis,adb") {
            call.respondText(
                JSONObject().put("error", "mode 必须是 charging,adb 或 rndis,adb").toString(),
                ContentType.Application.Json,
                HttpStatusCode.BadRequest
            )
            return@post
        }
        U20PlatformAdapter.scheduleUsbMode(context, mode).fold(
            onSuccess = {
                call.respondText(
                    JSONObject()
                        .put("result", "scheduled")
                        .put("mode", mode)
                        .put("temporary", true)
                        .toString(),
                    ContentType.Application.Json
                )
            },
            onFailure = { error ->
                call.respondText(
                    JSONObject().put("error", error.message).toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.InternalServerError
                )
            }
        )
    }
}

private fun generateWifiQrPng(
    ssid: String,
    password: String,
    encryption: String,
    hidden: Boolean
): ByteArray {
    require(ssid.isNotBlank()) { "U20 Wi-Fi 名称为空" }

    fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace(":", "\\:")

    val type = when (encryption) {
        "OPEN" -> "nopass"
        "WPA3-SAE" -> "SAE"
        else -> "WPA"
    }
    val payload = "WIFI:T:$type;S:${escape(ssid)};P:${escape(password)};H:$hidden;;"
    val matrix = MultiFormatWriter().encode(
        payload,
        BarcodeFormat.QR_CODE,
        480,
        480,
        mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2
        )
    )
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (matrix[x, y]) {
                0xff000000.toInt()
            } else {
                0xffffffff.toInt()
            }
        }
    }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    return try {
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "生成 Wi-Fi 二维码失败"
            }
            output.toByteArray()
        }
    } finally {
        bitmap.recycle()
    }
}

private object U20GoformBridge {
    suspend fun get(
        context: Context,
        commands: List<String>,
        page: Int = 0,
        pageSize: Int = 500
    ): JSONObject {
        val result = JSONObject()
        if (commands.isEmpty()) return result

        if (commands.contains("sms_data_total")) {
            return U20SmsAdapter.list(context, page, pageSize)
        }

        val needsDescription = commands.any { it in descriptionKeys }
        val needsModem = commands.any { it in modemKeys }
        val needsWireless = commands.any { it in wirelessKeys }
        val needsLanguage = commands.contains("Language")

        val description = if (needsDescription) {
            FlymodemApi.request(context, "GetDeviceDescription")
                .optJSONObject("Parameters")
                ?.optJSONObject("CommonDescription")
                ?: JSONObject()
        } else JSONObject()

        val modem = if (needsModem) {
            FlymodemApi.request(context, "GetModemSetting").optJSONObject("Parameters") ?: JSONObject()
        } else JSONObject()

        val wireless = if (needsWireless) {
            FlymodemApi.request(context, "GetWirelessSetting").optJSONObject("Parameters") ?: JSONObject()
        } else JSONObject()

        val language = if (needsLanguage) {
            FlymodemApi.request(context, "GetLanguage")
                .optJSONObject("Parameters")
                ?.optString("language", "cn")
                ?: "cn"
        } else "cn"
        val unreadSms = if (commands.any { it in smsStatusKeys }) {
            runCatching { U20SmsAdapter.unreadCount(context) }.getOrDefault(0)
        } else 0
        val nativeBand = description.string("band").trim().trimStart('N', 'n', 'B', 'b')
        val nativeNetworkType = description.string("networkType")
        val isNr = nativeNetworkType.contains("5G", ignoreCase = true) ||
            description.string("band").startsWith("N", ignoreCase = true)

        commands.forEach { command ->
            if (command == "queryWiFiModuleSwitch") {
                val enabled = wireless.optJSONObject("Wireless_24G")?.optBoolean("Enable") == true ||
                    wireless.optJSONObject("Wireless_5G")?.optBoolean("Enable") == true
                result.put("WiFiModuleSwitch", if (enabled) "1" else "0")
                return@forEach
            }
            if (command == "queryAccessPointInfo") {
                result.put("ResponseList", buildAccessPointList(wireless))
                return@forEach
            }
            if (command == "lte_band_lock") {
                result.put(command, U20BandControl.queryLteBands(context).joinToString(","))
                return@forEach
            }
            if (command == "nr_band_lock") {
                result.put(command, U20BandControl.queryNrBands(context).joinToString(","))
                return@forEach
            }

            val value: Any = when (command) {
                "LD", "RD" -> "u20-local"
                "loginfo" -> "ok"
                "Language" -> language
                "cr_version", "wa_inner_version" -> description.string("SoftwareVersion")
                "usb_port_switch" -> "1"
                "usb_network_protocal" -> "rndis"
                "performance_mode", "samba_switch" -> "0"
                "indicator_light_switch" -> "1"
                "psw_fail_num_str" -> "3"
                "login_lock_time" -> "0"
                "battery_charging" -> "0"
                "sms_received_flag" -> if (unreadSms > 0) "1" else "0"
                "sms_unread_num", "sms_sim_unread_num" -> unreadSms.toString()
                "sim_msisdn", "msisdn" -> description.string("MSISDN")
                "dual_sim_support" -> "0"
                "sim_slot" -> "1"
                "sim_status" -> description.string("SIMStatus")
                "battery_value", "battery_vol_percent" -> "0"
                "network_signalbar", "rssi" -> description.first("SignalStrength", "rssi", "rsrp")
                "network_rssi" -> description.first("rssi", "rsrp", "SignalStrength")
                "Z5g_rsrp" -> if (isNr) description.string("rsrp") else ""
                "Nr_snr" -> if (isNr) description.string("sinr") else ""
                "nr_rsrq" -> if (isNr) description.string("rsrq") else ""
                "Nr_bands" -> if (isNr) nativeBand else ""
                "Nr_fcn" -> if (isNr) description.string("arfcn") else ""
                "Nr_pci" -> if (isNr) description.string("pci") else ""
                "Nr_cell_id" -> if (isNr) description.string("cellId") else ""
                "lte_rsrp" -> if (!isNr) description.string("rsrp") else ""
                "Lte_snr" -> if (!isNr) description.string("sinr") else ""
                "lte_rsrq" -> if (!isNr) description.string("rsrq") else ""
                "Lte_bands" -> if (!isNr) nativeBand else ""
                "Lte_fcn" -> if (!isNr) description.string("arfcn") else ""
                "Lte_pci" -> if (!isNr) description.string("pci") else ""
                "Lte_cell_id" -> if (!isNr) description.string("cellId") else ""
                "iccid" -> description.string("ICCID")
                "imei" -> description.string("IMEI")
                "imsi" -> description.string("IMSI")
                "ipv6_wan_ipaddr" -> description.string("IPv6Addr")
                "lan_ipaddr" -> description.string("IPAddr")
                "mac_address" -> description.string("MACAddr")
                "network_information", "network_type" -> description.string("networkType")
                "network_provider" -> operatorName(context, description)
                "wifi_access_sta_num" -> description.first("ClientsNumber", "number_of_client")
                "ppp_status" -> if (modem.optString("NetworkState") == "Connected") {
                    "ppp_connected"
                } else {
                    "ppp_disconnected"
                }
                "net_select" -> when (modem.optString("NetworkType")) {
                    "only5G" -> "Only_5G"
                    "auto4G" -> "Only_LTE"
                    else -> "WL_AND_5G"
                }
                "roam_setting_option", "dial_roam_setting_option" ->
                    if (modem.optBoolean("EnableRoam")) "on" else "off"
                "realtime_rx_thrpt", "realtime_tx_thrpt", "realtime_time",
                "monthly_tx_bytes", "monthly_rx_bytes", "monthly_time" -> "0"
                "Lte_ca_status" -> ""
                else -> ""
            }
            result.put(command, value)
        }

        return result
    }

    suspend fun set(context: Context, goformId: String, form: Map<String, String>): JSONObject {
        return when (goformId) {
            "LOGIN", "LOGIN_MULTI_USER", "LOGOUT" -> success()
            "SEND_SMS" -> U20SmsAdapter.send(
                context,
                form["Number"].orEmpty(),
                form["MessageBody"].orEmpty()
            ).fold(
                onSuccess = { it },
                onFailure = { JSONObject().put("result", "failure").put("message", it.message) }
            )
            "DELETE_SMS" -> U20SmsAdapter.delete(context, form["msg_id"].orEmpty()).fold(
                onSuccess = { success() },
                onFailure = { JSONObject().put("result", "failure").put("message", it.message) }
            )
            "SET_MSG_READ" -> U20SmsAdapter.markRead(context, form["msg_id"].orEmpty()).fold(
                onSuccess = { success() },
                onFailure = { JSONObject().put("result", "failure").put("message", it.message) }
            )
            "REBOOT_DEVICE" -> {
                FlymodemApi.request(context, "DeviceReset")
                success()
            }
            "SET_BEARER_PREFERENCE" -> {
                val nativeMode = when (form["BearerPreference"]) {
                    "Only_5G" -> "only5G"
                    "Only_LTE", "Only_WCDMA", "WCDMA_AND_LTE" -> "auto4G"
                    else -> "auto5G"
                }
                updateModem(context) { put("NetworkType", nativeMode) }
                success()
            }
            "SET_CONNECTION_MODE" -> {
                updateModem(context) {
                    form["roam_setting_option"]?.let { put("EnableRoam", it == "on") }
                }
                success()
            }
            "CONNECT_NETWORK" -> {
                updateModem(context) { put("Enable", true) }
                success()
            }
            "DISCONNECT_NETWORK" -> {
                updateModem(context) { put("Enable", false) }
                success()
            }
            "switchWiFiModule" -> {
                updateWireless(context) {
                    optJSONObject("Wireless_24G")?.put("Enable", false)
                    optJSONObject("Wireless_5G")?.put("Enable", false)
                }
                success()
            }
            "switchWiFiChip" -> {
                val useFiveG = form["ChipEnum"] == "chip2"
                updateWireless(context) {
                    optJSONObject("Wireless_24G")?.put("Enable", !useFiveG)
                    optJSONObject("Wireless_5G")?.put("Enable", useFiveG)
                }
                success()
            }
            "setAccessPointInfo" -> {
                val useFiveG = form["ChipIndex"] == "1"
                updateWireless(context) {
                    form["ApMaxStationNumber"]?.toIntOrNull()?.let { put("MaxAccessNum", it) }
                    val target = optJSONObject(if (useFiveG) "Wireless_5G" else "Wireless_24G")
                        ?: error("U20 Wi-Fi 配置缺少目标频段")
                    form["SSID"]?.takeIf { it.isNotBlank() }?.let { target.put("SSID", it) }
                    form["Password"]?.takeIf { it.isNotBlank() }?.let {
                        target.put("Password", String(Base64.decode(it, Base64.DEFAULT), Charsets.UTF_8))
                    }
                    form["ApBroadcastDisabled"]?.let { target.put("Hidden", it == "1") }
                    form["AuthMode"]?.let { target.put("EncryptType", toNativeEncryption(it)) }
                }
                success()
            }
            "LTE_BAND_LOCK" -> {
                U20BandControl.setLteBands(context, parseBandList(form["lte_band_lock"]))
                success()
            }
            "NR_BAND_LOCK" -> {
                U20BandControl.setNrBands(context, parseBandList(form["nr_band_lock"]))
                success()
            }
            else -> JSONObject()
                .put("result", "unsupported")
                .put("error", "FM_U20 暂未适配 $goformId")
                .put("request", JSONObject(form))
        }
    }

    private fun success() = JSONObject().put("result", "success")

    private fun parseBandList(value: String?): List<Int> = value.orEmpty()
        .split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .distinct()

    private suspend fun updateModem(context: Context, change: JSONObject.() -> Unit) {
        val current = FlymodemApi.request(context, "GetModemSetting").optJSONObject("Parameters")
            ?: error("无法读取 U20 拨号设置")
        current.change()
        FlymodemApi.request(context, "SetModemSetting", current)
    }

    private suspend fun updateWireless(context: Context, change: JSONObject.() -> Unit) {
        val current = FlymodemApi.request(context, "GetWirelessSetting").optJSONObject("Parameters")
            ?: error("无法读取 U20 Wi-Fi 设置")
        current.change()
        FlymodemApi.request(context, "SetWirelessSetting", current)
    }

    private fun buildAccessPointList(wireless: JSONObject): JSONArray {
        val maxClients = wireless.optInt("MaxAccessNum", 10)
        val result = JSONArray()
        listOf("Wireless_24G" to 0, "Wireless_5G" to 1).forEach { (name, index) ->
            val item = wireless.optJSONObject(name) ?: return@forEach
            val password = Base64.encodeToString(
                item.optString("Password").toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
            result.put(
                JSONObject()
                    .put("AccessPointSwitchStatus", if (item.optBoolean("Enable")) "1" else "0")
                    .put("ChipIndex", index.toString())
                    .put("AccessPointIndex", index.toString())
                    .put("ApMaxStationNumber", maxClients.toString())
                    .put("Password", password)
                    .put("ApBroadcastDisabled", if (item.optBoolean("Hidden")) "1" else "0")
                    .put("SSID", item.optString("SSID"))
                    .put("AuthMode", fromNativeEncryption(item.optString("EncryptType")))
                    .put("QrImageUrl", "/u20/wifi_qr?band=$index")
            )
        }
        return result
    }

    private fun fromNativeEncryption(value: String): String = when (value) {
        "OPEN" -> "OPEN"
        "WPA3-SAE" -> "WPA3PSK"
        "Mixed" -> "WPA2PSKWPA3PSK"
        else -> "WPA2PSK"
    }

    private fun toNativeEncryption(value: String): String = when (value) {
        "OPEN" -> "OPEN"
        "WPA3PSK" -> "WPA3-SAE"
        "WPA2PSKWPA3PSK" -> "Mixed"
        else -> "WPA2-PSK"
    }

    private val descriptionKeys = setOf(
        "cr_version", "wa_inner_version", "sim_msisdn", "msisdn", "sim_status",
        "network_signalbar", "network_rssi", "rssi", "Z5g_rsrp", "lte_rsrp",
        "Nr_snr", "nr_rsrq", "Nr_bands", "Nr_fcn", "Nr_pci", "Nr_cell_id",
        "Lte_snr", "lte_rsrq", "Lte_bands", "Lte_fcn", "Lte_pci", "Lte_cell_id",
        "iccid", "imei", "imsi", "ipv6_wan_ipaddr", "lan_ipaddr", "mac_address",
        "network_information", "network_type", "network_provider", "wifi_access_sta_num"
    )
    private val modemKeys = setOf(
        "ppp_status", "net_select", "roam_setting_option", "dial_roam_setting_option"
    )
    private val wirelessKeys = setOf("queryWiFiModuleSwitch", "queryAccessPointInfo")
    private val smsStatusKeys = setOf(
        "sms_received_flag", "sms_unread_num", "sms_sim_unread_num"
    )

    private fun JSONObject.string(name: String): String = optString(name, "")

    private fun JSONObject.first(vararg names: String): String {
        for (name in names) {
            val value = optString(name, "")
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun operatorName(context: Context, description: JSONObject): String {
        description.first("networkProvider", "OperatorName").takeIf { it.isNotBlank() }?.let {
            return it
        }
        val telephonyName = runCatching {
            (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)
                ?.networkOperatorName
                .orEmpty()
        }.getOrDefault("").trim().trim(',')
        if (telephonyName.isNotBlank()) return telephonyName

        for (property in listOf("gsm.operator.alpha", "gsm.sim.operator.alpha")) {
            val actualName = readSystemProperty(property)
                .split(',')
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
                .trim()
            if (actualName.isNotBlank()) return actualName
        }

        val registeredPlmn = readSystemProperty("gsm.operator.numeric")
            .split(',')
            .firstOrNull { it.length >= 5 && it.all(Char::isDigit) }
            .orEmpty()
        val mcc = description.string("mcc").filter(Char::isDigit)
            .ifBlank { registeredPlmn.take(3) }
        val rawMnc = description.string("mnc").filter(Char::isDigit)
            .ifBlank { registeredPlmn.drop(3) }
        if (mcc.isBlank() || rawMnc.isBlank()) return ""
        val mnc = rawMnc.padStart(2, '0')
        if (mcc != "460") return listOf(mcc, mnc).joinToString("").ifBlank { "" }.let {
            if (it.isBlank()) "" else "PLMN $it"
        }
        return when (mnc) {
            "00", "02", "04", "07", "08", "13" -> "中国移动"
            "01", "06", "09" -> "中国联通"
            "03", "05", "11" -> "中国电信"
            "15" -> "中国广电"
            else -> if (mnc.isBlank()) "" else "运营商 460$mnc"
        }
    }

    private fun readSystemProperty(name: String): String = runCatching {
        val systemProperties = Class.forName("android.os.SystemProperties")
        systemProperties.getMethod("get", String::class.java).invoke(null, name) as? String ?: ""
    }.getOrDefault("")
}

private object U20BandControl {
    private val nrFddMasks = mapOf(1 to 1L, 3 to 4L, 5 to 8L, 8 to 32L, 28 to 512L)
    private val nrTddMasks = mapOf(41 to 16L, 77 to 128L, 78 to 256L, 79 to 512L)

    fun queryLteBands(context: Context): List<Int> {
        val values = queryMasks(context, "AT+SPLBAND=0")
        if (values.size < 5) error("LTE SPLBAND 返回字段不足: $values")
        val tddMask = values[1]
        val fddMask = values[3]
        return buildList {
            for (band in 1..32) if (fddMask and (1L shl (band - 1)) != 0L) add(band)
            for (band in 33..48) if (tddMask and (1L shl (band - 33)) != 0L) add(band)
        }
    }

    fun queryNrBands(context: Context): List<Int> {
        var values = queryMasks(context, "AT+SPLBAND=3")
        if (values.size < 3) error("NR SPLBAND 返回字段不足: $values")
        if (values.all { it == 0L }) {
            // On this modem zero means unrestricted; display the supported set.
            values = queryMasks(context, "AT+SPLBAND=4")
            if (values.size < 3) error("NR SPLBAND 能力字段不足: $values")
        }
        val fddMask = values[0]
        val tddMask = values[2]
        return buildList {
            nrFddMasks.forEach { (band, mask) -> if (fddMask and mask != 0L) add(band) }
            nrTddMasks.forEach { (band, mask) -> if (tddMask and mask != 0L) add(band) }
        }.sorted()
    }

    fun setLteBands(context: Context, bands: List<Int>) {
        var tddMask = 0L
        var fddMask = 0L
        bands.forEach { band ->
            when (band) {
                in 1..32 -> fddMask = fddMask or (1L shl (band - 1))
                in 33..48 -> tddMask = tddMask or (1L shl (band - 33))
            }
        }
        runChecked(context, "AT+SPLBAND=1,0,$tddMask,0,$fddMask,0")
    }

    fun setNrBands(context: Context, bands: List<Int>) {
        val fddMask = bands.fold(0L) { mask, band -> mask or (nrFddMasks[band] ?: 0L) }
        val tddMask = bands.fold(0L) { mask, band -> mask or (nrTddMasks[band] ?: 0L) }
        runChecked(context, "AT+SPLBAND=2,$fddMask,0,$tddMask,0")
    }

    private fun queryMasks(context: Context, command: String): List<Long> {
        val raw = runChecked(context, command)
        val values = raw.substringAfter("+SPLBAND:", "")
            .substringBefore("OK")
            .trim()
            .split(',')
            .mapNotNull { it.trim().toLongOrNull() }
        if (values.isEmpty()) error("SPLBAND 返回格式异常: $raw")
        return values
    }

    private fun runChecked(context: Context, command: String): String {
        val result = KanoUtils.runAT(context, command, 0)
        if (result.contains("ERROR", ignoreCase = true) || !result.contains("OK", ignoreCase = true)) {
            error("U20 AT 执行失败: $command => $result")
        }
        return result
    }
}

private object FlymodemApi {
    private const val ENDPOINT = "http://127.0.0.1:8080/MF08_Pages"
    private const val USER_NAME = "admin"
    private const val DEFAULT_PASSWORD = "admin"

    @Volatile
    private var token = ""

    suspend fun request(context: Context, request: String, parameters: Any? = null): JSONObject =
        withContext(Dispatchers.IO) {
            requestBlocking(context, request, parameters, true)
        }

    private fun requestBlocking(
        context: Context,
        request: String,
        parameters: Any?,
        retry: Boolean
    ): JSONObject {
        if (request != "LoginRequest" && token.isBlank()) login(context)

        val result = rawPost(request, if (request == "LoginRequest") "" else token, parameters)
        if (result != null && result.optString("Message") == "OK") return result

        if (request != "LoginRequest" && retry) {
            token = ""
            login(context)
            return requestBlocking(context, request, parameters, false)
        }
        error("Flymodem API 请求失败: $request, response=$result")
    }

    @Synchronized
    private fun login(context: Context) {
        if (token.isNotBlank()) return
        val password = context.getSharedPreferences("kano_ZTE_store", Context.MODE_PRIVATE)
            .getString("ADMIN_PWD", DEFAULT_PASSWORD)
            .orEmpty()
            .ifBlank { DEFAULT_PASSWORD }
        val parameters = JSONObject()
            .put("UserName", USER_NAME)
            .put("Password", password)
        val result = rawPost("LoginRequest", "", parameters)
            ?: error("Flymodem 登录无响应")
        token = result.optJSONObject("Parameters")?.optString("Token").orEmpty()
        if (result.optString("Message") != "OK" || token.isBlank()) {
            error("Flymodem 登录失败: $result")
        }
    }

    private fun rawPost(request: String, authToken: String, parameters: Any?): JSONObject? {
        val payload = JSONObject()
            .put("Request", request)
            .put("Token", authToken)
            .put("Parameters", parameters ?: "")

        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setRequestProperty(HttpHeaders.ContentType, "application/json; charset=UTF-8")
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty().trim()
            if (text.isEmpty() || text == "null") null else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
