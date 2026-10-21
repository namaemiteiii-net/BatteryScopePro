package com.batteryscope.pro.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.batteryscope.pro.data.model.ChargingSession
import com.batteryscope.pro.ui.BatteryViewModel
import com.batteryscope.pro.ui.components.EmptyStateMessage
import com.batteryscope.pro.ui.components.InfoCard
import com.batteryscope.pro.ui.components.SectionTitle
import com.batteryscope.pro.util.CsvExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: BatteryViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val estimates by viewModel.estimates.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        val sess = viewModel.getSessionsForExport()
                        val est = viewModel.getEstimatesForExport()
                        val logs = viewModel.getDischargeLogsForExport()
                        CsvExporter().exportSessions(sess, est, logs, out)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "履歴・グラフ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                IconButton(onClick = {
                    exportLauncher.launch("batteryscope_export.csv")
                }) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = "CSVエクスポート")
                }
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "履歴削除")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 容量推定推移（簡易リスト）
        if (estimates.isNotEmpty()) {
            InfoCard {
                SectionTitle("容量推定の推移")
                estimates.take(5).forEach { e ->
                    val df = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)
                    Text(
                        text = "${df.format(Date(e.timestampMs))}: " +
                                "%.0f mAh".format(e.estimatedCapacityMah) +
                                " (${e.confidenceLabel}, n=${e.validSessionCount})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (sessions.isEmpty()) {
            EmptyStateMessage(
                title = "履歴がありません",
                message = "充電を開始すると、セッションがここに記録されます。\n" +
                        "残量が15%以上増える充電を数回行うと、容量推定が安定しやすくなります。"
            )
        } else {
            Text(
                text = "充電セッション一覧",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("履歴をすべて削除") },
            text = { Text("充電セッション・容量推定・放電ログをすべて削除します。この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showClearDialog = false
                }) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun SessionCard(session: ChargingSession, onDelete: () -> Unit) {
    val df = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
    InfoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = df.format(Date(session.startTimestampMs)) +
                            (session.endTimestampMs?.let { " 〜 ${df.format(Date(it))}" } ?: " (計測中)"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "残量: ${session.startLevelPercent ?: "—"}% → ${session.endLevelPercent ?: "—"}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                session.durationMs?.let { dur ->
                    val min = dur / 60000
                    Text(
                        text = "時間: ${min}分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (session.estimatedChargeMah != null) {
                    Text(
                        text = "推定充電量: %.1f mAh".format(session.estimatedChargeMah),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (session.estimatedCapacityMah != null) {
                    Text(
                        text = "推定容量: %.0f mAh".format(session.estimatedCapacityMah),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "品質: ${session.qualityLabel}" +
                            if (session.isIncomplete) " (異常終了)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
