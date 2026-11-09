package com.mobilefork.hermesagent.ui.calls

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** A person from the phone's address book, with the number to call. */
data class DeviceContact(val id: String, val name: String, val number: String)

fun hasContactsPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

/** One entry per contact, by name, using the number marked as primary when there is one. */
fun loadDeviceContacts(context: Context): List<DeviceContact> {
    if (!hasContactsPermission(context)) return emptyList()
    return runCatching {
        val chosen = LinkedHashMap<String, Pair<DeviceContact, Boolean>>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0).orEmpty()
                val name = cursor.getString(1).orEmpty().trim()
                val number = cursor.getString(2).orEmpty().trim()
                val primary = cursor.getInt(3) != 0
                if (id.isBlank() || name.isBlank() || number.isBlank()) continue
                val existing = chosen[id]
                if (existing == null || (primary && !existing.second)) chosen[id] = DeviceContact(id, name, number) to primary
            }
        }
        chosen.values.map { it.first }.sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())
}

/** What the person typed, reduced to what a phone can dial. */
fun dialableNumber(typed: String): String = typed.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }

/**
 * Places a call to a real person with the phone's own calling. With the call permission it dials at
 * once, as a phone's contacts do; without it the dialer opens with the number filled in. Either way the
 * phone's own call screen takes over, which is the real thing.
 */
fun placePhoneCall(context: Context, number: String): Boolean {
    val dialable = dialableNumber(number)
    if (dialable.isBlank()) return false
    val uri = Uri.fromParts("tel", dialable, null)
    val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    return runCatching {
        context.startActivity(Intent(if (allowed) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrElse {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }
}
