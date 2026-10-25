"""背景シェーダー(src/background.agsl)を Skia(CPU)で描いて確認する。

- アプリと同じ AGSL を skia.RuntimeEffect でコンパイルし、同じ uniform の並びで描く。
- 色の uniform は KinuTokens.kt の sRGB の値をそのまま渡す(シェーダー側で toLinearSrgb する)。
- 出力(out/):
  - background.png: 1080x2400、8bit sRGB、hdr=1
  - background_vs_photo.png: 見本(ref_facade.png)と並べたもの(帯の色・光の線・ドレープの厚みの比較)
- HDR 確認は F16 で hdr=3 を描き、光の線の芯の最大値を表示する。
"""
import os
import re
import struct
import sys

import numpy as np
import skia

HERE = os.path.dirname(os.path.abspath(__file__))
SHADER = os.path.join(HERE, 'src', 'background.agsl')
TOKENS = os.path.join(HERE, '..', '..', '..', 'app', 'src', 'main', 'java', 'com', 'example', 'businesscard', 'ui', 'kinu', 'KinuTokens.kt')
REF = os.path.join(HERE, '..', 'ref_facade.png')
OUT = os.path.join(HERE, 'out')
CS = skia.ColorSpace.MakeSRGB()

W, H = 1080, 2400
DP = 2.625
ZONE_FRACTION = 0.20  # 上端・下端それぞれの帯の領域(シェーダーの ZONE と同じ)

COLOR_NAMES = ['facadeShade', 'facadeMauve', 'foldGold', 'goldLit', 'amber', 'amberHi', 'lineCore']


def token_colors():
    """KinuTokens.kt の DefaultKinuColors から、背景で使う色を読む。"""
    s = open(TOKENS, encoding='utf-8').read()
    body = s[s.index('val DefaultKinuColors'):]
    vals = {}
    for m in re.finditer(r'(\w+) = Color\(0x([0-9A-Fa-f]{8})\)', body):
        vals[m.group(1)] = int(m.group(2), 16)
    return {k: vals[k] for k in COLOR_NAMES}


def srgb4(argb):
    return [((argb >> 16) & 255) / 255, ((argb >> 8) & 255) / 255, (argb & 255) / 255, ((argb >> 24) & 255) / 255]


def load_effect():
    src = open(SHADER, encoding='utf-8').read()
    if '"""' in src or '$' in src:
        raise SystemExit('background.agsl contains a forbidden token ($ or """)')
    eff = skia.RuntimeEffect.MakeForShader(src)
    if eff is None:
        raise SystemExit('background.agsl did not compile')
    return eff, src


def decls(src):
    return re.findall(r'^\s*(?:layout\(color\)\s+)?uniform\s+(float|float2|half4)\s+(\w+)\s*;', src, re.M)


def pack(src, values):
    data = b''
    for typ, name in decls(src):
        v = values[name]
        flat = list(v) if isinstance(v, (list, tuple)) else [v]
        data += struct.pack('<%df' % len(flat), *[float(x) for x in flat])
    return data


def render(eff, src, w, h, hdr, f16, ignite=1.0):
    values = {
        'size': [w, h],
        'dp': DP,
        'ignite': ignite,
        'hdr': hdr,
    }
    for k, argb in token_colors().items():
        values[k] = srgb4(argb)
    shader = eff.makeShader(skia.Data.MakeWithCopy(pack(src, values)))
    ct = skia.kRGBA_F16_ColorType if f16 else skia.kRGBA_8888_ColorType
    surf = skia.Surface.MakeRaster(skia.ImageInfo.Make(w, h, ct, skia.kPremul_AlphaType, CS))
    paint = skia.Paint()
    paint.setShader(shader)
    surf.getCanvas().drawRect(skia.Rect.MakeWH(w, h), paint)
    return surf


def to_array(surf, w, h):
    """表面の画素を float の配列(H, W, 4)で得る。F16 は sRGB の拡張値(1 を超えうる)。"""
    info = skia.ImageInfo.Make(w, h, skia.kRGBA_F32_ColorType, skia.kUnpremul_AlphaType, CS)
    buf = np.zeros((h, w, 4), dtype=np.float32)
    ok = surf.makeImageSnapshot().readPixels(info, buf, w * 16)
    if not ok:
        raise SystemExit('readPixels failed')
    return buf


