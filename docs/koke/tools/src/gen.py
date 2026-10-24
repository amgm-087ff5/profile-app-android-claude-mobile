"""src/koke_moss.agsl を KokeMossShaders.kt に書き出す。
  python3 docs/koke/tools/src/gen.py
"""
import os

here = os.path.dirname(os.path.abspath(__file__))
root = os.path.abspath(os.path.join(here, '..', '..', '..', '..'))
src = open(os.path.join(here, 'koke_moss.agsl')).read()
assert '$' not in src and '"""' not in src
out = os.path.join(root, 'app/src/main/java/com/example/businesscard/ui/koke/KokeMossShaders.kt')
kt = '''package com.example.businesscard.ui.koke

/**
 * Koke(苔むす)の苔の AGSL シェーダー(Android 13 以上の RuntimeShader で描く)。
 *
 *  - MOSS: 平らな部品の一部に生える、立体的な苔の群れ。細い葉が星形に開いた株を4層、奥(群れの中央、小さく詰まる)から
 *    手前(縁と塊の上、大きく不揃い)へ重ねる。群れの中に小さな盛り上がり(塊)がある。左上からの光で株ごとに陰影を付け、株の間の暗がり、群れの縁の透ける黄緑のふち、
 *    葉先の水滴のきらめき、右下へ落ちる柔らかい影と接地の暗がり(黒い岩の上なので控えめ)を描く。
 *    押すと指の近くの株が沈んで外へ倒れ、growth(0..1)で根元から生える。
 *
 * uniform: size(部品の大きさ px), dp(1dp の px 数), seed(0..1), radius(角丸の半径 px), growth, press,
 * touch(指の位置 px), view(画面の中の位置 -1..1。苔を逆向きに最大 2.6dp ずらす), patchA..patchD(群れ = 縁の上の基準点 x, y, 縁に沿った広がり, 内側への深さ。使わない群れは 0),
 * patchH(各群れの高さ px)。返す色は premultiplied。
 * 宣言した uniform はすべて使う(使われずに消えると設定時に落ちる)。
 *
 * 元のファイル: docs/koke/tools/src/koke_moss.agsl(ここは書き出したもの。直接編集しない)。
 * 手元での確認: docs/koke/tools/koke.py(skia-python で同じシェーダーを CPU で描く)。
 */
object KokeMossShaders {
    const val MOSS = """''' + src + '''"""
}
'''
open(out, 'w').write(kt)
print('wrote', out)
