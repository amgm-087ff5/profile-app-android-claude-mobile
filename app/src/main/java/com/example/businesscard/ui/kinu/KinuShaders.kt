package com.example.businesscard.ui.kinu

/**
 * Kinu の AGSL シェーダーの本文(RuntimeShader で使う)。
 *
 * gen.py が生成。手で編集しない。元は docs/kinu/tools/src/*.agsl。
 * 書き直すときは python3 docs/kinu/tools/gen.py を実行する。
 */
internal object KinuShaders {
    const val BACKGROUND = """
half4 main(float2 p){return half4(0);}
"""

    const val RIBBON = """
// Kinu: 金の帯の面(RIBBON)。主ボタン・名刺の縁・選択中の印。SPEC §3・§6。
// 矩形いっぱいに描く(角丸は Compose 側で clip)。線形の光で計算し、toneMap → 出力 → ディザ。
uniform float2 size;
uniform float dp;
uniform float hdr;
uniform float line;
uniform float ignite;
uniform float2 touch;
uniform float seed;
layout(color) uniform half4 goldLit;
layout(color) uniform half4 goldShade;
layout(color) uniform half4 goldDeep;
layout(color) uniform half4 lineCore;

float3 toLin(half4 c) {
    return float3(toLinearSrgb(c.rgb));
}

float3 toDisplay(float3 c) {
    return float3(fromLinearSrgb(half3(c)));
}

float hash12(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// 線形の光を画面の明るさへ丸める。peak は出せる最大(通常 1、HDR は白の何倍まで)
float3 toneMap(float3 x, float peak) {
    float lum = dot(x, float3(0.2126, 0.7152, 0.0722));
    x = mix(x, float3(lum), smoothstep(0.75, 2.4, lum) * 0.8);
    float3 over = max(x - float3(0.8), float3(0.0));
    return min(x, float3(0.8)) + over / (float3(1.0) + over / (peak - 0.8));
}

half4 main(float2 p) {
    float u = clamp(p.x / size.x, 0.0, 1.0);
    float v = clamp(p.y / size.y, 0.0, 1.0);
    float H = size.y / dp;          // 帯の高さ(dp)
    float PI2 = 6.28318;

    // ---- 上縁: 波打つ線(上縁の高さは帯ごとの位相で少しうねる)。d は縁からの距離(dp、負は上)----
    float edgeDp = 0.9 + 0.35 * sin(PI2 * u + seed) + 0.12 * sin(PI2 * 2.0 * u + seed * 1.7);
    float d = p.y / dp - edgeDp;

    // ---- 点灯: 左から右へ線が灯る(ignite 0→1)----
    float front = ignite * 1.2 - 0.1;
    float on = 1.0 - smoothstep(front - 0.08, front + 0.08, u);
    float amt = line * on * (0.92 + 0.08 * sin(PI2 * 1.5 * u + seed));

    // ---- 面の明るさ: 上は赤茶の陰、下へ向かって琥珀の照り返しで明るい。上 20% で縁が丸く回り込む ----
    float3 shade = toLin(goldShade);
    float3 lit = toLin(goldLit);
    float3 deep = toLin(goldDeep);
    float wrap = exp(-max(v, 0.0) * H / 5.0);            // 縁の回り込みの明るさ(上の数 dp)
    float3 base = mix(shade, lit, smoothstep(0.25, 1.0, v));
    base += shade * 0.35 * wrap;

    // 緩い溝: 下へ向かってゆるく凹む(うねりつき)
    float gv = 0.62 + 0.05 * sin(PI2 * u + seed * 2.3);
    float groove = exp(-pow((v - gv) * H / 2.6, 2.0));
    base = mix(base, deep, 0.42 * groove);

    // ---- つや: 横に伸びるヘアライン。うねりは 0.5〜1.5 周期。指の x へ寄せる ----
    bool touchOn = touch.x >= 0.0;
    float tg = touchOn ? exp(-pow((p.x - touch.x) / (size.x * 0.28), 2.0)) : 0.0;
    float vh = 0.42 + 0.05 * sin(PI2 * 1.0 * u + seed);
    float streak = exp(-pow((v - vh) * H / 0.9, 2.0));
    float row = hash12(float2(0.0, floor(p.y) + seed * 97.0));
    float hair = 0.75 + 0.5 * row;
    float gloss = streak * hair * (0.28 + 0.9 * tg);
    float3 sheen = mix(lit, float3(1.0), 0.4) * gloss * 0.5;

    // ---- 光の線: 芯 1.2dp(半幅 0.6)・上へのにじみ 3dp・下の面を舐める 30% の高さまで ----
    float core = exp(-pow(d / 0.6, 2.0));
    float halo = exp(-max(-d, 0.0) / 3.0) * 0.45;
    float wash = exp(-max(d, 0.0) / max(0.3 * H, 0.5)) * 0.35;
    float3 coreCol = toLin(lineCore);
    float3 lightCol = coreCol * amt * (core * 2.2 + halo * 0.6) + coreCol * amt * wash * 0.5;

    float3 lin = base + sheen + lightCol;
    float3 shown = toDisplay(toneMap(lin, max(hdr, 1.0)));
    // ディザ: ±0.5 段ぶんの三角分布の揺らぎ(8bit の段差を消す)
    shown += (hash12(p) + hash12(p + float2(17.3, 41.7)) - 1.0) / 255.0;
    return half4(half3(shown), 1.0);
}
"""

