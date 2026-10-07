package com.minikano.f50_sms.modules.u20

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Minimal providers for framework components removed from the MN901/U20 image.
 *
 * The bundled dialer registers observers for voicemail, contacts and call-log
 * authorities during startup. The firmware omits ContactsProvider completely,
 * so merely opening the dialer throws before it can show the keypad. These
 * providers intentionally expose empty datasets; they only restore the Android
 * contracts expected by the dialer and do not invent contact or call history.
 */
abstract class U20EmptyTelephonyProvider : ContentProvider() {
    protected abstract val fallbackColumns: Array<String>

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor = MatrixCursor(
        projection?.map(String::toString)?.toTypedArray() ?: fallbackColumns,
        0
    ).apply {
        setNotificationUri(context?.contentResolver, uri)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}

class U20VoicemailCompatProvider : U20EmptyTelephonyProvider() {
    override val fallbackColumns = arrayOf(
        "_id",
        "source_package",
        "phone_account_component_name",
        "phone_account_id",
        "configuration_state",
        "data_channel_state",
        "notification_channel_state"
    )

    override fun getType(uri: Uri): String = when {
        uri.pathSegments.firstOrNull() == "status" ->
            "vnd.android.cursor.dir/voicemail.source.status"
        uri.lastPathSegment?.toLongOrNull() != null ->
            "vnd.android.cursor.item/voicemail"
        else -> "vnd.android.cursor.dir/voicemails"
    }
}

class U20ContactsCompatProvider : U20EmptyTelephonyProvider() {
    override val fallbackColumns = arrayOf("_id", "display_name", "lookup")

    override fun getType(uri: Uri): String =
        "vnd.android.cursor.dir/contact"
}

class U20CallLogCompatProvider : U20EmptyTelephonyProvider() {
    override val fallbackColumns = arrayOf(
        "_id",
        "number",
        "date",
        "duration",
        "type",
        "new",
        "is_read"
    )

    override fun getType(uri: Uri): String =
        "vnd.android.cursor.dir/calls"
}
