package com.minikano.f50_sms.modules.u20

import android.content.Context
import android.os.SystemClock
import com.minikano.f50_sms.utils.U20PlatformAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Native client monitor for the MN901 hotspot.
 *
 * This firmware does not ship `iw`.  The Unisoc driver exposes associated
 * stations through /proc/skwifi, while Android's neighbour/DHCP state provides
 * addresses and host names.  Forwarded byte counters are maintained in a
 * dedicated iptables chain so the values shown by the UI are measured rather
 * than estimated.
 */
internal object U20WifiClientAdapter {
    private const val PREFS = "u20_wifi_client_policy"
    private const val STATS_CHAIN = "UFI_WIFI_STATS"
    private const val BLOCK_CHAIN = "UFI_WIFI_BLOCK"

    private val macPattern = Regex("^[0-9a-f]{2}(?::[0-9a-f]{2}){5}$", RegexOption.IGNORE_CASE)
    private val ipv4Pattern = Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$")
    private val samples = linkedMapOf<String, ClientSample>()
    private var addressSignature = ""
    private var policySignature: String? = null

    private data class DriverClient(
        val mac: String,
        var rssi: Int? = null,
        var mcs: Int? = null,
        var nss: Int? = null,
        var txFailed: Long? = null
    )

    private data class AddressInfo(
        val ipv4: String = "",
        val ipv6: List<String> = emptyList(),
        val hostname: String = ""
    )

    private data class Counters(val upload: Long = 0, val download: Long = 0)

    private data class ClientSample(
        var firstSeenMs: Long,
        var lastSeenMs: Long,
        var lastSampleMs: Long,
        var lastUpload: Long,
        var lastDownload: Long,
        var totalUpload: Long,
        var totalDownload: Long,
        var uploadRate: Long = 0,
        var downloadRate: Long = 0
    )

