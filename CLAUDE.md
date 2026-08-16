# Hydra

Wear OS 向けの個人用水分記録。タイル → 記録画面 → スマホ companion → Health Connect の `HydrationRecord`。

- 公開しない。サイドロードのみ
- Intervo（`dev.marufeuille.intervo`）とは applicationId もリポジトリも分ける
- 仕様: `docs/spec/wear-hydration.md`

## モジュール

- `:app` — Wear OS。記録 UI・Tile。Health Connect は触らない
- `:companion` — スマホ。同じ `applicationId`。権限と `HydrationRecord` の読み書き

## 守ること

- 書き込みは Submit のときだけ。`+` / `−` はドラフト
- Health Connect への書き込みは companion のみ
- このアプリから Health Connect のレコードを削除しない
- 今日の合計は他アプリの水分も含む（`VOLUME_TOTAL`）
- 目標はウォッチ内 DataStore。初期値 2000ml
