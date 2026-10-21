package com.batteryscope.pro.domain.estimator

import com.batteryscope.pro.data.model.CapacityEstimate
import com.batteryscope.pro.data.model.ChargingSession
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 充電セッションから最大容量を推定する。
 *
 * 基本式:
 *   推定容量 = 充電量(mAh) / (残量増加率)
 * 例: 40%→70% で 1500mAh 充電 → 1500 / 0.30 = 5000mAh
 *
 * これは概算であり、実測満充電容量ではない。
 */
class CapacityEstimator {

    /**
     * 1セッションから容量を推定。
     * 残量増加が 0 以下、または充電量が null の場合は null。
     */
    fun estimateFromSession(
        startLevel: Int?,
        endLevel: Int?,
        chargedMah: Double?
    ): Double? {
        if (startLevel == null || endLevel == null || chargedMah == null) return null
        val delta = endLevel - startLevel
        if (delta <= 0) return null // ゼロ除算防止・無効
        if (chargedMah <= 0) return null
        val fraction = delta / 100.0
        return chargedMah / fraction
    }

    /**
     * セッション品質をスコア化（0.0–1.0）。
     *
     * ルール（根拠のある簡易ルール）:
     * - 残量増加が 5% 未満 → 大幅減点（推定誤差が大きい）
     * - 残量増加が 15% 以上 → 加点
     * - 計測時間が 5分未満 → 減点
     * - サンプル数が少ない → 減点
     * - 温度変化が極端 → 減点
     * - 電流欠損が多い → 減点
     */
    fun computeQualityScore(
        levelDelta: Int?,
        durationMs: Long?,
        sampleCount: Int,
        startTemp: Float?,
        endTemp: Float?,
        hasCurrentData: Boolean,
        abnormalIntervalCount: Int
    ): Double {
        var score = 0.5

        // 残量増加
        when {
            levelDelta == null || levelDelta <= 0 -> return 0.0
            levelDelta < 5 -> score -= 0.35
            levelDelta < 10 -> score -= 0.15
            levelDelta >= 20 -> score += 0.15
            levelDelta >= 15 -> score += 0.10
        }

        // 時間
        when {
            durationMs == null || durationMs < 3 * 60 * 1000L -> score -= 0.25
            durationMs < 5 * 60 * 1000L -> score -= 0.10
            durationMs >= 20 * 60 * 1000L -> score += 0.05
        }

        // サンプル数
        when {
            sampleCount < 3 -> score -= 0.20
            sampleCount < 10 -> score -= 0.05
            sampleCount >= 30 -> score += 0.05
        }

        // 電流データ
        if (!hasCurrentData) score -= 0.30

        // 異常間隔
        if (abnormalIntervalCount > 0) score -= 0.05 * abnormalIntervalCount.coerceAtMost(4)

        // 温度変化
        if (startTemp != null && endTemp != null) {
            val tempDelta = abs(endTemp - startTemp)
            if (tempDelta > 15f) score -= 0.15
            else if (tempDelta > 10f) score -= 0.08
        }

        return score.coerceIn(0.0, 1.0)
    }

    fun qualityLabel(score: Double): String = when {
        score < 0.25 -> "低信頼"
        score < 0.5 -> "中低信頼"
        score < 0.7 -> "中信頼"
        else -> "高信頼"
    }

    /**
     * 複数セッションを統合して容量推定を行う。
     * 外れ値の影響を抑えるため中央値を基本とし、品質スコアで重み付けも考慮。
     */
    fun estimateFromSessions(
        sessions: List<ChargingSession>,
        minQuality: Double = 0.3
    ): CapacityEstimate {
        val now = System.currentTimeMillis()
        val valid = sessions.filter {
            it.estimatedCapacityMah != null &&
                    it.qualityScore >= minQuality &&
                    !it.isIncomplete
        }

        if (valid.isEmpty()) {
            return CapacityEstimate(
                timestampMs = now,
                estimatedCapacityMah = 0.0,
                validSessionCount = 0,
                spreadMah = null,
                confidenceLabel = "データ不足",
                confidenceScore = 0.0,
                sessionIdsUsed = null
            )
        }

        val capacities = valid.map { it.estimatedCapacityMah!! }
        val median = median(capacities)

        // ばらつき（中央値からの MAD を近似）
        val deviations = capacities.map { abs(it - median) }
        val mad = median(deviations)
        val approxStd = mad * 1.4826 // MAD → 標準偏差近似

        // 外れ値除外（中央値 ± 2.5σ 相当）
        val filtered = if (approxStd > 0 && valid.size >= 3) {
            valid.filter {
                abs(it.estimatedCapacityMah!! - median) <= 2.5 * approxStd
            }
        } else valid

        val finalCaps = filtered.map { it.estimatedCapacityMah!! }
        val finalMedian = median(finalCaps)
        val finalDevs = finalCaps.map { abs(it - finalMedian) }
        val finalMad = if (finalDevs.isNotEmpty()) median(finalDevs) else 0.0
        val finalSpread = finalMad * 1.4826

        val count = filtered.size
        val avgQuality = filtered.map { it.qualityScore }.average()

        val (label, confScore) = when {
            count < 2 -> "推定中" to (0.2 + avgQuality * 0.2)
            count < 4 -> {
                if (finalSpread > finalMedian * 0.15) "ばらつき大" to 0.4
                else "推定中" to (0.45 + avgQuality * 0.15)
            }
            finalSpread > finalMedian * 0.12 -> "ばらつき大" to 0.55
            else -> "安定" to (0.7 + avgQuality * 0.25).coerceAtMost(0.95)
        }

        return CapacityEstimate(
            timestampMs = now,
            estimatedCapacityMah = finalMedian,
            validSessionCount = count,
            spreadMah = finalSpread.takeIf { count >= 2 },
            confidenceLabel = label,
            confidenceScore = confScore,
            sessionIdsUsed = filtered.map { it.id }.joinToString(",")
        )
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }
}
