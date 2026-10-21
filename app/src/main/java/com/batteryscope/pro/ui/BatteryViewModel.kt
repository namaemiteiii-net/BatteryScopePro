package com.batteryscope.pro.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.batteryscope.pro.BatteryScopeApp
import com.batteryscope.pro.data.model.AppSettings
import com.batteryscope.pro.data.model.BatterySnapshot
import com.batteryscope.pro.data.model.CapacityEstimate
import com.batteryscope.pro.data.model.ChargingSession
import com.batteryscope.pro.data.model.DischargeLog
import com.batteryscope.pro.data.model.ThemeMode
import com.batteryscope.pro.domain.estimator.HealthEstimator
import com.batteryscope.pro.domain.monitor.BatteryMonitor
import com.batteryscope.pro.domain.tracker.ChargingMonitorService
import com.batteryscope.pro.domain.tracker.ChargingSessionTracker
import com.batteryscope.pro.domain.tracker.DischargeTracker
import com.batteryscope.pro.util.CsvExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.OutputStream

data class HomeUiState(
    val snapshot: BatterySnapshot? = null,
    val estimate: CapacityEstimate? = null,
    val healthPercent: Double? = null,
    val healthNote: String? = null,
    val equivalentCycles: Double? = null,
    val osCycleCount: Int? = null,
    val nominalCapacityMah: Int? = null,
    val recentSession: ChargingSession? = null,
    val settings: AppSettings = AppSettings()
)

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BatteryScopeApp
    private val batteryMonitor = BatteryMonitor(application)
    private val historyRepo = app.historyRepository
    private val settingsRepo = app.settingsRepository
    private val healthEstimator = HealthEstimator()
    private val sessionTracker = ChargingSessionTracker(historyRepo)
    private val dischargeTracker = DischargeTracker(historyRepo)

    private val _snapshot = MutableStateFlow<BatterySnapshot?>(null)
    private var pollJob: Job? = null

    val sessions: StateFlow<List<ChargingSession>> = historyRepo.sessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val estimates: StateFlow<List<CapacityEstimate>> = historyRepo.allEstimatesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val homeState: StateFlow<HomeUiState> = combine(
        _snapshot,
        historyRepo.latestEstimateFlow(),
        settingsRepo.settingsFlow,
        historyRepo.sessionsFlow(),
        historyRepo.totalDischargedFlow()
    ) { snap, estimate, settings, sessions, totalDischarged ->
        val health = healthEstimator.estimate(
            estimate?.estimatedCapacityMah?.takeIf { estimate.validSessionCount > 0 },
            settings.nominalCapacityMah
        )
        val eqCycles = if (settings.nominalCapacityMah != null && totalDischarged > 0) {
            totalDischarged / settings.nominalCapacityMah.toDouble()
        } else {
            null
        }
        HomeUiState(
            snapshot = snap,
            estimate = estimate?.takeIf { it.validSessionCount > 0 },
            healthPercent = health.healthPercent,
            healthNote = health.note,
            equivalentCycles = eqCycles,
            osCycleCount = snap?.cycleCount,
            nominalCapacityMah = settings.nominalCapacityMah,
            recentSession = sessions.firstOrNull(),
            settings = settings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        startPolling()
        viewModelScope.launch {
            sessionTracker.recoverOrphanedSession(batteryMonitor.getCurrentSnapshot())
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                val snap = batteryMonitor.getCurrentSnapshot()
                _snapshot.value = snap
                sessionTracker.onSnapshot(snap)
                dischargeTracker.onSnapshot(snap)

                if (snap.isCharging || snap.isPlugged) {
                    startMonitorService()
                }

                val interval = settings.value.realtimeUpdateIntervalMs
                delay(interval)
            }
        }
    }

    private fun startMonitorService() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, ChargingMonitorService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        } catch (_: Exception) {
            // サービス起動失敗は主要機能を止めない
        }
    }

    fun setNominalCapacity(mah: Int?) {
        viewModelScope.launch { settingsRepo.setNominalCapacity(mah) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            historyRepo.deleteSession(id)
            historyRepo.recomputeCapacityEstimate()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch { historyRepo.clearAllHistory() }
    }

    suspend fun getSessionsForExport(): List<ChargingSession> = historyRepo.getAllSessions()

    suspend fun getEstimatesForExport(): List<CapacityEstimate> =
        app.database.capacityEstimateDao().getAll()

    suspend fun getDischargeLogsForExport(): List<DischargeLog> =
        app.database.dischargeLogDao().getAll()

    fun exportCsv(output: OutputStream) {
        viewModelScope.launch {
            val sessions = historyRepo.getAllSessions()
            val estimates = app.database.capacityEstimateDao().getAll()
            val discharges = app.database.dischargeLogDao().getAll()
            CsvExporter().exportSessions(sessions, estimates, discharges, output)
        }
    }

    fun deviceInfo(): Map<String, String> {
        return mapOf(
            "メーカー" to (Build.MANUFACTURER ?: "取得不可"),
            "モデル" to (Build.MODEL ?: "取得不可"),
            "端末" to (Build.DEVICE ?: "取得不可"),
            "Android バージョン" to (Build.VERSION.RELEASE ?: "取得不可"),
            "API レベル" to Build.VERSION.SDK_INT.toString(),
            "ビルド" to (Build.DISPLAY ?: "取得不可")
        )
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
