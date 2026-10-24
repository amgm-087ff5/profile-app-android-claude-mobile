# Koke の苔の模型

`KokeMossShaders.kt` の AGSL(MOSS)を Skia(skia-python)で CPU 実行し、平らな部品に苔が生えた所を PNG にして見本の写真と見比べる。

```
pip install --break-system-packages skia-python pillow
python3 docs/koke/tools/koke.py --out <出力フォルダ>
```

出力:
- `button.png`: 主ボタン(300x56dp、角丸 14dp、#1E2A22)の左上の角に群れ。上から「生えきった」「押した(指が群れの中)」「生える途中 growth=0.4」
- `card.png`: 名刺(340x200dp、#FAFAF7、線 #D9D7CF)の右下の角と下の辺に群れ。並びは同じ
- `*_zoom.png`: 各状態の苔の範囲を 3 倍に拡大して横に並べたもの

dp は 2.625(1080px 幅の端末)。群れの置き方(角丸の 45 度の点を基準にする)と描く矩形の求め方は `KokeMoss.kt` と同じ。

## シェーダーの元

`src/koke_moss.agsl` が元。編集したら Kotlin に書き出す:

```
python3 docs/koke/tools/src/gen.py
```

(`app/src/main/java/com/example/businesscard/ui/koke/KokeMossShaders.kt` を上書きする。`$` と `"""` は使えない)

## 描き方のあらまし

1. 群れの場 `field`: 群れごとに「縁に沿った距離 / spread」「内側への距離 / depth」「外へのはみ出し / 10dp」を合わせた遠さ q(0 = 芯、1 = 縁)。q から丸いクッションの高さを作る。押した所はへこむ。
2. 株 `tuftLayer` を 3 層(奥 → 手前、格子 4.4 / 5.5 / 6.6dp)。株ごとに葉の本数(10〜16)・葉ごとの長さ・巻き・色をばらつかせ、群れの縁(手前)ほど大きく、芯(奥)ほど小さく詰める。株の根元での q で、生える時刻(芯から縁へ)と押されたときの沈み・倒れ(指から離れる向き)を決める。
3. 光は左上から一つ。群れの起伏と株の丸みを合わせた向きで陰影、手前の株が奥に落とす影(株の間の暗がり)、群れの縁の葉先の透ける黄緑、葉の中心線のつや、葉先の水滴(下の葉が透け、左上に鋭い光。押すと強く光る)。
4. 平らな面の影: 群れの場を右下へずらして柔らかく落とす影と、縁ぎりぎりの接地の暗がり。苔の外は影だけ返して早く終わる。
