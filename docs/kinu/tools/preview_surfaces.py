"""Kinu の部品(RIBBON・WINDOW・NIGHT_GLASS)を Skia(CPU)で描いて確認する小さな模型。
アプリと同じ AGSL(docs/kinu/tools/src/*.agsl)と、アプリと同じ色(ui/kinu/KinuTokens.kt の DefaultKinuColors)を使う。

使い方: python3 preview_surfaces.py
出力: out/surfaces.png(F8・hdr=1 の通常の画面)、out/surfaces_hdr3.png(F16・hdr=3)。
"""
import os
import re
import struct

import skia

HERE = os.path.dirname(os.path.abspath(__file__))
SRC_DIR = os.path.join(HERE, 'src')
OUT = os.path.join(HERE, 'out')
TOKENS = os.path.join(HERE, '../../../app/src/main/java/com/example/businesscard/ui/kinu/KinuTokens.kt')
CS = skia.ColorSpace.MakeSRGB()
DP = 2.625
BG = 0x16161A


def tokens():
    """KinuTokens.kt の DefaultKinuColors から Color(0xAARRGGBB) を読む。"""
    s = open(TOKENS, encoding='utf-8').read()
    block = s[s.index('val DefaultKinuColors'):]
    out = {}
    for m in re.finditer(r'(\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)', block):
        out[m.group(1)] = int(m.group(2), 16)
    return out


TOK = tokens()