    suspend fun list(context: Context, wireless: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val raw = U20PlatformAdapter.runRootCommand(context, discoveryCommand(), 12_000).getOrThrow()
        val sections = splitSections(raw)
        val driver = parseDriver(sections["DRIVER"].orEmpty())
        val addresses = parseAddresses(
            sections["ARP"].orEmpty(),
            sections["NEIGH"].orEmpty(),
            sections["DHCP"].orEmpty()
        )
        val active = driver.keys.sorted()
        val activeAddresses = active.mapNotNull { mac ->
            addresses[mac]?.ipv4?.takeIf { isValidIpv4(it) }?.let { mac to it }
        }
        val signature = activeAddresses.joinToString("|") { "${it.first}@${it.second}" }
        val rebuild = signature != addressSignature
        if (rebuild) addressSignature = signature
        val counters = refreshCounters(context, activeAddresses, rebuild)
        applyPolicies(context, activeAddresses)

        val now = SystemClock.elapsedRealtime()
        synchronized(samples) {
            samples.keys.filter { it !in active }.forEach(samples::remove)
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val clients = JSONArray()
        var totalUploadRate = 0L
        var totalDownloadRate = 0L
        var totalUpload = 0L
        var totalDownload = 0L

        active.forEach { mac ->
            val station = driver.getValue(mac)
            val address = addresses[mac] ?: AddressInfo()
            val current = counters[mac] ?: Counters()
            val sample = updateSample(mac, current, now)
            totalUploadRate += sample.uploadRate
            totalDownloadRate += sample.downloadRate
            totalUpload += sample.totalUpload
            totalDownload += sample.totalDownload
            val customName = prefs.getString("name_$mac", "").orEmpty().trim()
            val hostname = customName.ifBlank { address.hostname }.ifBlank {
                if (isRandomizedMac(mac)) "随机 MAC 设备" else "未命名设备"
            }
            val rssi = station.rssi
            clients.put(
                JSONObject()
                    .put("mac", mac.uppercase(Locale.US))
                    .put("ip", address.ipv4)
                    .put("ipv6", JSONArray(address.ipv6))
                    .put("name", hostname)
                    .put("dhcp_name", address.hostname)
                    .put("custom_name", customName)
                    .put("randomized_mac", isRandomizedMac(mac))
                    .put("rssi", rssi ?: JSONObject.NULL)
                    .put("signal_percent", rssi?.let(::signalPercent) ?: JSONObject.NULL)
                    .put("mcs", station.mcs ?: JSONObject.NULL)
                    .put("spatial_streams", station.nss?.plus(1) ?: JSONObject.NULL)
                    .put("tx_failed", station.txFailed ?: JSONObject.NULL)
                    .put("connected_seconds", ((now - sample.firstSeenMs) / 1000L).coerceAtLeast(0))
                    .put("upload_bytes", sample.totalUpload)
                    .put("download_bytes", sample.totalDownload)
                    .put("upload_rate", sample.uploadRate)
                    .put("download_rate", sample.downloadRate)
                    .put("blocked", prefs.getBoolean("blocked_$mac", false))
                    .put("upload_limit_kbps", prefs.getInt("upload_$mac", 0))
                    .put("download_limit_kbps", prefs.getInt("download_$mac", 0))
            )
        }

        val driverMeta = parseDriverMeta(sections["DRIVER"].orEmpty())
        val ap = activeWireless(wireless)
        JSONObject()
            .put("result", "success")
            .put("interface", "wlan0")
            .put("ssid", ap?.optString("SSID").orEmpty())
            .put("max_clients", wireless?.optInt("MaxAccessNum", 0) ?: 0)
            .put("client_count", clients.length())
            .put("channel", driverMeta.first)
            .put("bandwidth", driverMeta.second)
            .put("band", driverMeta.first?.let { if (it > 14) "5 GHz" else "2.4 GHz" }.orEmpty())
            .put("upload_rate", totalUploadRate)
            .put("download_rate", totalDownloadRate)
            .put("upload_bytes", totalUpload)
            .put("download_bytes", totalDownload)
            .put("traffic_scope", "从本次监控开始的互联网转发流量")
            .put("duration_scope", "从本次监测到客户端开始")
            .put("clients", clients)
    }

    suspend fun applyAction(context: Context, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val action = body.optString("action")
        val mac = normalizeMac(body.optString("mac"))
        require(macPattern.matches(mac)) { "MAC 地址无效" }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        when (action) {
            "rename" -> {
                val name = body.optString("name").trim()
                require(name.length <= 40) { "设备名称不能超过 40 个字符" }
                prefs.edit().putString("name_$mac", name).apply()
            }
            "block" -> prefs.edit().putBoolean("blocked_$mac", true).apply()
            "unblock" -> prefs.edit().putBoolean("blocked_$mac", false).apply()
            "set_limit" -> {
                val upload = checkedLimit(body.optInt("upload_limit_kbps", 0))
                val download = checkedLimit(body.optInt("download_limit_kbps", 0))
                prefs.edit().putInt("upload_$mac", upload).putInt("download_$mac", download).apply()
            }
            else -> error("不支持的 Wi-Fi 客户端操作")
        }

        val active = readActiveAddresses(context)
        applyPolicies(context, active)
        JSONObject()
            .put("result", "success")
            .put("action", action)
            .put("mac", mac.uppercase(Locale.US))
            .put("applied", active.any { it.first == mac })
    }

    /**
     * Re-apply saved block and bandwidth policies after boot, and when a
     * limited client obtains a new address.  The service calls this
     * periodically, but applyPolicies itself is signature gated so unchanged
     * state does not rewrite qdisc/iptables rules.
     */
    suspend fun maintainPolicies(context: Context, resetCache: Boolean = false) = withContext(Dispatchers.IO) {
        if (resetCache) synchronized(this@U20WifiClientAdapter) { policySignature = null }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val hasLimits = prefs.all.keys.any { key ->
            (key.startsWith("upload_") || key.startsWith("download_")) &&
                (prefs.all[key] as? Int ?: 0) > 0
        }
        val active = if (hasLimits) readActiveAddresses(context) else emptyList()
        applyPolicies(context, active)
    }

    private fun checkedLimit(value: Int): Int {
        require(value == 0 || value in 128..1_000_000) { "限速值必须为 0，或 128-1000000 Kbps" }
        return value
    }

    private fun discoveryCommand() = """
        echo __DRIVER__
        cat /proc/skwifi/chip1.sdio/wlan0 2>/dev/null
        echo __ARP__
        cat /proc/net/arp 2>/dev/null
        echo __NEIGH__
        ip neigh show dev wlan0 2>/dev/null
        echo __DHCP__
        dumpsys network_stack 2>/dev/null | grep -E 'assignedLease|created/renewed lease' | tail -120
    """.trimIndent()

    private suspend fun readActiveAddresses(context: Context): List<Pair<String, String>> {
        val raw = U20PlatformAdapter.runRootCommand(context, discoveryCommand(), 12_000).getOrThrow()
        val sections = splitSections(raw)
        val driver = parseDriver(sections["DRIVER"].orEmpty())
        val addresses = parseAddresses(
            sections["ARP"].orEmpty(), sections["NEIGH"].orEmpty(), sections["DHCP"].orEmpty()
        )
        return driver.keys.mapNotNull { mac ->
            addresses[mac]?.ipv4?.takeIf(::isValidIpv4)?.let { mac to it }
        }
    }

    private fun refreshCounters(
        context: Context,
        clients: List<Pair<String, String>>,
        rebuild: Boolean
    ): Map<String, Counters> {
        if (rebuild) synchronized(samples) {
            samples.values.forEach {
                it.lastUpload = 0
                it.lastDownload = 0
            }
        }
        val rules = clients.joinToString("\n") { (mac, ip) ->
            val key = mac.replace(":", "").uppercase(Locale.US)
            "iptables -A $STATS_CHAIN -s $ip/32 -m comment --comment U20TX_$key -j RETURN\n" +
                "iptables -A $STATS_CHAIN -d $ip/32 -m comment --comment U20RX_$key -j RETURN"
        }
        val command = buildString {
            append("iptables -N $STATS_CHAIN >/dev/null 2>&1 || true\n")
            append("iptables -C FORWARD -j $STATS_CHAIN >/dev/null 2>&1 || iptables -I FORWARD 1 -j $STATS_CHAIN\n")
            if (rebuild) append("iptables -F $STATS_CHAIN\n")
            if (!rebuild) {
                clients.forEach { (mac, ip) ->
                    val key = mac.replace(":", "").uppercase(Locale.US)
                    append("iptables -C $STATS_CHAIN -s $ip/32 -m comment --comment U20TX_$key -j RETURN >/dev/null 2>&1 || iptables -A $STATS_CHAIN -s $ip/32 -m comment --comment U20TX_$key -j RETURN\n")
                    append("iptables -C $STATS_CHAIN -d $ip/32 -m comment --comment U20RX_$key -j RETURN >/dev/null 2>&1 || iptables -A $STATS_CHAIN -d $ip/32 -m comment --comment U20RX_$key -j RETURN\n")
                }
            } else {
                append(rules).append('\n')
            }
            append("iptables -nvxL $STATS_CHAIN --line-numbers 2>/dev/null")
        }
        val output = U20PlatformAdapter.runRootCommand(context, command, 8_000).getOrThrow()
        val byKey = clients.associate { it.first.replace(":", "").uppercase(Locale.US) to it.first }
        val result = linkedMapOf<String, Counters>()
        output.lineSequence().forEach { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 10 || parts[0].toIntOrNull() == null) return@forEach
            val bytes = parts[2].toLongOrNull() ?: return@forEach
            val comment = Regex("U20(TX|RX)_([0-9A-F]{12})").find(line) ?: return@forEach
            val mac = byKey[comment.groupValues[2]] ?: return@forEach
            val previous = result[mac] ?: Counters()
            result[mac] = if (comment.groupValues[1] == "TX") {
                previous.copy(upload = bytes)
            } else {
                previous.copy(download = bytes)
            }
        }
        return result
    }

