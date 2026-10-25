package com.example.businesscard.ui.kinu

/**
 * Kinu の AGSL シェーダーの本文(RuntimeShader で使う)。
 *
 * gen.py が生成。手で編集しない。元は docs/kinu/tools/src/ の .agsl。
 * 書き直すときは python3 docs/kinu/tools/gen.py を実行する。
 */
internal object KinuShaders {
    const val BACKGROUND = """
// Kinu の背景(画面全体)。SPEC.md v2-2・v2-3 BACKGROUND。見本は外装の布のドレープだけ(ref_facade.png)。
// 上端と下端に、厚みのある布の帯が 4 段重なる(各 画面高の 16%)。
// 各帯の上の縁は巻き込んで光の線を受け、下の縁は foldGold で折り返す。帯の間は窓(クリーム〜琥珀)のすき間。
// 中央は広いトープ〜灰紫のサテンの陰の面(縦にごく淡いヘアライン、横にゆるいうねり)。
// 帯の光の線は近くの面を暖かく照らす(40dp で減衰)。
// 計算は線形の光で行い、toneMapC で丸めてから sRGB に戻し、最後にディザを入れる。
// uniform の色は sRGB のまま渡し、ここで toLinearSrgb する。出力の y は画面の左上が原点。dp は 1dp の px。

uniform float2 size;
uniform float dp;
uniform float ignite;
uniform float hdr;
layout(color) uniform half4 facadeShade;
layout(color) uniform half4 facadeMauve;
layout(color) uniform half4 foldGold;
layout(color) uniform half4 goldLit;
layout(color) uniform half4 amber;
layout(color) uniform half4 amberHi;
layout(color) uniform half4 lineCore;

const float PI = 3.14159265;

// ---- 帯の領域(画面の上端・下端から各 16%) ----
const float ZONE = 0.16;
const float LAYERS = 4.0;        // 帯の段数(ループは 4 回)
const float BAND_FRAC = 0.72;    // 帯の高さ / 段の間隔
const float FADE_IN = 10.0;      // 下の領域の上側のなじみ(dp)
const float FADE_OUT = 14.0;     // 上の領域の下側のなじみ(dp)

// ---- 光の線 ----
const float LINE_CORE_K = 4.5;   // 芯の強さ(線形。hdr=1 では lineCore で止まる)
const float LINE_GLARE_K = 0.45; // 上へのにじみ
const float LINE_GLARE_UP = 3.0; // にじみの高さ(dp)

// ---- 中央の面 ----
const float LIGHT_REACH = 40.0;  // 帯の光が面を照らす距離(dp)
const float WARM_K = 0.12;       // 面が受ける暖かい照り返しの強さ

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

// 線形の光を、各チャンネルの天井 C へ向けてなめらかに丸める。
// 0.8C までは変えない。大きな値は C へ漸近するので、光の芯は lineCore の色(hdr=1)か白の hdr 倍で止まる
float3 toneMapC(float3 x, float3 C) {
    float3 k = 0.8 * C;
    float3 over = max(x - k, float3(0.0));
    return min(x, k) + (C - k) * over / ((C - k) + over);
}

// 段 k の上縁の位置(dp、帯の領域の上端からの距離)。緩く大きな波(画面幅で約 1 周期)を重ねる。
// 段の間隔より振幅が大きいので、帯は時々重なる(奥の段が手前の段の上に乗る)。
float edgeAt(float k, float xd, float pitch) {
    return k * pitch
        + 5.5 * sin(2.0 * PI * xd / 360.0 + 1.1 * k + 0.4)
        + 2.2 * sin(2.0 * PI * xd / 170.0 + 0.7 * k);
}

// 布の帯の面。s は上縁からの深さ(dp、0 以上)。上の縁は光の線に舐められて金、下へ向かって
// 陰の面(トープ〜灰紫)へ落ちる。下の縁は foldGold の折り返し。つやは横に伸びる緩い波。
float3 bandFace(float3 sh, float3 gl, float3 am, float3 fold, float s, float bh, float xd, float kf) {
    float lit = exp(-s / 7.0);
    float3 litCol = mix(am * 0.9, gl, 0.45);
    float3 face = mix(sh, litCol, 0.12 + 0.88 * lit);
    // つや: 上縁の少し下に、横へ伸びる弱いハイライト(段ごとに位相をずらす)
    float glint = pow(0.5 + 0.5 * sin(2.0 * PI * xd / 240.0 + kf * 2.1), 8.0)
        * exp(-((s - 6.0) * (s - 6.0)) / 12.0);
    face += gl * 0.16 * glint;
    // 下の縁の折り返し(下向きの面が照らされた金)
    float fm = smoothstep(bh - 8.0, bh - 1.0, s);
    face = mix(face, fold, 0.85 * fm);
    return face;
}

// 帯の間の窓。wv は窓の中の位置(0 = 上の帯の下端、1 = 次の帯の上縁)。
// 上がクリーム、中ほどが琥珀、下が深い琥珀。縦のブラインドを不揃いに、縦の桟を 4 分割。
float3 windowCol(float3 hi, float3 am, float3 deep, float wv, float xd, float wdp) {
    float3 w = mix(hi, am, smoothstep(0.0, 0.45, wv));
    w = mix(w, deep, smoothstep(0.5, 1.0, wv));
    float blind = 0.5 + 0.5 * sin(2.0 * PI * xd / 2.4 + 3.0 * sin(xd * 0.37));
    w *= 0.97 + 0.03 * blind;
    float mw = wdp / 4.0;
    float mx = mod(xd, mw);
    float md = min(mx, mw - mx);
    float mc = 1.0 - smoothstep(0.25, 0.55, md);
    w = mix(w, w * 0.6, 0.25 * mc);
    return w;
}

half4 main(float2 p) {
    float aa = 0.5 / dp;
    float xd = p.x / dp;
    float xn = p.x / size.x;
    float H = size.y;
    float zpx = ZONE * H;            // 帯の領域の高さ(px)
    float Zdp = zpx / dp;            // 同じ(dp)
    float pitch = Zdp / LAYERS;      // 段の間隔(dp)
    float bh = pitch * BAND_FRAC;    // 帯の高さ(dp)
    float wdp = size.x / dp;

    float3 sh = mix(toLin(facadeShade), toLin(facadeMauve), 0.5);
    float3 gl = toLin(goldLit);
    float3 am = toLin(amber);
    float3 hi = toLin(amberHi);
    float3 deep = am * 0.6;
    float3 fold = toLin(foldGold);
    float3 lc = toLin(lineCore);

    // 光の線の登場: 左から右へ灯る
    float gate = clamp((ignite * 1.2 - xn) / 0.2, 0.0, 1.0);

    // ---- 中央の陰の面(サテン): 横にゆるいうねり、縦のごく淡いヘアライン ----
    float yd = p.y / dp;
    float wave = 1.0 + 0.06 * sin(2.0 * PI * xd / 380.0 + 0.8 + 0.5 * sin(2.0 * PI * yd / 210.0));
    float hx = mod(xd + 0.5 * sin(yd * 0.05), 5.0) - 2.5;
    float hair = 0.035 * exp(-(hx * hx) / 0.12) - 0.012;
    float3 satin = sh * (wave + hair);

    // 帯の光が近くの面を暖める(上と下の領域の縁から 40dp で減衰)
    float dTop = max(yd - Zdp, 0.0);
    float dBot = max(((H - zpx) - p.y) / dp, 0.0);
    float warmAmt = exp(-dTop / LIGHT_REACH) + exp(-dBot / LIGHT_REACH);
    satin += am * WARM_K * warmAmt * (0.85 + 0.15 * sin(2.0 * PI * xd / 300.0));

    // ---- 帯の領域(上端と下端のどちらか) ----
    bool bottom = p.y > H * 0.5;
    float ly = bottom ? (p.y - (H - zpx)) / dp : yd;
    float ff = bottom ? smoothstep(-FADE_IN, -2.0, ly) : 1.0 - smoothstep(Zdp - FADE_OUT, Zdp, ly);

    float3 zc = satin;
    float3 lineAcc = float3(0.0);
    // 段ごとに、帯(上縁〜下縁)、その下の窓(次の帯の上縁まで)の順に塗る。後の段が前の段の上に乗る
    for (int k = 0; k < 4; k++) {
        float kf = float(k);
        float e0 = edgeAt(kf, xd, pitch);
        float e1 = edgeAt(kf + 1.0, xd, pitch);

        // 帯 k
        float s = ly - e0;
        float cov = smoothstep(-aa, aa, s) * (1.0 - smoothstep(bh - aa, bh + aa, s));
        zc = mix(zc, bandFace(sh, gl, am, fold, max(s, 0.0), bh, xd, kf), cov);

        // 光の線: 芯(幅 1.2dp 前後)と、上へのにじみ、下へのわずかな広がり
        float core = exp(-(s * s) / 0.45);
        float up = s < 0.0 ? exp(s / LINE_GLARE_UP) : 0.0;
        float down = s > 0.0 ? exp(-s / 2.0) : 0.0;
        lineAcc += lc * (LINE_CORE_K * core + LINE_GLARE_K * up + 0.12 * down);

        // 窓: 帯 k の下端から次の帯の上縁まで(最後の段は領域の終わりまで)
        float wStart = e0 + bh;
        float ws = ly - wStart;
        float wcov = smoothstep(-aa, aa, ws);
        float wv = clamp(ws / max(e1 - wStart, 1.0), 0.0, 1.0);
        zc = mix(zc, windowCol(hi, am, deep, wv, xd, wdp), wcov);
    }

    float3 col = mix(satin, zc, ff);
    col += lineAcc * gate;

    // 通常は lineCore で止まり、HDR では白の hdr 倍まで出す
    float3 C = lc * max(hdr, 1.0);
    float3 shown = toDisplay(toneMapC(col, C));
    // ディザ: ±0.5 段ぶんの三角分布の揺らぎ(8bit の段差を消す)
    shown += (hash12(p) + hash12(p + float2(17.3, 41.7)) - 1.0) / 255.0;
    return half4(half3(shown), 1.0);
}
"""

