package com.batteryscope.pro.domain.tracker

import com.batteryscope.pro.data.model.BatterySnapshot
import com.batteryscope.pro.data.model.ChargingSession
import com.batteryscope.pro.data.repository.HistoryRepository
import com.batteryscope.pro.domain.estimator.CapacityEstimator
import com.batteryscope.pro.domain.estimator.ChargeEstimator
import com.batteryscope.pro.domain.monitor.BatteryMonitor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 充電セッションの開始・終了を管理し、セッション終了時に容量推定を行う。
 */
class ChargingSessionTracker(
    private val repository: HistoryRepository,
    private val chargeEstimator: ChargeEstimator = ChargeEstimator(),
    private val capacityEstimator: CapacityEstimator = CapacityEstimator()
) {
    private val mutex = Mutex()
    private var activeSessionId: Long? = null
    private val samples = mutableListOf<ChargeEstimator.Sample>()
    private var lastWasCharging: Boolean = false

    /**
     * 新しいスナップショットを処理する。
     * 充電開始・終了を検出し、セッションを記録する。
     */
    suspend fun onSnapshot(snapshot: BatterySnapshot) {
        mutex.withLock {
            val isCharging = snapshot.isCharging ||
                    (snapshot.isPlugged && snapshot.status != android.os.BatteryManager.BATTERY_STATUS_DISCHARGING)

            if (isCharging && !lastWasCharging) {
                // 充電開始
                startSession(snapshot)
            } else if (isCharging && lastWasCharging) {
                // 継続中 — サンプル追加
                addSample(snapshot)
            } else if (!isCharging && lastWasCharging) {
                // 充電終了
                endSession(snapshot, incomplete = false)
            }
            // 放電中の放電量積算は別途 DischargeTracker で扱う想定

            lastWasCharging = isCharging
        }
    }

    private suspend fun startSession(snapshot: BatterySnapshot) {
        // 既存の未終了セッションがあれば異常終了として閉じる
        val existing = repository.getActiveSession()
        if (existing != null) {
            endSessionInternal(existing, snapshot, incomplete = true)
        }

        samples.clear()
        addSample(snapshot)

        val session = ChargingSession(
            startTimestampMs = snapshot.timestampMs,
            endTimestampMs = null,
            startLevelPercent = snapshot.levelPercent,
            endLevelPercent = null,
            startTemperatureC = snapshot.temperatureCelsius,
            endTemperatureC = null,
            estimatedChargeMah = null,
            estimatedCapacityMah = null,
            qualityScore = 0.0,
            qualityLabel = "計測中",
            sampleCount = 1,
            isIncomplete = false,
            chargeType = BatteryMonitor.pluggedToString(snapshot.plugged)
        )
        activeSessionId = repository.insertSession(session)
    }

    private fun addSample(snapshot: BatterySnapshot) {
        samples.add(
            ChargeEstimator.Sample(
                timestampMs = snapshot.timestampMs,
                currentMa = snapshot.currentMa,
                levelPercent = snapshot.levelPercent
            )
        )
    }

    private suspend fun endSession(snapshot: BatterySnapshot, incomplete: Boolean) {
        val id = activeSessionId ?: return
        val existing = repository.getSessionById(id) ?: return
        endSessionInternal(existing, snapshot, incomplete)
        activeSessionId = null
        samples.clear()
    }

    private suspend fun endSessionInternal(
        existing: ChargingSession,
        snapshot: BatterySnapshot,
        incomplete: Boolean
    ) {
        addSample(snapshot)

        val chargeResult = chargeEstimator.accumulate(samples)
        val levelDelta = run {
            val s = existing.startLevelPercent
            val e = snapshot.levelPercent
            if (s != null && e != null) e - s else null
        }
        val duration = snapshot.timestampMs - existing.startTimestampMs

        val capacity = capacityEstimator.estimateFromSession(
            existing.startLevelPercent,
            snapshot.levelPercent,
            chargeResult.chargedMah
        )

        val quality = capacityEstimator.computeQualityScore(
            levelDelta = levelDelta,
            durationMs = duration,
            sampleCount = samples.size,
            startTemp = existing.startTemperatureC,
            endTemp = snapshot.temperatureCelsius,
            hasCurrentData = chargeResult.chargedMah != null,
            abnormalIntervalCount = chargeResult.abnormalIntervalCount
        )

        // 品質が極端に低い、または残量が増えていない場合は容量推定を無効
        val finalCapacity = if (quality >= 0.25 && capacity != null) capacity else null

        val updated = existing.copy(
            endTimestampMs = snapshot.timestampMs,
            endLevelPercent = snapshot.levelPercent,
            endTemperatureC = snapshot.temperatureCelsius,
            estimatedChargeMah = chargeResult.chargedMah,
            estimatedCapacityMah = finalCapacity,
            qualityScore = quality,
            qualityLabel = if (incomplete) "異常終了" else capacityEstimator.qualityLabel(quality),
            sampleCount = samples.size,
            isIncomplete = incomplete,
            notes = chargeResult.notes.joinToString("; ").ifEmpty { null }
        )
        repository.updateSession(updated)

        // 容量推定の再計算
        if (finalCapacity != null && quality >= 0.3) {
            repository.recomputeCapacityEstimate()
        }
    }

    /** アプリ起動時に未終了セッションがあれば異常終了として閉じる */
    suspend fun recoverOrphanedSession(current: BatterySnapshot) {
        mutex.withLock {
            val orphan = repository.getActiveSession()
            if (orphan != null) {
                endSessionInternal(orphan, current, incomplete = true)
                activeSessionId = null
                samples.clear()
            }
        }
    }
}
