package com.example.businesscard.ui.moss

/**
 * Moss テーマの絵を描く AGSL シェーダー(Android 13 以上の RuntimeShader で GPU 上で動かす)。
 *
 * - ROCK: 画面全体の背景。濡れた黒い岩に、上はぼけた苔庭、下と端はくっきりした苔が広がる
 * - MOSS: 苔のクッション(主ボタン・選択中の選択肢)。細い葉が星形に開いた小さな株が密集する
 * - STONE: 磨いた濡れ石の板(名刺・副ボタン・入力欄・ダイアログ)
 *
 * ## 苔の株の作り方
 * 画面を細かいマス目に分け、マスごとに株の中心を1つずつ置く(ボロノイの考え方)。
 * 1画素では近くの 3x3 マスの株だけを調べ、いちばん濃い株の値を使う。
 * 株は中心から放射する細い葉の集まり。葉の数は 6〜9 本で、株ごとに変える。
 *
 * ## 光の考え方
 * 光は上(画面の上の方)から当たる。岩や石の表面の小さな凹凸は、雑音の高さの差から
 * 法線を作り、その法線と光の向きの内積で明るさを決める。濡れた所は鏡面反射で白い点が立つ。
 *
 * すべてのシェーダーは同じ決まりで書く(ui/moss/ の描画コードと対応)。
 * - 共通の uniform: size(描く大きさ、px), dp(1dp の px 数), seed(模様をずらす種)
 * - 出力は premultiplied(外側は a = 0)。色は画面の表示の値(線形化はしない)
 * - 雑音関数は各シェーダーの中に書く。AGSL に #include は無いので COMMON をつなげて使う
 *
 * docs/moss/tools/moss.py が、この文字列を正規表現で取り出して Skia(CPU)で同じ絵を描く。
 * 文字列の中では "$" を使わないこと。
 */
object MossShaders {

    /** 共通の関数。雑音、角丸の形、株の葉、表面の高さ、葉の色、帯の位置。 */
    private const val COMMON = """
const float PI = 3.14159265;

// 乱数(0..1)。画素の位置から決まる
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

// 大きさの違う雑音を4段重ねたもの(0..1 くらい)
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

// 角丸長方形の距離(中は負、外は正)。p は左上を 0 とした座標
float sdRoundRect(float2 p, float2 b, float r) {
    float2 q = abs(p - b * 0.5) - (b * 0.5 - float2(r));
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

// 株の1本ぶんの濃さ(0..1)。r は株の中心からの距離(株の半径 = 1)、a は角度。
// 葉は 6〜9 本。細い葉(spike)と、根元の詰まった芯(core)を足す
float tuftStar(float r, float a, float rnd) {
    float n = 6.0 + floor(rnd * 4.0);
    float ph = fract(rnd * 7.31) * 6.2832;
    float lobe = 0.5 + 0.5 * cos(n * a + ph);
    float spike = pow(lobe, 8.0) * smoothstep(1.0, 0.3, r);
    float core = smoothstep(0.6, 0.2, r);
    return max(core * 0.9, spike);
}

// 株の色。tip は中心からの位置(0 = 根元、1 = 先端)。先端が明るい黄緑、根元は黒緑
float3 leafColor(float tip, float rnd) {
    float3 root = float3(0.02, 0.045, 0.012);
    float3 shadow = float3(0.09, 0.22, 0.04);
    float3 mid = float3(0.30, 0.58, 0.11);
    float3 bright = float3(0.74, 0.93, 0.30);
    float3 c = mix(root, shadow, smoothstep(0.0, 0.35, tip));
    c = mix(c, mid, smoothstep(0.3, 0.7, tip));
    c = mix(c, bright, smoothstep(0.7, 1.0, tip));
    c *= 0.85 + 0.3 * rnd;
    // 株の先の、細かく白く光る水滴
    float sp = smoothstep(0.82, 1.0, tip) * step(0.7, fract(rnd * 23.7));
    return mix(c, float3(0.92, 1.0, 0.80), sp * 0.85);
}

// 岩や石の表面の高さ(凹凸の粒)
float surfH(float2 q, float dp) {
    return 0.65 * fbm(q / (2.4 * dp)) + 0.35 * fbm(q / (0.8 * dp));
}

// 苔が生える割合(0..1)。岩の下の帯と端に多く、上の方と中央は少ない
float bandAmt(float2 p, float2 sz, float dp, float seed) {
    float2 uv = p / sz;
    float n1 = fbm(p / (45.0 * dp) + seed * 3.1);
    float mb = smoothstep(0.55, 0.85, uv.y + (n1 - 0.5) * 0.45);
    float n2 = fbm(p / (35.0 * dp) + seed * 5.3 + 9.0);
    float ms = smoothstep(0.80, 0.98, abs(uv.x - 0.5) * 2.0 + (n2 - 0.5) * 0.5);
    return max(mb, ms);
}
"""

