package com.aistudio.jarvis.voiceagent.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

class MessagingTool : JarvisTool {
    override val id: String = "messaging"
    override val name: String = "Send Message"
    override val description: String = "Opens WhatsApp directly to a contact with the message pre-filled, ready to send."
    override val category: String = "Communication"
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM
    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.READ_CONTACTS
    )
    override val examplePhrases: List<String> = listOf(
        "WhatsApp Rahul I'll reach at 6 PM",
        "Tell Mom I am on my way",
        "Message Rahul saying I will be late",
        "Send WhatsApp to Dad I'm coming home"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val recipient = (params["recipient"] as? String
            ?: params["contactName"] as? String
            ?: "Contact").trim()
        val message = (params["message"] as? String
            ?: params["text"] as? String
            ?: "").trim()

        if (message.isBlank()) {
            return ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "What message would you like to send to $recipient?",
                displayMessage = "Message content was empty."
            )
        }

        // Check if WhatsApp is installed
        val pm = context.packageManager
        val whatsAppPackage = "com.whatsapp"
        val isWhatsAppInstalled = try {
            pm.getPackageInfo(whatsAppPackage, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) { false }

        if (isWhatsAppInstalled) {
            // Step 1: Try to look up phone number from contacts
            val phoneNumber = lookupContactNumber(context, recipient)

            if (!phoneNumber.isNullOrBlank()) {
                // Step 2: Open WhatsApp directly to this contact with message pre-filled
                // Strip non-digit chars and add country code if missing
                val cleanNumber = cleanPhoneNumber(phoneNumber)
                val waDirectIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}")
                    setPackage(whatsAppPackage)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                return try {
                    context.startActivity(waDirectIntent)
                    ToolExecutionResult(
                        isSuccess = true,
                        spokenMessage = "Opening WhatsApp to message $recipient. Just tap send.",
                        displayMessage = "WhatsApp opened to $recipient ($cleanNumber)\nMessage: \"$message\"",
                        actionIntent = waDirectIntent,
                        payload = mapOf("recipient" to recipient, "message" to message, "number" to cleanNumber)
                    )
                } catch (e: Exception) {
                    openWhatsAppGeneral(context, message, recipient, whatsAppPackage)
                }
            } else {
                // No contact number found — open WhatsApp share sheet with message
                return openWhatsAppGeneral(context, message, recipient, whatsAppPackage)
            }
        }

        // WhatsApp not installed — fallback to SMS
        return sendSms(context, recipient, message)
    }

    /**
     * Opens WhatsApp share screen with message pre-filled.
     * User must manually select the contact.
     */
    private fun openWhatsAppGeneral(
        context: Context,
        message: String,
        recipient: String,
        whatsAppPackage: String
    ): ToolExecutionResult {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage(whatsAppPackage)
            putExtra(Intent.EXTRA_TEXT, message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(shareIntent)
            ToolExecutionResult(
                isSuccess = true,
                spokenMessage = "WhatsApp opened. Please select $recipient from your contacts and tap send.",
                displayMessage = "WhatsApp opened — select $recipient and tap Send\nMessage: \"$message\"",
                actionIntent = shareIntent,
                payload = mapOf("recipient" to recipient, "message" to message)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "Could not open WhatsApp. Please open it manually.",
                displayMessage = "WhatsApp could not be launched: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Fallback: SMS if WhatsApp not installed.
     */
    private fun sendSms(context: Context, recipient: String, message: String): ToolExecutionResult {
        val phoneNumber = lookupContactNumber(context, recipient)
        val target = if (!phoneNumber.isNullOrBlank()) phoneNumber else recipient
        val smsUri = Uri.parse("smsto:${Uri.encode(target)}")
        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(smsIntent)
            ToolExecutionResult(
                isSuccess = true,
                spokenMessage = "SMS prepared for $recipient. Tap send.",
                displayMessage = "SMS to $recipient: \"$message\"",
                actionIntent = smsIntent,
                payload = mapOf("recipient" to recipient, "message" to message)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "Could not open messaging app.",
                displayMessage = "Failed to launch SMS: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Looks up a contact's phone number by name from the device contacts.
     */
    private fun lookupContactNumber(context: Context, name: String): String? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null

        return try {
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
                    if (numberIndex >= 0) it.getString(numberIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Cleans phone number: removes spaces/dashes/brackets.
     * Adds India country code +91 if no country code present.
     */
    private fun cleanPhoneNumber(number: String): String {
        val digits = number.replace(Regex("[^+\\d]"), "")
        return if (digits.startsWith("+")) {
            digits
        } else if (digits.startsWith("91") && digits.length >= 12) {
            "+$digits"
        } else {
            "+91$digits" // Default: India country code
        }
    }
}
