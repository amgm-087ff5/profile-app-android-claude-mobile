"""src/koke_rock.agsl を KokeRockShaders.kt に書き出す。
  python3 docs/koke/tools/src/gen_rock.py
"""
import os

here = os.path.dirname(os.path.abspath(__file__))
root = os.path.abspath(os.path.join(here, '..', '..', '..', '..'))
src = open(os.path.join(here, 'koke_rock.agsl')).read()
assert '$' not in src and '"""' not in src
out = os.path.join(root, 'app/src/main/java/com/example/businesscard/ui/koke/KokeRockShaders.kt')
kt = '''package com.example.businesscard.ui.koke

/**
 * Koke(苔むす)の部品の面: 黒い濡れた岩の AGSL シェーダー(Android 13 以上の RuntimeShader で描く)。
 *
 *  - ROCK: 角の欠けた 12 角形(凸)の面を、平らなファセット(ボロノイ)に分けて明るさを少しずつ変える。
 *    ファセットの稜線の細い光と陰、光の方を向いたファセットの控えめなつや、まばらな水滴の光点、左上を向いた縁の細い光。
 *    面から base だけずれた所に底があり、その間を薄い側面として塗る(画面の中の位置で、中心側の側面が見える)。
 *    地面には底の形の薄い接地の影を落とす。rim で縁に線(フォーカス・エラー・選択中)。
 *
 * uniform: size(部品の大きさ px), dp, seed(0..1), base(底のずれ px), press(0..1), rockLo / rockHi(ファセットの色の幅),
 * relief(ファセットの傾きの強さ), facet(ファセットの大きさ px), rim(縁の線の色と濃さ), rimWidth(px),
 * polyA..polyF(輪郭の 12 頂点、px)。返す色は premultiplied。宣言した uniform はすべて使う。
 *
 * 元のファイル: docs/koke/tools/src/koke_rock.agsl(ここは書き出したもの。直接編集しない)。
 * 手元での確認: docs/koke/tools/koke_rock.py(skia-python で同じシェーダーを CPU で描く)。
 */
object KokeRockShaders {
    const val ROCK = """''' + src + '''"""
}
'''
open(out, 'w').write(kt)
print('wrote', out)
