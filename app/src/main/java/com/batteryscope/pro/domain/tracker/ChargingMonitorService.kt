package com.batteryscope.pro.domain.tracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.batteryscope.pro.MainActivity
import com.batteryscope.pro.R
import com.batteryscope.pro.data.repository.HistoryRepository
import com.batteryscope.pro.domain.monitor.BatteryMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 充電中のみフォアグラウンドで計測を継続するサービス。
 * 常時監視は行わず、充電開始時に起動し、終了後に停止する想定。
 */
class ChargingMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val app = application as com.batteryscope.pro.BatteryScopeApp
        val repository = app.historyRepository
        val batteryMonitor = BatteryMonitor(this)
        val sessionTracker = ChargingSessionTracker(repository)
        val dischargeTracker = DischargeTracker(repository)

        monitorJob?.cancel()
        monitorJob = scope.launch {
            // 起動時の孤児セッション回復
            sessionTracker.recoverOrphanedSession(batteryMonitor.getCurrentSnapshot())

            while (isActive) {
                val snap = batteryMonitor.getCurrentSnapshot()
                sessionTracker.onSnapshot(snap)
                dischargeTracker.onSnapshot(snap)

                // 充電していなければサービス終了
                if (!snap.isCharging && !snap.isPlugged) {
                    stopSelf()
                    break
                }
                delay(SAMPLE_INTERVAL_MS)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "charging_monitor"
        const val NOTIFICATION_ID = 1001
        const val SAMPLE_INTERVAL_MS = 15_000L // 15秒間隔。過剰な電池消費を避ける
    }
}
