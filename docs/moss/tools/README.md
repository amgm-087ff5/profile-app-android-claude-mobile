# Moss の模型

`MossShaders.kt` の AGSL を Skia(skia-python)で CPU 実行し、背景の岩と部品(苔のボタン・石の板)を PNG にして見本と見比べる。

```
pip install --break-system-packages skia-python pillow
python3 docs/moss/tools/moss.py --out <出力フォルダ> --ref <見本の jpg のフォルダ>
```

出力: `rock.png`(背景 1080x2400)、`moss_button.png` / `stone_button.png` / `stone_card.png` / `stone_field_sunken.png`(押していない状態と押した状態を上下に並べたもの)、`compare.png`(見本との比較)。

シェーダーの元は `src/*.agsl`(common = 共通の関数、rock / moss / stone = 各シェーダー)。
編集したら `python3 docs/moss/tools/src/gen.py docs/moss/tools/src app/src/main/java/com/example/businesscard/ui/moss/MossShaders.kt` で Kotlin に書き出す。