    /**
     * ROCK: 画面全体の背景(静止画)。
     * 上ほど苔庭がぼけ、下と端には苔が這う。中央は部品が乗るので、模様は落ち着いた暗さ。
     */
    const val ROCK = COMMON + """
uniform float2 size;
uniform float dp;
uniform float seed;
// ぼけが消えてくっきりする位置(画面の高さに対する 0..1)
uniform float focus;

half4 main(float2 p) {
    float2 uv = p / size;
    float2 q = p + seed * float2(53.1, 27.7);

    // ---- 岩肌: 粒の凹凸から法線を作り、左上からの光で陰影 ----
    float h0 = surfH(q, dp);
    float hx = surfH(q + float2(1.0, 0.0), dp);
    float hy = surfH(q + float2(0.0, 1.0), dp);
    float3 n = normalize(float3(-(hx - h0) * 6.0 * dp, -(hy - h0) * 6.0 * dp, 1.0));
    float3 L = normalize(float3(-0.45, -0.6, 0.66));
    float diff = max(dot(n, L), 0.0);
    float3 albedo = mix(float3(0.05, 0.047, 0.045), float3(0.12, 0.105, 0.095), h0);
    float3 col = albedo * (0.3 + 0.9 * diff);

    // 濡れたつや(まだらに光る鏡面反射)
    float3 H = normalize(L + float3(0.0, 0.0, 1.0));
    float wet = smoothstep(0.42, 0.75, fbm(q / (30.0 * dp)));
    col += float3(0.45, 0.5, 0.46) * pow(max(dot(n, H), 0.0), 40.0) * wet * 0.5;

    // 濡れた小さな白い点
    float2 gc = floor(q / (9.0 * dp));
    float grnd = hash21(gc + 1.3);
    float2 gp = (gc + 0.2 + 0.6 * hash22(gc + 1.3)) * 9.0 * dp;
    float rr = (0.22 + 0.3 * fract(grnd * 7.7)) * dp;
    float spot = smoothstep(rr, rr * 0.2, length(q - gp)) * step(0.85, grnd);
    col = mix(col, float3(0.85, 0.9, 0.86), spot * 0.8);

    // 岩の溝(ひび): ボロノイの境目を黒くする
    float2 cq = q / (28.0 * dp);
    float2 ci = floor(cq);
    float f1 = 9.0;
    float f2 = 9.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            float2 cc = ci + float2(float(i), float(j));
            float2 pt = cc + hash22(cc + 3.7);
            float dd = length(cq - pt);
            if (dd < f1) {
                f2 = f1;
                f1 = dd;
            } else if (dd < f2) {
                f2 = dd;
            }
        }
    }
    float crack = 1.0 - smoothstep(0.0, 0.035, f2 - f1);
    col *= 1.0 - 0.7 * crack;

    // 地衣類の白っぽい斑
    float lich = smoothstep(0.66, 0.74, fbm(q / (16.0 * dp) + 5.0));
    col = mix(col, float3(0.40, 0.43, 0.37), lich * 0.5);

    // 杉の枯れ葉(赤茶の細い枝)
    float2 ta = float2(0.60, 0.66) * size;
    float2 tb = float2(0.67, 0.74) * size;
    float2 pa = p - ta;
    float2 ba = tb - ta;
    float th = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    float tw = length(pa - ba * th);
    float twig = 1.0 - smoothstep(0.5 * dp, 0.9 * dp, tw);
    col = mix(col, float3(0.42, 0.20, 0.11), twig * 0.85);

    // ---- 苔の帯(下と端): 株が密集し、すき間は黒緑 ----
    float cs = 3.6 * dp;
    float rt = 2.0 * dp;
    float2 q0 = p / cs;
    float2 cell0 = floor(q0);
    float bestS = 0.0;
    float bestTip = 0.0;
    float bestRnd = 0.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            float2 cell = cell0 + float2(float(i), float(j));
            float rnd = hash21(cell + 0.5 + seed * 7.1);
            float2 ctr = cell + 0.15 + 0.7 * hash22(cell + 0.5 + seed * 7.1);
            float amt = bandAmt(ctr * cs, size, dp, seed);
            float keep = step(rnd, clamp(amt * 1.2 - 0.1, 0.0, 1.0));
            float2 v = q0 - ctr;
            float rr2 = length(v) * cs / rt;
            float s = tuftStar(rr2, atan(v.y, v.x), rnd) * keep;
            if (s > bestS) {
                bestS = s;
                bestTip = smoothstep(0.25, 0.95, rr2);
                bestRnd = rnd;
            }
        }
    }
    float mAmt = bandAmt(p, size, dp, seed);
    float3 mossDark = float3(0.03, 0.06, 0.02);
    col = mix(col, mossDark, mAmt * 0.6);
    col = mix(col, leafColor(bestTip, bestRnd) * (0.6 + 0.4 * diff), clamp(bestS, 0.0, 1.0));

    // ---- ぼけ: 上の方は低い周波数だけの苔庭(明るい緑と灰色の柔らかい斑) ----
    float bl = smoothstep(0.0, 0.45, max(focus - uv.y, 0.0));
    float bk = fbm(q / (40.0 * dp) + 2.0);
    float3 blurCol = mix(float3(0.26, 0.28, 0.25), float3(0.34, 0.58, 0.15), smoothstep(0.35, 0.7, bk));
    blurCol = mix(blurCol, float3(0.52, 0.76, 0.24), smoothstep(0.6, 0.8, bk) * 0.6);
    col = mix(col, blurCol, bl);

    return half4(half3(col), 1.0);
}
"""

