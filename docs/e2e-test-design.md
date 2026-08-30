# Hydra E2E テスト設計

> 状態: 設計案  
> 対象: `:app`（Wear OS）と `:companion`（スマートフォン）  
> 関連: [Wear タイルから水分を Health Connect に記録する仕様](./spec/wear-hydration.md)

## 1. 目的と結論

このアプリの「E2E」は、1台の端末だけで完結するUIテストと、ウォッチ・スマートフォン・Health Connectを実際につなぐ連携テストを分けて考える。

| 範囲 | 実現性 | 方針 |
|---|---|---|
| ウォッチの記録・設定画面 | 高い | Composeのinstrumentation test。テスト用のRepositoryを注入して決定的に実行する |
| ウォッチのタイル | 中〜高 | `TileUiModel` はunit test、タイルのレイアウトと起動先はinstrumentationまたはTile host上で確認する |
| スマートフォンの権限画面 | 高い | Health Connectが利用可能な端末でUIを確認する。OS所有の権限ダイアログ自体は最小限に扱う |
| ウォッチ ↔ スマートフォンのData Layer | 中程度 | ペアリング済みの2台でsmoke test。通常のPR CIでは無理に実行しない |
| スマートフォン ↔ Health Connect | 中程度 | 専用のクリーンなエミュレータで、権限・書き込み・合計値を確認する |
| 通知と再起動 | 低〜中 | 時刻計算はunit test、AlarmManager・再起動は専用のinstrumentation smoke testに分ける |

最初に自動化する最小セットは、ウォッチの主要操作、スマートフォンの権限表示、Submitの1件性を対象にする。2端末の連携とHealth Connect実体への書き込みは、ペアリング済み環境での手動または夜間E2Eとして追加する。

## 2. 現行実装から見たテスト境界

### 2.1 そのまま自動化しやすい境界

- `RecordContent` と `SettingsContent` は状態とコールバックを引数に取るため、Compose UI testから画面表示と操作を検証できる。
- `HydrationRepository` は `GoalDraftStore`、`HydrationSender`、`HydrationReminder` を受け取るため、ウォッチ側のSubmit・ドラフト・目標変更はfakeで再現できる。
- ゲージ角度、ドラフトの上下限、日付変更、HealthStatusは既存のdomain関数とunit testで決定的に検証できる。
- `HydrationTileService` の表示モデルは `tileUiModel` に分離されている。今日の量、目標、ゲージ角度、権限状態による起動先はunit testで確認できる。
- `PendingSipStore` の同一ID排除は、Health Connectを起動せずにunit testで確認できる。

### 2.2 実端末でないと確認できない境界

- `Wearable.getDataClient()` によるData Layerの配送、遅延、再配送、端末間の到達。
- `HealthConnectClient` の権限状態、`HydrationRecord` の書き込み、`VOLUME_TOTAL` の集計。
- Tile hostが実際にタイルを表示し、タップで正しいActivityを起動すること。
- Androidの通知権限、通知チャンネル、AlarmManager、端末再起動後の再予約。

同じsip IDの再配送は、pendingに残っている間の重複排除だけでは不十分である。pendingから削除した後に同じデータが再配送された場合の扱いは、Health Connectのclient record IDの挙動を含めてLevel 3で確認し、必要なら処理済みIDの永続化などを別途設計する。E2EケースX-02は、現行実装が必ず通ることを前提にせず、重複記録を検出する回帰テストとする。

現在は `HydraApplication` と `CompanionRepository` が本番依存を直接生成しているため、Activityを起動するだけのE2Eから実際の送受信を制御することは難しい。実端末テストを追加する前に、後述のテスト用Composition Rootを用意する。

現状のテスト基盤は、`:app` にCompose UI test用の依存関係がある一方、`:companion` にはinstrumentation test用の依存関係がなく、どちらにも `androidTest` のケースはまだない。また、現在のCIはunit test・build・lintのみを実行し、接続済み端末のテストは実行していない。

## 3. テストレベル

### Level 1: domain / repository unit test

高速で、PRごとに必ず実行する。E2Eの前提となる不変条件をここで固定する。

- `+` / `−` はHealth Connectへ送信せず、100ml単位でドラフトだけを変更する
- ドラフトの0mlではSubmitしない
- Submitは1回につき1件を送り、成功時だけドラフトを100mlへ戻す
- 送信失敗・権限なしではドラフトを維持する
- 目標の範囲、日付をまたいだドラフトリセット
- 半円ゲージの左0%、中央50%、右100%、100%超過時のクランプ
- 21:00〜翌06:00の通知繰り下げ
- 同じsip IDをpending queueに重複保存しない

既存の `:app:testDebugUnitTest` と `:companion:testDebugUnitTest` をこのレベルとする。外部サービスをmockしているため、Health Connectへの実書き込みを証明するテストではない。

### Level 2: 端末内UI instrumentation test

1台のエミュレータ上でActivityを起動し、Compose semanticsを通して操作する。

対象:

