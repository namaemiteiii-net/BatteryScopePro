package com.batteryscope.pro.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.batteryscope.pro.data.model.ThemeMode
import com.batteryscope.pro.domain.monitor.BatteryMonitor
import com.batteryscope.pro.ui.BatteryViewModel
import com.batteryscope.pro.ui.components.InfoCard
import com.batteryscope.pro.ui.components.SectionTitle

@Composable
fun DeviceInfoScreen(viewModel: BatteryViewModel) {
    val state by viewModel.homeState.collectAsState()
    val deviceInfo = viewModel.deviceInfo()
    val snap = state.snapshot
    var capacityInput by remember(state.nominalCapacityMah) {
        mutableStateOf(state.nominalCapacityMah?.toString() ?: "")
    }
    val scroll = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Text(
            text = "端末・設定",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        InfoCard {
            SectionTitle("端末情報")
            deviceInfo.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            SectionTitle("バッテリー情報（OS）")
            InfoRow("健康状態", BatteryMonitor.healthToString(snap?.health))
            InfoRow("技術", snap?.technology ?: "取得不可")
            InfoRow(
                "サイクル数",
                snap?.cycleCount?.toString() ?: "未対応（API 34+ で一部端末）"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            SectionTitle("公称容量の設定")
            Text(
                text = "メーカー公表のバッテリー容量（mAh）を入力してください。\n" +
                        "機種名から自動確定はしません。不明な場合は空欄のままで構いません。\n" +
                        "公称容量が設定されている場合のみ、健康度を計算します。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = capacityInput,
                onValueChange = { capacityInput = it.filter { c -> c.isDigit() } },
                label = { Text("公称容量 (mAh)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                TextButton(onClick = {
                    val mah = capacityInput.toIntOrNull()
                    viewModel.setNominalCapacity(mah)
                }) {
                    Text("保存")
                }
                TextButton(onClick = {
                    capacityInput = ""
                    viewModel.setNominalCapacity(null)
                }) {
                    Text("クリア")
                }
            }
            if (state.nominalCapacityMah != null) {
                Text(
                    text = "現在の設定: ${state.nominalCapacityMah} mAh",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            SectionTitle("表示設定（テーマ）")
            Text(
                text = "「システム」はOSのライト/ダーク設定に自動で追従します。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.SYSTEM -> "システム"
                        ThemeMode.LIGHT -> "ライト"
                        ThemeMode.DARK -> "ダーク"
                    }
                    FilterChip(
                        selected = state.settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Text(
                text = "現在: " + when (state.settings.themeMode) {
                    ThemeMode.SYSTEM -> "システム（自動）"
                    ThemeMode.LIGHT -> "ライト"
                    ThemeMode.DARK -> "ダーク"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            SectionTitle("制限事項")
            Text(
                text = "・電流・サイクル数・満充電容量などは端末・OSによって取得できない場合があります。\n" +
                        "・取得できない値は「取得不可」「未対応」と表示し、架空の値は使いません。\n" +
                        "・容量・健康度は充電セッションからの推定であり、実測値ではありません。\n" +
                        "・root や非公開 API には依存しません。\n" +
                        "・ネットワーク通信・広告・解析 SDK は使用していません。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            SectionTitle("オープンソース")
            Text(
                text = "BatteryScope Pro はオープンソースでの公開を予定しています。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/namaemiteiii-net/BatteryScopePro")
                )
                context.startActivity(intent)
            }) {
                Text("GitHub リポジトリを開く")
            }
            Text(
                text = "https://github.com/namaemiteiii-net/BatteryScopePro",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.5f)
        )
    }
}
