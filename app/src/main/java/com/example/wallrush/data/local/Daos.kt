package com.example.wallrush.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM match_records ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<MatchRecord>>

    @Query("SELECT * FROM match_records WHERE id = :id LIMIT 1")
    suspend fun getMatchById(id: Long): MatchRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchRecord): Long

    @Query("DELETE FROM match_records")
    suspend fun clearHistory()
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM player_profile LIMIT 1")
    fun getProfileFlow(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile LIMIT 1")
    suspend fun getProfile(): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: PlayerProfile)
}
