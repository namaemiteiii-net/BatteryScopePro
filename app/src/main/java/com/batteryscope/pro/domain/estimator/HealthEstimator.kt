package com.batteryscope.pro.domain.estimator

/**
 * 健康度の計算。
 *
 * 健康度(%) = 推定最大容量 / 公称容量 × 100
 *
 * 注意:
 * - 公称容量が不明な場合は計算しない（null）。
 * - 100% を超える場合も切り詰めず、測定誤差の可能性としてそのまま返す。
 * - 機種名から公称容量を自動確定しない。
 */
class HealthEstimator {

    data class HealthResult(
        val healthPercent: Double?,
        val note: String?
    )

    fun estimate(
        estimatedCapacityMah: Double?,
        nominalCapacityMah: Int?
    ): HealthResult {
        if (estimatedCapacityMah == null || estimatedCapacityMah <= 0) {
            return HealthResult(null, "推定容量がありません")
        }
        if (nominalCapacityMah == null || nominalCapacityMah <= 0) {
            return HealthResult(null, "公称容量が未設定のため健康度を算出できません")
        }
        val health = (estimatedCapacityMah / nominalCapacityMah.toDouble()) * 100.0
        val note = when {
            health > 110.0 -> "推定値が公称容量を大きく上回っています。測定誤差の可能性があります"
            health > 100.0 -> "推定値が公称容量をやや上回っています。測定ばらつきの範囲内の可能性があります"
            health < 50.0 -> "推定健康度が低いです。複数回の計測で再確認することを推奨します"
            else -> null
        }
        return HealthResult(health, note)
    }
}
