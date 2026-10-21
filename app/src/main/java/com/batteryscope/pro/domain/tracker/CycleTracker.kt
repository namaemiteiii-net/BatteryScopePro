package com.batteryscope.pro.domain.tracker

/**
 * 充放電サイクル数の管理。
 *
 * 1. OS から取得したサイクル数（API 34+）はそのまま表示。架空値は返さない。
 * 2. アプリ独自の累積等価サイクル数 = 累積放電量 / 基準容量
 *    これはアプリが計測できた範囲に限られる。
 */
class CycleTracker {

    /**
     * 等価サイクル数を計算。
     * @param totalDischargedMah アプリが記録した累積放電量
     * @param referenceCapacityMah 基準容量（公称容量など）。不明時は null
     */
    fun computeEquivalentCycles(
        totalDischargedMah: Double,
        referenceCapacityMah: Int?
    ): Double? {
        if (referenceCapacityMah == null || referenceCapacityMah <= 0) return null
        if (totalDischargedMah < 0) return null
        return totalDischargedMah / referenceCapacityMah.toDouble()
    }
}
