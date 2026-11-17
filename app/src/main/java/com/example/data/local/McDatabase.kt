package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val isOfflineReply: Boolean
)

@Entity(tableName = "crypto_snapshots", primaryKeys = ["symbol", "timeframe"])
data class CryptoSnapshotEntity(
    val symbol: String,
    val timeframe: String,
    val priceUsd: Double,
    val change24hPercent: Double,
    val buyerPercent: Int,
    val sellerPercent: Int,
    val direction: String,
    val confidencePercent: Int,
    val rsi14: Double,
    val emaFast: Double,
    val emaSlow: Double,
    val sparklineClosesCsv: String,
    val dataSource: String,
    val updatedAtMillis: Long
)

@Entity(tableName = "code_snippets")
data class CodeSnippetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val language: String,
    val code: String,
    val timestamp: Long
)

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Dao
interface CryptoSnapshotDao {
    @Query("SELECT * FROM crypto_snapshots WHERE timeframe = :timeframe")
    fun observeByTimeframe(timeframe: String): Flow<List<CryptoSnapshotEntity>>

    @Query("SELECT * FROM crypto_snapshots WHERE timeframe = :timeframe")
    suspend fun getByTimeframe(timeframe: String): List<CryptoSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CryptoSnapshotEntity>)
}

@Dao
interface CodeSnippetDao {
    @Query("SELECT * FROM code_snippets ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CodeSnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snippet: CodeSnippetEntity)

    @Query("DELETE FROM code_snippets WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(
    entities = [ChatMessageEntity::class, CryptoSnapshotEntity::class, CodeSnippetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class McDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun cryptoSnapshotDao(): CryptoSnapshotDao
    abstract fun codeSnippetDao(): CodeSnippetDao

    companion object {
        @Volatile
        private var INSTANCE: McDatabase? = null

        fun getInstance(context: Context): McDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    McDatabase::class.java,
                    "mc_assistant.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
