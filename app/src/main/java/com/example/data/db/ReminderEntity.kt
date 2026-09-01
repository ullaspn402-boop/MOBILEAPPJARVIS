package com.aistudio.jarvis.voiceagent.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetTimeMillis: Long,
    val targetTimeString: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
