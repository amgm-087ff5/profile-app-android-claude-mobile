"""Kinu の部品(RIBBON・WINDOW・SATIN)を Skia(CPU)で描いて確認する小さな模型。
アプリと同じ AGSL(docs/kinu/tools/src/*.agsl)と、アプリと同じ色(ui/kinu/KinuTokens.kt の DefaultKinuColors)を使う。
背景は画面の中央の陰の面に近い #75625A。

使い方: python3 preview_surfaces.py
出力: out/surfaces.png(F8・hdr=1 の通常の画面)、out/surfaces_hdr3.png(F16・hdr=3)、
      out/surfaces_vs_photo.png(見本の外装の切り抜きと、同じ幅の描き)。
"""
import os
import re
import struct

import skia
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SRC_DIR = os.path.join(HERE, 'src')
OUT = os.path.join(HERE, 'out')
TOKENS = os.path.join(HERE, '../../../app/src/main/java/com/example/businesscard/ui/kinu/KinuTokens.kt')
REF = os.path.join(HERE, '..', 'ref_facade.png')      # 見本(外装だけの切り出し 560x556)
CS = skia.ColorSpace.MakeSRGB()
DP = 2.625
BG = 0x75625A                                          # 背景の陰の面


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


EFFECTS = {n: load_effect(n) for n in ('ribbon', 'window', 'satin')}


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
    fill_rect(canvas, w, h, sh)


def window(canvas, w, h, glow=1.0, seed=0.7, hdr=1.0):
    sh = make_shader('window', dict(
        size=[w, h], dp=DP, hdr=hdr, glow=glow, seed=seed,
        amberHi=argb_f(TOK['amberHi']), amber=argb_f(TOK['amber']),
        amberDeep=argb_f(TOK['amberDeep']), mullion=argb_f(TOK['mullion'])))
    fill_rect(canvas, w, h, sh)


def satin(canvas, w, h, line=0.35, touch=(-1, -1), hdr=1.0):
    sh = make_shader('satin', dict(
        size=[w, h], dp=DP, hdr=hdr, line=line, touch=list(touch),
        glassTop=argb_f(TOK['glassTop']), glassBottom=argb_f(TOK['glassBottom']),
        rim=argb_f(TOK['rim']), lineCore=argb_f(TOK['lineCore'])))
    fill_rect(canvas, w, h, sh)


def clip_round(canvas, w, h, r_dp):
    rr = skia.RRect.MakeRectXY(skia.Rect.MakeWH(w, h), r_dp * DP, r_dp * DP)
    canvas.clipRRect(rr, skia.ClipOp.kIntersect, True)


def rim_stroke(canvas, w, h, r_dp):
    """入力欄の周りの 1dp の rim(D が Compose で描く想定の模型)。"""
    paint = skia.Paint()
    paint.setStyle(skia.Paint.kStroke_Style)
    paint.setStrokeWidth(DP)
    c = TOK['rim']
    paint.setColor(skia.Color(c >> 16 & 255, c >> 8 & 255, c & 255, c >> 24 & 255))
    paint.setAntiAlias(True)
    rr = skia.RRect.MakeRectXY(skia.Rect.MakeWH(w, h).makeInset(DP / 2, DP / 2), r_dp * DP, r_dp * DP)
    canvas.drawRRect(rr, paint)


