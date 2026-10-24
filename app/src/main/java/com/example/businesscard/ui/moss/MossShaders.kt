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
    float3 n = normalize(float3((h0 - hx) * 4.0, (h0 - hy) * 4.0, 1.0));
    // 大きな溝(割れ目)
    float crack = fbm(q / (160.0 * dp) + 31.0);
    float groove = 0.12 + 0.88 * smoothstep(0.0, 0.05, abs(crack - 0.5) + (vnoise(q / (2.0 * dp)) - 0.5) * 0.02);
    float ao = clamp(0.2 + 1.1 * h0, 0.0, 1.0) * groove;
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
    // 盛り上がった苔の群れは、すぐ下の岩に影を落とす(光は上から)
    float mUp = mossField(p - float2(0.0, 7.0 * dp), (p - float2(0.0, 7.0 * dp)) / size);
    col *= 1.0 - 0.6 * smoothstep(0.42, 0.6, mUp) * (1.0 - smoothstep(0.36, 0.5, m));
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
        float lam = clamp(max(dot(mn, normalize(float3(0.0, -0.6, 0.8))), 0.0) * (0.25 + 0.6 * c0) + 0.08 * dome, 0.0, 1.0);
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
// ---- MOSS: 苔のクッション(主ボタン・選択中)。星形の株が何層にも重なった、ふかふかで厚みのある苔 ----
// 部品の範囲(0..size)が上面。その下に厚み(thick)の側面があり、株が縁から垂れ下がる。まわりの岩に接地の暗がりを落とす。
uniform float2 size;
uniform float dp;
uniform float seed;
uniform float radius;
uniform float pad;
uniform float press;
uniform float thick;

// 上面の高さ(px)。縁から内側へ大きく盛り上がるドームに、株の塊の凹凸を足す
float cushionH(float2 p) {
    float d = -sdRoundRect(p, size, radius);
    float s = clamp(d / (16.0 * dp), 0.0, 1.0);
    float dome = 1.0 - (1.0 - s) * (1.0 - s);
    float clump = fbm(p / (16.0 * dp) + seed * 3.0 + 5.0);
    return dome * 14.0 * dp * (1.0 - 0.4 * press) + clump * 7.0 * dp * (1.0 - 0.5 * press);
}

half4 main(float2 pIn) {
    // 押すと上面が沈む(潰れて減った厚みの分だけ下へ)
    float sink = thick * 0.6 * press / max(1.0 - 0.6 * press, 0.4);
    float2 p = pIn - float2(0.0, sink);

    float d0 = sdRoundRect(p, size, radius);
    float dExt = sdRoundRect(p, float2(size.x, size.y + thick), radius);
    float rag = (vnoise(p / (3.0 * dp) + seed * 13.7) - 0.5) * 3.0 * dp;

    // ---- 上面 ----
    float e = dp;
    float hx = cushionH(p + float2(e, 0.0)) - cushionH(p - float2(e, 0.0));
    float hy = cushionH(p + float2(0.0, e)) - cushionH(p - float2(0.0, e));
    float3 nrm = normalize(float3(-hx / (2.0 * e), -hy / (2.0 * e), 1.0));
    float lam = max(dot(nrm, normalize(float3(0.0, -0.6, 0.8))), 0.0);

    // 側面での位置(0 = 上面の縁 .. 1 = 接地)。上面では 0
    float t = clamp(d0 / max(thick, 1.0), 0.0, 1.0) * step(0.0, d0);
    float onSide = (1.0 - smoothstep(-0.5, 0.5, dExt)) * smoothstep(-1.0 * dp, 1.0 * dp, d0);
    // 側面の苔は下を向いて暗い。株は下へ行くほどまばらに垂れる
    float lamS = mix(lam, 0.18 - 0.12 * t, onSide);

    // 縁の外(側面の外)へ行くほど株はまばらで小さい。pad の端では消える
    float outside = clamp(dExt / max(pad * 0.6, 1.0), 0.0, 1.0);
    float density = (1.0 - outside) * mix(1.0, 1.0 - 0.6 * t, onSide);
    float shrink = mix(1.0, 0.6, outside) * mix(1.0, 0.85, press);
    float4 mc = mossColor(p, dp, seed, lamS, density, shrink);
    float3 col = mc.rgb;
    // 上面の縁は丸く落ちて暗くなる。側面の下は根元の黒緑
    col *= mix(0.62, 1.0, smoothstep(0.0, 12.0 * dp, -d0)) * mix(1.0, 0.55, onSide * t);
    col *= 1.0 - 0.15 * press;

    // 中央の帯: 模様のコントラストを弱め、明るさを中間に寄せる(黒緑の文字を読みやすく)
    float ay = abs(p.y - size.y * 0.5) / size.y;
    float ax = smoothstep(0.0, 18.0 * dp, min(p.x, size.x - p.x));
    float wb = (1.0 - smoothstep(0.22, 0.36, ay)) * ax * (1.0 - onSide);
    float3 meanC = mossRamp(0.58) * (0.85 + 0.25 * lam) * (1.0 - 0.15 * press);
    col = mix(col, mix(meanC, col, 0.5), wb * 0.85);

    // 本体(上面+側面)の塗り。輪郭は株の形でほつれる
    float body = 1.0 - smoothstep(-1.5 * dp, 0.0, dExt + rag);
    // 側面の奥は根元の黒緑で埋める(株のすき間から岩が見えないように)
    float3 rootC = mossRamp(0.04) * (1.0 - 0.5 * t);
    col = mix(rootC, col, max(mc.a, 1.0 - onSide));
    float a = clamp(max(body, mc.a * density), 0.0, 1.0);
    float3 outC = col * a;

    // ---- 接地の暗がり(岩に落ちる) ----
    float shade = (1.0 - smoothstep(0.0, 7.0 * dp, dExt)) * step(0.0, dExt) * 0.6 * smoothstep(0.0, 2.0 * dp, thick);
    a = a + shade * (1.0 - a);
    return half4(half3(clamp(outC, 0.0, 1.0)), half(clamp(a, 0.0, 1.0)));
}
"""

    const val STONE = COMMON + """
