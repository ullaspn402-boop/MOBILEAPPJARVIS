package com.aistudio.jarvis.voiceagent.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

class CallContactTool : JarvisTool {
    override val id: String = "call_contact"
    override val name: String = "Place Phone Call"
    override val description: String = "Initiates a phone call or opens the phone dialer with a contact's number."
    override val category: String = "Communication"
    override val riskLevel: RiskLevel = RiskLevel.HIGH
    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS
    )
    override val examplePhrases: List<String> = listOf(
        "Call Mom",
        "Call Rahul",
        "Call 9876543210",
        "Open the phone app"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val target = (params["contactName"] as? String ?: params["phoneNumber"] as? String ?: params["target"] as? String ?: "").trim()
        val directCall = params["directCall"] as? Boolean ?: true

        if (target.isBlank() || target.equals("phone", ignoreCase = true)) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
            return ToolExecutionResult(
                isSuccess = true,
                spokenMessage = "Opening the phone dialer.",
                displayMessage = "Opened Phone Dialer",
                actionIntent = dialIntent
            )
        }

        // Check if target is a raw phone number
        val isNumeric = target.matches(Regex("^[+0-9\\s\\-\\(\\)]{3,20}$"))
        var resolvedNumber = if (isNumeric) target.replace(" ", "") else null

        // Try looking up contact from contacts provider if target is a name
        if (resolvedNumber == null) {
            resolvedNumber = lookupContactPhoneNumber(context, target)
        }

        if (resolvedNumber.isNullOrBlank()) {
            // Couldn't resolve phone number, fallback to dialer with query
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(target)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return try {
                context.startActivity(dialIntent)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Opening dialer for $target.",
                    displayMessage = "Dialing $target",
                    actionIntent = dialIntent
                )
            } catch (e: Exception) {
                ToolExecutionResult(
                    isSuccess = false,
                    spokenMessage = "Could not find a phone number for $target.",
                    displayMessage = "No contact found for '$target'"
                )
            }
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return if (directCall && hasCallPermission) {
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(resolvedNumber)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(callIntent)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Calling $target.",
                    displayMessage = "Calling $target ($resolvedNumber)",
                    actionIntent = callIntent,
                    payload = mapOf("contact" to target, "number" to resolvedNumber)
                )
            } catch (e: Exception) {
                fallbackToDialer(context, resolvedNumber, target)
            }
        } else {
            fallbackToDialer(context, resolvedNumber, target)
        }
    }

    private fun fallbackToDialer(context: Context, number: String, contactName: String): ToolExecutionResult {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(dialIntent)
            ToolExecutionResult(
                isSuccess = true,
                spokenMessage = "Opening dialer for $contactName.",
                displayMessage = "Prepared call to $contactName ($number)",
                actionIntent = dialIntent,
                payload = mapOf("contact" to contactName, "number" to number)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "Unable to start phone dialer.",
                displayMessage = "Failed to launch phone dialer: ${e.localizedMessage}"
            )
        }
    }

    private fun lookupContactPhoneNumber(context: Context, name: String): String? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIndex >= 0) {
                        return it.getString(numberIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore contact lookup error
        }
        return null
    }
}
