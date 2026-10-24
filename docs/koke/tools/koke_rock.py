"""Koke の岩の部品(KokeRockShaders.kt の ROCK)を skia-python(CPU)で描いて PNG にする。

  python3 docs/koke/tools/koke_rock.py --out <出力フォルダ>

主ボタン・副ボタン・入力欄・名刺を、画面の上・中央・下に置いたとき(view.y = -0.8 / 0 / +0.8)の3通り、
横に並べて描く(等倍)。苔は koke.py の draw_moss(KokeMoss.kt と同じ決まり)で重ねる。
輪郭の多角形の作り方・側面のずれ・面のずれは KokeRock.kt と同じ。
出力: rock_views.png(全部)、rock_zoom.png(主ボタンの右上と名刺の右下を 3 倍)
"""
import argparse
import math
import os
import re
import struct
import sys

import skia

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import koke  # noqa: E402  苔の描き方(draw_moss)を借りる

DP = koke.DP

# ---- KokeTokens.kt と同じ色 ----
GROUND = 0xFFD8DAD5
GROUND_INK = 0xFF1C201D
GROUND_INK_MUTED = 0xFF555B55
ROCK_LO = (0x13, 0x15, 0x17)
ROCK_HI = (0x2C, 0x2F, 0x33)
INK = 0xFFECEDE8
INK_MUTED = 0xFFA4A9A3
ON_PRIMARY = 0xFFC8DE6E
DANGER = 0xFFEF8B74


def rgbf(c):
    return [c[0] / 255.0, c[1] / 255.0, c[2] / 255.0]


def argbf(c, a):
    return [((c >> 16) & 255) / 255.0, ((c >> 8) & 255) / 255.0, (c & 255) / 255.0, a]


def load():
    src = open(os.path.join(HERE, 'src', 'koke_rock.agsl')).read()
    eff = skia.RuntimeEffect.MakeForShader(src)
    if eff is None:
        sys.exit('compile failed')
    return src, eff


def make_shader(src, eff, vals):
    data = b''
    for typ, name in re.findall(r'^\s*uniform\s+(float|float2|float3|float4)\s+(\w+)\s*;', src, re.M):
        n = {'float': 1, 'float2': 2, 'float3': 3, 'float4': 4}[typ]
        if name not in vals:
            sys.exit('uniform not set: ' + name)
        v = vals[name]
        v = list(v) if isinstance(v, (list, tuple)) else [v]
        assert len(v) == n, name
        data += struct.pack('<%df' % n, *v)
    return eff.makeShader(skia.Data.MakeWithCopy(data))


# ---- KokeRock.kt と同じ ----
def h01(seed, i):
    x = math.sin(seed * 12.9898 + i * 78.233) * 43758.5453
    return x - math.floor(x)


def rock_polygon(w, h, seed, chip):
    """角の欠けた凸の 12 角形(時計回り)。角ごとに欠けの大きさを変え、辺の途中の頂点を外へ、角の近くの頂点を少し内へ"""
    c = [chip * (0.75 + 0.4 * h01(seed, i)) for i in range(4)]
    c = [min(x, 0.42 * min(w, h)) for x in c]
    j = lambda i: (0.25 + 1.0 * h01(seed, 10 + i)) * DP   # 角の近くの頂点の、内へのずれ
    m = lambda i: 0.3 + 0.4 * h01(seed, 30 + i)            # 辺の途中の頂点の位置
    pts = [
        (c[0], j(0)), (w * m(0), 0.0), (w - c[1], j(1)),
        (w - j(2), c[1]), (w, h * m(1)), (w - j(3), h - c[2]),
        (w - c[2], h - j(4)), (w * m(2), h), (c[3], h - j(5)),
        (j(6), h - c[3]), (0.0, h * m(3)), (j(7), c[0]),
    ]
    return pts


def base_offset(view):
    vx = max(-1.0, min(1.0, view[0]))
    vy = max(-1.0, min(1.0, view[1]))
    return (-vx * 3.0 * DP, (2.5 - vy * 4.5) * DP)


def face_shift(view):
    vx = max(-1.0, min(1.0, view[0]))
    vy = max(-1.0, min(1.0, view[1]))
    return (vx * 2.5 * DP, vy * 2.5 * DP)


def draw_rock(c, src, eff, w, h, seed, view, facet, relief, chip, rim=(0, 0.0), rim_w=1.0, press=0.0):
    pts = rock_polygon(w, h, seed, chip)
    flat = [v for p in pts for v in p]
    vals = {
        'size': (w, h), 'dp': DP, 'seed': seed, 'base': base_offset(view), 'press': press,
        'rockLo': rgbf(ROCK_LO), 'rockHi': rgbf(ROCK_HI), 'relief': relief, 'facet': facet,
        'rim': argbf(rim[0], rim[1]), 'rimWidth': rim_w * DP,
        'polyA': flat[0:4], 'polyB': flat[4:8], 'polyC': flat[8:12],
        'polyD': flat[12:16], 'polyE': flat[16:20], 'polyF': flat[20:24],
    }
    e = 12 * DP  # KokeDepth.drawMargin
    c.drawRect(skia.Rect(-e, -e, w + e, h + e), skia.Paint(Shader=make_shader(src, eff, vals)))


def font(size, serif=False, weight=400):
    fam = 'Noto Serif CJK JP' if serif else 'Noto Sans CJK JP'
    tf = skia.Typeface(fam, skia.FontStyle(weight, 5, skia.FontStyle.kUpright_Slant))
    return skia.Font(tf, size)


