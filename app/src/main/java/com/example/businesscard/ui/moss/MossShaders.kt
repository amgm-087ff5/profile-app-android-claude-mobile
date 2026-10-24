package com.example.businesscard.ui.moss

/**
 * Moss(苔岩)の AGSL シェーダー(Android 13 以上の RuntimeShader で描く)。
 *
 *  - ROCK: 画面全体の背景。雨に濡れた黒い岩(溶岩のような不揃いのこぶ、こぶの頂上の濡れたつや、割れ目)、
 *    画面の下と端に這う苔の群れ、地衣類の斑点、杉の枯れ葉。画面の上ほど奥で、苔庭がぼける(浅い被写界深度)
 *  - MOSS: 苔のクッション(主ボタン・選択中)。細い葉が星形に開いた株を3層重ね、上からの光で上面を明るく、谷を黒緑に
 *  - STONE: 磨いた濡れ石の板(名刺・副ボタン・入力欄・ダイアログ)。面取りの縁のつや、曇天の映り込み、縁に生える苔
 *
 * 共通の uniform: size(描く大きさ、px), dp(1dp の px 数), seed(模様をずらす種)。
 * MOSS / STONE は (-pad..size+pad) の矩形に描き、premultiplied の色を返す(縁の苔が形の外へほつれるため)。
 * 宣言した uniform はすべて使う(コードから値を入れるので、使われずに消えると設定時に落ちる)。
 * 重い計算(苔の株・岩のこぶ)は、要る所だけで行う(苔の無い所・完全にぼけた所は飛ばす)。
 *
 * 手元での確認: docs/moss/tools/moss.py(skia-python で同じシェーダーを CPU で描く)。
 */
