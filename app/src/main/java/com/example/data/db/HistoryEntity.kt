package com.aistudio.jarvis.voiceagent.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_entries")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userQuery: String,
    val responseText: String,
    val status: String = "COMPLETED", // COMPLETED, FAILED, CANCELLED
    val toolUsed: String? = null,
    val executionDetails: String? = null
)
