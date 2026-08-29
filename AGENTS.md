# Hydra エージェント向け手順

## 初回セットアップ

テストの前に Android SDK の場所を確認する。`ANDROID_HOME` が未設定で、macOS の
標準パスに SDK がある場合は、次の手順で `local.properties` を作成する。
`local.properties` はマシン固有の設定なので、コミットしない。

```bash
sdk_dir="${ANDROID_HOME:-}"
if [ -z "$sdk_dir" ] && [ "$(uname -s)" = "Darwin" ] && [ -d "$HOME/Library/Android/sdk" ]; then
  sdk_dir="$HOME/Library/Android/sdk"
fi
if [ -z "$sdk_dir" ] || [ ! -d "$sdk_dir" ]; then
  echo "Android SDK が見つかりません。ANDROID_HOME または SDK の場所を設定してください。" >&2
  exit 1
fi
printf 'sdk.dir=%s\n' "$sdk_dir" > local.properties
```

## 変更後のテスト

コード、テスト、ビルド設定、ドキュメントのいずれを変更した場合も、完了前に必ず
ユニットテストを実行する。テストが成功するまで完了扱いにしない。

変更範囲に応じて、次のコマンドを使う。

```bash
# :app の変更
./gradlew :app:testDebugUnitTest --configuration-cache

# :companion の変更
./gradlew :companion:testDebugUnitTest --configuration-cache

# ルート設定、依存関係、または両モジュールに影響する変更
./gradlew :app:testDebugUnitTest :companion:testDebugUnitTest --configuration-cache
```

Android SDK が見つからない場合は、`ANDROID_HOME` または `local.properties` の
`sdk.dir` を設定してからテストを再実行する。環境を直せない場合は、テスト未実行の
まま成功と報告せず、SDK 未設定を明記して完了を止める。

アプリのコードを変更した場合は、テストに加えて CI と同じく必要なモジュールの
`assembleDebug` と `lintDebug` も実行する。

## PR 作成

テストが成功した変更は、作業ブランチをリモートへ push してから GitHub CLI で PR
を作成する。PR の base は `main` とし、タイトルには変更の目的を、本文には概要と
テスト結果を記載する。テストが失敗または未実行の状態で PR を作成しない。

```bash
git add <変更したファイル>
git commit -m "<変更の目的>"
git push -u origin HEAD
gh pr create --base main --title "<PRタイトル>" --body "<概要とテスト結果>"
```

Linear に紐づく作業では、PR 作成後に Linear の issue へ PR URL を添付する。
