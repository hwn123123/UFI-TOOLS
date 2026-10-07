package com.minikano.f50_sms.utils

import android.content.Context
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class U20SystemStats(
    val cpuUsage: Double,
    val memoryUsage: Double,
    val cpuUsageInfo: JSONObject,
    val memoryInfo: JSONObject,
    val cpuFrequencyInfo: JSONObject,
    val thermalZones: JSONArray,
    val maxTemperature: Int,
    val networkInterface: String,
    val networkRxBytes: Long,
    val networkTxBytes: Long,
    val networkRxBytesPerSecond: Long,
    val networkTxBytesPerSecond: Long,
    val wifiRxBytesPerSecond: Long,
    val wifiTxBytesPerSecond: Long,
    val batteryCurrentUa: Int,
    val batteryVoltageUv: Int,
    val networkSampleTime: Long
)

private data class U20NetworkSample(
    val interfaceName: String,
    val rxBytes: Long,
    val txBytes: Long,
    val sampledAtMs: Long
)

private data class U20NetworkRate(
    val rxBytesPerSecond: Long,
    val txBytesPerSecond: Long
)

private val networkSampleLock = Any()
private val previousNetworkSamples = mutableMapOf<String, U20NetworkSample>()

private fun calculateNetworkRate(current: U20NetworkSample): U20NetworkRate = synchronized(networkSampleLock) {
    val previous = previousNetworkSamples[current.interfaceName]
    previousNetworkSamples[current.interfaceName] = current
    if (previous == null ||
        previous.interfaceName != current.interfaceName ||
        current.sampledAtMs <= previous.sampledAtMs ||
        current.rxBytes < previous.rxBytes ||
        current.txBytes < previous.txBytes
    ) {
        return@synchronized U20NetworkRate(0L, 0L)
    }

    val elapsedMs = current.sampledAtMs - previous.sampledAtMs
    U20NetworkRate(
        rxBytesPerSecond = ((current.rxBytes - previous.rxBytes) * 1000L / elapsedMs).coerceAtLeast(0L),
        txBytesPerSecond = ((current.txBytes - previous.txBytes) * 1000L / elapsedMs).coerceAtLeast(0L)
    )
}

