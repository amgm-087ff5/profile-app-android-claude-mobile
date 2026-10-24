"""Koke の苔(KokeMossShaders.kt の MOSS)を skia-python(CPU)で描いて PNG にする。

  python3 docs/koke/tools/koke.py --out <出力フォルダ>

平らな部品(主ボタン 300x56dp、名刺 340x200dp)を描き、その上に苔のシェーダーを
KokeMoss.kt と同じ決まり(群れの置き方・描く矩形)で重ねる。
出力: button.png / card.png(押していない・押した・生える途中 growth=0.4 を縦に並べたもの)、
それぞれ苔の部分を 3 倍に拡大した *_zoom.png。
"""
import argparse
import math
import os
import re
import struct
import sys

import skia

HERE = os.path.dirname(os.path.abspath(__file__))
DP = 2.625
OVERHANG = 10.0  # dp。KokeMoss.kt の MossOverhang と同じ
SHADOW = 6.0     # dp。影のための余白


def load_effect():
    src = open(os.path.join(HERE, 'src', 'koke_moss.agsl')).read()
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


# ---- KokeMoss.kt と同じ: 群れの基準点と描く矩形 ----
def patch_vec(anchor, spread, depth, along, w, h, r):
    """群れ1つを (x, y, spread, depth) にする(px)。基準点は部品の縁の上"""
    k = r * (1.0 - math.sqrt(0.5))  # 角丸の 45 度の点
    pts = {
        'TopStart': (k, k), 'TopEnd': (w - k, k),
        'BottomStart': (k, h - k), 'BottomEnd': (w - k, h - k),
        'EdgeTop': (w * along, 0.0), 'EdgeBottom': (w * along, h),
        'EdgeStart': (0.0, h * along), 'EdgeEnd': (w, h * along),
    }
    x, y = pts[anchor]
    return (x, y, spread, depth)


def moss_bounds(vecs, w, h, dp):
    """苔と影を描く矩形(部品の座標、px)。群れの広がりの 1.6 倍 + はみ出し + 影"""
    out = (OVERHANG + SHADOW) * dp
    l, t, rr, b = 1e9, 1e9, -1e9, -1e9
    for (x, y, s, d) in vecs:
        e = s * 1.6 + 4 * dp
        l, t = min(l, x - e), min(t, y - e)
        rr, b = max(rr, x + e), max(b, y + e)
    return (max(l, -out), max(t, -out), min(rr, w + out), min(b, h + out))


def draw_moss(canvas, src, eff, w, h, radius, patches, seed, growth=1.0, press=0.0, touch=(0, 0), dp=DP, view=(0.0, 0.0)):
    vecs = [patch_vec(a, s * dp, d * dp, al, w, h, radius) for (a, s, d, hh, al) in patches]
    hs = [hh * dp for (a, s, d, hh, al) in patches]
    while len(vecs) < 4:
        vecs.append((0.0, 0.0, 0.0, 0.0))
        hs.append(0.0)
    vals = {
        'size': (w, h), 'dp': dp, 'seed': seed, 'radius': radius, 'growth': growth,
        'press': press, 'touch': touch, 'view': view,
        'patchA': vecs[0], 'patchB': vecs[1], 'patchC': vecs[2], 'patchD': vecs[3],
        'patchH': hs[:4],
    }
    l, t, r, b = moss_bounds([v for v in vecs if v[2] > 0], w, h, dp)
    p = skia.Paint(Shader=make_shader(src, eff, vals))
    canvas.drawRect(skia.Rect(l, t, r, b), p)
    return (l, t, r, b)


def font(size, bold=False):
    tf = skia.Typeface('Inter', skia.FontStyle(600 if bold else 400, 5, skia.FontStyle.kUpright_Slant))
    return skia.Font(tf, size)


def rock(c, w, h, seed, view):
    """黒い濡れた岩の部品の見本(UI 担当の本物の代わり)。角の欠けた多角形 + 数枚のファセット + 薄い側面"""
    import random
    dp = DP
    rnd = random.Random(int(seed * 1000))
    k = 14 * dp
    pts = [(rnd.uniform(0.3, 0.8) * k, 0), (w * 0.55, rnd.uniform(0, 1.5) * dp), (w - rnd.uniform(0.4, 0.9) * k, 0),
           (w, rnd.uniform(0.3, 0.8) * k), (w - rnd.uniform(0, 1.5) * dp, h * 0.5), (w, h - rnd.uniform(0.4, 0.9) * k),
           (w - rnd.uniform(0.3, 0.8) * k, h), (w * 0.45, h - rnd.uniform(0, 1.5) * dp), (rnd.uniform(0.4, 0.9) * k, h),
           (0, h - rnd.uniform(0.3, 0.8) * k), (rnd.uniform(0, 1.5) * dp, h * 0.5), (0, rnd.uniform(0.4, 0.9) * k)]
    path = skia.Path()
    path.moveTo(*pts[0])
    for p in pts[1:]:
        path.lineTo(*p)
    path.close()
    # 側面(画面の上にある部品は下の側面が見える)
    side = (3.5 - 2.5 * view[1]) * dp
    c.save(); c.translate(0, side)
    c.drawPath(path, skia.Paint(AntiAlias=True, Color=0xFF0B0C0D))
    c.restore()
    c.save(); c.translate(-view[0] * 2 * dp, -view[1] * 1.5 * dp)
    c.drawPath(path, skia.Paint(AntiAlias=True, Color=0xFF1C1E20))
    c.save(); c.clipPath(path, doAntiAlias=True)
    cx, cy = w * rnd.uniform(0.35, 0.65), h * rnd.uniform(0.35, 0.65)
    shades = [0xFF141516, 0xFF2B2D2F, 0xFF1A1C1E, 0xFF232528, 0xFF17191A, 0xFF26282B]
    for i in range(len(pts)):
        a, b = pts[i], pts[(i + 1) % len(pts)]
        f = skia.Path(); f.moveTo(cx, cy); f.lineTo(*a); f.lineTo(*b); f.close()
        c.drawPath(f, skia.Paint(AntiAlias=True, Color=shades[i % len(shades)]))
    c.restore()
    c.drawPath(path, skia.Paint(AntiAlias=True, Color=0x40D8E0E4, Style=skia.Paint.kStroke_Style, StrokeWidth=0.8 * dp))
    c.restore()


