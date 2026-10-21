package com.batteryscope.pro.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.batteryscope.pro.data.model.ChargingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChargingSession): Long

    @Update
    suspend fun update(session: ChargingSession)

    @Query("SELECT * FROM charging_sessions ORDER BY startTimestampMs DESC")
    fun getAllFlow(): Flow<List<ChargingSession>>

    @Query("SELECT * FROM charging_sessions ORDER BY startTimestampMs DESC")
    suspend fun getAll(): List<ChargingSession>

    @Query("SELECT * FROM charging_sessions WHERE id = :id")
    suspend fun getById(id: Long): ChargingSession?

    @Query("SELECT * FROM charging_sessions WHERE endTimestampMs IS NULL LIMIT 1")
    suspend fun getActiveSession(): ChargingSession?

    @Query("SELECT * FROM charging_sessions WHERE qualityScore >= :minQuality AND estimatedCapacityMah IS NOT NULL ORDER BY startTimestampMs DESC")
    suspend fun getValidSessionsForEstimate(minQuality: Double = 0.3): List<ChargingSession>

    @Query("DELETE FROM charging_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM charging_sessions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM charging_sessions")
    suspend fun count(): Int
}
