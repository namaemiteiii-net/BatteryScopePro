package com.batteryscope.pro

import com.batteryscope.pro.data.model.ChargingSession
import com.batteryscope.pro.domain.estimator.CapacityEstimator
import com.batteryscope.pro.domain.estimator.ChargeEstimator
import com.batteryscope.pro.domain.estimator.HealthEstimator
import com.batteryscope.pro.domain.tracker.CycleTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 仕様書セクション14のテストケースを実装。
 */
class CapacityEstimatorTest {

    private val capacityEstimator = CapacityEstimator()
    private val chargeEstimator = ChargeEstimator()
    private val healthEstimator = HealthEstimator()
    private val cycleTracker = CycleTracker()

    /** 1. 残量40%→70%、推定充電量1,500mAh → 推定容量5,000mAh */
    @Test
    fun estimateFromSession_40to70_1500mah_equals_5000() {
        val result = capacityEstimator.estimateFromSession(40, 70, 1500.0)
        assertEquals(5000.0, result!!, 0.01)
    }

    /** 2. 残量増加が0の場合、ゼロ除算せず推定を無効にする */
    @Test
    fun estimateFromSession_zeroDelta_returnsNull() {
        assertNull(capacityEstimator.estimateFromSession(50, 50, 1000.0))
        assertNull(capacityEstimator.estimateFromSession(60, 50, 1000.0)) // 減少
    }

    /** 3. 電流値が欠損した場合、架空の充電量を生成しない */
    @Test
    fun chargeEstimator_missingCurrent_returnsNull() {
        val samples = listOf(
            ChargeEstimator.Sample(1000L, null, 40),
            ChargeEstimator.Sample(2000L, null, 41),
            ChargeEstimator.Sample(3000L, null, 42)
        )
        val result = chargeEstimator.accumulate(samples)
        assertNull(result.chargedMah)
    }

    /** 4. サンプル間隔が異常な場合、品質判定で検出する */
    @Test
    fun chargeEstimator_abnormalInterval_detected() {
        val samples = listOf(
            ChargeEstimator.Sample(0L, 1000f, 40),
            ChargeEstimator.Sample(10 * 60 * 1000L, 1000f, 50) // 10分間隔（閾値5分超）
        )
        val result = chargeEstimator.accumulate(samples, maxIntervalMs = 5 * 60 * 1000L)
        assertTrue(result.abnormalIntervalCount >= 1)
        // 異常間隔は除外されるため chargedMah は null または 0 に近い
    }

    /** 5. 複数セッションの中央値・外れ値処理 */
    @Test
    fun estimateFromSessions_medianAndOutlier() {
        val sessions = listOf(
            session(5000.0, 0.8),
            session(5100.0, 0.8),
            session(4900.0, 0.8),
            session(8000.0, 0.8) // 外れ値
        )
        val estimate = capacityEstimator.estimateFromSessions(sessions)
        assertTrue(estimate.validSessionCount >= 3)
        // 中央値はおおよそ 5000 近辺
        assertTrue(estimate.estimatedCapacityMah in 4800.0..5200.0)
    }

    /** 6. 公称容量が不明な場合、健康度を算出しない */
    @Test
    fun healthEstimator_noNominal_returnsNull() {
        val result = healthEstimator.estimate(4500.0, null)
        assertNull(result.healthPercent)
    }

    @Test
    fun healthEstimator_withNominal_calculates() {
        val result = healthEstimator.estimate(4500.0, 5000)
        assertEquals(90.0, result.healthPercent!!, 0.01)
    }

    /** 7. 累積放電量10,000mAh、基準容量5,000mAh → 等価サイクル2 */
    @Test
    fun cycleTracker_equivalentCycles() {
        val cycles = cycleTracker.computeEquivalentCycles(10000.0, 5000)
        assertEquals(2.0, cycles!!, 0.01)
    }

    @Test
    fun cycleTracker_noReference_returnsNull() {
        assertNull(cycleTracker.computeEquivalentCycles(10000.0, null))
    }

    /** 品質スコア: 残量増加が小さいと低スコア */
    @Test
    fun qualityScore_smallDelta_low() {
        val score = capacityEstimator.computeQualityScore(
            levelDelta = 3,
            durationMs = 30 * 60 * 1000L,
            sampleCount = 50,
            startTemp = 30f,
            endTemp = 32f,
            hasCurrentData = true,
            abnormalIntervalCount = 0
        )
        assertTrue(score < 0.4)
    }

    private fun session(capacity: Double, quality: Double) = ChargingSession(
        id = 0,
        startTimestampMs = System.currentTimeMillis(),
        endTimestampMs = System.currentTimeMillis() + 600000,
        startLevelPercent = 20,
        endLevelPercent = 80,
        startTemperatureC = 30f,
        endTemperatureC = 35f,
        estimatedChargeMah = capacity * 0.6,
        estimatedCapacityMah = capacity,
        qualityScore = quality,
        qualityLabel = "高信頼",
        sampleCount = 40,
        isIncomplete = false,
        chargeType = "USB"
    )
}
