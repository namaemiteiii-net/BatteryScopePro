package com.batteryscope.pro.domain.tracker

import com.batteryscope.pro.data.model.BatterySnapshot
import com.batteryscope.pro.data.model.DischargeLog
import com.batteryscope.pro.data.repository.HistoryRepository
import com.batteryscope.pro.domain.estimator.ChargeEstimator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/**
 * 放電量の積算。
 * 充電中の電流は放電量に加算しない。
 */
class DischargeTracker(
    private val repository: HistoryRepository
) {
    private val mutex = Mutex()
    private val samples = mutableListOf<ChargeEstimator.Sample>()
    private var lastLevel: Int? = null
    private var segmentStartLevel: Int? = null
    private var isDischarging = false

    suspend fun onSnapshot(snapshot: BatterySnapshot) {
        mutex.withLock {
            val discharging = snapshot.isDischarging ||
                    (!snapshot.isPlugged && snapshot.status != android.os.BatteryManager.BATTERY_STATUS_CHARGING
                            && snapshot.status != android.os.BatteryManager.BATTERY_STATUS_FULL)

            if (discharging) {
                if (!isDischarging) {
                    // 放電開始
                    samples.clear()
                    segmentStartLevel = snapshot.levelPercent
                }
                samples.add(
                    ChargeEstimator.Sample(
                        timestampMs = snapshot.timestampMs,
                        currentMa = snapshot.currentMa?.let { -abs(it) }, // 放電方向を負に揃える
                        levelPercent = snapshot.levelPercent
                    )
                )
                isDischarging = true
            } else if (isDischarging) {
                // 放電終了 → 積算して保存
                flushSegment(snapshot)
                isDischarging = false
            }
            lastLevel = snapshot.levelPercent
        }
    }

    private suspend fun flushSegment(snapshot: BatterySnapshot) {
        if (samples.size < 2) {
            samples.clear()
            return
        }
        // 電流があれば積算、なければレベル変化のみでは mAh を推定しない（架空値禁止）
        val estimator = ChargeEstimator()
        // 放電電流は負で積算される想定なので、絶対値を取る
        val result = estimator.accumulate(
            samples.map { it.copy(currentMa = it.currentMa?.let { c -> abs(c) }) }
        )
        val discharged = result.chargedMah
        if (discharged != null && discharged > 0.5) {
            repository.insertDischargeLog(
                DischargeLog(
                    timestampMs = snapshot.timestampMs,
                    dischargedMah = discharged,
                    startLevelPercent = segmentStartLevel,
                    endLevelPercent = snapshot.levelPercent,
                    sampleCount = samples.size
                )
            )
        }
        samples.clear()
        segmentStartLevel = null
    }
}
