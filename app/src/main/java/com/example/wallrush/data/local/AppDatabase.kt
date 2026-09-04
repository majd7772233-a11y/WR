package com.example.wallrush.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.wallrush.domain.model.FinishReason
import com.example.wallrush.domain.model.GameMode
import com.example.wallrush.domain.model.PlayerId

class Converters {
    @TypeConverter
    fun fromGameMode(value: GameMode): String = value.name

    @TypeConverter
    fun toGameMode(value: String): GameMode = try {
        GameMode.valueOf(value)
    } catch (e: Exception) {
        GameMode.VS_AI
    }

    @TypeConverter
    fun fromPlayerId(value: PlayerId?): String? = value?.name

    @TypeConverter
    fun toPlayerId(value: String?): PlayerId? = value?.let {
        try { PlayerId.valueOf(it) } catch (e: Exception) { null }
    }

    @TypeConverter
    fun fromFinishReason(value: FinishReason?): String? = value?.name

    @TypeConverter
    fun toFinishReason(value: String?): FinishReason? = value?.let {
        try { FinishReason.valueOf(it) } catch (e: Exception) { null }
    }
}

@Database(
    entities = [MatchRecord::class, PlayerProfile::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wallrush_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
