package com.batteryscope.pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 放電量積算用のログ。
 * アプリが記録できた放電量を累積し、等価サイクル計算に使う。
 */
@Entity(tableName = "discharge_logs")
data class DischargeLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val timestampMs: Long,

    /** この区間で放電したと推定される量（mAh）。正の値 */
    val dischargedMah: Double,

    /** 区間開始残量 */
    val startLevelPercent: Int?,

    /** 区間終了残量 */
    val endLevelPercent: Int?,

    /** サンプル数 */
    val sampleCount: Int
)