/** Reads procfs/sysfs through the app's private Magisk socket on the MN901. */
fun readU20SystemStats(context: Context): U20SystemStats {
    val command = """
        echo __STAT1__
        cat /proc/stat
        sleep 0.2
        echo __STAT2__
        cat /proc/stat
        echo __MEM__
        cat /proc/meminfo
        echo __NET__
        IFACE=""
        for CANDIDATE in /sys/class/net/sipa_eth* /sys/class/net/seth* /sys/class/net/rmnet* /sys/class/net/wwan* /sys/class/net/ccmni*; do
            [ -d "${'$'}CANDIDATE" ] || continue
            [ "${'$'}(cat "${'$'}CANDIDATE/operstate" 2>/dev/null)" = "up" ] || continue
            IFACE=${'$'}{CANDIDATE##*/}
            break
        done
        if [ -n "${'$'}IFACE" ] && [ -r "/sys/class/net/${'$'}IFACE/statistics/rx_bytes" ]; then
            RX=${'$'}(cat "/sys/class/net/${'$'}IFACE/statistics/rx_bytes")
            TX=${'$'}(cat "/sys/class/net/${'$'}IFACE/statistics/tx_bytes")
            WIFI_RX=0
            WIFI_TX=0
            [ -r /sys/class/net/wlan0/statistics/rx_bytes ] && WIFI_RX=${'$'}(cat /sys/class/net/wlan0/statistics/rx_bytes)
            [ -r /sys/class/net/wlan0/statistics/tx_bytes ] && WIFI_TX=${'$'}(cat /sys/class/net/wlan0/statistics/tx_bytes)
            echo "${'$'}IFACE|${'$'}RX|${'$'}TX|${'$'}WIFI_RX|${'$'}WIFI_TX"
        fi
        echo __FREQ__
        for P in /sys/devices/system/cpu/cpu[0-9]*; do
            [ -d "${'$'}P" ] || continue
            N=${'$'}{P##*/}
            C=0; M=0
            [ -r "${'$'}P/cpufreq/scaling_cur_freq" ] && C=${'$'}(cat "${'$'}P/cpufreq/scaling_cur_freq")
            [ -r "${'$'}P/cpufreq/cpuinfo_max_freq" ] && M=${'$'}(cat "${'$'}P/cpufreq/cpuinfo_max_freq")
            echo "${'$'}N|${'$'}C|${'$'}M"
        done
        echo __THERMAL__
        for Z in /sys/class/thermal/thermal_zone*; do
            [ -r "${'$'}Z/type" ] && [ -r "${'$'}Z/temp" ] || continue
            echo "${'$'}(cat "${'$'}Z/type")|${'$'}(cat "${'$'}Z/temp")"
        done
        echo __BATTERY__
        CURRENT=-1
        VOLTAGE=-1
        [ -r /sys/class/power_supply/battery/current_now ] && CURRENT=${'$'}(cat /sys/class/power_supply/battery/current_now)
        [ -r /sys/class/power_supply/battery/voltage_now ] && VOLTAGE=${'$'}(cat /sys/class/power_supply/battery/voltage_now)
        echo "${'$'}CURRENT|${'$'}VOLTAGE"
    """.trimIndent()

    val output = U20PlatformAdapter.runRootCommand(context, command, 10_000).getOrThrow()
    val sections = splitSections(output)
    val firstStats = parseCpuStats(sections["STAT1"].orEmpty())
    val secondStats = parseCpuStats(sections["STAT2"].orEmpty())
    val usageJson = JSONObject()
    var overallUsage = 0.0
    firstStats.forEach { (name, first) ->
        val second = secondStats[name] ?: return@forEach
        val totalDiff = second.first - first.first
        val idleDiff = second.second - first.second
        val usage = if (totalDiff > 0) {
            ((totalDiff - idleDiff) * 100.0 / totalDiff).coerceIn(0.0, 100.0)
        } else 0.0
        val rounded = String.format(Locale.US, "%.1f", usage).toDouble()
        usageJson.put(name, rounded)
        if (name == "cpu") overallUsage = rounded
    }

    val memValues = sections["MEM"].orEmpty().lineSequence().mapNotNull { line ->
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 2) null else parts[0].removeSuffix(":") to (parts[1].toLongOrNull() ?: 0L)
    }.toMap()
    val memTotal = memValues["MemTotal"] ?: 0L
    val memAvailable = memValues["MemAvailable"] ?: 0L
    val memUsed = (memTotal - memAvailable).coerceAtLeast(0L)
    val memUsage = if (memTotal > 0) memUsed * 100.0 / memTotal else 0.0
    val swapTotal = memValues["SwapTotal"] ?: 0L
    val swapFree = memValues["SwapFree"] ?: 0L
    val swapUsed = (swapTotal - swapFree).coerceAtLeast(0L)
    val memJson = JSONObject()
        .put("mem_total_kb", memTotal)
        .put("mem_available_kb", memAvailable)
        .put("mem_used_kb", memUsed)
        .put("mem_usage_percent", String.format(Locale.US, "%.1f", memUsage).toDouble())
        .put("swap_total_kb", swapTotal)
        .put("swap_free_kb", swapFree)
        .put("swap_used_kb", swapUsed)
        .put(
            "swap_usage_percent",
            if (swapTotal > 0) String.format(Locale.US, "%.1f", swapUsed * 100.0 / swapTotal).toDouble() else 0.0
        )

    val frequencyJson = JSONObject()
    sections["FREQ"].orEmpty().lineSequence().forEach { line ->
        val parts = line.trim().split('|')
        if (parts.size == 3 && parts[0].matches(Regex("cpu\\d+"))) {
            frequencyJson.put(
                parts[0],
                JSONObject()
                    .put("cur", (parts[1].toLongOrNull() ?: 0L) / 1000L)
                    .put("max", (parts[2].toLongOrNull() ?: 0L) / 1000L)
            )
        }
    }

    val thermalJson = JSONArray()
    var maxTemperature = -1
    val excluded = listOf("chg", "front", "frame", "wcn", "usb", "bcl", "interface", "skin", "back")
    sections["THERMAL"].orEmpty().lineSequence().forEach { line ->
        val separator = line.lastIndexOf('|')
        if (separator <= 0) return@forEach
        val type = line.substring(0, separator).trim()
        val temperature = line.substring(separator + 1).trim().toIntOrNull() ?: return@forEach
        if (type.isBlank() || excluded.any { type.contains(it, ignoreCase = true) }) return@forEach
        if (temperature !in 0..124_000) return@forEach
        thermalJson.put(JSONObject().put("type", type).put("temp", temperature))
        if (temperature > maxTemperature) maxTemperature = temperature
    }

    val sampledAtMs = SystemClock.elapsedRealtime()
    val networkParts = sections["NET"].orEmpty().lineSequence()
        .map { it.trim().split('|') }
        .firstOrNull { parts -> parts.size >= 5 && parts[0].isNotBlank() }
    val networkInterface = networkParts?.getOrNull(0).orEmpty()
    val networkRxBytes = networkParts?.getOrNull(1)?.toLongOrNull() ?: 0L
    val networkTxBytes = networkParts?.getOrNull(2)?.toLongOrNull() ?: 0L
    val networkRate = calculateNetworkRate(
        U20NetworkSample(networkInterface, networkRxBytes, networkTxBytes, sampledAtMs)
    )
    val wifiRate = calculateNetworkRate(
        U20NetworkSample(
            interfaceName = "wlan0",
            rxBytes = networkParts?.getOrNull(3)?.toLongOrNull() ?: 0L,
            txBytes = networkParts?.getOrNull(4)?.toLongOrNull() ?: 0L,
            sampledAtMs = sampledAtMs
        )
    )
    val batteryParts = sections["BATTERY"].orEmpty().lineSequence()
        .map { it.trim().split('|') }
        .firstOrNull { it.size == 2 }
    val batteryCurrentUa = batteryParts?.getOrNull(0)?.toIntOrNull() ?: -1
    val batteryVoltageUv = batteryParts?.getOrNull(1)?.toIntOrNull() ?: -1

    return U20SystemStats(
        cpuUsage = overallUsage,
        memoryUsage = String.format(Locale.US, "%.1f", memUsage).toDouble(),
        cpuUsageInfo = usageJson,
        memoryInfo = memJson,
        cpuFrequencyInfo = frequencyJson,
        thermalZones = thermalJson,
        maxTemperature = maxTemperature,
        networkInterface = networkInterface,
        networkRxBytes = networkRxBytes,
        networkTxBytes = networkTxBytes,
        networkRxBytesPerSecond = networkRate.rxBytesPerSecond,
        networkTxBytesPerSecond = networkRate.txBytesPerSecond,
        wifiRxBytesPerSecond = wifiRate.rxBytesPerSecond,
        wifiTxBytesPerSecond = wifiRate.txBytesPerSecond,
        batteryCurrentUa = batteryCurrentUa,
        batteryVoltageUv = batteryVoltageUv,
        networkSampleTime = sampledAtMs
    )
}

private fun splitSections(output: String): Map<String, String> {
    val result = linkedMapOf<String, StringBuilder>()
    var current: String? = null
    output.lineSequence().forEach { line ->
        val marker = Regex("^__(STAT1|STAT2|MEM|NET|FREQ|THERMAL|BATTERY)__$").matchEntire(line.trim())
        if (marker != null) {
            current = marker.groupValues[1]
            result.getOrPut(current!!) { StringBuilder() }
        } else {
            current?.let { result.getOrPut(it) { StringBuilder() }.appendLine(line) }
        }
    }
    return result.mapValues { it.value.toString() }
}

private fun parseCpuStats(text: String): Map<String, Pair<Long, Long>> {
    val result = linkedMapOf<String, Pair<Long, Long>>()
    text.lineSequence().filter { it.startsWith("cpu") }.forEach { line ->
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 5 || !parts[0].matches(Regex("cpu\\d*"))) return@forEach
        val values = parts.drop(1).map { it.toLongOrNull() ?: 0L }
        val total = values.sum()
        val idle = (values.getOrNull(3) ?: 0L) + (values.getOrNull(4) ?: 0L)
        result[parts[0]] = total to idle
    }
    return result
}