    private fun updateSample(mac: String, counters: Counters, now: Long): ClientSample = synchronized(samples) {
        val existing = samples[mac]
        if (existing == null) {
            return@synchronized ClientSample(
                firstSeenMs = now,
                lastSeenMs = now,
                lastSampleMs = now,
                lastUpload = counters.upload,
                lastDownload = counters.download,
                totalUpload = counters.upload,
                totalDownload = counters.download
            ).also { samples[mac] = it }
        }
        val elapsed = (now - existing.lastSampleMs).coerceAtLeast(1L)
        val uploadDelta = if (counters.upload >= existing.lastUpload) {
            counters.upload - existing.lastUpload
        } else counters.upload
        val downloadDelta = if (counters.download >= existing.lastDownload) {
            counters.download - existing.lastDownload
        } else counters.download
        existing.totalUpload += uploadDelta
        existing.totalDownload += downloadDelta
        existing.uploadRate = uploadDelta * 1000L / elapsed
        existing.downloadRate = downloadDelta * 1000L / elapsed
        existing.lastUpload = counters.upload
        existing.lastDownload = counters.download
        existing.lastSampleMs = now
        existing.lastSeenMs = now
        existing
    }

    private fun applyPolicies(context: Context, clients: List<Pair<String, String>>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val blocked = prefs.all.keys
            .filter { it.startsWith("blocked_") && prefs.getBoolean(it, false) }
            .map { it.removePrefix("blocked_") }
            .filter(macPattern::matches)
        val limits = clients.mapNotNull { (mac, ip) ->
            val upload = prefs.getInt("upload_$mac", 0)
            val download = prefs.getInt("download_$mac", 0)
            if (upload > 0 || download > 0) LimitPolicy(mac, ip, upload, download) else null
        }
        val nextSignature = blocked.sorted().joinToString(",") + "|" +
            limits.sortedBy { it.mac }.joinToString(",") { "${it.mac}@${it.ip}:${it.upload}:${it.download}" }
        synchronized(this) {
            if (nextSignature == policySignature) return
        }
        val command = buildString {
            append("iptables -N $BLOCK_CHAIN >/dev/null 2>&1 || true\n")
            append("iptables -C FORWARD -j $BLOCK_CHAIN >/dev/null 2>&1 || iptables -I FORWARD 1 -j $BLOCK_CHAIN\n")
            append("iptables -F $BLOCK_CHAIN\n")
            blocked.forEach { mac ->
                append("iptables -A $BLOCK_CHAIN -m mac --mac-source $mac -j DROP\n")
            }
            (20000..20031).forEach { preference ->
                append("tc filter del dev wlan0 ingress pref $preference >/dev/null 2>&1 || true\n")
            }
            if (limits.any { it.download > 0 }) {
                append("tc qdisc replace dev wlan0 root handle 20: htb default 9999\n")
                append("tc class replace dev wlan0 parent 20: classid 20:1 htb rate 1000mbit ceil 1000mbit\n")
                append("tc class replace dev wlan0 parent 20:1 classid 20:9999 htb rate 1000mbit ceil 1000mbit\n")
                limits.forEachIndexed { index, policy ->
                    if (policy.download <= 0) return@forEachIndexed
                    val classId = 100 + index
                    append("tc class replace dev wlan0 parent 20:1 classid 20:$classId htb rate ${policy.download}kbit ceil ${policy.download}kbit\n")
                    append("tc filter add dev wlan0 parent 20: protocol ip pref ${100 + index} u32 match ip dst ${policy.ip}/32 flowid 20:$classId\n")
                }
            } else {
                // The stock driver uses an mq root qdisc. `tc ... show ... root`
                // is not supported by this toybox build, so restore it directly.
                append("tc qdisc replace dev wlan0 root mq\n")
            }
            limits.forEachIndexed { index, policy ->
                if (policy.upload <= 0) return@forEachIndexed
                append("tc filter add dev wlan0 ingress protocol ip pref ${20000 + index} u32 match ip src ${policy.ip}/32 police rate ${policy.upload}kbit burst 128k drop\n")
            }
        }
        U20PlatformAdapter.runRootCommand(context, command, 8_000).getOrThrow()
        synchronized(this) { policySignature = nextSignature }
    }