    /**
     * MOSS: 苔のクッション(主ボタン・選択中)。
     * 角丸長方形の形の中に株が密集する。縁は株がまばらにほつれ、pad の範囲まで株がはみ出す。
     * 中央は文字が読めるよう、明るく比較的なめらか。
     */
    const val MOSS = COMMON + """
uniform float2 size;
uniform float dp;
uniform float seed;
uniform float radius;
uniform float pad;
uniform float press;

half4 main(float2 p) {
    float2 c = size * 0.5;
    float rad = min(radius, min(size.x, size.y) * 0.5);
    float d = sdRoundRect(p, size, rad);
    // 縁のほつれ: 輪郭を雑音でゆがめる
    float rag = (vnoise(p / (3.0 * dp) + seed * 13.7) - 0.5) * 3.0 * dp;
    float d2 = d + rag;
    float body = 1.0 - smoothstep(-0.6 * dp, 0.6 * dp, d2);
    float depth = clamp(-d2 / (5.0 * dp), 0.0, 1.0);

    // ---- 株: 近い 3x3 マスの株から、いちばん濃いものを選ぶ ----
    float cs = 3.6 * dp;
    float rt = 2.0 * dp;
    float fr = max(pad, 1.0);
    float2 q0 = p / cs;
    float2 cell0 = floor(q0);
    float bestS = 0.0;
    float bestTip = 0.0;
    float bestRnd = 0.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            float2 cell = cell0 + float2(float(i), float(j));
            float rnd = hash21(cell + 0.5 + seed * 19.3);
            float2 ctr = cell + 0.15 + 0.7 * hash22(cell + 0.5 + seed * 19.3);
            // 外へ行くほど株は減る。縁の外は pad の範囲だけ
            float dc = sdRoundRect(ctr * cs, size, rad);
            float keep = step(rnd, clamp(1.0 - dc / fr, 0.0, 1.0));
            float2 v = q0 - ctr;
            float rr = length(v) * cs / rt;
            float s = tuftStar(rr, atan(v.y, v.x), rnd) * keep;
            if (s > bestS) {
                bestS = s;
                bestTip = smoothstep(0.25, 0.95, rr);
                bestRnd = rnd;
            }
        }
    }

    // ---- 色: 根元の黒緑のすき間に、株が明るく立つ ----
    float cover = clamp(bestS, 0.0, 1.0);
    float3 gap = mix(float3(0.02, 0.045, 0.012), float3(0.09, 0.2, 0.04), vnoise(p / (1.2 * dp) + seed * 5.0));
    float3 texCol = mix(gap, leafColor(bestTip, bestRnd), cover);
    texCol *= mix(0.5, 1.0, depth) * mix(1.08, 0.85, clamp(p.y / size.y, 0.0, 1.0));

    // 中央は明るい黄緑でなめらか(模様は弱い)。文字が読めるように
    float ex = abs(p.x - c.x) / (size.x * 0.5);
    float ey = abs(p.y - c.y) / (size.y * 0.5);
    float edgeness = smoothstep(0.35, 0.95, max(ex * 0.8, ey));
    float3 calm = mix(float3(0.62, 0.86, 0.28), float3(0.70, 0.91, 0.34), vnoise(p / (18.0 * dp) + seed * 3.0));
    calm *= 0.92 + 0.08 * cover;
    float3 col = mix(calm, texCol, edgeness);

    // 押した状態: 少し暗く、平たくつぶれる
    col *= 1.0 - 0.22 * press;

    float a = clamp(max(body, cover), 0.0, 1.0);
    return half4(half3(col * a), half(a));
}
"""