object MossShaders {
    private const val COMMON = """
// ---- 共通: 乱数・雑音・形・苔の株 ----
const float TAU = 6.2831853;

float hash21(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float2 hash22(float2 p) {
    float3 p3 = fract(float3(p.xyx) * float3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

// なめらかな雑音(0..1)
float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + float2(1.0, 0.0)), u.x),
               mix(hash21(i + float2(0.0, 1.0)), hash21(i + float2(1.0, 1.0)), u.x), u.y);
}

// 大きさの違う雑音を4段重ねたもの(0..1)
float fbm(float2 p) {
    float s = 0.0;
    float a = 0.5;
    float2 q = p;
    for (int i = 0; i < 4; i++) {
        s += a * vnoise(q);
        q = q * 2.03 + float2(11.7, 3.1);
        a *= 0.5;
    }
    return s / 0.9375;
}

// 丸い粒の盛り上がり(0..1)。セルごとに1つの粒。中心が高く、粒の間は 0
float granules(float2 q) {
    float2 c0 = floor(q);
    float h = 0.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            float2 cell = c0 + float2(float(i), float(j));
            float2 ctr = cell + 0.2 + 0.6 * hash22(cell);
            float r = 0.55 + 0.35 * hash21(cell + 7.1);
            float d = length(q - ctr) / r;
            h = max(h, (1.0 - d * d) * (0.6 + 0.4 * hash21(cell + 3.3)));
        }
    }
    return clamp(h, 0.0, 1.0);
}

// 角丸長方形の距離(中は負、外は正)。p は左上を 0 とした座標、b は大きさ
float sdRoundRect(float2 p, float2 b, float r) {
    float rr = min(r, min(b.x, b.y) * 0.5);
    float2 q = abs(p - b * 0.5) - (b * 0.5 - float2(rr));
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - rr;
}

// 苔の色の並び。x は 0(根元の黒緑) から 1(先端の淡い黄緑)
float3 mossRamp(float x) {
    float3 c = mix(float3(0.070, 0.098, 0.031), float3(0.235, 0.306, 0.086), smoothstep(0.0, 0.25, x));
    c = mix(c, float3(0.502, 0.604, 0.212), smoothstep(0.25, 0.5, x));
    c = mix(c, float3(0.718, 0.855, 0.325), smoothstep(0.5, 0.75, x));
    c = mix(c, float3(0.860, 0.945, 0.500), smoothstep(0.75, 1.0, x));
    return c;
}

// 1層ぶんの苔の株(細い葉が星形に開く)。cs は格子の大きさ(px)。
// keepBias: 株を残す割合(0..1)。shrink: 株の大きさの倍率
// 戻り値: x = 被覆, y = 株の中での位置(0 根元..1 先端), z = 株の乱数, w = 水滴
float4 tuftLayer(float2 p, float cs, float2 sd, float dpx, float keepBias, float shrink, out float dirOut) {
    float2 q = p / cs;
    float2 cell0 = floor(q);
    float bestC = 0.0;
    float bestT = 0.0;
    float bestR = 0.0;
    float bestW = 0.0;
    float bestD = 0.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            float2 cell = cell0 + float2(float(i), float(j));
            float2 key = cell + 0.5 + sd;
            float rnd = hash21(key);
            float2 ctr = cell + 0.5 + (hash22(key + 3.7) - 0.5) * 0.9;
            float keep = step(1.0 - keepBias, fract(rnd * 3.91 + 0.17));
            float R = (0.62 + 0.18 * fract(rnd * 5.3)) * shrink * cs;
            float2 v = q - ctr;
            float r = length(v) * cs;
            float ur = r / max(R, 0.001);
            float n = 7.0 + floor(rnd * 4.0);
            float P = TAU / n;
            float ang = atan(v.y, v.x) - fract(rnd * 7.31) * P;
            float f = ang / P;
            float dA = abs(f - floor(f + 0.5)) * P;
            float dperp = dA * r;
            float hw = (0.26 + 0.2 * fract(rnd * 13.7)) * dpx * (1.0 - 0.7 * ur);
            float leaf = 1.0 - smoothstep(hw * 0.55, hw, dperp);
            float core = 1.0 - smoothstep(0.1, 0.3, ur);
            float inside = 1.0 - smoothstep(0.9, 1.0, ur);
            float cov = max(leaf, core * 0.7) * inside * keep;
            float spot = smoothstep(0.78, 1.0, ur) * step(0.88, fract(rnd * 31.7)) * cov;
            if (cov > bestC) {
                bestC = cov;
                bestT = ur;
                bestR = rnd;
                bestW = spot;
                bestD = -v.y / max(length(v), 0.001);
            }
        }
    }
    dirOut = bestD;
    return float4(bestC, clamp(bestT, 0.0, 1.0), bestR, bestW);
}

// 苔の面の色。lam = 群れの起伏への上からの光(0..1)、density = 株の密度(0..1)、shrink = 株の大きさ。
// 戻り値: rgb と、株が覆っている割合(a)
float4 mossColor(float2 p, float dpx, float seedv, float lam, float density, float shrink) {
    float gapN = vnoise(p / (1.2 * dpx) + seedv * 5.0);
    float3 col = mossRamp(0.03 + 0.08 * gapN + 0.1 * lam);
    float covMax = 0.0;
    for (int k = 0; k < 3; k++) {
        float kk = float(k);
        float dirY = 0.0;
        float layerKeep = clamp(density * (1.15 - 0.15 * kk), 0.0, 1.0);
        float4 tf = tuftLayer(p, (3.5 + kk) * dpx, float2(seedv * 19.3 + kk * 37.1, seedv * 11.7 + kk * 21.9), dpx, layerKeep, shrink, dirY);
        float idx = 0.12 + 0.68 * pow(tf.y, 1.5) + 0.35 * (lam - 0.6) + 0.12 * (tf.z - 0.5) + 0.12 * dirY + 0.06 * kk;
        float3 lc = mossRamp(clamp(idx, 0.0, 1.0));
        float lum = dot(lc, float3(0.3, 0.59, 0.11));
        lc = mix(float3(lum), lc, 1.2) * (0.9 + 0.2 * fract(tf.z * 41.9));
        lc = mix(lc, float3(0.92, 0.97, 0.78), tf.w * 0.85);
        col = mix(col, lc, tf.x);
        covMax = max(covMax, tf.x);
    }
    col *= 0.35 + 0.9 * lam;
    return float4(col, covMax);
}

// 濡れた岩の高さ(0..1 くらい)。s = 粒の細かさの倍率(石の板は浅く細かい)
float rockH(float2 p, float dpx, float seedv) {
    // 粒の並びを雑音でゆがめ、溶岩のような不揃いのこぶにする
    float2 w = float2(vnoise(p / (14.0 * dpx) + seedv), vnoise(p / (14.0 * dpx) + seedv + 9.0)) - 0.5;
    float2 pw = p + w * 9.0 * dpx;
    float g1 = granules(pw / (3.6 * dpx) + seedv * 7.0);
    float g2 = granules(pw / (11.0 * dpx) + seedv * 3.0 + 17.0);
    float big = fbm(p / (55.0 * dpx) + seedv);
    float mixv = vnoise(p / (30.0 * dpx) + seedv + 4.0);
    return (0.20 + 0.25 * mixv) * g1 + 0.45 * g2 + 0.45 * big;
}

// 濡れた岩肌の色。n = 法線、h = 高さ、ao = 溝の暗さ(0..1、1 が開けた面)
float3 wetRock(float3 n, float h, float ao, float tint, float glintAmt) {
    float3 L = normalize(float3(-0.15, -0.6, 0.78));
    float3 V = float3(0.0, 0.0, 1.0);
    float3 albedo = mix(float3(0.060, 0.054, 0.050), float3(0.112, 0.104, 0.096), tint);
    float diff = max(dot(n, L), 0.0);
    float3 col = albedo * (0.35 + 1.1 * diff) * ao;
    // 濡れたつや: 粒の頂上の小さな白い点
    float3 Hv = normalize(L + V);
    float spec = pow(max(dot(n, Hv), 0.0), 140.0);
    col += float3(0.90, 0.90, 0.86) * spec * glintAmt * ao * smoothstep(0.45, 0.85, h);
    // 曇天の空の映り込み(広く淡い)
    float sky = pow(max(dot(n, normalize(float3(0.0, -0.35, 0.94))), 0.0), 10.0);
    col += float3(0.40, 0.42, 0.42) * sky * 0.10;
    return col;
}
"""