    const val RIBBON = """
// Kinu: 金の帯の面(RIBBON)。v2。主ボタン・名刺の縁・選択中の印。SPEC v2-1・v2-3。
// 矩形いっぱいに描く(角丸は Compose 側で clip)。線形の光で計算し、toneMap → 出力 → ディザ。
// 面は #D99B74〜#EDBE8F。上の巻き込みに光の線、その直下に goldShade の細い陰、下の縁は #C6A772 で折り返す。
// つやは横に伸びる鋭い1本(幅は高さの 8%、#F9E2CE)を、ゆるくうねらせる。
// uniform は v1 のまま。面の中間色・折り返しの金・つやの色はトークンに無いので定数で持つ(一覧は統括に報告)。
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

    // ---- 上縁: 波打つ線(帯ごとの位相でゆるくうねる)。d は縁からの距離(dp、負は上)----
    float edgeDp = 0.9 + 0.35 * sin(PI2 * u + seed) + 0.12 * sin(PI2 * 2.0 * u + seed * 1.7);
    float d = p.y / dp - edgeDp;

    // ---- 点灯: 左から右へ線が灯る(ignite 0→1)----
    float front = ignite * 1.2 - 0.1;
    float on = 1.0 - smoothstep(front - 0.08, front + 0.08, u);
    float amt = line * on * (0.92 + 0.08 * sin(PI2 * 1.5 * u + seed));

    // ---- 色(線形へ変換して使う) ----
    float3 faceMid = toLin(half4(0.8510, 0.6078, 0.4549, 1.0));   // #D99B74
    float3 faceLit = toLin(goldLit);                              // #EDBE8F(トークン)
    float3 foldCol = toLin(half4(0.7765, 0.6549, 0.4471, 1.0));   // #C6A772 下の縁の折り返し
    float3 shade = toLin(goldShade);                              // 巻き込みの直下の細い陰だけ
    float3 gloss = toLin(half4(0.9765, 0.8863, 0.8078, 1.0));     // #F9E2CE つやと光の際
    float3 coreCol = toLin(lineCore);                             // #FFF4E6 光の芯(HDR で白を超える)
    float3 edgeCol = toLin(half4(0.8275, 0.5882, 0.4353, 1.0));   // #D3966F 光の際の暖色

    // ---- 面: 上は #D99B74、下へ #EDBE8F(下からの琥珀の照り返し)。中央がゆるく沈む ----
    float3 face = mix(faceMid, faceLit, smoothstep(0.1, 0.8, v));
    face *= 1.0 - 0.04 * sin(3.14159 * clamp((v - 0.05) / 0.9, 0.0, 1.0));

    // 下の縁の折り返し: 下 14% ほどが #C6A772 へ。折り返しの境に細い影
    float fold = smoothstep(0.84, 0.97, v);
    face = mix(face, foldCol, 0.9 * fold);
    float crease = exp(-pow((v - 0.835) * H / 0.45, 2.0));
    face *= 1.0 - 0.12 * crease;

    // 上の巻き込みの直下の細い陰(d が 0〜4dp だけ。陰は細い帯)
    float under = step(0.0, d);
    float shadeBand = under * exp(-d / 1.1) * 0.95;
    float3 base = mix(face, shade, shadeBand);

    // ---- つや: 横に伸びる鋭い1本。幅は高さの 8%(全幅)。うねりは 1 周期。指の x へ寄る ----
    bool touchOn = touch.x >= 0.0;
    float tg = touchOn ? exp(-pow((p.x - touch.x) / (size.x * 0.22), 2.0)) : 0.0;
    float vh = 0.46 + 0.09 * sin(PI2 * 1.0 * u + seed) + 0.03 * sin(PI2 * 2.0 * u + seed * 2.1);
    float gdy = (v - vh) * size.y;                     // px
    float halfW = 0.045 * size.y;                      // 全幅 8%(わずかに広めに取り、縁は鋭く)
    float streak = exp(-pow(abs(gdy) / halfW, 6.0));   // 縁が鋭く立つ(平らな頂 + 急な裾)
    float taper = smoothstep(0.0, 0.06, u) * smoothstep(1.0, 0.94, u);
    base += gloss * streak * taper * (0.7 + 1.0 * tg) * 0.75;

    // ---- 光の線: 芯 1.2dp(半幅 0.6)・上へのにじみ 3dp・下の面を舐める 30% の高さまで ----
    float core = exp(-pow(d / 0.6, 2.0));
    float halo = exp(d / 3.0) * step(d, 0.0) * 0.45;   // 縁の上へ 3dp のにじみ(下には出さない)
    float wash = exp(-max(d, 0.0) / max(0.3 * H, 0.5)) * 0.35;
    float below = max(d, 0.0) * under;
    float warmA = exp(-below / 1.0) * 0.9 * under;      // 際 #F9E2CE(1dp)
    float warmB = exp(-below / 3.0) * 0.5 * under;      // 際 #D3966F(3dp まで)
    float3 lightCol = coreCol * amt * (core * 1.9 + halo * 0.6)
        + coreCol * amt * wash * 0.5
        + gloss * amt * warmA * 0.6
        + edgeCol * amt * warmB * 0.6;

    float3 lin = base + lightCol;
    float3 shown = toDisplay(toneMap(lin, max(hdr, 1.0)));
    // ディザ: ±0.5 段ぶんの三角分布の揺らぎ(8bit の段差を消す)
    shown += (hash12(p) + hash12(p + float2(17.3, 41.7)) - 1.0) / 255.0;
    return half4(half3(shown), 1.0);
}
"""

