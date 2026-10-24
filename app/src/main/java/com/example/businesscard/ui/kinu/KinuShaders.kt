package com.example.businesscard.ui.kinu

/**
 * Kinu の AGSL シェーダーの本文(RuntimeShader で使う)。
 *
 * gen.py が生成。手で編集しない。元は docs/kinu/tools/src/ の .agsl。
 * 書き直すときは python3 docs/kinu/tools/gen.py を実行する。
 */
internal object KinuShaders {
    const val BACKGROUND = """
// Kinu の背景(画面全体)。SPEC.md §3・§6 BACKGROUND。
// 上=もやの空、黒い塔(銀の縦の波線)、下=金の帯 2〜3 本と、その間の琥珀の窓。各帯の上の縁に細い光の線。
// 計算は線形の光で行い、toneMapC で丸めてから sRGB に戻し、最後にディザを入れる(Porcelain と同じ並び)。
// uniform の色は sRGB のまま渡し、ここで toLinearSrgb する(Porcelain と同じ扱い)。
// 出力の y は画面の左上が原点。dp は 1dp の px。

uniform float2 size;
uniform float dp;
uniform float facadeTop;
uniform float ignite;
uniform float hdr;
layout(color) uniform half4 tower;
layout(color) uniform half4 fin;
layout(color) uniform half4 goldLit;
layout(color) uniform half4 goldShade;
layout(color) uniform half4 amber;
layout(color) uniform half4 lineCore;

const float PI = 3.14159265;

// ---- 塔の銀の縦の波線(dp) ----
const float FIN_STEP = 11.0;
const float FIN_HALF = 0.3;      // 線の幅 0.6dp の半分
const float FIN_WAVE_AMP = 2.5;
const float FIN_WAVE_LEN = 140.0;
// 線の明るさ(見本は黒地に低コントラストの灰銀。白く強すぎると糸に見える)
const float FIN_COVER = 0.36;

// ---- 帯と窓(dp) ----
const float BAND_H = 26.0;
const float WIN_H = 30.0;
const float BAND_PITCH = 56.0;   // BAND_H + WIN_H
const float BAND_COUNT = 3.0;
const float LOWER_GLASS = 138.0; // 最後の帯の下端(ここより下は窓だけ)
const float LIGHT_PITCH = 9.0;   // 天井の灯りの間隔(おおよそ。位置と明るさは揺らす)

// ---- 光の線 ----
const float LINE_CORE_K = 4.5;   // 芯の強さ(線形。hdr=1 では lineCore で止まる)
const float LINE_GLARE_K = 0.45; // 上へのにじみ
const float LINE_GLARE_UP = 3.0; // にじみの高さ(dp)

float3 toLin(half4 c) {
    return float3(toLinearSrgb(c.rgb));
}

float3 toDisplay(float3 c) {
    return float3(fromLinearSrgb(half3(c)));
}

// 画素ごとの乱数 0..1(ディザ用)
float hash12(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// 整数の番号から 0..1 の揺らぎ(線ごと・灯りごとの位置と明るさ)
float hash1(float n) {
    return fract(sin(n * 127.1 + 311.7) * 43758.5453);
}

// 線形の光を、各チャンネルの天井 C へ向けてなめらかに丸める。
// 0.8C までは変えない。大きな値は C へ漸近するので、光の芯は lineCore の色(hdr=1)か白の hdr 倍で止まる
float3 toneMapC(float3 x, float3 C) {
    float3 k = 0.8 * C;
    float3 over = max(x - k, float3(0.0));
    return min(x, k) + (C - k) * over / ((C - k) + over);
}

// 塔の銀の線の被覆率 0..1。線は x 方向に 11dp おき、位相と明るさ(0.6..1.0)は線ごとにずらす。
// 近い線(前後1本)だけを見れば足りる(うねりは ±2.5dp で、間隔 11dp より小さい)
float finCover(float xd, float yd) {
    float aa = 0.5 / dp;
    float fi0 = floor(xd / FIN_STEP + 0.5);
    float cov = 0.0;
    for (int k = -1; k <= 1; k++) {
        float i = fi0 + float(k);
        float xc = i * FIN_STEP + FIN_WAVE_AMP * sin(2.0 * PI * yd / FIN_WAVE_LEN + i * 2.399);
        float d = abs(xd - xc);
        float bright = 0.6 + 0.4 * hash1(i);
        cov = max(cov, bright * (1.0 - smoothstep(FIN_HALF - aa, FIN_HALF + aa, d)));
    }
    return cov;
}

// 金の帯の面。s は帯の上の縁からの深さ(dp、下が正)。
// 上の光の線で照らされ、陰の面へ落ちる。下 30% は下の琥珀の照り返しを受ける。つやは横に伸びる波。
float3 bandFace(float3 gl, float3 gs, float3 am, float s, float xd, float kf) {
    float lit = exp(-s / 6.0);
    float3 face = mix(gs * 0.8, gl, 0.05 + 0.85 * lit);
    face *= 0.92 + 0.08 * sin(PI * s / BAND_H);
    face += am * 0.22 * smoothstep(16.0, 26.0, s);
    float sh = pow(0.5 + 0.5 * sin(xd * 2.0 * PI / 300.0 + kf * 2.1), 6.0)
        * exp(-pow((s - 5.0) / 4.0, 2.0)) * 0.2;
    face += gl * sh;
    face = mix(face, gs * 0.5, smoothstep(23.0, 26.0, s));
    return face;
}

// 琥珀の窓。wv は窓の中の位置(0 = 上、1 = 下)。上に灯りの列、縦のカーテンのひだ、縦の桟。
// 灯りは間隔を大きくずらさず(おおよそ 9dp)、位置(横・縦)と明るさを灯りごとに揺らす。
float3 windowCol(float3 am, float3 lc, float wv, float xd, float vF, float dp2) {
    float3 w = mix(am * 1.1, am * 0.85, smoothstep(0.0, 0.5, wv));
    w = mix(w, am * 0.3, smoothstep(0.4, 1.0, wv));
    // 天井の灯り(芯は HDR で白を少し超える)
    float n = floor(xd / LIGHT_PITCH);
    float lights = 0.0;
    for (int k = -1; k <= 1; k++) {
        float idx = n + float(k);
        float cx = (idx + 0.5) * LIGHT_PITCH + 2.2 * (hash1(idx + 40.0) - 0.5) * 2.0;
        float cy = 2.7 + 1.2 * (hash1(idx + 80.0) - 0.5) * 2.0;
        float b = 0.55 + 0.45 * hash1(idx + 120.0);
        float dx = xd - cx;
        float dy = wv * WIN_H - cy;
        lights += b * exp(-(dx * dx + dy * dy) / 0.8);
    }
    w += lc * 1.2 * lights;
    // 縦の薄いカーテン(振幅 6%)
    w *= 1.0 + 0.06 * sin(xd * 2.0 * PI / 3.3 + floor(vF / BAND_PITCH) * 0.7);
    // 縦の桟(4分割、1dp 幅、30%)
    float mw = dp2 / 4.0;
    float mx = mod(xd, mw);
    float md = min(mx, mw - mx);
    float mc = 1.0 - smoothstep(0.5 - 0.5 / dp, 0.5 + 0.5 / dp, md);
    w = mix(w, w * 0.45, 0.3 * mc);
    return w;
}

half4 main(float2 p) {
    float aa = 0.5 / dp;
    float xd = p.x / dp;
    float yd = p.y / dp;
    float vF = (p.y - facadeTop) / dp;           // 足元の帯の上端からの深さ(dp)。負なら塔
    float H = (size.y - facadeTop) / dp;         // 足元の高さ(dp)
    float dpW = size.x / dp;                     // 画面の幅(dp)
    float xn = p.x / size.x;

    float3 tw = toLin(tower);
    float3 fi = toLin(fin);
    float3 gl = toLin(goldLit);
    float3 gs = toLin(goldShade);
    float3 am = toLin(amber);
    float3 lc = toLin(lineCore);

    // 光の線の登場: 左から右へ灯る
    float gate = clamp((ignite * 1.2 - xn) / 0.2, 0.0, 1.0);

    // ---- 光の線(帯ごとに1本。上縁に細い芯と、上へのにじみ) ----
    float3 lineAcc = float3(0.0);
    float3 haze = mix(tw, fi, 0.4) + am * 0.02;
    float warm = exp(min(vF, 0.0) / 60.0);       // 足元の帯の光が塔の下を暖める(上へ 60dp で減衰)

    // ---- 塔(上〜中) ----
    float fc = finCover(xd, yd);
    float3 finCol = mix(fi, am * 0.6, 0.4 * warm);
    float3 towerCol = mix(mix(haze, tw, smoothstep(0.0, 90.0, yd)), finCol, fc * FIN_COVER);
    towerCol += am * 0.10 * warm;

    // ---- 足元: 窓(地)。帯はこの上に、塔の側へもはみ出して描く ----
    float wv;
    if (vF >= LOWER_GLASS) {
        wv = clamp((vF - LOWER_GLASS) / max(H - LOWER_GLASS, 1.0), 0.0, 1.0);
    } else {
        wv = clamp((mod(vF, BAND_PITCH) - BAND_H) / WIN_H, 0.0, 1.0);
    }
    float3 facadeCol = windowCol(am, lc, wv, xd, vF, dpW);
    // 窓の地は足元より上(vF < 0)では使わない。塔の色を基調にする
    float3 col = vF < 0.0 ? towerCol : facadeCol;

    for (int k = 0; k < 3; k++) {
        float kf = float(k);
        // 帯の上縁は緩く波打つ(2本の正弦を重ねる)。波の上がり側は塔の上まで届く
        float e = kf * BAND_PITCH
            + 4.5 * sin(2.0 * PI * xd / 260.0 + kf * 1.7)
            + 2.4 * sin(2.0 * PI * xd / 120.0 + kf * 0.9 + 0.5);
        float s = vF - e;
        // 帯の面(vF で切らない。上縁が facadeTop より上へ出る所も帯として描く)
        if (kf < BAND_COUNT) {
            float cov = smoothstep(-aa, aa, s) * (1.0 - smoothstep(BAND_H - aa, BAND_H + aa, s));
            if (cov > 0.0) {
                col = mix(col, bandFace(gl, gs, am, max(s, 0.0), xd, kf), cov);
            }
        }
        // 上縁の光の線: 芯(幅 1.2dp)と、上へのにじみ(3dp)
        float core = exp(-(s * s) / 0.3);
        float upper = s < 0.0 ? exp(s / LINE_GLARE_UP) : 0.0;
        lineAcc += lc * (LINE_CORE_K * core + LINE_GLARE_K * upper);
    }
    lineAcc *= gate;

    col += lineAcc;

    // 通常は lineCore で止まり、HDR では白の hdr 倍まで出す
    float3 C = lc * max(hdr, 1.0);
    float3 shown = toDisplay(toneMapC(col, C));
    // ディザ: ±0.5 段ぶんの三角分布の揺らぎ(8bit の段差を消す)
    shown += (hash12(p) + hash12(p + float2(17.3, 41.7)) - 1.0) / 255.0;
    return half4(half3(shown), 1.0);
}
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
    // 面の大部分は見本の光る帯(淡い金・わずかにピンク寄り #CC9871〜#F3CAAE)。陰の色は細い溝だけに使う
    float3 shade = toLin(goldShade);
    float3 lit = toLin(goldLit);
    float3 bright = toLin(half4(0.9529, 0.7922, 0.6824, 1.0));   // #F3CAAE
    float3 face = mix(lit, bright, smoothstep(0.2, 0.9, v));
    // 縁の回り込み: 上 20% だけ陰へ寄せる(面の大部分は明るいまま)
    float wrap = exp(-max(v, 0.0) * H / 4.0);
    float3 base = mix(face, shade, 0.35 * wrap);
    base *= 1.3;                                          // 全体の平均の明るさを 1.5 倍程度に(白へ飛ばないよう 1.3 で止める)

    // 細い溝: 下寄りに 1 本だけ、狭く。最暗部も goldShade より暗くしない(赤茶の彩度を保つ)
    float gv = 0.62 + 0.05 * sin(PI2 * u + seed * 2.3);
    float groove = exp(-pow((v - gv) * H / 0.9, 2.0));
    float3 warmGroove = max(base * float3(0.84, 0.66, 0.56), shade * 1.5);
    base = mix(base, warmGroove, 0.6 * groove);

    // ---- つや: 横に伸びるヘアライン。うねりは 0.5〜1.5 周期。指の x へ寄せる ----
    bool touchOn = touch.x >= 0.0;
    float tg = touchOn ? exp(-pow((p.x - touch.x) / (size.x * 0.28), 2.0)) : 0.0;
    float vh = 0.42 + 0.05 * sin(PI2 * 1.0 * u + seed);
    float streak = exp(-pow((v - vh) * H / 0.9, 2.0));
    float row = hash12(float2(0.0, floor(p.y) + seed * 97.0));
    float hair = 0.88 + 0.24 * row;
    float gloss = streak * hair * (0.28 + 0.9 * tg);
    float3 sheen = lit * gloss * 0.7;

    // ---- 光の線: 芯 1.2dp(半幅 0.6)・上へのにじみ 3dp・下の面を舐める 30% の高さまで ----
    float core = exp(-pow(d / 0.6, 2.0));
    float halo = exp(d / 3.0) * step(d, 0.0) * 0.45;   // 縁の上へ 3dp のにじみ(下には出さない)
    float wash = exp(-max(d, 0.0) / max(0.3 * H, 0.5)) * 0.35;
    float3 coreCol = toLin(lineCore);
    // 芯の下側の暖色のにじみ(lineEdge #EBA874 相当。2〜4dp で帯の面へ柔らかく落ちる)。上側は部品の外なので無し
    float3 edgeCol = toLin(half4(0.9216, 0.6588, 0.4549, 1.0));
    float below = max(d, 0.0) * step(0.0, d);
    float warm = (exp(-below / 1.2) * 0.8 + exp(-below / 3.0) * 0.45) * step(0.0, d);
    float3 lightCol = coreCol * amt * (core * 1.9 + halo * 0.6) + coreCol * amt * wash * 0.5
        + edgeCol * amt * warm * 0.7;

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
    // 全体をクリーム寄りに明るく(#FFDEAA→#ECB272。奥は暗い琥珀)
    float3 hi = toLin(amberHi) * 1.15;
    float3 mid = toLin(amber) * 1.15;
    float3 deep = toLin(amberDeep) * 1.1;
    float3 col = mix(mid, hi, smoothstep(0.55, 0.0, v));
    col = mix(col, deep, smoothstep(0.45, 1.0, v));

    // 天井の灯り: 上 18% に点光源を横に並べる。HDR で白を少し超える
    float ceilY = 0.09 * size.y;
    float cellX = 22.0 * dp;
    float dx = (fract(p.x / cellX + 0.5) - 0.5) * cellX;
    float dyc = p.y - ceilY;
    // 点のにじみは半径を大きく(2.6dp)、中心は HDR で白の 2 倍近くまで(通常の画面では白で止まる)
    float lamp = exp(-(dx * dx + dyc * dyc) / (2.6 * dp * 2.6 * dp));
    float ceilBand = exp(-pow(dyc / (4.0 * dp), 2.0)) * 0.35;
    col += toLin(half4(1.0, 0.95, 0.80, 1.0)) * (lamp * 2.2 + ceilBand) * smoothstep(0.3, 0.0, v);

    // 縦の薄いカーテン: 柔らかい縦縞(振幅 11%。見本のひだに合わせて SPEC の 6% より強め)
    // 布のひだの間隔はまばら。x をゆるく歪ませて周期を不揃いにする(約 74dp と 27dp のうねりを重ねる)
    float xw = p.x + dp * (4.0 * sin(p.x / dp * 0.085 + seed * 2.0) + 2.0 * sin(p.x / dp * 0.23 + seed * 5.0));
    float fold = 0.5 + 0.5 * sin(xw / dp * (PI2 / 9.0) + seed * 3.1 + 0.6 * sin(p.y / dp * 0.05));
    col *= 1.0 - 0.11 * fold;

    // 縦の桟: 4分割、幅 1dp、#7A5A40 30%
    float cell = size.x / 4.0;
    float m = abs(fract(p.x / cell + 0.5) - 0.5) * cell;
    float bar = 1.0 - smoothstep(0.35 * dp, 0.5 * dp + 0.5, m);
    col = mix(col, toLin(mullion), 0.45 * bar);

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