    const val ROCK = COMMON + """
// ---- ROCK: 画面全体の背景。雨に濡れた黒い岩、端と下に這う苔、上ほど奥でぼけた苔庭 ----
uniform float2 size;
uniform float dp;
uniform float seed;
uniform float focus;

// 苔の生える場所(0..1)。画面の下と左右の端に多く、中央(部品が乗る所)には生えない
float mossField(float2 p, float2 uv) {
    float n = fbm(p / (70.0 * dp) + seed * 4.3 + 2.0);
    float edge = max(smoothstep(0.66, 1.0, uv.y), 0.75 * smoothstep(0.84, 1.0, abs(uv.x - 0.5) * 2.0) * smoothstep(0.2, 0.6, uv.y));
    float centre = (1.0 - smoothstep(0.30, 0.42, abs(uv.x - 0.5))) * (1.0 - smoothstep(0.24, 0.34, abs(uv.y - 0.52)));
    float n2 = fbm(p / (24.0 * dp) + seed * 7.1 + 5.0);
    return clamp(edge * 0.62 + (n - 0.5) * 1.3 + (n2 - 0.5) * 0.35 - centre * 0.7 + 0.12, 0.0, 1.0);
}

// 杉の枯れ葉(細い枝に小さな鱗片)。a = 始点、b = 終点
float cedarTwig(float2 p, float2 a, float2 b) {
    float2 pa = p - a;
    float2 ba = b - a;
    float t = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    float2 off = pa - ba * t;
    float d = length(off);
    float along = t * length(ba) / (1.6 * dp);
    float scale = 0.5 + 0.5 * cos(along * TAU);
    float w = (0.6 + 1.0 * scale) * dp * (1.0 - 0.5 * t);
    return 1.0 - smoothstep(w * 0.6, w, d);
}

half4 main(float2 p) {
    float2 uv = p / size;
    float2 q = p + seed * float2(53.1, 27.7);

    // ぼけの強さ: 画面の上(奥)ほど強い。focus より下はくっきり
    float blur = 1.0 - smoothstep(focus * 0.25, focus, uv.y);

    // ---- 岩肌(くっきり) ----
    float3 col = float3(0.0);
    if (blur < 0.995) {
    float e = 0.8 * dp;
    float h0 = rockH(q, dp, seed);
    float hx = rockH(q + float2(e, 0.0), dp, seed);
    float hy = rockH(q + float2(0.0, e), dp, seed);
    float3 n = normalize(float3((h0 - hx) * 2.6, (h0 - hy) * 2.6, 1.0));
    // 大きな溝(割れ目)
    float crack = fbm(q / (160.0 * dp) + 31.0);
    float groove = 0.12 + 0.88 * smoothstep(0.0, 0.05, abs(crack - 0.5) + (vnoise(q / (2.0 * dp)) - 0.5) * 0.02);
    float ao = clamp(0.35 + 0.9 * h0, 0.0, 1.0) * groove;
    float tint = fbm(q / (120.0 * dp) + 9.0);
    col = wetRock(n, h0, ao, tint, 1.3);

    // 地衣類の白っぽい斑点(まばら)
    float2 lc = floor(q / (34.0 * dp));
    float2 lctr = (lc + 0.2 + 0.6 * hash22(lc + 5.5)) * 34.0 * dp;
    float lr = (1.5 + 3.5 * hash21(lc + 2.2)) * dp;
    float lich = (1.0 - smoothstep(lr * 0.6, lr, length(q - lctr) + (vnoise(q / (0.9 * dp)) - 0.5) * 1.6 * dp))
        * step(0.93, hash21(lc + 9.9));
    col = mix(col, float3(0.62, 0.64, 0.58) * (0.55 + 0.45 * h0), lich * 0.85);

    // ---- 苔の群れ ----
    float m = mossField(p, uv);
    if (m > 0.3) {
        float mE = 4.0 * dp;
        float mx = mossField(p + float2(mE, 0.0), (p + float2(mE, 0.0)) / size);
        float my = mossField(p + float2(0.0, mE), (p + float2(0.0, mE)) / size);
        // 群れの盛り上がり(クッション)。群れの縁ほど急に下がる
        float dome = smoothstep(0.5, 0.75, m);
        // 群れの中の小さなクッション(株の塊)。塊の上は明るく、塊の間は深い谷
        float cE = 2.5 * dp;
        float2 cq = q / (24.0 * dp) + 13.0;
        float c0 = granules(cq);
        float cx = granules(cq + float2(cE, 0.0) / (24.0 * dp));
        float cy = granules(cq + float2(0.0, cE) / (24.0 * dp));
        float3 mn = normalize(float3((m - mx) * 7.0 + (c0 - cx) * 2.2, (m - my) * 7.0 + (c0 - cy) * 2.2, 1.0));
        float lam = clamp(max(dot(mn, normalize(float3(0.0, -0.6, 0.8))), 0.0) * (0.35 + 0.45 * c0) + 0.1 * dome, 0.0, 1.0);
        // 群れの縁では株がまばらに(岩の上に小さな株が点在)
        float density = smoothstep(0.38, 0.58, m);
        float4 mc = mossColor(p, dp, seed, lam, density, mix(0.7, 1.0, dome));
        col = mix(col, mc.rgb, mc.a * smoothstep(0.36, 0.5, m));
        // 苔の縁の根元に落ちる陰
        col *= 1.0 - 0.35 * (smoothstep(0.30, 0.48, m) - smoothstep(0.48, 0.62, m));

}
    // 杉の枯れ葉(画面の下半分に3本)
    for (int k = 0; k < 3; k++) {
        float kk = float(k);
        float2 r0 = hash22(float2(kk * 7.3 + seed * 3.0, 1.9));
        float2 a = float2(0.08 + 0.84 * r0.x, 0.60 + 0.36 * r0.y) * size;
        float ang = hash21(float2(kk, seed + 4.0)) * TAU;
        float len = (22.0 + 18.0 * hash21(float2(kk + 2.0, 8.0))) * dp;
        float2 b = a + float2(cos(ang), sin(ang)) * len;
        float tw = cedarTwig(p, a, b);
        col = mix(col, float3(0.60, 0.36, 0.22) * (0.75 + 0.25 * vnoise(p / dp)), tw * 0.95);
    }

    }

    // ---- 奥の苔庭(ぼけ) ----
    float g = fbm(p / (150.0 * dp) + seed * 2.0 + 40.0);
    float g2 = fbm(p / (60.0 * dp) + 77.0);
    float3 garden = mix(float3(0.36, 0.37, 0.35), float3(0.46, 0.55, 0.22), smoothstep(0.42, 0.62, g));
    garden = mix(garden, float3(0.16, 0.17, 0.14), smoothstep(0.5, 0.8, g2) * 0.45);
    garden *= 0.70 + 0.55 * (1.0 - uv.y / max(focus, 0.01));
    // 丸いぼけ(水滴の光)
    float2 bc = floor(p / (70.0 * dp));
    float2 bctr = (bc + 0.25 + 0.5 * hash22(bc + 1.7)) * 70.0 * dp;
    float br = (6.0 + 10.0 * hash21(bc + 6.1)) * dp;
    float bok = (1.0 - smoothstep(br * 0.75, br, length(p - bctr))) * step(0.78, hash21(bc + 4.4));
    garden += float3(0.55, 0.60, 0.45) * bok * 0.18;

    col = mix(col, garden, blur);
    // ぼけと岩のあいだ(focus の少し下)は、苔庭の光で少しかすむ
    col = mix(col, garden * 0.6, (1.0 - smoothstep(focus, focus + 0.12, uv.y)) * (1.0 - blur) * 0.35);

    return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
}
"""