// ---- STONE: 磨いた濡れ石の板(名刺・副ボタン・入力欄・ダイアログ)。厚みのある石で、縁に苔が生える ----
// 部品の範囲(0..size)が上面。その下に厚み(thick)の側面があり、まわりの岩に接地の暗がりを落とす。
uniform float2 size;
uniform float dp;
uniform float seed;
uniform float radius;
uniform float pad;
uniform float press;
uniform float sunken;
uniform float moss;
uniform float thick;

// 自然石らしく、輪郭を少し波打たせる量
float wobble(float2 p) {
    return (vnoise(float2(p.x, 0.0) / (30.0 * dp) + float2(seed * 5.0, p.y / (30.0 * dp))) - 0.5) * 3.0 * dp * (1.0 - sunken);
}

// 上面の高さ: 縁から 10dp で丸く落ちる面取り(溝なら逆)+ 浅い石肌
float slabH(float2 p, float d) {
    float s = clamp(-d / (10.0 * dp), 0.0, 1.0);
    float bevel = sqrt(1.0 - (1.0 - s) * (1.0 - s));
    float dir = mix(1.0, -1.0, sunken);
    float grain = granules(p / (3.6 * dp) + seed * 7.0) * 0.5 + vnoise(p / (20.0 * dp) + seed) * 0.5;
    return dir * bevel * 8.0 * dp + grain * 0.5 * dp;
}

