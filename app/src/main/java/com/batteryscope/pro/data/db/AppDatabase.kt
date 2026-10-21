package com.batteryscope.pro.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.batteryscope.pro.data.model.CapacityEstimate
import com.batteryscope.pro.data.model.ChargingSession
import com.batteryscope.pro.data.model.DischargeLog

@Database(
    entities = [
        ChargingSession::class,
        CapacityEstimate::class,
        DischargeLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chargingSessionDao(): ChargingSessionDao
    abstract fun capacityEstimateDao(): CapacityEstimateDao
    abstract fun dischargeLogDao(): DischargeLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "batteryscope_pro.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