    private data class LimitPolicy(val mac: String, val ip: String, val upload: Int, val download: Int)

    private fun splitSections(output: String): Map<String, String> {
        val result = linkedMapOf<String, StringBuilder>()
        var current: String? = null
        output.lineSequence().forEach { line ->
            val match = Regex("^__(DRIVER|ARP|NEIGH|DHCP)__$").matchEntire(line.trim())
            if (match != null) {
                current = match.groupValues[1]
                result.getOrPut(current!!) { StringBuilder() }
            } else current?.let { result.getOrPut(it) { StringBuilder() }.appendLine(line) }
        }
        return result.mapValues { it.value.toString() }
    }

    private fun parseDriver(text: String): LinkedHashMap<String, DriverClient> {
        val result = linkedMapOf<String, DriverClient>()
        var current: DriverClient? = null
        text.lineSequence().forEach { line ->
            val peer = Regex("^\\s*([0-9a-f]{2}(?::[0-9a-f]{2}){5})\\s+\\(\\d+\\)\\s+COMPLETED", RegexOption.IGNORE_CASE)
                .find(line)
            if (peer != null) {
                val mac = normalizeMac(peer.groupValues[1])
                current = DriverClient(mac)
                result[mac] = current!!
                return@forEach
            }
            val station = current ?: return@forEach
            Regex("tx_failed:\\s*(\\d+)", RegexOption.IGNORE_CASE).find(line)?.let {
                station.txFailed = it.groupValues[1].toLongOrNull()
            }
            if (line.trimStart().startsWith("RX:")) {
                Regex("mcs:\\s*(\\d+)", RegexOption.IGNORE_CASE).find(line)?.let {
                    station.mcs = it.groupValues[1].toIntOrNull()
                }
                Regex("nss:\\s*(\\d+)", RegexOption.IGNORE_CASE).find(line)?.let {
                    station.nss = it.groupValues[1].toIntOrNull()
                }
            }
            Regex("rssi:.*data:\\s*(-?\\d+)", RegexOption.IGNORE_CASE).find(line)?.let {
                station.rssi = it.groupValues[1].toIntOrNull()
            }
        }
        return result
    }

