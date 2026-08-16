# Hydra

Wear OS 向けの個人用水分記録アプリ。タイルから飲む量を合わせて、スマホの companion が Health Connect の `HydrationRecord` に書く。

Intervo とは別アプリ。Play 公開はしない（サイドロード前提）。

## 構成

| モジュール | 役割 |
|---|---|
| `:app` | ウォッチ。タイル / 記録 / 目標。Submit は Data Layer で送るだけ |
| `:companion` | スマホ。Health Connect の権限と書き込み。今日の合計をウォッチへ返す |

`applicationId` は両方とも `dev.marufeuille.hydra`（debug は `.debug`）。Wear の Data Layer が同じパッケージ名を要求するため。

## できること

- タイルに今日の摂取量 / 目標と、左 0%・右 100% の半円ゲージ
- 記録画面で `+` / `−`（未送信のドラフトのみ）→ Submit で 1 件送信
- ウォッチ設定で 1 日の目標（初期値 2000ml）
- スマホの Hydra で Health Connect の水分読み取り・書き込みを許可

## やらないこと

- 履歴一覧、記録の削除、リマインダー
- `+` / `−` のたびに Health Connect へ書くこと
- Intervo や pulse-board への混在

仕様と操作モックは [docs/spec/wear-hydration.md](docs/spec/wear-hydration.md)。

## ビルドと実機

ウォッチ:

```bash
./gradlew :app:assembleDebug
adb -s <watch-serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

スマホ（USB またはワイヤレス）:

```bash
./gradlew :companion:assembleDebug
adb -s <phone-serial> install -r companion/build/outputs/apk/debug/companion-debug.apk
```

入れたあと:

1. スマホで **Hydra** を開く → **許可する** → 水分の読み取り・書き込みを許可
2. ウォッチのタイルを追加。連携済みなら今日の量が表示される

ユニットテスト:

```bash
./gradlew :app:testDebugUnitTest :companion:testDebugUnitTest
```