def text(c, s, x, y, f, color):
    c.drawString(s, x, y, f, skia.Paint(AntiAlias=True, Color=color))


def moss_eff():
    return koke.load_effect()


def button(c, R, M, view, label, primary, seed, w=300):
    w, h = w * DP, 52 * DP
    dx, dy = face_shift(view)
    c.save(); c.translate(dx, dy)
    draw_rock(c, *R, w, h, seed, view, facet=40 * DP, relief=0.8, chip=10 * DP,
              rim=(ON_PRIMARY, 0.32) if primary else (INK, 0.0), rim_w=1.0)
    f = font(15 * DP, weight=500)
    tw = f.measureText(label)
    text(c, label, (w - tw) / 2, h / 2 + 5.3 * DP, f, ON_PRIMARY if primary else INK)
    if primary:
        chipr = 10 * DP * 1.7
        # KokeMossSpots.primaryButton(偶数の seed: 右上)
        koke.draw_moss(c, *M, w, h, chipr, [('TopEnd', 38, 12, 6, 0.5)], 0.37, view=view)
    c.restore()


def field(c, R, M, view, label, value, focused, error):
    w, h = 340 * DP, 52 * DP
    text(c, label, 2 * DP, -8 * DP, font(13 * DP, weight=500), 0xFFA63A26 if error else GROUND_INK_MUTED)
    dx, dy = face_shift(view)
    c.save(); c.translate(dx, dy)
    if error:
        rim = (DANGER, 0.95)
    elif focused:
        rim = (INK, 0.85)
    else:
        rim = (INK, 0.12)
    draw_rock(c, *R, w, h, 0.61, view, facet=44 * DP, relief=0.5, chip=8 * DP, rim=rim,
              rim_w=1.5 if (focused or error) else 1.0)
    text(c, value, 16 * DP, h / 2 + 5.5 * DP, font(16 * DP), INK if value else INK_MUTED)
    c.restore()


def card(c, R, M, view, seed):
    w, h = 340 * DP, 205 * DP
    dx, dy = face_shift(view)
    c.save(); c.translate(dx, dy)
    draw_rock(c, *R, w, h, seed, view, facet=64 * DP, relief=1.0, chip=16 * DP)
    text(c, 'Moss & Stone Atelier', 26 * DP, 40 * DP, font(13 * DP, weight=500), INK)
    text(c, '庭師', 26 * DP, 60 * DP, font(13 * DP), INK_MUTED)
    text(c, '苔野 しずく', 26 * DP, 128 * DP, font(26 * DP, serif=True), INK)
    c.drawRect(skia.Rect(26 * DP, 140 * DP, 50 * DP, 141 * DP), skia.Paint(Color=ON_PRIMARY))
    text(c, '090-0000-1234', 26 * DP, 164 * DP, font(13 * DP), INK_MUTED)
    text(c, 'shizuku@example.com', 26 * DP, 184 * DP, font(13 * DP), INK_MUTED)
    koke.draw_moss(c, *M, w, h, 16 * DP * 1.7,
                   [('BottomEnd', 64, 20, 8, 0.5), ('EdgeTop', 40, 11, 5, 0.8)], 0.71, view=view)
    c.restore()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    R = load()
    M = moss_eff()
    views = [(0.0, -0.8), (0.0, 0.0), (0.0, 0.8)]
    colw = 388 * DP
    W, H = int(colw * 3), int(560 * DP)
    surf = skia.Surface(W, H)
    c = surf.getCanvas()
    c.clear(skia.Color4f(*[((GROUND >> s) & 255) / 255.0 for s in (16, 8, 0)], 1.0))
    for i, v in enumerate(views):
        c.save(); c.translate(i * colw + 24 * DP, 24 * DP)
        text(c, 'view.y = %+.1f' % v[1], 0, 10 * DP, font(13 * DP, weight=500), GROUND_INK_MUTED)
        c.translate(0, 34 * DP)
        button(c, R, M, v, 'Press', True, 0.37)
        c.translate(0, 72 * DP)
        button(c, R, M, v, 'キャンセル', False, 0.52)
        c.translate(0, 98 * DP)
        field(c, R, M, v, '名前', '山田 太郎', i == 1, False)
        c.translate(0, 72 * DP)
        card(c, R, M, v, 0.71)
        c.restore()
    img = surf.makeImageSnapshot()
    p = os.path.join(a.out, 'rock_views.png')
    img.save(p, skia.kPNG)
    # 拡大: 中央の列の主ボタンの右上、名刺の右下
    z = skia.Surface(int(1200), int(700))
    zc = z.getCanvas()
    zc.clear(skia.Color(255, 255, 255))
    x0 = colw + 24 * DP
    r1 = skia.Rect(x0 + 200 * DP, 40 * DP, x0 + 330 * DP, 110 * DP)
    zc.drawImageRect(img, r1, skia.Rect(0, 0, r1.width() * 1.7, r1.height() * 1.7))
    r2 = skia.Rect(x0 + 220 * DP, 380 * DP, x0 + 350 * DP, 515 * DP)
    zc.drawImageRect(img, r2, skia.Rect(600, 0, 600 + r2.width() * 1.6, r2.height() * 1.6))
    zp = os.path.join(a.out, 'rock_zoom.png')
    z.makeImageSnapshot().save(zp, skia.kPNG)
    print('wrote', p, zp)


if __name__ == '__main__':
    main()
