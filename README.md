# BatteryScope Pro

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com/)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Android スマートフォンのバッテリー状態を詳細に計測・分析する診断アプリです。

[AccuBattery](https://play.google.com/store/apps/details?id=com.digibites.accubattery) のように、**充電セッションからバッテリー容量・健康度を推定**でき、端末が公開する実際のバッテリー情報も確認できます。

> **ネットワーク不要・ログイン不要・広告なし。**  
> 計測データはすべて端末内にのみ保存されます。

**リポジトリ**: https://github.com/namaemiteiii-net/BatteryScopePro

---

## 主な機能

| # | 機能 | 内容 |
|---|------|------|
| 1 | **リアルタイム表示** | 残量・温度・電圧・電流・充電状態など（取得可能な範囲） |
| 2 | **容量・健康度の推定** | 充電中の電流積算と残量増加から満充電相当容量を推定 |
| 3 | **充放電サイクル数** | OS 公開値（API 34+ の一部端末）と、アプリ記録の累積等価サイクル |
| 4 | **履歴・CSV エクスポート** | Room によるローカル保存、履歴削除、CSV 出力 |

### 推定について（重要）

- 推定値であり、**実測の満充電容量ではありません**
- UI 上の「安定」は**測定の再現性**を示すものであり、正確性の保証ではありません
- 電流が取得できない端末では、充電量積算ができず容量推定ができない場合があります
- 端末自身の消費やセンサー誤差の影響を受けます

---

## スクリーンショット

（公開時に追加予定）

---

## 要件

- **Android 8.0 (API 26) 以降**
- ネットワーク接続不要
- アカウント登録・ログイン不要
- 広告 SDK・解析 SDK なし

---

## ビルド手順

### 必要なもの

- [Android Studio](https://developer.android.com/studio) Hedgehog (2023.1.1) 以降を推奨
- JDK 17

### 手順

```bash
git clone https://github.com/namaemiteiii-net/BatteryScopePro.git
cd BatteryScopePro
```

1. Android Studio で `BatteryScopePro` フォルダを **Open**
2. **Gradle Sync** を実行
3. 実機またはエミュレータを選択して **Run**

コマンドラインで Debug APK を生成する場合:

```bash
./gradlew assembleDebug
```

生成物: `app/build/outputs/apk/debug/app-debug.apk`

単体テスト:

```bash
./gradlew test
```

---

## 使い方（容量推定）

1. アプリを起動した状態で **充電を開始** する
2. 残量が十分に増えるまで待つ（目安: **15% 以上**の増加）
3. 充電を終了（または満充電）すると **セッションが記録** される
4. 有効なセッションが複数回たまると、中央値ベースで容量を推定
5. **端末・設定** 画面でメーカー公称容量（mAh）を入力すると、健康度を表示

充電中のみ、必要に応じてフォアグラウンドサービスで計測を継続します（常時バックグラウンド監視は行いません）。

---

## テーマ

| モード | 動作 |
|--------|------|
| **システム**（初期値） | OS のライト / ダーク設定に自動追従 |
| **ライト** | 常にライトモード |
| **ダーク** | 常にダークモード |

**端末・設定** 画面から切り替えできます。色はすべて `MaterialTheme.colorScheme` 経由で、モードに応じて切り替わります。

---

## 制限事項

Android の仕様やメーカー実装により、取得できる情報は端末ごとに異なります。

- 電流・サイクル数・満充電容量などは **取得できない場合がある**
- 取得できない値は **「取得不可」「未対応」** と表示し、架空の値は使いません
- **root 権限・非公開 API には依存しません**
- ネットワーク通信・広告・解析 SDK は使用していません
- バックグラウンドの常時監視は行いません（充電中のみフォアグラウンドサービス）

---

## アーキテクチャ

```
app/src/main/java/com/batteryscope/pro/
├── ui/                 # Jetpack Compose 画面 + ViewModel
│   ├── screens/        # Home / Realtime / History / Device
│   ├── components/
│   └── theme/          # Material 3（ライト・ダーク）
├── data/
│   ├── db/             # Room (DAO / Database)
│   ├── model/
│   └── repository/
├── domain/
│   ├── monitor/        # BatteryMonitor（公開 API のみ）
│   ├── estimator/      # Charge / Capacity / Health 推定
│   └── tracker/        # 充電セッション・放電・Service
└── util/               # CSV エクスポート
```

- **UI** … Compose + Material 3  
- **永続化** … Room + DataStore  
- **計測** … `BatteryManager` / `ACTION_BATTERY_CHANGED` など公式 API のみ

---

## テスト

`./gradlew test` で実行できる主な単体テスト:

| 内容 | 期待結果 |
|------|----------|
| 残量 40%→70%、充電量 1,500 mAh | 推定容量 5,000 mAh |
| 残量増加 0 | ゼロ除算せず推定無効 |
| 電流欠損 | 架空の充電量を生成しない |
| 異常なサンプル間隔 | 品質判定で検出 |
| 複数セッション | 中央値・外れ値処理 |
| 公称容量が不明 | 健康度を算出しない |
| 累積放電 10,000 mAh / 基準 5,000 mAh | 等価サイクル 2 |

---

## プライバシー

- 計測履歴・設定は **端末内のみ** に保存します
- **外部サーバーへの送信は行いません**
- 広告・トラッキング・解析 SDK は含みません

---

## コントリビューション

Issue と Pull Request を歓迎します。

1. このリポジトリをフォーク
2. 機能ブランチを作成（例: `feature/improve-estimate`）
3. 変更をコミット
4. プルリクエストを送る

大きな変更の前に、Issue で方針を共有してもらえると助かります。

---

## ライセンス

[MIT License](LICENSE)

Copyright (c) 2026 namaemiteiii-net

---

## 関連リンク

- リポジトリ: https://github.com/namaemiteiii-net/BatteryScopePro
- Issues: https://github.com/namaemiteiii-net/BatteryScopePro/issues
