"""Moss の描画を Skia(CPU)で再現する模型。アプリと同じ AGSL(MossShaders.kt)を使い、
背景の岩と部品(苔のボタン・石の板)を画像にして、見本写真と見比べる。

使い方(リポジトリのルートで):
    python3 docs/moss/tools/moss.py --out <出力フォルダ> --ref <見本の jpg があるフォルダ>
"""
import argparse
import glob
import os
import re
import struct

import numpy as np  # noqa: F401  (依存として入れておく)
import skia
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
KT = os.path.join(HERE, '../../../app/src/main/java/com/example/businesscard/ui/moss/MossShaders.kt')
CS = skia.ColorSpace.MakeSRGB()

DP = 2.625          # 1dp が何 px か(1080 幅の画面)
SCREEN_W, SCREEN_H = 1080, 2400
MARGIN = 120        # 部品の周りの背景の余白(px)


def load(path=KT):
    """Kotlin の文字列定数(const val X = 生文字列、または const val X = A + 生文字列)を読んで、シェーダーのソースを組み立てる。"""
    s = open(path, encoding='utf-8').read()
    out = {}
    for m in re.finditer(r'const val (\w+) = (?:(\w+) \+ )?"""(.*?)"""', s, re.S):
        name, prefix, body = m.groups()
        out[name] = (out[prefix] if prefix else '') + body
    return out


SRC = load()
EFFECTS = {}
for k in ('ROCK', 'MOSS', 'STONE'):
    eff = skia.RuntimeEffect.MakeForShader(SRC[k])
    if eff is None:
        raise SystemExit(f'shader {k} did not compile')
    EFFECTS[k] = eff


def _decls(src):
    return re.findall(r'^\s*uniform\s+(float|float2|float3|float4)\s+(\w+)\s*;', src, re.M)


def shader(name, values):
    """uniform の値を宣言の順に詰めて、シェーダーを作る。値が無い uniform は 0 にする。"""
    data = b''
    for typ, uname in _decls(SRC[name]):
        v = values.get(uname, 0.0)
        flat = v if isinstance(v, (list, tuple)) else [v]
        data += struct.pack('<%df' % len(flat), *flat)
    return EFFECTS[name].makeShader(skia.Data.MakeWithCopy(data))


def new_canvas(w, h):
    surf = skia.Surface.MakeRaster(skia.ImageInfo.Make(int(w), int(h), skia.kRGBA_8888_ColorType,
                                                       skia.kPremul_AlphaType, CS))
    return surf, surf.getCanvas()


def draw_part(canvas, name, x, y, w, h, pad, values):
    """部品(w x h px)を (x, y) に描く。シェーダーの p は部品の左上を 0 とした座標(負もある)。"""
    paint = skia.Paint()
    paint.setShader(shader(name, dict(values, size=[w, h], dp=DP)))
    canvas.save()
    canvas.translate(x, y)
    canvas.drawRect(skia.Rect.MakeLTRB(-pad, -pad, w + pad, h + pad), paint)
    canvas.restore()


def png_bytes(image):
    return bytes(image.encodeToData(skia.EncodedImageFormat.kPNG, 100).bytes())


def rock_image(focus=0.35):
    """画面全体の背景(1080x2400)。"""
    surf, canvas = new_canvas(SCREEN_W, SCREEN_H)
    paint = skia.Paint()
    paint.setShader(shader('ROCK', dict(size=[SCREEN_W, SCREEN_H], dp=DP, seed=0.0, focus=focus)))
    canvas.drawRect(skia.Rect.MakeWH(SCREEN_W, SCREEN_H), paint)
    return surf.makeImageSnapshot()


# 部品の大きさ(dp)と、角丸(dp)・苔のはみ出し(dp)
PARTS = {
    'moss_button': dict(name='MOSS', w=300, h=56, radius=12, pad=8, seed=1.0, extra={}),
    'stone_button': dict(name='STONE', w=300, h=56, radius=12, pad=4, seed=2.0, extra=dict(moss=0.0)),
    'stone_card': dict(name='STONE', w=340, h=200, radius=16, pad=4, seed=3.0, extra=dict(moss=0.7)),
    'stone_field_sunken': dict(name='STONE', w=340, h=56, radius=10, pad=4, seed=4.0, extra=dict(sunken=1.0)),
}


def compose(bg, key, press):
    """背景の上に部品を1つ重ねた画像(1080 幅)。press = 0 または 1。"""
    spec = PARTS[key]
    w, h = spec['w'] * DP, spec['h'] * DP
    pad = spec['pad'] * DP
    radius = spec['radius'] * DP
    W = SCREEN_W
    H = int(h + 2 * MARGIN)
    bg_y = SCREEN_H // 2 - H // 2           # 画面の中ほどの岩を切り出す
    surf, canvas = new_canvas(W, H)
    canvas.drawImage(bg, 0, -bg_y)
    values = dict(radius=radius, pad=pad, press=float(press), seed=spec['seed'])
    values.update(spec['extra'])
    x = (W - w) / 2
    draw_part(canvas, spec['name'], x, MARGIN, w, h, pad, values)
    return Image.open(__import__('io').BytesIO(png_bytes(surf.makeImageSnapshot())))


def stack(images, gap=24):
    W = max(im.width for im in images)
    H = sum(im.height for im in images) + gap * (len(images) - 1)
    out = Image.new('RGB', (W, H), (240, 240, 240))
    y = 0
    for im in images:
        out.paste(im.convert('RGB'), (0, y))
        y += im.height + gap
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default='previews')
    ap.add_argument('--ref', default=os.path.join(HERE, 'ref'))
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)

    bg = rock_image()
    rock_png = png_bytes(bg)
    open(os.path.join(args.out, 'rock.png'), 'wb').write(rock_png)

    for key in PARTS:
        imgs = [compose(bg, key, press) for press in (0, 1)]
        stack(imgs).save(os.path.join(args.out, key + '.png'))

    # 見本と並べる比較(上段: 見本、下段: 背景全体と、ボタンの1:1の拡大)
    refs = sorted(glob.glob(os.path.join(args.ref, '*.jpg')))[:3]
    H = 600
    tiles = [Image.open(r).convert('RGB') for r in refs]
    tiles = [t.resize((int(t.width * H / t.height), H)) for t in tiles]
    rock_small = Image.open(__import__('io').BytesIO(rock_png)).convert('RGB')
    rock_small = rock_small.resize((int(rock_small.width * H / rock_small.height), H))
    btn = Image.open(os.path.join(args.out, 'moss_button.png')).convert('RGB')
    close = btn.crop((int(btn.width * 0.0), 0, int(btn.width * 0.0) + 460, 460))
    row1 = stack_h(tiles)
    row2 = stack_h([rock_small, close])
    canvas = stack([row1, row2], gap=16)
    canvas.save(os.path.join(args.out, 'compare.png'))
    print('written to', os.path.abspath(args.out))


def stack_h(images, gap=16):
    W = sum(im.width for im in images) + gap * (len(images) - 1)
    H = max(im.height for im in images)
    out = Image.new('RGB', (W, H), (240, 240, 240))
    x = 0
    for im in images:
        out.paste(im, (x, 0))
        x += im.width + gap
    return out


if __name__ == '__main__':
    main()