half4 main(float2 pIn) {
    // 押すと上面が沈む(減った厚みの分だけ下へ)
    float sink = thick * 0.6 * press / max(1.0 - 0.6 * press, 0.4);
    float2 p = pIn - float2(0.0, sink);
    float w = wobble(p);
    float d0 = sdRoundRect(p, size, radius) + w;
    // 上面を下へ厚みの分だけ伸ばした形(上面+側面)
    float dExt = sdRoundRect(p, float2(size.x, size.y + thick), radius) + w;

    // ---- 上面 ----
    float faceA = 1.0 - smoothstep(-0.5, 0.5, d0);
    float3 face = float3(0.0);
    float2 uv = p / size;
    if (faceA > 0.0) {
        float e = 0.8 * dp;
        float h0 = slabH(p, d0);
        float hx = slabH(p + float2(e, 0.0), sdRoundRect(p + float2(e, 0.0), size, radius) + w);
        float hy = slabH(p + float2(0.0, e), sdRoundRect(p + float2(0.0, e), size, radius) + w);
        float3 n = normalize(float3((h0 - hx) / e, (h0 - hy) / e, 1.0));
        float inner = smoothstep(4.0 * dp, 18.0 * dp, -d0);
        float r = clamp(0.5 + 0.5 * (vnoise(p / (3.0 * dp) + seed) - 0.5), 0.0, 1.0);
        face = wetRock(n, 0.75, 0.8 + 0.2 * r, 0.45 + 0.3 * fbm(p / (60.0 * dp) + seed), mix(0.9, 0.6, inner));
        face = mix(face, float3(0.085, 0.078, 0.072) + (face - float3(0.085, 0.078, 0.072)) * 0.45, inner * 0.6);
        // 曇天の空の映り込み: 上側に斜めの柔らかいつや(溝には無い)
        float band = smoothstep(0.0, 0.25, uv.y) * (1.0 - smoothstep(0.25, 0.5, uv.y + (uv.x - 0.5) * 0.25));
        face += float3(0.32, 0.35, 0.37) * band * 0.10 * (1.0 - sunken) * (1.0 - 0.6 * press);
        // 溝は、上の縁の内側に深い影
        float lip = 1.0 - smoothstep(0.0, 9.0 * dp, p.y + 2.0 * dp * (1.0 - smoothstep(0.0, 6.0 * dp, -d0)));
        face *= (1.0 - 0.35 * sunken) * (1.0 - 0.55 * sunken * lip) * (1.0 - 0.08 * press);
    }

    // ---- 側面(厚み) ----
    float sideA = (1.0 - smoothstep(-0.5, 0.5, dExt)) * step(0.5, thick) * (1.0 - faceA);
    float3 side = float3(0.0);
    if (sideA > 0.0) {
        float t = clamp(d0 / max(thick, 1.0), 0.0, 1.0);    // 0 = 上の縁 .. 1 = 接地
        // 縦に流れる濡れた筋と、粗い割れ肌
        float streak = vnoise(float2(p.x / (1.6 * dp), p.y / (9.0 * dp)) + seed * 3.0);
        float grain = granules(p / (2.6 * dp) + seed * 11.0);
        float3 base = mix(float3(0.120, 0.110, 0.100), float3(0.030, 0.027, 0.025), smoothstep(0.0, 1.0, t));
        side = base * (0.75 + 0.35 * grain) * (0.85 + 0.3 * streak);
        // 上の縁は光を受けて細く光り、接地の近くは真っ黒
        side += float3(0.55, 0.56, 0.52) * (1.0 - smoothstep(0.0, 0.12, t)) * 0.12;
        // 濡れた筋の小さなつや
        side += float3(0.8, 0.8, 0.76) * smoothstep(0.82, 0.95, streak) * smoothstep(0.6, 0.9, grain) * (1.0 - t) * 0.5;
        side *= 1.0 - 0.6 * smoothstep(0.6, 1.0, t);
        // 左右の側は少し明るい(上からの光が回る)
        side *= 0.9 + 0.25 * abs(uv.x - 0.5);
    }

    float3 col = face * faceA + side * sideA;
    float a = faceA + sideA;

    // ---- 縁に生える苔(上面の縁と、側面へ垂れる所) ----
    if (moss > 0.001 && dExt > -18.0 * dp && dExt < pad) {
        float edgeBand = smoothstep(-16.0 * dp, -4.0 * dp, d0) * (1.0 - smoothstep(0.0, pad * 0.8, dExt));
        float pref = max(max(1.0 - smoothstep(0.0, 0.45, length(uv)), smoothstep(0.75, 1.0, uv.y)),
                         0.6 * smoothstep(0.85, 1.0, uv.x) * smoothstep(0.4, 0.9, uv.y));
        float f = fbm(p / (26.0 * dp) + seed * 9.0);
        float m = clamp(edgeBand * (pref * 1.1 + (f - 0.55) * 1.8) * moss * 1.6, 0.0, 1.0);
        // 苔は上面より盛り上がり、側面へは垂れ下がって暗くなる
        float onSide = smoothstep(0.0, 2.0 * dp, d0);
        float lam = 0.6 + 0.35 * (f - 0.5) + 0.25 * smoothstep(-8.0 * dp, 0.0, -d0) - 0.35 * onSide;
        float4 mc = mossColor(p, dp, seed + 3.0, clamp(lam, 0.0, 1.0), smoothstep(0.2, 0.7, m), mix(0.65, 1.0, m));
        float mm = mc.a * smoothstep(0.15, 0.5, m);
        float3 mcol = mc.rgb * (1.0 - 0.45 * onSide * clamp(d0 / max(thick, 1.0), 0.0, 1.0));
        col = col * (1.0 - mm) + mcol * mm;
        a = a * (1.0 - mm) + mm;
    }

    // ---- 接地の暗がり(岩に落ちる) ----
    float shade = (1.0 - smoothstep(0.0, 6.0 * dp, dExt)) * step(0.0, dExt) * 0.55 * smoothstep(0.0, 2.0 * dp, thick);
    col = col;
    a = a + shade * (1.0 - a);

    return half4(half3(clamp(col, 0.0, 1.0)), half(clamp(a, 0.0, 1.0)));
}
"""
}
