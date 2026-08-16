# Hydra

Wear OS 向けの個人用水分記録。タイル → 記録画面 → スマホ companion → Health Connect の `HydrationRecord`。

- Intervo（`dev.marufeuille.intervo`）とは applicationId もリポジトリも分ける
- 仕様: `docs/spec/wear-hydration.md`
- 配信: Play **内部テスト**（製品版公開はしない）

## モジュール

- `:app` — Wear OS。記録 UI・Tile。Health Connect は触らない
- `:companion` — スマホ。同じ `applicationId`。権限と `HydrationRecord` の読み書き

## リリース（タグ駆動）

`git tag vX.Y.Z && git push origin vX.Y.Z` で署名ビルドし、Play 内部テストへ出す（`.github/workflows/release.yml`）。手順と Secrets は `docs/release-ci.md`。

タグを打つ前に:

1. `docs/release-notes-<VERSION>.md` を用意する（テンプレは `docs/release-notes-TEMPLATE.md`）
2. `python3 scripts/check_release_notes.py docs/release-notes-<VERSION>.md`
3. 対象が main に揃っていること

versionCode は semver から自動採番。`build.gradle.kts` を手で変えない。配信は Wear=`wear:internal` / phone=`internal`。署名鍵はエージェントでは扱わない。

## 守ること

- 書き込みは Submit のときだけ。`+` / `−` はドラフト
- Health Connect への書き込みは companion のみ
- このアプリから Health Connect のレコードを削除しない
- 今日の合計は他アプリの水分も含む（`VOLUME_TOTAL`）
- 目標はウォッチ内 DataStore。初期値 2000ml
