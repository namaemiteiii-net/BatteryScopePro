package com.batteryscope.pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 1回の充電セッション記録。
 * AccuBattery 式の容量推定の基礎データとなる。
 */
@Entity(tableName = "charging_sessions")
data class ChargingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val startTimestampMs: Long,
    val endTimestampMs: Long?,

    val startLevelPercent: Int?,
    val endLevelPercent: Int?,

    val startTemperatureC: Float?,
    val endTemperatureC: Float?,

    /** 充電中に積算した推定充電量（mAh）。電流が取得できない場合は null */
    val estimatedChargeMah: Double?,

    /** 残量増加から算出した推定満充電容量（mAh）。品質が低い場合は null */
    val estimatedCapacityMah: Double?,

    /** セッション品質スコア 0.0–1.0。推定に使う重みの基準 */
    val qualityScore: Double,

    /** 品質ラベル（データ不足 / 低信頼 / 中信頼 / 高信頼 など） */
    val qualityLabel: String,

    /** サンプル数 */
    val sampleCount: Int,

    /** 異常終了フラグ（途中切断など） */
    val isIncomplete: Boolean,

    /** 充電方式メモ（USB / AC / Wireless など、判別できる範囲） */
    val chargeType: String?,

    /** メモ・備考 */
    val notes: String? = null
) {
    val durationMs: Long?
        get() = endTimestampMs?.let { it - startTimestampMs }

    val levelDelta: Int?
        get() {
            val s = startLevelPercent ?: return null
            val e = endLevelPercent ?: return null
            return e - s
        }
}
