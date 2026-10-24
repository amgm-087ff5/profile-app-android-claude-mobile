"""背景シェーダー(src/background.agsl)を Skia(CPU)で描いて確認する。

- アプリと同じ AGSL を skia.RuntimeEffect でコンパイルし、同じ uniform の並びで描く。
- 色の uniform は KinuTokens.kt の sRGB の値をそのまま渡す(シェーダー側で toLinearSrgb する)。
- 出力: out/background.png(hdr=1、8bit sRGB)。HDR 確認は F16 で hdr=3 を描き、光の線の芯の値を表示する。
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
OUT = os.path.join(HERE, 'out')
CS = skia.ColorSpace.MakeSRGB()

W, H = 1080, 2400
DP = 2.625
FACADE_FRACTION = 0.22


def token_colors():
    """KinuTokens.kt の DefaultKinuColors から、背景で使う色を読む。"""
    s = open(TOKENS, encoding='utf-8').read()
    body = s[s.index('val DefaultKinuColors'):]
    vals = {}
    for m in re.finditer(r'(\w+) = Color\(0x([0-9A-Fa-f]{8})\)', body):
        vals[m.group(1)] = int(m.group(2), 16)
    need = {'tower': 'tower', 'fin': 'fin', 'goldLit': 'goldLit', 'goldShade': 'goldShade',
            'amber': 'amber', 'lineCore': 'lineCore'}
    return {k: vals[v] for k, v in need.items()}


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
    colors = token_colors()
    values = {
        'size': [w, h],
        'dp': DP,
        'facadeTop': h * (1.0 - FACADE_FRACTION),
        'ignite': ignite,
        'hdr': hdr,
    }
    for k, argb in colors.items():
        values[k] = srgb4(argb)
    shader = eff.makeShader(skia.Data.MakeWithCopy(pack(src, values)))
    ct = skia.kRGBA_F16_ColorType if f16 else skia.kRGBA_8888_ColorType
    surf = skia.Surface.MakeRaster(skia.ImageInfo.Make(w, h, ct, skia.kPremul_AlphaType, CS))
    paint = skia.Paint()
    paint.setShader(shader)
    surf.getCanvas().drawRect(skia.Rect.MakeWH(w, h), paint)
    return surf


def to_array(surf, w, h, f16):
    """表面の画素を float の配列(H, W, 4)で得る。F16 は sRGB の拡張値(1 を超えうる)。"""
    info = skia.ImageInfo.Make(w, h, skia.kRGBA_F32_ColorType, skia.kUnpremul_AlphaType, CS)
    buf = np.zeros((h, w, 4), dtype=np.float32)
    ok = surf.makeImageSnapshot().readPixels(info, buf, w * 16)
    if not ok:
        raise SystemExit('readPixels failed')
    return buf


def save_png(arr, path):
    img8 = np.clip(np.rint(arr[..., :3] * 255.0), 0, 255).astype(np.uint8)
    from PIL import Image
    Image.fromarray(img8, 'RGB').save(path)


def main():
    os.makedirs(OUT, exist_ok=True)
    eff, src = load_effect()
    print('compiled: background.agsl OK')

    # 1) 8bit sRGB で hdr=1
    surf8 = render(eff, src, W, H, 1.0, f16=False)
    arr8 = to_array(surf8, W, H, f16=False)
    save_png(arr8, os.path.join(OUT, 'background.png'))
    print('saved', os.path.join(OUT, 'background.png'))

    # 2) F16 で hdr=3 の光の線の芯の値を調べる
    surf16 = render(eff, src, W, H, 3.0, f16=True)
    arr16 = to_array(surf16, W, H, f16=True)
    facade = int(H * (1.0 - FACADE_FRACTION))
    # 光の線は帯の上縁(facadeTop 付近)にある。その帯を縦に走査して最大値を取る
    band = slice(facade - 20, facade + 30)
    peak = arr16[band, :, :3].reshape(-1, 3).max(axis=0)
    print('HDR hdr=3 line core peak (sRGB ext) R G B =', np.round(peak, 3))
    print('  > 1.0 in any channel:', bool((peak > 1.0).any()))
    arr_lo = to_array(render(eff, src, W, H, 1.0, f16=True), W, H, f16=True)
    peak1 = arr_lo[band, :, :3].reshape(-1, 3).max(axis=0)
    print('hdr=1 line core peak R G B =', np.round(peak1, 3), '(#FFF6E0 = 1.000 0.965 0.878)')

    # 3) 見本の値との対応(帯・窓・塔の代表点、8bit 結果)
    def hexof(p):
        r, g, b = (np.clip(p, 0, 1) * 255).round().astype(int)
        return '#%02X%02X%02X' % (r, g, b)
    samples = {
        'tower mid (x=540,y=600)': (540, 600),
        'tower lower (x=300,y=1700)': (300, 1700),
        'band top lit (x=540, facade+2)': (540, facade + 2),
        'window amber (x=540, facade+42)': (540, facade + 42),
    }
    for name, (sx, sy) in samples.items():
        print(f'{name}: {hexof(arr8[sy, sx, :3])}')


if __name__ == '__main__':
    sys.exit(main())
