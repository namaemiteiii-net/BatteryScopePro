package com.batteryscope.pro.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.batteryscope.pro.data.model.DischargeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DischargeLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DischargeLog): Long

    @Query("SELECT * FROM discharge_logs ORDER BY timestampMs DESC")
    fun getAllFlow(): Flow<List<DischargeLog>>

    @Query("SELECT * FROM discharge_logs ORDER BY timestampMs DESC")
    suspend fun getAll(): List<DischargeLog>

    @Query("SELECT COALESCE(SUM(dischargedMah), 0.0) FROM discharge_logs")
    suspend fun getTotalDischargedMah(): Double

    @Query("SELECT COALESCE(SUM(dischargedMah), 0.0) FROM discharge_logs")
    fun getTotalDischargedMahFlow(): Flow<Double>

    @Query("DELETE FROM discharge_logs")
    suspend fun deleteAll()
}