    const val WINDOW = """
// Kinu: 琥珀の窓(WINDOW)。v2。名刺の面と、入力欄(glow 0.35 = 落ち着いたクリームのガラス)。SPEC v2-1・v2-3。
// 上の 9% に天井の灯りの列、縦のブラインド(約 2.5dp 周期・不揃い)、縦の桟(4分割)。
// glow 0 は薄いクリームのガラス、1 で琥珀に灯る。
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
    float xd = p.x / dp;                       // x を dp 単位で

    // ---- 琥珀の面: 上 #F1CFAF(クリーム)→ 中 #E7AD84 → 奥 #BA7D50(線形の値はトークンから) ----
    float3 hi = toLin(amberHi);
    float3 mid = toLin(amber);
    float3 deep = toLin(amberDeep);
    float3 col = mix(hi, mid, smoothstep(0.0, 0.5, v));
    col = mix(col, deep, smoothstep(0.5, 1.0, v));

    // ---- 天井の灯り: 上 9% に点光源を横に並べる。HDR で白を少し超える ----
    float ceilY = 0.09 * size.y;
    float cellX = 22.0 * dp;
    float dx = (fract(p.x / cellX + 0.5) - 0.5) * cellX;
    float dyc = p.y - ceilY;
    float lamp = exp(-(dx * dx + dyc * dyc) / (2.6 * dp * 2.6 * dp));
    float ceilBand = exp(-pow(dyc / (4.0 * dp), 2.0)) * 0.35;
    col += toLin(half4(1.0, 0.95, 0.80, 1.0)) * (lamp * 1.9 + ceilBand * 0.8) * (1.0 - smoothstep(0.0, 0.3, v));

    // ---- 縦のブラインド: 約 2.5dp 周期。位相をゆるく揺らして不揃いにする ----
    float jit = 0.5 * sin(xd * 0.37 + seed * 2.0) + 0.35 * sin(xd * 0.91 + seed * 4.3);
    float blind = 0.5 + 0.5 * sin(PI2 * xd / 2.5 + 1.6 * jit);

    // ---- 縦の桟: 4分割、幅 1dp、#987960 45% ----
    float cell = size.x / 4.0;
    float m = abs(fract(p.x / cell + 0.5) - 0.5) * cell;
    float bar = 1.0 - smoothstep(0.35 * dp, 0.5 * dp + 0.5, m);
    col = mix(col, toLin(mullion), 0.45 * bar);

    // ---- 灯りの強さ: glow 0 は落ち着いたクリームのガラス、1 は琥珀 ----
    // 入力欄の glow 0.35 は、クリームがほぼ勝つ(琥珀は薄く混ざる)
    float3 cream = toLin(half4(0.9569, 0.8863, 0.8000, 1.0));     // #F4E2CC
    float3 rest = cream * (1.0 - 0.12 * v);
    float3 lin = mix(rest, col, clamp(glow, 0.0, 1.0));

    // ブラインドの陰(細かい縦縞。振幅 8%)
    lin *= 1.0 - 0.08 * blind;

    float3 shown = toDisplay(toneMap(lin, max(hdr, 1.0)));
    shown += (hash12(p) + hash12(p + float2(17.3, 41.7)) - 1.0) / 255.0;
    return half4(half3(shown), 1.0);
}
"""

