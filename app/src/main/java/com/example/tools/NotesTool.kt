package com.aistudio.jarvis.voiceagent.tools

import android.content.Context
import com.aistudio.jarvis.voiceagent.data.db.AppDatabase
import com.aistudio.jarvis.voiceagent.data.db.NoteEntity

class NotesTool : JarvisTool {
    override val id: String = "notes"
    override val name: String = "Notes Vault"
    override val description: String = "Takes notes and retrieves saved notes stored securely on your device."
    override val category: String = "Productivity"
    override val riskLevel: RiskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val examplePhrases: List<String> = listOf(
        "Take a note: buy a new charger",
        "Take a note that my project deadline is Friday",
        "What did I ask you to remember?",
        "Show my notes"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val action = (params["action"] as? String ?: "create").lowercase()
        val content = (params["content"] as? String ?: params["note"] as? String ?: "").trim()
        val title = (params["title"] as? String ?: if (content.length > 25) content.take(25) + "..." else content).ifBlank { "Note" }

        val db = AppDatabase.getDatabase(context)
        val dao = db.jarvisDao()

        return when (action) {
            "retrieve", "search", "list" -> {
                val query = (params["query"] as? String ?: "").trim()
                val notes = if (query.isBlank()) {
                    dao.searchNotes("")
                } else {
                    dao.searchNotes(query)
                }

                if (notes.isEmpty()) {
                    ToolExecutionResult(
                        isSuccess = true,
                        spokenMessage = "You don't have any notes saved yet.",
                        displayMessage = "No notes found.",
                        payload = emptyList<NoteEntity>()
                    )
                } else {
                    val summary = notes.take(3).joinToString("; ") { "${it.title}: ${it.content}" }
                    ToolExecutionResult(
                        isSuccess = true,
                        spokenMessage = "You have ${notes.size} note${if (notes.size > 1) "s" else ""}. Latest: $summary",
                        displayMessage = "Found ${notes.size} note(s)",
                        payload = notes
                    )
                }
            }
            else -> {
                // Create note
                if (content.isBlank()) {
                    return ToolExecutionResult(
                        isSuccess = false,
                        spokenMessage = "What note would you like me to take?",
                        displayMessage = "Note content was empty."
                    )
                }

                val note = NoteEntity(title = title, content = content)
                dao.insertNote(note)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Note saved.",
                    displayMessage = "Note saved: \"$content\"",
                    payload = note
                )
            }
        }
    }
}
