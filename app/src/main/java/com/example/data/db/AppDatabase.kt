package com.aistudio.jarvis.voiceagent.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aistudio.jarvis.voiceagent.data.call.CallSummaryEntity

@Database(
    entities = [
        HistoryEntity::class,
        MemoryEntity::class,
        NoteEntity::class,
        ReminderEntity::class,
        CallSummaryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jarvisDao(): JarvisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val cached = INSTANCE
            if (cached != null) return cached
            synchronized(this) {
                val existing = INSTANCE
                if (existing != null) return existing
                val appContext = context.applicationContext
                val instance = try {
                    Room.databaseBuilder(appContext, AppDatabase::class.java, "jarvis_core.db")
                        .fallbackToDestructiveMigration()
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()
                } catch (diskError: Throwable) {
                    android.util.Log.e("AppDatabase", "Disk database failed, using memory", diskError)
                    Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
                        .fallbackToDestructiveMigration()
                        .build()
                }
                INSTANCE = instance
                return instance
            }
        }
    }
}