def draw_part(canvas, x, y, w, h, kind, **kw):
    """部品1つを (x, y) に描く。ボタン・入力欄は角丸 14dp、名刺は 22dp。"""
    hdr = kw.get('hdr', 1.0)
    canvas.save()
    canvas.translate(x, y)
    if kind == 'card':
        clip_round(canvas, w, h, 22)
        window(canvas, w, h, glow=1.0, seed=kw.get('seed', 0.7), hdr=hdr)
        lip = 10 * DP
        # 上下の金の縁(リップ)。下の縁は別の位相の帯を重ねる
        canvas.save()
        canvas.clipRect(skia.Rect.MakeWH(w, lip), skia.ClipOp.kIntersect, True)
        ribbon(canvas, w, lip, line=kw.get('line', 1.0), ignite=1.0, seed=0.2, hdr=hdr)
        canvas.restore()
        canvas.save()
        canvas.translate(0, h - lip)
        canvas.clipRect(skia.Rect.MakeWH(w, lip), skia.ClipOp.kIntersect, True)
        ribbon(canvas, w, lip, line=kw.get('line', 1.0), ignite=1.0, seed=1.1, hdr=hdr)
        canvas.restore()
    elif kind == 'field':
        clip_round(canvas, w, h, 14)
        window(canvas, w, h, glow=kw.get('glow', 0.35), seed=kw.get('seed', 0.5), hdr=hdr)
        rim_stroke(canvas, w, h, 14)
    elif kind == 'gold':
        clip_round(canvas, w, h, 14)
        ribbon(canvas, w, h, line=kw.get('line', 1.0), ignite=1.0,
               touch=kw.get('touch', (-1, -1)), seed=kw.get('seed', 0.3), hdr=hdr)
    elif kind == 'satin':
        clip_round(canvas, w, h, 14)
        satin(canvas, w, h, line=kw.get('line', 0.35), touch=kw.get('touch', (-1, -1)), hdr=hdr)
    canvas.restore()


# 並べる部品: (kind, 幅dp, 高さdp, 引数)
ITEMS = [
    ('gold', 320, 52, dict(line=1.0)),                                   # 主ボタン(RIBBON)
    ('card', 340, 200, dict(line=1.0)),                                  # 名刺(WINDOW + 上下の RIBBON の縁)
    ('field', 320, 52, dict(glow=0.35, seed=0.5)),                       # 入力欄(WINDOW glow 0.35)
    ('field', 320, 52, dict(glow=0.8, seed=0.9)),                        # 入力中の欄(WINDOW glow 0.8)
    ('satin', 320, 52, dict(line=0.35)),                                 # 副ボタン(SATIN)
    ('gold', 320, 52, dict(line=1.6, touch=(320 * DP * 0.7, 52 * DP * 0.5), seed=0.3)),  # 押したボタン
]


def render(f16, hdr, path):
    W = 340 * DP + 2 * 60
    gap = 36
    heights = [round(h * DP) for _, _, h, _ in ITEMS]
    total_h = sum(heights) + gap * (len(ITEMS) + 1)
    surf = surface(W, total_h, f16=f16)
    canvas = surf.getCanvas()
    canvas.clear(skia.Color(BG >> 16 & 255, BG >> 8 & 255, BG & 255, 255))
    y = gap
    placed = []
    for (kind, wdp, hdp, kw), hp in zip(ITEMS, heights):
        wp = round(wdp * DP)
        x = (W - wp) / 2
        draw_part(canvas, x, y, wp, hp, kind, hdr=hdr, **kw)
        placed.append((x, y, wp, hp))
        y += hp + gap
    surf.flushAndSubmit()
    img = surf.makeImageSnapshot()
    with open(path, 'wb') as f:
        f.write(bytes(img.encodeToData()))
    return surf, placed


def read_rect_f32(surf, rect):
    """矩形の画素(線形 F32、RGBA の順)を返す。"""
    x0, y0, w, h = rect
    info = skia.ImageInfo.Make(int(w), int(h), skia.kRGBA_F32_ColorType, skia.kUnpremul_AlphaType, CS)
    buf = bytearray(int(w) * int(h) * 16)
    assert surf.readPixels(info, buf, int(w) * 16, int(x0), int(y0))
    return struct.unpack('<%df' % (int(w) * int(h) * 4), bytes(buf))


def max_in_rect(surf, rect):
    """矩形の中で一番明るい値(R, G, B のうち最大)。"""
    vals = read_rect_f32(surf, rect)
    return max(vals[i] for i in range(len(vals)) if i % 4 != 3)


