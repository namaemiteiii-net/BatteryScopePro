package com.batteryscope.pro.domain.estimator

import com.batteryscope.pro.data.model.BatterySnapshot
import kotlin.math.abs

/**
 * 充電量（mAh）の積算。
 *
 * 概念式: 充電量 ≒ Σ (電流_mA × 経過時間_h)
 * 台形積分を使用し、単純な瞬間値×総時間による誤差を抑える。
 *
 * 注意:
 * - Android が公開する電流値はバッテリーに入った正味電流と必ずしも一致しない。
 * - 端末自身の消費、センサー誤差、充電制御の影響がある。
 * - 電流が取得できない場合は null を返し、架空値を生成しない。
 */
class ChargeEstimator {

    data class Sample(
        val timestampMs: Long,
        val currentMa: Float?,
        val levelPercent: Int?
    )

    data class Result(
        /** 積算充電量（mAh）。電流欠損時は null */
        val chargedMah: Double?,
        /** 有効サンプル数 */
        val validSampleCount: Int,
        /** 総サンプル数 */
        val totalSampleCount: Int,
        /** 異常に長い間隔のサンプル数 */
        val abnormalIntervalCount: Int,
        /** 品質に影響するメモ */
        val notes: List<String>
    )

    /**
     * サンプル列から充電量を積算する。
     * @param samples 時系列順のサンプル
     * @param maxIntervalMs これより長い間隔は異常として除外（デフォルト 5分）
     */
    fun accumulate(
        samples: List<Sample>,
        maxIntervalMs: Long = 5 * 60 * 1000L
    ): Result {
        if (samples.size < 2) {
            return Result(
                chargedMah = null,
                validSampleCount = samples.size,
                totalSampleCount = samples.size,
                abnormalIntervalCount = 0,
                notes = listOf("サンプル不足")
            )
        }

        var totalMah = 0.0
        var validPairs = 0
        var abnormal = 0
        val notes = mutableListOf<String>()
        var anyCurrentMissing = false

        for (i in 1 until samples.size) {
            val prev = samples[i - 1]
            val curr = samples[i]
            val dtMs = curr.timestampMs - prev.timestampMs
            if (dtMs <= 0) continue

            if (dtMs > maxIntervalMs) {
                abnormal++
                continue
            }

            val i1 = prev.currentMa
            val i2 = curr.currentMa
            if (i1 == null || i2 == null) {
                anyCurrentMissing = true
                continue
            }

            // 充電中は正の電流（端末によって符号が逆の場合があるため、絶対値ではなく
            // 呼び出し側で充電中のみこの estimator を使う前提。ここでは符号をそのまま使う）
            // ただし負の値が多い端末もあるため、積算時は「充電方向」を正とする。
            // ここでは絶対値を取らず、呼び出し側で充電中サンプルのみ渡すことを推奨。
            val avgCurrentMa = (i1 + i2) / 2.0
            val hours = dtMs / 3_600_000.0
            totalMah += avgCurrentMa * hours
            validPairs++
        }

        if (anyCurrentMissing) {
            notes.add("電流欠損あり")
        }
        if (abnormal > 0) {
            notes.add("異常間隔サンプル: $abnormal")
        }

        val charged = if (validPairs > 0) {
            // 充電方向を正に揃える（負の積算は放電方向なので絶対値を取る）
            abs(totalMah)
        } else null

        return Result(
            chargedMah = charged,
            validSampleCount = validPairs + 1,
            totalSampleCount = samples.size,
            abnormalIntervalCount = abnormal,
            notes = notes
        )
    }
}
