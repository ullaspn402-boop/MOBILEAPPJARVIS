package com.aistudio.jarvis.voiceagent.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.jarvis.voiceagent.data.call.CallSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
    // --- History Queries ---
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history_entries")
    suspend fun clearAllHistory()

    // --- Memory Queries ---
    @Query("SELECT * FROM memory_entries ORDER BY updatedAt DESC")
    fun getAllMemory(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory_entries ORDER BY updatedAt DESC")
    suspend fun getAllMemoryList(): List<MemoryEntity>

    @Query("SELECT * FROM memory_entries WHERE `key` = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): MemoryEntity?

    @Query("SELECT * FROM memory_entries WHERE `key` LIKE '%' || :query || '%' OR `value` LIKE '%' || :query || '%'")
    suspend fun searchMemory(query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMemory(memory: MemoryEntity): Long

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memory_entries WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memory_entries")
    suspend fun clearAllMemory()

    // --- Notes Queries ---
    @Query("SELECT * FROM notes ORDER BY pinned DESC, createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun searchNotes(query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()

    // --- Reminders Queries ---
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, targetTimeMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    // --- Call Summary Queries ---
    @Query("SELECT * FROM call_summaries ORDER BY timestampMs DESC")
    fun getAllCallSummaries(): Flow<List<CallSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallSummary(summary: CallSummaryEntity): Long

    @Query("DELETE FROM call_summaries WHERE id = :id")
    suspend fun deleteCallSummaryById(id: Long)

    @Query("DELETE FROM call_summaries")
    suspend fun clearAllCallSummaries()
}
