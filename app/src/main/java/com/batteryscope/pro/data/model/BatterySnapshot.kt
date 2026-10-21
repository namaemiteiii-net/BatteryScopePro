package com.batteryscope.pro.data.model

/**
 * ある瞬間のバッテリー状態スナップショット。
 * BatteryManager / ACTION_BATTERY_CHANGED から取得可能な値のみを保持する。
 * 取得できなかった項目は null とし、0 や架空値で埋めない。
 */
data class BatterySnapshot(
    val timestampMs: Long,
    /** 残量 0–100。取得不可時は null */
    val levelPercent: Int?,
    /** 温度（0.1℃単位で取得される場合があるため、℃に変換後）。取得不可時は null */
    val temperatureCelsius: Float?,
    /** 電圧（mV）。取得不可時は null */
    val voltageMv: Int?,
    /**
     * 電流（mA）。BatteryManager.BATTERY_PROPERTY_CURRENT_NOW から μA → mA 変換。
     * 符号は端末依存のため、充電状態の判定には status / plugged を優先する。
     * 取得不可時は null。
     */
    val currentMa: Float?,
    /** 平均電流（mA）。BATTERY_PROPERTY_CURRENT_AVERAGE。取得不可時は null */
    val averageCurrentMa: Float?,
    /** BatteryManager.BATTERY_STATUS_* */
    val status: Int?,
    /** BatteryManager.BATTERY_PLUGGED_* の組み合わせ。0 = 未接続 */
    val plugged: Int?,
    /** BatteryManager.BATTERY_HEALTH_* */
    val health: Int?,
    /** 技術情報文字列（"Li-ion" など）。取得不可時は null */
    val technology: String?,
    /** サイクル数（API 34+ の BATTERY_PROPERTY_CYCLE_COUNT）。未対応時は null */
    val cycleCount: Int?,
    /** 充電カウンタ（μAh）。BATTERY_PROPERTY_CHARGE_COUNTER。取得不可時は null */
    val chargeCounterUah: Long?
) {
    val isCharging: Boolean
        get() = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL

    val isDischarging: Boolean
        get() = status == android.os.BatteryManager.BATTERY_STATUS_DISCHARGING

    val isPlugged: Boolean
        get() = (plugged ?: 0) != 0
}