def save_png(arr, path):
    from PIL import Image
    img8 = np.clip(np.rint(arr[..., :3] * 255.0), 0, 255).astype(np.uint8)
    Image.fromarray(img8, 'RGB').save(path)


def hexof(p):
    r, g, b = (np.clip(p, 0, 1) * 255).round().astype(int)
    return '#%02X%02X%02X' % (r, g, b)


def make_comparison(arr8, path):
    """見本(左)と、背景の全体(中)・上下の帯の領域の拡大(右)を並べる。"""
    from PIL import Image
    ref = Image.open(REF).convert('RGB')
    bg = Image.fromarray(np.clip(np.rint(arr8[..., :3] * 255.0), 0, 255).astype(np.uint8), 'RGB')
    zh = int(H * ZONE_FRACTION)
    top = bg.crop((0, 0, W, zh)).resize((560, int(zh * 560 / W)), Image.LANCZOS)
    bot = bg.crop((0, H - zh, W, H)).resize((560, int(zh * 560 / W)), Image.LANCZOS)
    full = bg.resize((int(W * 556 / H), 556), Image.LANCZOS)
    gap = 16
    cw = ref.width + gap + full.width + gap + 560
    ch = max(ref.height, top.height + gap + bot.height)
    canvas = Image.new('RGB', (cw, ch), (40, 36, 34))
    canvas.paste(ref, (0, 0))
    canvas.paste(full, (ref.width + gap, 0))
    x = ref.width + gap + full.width + gap
    canvas.paste(top, (x, 0))
    canvas.paste(bot, (x, top.height + gap))
    canvas.save(path)


def main():
    os.makedirs(OUT, exist_ok=True)
    eff, src = load_effect()
    print('compiled: background.agsl OK')

    # 1) 8bit sRGB で hdr=1
    arr8 = to_array(render(eff, src, W, H, 1.0, f16=False), W, H)
    save_png(arr8, os.path.join(OUT, 'background.png'))
    print('saved', os.path.join(OUT, 'background.png'))
    make_comparison(arr8, os.path.join(OUT, 'background_vs_photo.png'))
    print('saved', os.path.join(OUT, 'background_vs_photo.png'))

    # 2) F16 で hdr=3 と hdr=1 の光の線の芯の値(帯の領域の全体の最大値)
    zh = int(H * ZONE_FRACTION)
    arr16 = to_array(render(eff, src, W, H, 3.0, f16=True), W, H)
    zone = np.concatenate([arr16[:zh, :, :3], arr16[H - zh:, :, :3]]).reshape(-1, 3)
    peak = zone.max(axis=0)
    print('HDR hdr=3 zone peak (sRGB ext) R G B =', np.round(peak, 3))
    print('  > 1.0 in any channel:', bool((peak > 1.0).any()))
    arr1 = to_array(render(eff, src, W, H, 1.0, f16=True), W, H)
    zone1 = np.concatenate([arr1[:zh, :, :3], arr1[H - zh:, :, :3]]).reshape(-1, 3)
    print('hdr=1 zone peak R G B =', np.round(zone1.max(axis=0), 3), '(lineCore #FFF4E6 = 1.000 0.957 0.902)')

    # 3) 代表点(8bit、hdr=1)
    samples = {
        'centre satin mean (rows zh..H-zh)': None,
        'top band lip (x=540, y=12)': (540, 12),
        'top band face (x=300, y=40)': (300, 40),
        'top window (x=200, y=28)': (200, 28),
        'bottom band face (x=540, y=H-40)': (540, H - 40),
        'bottom fold (x=540, y=H-18)': (540, H - 18),
    }
    centre = arr8[zh + 20:H - zh - 20, :, :3].reshape(-1, 3).mean(axis=0)
    print('centre satin mean:', hexof(centre))
    for name, pt in samples.items():
        if pt is None:
            continue
        sx, sy = pt
        print(f'{name}: {hexof(arr8[sy, sx, :3])}')


if __name__ == '__main__':
    sys.exit(main())