- 記録画面の初期表示、ドラフト表示、`+` / `−`、Submitのenabled状態
- `+` / `−` では今日の合計とゲージが変わらないこと
- Submit成功後にドラフトが100mlへ戻り、今日の合計が増えること
- Submit失敗時にドラフトが維持され、エラーメッセージが出ること
- 目標の変更、上下限、画面を離れて戻った後の保持
- 権限なし・スマートフォンなしの状態表示と設定画面への遷移
- コンパニオンのHealth Connect状態表示、許可ボタンのenabled状態

画面の実装詳細に依存しないよう、以下のような安定したsemantics識別子を付ける。

| 画面 | 識別子の例 |
|---|---|
| 記録 | `record_today`, `record_draft`, `record_minus`, `record_plus`, `record_submit` |
| 設定 | `settings_goal`, `settings_minus`, `settings_plus`, `settings_back` |
| コンパニオン | `health_connect_status`, `health_connect_permission` |

表示文言だけをセレクタにすると、日本語の文言変更や記号フォント変更でテストが壊れる。`Submit` のような固定文言は補助的なassertionに留める。

### Level 3: 2端末連携E2E

ペアリングしたWear OSエミュレータとスマートフォンエミュレータに、それぞれdebug APKを入れて実行する。

基本フロー:

1. Health Connectが利用可能なスマートフォンで、読み取り・書き込み権限を許可する
2. ウォッチとスマートフォンをWear OS Pairing Assistantでペアリングする
3. コンパニオンがウォッチへ `available=true`、`permitted=true`、今日の合計を返すことを確認する
4. ウォッチでドラフトを300mlまで増やしてSubmitする
5. `/hydration/sip/<id>` がコンパニオンに届くことを確認する
6. コンパニオンがHealth Connectへ1件を書き込み、pendingから削除することを確認する
7. `/hydration/status` がウォッチへ戻り、今日の合計とタイルが更新されることを確認する
8. 同じsipが再配送されても、Health Connectの増分が1件分に留まることを確認する

このレベルでは非同期処理が多いため、固定sleepではなく、画面の状態・Health Connectの合計・Data Layerの到達を一定時間pollして待つ。タイムアウト時は、ウォッチ・スマートフォン双方のlogcatとpending状態を保存する。

### Level 4: OS / ライフサイクル smoke test

全ケースを毎回実行せず、代表ケースだけを実端末で確認する。

- 通知権限を拒否した場合、記録画面が操作不能にならず、アプリ本体の記録フローは継続できる
- Health Connect権限を拒否した場合、Submitせずドラフトを保持する
- 権限を後から許可した場合、pendingのsipが1回だけflushされる
- ウォッチのプロセス終了・再起動後も目標とドラフトの保存規則が守られる
- 端末再起動後に通知予約が再開する
- 21:00〜翌06:00に通知を出さず、06:00へ繰り下げる

時刻を使うケースは、可能ならテスト用Clockとテスト用ReminderSchedulerを注入する。端末時刻の変更だけに依存すると、エミュレータやCIのタイムゾーン差で不安定になる。

## 4. 受け入れ条件との対応

| ID | シナリオ | 主なassertion | レベル |
|---|---|---|---|
| W-01 | タイルに今日の量・目標・半円を表示し、タップする | `today / goal`、ゲージ角度、記録画面起動 | 1 + 2/3 |
| W-02 | 権限なしのタイルをタップする | 「許可が必要」または「スマホが必要」、設定画面起動 | 1 + 2 |
| W-03 | `+` を2回押す | ドラフトが100→300ml、今日の量とゲージは不変 | 1 + 2 |
| W-04 | 300mlでSubmitする | Health Connectへの増分が300ml、1件、ドラフトが100ml | 1 + 3 |
| W-05 | 0mlでSubmitする | ボタン無効、送信なし、過去の記録も不変 | 1 + 2 |
| W-06 | `−` を押す | ドラフトだけ減り、既存レコードは削除されない | 1 + 2/3 |
| W-07 | 目標を変更する | 100ml単位、100〜5000ml、再表示後も保持 | 1 + 2 |
| W-08 | 1000/2000mlと2200/2000mlを表示する | 中央90度、超過しても180度で停止 | 1、必要ならスクリーンショット |
| C-01 | コンパニオンで権限を許可する | 未連携→連携済み、今日の合計表示 | 2 + 3 |
| C-02 | 権限なしでsipを受け、後から許可する | pending保持→許可後に1回だけ書き込み | 1 + 3/4 |
| X-01 | ウォッチからSubmitしてHealth Connectまで届ける | sip、HydrationRecord、status更新の一連の到達 | 3 |
| X-02 | 同じsipを再配送する | 同じIDが二重に記録されない | 1 + 3 |
| N-01 | 非通知時間帯にアラームを発火する | 通知なし、翌06:00に再予約 | 1 + 4 |

## 5. テスト用Composition Root

### ウォッチ

