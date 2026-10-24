import sys
d=sys.argv[1]; out=sys.argv[2]
c,r,m,s=[open(f'{d}/{n}.agsl').read() for n in ('common','rock','moss','stone')]
for x in (c,r,m,s): assert '$' not in x
kt='''package com.example.businesscard.ui.moss

/**
 * Moss(苔岩)の AGSL シェーダー(Android 13 以上の RuntimeShader で描く)。
 *
 *  - ROCK: 画面全体の背景。雨に濡れた黒い岩(溶岩のような不揃いのこぶ、こぶの頂上の濡れたつや、割れ目)、
 *    画面の下と端に這う苔の群れ、地衣類の斑点、杉の枯れ葉。画面の上ほど奥で、苔庭がぼける(浅い被写界深度)
 *  - MOSS: 苔のクッション(主ボタン・選択中)。細い葉が星形に開いた株を3層重ね、上からの光で上面を明るく、谷を黒緑に
 *  - STONE: 磨いた濡れ石の板(名刺・副ボタン・入力欄・ダイアログ)。面取りの縁のつや、曇天の映り込み、縁に生える苔
 *
 * 共通の uniform: size(描く大きさ、px), dp(1dp の px 数), seed(模様をずらす種)。
 * MOSS / STONE は (-pad..size+pad) の矩形に描き、premultiplied の色を返す(縁の苔が形の外へほつれるため)。
 * 宣言した uniform はすべて使う(コードから値を入れるので、使われずに消えると設定時に落ちる)。
 * 重い計算(苔の株・岩のこぶ)は、要る所だけで行う(苔の無い所・完全にぼけた所は飛ばす)。
 *
 * 手元での確認: docs/moss/tools/moss.py(skia-python で同じシェーダーを CPU で描く)。
 */
object MossShaders {
    private const val COMMON = """'''+c+'''"""

    const val ROCK = COMMON + """'''+r+'''"""

    const val MOSS = COMMON + """'''+m+'''"""

    const val STONE = COMMON + """'''+s+'''"""
}
'''
open(out,'w').write(kt)