    const val WINDOW = """
// Kinu: 琥珀の窓(WINDOW)。名刺の面。SPEC §3・§6。
// 上 18% に天井の灯り、縦の薄いカーテンのひだ、縦の桟(4分割)、手前のガラスに青海波の点描。
uniform float2 size;
uniform float dp;
uniform float hdr;
uniform float glow;
uniform float seed;
layout(color) uniform half4 amberHi;
layout(color) uniform half4 amber;
layout(color) uniform half4 amberDeep;
layout(color) uniform half4 mullion;

float3 toLin(half4 c) {
    return float3(toLinearSrgb(c.rgb));
}

float3 toDisplay(float3 c) {
    return float3(fromLinearSrgb(half3(c)));
}

float hash12(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float3 toneMap(float3 x, float peak) {
    float lum = dot(x, float3(0.2126, 0.7152, 0.0722));
    x = mix(x, float3(lum), smoothstep(0.75, 2.4, lum) * 0.8);
    float3 over = max(x - float3(0.8), float3(0.0));
    return min(x, float3(0.8)) + over / (float3(1.0) + over / (peak - 0.8));
}

half4 main(float2 p) {
    float u = clamp(p.x / size.x, 0.0, 1.0);
    float v = clamp(p.y / size.y, 0.0, 1.0);
    float PI2 = 6.28318;

    // 上から下へ: 明るい琥珀 → 琥珀 → 奥の暗い琥珀
    float3 hi = toLin(amberHi);
    float3 mid = toLin(amber);
    float3 deep = toLin(amberDeep);
    float3 col = mix(mid, hi, smoothstep(0.55, 0.0, v));
    col = mix(col, deep, smoothstep(0.45, 1.0, v));

    // 天井の灯り: 上 18% に点光源を横に並べる。HDR で白を少し超える
    float ceilY = 0.09 * size.y;
    float cellX = 22.0 * dp;
    float dx = (fract(p.x / cellX + 0.5) - 0.5) * cellX;
    float dyc = p.y - ceilY;
    float lamp = exp(-(dx * dx + dyc * dyc) / (1.3 * dp * 1.3 * dp));
    float ceilBand = exp(-pow(dyc / (3.0 * dp), 2.0)) * 0.25;
    col += toLin(amberHi) * (lamp * 1.1 + ceilBand) * smoothstep(0.3, 0.0, v);

    // 縦の薄いカーテン: 柔らかい縦縞(振幅 6%)
    float fold = 0.5 + 0.5 * sin(p.x / dp * (PI2 / 13.0) + seed * 3.1 + 0.6 * sin(p.y / dp * 0.05));
    col *= 1.0 - 0.06 * fold;

    // 縦の桟: 4分割、幅 1dp、#7A5A40 30%
    float cell = size.x / 4.0;
    float m = abs(fract(p.x / cell + 0.5) - 0.5) * cell;
    float bar = 1.0 - smoothstep(0.35 * dp, 0.5 * dp + 0.5, m);
    col = mix(col, toLin(mullion), 0.3 * bar);

    // 青海波の点描(不透明度 6%。扇の半径 10dp)
    float rowH = 10.0 * dp;
    float row = floor(p.y / rowH);
    float ox = mod(row, 2.0) * 10.0 * dp;
    float cx = floor((p.x - ox) / (20.0 * dp) + 0.5) * 20.0 * dp + ox;
    float cy = (row + 1.0) * rowH;
    float2 q = float2(p.x - cx, p.y - cy);
    float r = length(q) / (10.0 * dp);
    float ring = 1.0 - smoothstep(0.0, 0.08, abs(fract(r * 2.0) - 0.5) - 0.42);
    float fan = step(q.y, 0.0) * step(r, 1.0) * ring;
    col = mix(col, col * 0.7, 0.06 * fan);

    // 灯りの強さ(0 → 暗いガラス、1 → 琥珀)
    float3 dark = toLin(amberDeep) * 0.12;
    float3 lin = mix(dark, col, smoothstep(0.0, 1.0, glow));

    float3 shown = toDisplay(toneMap(lin, max(hdr, 1.0)));
    shown += (hash12(p) + hash12(p + float2(17.3, 41.7)) - 1.0) / 255.0;
    return half4(half3(shown), 1.0);
}
"""

