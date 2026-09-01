package com.aistudio.jarvis.voiceagent.data.call

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log

/**
 * Resolves an incoming phone number to a contact display name.
 *
 * Uses [ContactsContract] and [CallLog] — permissions READ_CONTACTS and READ_CALL_LOG.
 * Performs only a local lookup; never uploads data externally.
 */
class CallerIdentifier(private val context: Context) {

    private val tag = "CallerIdentifier"

    /**
     * Look up a phone number and return [CallerInfo].
     * Falls back gracefully if permission is missing or the number is not found.
     */
    fun identify(rawNumber: String): CallerInfo {
        // Step 1: If number is blank, try fetching the latest RINGING number from CallLog
        val numberToLookup = if (rawNumber.isBlank()) {
            getLatestIncomingNumberFromCallLog() ?: ""
        } else {
            rawNumber
        }

        if (numberToLookup.isBlank()) {
            return CallerInfo(
                number = "",
                displayName = "Unknown",
                isKnownContact = false
            )
        }

        return try {
            // Try 1: Direct lookup with original number
            val directMatch = lookupContact(numberToLookup)
            if (directMatch != null) return directMatch

            // Try 2: Strip country code → last 10 digits (handles +91XXXXXXXXXX → XXXXXXXXXX)
            val digitsOnly = numberToLookup.filter { it.isDigit() }
            val last10 = if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else digitsOnly

            if (last10 != digitsOnly) {
                val normalizedMatch = lookupContact(last10)
                if (normalizedMatch != null) return normalizedMatch.copy(number = numberToLookup)
            }

            // Try 3: With +91 prefix (Indian numbers stored as +91XXXXXXXXXX in contacts)
            if (!numberToLookup.startsWith("+") && digitsOnly.length == 10) {
                val withCountryCode = "+91$digitsOnly"
                val ccMatch = lookupContact(withCountryCode)
                if (ccMatch != null) return ccMatch.copy(number = numberToLookup)
            }

            // Try 4: With 0 prefix (some Indian contacts stored as 0XXXXXXXXXX)
            if (!numberToLookup.startsWith("0") && digitsOnly.length == 10) {
                val with0Prefix = "0$digitsOnly"
                val prefixMatch = lookupContact(with0Prefix)
                if (prefixMatch != null) return prefixMatch.copy(number = numberToLookup)
            }

            // No contact found — return formatted number
            CallerInfo(
                number = numberToLookup,
                displayName = formatNumber(numberToLookup),
                isKnownContact = false
            )
        } catch (e: SecurityException) {
            Log.w(tag, "READ_CONTACTS permission not granted — cannot identify caller")
            CallerInfo(
                number = numberToLookup,
                displayName = formatNumber(numberToLookup),
                isKnownContact = false
            )
        } catch (e: Throwable) {
            Log.e(tag, "Unexpected error identifying caller", e)
            CallerInfo(
                number = numberToLookup,
                displayName = formatNumber(numberToLookup),
                isKnownContact = false
            )
        }
    }

    private fun lookupContact(phoneNumber: String): CallerInfo? {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isBlank()) return null

        // Method 1: Standard PhoneLookup (without double Uri.encode)
        try {
            val uri: Uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                cleanNumber
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val thumbIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val thumb = if (thumbIdx >= 0) cursor.getString(thumbIdx) else null

                    if (!name.isNullOrBlank()) {
                        Log.i(tag, "✅ Contact found via PhoneLookup: '$name' for $cleanNumber")
                        return CallerInfo(
                            number = cleanNumber,
                            displayName = name,
                            isKnownContact = true,
                            thumbnailUri = thumb
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "PhoneLookup failed for $cleanNumber: ${e.message}")
        }

        // Method 2: Fallback query on CommonDataKinds.Phone using last 10 digits
        val digitsOnly = cleanNumber.filter { it.isDigit() }
        val last10 = if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else digitsOnly
        if (last10.length >= 7) {
            try {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                )
                val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
                val selectionArgs = arrayOf("%$last10%")

                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val thumbIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                        val thumb = if (thumbIdx >= 0) cursor.getString(thumbIdx) else null

                        if (!name.isNullOrBlank()) {
                            Log.i(tag, "✅ Contact found via CommonDataKinds.Phone LIKE query: '$name' for $cleanNumber")
                            return CallerInfo(
                                number = cleanNumber,
                                displayName = name,
                                isKnownContact = true,
                                thumbnailUri = thumb
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(tag, "CommonDataKinds.Phone query failed for $last10: ${e.message}")
            }
        }

        return null
    }

    /**
     * Fallback: query the system CallLog for the most recent number.
     * IMPORTANT: Only use a call log entry if it was logged within the last 30 seconds
     * to avoid returning a stale old call number when TelephonyCallback passes "".
     * Note: The current ringing call won't be in the log yet — this is a best-effort fallback.
     */
    private fun getLatestIncomingNumberFromCallLog(): String? {
        return try {
            val thirtySecondsAgo = System.currentTimeMillis() - 30_000L
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                "${CallLog.Calls.DATE} > ?",
                arrayOf(thirtySecondsAgo.toString()),
                "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val number = if (numIdx >= 0) it.getString(numIdx) else null
                    Log.d(tag, "CallLog fallback: found recent number ${number?.takeLast(4)?.padStart(number.length, '*')}")
                    number?.ifBlank { null }
                } else {
                    Log.d(tag, "CallLog fallback: no recent calls within 30s")
                    null
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "CallLog query failed: ${e.message}")
            null
        }
    }

    /** Formats a raw number for display when no contact name is found. */
    private fun formatNumber(number: String): String {
        return try {
            val digits = number.filter { it.isDigit() }
            when {
                digits.length == 10 -> "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
                digits.length == 11 && digits.startsWith("1") ->
                    "+1 ${digits.substring(1, 4)}-${digits.substring(4, 7)}-${digits.substring(7)}"
                else -> number
            }
        } catch (e: Throwable) {
            number
        }
    }
}

