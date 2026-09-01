package com.aistudio.jarvis.voiceagent.data.repository

import com.aistudio.jarvis.voiceagent.data.call.CallSummaryEntity
import com.aistudio.jarvis.voiceagent.data.db.HistoryEntity
import com.aistudio.jarvis.voiceagent.data.db.JarvisDao
import com.aistudio.jarvis.voiceagent.data.db.MemoryEntity
import com.aistudio.jarvis.voiceagent.data.db.NoteEntity
import com.aistudio.jarvis.voiceagent.data.db.ReminderEntity
import kotlinx.coroutines.flow.Flow

class JarvisRepository(private val jarvisDao: JarvisDao) {

    val allHistory: Flow<List<HistoryEntity>> = jarvisDao.getAllHistory()
    val allMemory: Flow<List<MemoryEntity>> = jarvisDao.getAllMemory()
    val allNotes: Flow<List<NoteEntity>> = jarvisDao.getAllNotes()
    val allReminders: Flow<List<ReminderEntity>> = jarvisDao.getAllReminders()
    val allCallSummaries: Flow<List<CallSummaryEntity>> = jarvisDao.getAllCallSummaries()

    suspend fun addHistory(entry: HistoryEntity): Long = jarvisDao.insertHistory(entry)
    suspend fun deleteHistory(id: Long) = jarvisDao.deleteHistoryById(id)
    suspend fun clearHistory() = jarvisDao.clearAllHistory()

    suspend fun saveMemory(memory: MemoryEntity): Long = jarvisDao.insertOrUpdateMemory(memory)
    suspend fun deleteMemory(memory: MemoryEntity) = jarvisDao.deleteMemory(memory)
    suspend fun deleteMemoryById(id: Long) = jarvisDao.deleteMemoryById(id)
    suspend fun clearMemory() = jarvisDao.clearAllMemory()
    suspend fun getMemoryMap(): Map<String, String> {
        val list = jarvisDao.getAllMemoryList()
        return list.associate { it.key to it.value }
    }

    suspend fun saveNote(note: NoteEntity): Long = jarvisDao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = jarvisDao.updateNote(note)
    suspend fun deleteNote(id: Long) = jarvisDao.deleteNoteById(id)
    suspend fun clearNotes() = jarvisDao.clearAllNotes()

    suspend fun saveReminder(reminder: ReminderEntity): Long = jarvisDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: ReminderEntity) = jarvisDao.updateReminder(reminder)
    suspend fun deleteReminder(id: Long) = jarvisDao.deleteReminderById(id)

    // ─── Call Summaries ────────────────────────────────────────────────────────
    suspend fun addCallSummary(summary: CallSummaryEntity): Long =
        jarvisDao.insertCallSummary(summary)

    suspend fun deleteCallSummary(id: Long) = jarvisDao.deleteCallSummaryById(id)

    suspend fun clearAllCallSummaries() = jarvisDao.clearAllCallSummaries()
}
