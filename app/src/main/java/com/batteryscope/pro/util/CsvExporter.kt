package com.batteryscope.pro.util

import com.batteryscope.pro.data.model.CapacityEstimate
import com.batteryscope.pro.data.model.ChargingSession
import com.batteryscope.pro.data.model.DischargeLog
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 計測履歴を CSV でエクスポート。
 * UTF-8 BOM 付きで Excel での日本語文字化けを防ぐ。
 */
class CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN)

    fun exportSessions(
        sessions: List<ChargingSession>,
        estimates: List<CapacityEstimate>,
        dischargeLogs: List<DischargeLog>,
        output: OutputStream
    ) {
        OutputStreamWriter(output, StandardCharsets.UTF_8).use { writer ->
            // BOM
            writer.write("\uFEFF")

            // 充電セッション
            writer.appendLine("=== 充電セッション ===")
            writer.appendLine(
                listOf(
                    "ID", "開始日時", "終了日時", "開始残量(%)", "終了残量(%)",
                    "開始温度(℃)", "終了温度(℃)", "推定充電量(mAh)", "推定容量(mAh)",
                    "品質スコア", "品質ラベル", "サンプル数", "異常終了", "充電方式", "メモ"
                ).joinToString(",")
            )
            sessions.forEach { s ->
                writer.appendLine(
                    listOf(
                        s.id.toString(),
                        formatTime(s.startTimestampMs),
                        s.endTimestampMs?.let { formatTime(it) } ?: "",
                        s.startLevelPercent?.toString() ?: "",
                        s.endLevelPercent?.toString() ?: "",
                        s.startTemperatureC?.toString() ?: "",
                        s.endTemperatureC?.toString() ?: "",
                        s.estimatedChargeMah?.let { "%.2f".format(it) } ?: "",
                        s.estimatedCapacityMah?.let { "%.1f".format(it) } ?: "",
                        "%.3f".format(s.qualityScore),
                        escape(s.qualityLabel),
                        s.sampleCount.toString(),
                        if (s.isIncomplete) "はい" else "いいえ",
                        escape(s.chargeType ?: ""),
                        escape(s.notes ?: "")
                    ).joinToString(",")
                )
            }

            writer.appendLine()
            writer.appendLine("=== 容量推定履歴 ===")
            writer.appendLine(
                listOf(
                    "ID", "日時", "推定容量(mAh)", "有効セッション数",
                    "ばらつき(mAh)", "信頼度ラベル", "信頼度スコア", "使用セッションID"
                ).joinToString(",")
            )
            estimates.forEach { e ->
                writer.appendLine(
                    listOf(
                        e.id.toString(),
                        formatTime(e.timestampMs),
                        "%.1f".format(e.estimatedCapacityMah),
                        e.validSessionCount.toString(),
                        e.spreadMah?.let { "%.1f".format(it) } ?: "",
                        escape(e.confidenceLabel),
                        "%.3f".format(e.confidenceScore),
                        escape(e.sessionIdsUsed ?: "")
                    ).joinToString(",")
                )
            }

            writer.appendLine()
            writer.appendLine("=== 放電ログ ===")
            writer.appendLine(
                listOf("ID", "日時", "放電量(mAh)", "開始残量(%)", "終了残量(%)", "サンプル数")
                    .joinToString(",")
            )
            dischargeLogs.forEach { d ->
                writer.appendLine(
                    listOf(
                        d.id.toString(),
                        formatTime(d.timestampMs),
                        "%.2f".format(d.dischargedMah),
                        d.startLevelPercent?.toString() ?: "",
                        d.endLevelPercent?.toString() ?: "",
                        d.sampleCount.toString()
                    ).joinToString(",")
                )
            }
        }
    }

    private fun formatTime(ms: Long): String = dateFormat.format(Date(ms))

    private fun escape(s: String): String {
        return if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"${s.replace("\"", "\"\"")}\""
        } else s
    }
}
