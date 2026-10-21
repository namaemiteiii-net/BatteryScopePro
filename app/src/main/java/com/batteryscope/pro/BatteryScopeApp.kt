package com.batteryscope.pro

import android.app.Application
import com.batteryscope.pro.data.db.AppDatabase
import com.batteryscope.pro.data.repository.HistoryRepository
import com.batteryscope.pro.data.repository.SettingsRepository

class BatteryScopeApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(database) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
