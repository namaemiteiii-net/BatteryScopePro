package com.batteryscope.pro.domain.tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.batteryscope.pro.domain.monitor.BatteryMonitor

/**
 * 起動時に充電中であれば監視サービスを開始する。
 * 常時監視は行わない。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val monitor = BatteryMonitor(context)
        val snap = monitor.getCurrentSnapshot()
        if (snap.isCharging || snap.isPlugged) {
            val serviceIntent = Intent(context, ChargingMonitorService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
