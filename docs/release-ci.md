# タグ駆動リリース（Play 内部テスト）

`git tag vX.Y.Z && git push origin vX.Y.Z` で、app / companion の AAB を署名ビルドし、Play の**内部テスト**へ配信する。

```
git tag v0.1.0 && git push origin v0.1.0
        │
        ▼
release.yml
  1. Secrets から keystore を復元
  2. ユニットテスト（:app / :companion）
  3. :app:bundleRelease / :companion:bundleRelease
  4. Play へ配信（Wear=`wear:internal` / phone=`internal`）
  5. AAB を artifact 保存
```

`workflow_dispatch` は配信せず、復元・テスト・ビルドだけ確認できる。E2E / エミュレータは無い。Firebase も使わない。

## versionCode

`(major*10000 + minor*100 + patch) * 10 + offset`。app=0 / companion=1。`build.gradle.kts` を手で変えない。

例: `0.1.0` → Wear `1000` / phone `1001`。

ローカル確認:

```bash
VERSION_NAME=0.1.0 ./gradlew :app:bundleRelease
```

## 必要な GitHub Secrets

鍵はエージェントでは扱わない。GitHub の Settings → Secrets and variables → Actions にユーザーが登録する。

| Secret | 内容 | 作り方 |
|---|---|---|
| `KEYSTORE_BASE64` | リリース用 `.jks` の base64 | macOS: `base64 -i file.jks \| pbcopy` |
| `KEYSTORE_PASSWORD` | ストアパスワード | `keystore.properties` の `storePassword` |
| `KEY_ALIAS` | 鍵エイリアス | `keystore.properties` の `keyAlias` |
| `KEY_PASSWORD` | 鍵パスワード | `keystore.properties` の `keyPassword` |
| `PLAY_SERVICE_ACCOUNT_JSON` | Play API サービスアカウント JSON の全文 | 下記 |

`google-services.json` は不要。

Intervo と同じアップロード用キーストアを流用してよい。その場合は Intervo の Secrets と同じ値をこのリポジトリにも登録する。Play App Signing のアプリ署名鍵はアプリごとに Play が持つ。

CI は復元時に次を生成する（`build.gradle.kts` の signingConfig が読む）:

```properties
storeFile=$GITHUB_WORKSPACE/keystore-release.jks
storePassword=$KEYSTORE_PASSWORD
keyAlias=$KEY_ALIAS
keyPassword=$KEY_PASSWORD
```

`storeFile` は絶対パスにする。相対パスだと各モジュール基準で解決され、見つからない。

## 初回だけ Play Console でやること

API 配信の前に、アプリが Play 上に存在している必要がある。初回は Console から手で AAB を上げてもよい。

1. [Play Console](https://play.google.com/console) でアプリを作成。パッケージ名は `dev.marufeuille.hydra`
2. フォームファクタを **スマホ + Wear OS** にする（同一 applicationId のマルチフォームファクタ）
3. ストアの最低限（内部テストでも必要なもの）
   - アプリ名・短い説明・アイコン
   - プライバシーポリシー URL。`docs/privacy.html` を GitHub Pages（`/docs`）で出す
     - 想定 URL: `https://marufeuille.github.io/hydra/privacy.html`
     - リポジトリ Settings → Pages → Deploy from a branch → `main` / `/docs`
4. **データセーフティ**と Health Connect の宣言
   - 水分（Hydration）の読み取り・書き込み
   - 収集したデータをサーバに送らない（端末上の Health Connect のみ）
5. 内部テストのテスターに自分の Google アカウントを追加
6. サービスアカウント（下記）
7. Wear は `wear:internal`、phone は `internal`。同じリリースに両 AAB を混ぜない

### Play サービスアカウント

1. GCP でサービスアカウントを作り、JSON 鍵を発行する。中身全文を `PLAY_SERVICE_ACCOUNT_JSON` に入れる
2. Play Console の API アクセスに GCP プロジェクトをリンクし、そのサービスアカウントを追加する
3. このアプリへのリリース権限を付ける（内部テストへのアップロード）
4. `androidpublisher.googleapis.com` を有効化する。未有効だと配信が `API has not been used` で落ちる
5. 権限の反映に時間がかかることがある。403 なら伝播待ちを疑う

Intervo 用のサービスアカウントを流用してもよい。その場合は同じ JSON を Hydra の Secret にも入れ、Play Console で **hydra アプリ**への権限を付ける。

## 2 回目以降

1. `docs/release-notes-TEMPLATE.md` を `docs/release-notes-X.Y.Z.md` にコピーして最新情報を書く
2. `python3 scripts/check_release_notes.py docs/release-notes-X.Y.Z.md`
3. 対象が main に揃っていること
4. タグを打つ:

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```

5. 内部テストのリンクからウォッチとスマホに入れる

debug の `dev.marufeuille.hydra.debug` は残してよい。Play 版は接尾辞なしの `dev.marufeuille.hydra`。

### 配信前だけ検証したいとき

GitHub Actions → Release → Run workflow。`version` は任意。配信はされない。

## 注意

- 署名鍵とサービスアカウント鍵はリポジトリに置かない。`keystore.properties` / `*.jks` は gitignore 済み
- Wear と phone の AAB を 1 リリースに混ぜると Play が拒否する（`android.hardware.type.watch`）
- Wear AAB を phone 用の `internal` に出すと、既存ユーザーが上げられない、で失敗する
- 参考: [フォームファクタ別トラック](https://support.google.com/googleplay/android-developer/answer/13295490)
