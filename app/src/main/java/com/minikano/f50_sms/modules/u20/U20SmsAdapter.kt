package com.minikano.f50_sms.modules.u20

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Base64
import com.minikano.f50_sms.utils.KanoUtils
import com.minikano.f50_sms.utils.U20PlatformAdapter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** SMS compatibility for the native MN901 firmware and its SIM storage. */
internal object U20SmsAdapter {
    private const val PROVIDER_ID_OFFSET = 1_000_000L
    private val smsUri: Uri = Uri.parse("content://sms")
    private val idPattern = Regex("^[0-9]+$")
    private val requestCode = AtomicInteger(20_000)

    private data class StoredMessage(
        val ids: List<Long>,
        val number: String,
        val body: String,
        val timestamp: Long,
        val tag: String,
        val store: String,
        val concatRef: Int? = null,
        val concatPart: Int? = null,
        val concatTotal: Int? = null
    )

    private data class ConcatInfo(val reference: Int, val total: Int, val part: Int)

    suspend fun list(context: Context, page: Int, pageSize: Int): JSONObject =
        withContext(Dispatchers.IO) {
            grantPermissions(context).getOrThrow()
            val safePage = page.coerceAtLeast(0)
            val safeSize = pageSize.coerceIn(1, 500)
            val simMessages = mergeMultipartMessages(readSimMessages(context))
            val providerMessages = readProviderMessages(context)
            // Android's Messaging app assembles multipart SIM records into
            // complete conversations. Prefer that view after a full import;
            // otherwise merge the direct SIM records with device-side sent
            // and failed messages so neither source disappears.
            val providerHasImportedSim = providerMessages.size >= (simMessages.size / 2).coerceAtLeast(1)
            val allMessages = if (providerHasImportedSim) {
                providerMessages
            } else {
                simMessages + providerMessages
            }.sortedByDescending { it.timestamp }
            val offset = (safePage * safeSize).coerceAtMost(allMessages.size)
            val messages = JSONArray()
            allMessages.drop(offset).take(safeSize).forEach { message ->
                messages.put(
                    JSONObject()
                        .put("id", message.ids.joinToString(","))
                        .put("number", message.number)
                        .put(
                            "content",
                            Base64.encodeToString(message.body.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                        )
                        .put("date", formatDate(message.timestamp))
                        .put("tag", message.tag)
                        .put("store", message.store)
                        .put("parts", message.ids.size)
                        .put("expected_parts", message.concatTotal ?: message.ids.size)
                )
            }
            JSONObject()
                .put("sms_data_total", allMessages.size.toString())
                .put("sim_sms_total", simMessages.size)
                .put("device_sms_total", providerMessages.size)
                .put(
                    "active_source",
                    when {
                        providerHasImportedSim -> "ME"
                        providerMessages.isNotEmpty() -> "SM+ME"
                        else -> "SM"
                    }
                )
                .put("messages", messages)
        }

    fun unreadCount(context: Context): Int = runCatching {
        context.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("_id"),
            "read=0",
            null,
            null
        )?.use { it.count } ?: 0
    }.getOrDefault(0)