def mean_srgb_in_rect(surf, rect):
    """矩形の平均色を sRGB の 0..255 で返す(表示される色の見当)。"""
    vals = read_rect_f32(surf, rect)
    n = len(vals) // 4
    out = []
    for c in range(3):
        lin = sum(vals[i * 4 + c] for i in range(n)) / n
        lin = min(max(lin, 0.0), 1.0)
        srgb = 12.92 * lin if lin <= 0.0031308 else 1.055 * lin ** (1 / 2.4) - 0.055
        out.append(round(srgb * 255))
    return out


def strip_render(w, h):
    """見本の外装と見比べるための帯(RIBBON 26dp)と窓(WINDOW 30dp)の重ね。見本と同じ幅に合わせる。"""
    surf = surface(w, h)
    canvas = surf.getCanvas()
    y = 0.0
    kinds = [('ribbon', 26), ('window', 30), ('ribbon', 26), ('window', 30), ('ribbon', 26)]
    for k, (kind, hdp) in enumerate(kinds):
        hp = hdp * DP
        canvas.save()
        canvas.translate(0, y)
        if kind == 'ribbon':
            ribbon(canvas, w, hp, line=1.0, ignite=1.0, seed=0.4 + k * 0.7)
        else:
            window(canvas, w, hp, glow=1.0, seed=0.7 + k * 0.5)
        canvas.restore()
        y += hp
    surf.flushAndSubmit()
    return surf.makeImageSnapshot()


def compare(path):
    """見本の外装(上)と、同じ幅の描き(下)を並べて保存する。"""
    ref = Image.open(REF).convert('RGB')
    ref_path = os.path.join(OUT, 'ref_facade_rgb.png')
    ref.save(ref_path)
    from_photo = skia.Image.open(ref_path)
    pw, ph = from_photo.width(), from_photo.height()
    mine = strip_render(pw, ph)
    out = skia.Surface.MakeRaster(skia.ImageInfo.Make(pw, ph * 2 + 16, skia.kRGBA_8888_ColorType, skia.kPremul_AlphaType, CS))
    c = out.getCanvas()
    c.clear(skia.Color(0x16, 0x16, 0x1A, 255))
    c.drawImage(from_photo, 0, 0)
    c.drawImage(mine, 0, ph + 16)
    with open(path, 'wb') as f:
        f.write(bytes(out.makeImageSnapshot().encodeToData()))


def main():
    os.makedirs(OUT, exist_ok=True)
    compare(os.path.join(OUT, 'surfaces_vs_photo.png'))
    surf8, placed = render(False, 1.0, os.path.join(OUT, 'surfaces.png'))
    surf16, placed16 = render(True, 3.0, os.path.join(OUT, 'surfaces_hdr3.png'))

    # 数値の確認: 押したボタン(ITEMS の 6 番目)の光の線の芯が 1.0 を超えるか
    def core_rect(pl):
        x, y, w, h = pl[5]
        return (x + w * 0.3, y + 0.5 * DP - 0.4 * DP, w * 0.4, 1.2 * DP)

    print('pressed ribbon core max  F8 hdr=1 :', round(max_in_rect(surf8, core_rect(placed)), 3))
    print('pressed ribbon core max  F16 hdr=3:', round(max_in_rect(surf16, core_rect(placed16)), 3))

    # 入力欄の平均色(sRGB)。glow 0.35 は落ち着いたクリーム、0.8 は琥珀に寄る
    for idx, label in ((2, 'field glow 0.35'), (3, 'field glow 0.8'), (4, 'satin'), (0, 'gold main')):
        x, y, w, h = placed[idx]
        print(label, 'mean sRGB', mean_srgb_in_rect(surf8, (x + w * 0.2, y + h * 0.3, w * 0.6, h * 0.4)))
    print('saved', os.path.join(OUT, 'surfaces.png'), os.path.join(OUT, 'surfaces_hdr3.png'),
          os.path.join(OUT, 'surfaces_vs_photo.png'))


if __name__ == '__main__':
    main()
