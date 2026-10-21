package com.batteryscope.pro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.batteryscope.pro.domain.monitor.BatteryMonitor
import com.batteryscope.pro.ui.BatteryViewModel
import com.batteryscope.pro.ui.components.InfoCard
import com.batteryscope.pro.ui.components.MetricItem
import com.batteryscope.pro.ui.components.SectionTitle

@Composable
fun RealtimeScreen(viewModel: BatteryViewModel) {
    val state by viewModel.homeState.collectAsState()
    val snap = state.snapshot
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Text(
            text = "リアルタイム診断",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        InfoCard {
            SectionTitle("基本情報")
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem(
                    label = "残量",
                    value = snap?.levelPercent?.toString() ?: "取得不可",
                    unit = if (snap?.levelPercent != null) "%" else "",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "状態",
                    value = BatteryMonitor.statusToString(snap?.status),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem(
                    label = "温度",
                    value = snap?.temperatureCelsius?.let { "%.1f".format(it) } ?: "取得不可",
                    unit = if (snap?.temperatureCelsius != null) "℃" else "",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "電圧",
                    value = snap?.voltageMv?.let { "%d".format(it) } ?: "取得不可",
                    unit = if (snap?.voltageMv != null) "mV" else "",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            SectionTitle("電流")
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem(
                    label = "瞬間電流",
                    value = snap?.currentMa?.let { "%.1f".format(it) } ?: "取得不可",
                    unit = if (snap?.currentMa != null) "mA" else "",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "平均電流",
                    value = snap?.averageCurrentMa?.let { "%.1f".format(it) } ?: "取得不可",
                    unit = if (snap?.averageCurrentMa != null) "mA" else "",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "電流の符号は端末依存です。充電・放電の判定にはOSの充電状態を優先しています。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            SectionTitle("バッテリー詳細")
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem(
                    label = "健康状態(OS)",
                    value = BatteryMonitor.healthToString(snap?.health),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "技術",
                    value = snap?.technology ?: "取得不可",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem(
                    label = "電源接続",
                    value = BatteryMonitor.pluggedToString(snap?.plugged),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "サイクル数(OS)",
                    value = snap?.cycleCount?.toString() ?: "未対応",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            MetricItem(
                label = "充電カウンタ",
                value = snap?.chargeCounterUah?.let { "%d μAh".format(it) } ?: "取得不可"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            Text(
                text = "更新間隔は約 ${state.settings.realtimeUpdateIntervalMs / 1000} 秒です。" +
                        "過剰な更新による電池消費を抑えています。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
