package com.aistudio.jarvis.voiceagent.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_entries")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "PREFERENCE", // PROFILE, ROUTINE, SAVED_LOCATION, PREFERENCE, IMPORTANT_FACT
    val updatedAt: Long = System.currentTimeMillis()
)