    const val SATIN = """
// Kinu: サテンの金属(SATIN)。v2。副ボタン・アイコンボタン・入力欄・ダイアログの面。SPEC v2-2・v2-3。
// 上がやや明るい灰紫(glassTop)、下がトープ(glassBottom)。縦の細かいヘアライン。
// 上の縁に細い光の線(line 0.35 が通常)。touch で指の x へ寄る。1dp の縁(rim)。
uniform float2 size;
uniform float dp;
uniform float hdr;
uniform float line;
uniform float2 touch;
layout(color) uniform half4 glassTop;
layout(color) uniform half4 glassBottom;
layout(color) uniform half4 rim;
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

    // ---- 面: 上の灰紫 → 下のトープ ----
    float3 col = mix(toLin(glassTop), toLin(glassBottom), smoothstep(0.0, 1.0, v));

    // サテンの横のゆるい揺れ(振幅 2%)
    col *= 1.0 + 0.02 * sin(PI2 * u + 0.8) + 0.01 * sin(PI2 * 2.5 * u + 2.1);

    // 縦の細かいヘアライン: 列ごとにわずかに明暗(振幅 ±2.5%)
    float hl = hash12(float2(floor(p.x), 7.0));
    col *= 1.0 + 0.09 * (hl - 0.5);

    // ---- 上の縁の光の線: 芯 1dp 前後、下へのにじみ 3dp。touch で指の x へ寄る ----
    bool touchOn = touch.x >= 0.0;
    float tg = touchOn ? exp(-pow((p.x - touch.x) / (size.x * 0.25), 2.0)) : 0.0;
    float lineAmt = line * (touchOn ? (0.6 + 0.8 * tg) : 1.0);
    float d = p.y / dp - 0.5;                       // 縁からの距離(dp)
    float core = exp(-pow(d / 0.5, 2.0));
    float halo = exp(-max(d, 0.0) / 3.0) * 0.4;
    float3 lineCol = toLin(lineCore);
    col += lineCol * lineAmt * (core * 1.6 + halo * 0.45) * (0.9 + 0.1 * sin(PI2 * u));

    // ---- 縁: 1dp の細い線(rim。アルファはトークンのまま使う) ----
    float edgeD = min(min(p.x, size.x - p.x), min(p.y, size.y - p.y)) / dp;
    float rimMask = 1.0 - smoothstep(0.5, 1.2, edgeD);
    col = mix(col, toLin(rim), rimMask * rim.a * 0.5);

    float3 shown = toDisplay(toneMap(col, max(hdr, 1.0)));
    shown += (hash12(p) + hash12(p + float2(17.3, 41.7)) - 1.0) / 255.0;
    return half4(half3(shown), 1.0);
}
"""
}