    const val MOSS = COMMON + """
// ---- MOSS: 苔のクッション(主ボタン・選択中)。星形の株が何層にも重なった、ふかふかの苔 ----
uniform float2 size;
uniform float dp;
uniform float seed;
uniform float radius;
uniform float pad;
uniform float press;

// クッションの高さ(px)。縁から内側へ盛り上がるドームに、株の塊の凹凸を足す
float cushionH(float2 p) {
    float d = -sdRoundRect(p, size, radius);
    float s = clamp(d / (12.0 * dp), 0.0, 1.0);
    float dome = 1.0 - (1.0 - s) * (1.0 - s);
    float clump = fbm(p / (16.0 * dp) + seed * 3.0 + 5.0);
    return dome * 9.0 * dp + clump * 7.0 * dp * (1.0 - 0.5 * press);
}

half4 main(float2 p) {
    float d0 = sdRoundRect(p, size, radius);
    // 縁のほつれ: 輪郭を雑音でゆがめる
    float rag = (vnoise(p / (3.0 * dp) + seed * 13.7) - 0.5) * 3.0 * dp;
    float body = 1.0 - smoothstep(-1.5 * dp, 0.0, d0 + rag);

    float e = dp;
    float hx = cushionH(p + float2(e, 0.0)) - cushionH(p - float2(e, 0.0));
    float hy = cushionH(p + float2(0.0, e)) - cushionH(p - float2(0.0, e));
    float3 nrm = normalize(float3(-hx / (2.0 * e), -hy / (2.0 * e), 1.0));
    float lam = max(dot(nrm, normalize(float3(0.0, -0.6, 0.8))), 0.0);

    // 縁の外へ行くほど株はまばらで小さい。pad の端では消える
    float outside = clamp(d0 / max(pad, 1.0), 0.0, 1.0);
    float density = 1.0 - outside;
    float shrink = mix(1.0, 0.6, outside) * mix(1.0, 0.85, press);
    float4 mc = mossColor(p, dp, seed, lam, density, shrink);
    float3 col = mc.rgb;
    col *= mix(0.72, 1.0, smoothstep(0.0, 10.0 * dp, -d0));
    col *= 1.0 - 0.15 * press;

    // 中央の帯: 模様のコントラストを弱め、明るさを中間に寄せる(黒緑の文字を読みやすく)
    float ay = abs(p.y - size.y * 0.5) / size.y;
    float ax = smoothstep(0.0, 18.0 * dp, min(p.x, size.x - p.x));
    float wb = (1.0 - smoothstep(0.22, 0.36, ay)) * ax;
    float3 meanC = mossRamp(0.56) * (1.0 - 0.15 * press);
    col = mix(col, mix(meanC, col, 0.45), wb * 0.9);

    float a = clamp(max(body, mc.a * density), 0.0, 1.0);
    return half4(half3(clamp(col, 0.0, 1.0) * a), half(a));
}
"""

