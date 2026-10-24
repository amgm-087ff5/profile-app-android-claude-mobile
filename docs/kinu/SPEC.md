# Kinu(絹・仮称)— 取り決め(統括役が作成。実装担当はこれに従う)

5つ目の見た目。見本は「夜の銀座の角の外観(ライトアップ時)」の写真。
ブランド名・ロゴ・文字は一切描かない。素材・光・形だけを使う。

## 1. 見立て(ひとことで)

黒い塔の足元に、**絹のドレープのように波打つ淡い金の帯**が幾重にも重なる。
各帯の**上の縁に細く強い光の線**(HDR で白より明るい)が仕込まれ、帯の面を舐めるように照らす。
帯と帯の間のガラスの奥は**琥珀色に灯る**(天井の灯り・薄いカーテン・縦の桟)。
ガラスには青海波(扇を連ねた模様)のごく淡いセラミック印刷。上の塔は黒く、銀の細い線が縦に波打つ。

部品への対応:

| 写真の要素 | UI の部品 |
| --- | --- |
| 黒い塔+銀の縦の波線 | 画面の背景(上〜中) |
| 金の帯+上縁の光の線 | 主ボタン、名刺・ダイアログの上下の縁(リップ)、選択中の印 |
| 琥珀に灯る窓 | 名刺の面、入力中の欄の奥のにじみ |
| 黒いガラス(夜) | 副ボタン、アイコンボタン、入力欄、ダイアログの面 |
| 足元の帯の重なり | 背景の下 22% (下の操作ボタンの奥) |

## 2. 採寸(見本の写真 1449×2576 から。2026-10-25)

- 空(もや): #43443E / 塔の地: #121216〜#212026 / 塔の銀の線: #5E5D65〜#63626A(細い・低コントラスト)
- 金の帯: 光の当たる所 #CC9871〜#F3CAAE / 陰の面 #795C4C〜#7C624B(赤みのある茶)/ 最も暗い所 #654013
- 光の線: 写真では白飛び #FFFACD〜#FFFFC1(=HDR で白を超える所)。線の際は #EBA874
- 窓(琥珀): #ECB272、明るい所 #FFDEAA〜#FFE2C5、奥の暗い所 #997255
- 足元のガラス: #514545

値は `app/src/main/java/com/example/businesscard/ui/kinu/KinuTokens.kt` に定義済み(統括役)。**色を直接書かず必ずトークンを使う**。

## 3. 光の模型(全シェーダー共通)

- 計算は線形の光で行い、最後に「ソフトニー」で丸める: `toneMap(c, hdr)`。
  - 通常の画面(hdr=1): 光の芯は #FFF6E0(クリーム白)で止まる。白く飛ばすが縞を出さない
  - HDR 対応の画面(hdr>1): 光の芯は白の `hdr` 倍まで(Porcelain と同じ扱い。`LocalGlowHeadroom` の値をそのまま uniform `hdr` に渡す)
  - 8bit の縞を消すディザ(±0.5段の三角分布)を最後に入れる(Porcelain の教訓)
