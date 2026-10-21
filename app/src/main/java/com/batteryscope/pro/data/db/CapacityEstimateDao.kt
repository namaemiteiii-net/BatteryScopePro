package com.batteryscope.pro.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.batteryscope.pro.data.model.CapacityEstimate
import kotlinx.coroutines.flow.Flow

@Dao
interface CapacityEstimateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(estimate: CapacityEstimate): Long

    @Query("SELECT * FROM capacity_estimates ORDER BY timestampMs DESC LIMIT 1")
    fun getLatestFlow(): Flow<CapacityEstimate?>

    @Query("SELECT * FROM capacity_estimates ORDER BY timestampMs DESC LIMIT 1")
    suspend fun getLatest(): CapacityEstimate?

    @Query("SELECT * FROM capacity_estimates ORDER BY timestampMs DESC")
    fun getAllFlow(): Flow<List<CapacityEstimate>>

    @Query("SELECT * FROM capacity_estimates ORDER BY timestampMs DESC")
    suspend fun getAll(): List<CapacityEstimate>

    @Query("DELETE FROM capacity_estimates")
    suspend fun deleteAll()
}