    suspend fun status(context: Context): JSONObject = withContext(Dispatchers.IO) {
        grantPermissions(context).getOrThrow()
        val subscription = activeSubscription(context)
        val manager = subscription?.subscriptionId?.let(SmsManager::getSmsManagerForSubscriptionId)
            ?: SmsManager.getDefault()
        val storage = parseStorage(runAt(context, "AT+CPMS?"))
        val smsc = parseSmsc(runAt(context, "AT+CSCA?"))
        val csRegistration = parseRegistration(runAt(context, "AT+CREG?"))
        val epsRegistration = parseRegistration(runAt(context, "AT+CEREG?"))
        val telephony = (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
            .let { base -> subscription?.subscriptionId?.let(base::createForSubscriptionId) ?: base }

        JSONObject()
            .put("smsc", smsc)
            .put("subscription_id", subscription?.subscriptionId ?: -1)
            .put("sim_slot", subscription?.simSlotIndex ?: -1)
            .put("carrier", subscription?.carrierName?.toString().orEmpty())
            .put("sim_ready", subscription != null)
            .put("icc_capacity", runCatching { manager.smsCapacityOnIcc }.getOrDefault(0))
            .put("read_store", storage.getOrNull(0)?.first ?: "--")
            .put("read_used", storage.getOrNull(0)?.second ?: 0)
            .put("read_total", storage.getOrNull(0)?.third ?: 0)
            .put("write_store", storage.getOrNull(1)?.first ?: "--")
            .put("write_used", storage.getOrNull(1)?.second ?: 0)
            .put("write_total", storage.getOrNull(1)?.third ?: 0)
            .put("receive_store", storage.getOrNull(2)?.first ?: "--")
            .put("receive_used", storage.getOrNull(2)?.second ?: 0)
            .put("receive_total", storage.getOrNull(2)?.third ?: 0)
            .put("cs_registration", csRegistration)
            .put("eps_registration", epsRegistration)
            .put("voice_registered", csRegistration == 1 || csRegistration == 5)
            .put("voice_capable", runCatching { telephony.isVoiceCapable }.getOrDefault(false))
            .put("call_state", runCatching { telephony.callState }.getOrDefault(0))
    }

    suspend fun updateSettings(
        context: Context,
        smsc: String?,
        readStore: String?,
        writeStore: String?,
        receiveStore: String?
    ): JSONObject = withContext(Dispatchers.IO) {
        if (!smsc.isNullOrBlank()) {
            val normalized = smsc.trim().replace(" ", "")
            require(normalized.matches(Regex("^\\+?[0-9]{3,24}$"))) { "短信中心号码格式不正确" }
            val encoded = normalized.toByteArray(StandardCharsets.US_ASCII)
                .joinToString("") { "%02X".format(it) }
            val type = if (normalized.startsWith('+')) 145 else 129
            runAtChecked(context, "AT+CSCA=\"$encoded\",$type")
        }

        if (readStore != null || writeStore != null || receiveStore != null) {
            val current = parseStorage(runAt(context, "AT+CPMS?"))
            val read = validateStore(readStore ?: current.getOrNull(0)?.first ?: "SM")
            val write = validateStore(writeStore ?: current.getOrNull(1)?.first ?: read)
            val receive = validateStore(receiveStore ?: current.getOrNull(2)?.first ?: read)
            runAtChecked(context, "AT+CPMS=\"$read\",\"$write\",\"$receive\"")
        }
        status(context)
    }

    @Suppress("DEPRECATION")
    suspend fun send(context: Context, number: String, encodedBody: String): Result<JSONObject> =
        runCatching {
            require(number.matches(Regex("^\\+?[0-9]{3,32}$"))) { "手机号码格式不正确" }
            grantPermissions(context).getOrThrow()

            val body = decodeUfiBody(encodedBody)
            require(body.isNotBlank()) { "短信内容不能为空" }

            val activeSubId = activeSubscription(context)?.subscriptionId
            val manager = activeSubId?.let(SmsManager::getSmsManagerForSubscriptionId)
                ?: SmsManager.getDefault()
            val parts = manager.divideMessage(body)
            val action = "${context.packageName}.U20_SMS_SENT.${System.nanoTime()}"
            val results = ConcurrentHashMap<Int, Int>()
            val completion = CompletableDeferred<Map<Int, Int>>()
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context?, intent: Intent?) {
                    val part = intent?.getIntExtra("part", -1) ?: -1
                    if (part >= 0) results[part] = resultCode
                    if (results.size >= parts.size && !completion.isCompleted) {
                        completion.complete(results.toMap())
                    }
                }
            }
            val filter = IntentFilter(action)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }

            try {
                val sentIntents = ArrayList<PendingIntent>(parts.size)
                parts.indices.forEach { part ->
                    val intent = Intent(action)
                        .setPackage(context.packageName)
                        .putExtra("part", part)
                    sentIntents += PendingIntent.getBroadcast(
                        context,
                        requestCode.incrementAndGet(),
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                if (parts.size == 1) {
                    manager.sendTextMessage(number, null, parts.first(), sentIntents.first(), null)
                } else {
                    manager.sendMultipartTextMessage(number, null, parts, sentIntents, null)
                }

                val sendResults = withTimeout(60_000) { completion.await() }
                val failure = sendResults.entries.firstOrNull { it.value != Activity.RESULT_OK }
                if (failure != null) {
                    throw IllegalStateException(describeSendFailure(failure.value))
                }
                JSONObject()
                    .put("result", "success")
                    .put("message", "基带已接受短信")
                    .put("parts", parts.size)
                    .put("subscription_id", activeSubId ?: -1)
            } finally {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }

    fun delete(context: Context, ids: String): Result<Unit> = runCatching {
        parseIds(ids).forEach { rawId ->
            val id = rawId.toLong()
            if (id >= PROVIDER_ID_OFFSET) {
                U20PlatformAdapter.runRootCommand(
                    context,
                    "content delete --uri content://sms/${id - PROVIDER_ID_OFFSET}"
                ).getOrThrow()
            } else {
                runAtChecked(context, "AT+CMGD=$id")
            }
        }
    }

    fun markRead(context: Context, ids: String): Result<Unit> = runCatching {
        parseIds(ids).forEach { rawId ->
            val id = rawId.toLong()
            if (id >= PROVIDER_ID_OFFSET) {
                U20PlatformAdapter.runRootCommand(
                    context,
                    "content update --uri content://sms/${id - PROVIDER_ID_OFFSET} --bind read:i:1"
                ).getOrThrow()
            } else {
                runAtChecked(context, "AT+CMGR=$id")
            }
        }
    }

    fun grantPermissions(context: Context): Result<String> {
        val pkg = context.packageName
        val command = listOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.READ_PHONE_STATE"
        ).joinToString("; ") { "pm grant $pkg $it >/dev/null 2>&1 || true" }
        return U20PlatformAdapter.runRootCommand(context, command)
    }

    private fun activeSubscription(context: Context) = runCatching {
        SubscriptionManager.from(context).activeSubscriptionInfoList
            ?.firstOrNull { it.simSlotIndex >= 0 }
    }.getOrNull()

    private fun readSimMessages(context: Context): List<StoredMessage> = runCatching {
        val raw = runAt(context, "AT+CMGL=4")
        val header = Regex("""\+CMGL:\s*(\d+)\s*,\s*(\d+)\s*,\s*,""")
        val matches = header.findAll(raw).toList()
        buildList {
            matches.forEachIndexed { index, match ->
                val segmentEnd = matches.getOrNull(index + 1)?.range?.first
                    ?: raw.indexOf("OK", match.range.last + 1).takeIf { it >= 0 }
                    ?: raw.length
                val segment = raw.substring(match.range.last + 1, segmentEnd).trim()
                val pdu = extractPdu(segment) ?: return@forEachIndexed
                val pduBytes = hexToBytes(pdu)
                val sms = SmsMessage.createFromPdu(pduBytes, SmsMessage.FORMAT_3GPP)
                    ?: SmsMessage.createFromPdu(pduBytes)
                    ?: return@forEachIndexed
                val status = match.groupValues[2].toIntOrNull() ?: sms.statusOnIcc
                val concat = parseConcatInfo(pduBytes)
                add(
                    StoredMessage(
                        ids = listOf(match.groupValues[1].toLong()),
                        number = sms.displayOriginatingAddress.orEmpty().ifBlank { "SIM 短信" },
                        body = sms.displayMessageBody.orEmpty(),
                        timestamp = sms.timestampMillis.takeIf { it > 0 } ?: match.groupValues[1].toLong(),
                        tag = when (status) {
                            SmsManager.STATUS_ON_ICC_UNREAD -> "1"
                            SmsManager.STATUS_ON_ICC_UNSENT -> "3"
                            SmsManager.STATUS_ON_ICC_SENT -> "2"
                            else -> "0"
                        },
                        store = "SIM",
                        concatRef = concat?.reference,
                        concatPart = concat?.part,
                        concatTotal = concat?.total
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun readProviderMessages(context: Context): List<StoredMessage> = runCatching {
        val messages = mutableListOf<StoredMessage>()
        context.contentResolver.query(
            smsUri,
            arrayOf("_id", "address", "body", "date", "type", "read", "status"),
            null,
            null,
            "date DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow("type"))
                val read = cursor.getInt(cursor.getColumnIndexOrThrow("read"))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow("status"))
                messages += StoredMessage(
                    ids = listOf(PROVIDER_ID_OFFSET + id),
                    number = cursor.getString(cursor.getColumnIndexOrThrow("address")).orEmpty(),
                    body = cursor.getString(cursor.getColumnIndexOrThrow("body")).orEmpty(),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date")),
                    tag = toUfiTag(type, read, status),
                    store = "设备"
                )
            }
        }
        messages
    }.getOrDefault(emptyList())

    private fun mergeMultipartMessages(messages: List<StoredMessage>): List<StoredMessage> {
        val standalone = messages.filter { it.concatRef == null || it.concatPart == null || it.concatTotal == null }
        val groups = messages
            .filter { it.concatRef != null && it.concatPart != null && it.concatTotal != null }
            .groupBy { message ->
                val normalizedNumber = message.number.filter(Char::isDigit).takeLast(11)
                val direction = if (message.tag == "2" || message.tag == "3") "out" else "in"
                val timeWindow = message.timestamp / (30 * 60 * 1000L)
                "$normalizedNumber|${message.concatRef}|${message.concatTotal}|$direction|$timeWindow"
            }

        val merged = groups.values.map { parts ->
            val ordered = parts
                .sortedBy { it.concatPart }
                .distinctBy { it.concatPart }
            val outgoing = ordered.any { it.tag == "2" || it.tag == "3" }
            val mergedTag = when {
                ordered.any { it.tag == "3" } -> "3"
                outgoing -> "2"
                ordered.any { it.tag == "1" } -> "1"
                else -> "0"
            }
            ordered.first().copy(
                ids = ordered.flatMap { it.ids },
                body = ordered.joinToString("") { it.body },
                timestamp = ordered.maxOf { it.timestamp },
                tag = mergedTag,
                concatPart = null
            )
        }
        return standalone + merged
    }

    private fun parseConcatInfo(pdu: ByteArray): ConcatInfo? {
        fun unsigned(index: Int): Int? = pdu.getOrNull(index)?.toInt()?.and(0xff)

        val smscLength = unsigned(0) ?: return null
        var cursor = 1 + smscLength
        val firstOctet = unsigned(cursor) ?: return null
        if (firstOctet and 0x40 == 0) return null
        cursor += 1

        when (firstOctet and 0x03) {
            0 -> {
                val addressDigits = unsigned(cursor) ?: return null
                cursor += 2 + (addressDigits + 1) / 2
                cursor += 1 // PID
                cursor += 1 // DCS
                cursor += 7 // service centre timestamp
            }
            1 -> {
                cursor += 1 // message reference
                val addressDigits = unsigned(cursor) ?: return null
                cursor += 2 + (addressDigits + 1) / 2
                cursor += 1 // PID
                cursor += 1 // DCS
                cursor += when ((firstOctet shr 3) and 0x03) {
                    2 -> 1 // relative validity period
                    1, 3 -> 7 // enhanced or absolute validity period
                    else -> 0
                }
            }
            else -> return null
        }

        cursor += 1 // user data length
        val userDataHeaderLength = unsigned(cursor) ?: return null
        val headerEnd = cursor + 1 + userDataHeaderLength
        cursor += 1
        if (headerEnd > pdu.size) return null

        while (cursor + 1 < headerEnd) {
            val identifier = unsigned(cursor) ?: return null
            val length = unsigned(cursor + 1) ?: return null
            val dataStart = cursor + 2
            val dataEnd = dataStart + length
            if (dataEnd > headerEnd) return null
            val info = when {
                identifier == 0x00 && length >= 3 -> ConcatInfo(
                    reference = unsigned(dataStart) ?: return null,
                    total = unsigned(dataStart + 1) ?: return null,
                    part = unsigned(dataStart + 2) ?: return null
                )
                identifier == 0x08 && length >= 4 -> ConcatInfo(
                    reference = ((unsigned(dataStart) ?: return null) shl 8) or
                        (unsigned(dataStart + 1) ?: return null),
                    total = unsigned(dataStart + 2) ?: return null,
                    part = unsigned(dataStart + 3) ?: return null
                )
                else -> null
            }
            if (info != null && info.total > 1 && info.part in 1..info.total) return info
            cursor = dataEnd
        }
        return null
    }

    private fun extractPdu(rawSegment: String): String? {
        val segment = rawSegment.trim().takeWhile { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
        for (lengthDigits in 1..minOf(4, segment.length - 2)) {
            val lengthText = segment.take(lengthDigits)
            if (!lengthText.all(Char::isDigit)) continue
            val tpduLength = lengthText.toIntOrNull() ?: continue
            val candidate = segment.drop(lengthDigits)
            if (candidate.length < 2 || !candidate.take(2).all(::isHex)) continue
            val smscLength = candidate.take(2).toIntOrNull(16) ?: continue
            val expectedLength = (1 + smscLength + tpduLength) * 2
            if (candidate.length == expectedLength && candidate.all(::isHex)) return candidate
        }
        return null
    }

    private fun parseStorage(raw: String): List<Triple<String, Int, Int>> =
        Regex("""\"(ME|SM)\"\s*,\s*(\d+)\s*,\s*(\d+)""")
            .findAll(raw)
            .map { match ->
                Triple(
                    match.groupValues[1],
                    match.groupValues[2].toIntOrNull() ?: 0,
                    match.groupValues[3].toIntOrNull() ?: 0
                )
            }
            .toList()

    private fun parseSmsc(raw: String): String {
        val encoded = Regex("""\+CSCA:\s*\"([^\"]+)\"""")
            .find(raw)?.groupValues?.getOrNull(1).orEmpty()
        if (encoded.isBlank()) return ""
        return if (encoded.length % 2 == 0 && encoded.all(::isHex)) {
            runCatching { String(hexToBytes(encoded), StandardCharsets.US_ASCII) }.getOrDefault(encoded)
        } else encoded
    }

    private fun parseRegistration(raw: String): Int =
        Regex("""\+C(?:E)?REG:\s*\d+\s*,\s*(\d+)""")
            .find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: -1

    private fun validateStore(value: String): String {
        val normalized = value.trim().uppercase(Locale.US)
        require(normalized == "SM" || normalized == "ME") { "短信存储只能选择 SIM 或设备" }
        return normalized
    }

    private fun runAt(context: Context, command: String): String = KanoUtils.runAT(context, command, 0)

    private fun runAtChecked(context: Context, command: String): String {
        val result = runAt(context, command)
        if (result.contains("ERROR", true) || !result.contains("OK", true)) {
            error("AT 命令执行失败：$result")
        }
        return result
    }

    private fun parseIds(raw: String): List<String> {
        val ids = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        require(ids.isNotEmpty() && ids.all(idPattern::matches)) { "短信 ID 格式不正确" }
        return ids
    }

    private fun decodeUfiBody(value: String): String {
        val trimmed = value.trim()
        if (trimmed.length % 2 == 0 && trimmed.matches(Regex("^[0-9a-fA-F]+$"))) {
            return String(hexToBytes(trimmed), StandardCharsets.UTF_16BE)
        }
        return trimmed
    }

    private fun hexToBytes(value: String): ByteArray =
        ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }

    private fun isHex(char: Char): Boolean = char.isDigit() || char.lowercaseChar() in 'a'..'f'

    private fun describeSendFailure(code: Int): String = when (code) {
        SmsManager.RESULT_ERROR_NO_SERVICE -> "短信发送失败：语音/短信网络未注册"
        SmsManager.RESULT_ERROR_RADIO_OFF -> "短信发送失败：基带未开启"
        SmsManager.RESULT_INVALID_SMSC_ADDRESS -> "短信发送失败：短信中心号码无效"
        SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE -> "短信发送失败：SIM 固定拨号限制"
        SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "短信发送失败：发送频率受限"
        SmsManager.RESULT_NETWORK_REJECT -> "短信发送失败：运营商网络拒绝"
        SmsManager.RESULT_MODEM_ERROR -> "短信发送失败：基带返回错误"
        SmsManager.RESULT_NETWORK_ERROR -> "短信发送失败：网络错误"
        SmsManager.RESULT_OPERATION_NOT_ALLOWED -> "短信发送失败：当前网络不允许"
        SmsManager.RESULT_RADIO_NOT_AVAILABLE -> "短信发送失败：无线服务不可用"
        SmsManager.RESULT_REQUEST_NOT_SUPPORTED -> "短信发送失败：固件不支持该请求"
        SmsManager.RESULT_NO_RESOURCES -> "短信发送失败：系统资源不足"
        SmsManager.RESULT_CANCELLED -> "短信发送已取消"
        else -> "短信发送失败：基带错误码 $code"
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("yyyy,M,d,H,m,s", Locale.US).format(Date(timestamp)) + ",0"

    private fun toUfiTag(type: Int, read: Int, status: Int): String = when {
        type == 5 || status > 0 -> "3"
        type == 2 || type == 4 || type == 6 -> "2"
        read == 0 -> "1"
        else -> "0"
    }
}