- 光の線(cove line): 帯の上の縁に沿う細い線光源。芯の幅 1.2dp、まわりのにじみ(グレア)は縁の**上側**へ 3dp・下側の金属面へは「面を舐める光」として 30% の高さまで。線の強さ `line` は 0..1.6(1=通常、押すと 1.6)
- 金属(帯): 断面は上に膨らんだ布のドレープ(上 20% で丸く回り込み、下へ向かって緩く凹む)。
  照度 = 上縁の線光源からのかすめる光(上ほど強く、陰の面 #795C4C へ落ちる)+ 下からの琥珀の照り返し(下 30% を #CC9871 へ)。
  つや: 横方向に伸びる異方性のハイライト(ヘアライン)を、ゆるい波(1部品あたり 0.5〜1.5 周期)でうねらせる
- 窓(琥珀): 上から下へ #FFDEAA→#ECB272→#997255。上 18% に天井の灯りの列(点光源を横に並べ、HDR で少し白を超える)、
  縦の薄いカーテンのひだ(柔らかい縦縞・振幅 6%)、縦の桟(4分割、幅 1dp、#7A5A40 30%)、
  手前のガラスに青海波の点描(不透明度 6%。扇の半径 10dp)
- 夜のガラス: #16161A〜#1E1E23。上の縁に細い光の線(弱め、`line` 0.35 が通常)。
  面にごく淡く塔の銀の縦の波線が映り込む(不透明度 5%)。縁は 1dp の #8B7A62 40%

## 4. 形・動き

- 角丸: ボタン・入力欄 14dp / 名刺 22dp / ダイアログ 26dp(建物の大きく丸い角)。pill にはしない
- 名刺の上下の縁: 高さ 10dp の金の帯(リップ)。下の縁はゆるく波打つ(振幅 3dp・1周期)
- 押す: 光の線が 1.0→1.6 に強まり(HDR で白を超える)、つやが指の x へ寄る。大きさ・位置は変えない(沈まない・浮かない)。**押した感触は要確認**
- 登場: 画面を開くと光の線が左から右へ 600ms で灯る(`ignite` 0→1)。「アニメーションを削除」設定では最初から 1
- 文字: タイトル・名刺の名前は明朝(`FontFamily.Serif`)。ラベルは字間を広めに(0.08em)

## 5. ファイルと担当(同じ作業ツリーを並行して触るので、**自分の担当ファイル以外は編集しない**)

| 担当 | ファイル |
| --- | --- |
| 統括役 | `docs/kinu/SPEC.md`, `ui/kinu/KinuTokens.kt` |
| A: 組み込み(Haiku) | `domain/model/ThemeStyle.kt`, `ui/designsystem/*`, `ui/settings/*`, `res/values/strings.xml`, `ui/kinu/KinuThemeHost.kt`, `docs/kinu/tools/gen.py`, `app/src/androidTest/.../KinuScreenshotTest.kt`, `scripts/device-screenshots.sh`, 単体テスト, README |
| B: 背景シェーダー(Haiku) | `docs/kinu/tools/src/background.agsl`, `docs/kinu/tools/preview_background.py` |
| C: 部品シェーダー(Haiku) | `docs/kinu/tools/src/ribbon.agsl`, `window.agsl`, `nightglass.agsl`, `docs/kinu/tools/preview_surfaces.py` |
| D: Compose の部品(Haiku) | `ui/kinu/KinuSurface.kt`, `KinuBackground.kt`, `KinuControls.kt`, `KinuBusinessCard.kt`, `KinuCatalog.kt` |

`KinuShaders.kt` は gen.py(A)が `docs/kinu/tools/src/*.agsl` から書き出す(手で編集しない)。
中身は `internal object KinuShaders { const val BACKGROUND = """…"""; const val RIBBON …; const val WINDOW …; const val NIGHT_GLASS … }`。
B・C が作業中は .agsl が空でも、A は gen.py を先に作り、空のファイルからも書き出せるようにしておく。

## 6. シェーダーの uniform(名前・型を固定。B/C は必ずこの名前で宣言し、D はこの名前で設定する)

すべて AGSL。`half4 main(float2 p)`。p はその部品(または画面)の左上を原点とするピクセル。

### BACKGROUND(画面全体)
```
uniform float2 size;       // 画面の大きさ(px)
uniform float  dp;         // 1dp の px
uniform float  facadeTop;  // 足元の帯が始まる y(px)。これより下に帯と窓が重なる
uniform float  ignite;     // 0..1 光の線が左から灯る
uniform float  hdr;        // HDR の余裕(1=通常)
layout(color) uniform half4 tower;    // KinuColors.tower
layout(color) uniform half4 fin;      // KinuColors.fin
layout(color) uniform half4 goldLit;  // KinuColors.goldLit
layout(color) uniform half4 goldShade;// KinuColors.goldShade
layout(color) uniform half4 amber;    // KinuColors.amber
layout(color) uniform half4 lineCore; // KinuColors.lineCore
```
描くもの: 上=もやの空 → 黒い塔(縦の銀の波線: 間隔 11dp、幅 0.8dp、うねり振幅 2.5dp・波長 140dp、線ごとに位相をずらす。下ほどわずかに暖かく照らされる)。
`facadeTop` より下: 金の帯2〜3本(高さ 26dp、上下の縁がゆるく波打つ、各帯の上縁に光の線)と、その間の琥珀の窓(高さ 30dp)。
帯の光が塔の下端を暖かく照らす(上へ 60dp で減衰)。

### RIBBON(金の帯の面。主ボタン・名刺の縁・選択中の印)
```
uniform float2 size; uniform float dp; uniform float hdr;
uniform float  line;     // 光の線の強さ 0..1.6
uniform float  ignite;   // 0..1(左から灯る)
uniform float2 touch;    // 指の位置(px)。触れていないとき (-1,-1)
uniform float  seed;     // 部品ごとの位相(つやのうねり)
layout(color) uniform half4 goldLit; layout(color) uniform half4 goldShade;
layout(color) uniform half4 goldDeep; layout(color) uniform half4 lineCore;
```
形の切り抜き(角丸)は Compose 側(D)が clip するので、シェーダーは矩形いっぱいに描く。

### WINDOW(琥珀の窓。名刺の面)
```
uniform float2 size; uniform float dp; uniform float hdr;
uniform float  glow;     // 0..1 灯りの強さ(登場で 0→1)
uniform float  seed;
layout(color) uniform half4 amberHi; layout(color) uniform half4 amber; layout(color) uniform half4 amberDeep;
layout(color) uniform half4 mullion;
```

### NIGHT_GLASS(夜のガラス。副ボタン・アイコンボタン・入力欄・ダイアログ)
```
uniform float2 size; uniform float dp; uniform float hdr;
uniform float  line;     // 上の縁の光の線 0..1.6(通常 0.35、入力中 1.0、押すと +0.5)
uniform float  ember;    // 0..1 奥の琥珀のにじみ(入力中の欄)
uniform float2 touch;
layout(color) uniform half4 glassTop; layout(color) uniform half4 glassBottom;
layout(color) uniform half4 fin; layout(color) uniform half4 lineCore; layout(color) uniform half4 amber;
```

## 7. Compose 側の窓口(D が実装。A の AppComponents がこれを呼ぶ。Porcelain と同じ並び)

```kotlin
enum class KinuTone { Gold, Night }
fun KinuButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tone: KinuTone = KinuTone.Gold, leadingIcon: ImageVector? = null, contentColor: Color = <toneの既定>, enabled: Boolean = true)
fun KinuIconButton(onClick: () -> Unit, icon: ImageVector, contentDescription: String, modifier: Modifier = Modifier, tone: KinuTone = KinuTone.Night)
fun KinuTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, placeholder: String? = null, leadingIcon: ImageVector? = null, isError: Boolean = false, errorText: String? = null, keyboardOptions: KeyboardOptions = KeyboardOptions.Default)
fun KinuTopBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actions: (@Composable RowScope.() -> Unit)? = null)
fun KinuMessage(text: String, modifier: Modifier = Modifier)
fun KinuChoice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)
fun KinuDialog(title: String, onDismiss: () -> Unit, message: String? = null, content: @Composable ColumnScope.() -> Unit)
fun KinuConfirmDialog(title: String, message: String, confirmLabel: String, dismissLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit)
fun KinuBusinessCard(card: BusinessCard, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null)
fun KinuBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)
fun KinuScaffold(modifier: Modifier = Modifier, topBar: @Composable () -> Unit = {}, bottomBar: @Composable () -> Unit = {}, content: @Composable (PaddingValues) -> Unit)
fun KinuCatalog(modifier: Modifier = Modifier)   // 全部品を並べた確認用画面
```
AppButtonStyle の対応: Primary→Gold、Secondary→Night、Danger→Night(文字 `KinuColors.danger`)。

## 8. 引き継ぐノウハウ(過去の見た目から)

- シェーダーは `RuntimeShader`(minSdk 33)。HDR は `LocalGlowHeadroom`(ui/glass)を読む。Porcelain の `PorcelainBackground.kt` / `PorcelainSurface.kt` の描き方が手本
- 重い背景は `drawWithCache` で shader を作り、uniform の更新だけを毎フレーム行う。画面全体のシェーダーはループ回数を抑える(Moss で撮影が時間切れになった)
- skia-python(`pip install skia-python`)で同じ AGSL を CPU 描画して確認できる(`skia.RuntimeEffect.MakeForShader`)。HDR の確認は F16 のサーフェスで hdr=3
- 見た目は縮小画像だけでなく等倍・拡大で確認する。勝手に部品の形・立体感を変えない
- このクラウドでは Gradle の依存が取れずビルドできない。ビルド・テスト・撮影は push 後の GitHub Actions(統括役が push する。担当は commit・push しない)
