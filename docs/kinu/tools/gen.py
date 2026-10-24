"""Kinu の AGSL を Kotlin の定数に書き出す。

入力: docs/kinu/tools/src/{background,ribbon,window,nightglass}.agsl
出力: app/src/main/java/com/example/businesscard/ui/kinu/KinuShaders.kt

使い方(リポジトリのルートで): python3 docs/kinu/tools/gen.py
ファイルが無い、または空のときは、コンパイルが通る空のシェーダーを書く。
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC_DIR = os.path.join(HERE, 'src')
OUT = os.path.join(HERE, '../../../app/src/main/java/com/example/businesscard/ui/kinu/KinuShaders.kt')

# Kotlin の定数名 と 入力ファイル名
SHADERS = [
    ('BACKGROUND', 'background.agsl'),
    ('RIBBON', 'ribbon.agsl'),
    ('WINDOW', 'window.agsl'),
    ('NIGHT_GLASS', 'nightglass.agsl'),
]
PLACEHOLDER = 'half4 main(float2 p){return half4(0);}\n'
# 定数 1 つの上限。JVM の定数プールは 1 文字列 64KB まで
MAX_UTF8_BYTES = 60_000


def read_source(name):
    path = os.path.join(SRC_DIR, name)
    if not os.path.exists(path):
        return PLACEHOLDER, 'placeholder (missing)'
    with open(path, encoding='utf-8') as f:
        text = f.read()
    if not text.strip():
        return PLACEHOLDER, 'placeholder (empty)'
    return text if text.endswith('\n') else text + '\n', 'ok'


def check(const, text):
    # Kotlin の生文字列の中で意味を持つもの: 終端の """ と、テンプレートの $
    if '"""' in text:
        sys.exit(f'{const}: the source contains a triple quote (""") and cannot be written as a raw string')
    if '$' in text:
        sys.exit(f'{const}: the source contains "$", which Kotlin treats as a template in a raw string')
    size = len(text.encode('utf-8'))
    if size > MAX_UTF8_BYTES:
        sys.exit(f'{const}: the source is {size} bytes, over the {MAX_UTF8_BYTES}-byte limit for one constant')


def main():
    parts = [
        'package com.example.businesscard.ui.kinu',
        '',
        '/**',
        ' * Kinu の AGSL シェーダーの本文(RuntimeShader で使う)。',
        ' *',
        ' * gen.py が生成。手で編集しない。元は docs/kinu/tools/src/ の .agsl。',
        ' * 書き直すときは python3 docs/kinu/tools/gen.py を実行する。',
        ' */',
        'internal object KinuShaders {',
    ]
    notes = []
    for const, name in SHADERS:
        text, state = read_source(name)
        check(const, text)
        notes.append(f'{name}: {state}')
        parts.append(f'    const val {const} = """')
        parts.append(text.rstrip('\n'))
        parts.append('"""')
        parts.append('')
    parts[-1:] = []  # 最後の空行を消す
    parts.append('}')
    out = '\n'.join(parts) + '\n'

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8') as f:
        f.write(out)
    print(f'wrote {os.path.normpath(OUT)}')
    for n in notes:
        print(' ', n)


if __name__ == '__main__':
    main()