    /**
     * STONE: 磨いた濡れ石の板。暗い細かな粒の石肌、上の縁に柔らかいつや。
     * sunken=1 なら彫り込んだ溝になり、moss で縁(主に左上と下)に小さな苔が生える。
     * 中央は文字が読めるよう暗く、模様は控えめ。
     */
    const val STONE = COMMON + """
uniform float2 size;
uniform float dp;
uniform float seed;
uniform float radius;
uniform float pad;
uniform float press;
uniform float sunken;
uniform float moss;

half4 main(float2 p) {
    float2 uv = p / size;
    float rad = min(radius, min(size.x, size.y) * 0.5);
    float d = sdRoundRect(p, size, rad);
    float body = 1.0 - smoothstep(-0.5, 0.5, d);
    float din = -d;

    // 中央ほど模様を控えめに
    float ex = abs(p.x - size.x * 0.5) / (size.x * 0.5);
    float ey = abs(p.y - size.y * 0.5) / (size.y * 0.5);
    float edgeness = smoothstep(0.4, 1.0, max(ex * 0.8, ey));
    float shade = mix(0.45, 1.0, edgeness);

    // ---- 石肌: 粒の凹凸と左上からの光 ----
    float2 q = p + seed * float2(41.3, 11.9);
    float h0 = surfH(q, dp);
    float hx = surfH(q + float2(1.0, 0.0), dp);
    float hy = surfH(q + float2(0.0, 1.0), dp);
    float3 n = normalize(float3(-(hx - h0) * 6.0 * dp, -(hy - h0) * 6.0 * dp, 1.0));
    float3 L = normalize(float3(-0.45, -0.6, 0.66));
    float diff = max(dot(n, L), 0.0);
    float3 albedo = mix(float3(0.07, 0.072, 0.07), float3(0.15, 0.15, 0.145), h0);
    float3 col = albedo * (0.3 + 0.9 * mix(0.5, diff, shade));

    // 濡れたつや: 上の方ほど強い
    float3 H = normalize(L + float3(0.0, 0.0, 1.0));
    float wetTop = 1.0 - smoothstep(0.0, 0.5, uv.y);
    col += float3(0.5, 0.55, 0.52) * pow(max(dot(n, H), 0.0), 30.0) * (0.25 + 0.6 * wetTop) * 0.5 * shade;

    // 上の縁の光の帯と、縁の下の暗がり
    float rim = exp(-din / (1.5 * dp)) * (0.5 + 0.5 * (1.0 - uv.y));
    col += float3(0.35, 0.38, 0.36) * rim * 0.3;
    float lowT = smoothstep(0.5, 1.0, uv.y);
    col *= mix(1.0, 0.6, lowT * (1.0 - smoothstep(0.0, 7.0 * dp, din)));
    col *= mix(0.7, 1.0, smoothstep(0.0, 4.0 * dp, din));

    // 彫り込み: 上と左の内側に影、縁に溝
    float sh = (1.0 - smoothstep(0.0, 5.0 * dp, p.y)) + (1.0 - smoothstep(0.0, 5.0 * dp, p.x)) * 0.6;
    col *= 1.0 - 0.55 * sunken * clamp(sh, 0.0, 1.0);
    float groove = 1.0 - smoothstep(0.0, 1.2 * dp, abs(din - 1.5 * dp));
    col *= 1.0 - 0.35 * sunken * groove;

    // 押した状態: 少し暗く
    col *= 1.0 - 0.14 * press;

    // ---- 苔: 縁の帯に、左と下の角から生える小さな株 ----
    float cs = 3.6 * dp;
    float rt = 2.0 * dp;
    float2 q0 = p / cs;
    float2 cell0 = floor(q0);
    float bestS = 0.0;
    float bestTip = 0.0;
    float bestRnd = 0.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            float2 cell = cell0 + float2(float(i), float(j));
            float rnd = hash21(cell + 0.5 + seed * 23.9);
            float2 ctr = cell + 0.15 + 0.7 * hash22(cell + 0.5 + seed * 23.9);
            float2 cp = ctr * cs;
            float dc = sdRoundRect(cp, size, rad);
            float2 uc = cp / size;
            float cw = max(smoothstep(0.35, 0.0, uc.x) * (0.4 + 0.6 * smoothstep(0.3, 0.0, uc.y)), smoothstep(0.75, 1.0, uc.y));
            float keep = step(-1.5 * dp, dc) * step(dc, 3.0 * dp) * step(rnd, moss * cw);
            float2 v = q0 - ctr;
            float rr = length(v) * cs / rt;
            float s = tuftStar(rr, atan(v.y, v.x), rnd) * keep;
            if (s > bestS) {
                bestS = s;
                bestTip = smoothstep(0.25, 0.95, rr);
                bestRnd = rnd;
            }
        }
    }
    float cover = clamp(bestS, 0.0, 1.0);
    col = mix(col, leafColor(bestTip, bestRnd) * (0.6 + 0.4 * diff), cover);

    float a = clamp(max(body, cover), 0.0, 1.0);
    return half4(half3(col * a), half(a));
}
"""
}