def button_scene(c, src, eff, ox, oy, growth, press, touchdp, view=(0.0, 0.0), seed=0.37):
    dp = DP
    w, h, r = 300 * dp, 56 * dp, 14 * dp
    c.save(); c.translate(ox, oy)
    rock(c, w, h, seed, view)
    txt = 'Press'
    f = font(16 * dp, True)
    tw = f.measureText(txt)
    c.drawString(txt, (w - tw) / 2, h / 2 + 5.6 * dp, f, skia.Paint(AntiAlias=True, Color=0xFFC9E07A))
    patches = [('TopStart', 46, 18, 7, 0.5)]
    tp = (touchdp[0] * dp, touchdp[1] * dp)
    bounds = draw_moss(c, src, eff, w, h, r, patches, seed, growth, press, tp, view=view)
    c.restore()
    return (ox, oy, w, h, bounds)


def card_scene(c, src, eff, ox, oy, growth, press, touchdp, view=(0.0, 0.0), seed=0.71):
    dp = DP
    w, h, r = 340 * dp, 200 * dp, 16 * dp
    c.save(); c.translate(ox, oy)
    rock(c, w, h, seed, view)
    ink = skia.Paint(AntiAlias=True, Color=0xFFECEDE8)
    mut = skia.Paint(AntiAlias=True, Color=0xFF9A9E9A)
    c.drawString('Aoi Tanaka', 24 * dp, 52 * dp, font(22 * dp, True), ink)
    c.drawString('Moss & Stone Studio', 24 * dp, 76 * dp, font(14 * dp), mut)
    c.drawString('aoi@example.com', 24 * dp, 130 * dp, font(13 * dp), mut)
    c.drawString('+81 90 1234 5678', 24 * dp, 152 * dp, font(13 * dp), mut)
    patches = [('BottomEnd', 58, 22, 9, 0.5), ('EdgeBottom', 40, 12, 5, 0.52)]
    tp = (touchdp[0] * dp, touchdp[1] * dp)
    bounds = draw_moss(c, src, eff, w, h, r, patches, seed, growth, press, tp, view=view)
    c.restore()
    return (ox, oy, w, h, bounds)


def render(scene, states, out, name):
    src, eff = load_effect()
    m = 40 * DP
    probe = scene(skia.Surface(4, 4).getCanvas(), src, eff, 0, 0, 1.0, 0.0, (0, 0))
    w, h = probe[2], probe[3]
    W, H = int(w + 2 * m), int((h + 2 * m) * len(states))
    surf = skia.Surface(W, H)
    c = surf.getCanvas()
    c.clear(skia.Color(0xD6, 0xD8, 0xD5))
    info = []
    for i, (g, pr, tdp, vy) in enumerate(states):
        info.append(scene(c, src, eff, m, m + i * (h + 2 * m), g, pr, tdp, (0.0, vy)))
    img = surf.makeImageSnapshot()
    path = os.path.join(out, name + '.png')
    img.save(path, skia.kPNG)
    # 3 倍拡大(各状態の苔の矩形を横に並べる)
    subs = []
    for (ox, oy, w, h, (l, t, r, b)) in info:
        subs.append(skia.IRect(int(ox + l), int(oy + t), int(ox + r), int(oy + b)))
    zw = sum(s.width() * 3 + 12 for s in subs)
    zh = max(s.height() * 3 for s in subs)
    z = skia.Surface(int(zw), int(zh))
    zc = z.getCanvas()
    zc.clear(skia.Color(255, 255, 255))
    x = 0
    for s in subs:
        zc.drawImageRect(img, skia.Rect.Make(s), skia.Rect(x, 0, x + s.width() * 3, s.height() * 3),
                         skia.SamplingOptions(skia.FilterMode.kNearest))
        x += s.width() * 3 + 12
    zp = os.path.join(out, name + '_zoom.png')
    z.makeImageSnapshot().save(zp, skia.kPNG)
    print('wrote', path, zp)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    # (growth, press, 指の位置 dp)
    # (growth, press, 指の位置 dp, view.y)。上から view.y = -0.8 / 0 / +0.8、押した、生える途中
    states = lambda t: [(1.0, 0.0, (0, 0), -0.8), (1.0, 0.0, (0, 0), 0.0), (1.0, 0.0, (0, 0), 0.8),
                        (1.0, 1.0, t, 0.0), (0.4, 0.0, (0, 0), 0.0)]
    render(button_scene, states((22, 14)), a.out, 'button')
    render(card_scene, states((318, 186)), a.out, 'card')


if __name__ == '__main__':
    main()
