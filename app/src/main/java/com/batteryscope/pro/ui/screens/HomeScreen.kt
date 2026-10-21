package com.batteryscope.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
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
import com.batteryscope.pro.ui.components.EmptyStateMessage
import com.batteryscope.pro.ui.components.InfoCard
import com.batteryscope.pro.ui.components.MetricItem
import com.batteryscope.pro.ui.components.SectionTitle
import com.batteryscope.pro.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: BatteryViewModel) {
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
            text = "BatteryScope Pro",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "バッテリー診断",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        InfoCard {
            val level = snap?.levelPercent
            MetricItem(
                label = "バッテリー残量",
                value = level?.toString() ?: "—",
                unit = if (level != null) "%" else "",
                large = true
            )
            if (level != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { level / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "状態",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusBadge(
                        text = BatteryMonitor.statusToString(snap?.status),
                        isPositive = snap?.isCharging == true
                    )
                }
                Column {
                    Text(
                        text = "電源",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = BatteryMonitor.pluggedToString(snap?.plugged),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            SectionTitle("リアルタイム")
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem(
                    label = "温度",
                    value = snap?.temperatureCelsius?.let { "%.1f".format(it) } ?: "取得不可",
                    unit = if (snap?.temperatureCelsius != null) "℃" else "",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "電圧",
                    value = snap?.voltageMv?.let { "%.2f".format(it / 1000.0) } ?: "取得不可",
                    unit = if (snap?.voltageMv != null) "V" else "",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem(
                    label = "電流",
                    value = snap?.currentMa?.let { "%.0f".format(it) } ?: "取得不可",
                    unit = if (snap?.currentMa != null) "mA" else "",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "平均電流",
                    value = snap?.averageCurrentMa?.let { "%.0f".format(it) } ?: "取得不可",
                    unit = if (snap?.averageCurrentMa != null) "mA" else "",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            SectionTitle("容量・健康度（推定）")
            val estimate = state.estimate
            if (estimate == null || estimate.validSessionCount == 0) {
                EmptyStateMessage(
                    title = "計測データを収集中",
                    message = "充電を開始するとセッションが記録されます。\n" +
                            "残量が十分に増える充電（目安15%以上）を数回行うと、\n" +
                            "最大容量の推定が可能になります。\n\n" +
                            "※ 推定値であり、実測満充電容量ではありません。"
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricItem(
                        label = "推定最大容量",
                        value = "%.0f".format(estimate.estimatedCapacityMah),
                        unit = "mAh",
                        modifier = Modifier.weight(1f)
                    )
                    MetricItem(
                        label = "公称容量",
                        value = state.nominalCapacityMah?.toString() ?: "未設定",
                        unit = if (state.nominalCapacityMah != null) "mAh" else "",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricItem(
                        label = "推定健康度",
                        value = state.healthPercent?.let { "%.1f".format(it) } ?: "—",
                        unit = if (state.healthPercent != null) "%" else "",
                        modifier = Modifier.weight(1f)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "信頼度",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusBadge(text = estimate.confidenceLabel)
                        Text(
                            text = "有効セッション: ${estimate.validSessionCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (state.healthNote != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.healthNote!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "※ 推定値です。「安定」は測定の再現性を示し、正確性を保証するものではありません。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            SectionTitle("サイクル数")
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricItem(
                    label = "OSサイクル数",
                    value = state.osCycleCount?.toString() ?: "未対応",
                    unit = "",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "アプリ記録・等価サイクル",
                    value = state.equivalentCycles?.let { "%.2f".format(it) } ?: "—",
                    unit = "",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "等価サイクルはアプリが計測できた放電量に基づく推定です。インストール前の使用分は含まれません。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            SectionTitle("直近の充電セッション")
            val recent = state.recentSession
            if (recent == null) {
                Text(
                    text = "まだ充電セッションがありません。充電を開始してください。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val df = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)
                Text(
                    text = "${df.format(Date(recent.startTimestampMs))} 〜 " +
                            (recent.endTimestampMs?.let { df.format(Date(it)) } ?: "計測中"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "残量: ${recent.startLevelPercent ?: "—"}% → ${recent.endLevelPercent ?: "—"}%  /  " +
                            "品質: ${recent.qualityLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (recent.estimatedCapacityMah != null) {
                    Text(
                        text = "このセッションの推定容量: %.0f mAh".format(recent.estimatedCapacityMah),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
