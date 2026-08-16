# Hydra

Wear OS 向けの個人用水分記録アプリ。タイルから飲む量を合わせて、スマホの companion が Health Connect の `HydrationRecord` に書く。

Intervo とは別アプリ。製品版公開はせず、**Play 内部テスト**で自分用に入れる。

## 構成

| モジュール | 役割 |
|---|---|
| `:app` | ウォッチ。タイル / 記録 / 目標。Submit は Data Layer で送るだけ |
| `:companion` | スマホ。Health Connect の権限と書き込み。今日の合計をウォッチへ返す |

`applicationId` は両方とも `dev.marufeuille.hydra`（debug は `.debug`）。

## できること

- タイルに今日の摂取量 / 目標と、左 0%・右 100% の半円ゲージ
- 記録画面で `+` / `−`（未送信のドラフトのみ）→ Submit で 1 件送信
- ウォッチ設定で 1 日の目標（初期値 2000ml）
- スマホの Hydra で Health Connect の水分読み取り・書き込みを許可

仕様と操作モックは [docs/spec/wear-hydration.md](docs/spec/wear-hydration.md)。配信手順は [docs/release-ci.md](docs/release-ci.md)。

## 開発用（debug サイドロード）

```bash
./gradlew :app:assembleDebug :companion:assembleDebug
adb -s <watch> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <phone> install -r companion/build/outputs/apk/debug/companion-debug.apk
```

Play 内部テスト版（`dev.marufeuille.hydra`）とは別アプリとして共存する。

入れたあと:

1. スマホで **Hydra** を開く → **許可する** → 水分の読み取り・書き込みを許可
2. ウォッチの文字盤を長押し → タイルを追加 → Hydra

## リリース

```bash
# ノートを書いてから
git tag v0.1.0
git push origin v0.1.0
```

内部テストのリンクからウォッチとスマホに入る。