    const val STONE = COMMON + """
// ---- STONE: 磨いた濡れ石の板(名刺・副ボタン・入力欄・ダイアログ)。縁に苔が生える ----
uniform float2 size;
uniform float dp;
uniform float seed;
uniform float radius;
uniform float pad;
uniform float press;
uniform float sunken;
uniform float moss;

// 自然石らしく、輪郭を少し波打たせた距離
float stoneD(float2 p) {
    float wob = (vnoise(p / (30.0 * dp) + seed * 5.0) - 0.5) * 3.0 * dp;
    return sdRoundRect(p, size, radius) + wob * (1.0 - sunken);
}

// 面の高さ: 縁から 8dp で丸く落ちる面取り(溝なら逆)+ 浅い石肌
float slabH(float2 p) {
    float d = -stoneD(p);
    float s = clamp(d / (8.0 * dp), 0.0, 1.0);
    float bevel = sqrt(1.0 - (1.0 - s) * (1.0 - s));
    float dir = mix(1.0, -1.0, sunken);
    // 磨いた石なので粒は1段で浅く
    float grain = granules(p / (3.6 * dp) + seed * 7.0) * 0.5 + vnoise(p / (20.0 * dp) + seed) * 0.5;
    return dir * bevel * 6.0 * dp + grain * 0.9 * dp;
}

half4 main(float2 p) {
    float d0 = stoneD(p);
    float aa = 1.0 - smoothstep(-0.5, 0.5, d0);

    float e = 0.8 * dp;
    float h0 = slabH(p);
    float hx = slabH(p + float2(e, 0.0));
    float hy = slabH(p + float2(0.0, e));
    float3 n = normalize(float3((h0 - hx) / e, (h0 - hy) / e, 1.0));

    // 中央は文字が読めるよう、明るさの揺らぎを小さく
    float inner = smoothstep(4.0 * dp, 16.0 * dp, -d0);
    float r = clamp(h0 / dp - mix(1.0, -1.0, sunken) * 6.0 * clamp(-d0 / (8.0 * dp), 0.0, 1.0), 0.0, 1.0);
    float3 col = wetRock(n, r, 0.75 + 0.25 * r, 0.45 + 0.3 * fbm(p / (60.0 * dp) + seed), mix(0.75, 0.6, inner));
    col = mix(col, float3(0.085, 0.078, 0.072) + (col - float3(0.085, 0.078, 0.072)) * 0.45, inner * 0.6);

    // 曇天の空の映り込み: 上側に斜めの柔らかいつや(溝には無い)
    float2 uv = p / size;
    float band = smoothstep(0.0, 0.25, uv.y) * (1.0 - smoothstep(0.25, 0.5, uv.y + (uv.x - 0.5) * 0.25));
    col += float3(0.32, 0.35, 0.37) * band * 0.10 * (1.0 - sunken) * (1.0 - 0.6 * press);
    // 溝は全体に暗い。押すと少し暗く(沈む)
    col *= (1.0 - 0.35 * sunken) * (1.0 - 0.08 * press);

    float a = aa;
    // 縁に生える苔: 左上の角と下の辺を中心に、縁の帯の中だけ
    if (moss > 0.001 && d0 > -18.0 * dp) {
        float band2 = smoothstep(-16.0 * dp, -4.0 * dp, d0) * (1.0 - smoothstep(0.0, pad * 0.9, d0));
        float pref = max(max(1.0 - smoothstep(0.0, 0.45, length(uv - float2(0.0, 0.0))), smoothstep(0.75, 1.0, uv.y)),
                         0.6 * smoothstep(0.85, 1.0, uv.x) * smoothstep(0.4, 0.9, uv.y));
        float f = fbm(p / (26.0 * dp) + seed * 9.0);
        float m = clamp(band2 * (pref * 1.1 + (f - 0.55) * 1.8) * moss * 1.6, 0.0, 1.0);
        float lam = 0.55 + 0.35 * (f - 0.5) + 0.25 * smoothstep(-8.0 * dp, 0.0, -d0);
        float4 mc = mossColor(p, dp, seed + 3.0, clamp(lam, 0.0, 1.0), smoothstep(0.2, 0.7, m), mix(0.65, 0.95, m));
        float mm = mc.a * smoothstep(0.15, 0.5, m);
        col = mix(col, mc.rgb, mm);
        a = max(a, mm);
    }
    return half4(half3(clamp(col, 0.0, 1.0) * a), half(a));
}
"""
}