    const val NIGHT_GLASS = """
// Kinu: 夜のガラス(NIGHT_GLASS)。副ボタン・アイコンボタン・入力欄・ダイアログ。SPEC §3・§6。
// 黒いガラス。上の縁に弱い光の線。塔の銀の縦の波線がごくわずかに映る。縁は 1dp の細い線。
uniform float2 size;
uniform float dp;
uniform float hdr;
uniform float line;
uniform float ember;
uniform float2 touch;
layout(color) uniform half4 glassTop;
layout(color) uniform half4 glassBottom;
layout(color) uniform half4 fin;
layout(color) uniform half4 lineCore;
layout(color) uniform half4 amber;

float3 toLin(half4 c) {
    return float3(toLinearSrgb(c.rgb));
}

float3 toDisplay(float3 c) {
    return float3(fromLinearSrgb(half3(c)));
}

float hash12(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float3 toneMap(float3 x, float peak) {
    float lum = dot(x, float3(0.2126, 0.7152, 0.0722));
    x = mix(x, float3(lum), smoothstep(0.75, 2.4, lum) * 0.8);
    float3 over = max(x - float3(0.8), float3(0.0));
    return min(x, float3(0.8)) + over / (float3(1.0) + over / (peak - 0.8));
}

half4 main(float2 p) {
    float u = clamp(p.x / size.x, 0.0, 1.0);
    float v = clamp(p.y / size.y, 0.0, 1.0);
    float PI2 = 6.28318;

    // 黒いガラス: 上がわずかに明るく、下へ沈む
    float3 col = mix(toLin(glassTop), toLin(glassBottom), v);

    // 塔の銀の縦の波線が映り込む(不透明度 5%)。間隔 11dp・うねり振幅 2.5dp・波長 140dp
    float idx = floor(p.x / (11.0 * dp));
    float ph = idx * 1.7;
    float wx = p.x - 2.5 * dp * sin(PI2 * p.y / (140.0 * dp) + ph);
    float fx = abs(fract(wx / (11.0 * dp) + 0.5) - 0.5) * 11.0 * dp;
    float wire = exp(-pow(fx / (0.4 * dp), 2.0));
    col += toLin(fin) * wire * 0.05;

    // 上の縁の光の線(弱め。line 0.35 が通常)。芯 1dp 前後、上にじみ 3dp
    bool touchOn = touch.x >= 0.0;
    float tg = touchOn ? exp(-pow((p.x - touch.x) / (size.x * 0.25), 2.0)) : 0.0;
    float lineAmt = line * (touchOn ? (0.6 + 0.8 * tg) : 1.0);
    float d = p.y / dp - 0.5;                       // 縁からの距離(dp)
    float core = exp(-pow(d / 0.6, 2.0));
    float halo = exp(-max(d, 0.0) / 3.0) * 0.4;
    float3 lineCol = toLin(lineCore);
    col += lineCol * lineAmt * (core * 1.8 + halo * 0.5) * (0.9 + 0.1 * sin(PI2 * u));

    // 奥の琥珀のにじみ(入力中の欄)。下寄りの中央
    float2 e = float2(u - 0.5, (v - 0.7) * 1.4);
    float bloom = exp(-dot(e, e) / 0.18);
    col += toLin(amber) * ember * bloom * 0.16;

    // 縁: 1dp の細い線(#8B7A62 40% 相当。アンバーを暗く混ぜて代用)
    float edgeD = min(min(p.x, size.x - p.x), min(p.y, size.y - p.y)) / dp;
    float rimMask = 1.0 - smoothstep(0.5, 1.2, edgeD);
    float3 rimCol = mix(toLin(amber), toLin(glassBottom), 0.45);
    col = mix(col, rimCol, 0.4 * rimMask);

    float3 shown = toDisplay(toneMap(col, max(hdr, 1.0)));
    shown += (hash12(p) + hash12(p + float2(17.3, 41.7)) - 1.0) / 255.0;
    return half4(half3(shown), 1.0);
}
"""
}
