# BusinessCard

Android公式の推奨アーキテクチャを学ぶための、いちばんシンプルな名刺アプリ。

- UI: Jetpack Compose + Navigation Compose(3画面: 一覧 / 表示 / 編集)。Android 13 以上
- 表示画面は横画面固定
- 永続化: Room
- 複数の名刺を保持。レイアウトは固定で、MVPでは文字のみ編集可能(背景色・文字色の変更は今後)

## アーキテクチャ

Android公式の推奨アーキテクチャ(UI層 / ドメイン層 / データ層)に沿って、単一モジュール内をパッケージで明示的に分ける。依存の向きは `ui → domain ← data`(domainは何にも依存しない)。

```
ui (Composable / ViewModel)
        │ 呼ぶ
        ▼
domain (model / Repositoryインタフェース / UseCase)   ← 業務ルール(氏名必須、空白除去)
        ▲ 実装する
        │
data (Room: Dao・Entity / mapper / Repository実装)
```

```
com.example.businesscard
├─ BusinessCardApplication.kt   @HiltAndroidApp
├─ MainActivity.kt              @AndroidEntryPoint
├─ di/                          Hiltモジュール(DatabaseModule, RepositoryModule)
├─ domain/                      純粋なKotlin(Android非依存)
│  ├─ model/                    BusinessCard
│  ├─ repository/               BusinessCardRepository(インタフェース)
│  └─ usecase/                  Observe(s) / Save / Delete
├─ data/
│  ├─ local/                    AppDatabase, BusinessCardDao, BusinessCardEntity
│  ├─ mapper/                   Entity <-> Domain変換
│  └─ repository/               OfflineFirstBusinessCardRepository(実装)
└─ ui/
   ├─ navigation/               型安全ナビゲーション(Destinations, AppNavHost)
   ├─ list/ detail/ edit/       画面ごとに *Screen.kt と *ViewModel.kt
   ├─ component/ theme/         共通部品・デザイントークン
```

- 単方向データフロー: 状態(UiState)は下へ、イベントは上へ
- DI: Hilt。ViewModelは `@HiltViewModel`、RepositoryはインタフェースをBindsで実装に結びつける
- ナビゲーション: Navigation Compose の型安全API(`@Serializable` な遷移先 + `toRoute`)

## デザイン

デザインシステム「Soft Glass」(グレージュ版)。グレージュの壁に、下端が光るすりガラスの部品が浮き、右下に影を落とす。共有されたUIキットの画像を再現したもの。

光・影・透過は近似ではなく、AGSL シェーダー(GPU)と RenderEffect の実ブラーで計算して描く(`ui/glass/`)。そのため **Android 13 以上**。トークンは `ui/theme/`、共通部品は `ui/component/`。詳細とボタン配置のルールは [DESIGN.md](DESIGN.md)。

最新の見た目は [`screenshots` ブランチ](../../tree/screenshots) で確認できる(pushごとにCIがAndroidエミュレータで撮影)。

### 見た目の切り替え

見た目は3種類あり、一覧の右上のボタンから切り替える(選んだ見た目は DataStore に保存)。初めて開いたときは Edgelit(`ThemeStyle.DEFAULT`)。

| 見た目 | 中身 | 描き方 |
|---|---|---|
| Edgelit(すりガラス) | `ui/glass/` `ui/component/` `ui/theme/` | 上記 |
| Porcelain(陶器、仮称) | `ui/porcelain/` | 明るいグレーの板に、柔らかい影の丸いボタン。板の中ほどを波打つ板が横切り、その縁の裏に隠れた帯状の光源が奥の面を照らす。光の量(帯に沿った照度の積分・透過)を AGSL で計算し、HDR 対応の画面では光の芯を白より明るく光らせる(`PorcelainShaders.kt`)。影は BlurMaskFilter |
| Koke(苔むす) | `ui/koke/` | 角ばった黒い濡れた岩の部品に、精巧な苔が部分的に生える。苔は AGSL のシェーダーで計算して描き、押すと指の近くの株が沈んで戻る |

画面(一覧・表示・編集)は `ui/designsystem/` の共通部品(`AppButton` など)だけを使い、部品が見た目ごとの実装に振り分ける。
見た目を増やすときは `ThemeStyle` に種類を足し、`ui/designsystem/` の分岐を足す(画面は変えなくてよい)。

```
ui/designsystem/   App* の共通部品と、見た目ごとの余白(AppTheme.dimens)
ui/porcelain/      Porcelain のトークン・部品・シェーダー・カタログ
ui/settings/       見た目を選ぶダイアログ
domain/model/ThemeStyle.kt, domain/repository/UserPreferencesRepository.kt
data/preferences/  DataStore の実装
```

`theme/**` ブランチの見た目は `screenshots-<ブランチ名>` ブランチに置かれる(例: `screenshots-theme-porcelain`)。

## ビルドとテスト

```
./gradlew testDebugUnitTest   # ユニットテスト
./gradlew assembleDebug       # APK作成
./gradlew assembleDebugAndroidTest && bash scripts/device-screenshots.sh   # 接続した端末で画面を撮る
```

push時に GitHub Actions(`.github/workflows/ci.yml`)でユニットテストとビルドを行い、別のジョブで Android エミュレータを起動して画面を撮り、`screenshots` ブランチに置く。

## 今後

- 名刺ごとの背景色・文字色の変更