    private fun parseDriverMeta(text: String): Pair<Int?, String> {
        val channel = Regex("^\\s*channel:\\s*(\\d+)", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
        val width = Regex("^\\s*width:\\s*([^\\r\\n]+)", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
            .find(text)?.groupValues?.get(1)?.trim().orEmpty()
        return channel to width
    }

    private fun parseAddresses(arp: String, neigh: String, dhcp: String): Map<String, AddressInfo> {
        val ipv4 = linkedMapOf<String, String>()
        arp.lineSequence().forEach { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 6 && parts[5] == "wlan0" && macPattern.matches(parts[3])) {
                ipv4[normalizeMac(parts[3])] = parts[0]
            }
        }
        val ipv6 = linkedMapOf<String, MutableList<String>>()
        neigh.lineSequence().forEach { line ->
            val parts = line.trim().split(Regex("\\s+"))
            val lladdr = parts.indexOf("lladdr")
            if (parts.isEmpty() || lladdr < 0 || lladdr + 1 >= parts.size) return@forEach
            val mac = normalizeMac(parts[lladdr + 1])
            if (!macPattern.matches(mac)) return@forEach
            if (parts[0].contains(':')) ipv6.getOrPut(mac) { mutableListOf() }.add(parts[0])
            else if (isValidIpv4(parts[0])) ipv4[mac] = parts[0]
        }
        val hostnames = linkedMapOf<String, String>()
        dhcp.lineSequence().forEach { line ->
            val match = Regex(
                "hwAddr:\\s*([0-9a-f:]{17}).*?netAddr:\\s*((?:\\d{1,3}\\.){3}\\d{1,3})/\\d+.*?hostname:\\s*([^,\\s]+)",
                RegexOption.IGNORE_CASE
            ).find(line) ?: return@forEach
            val mac = normalizeMac(match.groupValues[1])
            if (!ipv4.containsKey(mac)) ipv4[mac] = match.groupValues[2]
            val hostname = match.groupValues[3].trim().takeUnless { it.equals("null", true) }.orEmpty()
            if (hostname.isNotBlank()) hostnames[mac] = hostname
        }
        return (ipv4.keys + ipv6.keys + hostnames.keys).associateWith { mac ->
            AddressInfo(ipv4[mac].orEmpty(), ipv6[mac].orEmpty().distinct(), hostnames[mac].orEmpty())
        }
    }

    private fun activeWireless(wireless: JSONObject?): JSONObject? {
        if (wireless == null) return null
        return listOf("Wireless_5G", "Wireless_24G")
            .mapNotNull(wireless::optJSONObject)
            .firstOrNull { it.optBoolean("Enable") }
    }

    private fun normalizeMac(value: String) = value.trim().lowercase(Locale.US)

    private fun isValidIpv4(value: String): Boolean = ipv4Pattern.matches(value) &&
        value.split('.').all { (it.toIntOrNull() ?: 256) in 0..255 }

    private fun isRandomizedMac(mac: String): Boolean {
        val first = mac.substringBefore(':').toIntOrNull(16) ?: return false
        return first and 0x02 != 0
    }

    private fun signalPercent(rssi: Int): Int = (((rssi.coerceIn(-90, -35) + 90) * 100.0) / 55.0)
        .toInt().coerceIn(0, 100)
}
