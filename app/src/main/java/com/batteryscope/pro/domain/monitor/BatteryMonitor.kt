package com.batteryscope.pro.domain.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.batteryscope.pro.data.model.BatterySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Android の正式 API のみを使用してバッテリー情報を取得する。
 * 取得できない値は null を返す（0 や架空値で埋めない）。
 */
class BatteryMonitor(private val context: Context) {

    private val batteryManager: BatteryManager? =
        context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    /**
     * 現在のスナップショットを1回取得する。
     */
    fun getCurrentSnapshot(): BatterySnapshot {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        return parseSnapshot(intent)
    }

    /**
     * ACTION_BATTERY_CHANGED を購読する Flow。
     * 更新頻度は OS に依存する。アプリ側で過剰更新しないよう間引き推奨。
     */
    fun observeBatteryChanges(): Flow<BatterySnapshot> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    trySend(parseSnapshot(intent))
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        // 初回値
        trySend(getCurrentSnapshot())
        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    private fun parseSnapshot(intent: Intent?): BatterySnapshot {
        val now = System.currentTimeMillis()
        if (intent == null) {
            return BatterySnapshot(
                timestampMs = now,
                levelPercent = null,
                temperatureCelsius = null,
                voltageMv = null,
                currentMa = null,
                averageCurrentMa = null,
                status = null,
                plugged = null,
                health = null,
                technology = null,
                cycleCount = null,
                chargeCounterUah = null
            )
        }

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val levelPercent = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else null

        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temperatureC = if (tempRaw != Int.MIN_VALUE) tempRaw / 10f else null

        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val voltageMv = if (voltage > 0) voltage else null

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1).takeIf { it >= 0 }
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1).takeIf { it >= 0 }
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1).takeIf { it >= 0 }
        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

        // 電流・平均電流・サイクル数・充電カウンタは BatteryManager プロパティから
        val currentMa = readCurrentNowMa()
        val averageCurrentMa = readCurrentAverageMa()
        val cycleCount = readCycleCount()
        val chargeCounter = readChargeCounterUah()

        return BatterySnapshot(
            timestampMs = now,
            levelPercent = levelPercent,
            temperatureCelsius = temperatureC,
            voltageMv = voltageMv,
            currentMa = currentMa,
            averageCurrentMa = averageCurrentMa,
            status = status,
            plugged = plugged,
            health = health,
            technology = technology,
            cycleCount = cycleCount,
            chargeCounterUah = chargeCounter
        )
    }

    /**
     * BATTERY_PROPERTY_CURRENT_NOW（μA）を mA に変換。
     * 符号は端末依存。取得不可時は null。
     */
    private fun readCurrentNowMa(): Float? {
        val bm = batteryManager ?: return null
        return try {
            val uA = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (uA == Long.MIN_VALUE || uA == Long.MAX_VALUE) null
            else uA / 1000f
        } catch (_: Exception) {
            null
        }
    }

    private fun readCurrentAverageMa(): Float? {
        val bm = batteryManager ?: return null
        return try {
            val uA = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
            if (uA == Long.MIN_VALUE || uA == Long.MAX_VALUE) null
            else uA / 1000f
        } catch (_: Exception) {
            null
        }
    }

    /**
     * API 34+ で BATTERY_PROPERTY_CYCLE_COUNT が利用可能。
     * それ以前や未対応端末では null。
     */
    private fun readCycleCount(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        val bm = batteryManager ?: return null
        return try {
            // BATTERY_PROPERTY_CYCLE_COUNT = 5 (API 34)
            val value = bm.getIntProperty(5)
            if (value == Int.MIN_VALUE || value < 0) null else value
        } catch (_: Exception) {
            null
        }
    }

    private fun readChargeCounterUah(): Long? {
        val bm = batteryManager ?: return null
        return try {
            val uAh = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            if (uAh == Long.MIN_VALUE || uAh < 0) null else uAh
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun statusToString(status: Int?): String = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "充電中"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放電中"
            BatteryManager.BATTERY_STATUS_FULL -> "満充電"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "充電停止"
            BatteryManager.BATTERY_STATUS_UNKNOWN -> "不明"
            null -> "取得不可"
            else -> "不明($status)"
        }

        fun healthToString(health: Int?): String = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "過熱"
            BatteryManager.BATTERY_HEALTH_DEAD -> "劣化"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "過電圧"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "異常"
            BatteryManager.BATTERY_HEALTH_COLD -> "低温"
            null -> "取得不可"
            else -> "不明"
        }

        fun pluggedToString(plugged: Int?): String {
            if (plugged == null || plugged == 0) return "未接続"
            val parts = mutableListOf<String>()
            if (plugged and BatteryManager.BATTERY_PLUGGED_AC != 0) parts.add("AC")
            if (plugged and BatteryManager.BATTERY_PLUGGED_USB != 0) parts.add("USB")
            if (plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0) parts.add("ワイヤレス")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                plugged and BatteryManager.BATTERY_PLUGGED_DOCK != 0
            ) {
                parts.add("ドック")
            }
            return if (parts.isEmpty()) "接続中" else parts.joinToString(" / ")
        }
    }
}