`HydraApplication` の本番用依存生成と、instrumentation用の依存生成を分ける。テスト側では以下を差し替えられるようにする。

- `GoalDraftStore`: in-memory store
- `HydrationSender`: 呼び出し履歴を記録し、statusを任意に返すfake
- `HydrationReminder`: 予約履歴だけを記録するfake
- 時刻: 固定Clock

これにより、UI testで「Submitを押したがData Layerが失敗した」「権限なし」「statusが遅れて届く」を実時間や実端末の状態に依存せず再現できる。本番コードの`AppContainer`を直接書き換えるのではなく、テスト用Applicationまたはdebug専用の差し替え入口を用意する。

### スマートフォン

`CompanionRepository` は現在、Health Connect、Data Layer、pending storeを内部で生成している。次の境界をconstructorまたはテスト用factoryから差し替えられるようにする。

- Health Connectのhydration gateway
- Wearable DataClient
- `PendingSipStore`
- 時刻取得

なお、fake gatewayで検証できるのはpending処理と送信制御であり、実際の`HydrationRecord`形式や権限は検証できない。Level 3では本物のHealth Connectを使うテストを別に残す。

## 6. Health Connectテストの分離とデータ方針

このアプリはHealth Connectの既存レコードを削除しない。また、今日の合計は他アプリの水分も含む。そのため、E2Eで合計値を常に0からの絶対値としてassertしてはいけない。

- テスト開始時の今日の合計を `beforeMl` として取得する
- テスト後は `afterMl - beforeMl == expectedMl` を確認する
- テストは1つの専用Health Connectプロファイルまたは使い捨てエミュレータで直列実行する
- 前回テストのデータをアプリから削除して再利用しない
- 失敗時の後処理はテスト環境の再作成・Health Connect側の手動整理とし、本番アプリの削除APIを追加しない
- Health Connect権限ダイアログはOSのUIなので、文言やボタン配置を細かくassertしない

現在の実装では書き込み時の`clientRecordId`にUUIDを使っている。重複防止のE2EはIDそのものを画面で探すのではなく、同じsipの再配送後に合計が二重に増えないことと、pendingが空になることを確認する。

## 7. 実行環境とCI段階

### ローカル

既存の `scripts/setup_emulator.sh` はAPI 36のウォッチ・スマートフォンAVDを作成し、debug APKをインストールして起動する。ただし、現状は次の処理を行わない。

- 2台のペアリング
- Health Connectの権限付与
- instrumentation testの実行

したがって、Level 3を手元で実行する場合は、スクリプト実行後にPairing Assistantでペアリングし、スマートフォンでHealth Connect権限を許可する。テストランナー追加後の目標コマンドは次のとおり。

```bash
./gradlew :app:connectedDebugAndroidTest \
  :companion:connectedDebugAndroidTest \
  --configuration-cache
```

### CI

段階的に次の運用にする。

1. **PR必須**: 既存のunit test、`assembleDebug`、`lintDebug`
2. **PRまたは専用workflow**: 単一端末のCompose instrumentation smoke test
3. **夜間・手動workflow**: ペアリング済み2端末とHealth Connectを使うLevel 3/4
4. **リリース前**: 最小連携E2Eを成功させ、Play内部テストへ出す

Wear OSとHealth Connectを含む2端末CIは、AVDの起動、ペアリング、OS権限、実行時間の影響を受けやすい。最初から通常PRの必須ジョブにすると、製品ロジックの変更がないPRまで環境要因で止まるため、まずは単一端末の決定的なテストと夜間の実連携テストを分ける。

## 8. 不安定化を防ぐルール

- 固定sleepではなく、状態の変化をpollし、全体のタイムアウトを設ける
- Health Connectの合計値は必ずテスト開始時との差分で確認する
- テスト間でData Layerのsip ID、pending store、Health Connectプロファイルを共有しない
- 実端末E2Eでは同時実行しない
- UI selectorは表示文言やレイアウトではなくsemantics識別子を優先する
- 通知時刻はテスト用Clockで固定し、端末の壁時計だけに依存しない
- 失敗時に両端末のlogcat、画面状態、Health Connectの権限状態を保存する
- テストのために本番アプリへHealth Connect削除機能や本番専用のバイパスを追加しない
- UI E2Eで確認したロジックをunit testにも重複して大量に書かず、各レベルの責務を分ける

## 9. 実装順序

1. `:app` と `:companion` に安定したsemantics識別子を追加する
2. Application / Repositoryのテスト用Composition RootとClockの差し替え口を追加する
3. ウォッチ記録・設定画面のLevel 2 smoke testを追加する
4. コンパニオン権限画面のLevel 2 smoke testを追加する
5. Data Layerのfakeを使ったpending・再送のinstrumentation testを追加する
6. ペアリング済み2端末でLevel 3のSubmit→Health Connect→status戻りを実装する
7. 通知と再起動の代表ケースをLevel 4として追加する
8. ローカルセットアップと専用workflowに実行手順、タイムアウト、失敗時artifactを組み込む

