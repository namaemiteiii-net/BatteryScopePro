package com.batteryscope.pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 複数セッションから統合した最大容量推定結果のスナップショット。
 */
@Entity(tableName = "capacity_estimates")
data class CapacityEstimate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val timestampMs: Long,

    /** 推定最大容量（mAh） */
    val estimatedCapacityMah: Double,

    /** 採用した有効セッション数 */
    val validSessionCount: Int,

    /** ばらつき（標準偏差相当、mAh） */
    val spreadMah: Double?,

    /**
     * 信頼度ラベル:
     * - "データ不足"
     * - "推定中"
     * - "ばらつき大"
     * - "安定"
     */
    val confidenceLabel: String,

    /** 信頼度スコア 0.0–1.0 */
    val confidenceScore: Double,

    /** 使用したセッションIDのカンマ区切り（デバッグ用） */
    val sessionIdsUsed: String?
)
