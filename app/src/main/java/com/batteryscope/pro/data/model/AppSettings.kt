package com.batteryscope.pro.data.model

/**
 * テーマモード
 * - SYSTEM: OS の設定に従う（自動切替）
 * - LIGHT: 常にライト
 * - DARK: 常にダーク
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * アプリ設定（DataStore で永続化）。
 * 公称容量はユーザーが手動入力する。機種名から自動確定はしない。
 */
data class AppSettings(
    /** メーカー公称容量（mAh）。不明時は null */
    val nominalCapacityMah: Int? = null,

    /** テーマモード。初期値は SYSTEM（OS に追従） */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    /** リアルタイム更新間隔（ミリ秒） */
    val realtimeUpdateIntervalMs: Long = 2000L,

    /** 充電監視を有効にするか */
    val chargingMonitorEnabled: Boolean = true
)