def argb_f(c):
    return [((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255, ((c >> 24) & 255) / 255]


def load_effect(name):
    src = open(os.path.join(SRC_DIR, name + '.agsl'), encoding='utf-8').read()
    eff = skia.RuntimeEffect.MakeForShader(src)
    if eff is None:
        raise SystemExit(f'{name}.agsl did not compile')
    decls = re.findall(r'^\s*(?:layout\(color\)\s+)?uniform\s+(float|float2|half4)\s+(\w+)\s*;', src, re.M)
    return eff, decls


EFFECTS = {n: load_effect(n) for n in ('ribbon', 'window', 'nightglass')}


def make_shader(name, values):
    """uniform 宣言の順に値を詰めて、シェーダーを作る。色の uniform(half4)は 0..1 の sRGB のまま渡す。"""
    eff, decls = EFFECTS[name]
    data = b''
    for typ, uname in decls:
        v = values[uname]
        flat = list(v) if isinstance(v, (list, tuple)) else [v]
        data += struct.pack('<%df' % len(flat), *flat)
    return eff.makeShader(skia.Data.MakeWithCopy(data))


def surface(w, h, f16=False):
    color = skia.kRGBA_F16_ColorType if f16 else skia.kRGBA_8888_ColorType
    return skia.Surface.MakeRaster(skia.ImageInfo.Make(int(w), int(h), color, skia.kPremul_AlphaType, CS))


def fill_rect(canvas, w, h, shader):
    paint = skia.Paint()
    paint.setShader(shader)
    canvas.drawRect(skia.Rect.MakeWH(w, h), paint)


def ribbon(canvas, w, h, line=1.0, ignite=1.0, touch=(-1, -1), seed=0.3, hdr=1.0):
    sh = make_shader('ribbon', dict(
        size=[w, h], dp=DP, hdr=hdr, line=line, ignite=ignite, touch=list(touch), seed=seed,
        goldLit=argb_f(TOK['goldLit']), goldShade=argb_f(TOK['goldShade']),
        goldDeep=argb_f(TOK['goldDeep']), lineCore=argb_f(TOK['lineCore'])))
    canvas.save()
    fill_rect(canvas, w, h, sh)
    canvas.restore()


def window(canvas, w, h, glow=1.0, seed=0.7, hdr=1.0):
    sh = make_shader('window', dict(
        size=[w, h], dp=DP, hdr=hdr, glow=glow, seed=seed,
        amberHi=argb_f(TOK['amberHi']), amber=argb_f(TOK['amber']),
        amberDeep=argb_f(TOK['amberDeep']), mullion=argb_f(TOK['mullion'])))
    fill_rect(canvas, w, h, sh)


def night(canvas, w, h, line=0.35, ember=0.0, touch=(-1, -1), hdr=1.0):
    sh = make_shader('nightglass', dict(
        size=[w, h], dp=DP, hdr=hdr, line=line, ember=ember, touch=list(touch),
        glassTop=argb_f(TOK['glassTop']), glassBottom=argb_f(TOK['glassBottom']),
        fin=argb_f(TOK['fin']), lineCore=argb_f(TOK['lineCore']), amber=argb_f(TOK['amber'])))
    fill_rect(canvas, w, h, sh)


def clip_round(canvas, w, h, r_dp):
    rr = skia.RRect.MakeRectXY(skia.Rect.MakeWH(w, h), r_dp * DP, r_dp * DP)
    canvas.clipRRect(rr, skia.ClipOp.kIntersect, True)


def draw_button(canvas, x, y, w, h, kind, **kw):
    """部品1つを (x, y) に描く。ボタン・入力欄は角丸 14dp、名刺は 22dp。"""
    canvas.save()
    canvas.translate(x, y)
    if kind == 'card':
        clip_round(canvas, w, h, 22)
        window(canvas, w, h, glow=kw.get('glow', 1.0))
        lip = 10 * DP
        canvas.save()
        canvas.clipRect(skia.Rect.MakeWH(w, lip), skia.ClipOp.kIntersect, True)
        ribbon(canvas, w, lip, line=kw.get('line', 1.0), ignite=1.0, seed=0.2)
        canvas.restore()
        canvas.save()
        canvas.translate(0, h - lip)
        canvas.clipRect(skia.Rect.MakeWH(w, lip), skia.ClipOp.kIntersect, True)
        ribbon(canvas, w, lip, line=kw.get('line', 1.0), ignite=1.0, seed=1.1)
        canvas.restore()
    else:
        clip_round(canvas, w, h, 14)
        if kind == 'gold':
            ribbon(canvas, w, h, line=kw.get('line', 1.0), ignite=1.0,
                   touch=kw.get('touch', (-1, -1)), seed=kw.get('seed', 0.3))
        else:
            night(canvas, w, h, line=kw.get('line', 0.35), ember=kw.get('ember', 0.0),
                  touch=kw.get('touch', (-1, -1)))
    canvas.restore()


def render(f16, hdr, path):
    """6 つの部品を並べる: 主ボタン・名刺・入力欄(NIGHT line=1, ember=0.6)・副ボタン(line=0.35)・押したボタン(line=1.6)."""
    W = 340 * DP + 2 * 60
    items = [
        ('gold', 320, 52, dict(line=1.0, hdr=hdr)),
        ('card', 340, 200, dict(hdr=hdr)),
        ('night', 320, 52, dict(line=1.0, ember=0.6, hdr=hdr)),
        ('night', 320, 52, dict(line=0.35, hdr=hdr)),
        ('gold', 320, 52, dict(line=1.6, touch=(320 * DP * 0.7, 52 * DP * 0.5), hdr=hdr)),
    ]
    gap = 36
    heights = [round(h * DP) for _, _, h, _ in [(k, w, h, kw) for k, w, h, kw in items]]
    total_h = sum(heights) + gap * (len(items) + 1)
    surf = surface(W, total_h, f16=f16)
    canvas = surf.getCanvas()
    canvas.clear(skia.Color(0x16, 0x16, 0x1A, 255))
    y = gap
    placed = []
    for (kind, wdp, hdp, kw), hp in zip(items, heights):
        wp = round(wdp * DP)
        x = (W - wp) / 2
        draw_button(canvas, x, y, wp, hp, kind, **kw)
        placed.append((x, y, wp, hp))
        y += hp + gap
    surf.flushAndSubmit()
    img = surf.makeImageSnapshot()
    with open(path, 'wb') as f:
        f.write(bytes(img.encodeToData()))
    return surf, placed


def read_float(surf, x, y):
    """F16 の画面から1画素を読む(sRGB の符号化値。1.0 を超えるのは白より明るい)。"""
    info = skia.ImageInfo.Make(1, 1, skia.kRGBA_F32_ColorType, skia.kUnpremul_AlphaType, CS)
    buf = bytearray(16)
    ok = surf.readPixels(info, buf, 4 * 4, int(x), int(y))
    assert ok
    return struct.unpack('<4f', bytes(buf))


def max_in_rect(surf, rect):
    """矩形の中で一番明るい値(R, G, B のうち最大)。"""
    x0, y0, w, h = rect
    info = skia.ImageInfo.Make(int(w), int(h), skia.kRGBA_F32_ColorType, skia.kUnpremul_AlphaType, CS)
    buf = bytearray(int(w) * int(h) * 16)
    assert surf.readPixels(info, buf, int(w) * 16, int(x0), int(y0))
    vals = struct.unpack('<%df' % (int(w) * int(h) * 4), bytes(buf))
    return max(vals[i] for i in range(len(vals)) if i % 4 != 3)


def main():
    os.makedirs(OUT, exist_ok=True)
    surf8, placed = render(False, 1.0, os.path.join(OUT, 'surfaces.png'))
    surf16, placed16 = render(True, 3.0, os.path.join(OUT, 'surfaces_hdr3.png'))

    # 数値の確認: 押したボタンの光の線(上の縁の芯)が 1.0 を超えるか
    x, y, w, h = placed16[4]
    core_rect = (x + w * 0.3, y + 0.5 * DP - 0.4 * DP, w * 0.4, 1.2 * DP)
    core16 = max_in_rect(surf16, core_rect)
    x8, y8, w8, h8 = placed[4]
    core8 = max_in_rect(surf8, (x8 + w8 * 0.3, y8 + 0.5 * DP - 0.4 * DP, w8 * 0.4, 1.2 * DP))
    print('pressed ribbon core max  F8 hdr=1 :', round(core8, 3))
    print('pressed ribbon core max  F16 hdr=3:', round(core16, 3))
    print('saved', os.path.join(OUT, 'surfaces.png'), os.path.join(OUT, 'surfaces_hdr3.png'))


if __name__ == '__main__':
    main()
