package com.example.businesscard.ui.koke

/**
 * Koke(苔むす)の苔の AGSL シェーダー(Android 13 以上の RuntimeShader で描く)。
 *
 *  - MOSS: 平らな部品の一部に生える、立体的な苔の群れ。細い葉が星形に開いた株を3層、奥(群れの中央)から
 *    手前(縁)へ重ねる。左上からの光で株ごとに陰影を付け、株の間の暗がり、群れの縁の透ける黄緑のふち、
 *    葉先の水滴のきらめき、平らな面に落ちる柔らかい影と接地の暗がりを描く。
 *    押すと指の近くの株が沈んで外へ倒れ、growth(0..1)で根元から生える。
 *
 * uniform: size(部品の大きさ px), dp(1dp の px 数), seed(0..1), radius(角丸の半径 px), growth, press,
 * touch(指の位置 px), patchA..patchD(群れ = 縁の上の基準点 x, y, 縁に沿った広がり, 内側への深さ。使わない群れは 0),
 * patchH(各群れの高さ px)。返す色は premultiplied。
 * 宣言した uniform はすべて使う(使われずに消えると設定時に落ちる)。
 *
 * 元のファイル: docs/koke/tools/src/koke_moss.agsl(ここは書き出したもの。直接編集しない)。
 * 手元での確認: docs/koke/tools/koke.py(skia-python で同じシェーダーを CPU で描く)。
 */
object KokeMossShaders {
    const val MOSS = """// ---- KOKE: 平らな部品の一部に生える、立体的な苔の群れ ----
// 部品の中身を描いた後に重ねて描く。座標 p は部品の左上を 0 とした px。部品の外(はみ出し・影)にも描く。
// 返す色は premultiplied(影は黒の半透明、苔は不透明に近い)。
//
// 群れ(patch)は最大4つ。patchX = (縁の上の基準点 x, y, 縁に沿った広がり, 内側への深さ)、
// patchH = 各群れの盛り上がりの高さ(px)。広がりが 0 の群れは無いものとして扱う。
uniform float2 size;     // 部品の大きさ(px)
uniform float dp;        // 1dp の px 数
uniform float seed;      // 生え方の種(0..1)
uniform float radius;    // 部品の角丸の半径(px)
uniform float growth;    // 生える途中 0..1(1 = 生えきった)
uniform float press;     // 押されている強さ 0..1
uniform float2 touch;    // 指の位置(px)
uniform float4 patchA;
uniform float4 patchB;
uniform float4 patchC;
uniform float4 patchD;
uniform float4 patchH;

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

// 角丸長方形の距離(中は負、外は正)
float sdRoundRect(float2 p, float2 b, float r) {
    float rr = min(r, min(b.x, b.y) * 0.5);
    float2 q = abs(p - b * 0.5) - (b * 0.5 - float2(rr));
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - rr;
}

// 苔の色の並び。0 = 根元の黒緑 .. 1 = 先端の淡い黄緑
float3 mossRamp(float x) {
    float3 c = mix(float3(0.055, 0.080, 0.028), float3(0.180, 0.250, 0.060), smoothstep(0.0, 0.22, x));
    c = mix(c, float3(0.400, 0.540, 0.120), smoothstep(0.22, 0.48, x));
    c = mix(c, float3(0.640, 0.800, 0.230), smoothstep(0.48, 0.74, x));
    c = mix(c, float3(0.840, 0.930, 0.450), smoothstep(0.74, 1.0, x));
    return c;
}

// 1つの群れの「中心からの遠さ」(0 = 群れの芯 .. 1 = 群れの縁、1 より外は群れの外)。
// 縁に沿っては spread、内側へは depth、外へは はみ出し(10dp)まで。e = 部品の縁までの距離
float patchQ(float2 p, float4 P, float e, float wob) {
    if (P.z <= 0.0) {
        return 9.0;
    }
    float r = length(p - P.xy) / P.z;
    float inw = max(-e, 0.0) / (max(P.w, 1.0) * (1.0 + 0.9 * clamp(1.0 - r, 0.0, 1.0)));
    // はみ出しは群れの中ほどで大きく、端ほど小さい(垂れた房)
    float ov = 10.0 * dp * (0.35 + 0.65 * clamp(1.0 - r * r, 0.0, 1.0));
    float outw = max(e, 0.0) / ov;
    float r2 = r * r;
    float i2 = inw * inw;
    float o2 = outw * outw;
    float q = sqrt(sqrt(r2 * r2 + i2 * i2 + o2 * o2));
    return q * (1.0 + wob);
}

// 群れ全体の場。x = q(いちばん近い群れの遠さ)、y = 盛り上がりの高さ(px)。押された所はへこむ
float2 field(float2 p, float wob) {
    float e = sdRoundRect(p, size, radius);
    float qa = patchQ(p, patchA, e, wob);
    float qb = patchQ(p, patchB, e, wob);
    float qc = patchQ(p, patchC, e, wob);
    float qd = patchQ(p, patchD, e, wob);
    float q = qa;
    float h = patchH.x;
    if (qb < q) { q = qb; h = patchH.y; }
    if (qc < q) { q = qc; h = patchH.z; }
    if (qd < q) { q = qd; h = patchH.w; }
    // 生える途中は群れが基準点の近くに小さくまとまる
    float g = 0.55 + 0.45 * (1.0 - (1.0 - growth) * (1.0 - growth));
    q = q / g;
    float dome = sqrt(max(1.0 - q * q, 0.0));
    float2 dt = p - touch;
    float dent = press * exp(-dot(dt, dt) / (196.0 * dp * dp));
    return float2(q, h * g * dome * (1.0 - 0.75 * dent));
}

// 縁のゆらぎ(群れの輪郭を不揃いにする)
float wobble(float2 p) {
    return 0.42 * (vnoise(p / (6.5 * dp) + seed * 17.0) - 0.5) + 0.18 * (vnoise(p / (2.6 * dp) + seed * 9.0) - 0.5);
}

// 1層ぶんの株(細い葉が星形に開く)。3x3 のセルを見て、手前(縁に近い)の株を優先して重ねる。
// cs = セルの大きさ(px)、front = 0(奥の層)..1(手前の層)。
// tA = (被覆, 株の中の位置 0 根元..1 先端, 株の乱数, 下の層へ落とす影)
// tB = (葉の向き x, y, 葉の中心線への近さ, 株の沈み)
// tC = (水滴の被覆, 水滴の中での位置 x, y, 株の明るさの倍率)
void tuftLayer(float2 p, float cs, float2 sd, float front, out float4 tA, out float4 tB, out float4 tC) {
    float2 q = p / cs;
    float2 cell0 = floor(q);
    float bestS = 0.0;
    tA = float4(0.0);
    tB = float4(0.0);
    tC = float4(0.0, 0.0, 0.0, 1.0);
    float shadow = 0.0;
    float drop = 0.0;
    float2 dropV = float2(0.0);
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            float2 cell = cell0 + float2(float(i), float(j));
            float2 key = cell + sd;
            float rnd = hash21(key);
            float2 ctr = cell + 0.5 + (hash22(key + 3.7) - 0.5) * 0.85;
            float2 cpx = ctr * cs;
            // 株の根元での群れの場(大きさ・残すか・生える時刻を決める)
            float2 fc = field(cpx, 0.6 * wobble(cpx));
            float qc = fc.x;
            // 奥の層は群れの中ほどに詰まり、手前の層は縁寄り(はみ出しまで)
            float lo = mix(0.97, 1.05, front);
            float keepP = (1.0 - smoothstep(lo - 0.14, lo, qc)) * mix(1.0, smoothstep(0.0, 0.5, qc) * 0.75 + 0.25, front);
            float keep = step(fract(rnd * 3.91 + 0.17), keepP);
            // 生える: 芯から縁へ、少しずつずれて伸びる
            float gt = clamp((growth - 0.42 * min(qc, 1.0) - 0.16 * fract(rnd * 7.7)) / 0.42, 0.0, 1.0);
            gt = gt * gt * (3.0 - 2.0 * gt);
            // 押す: 指の近くの株が沈み、外へ倒れる
            float2 dt = cpx - touch;
            float dl = length(dt);
            float kp = press * exp(-dl * dl / (324.0 * dp * dp));
            // 手前(縁)の株は大きく、奥は小さく詰まる。群れの外へ出た株は小さい
            float sz = mix(0.78, 1.18, clamp(qc, 0.0, 1.0)) * (1.0 - 0.45 * smoothstep(0.95, 1.15, qc));
            float R = (0.74 + 0.30 * fract(rnd * 5.3)) * sz * cs * gt * (1.0 - 0.3 * kp);
            float2 v = (q - ctr) * cs;
            float r0 = length(v);
            float ur0 = r0 / max(R, 0.001);
            // 倒れ: 葉先ほど指から離れる向きへずれる(元の位置へ戻して葉を探す)
            float2 lean = dt / max(dl, 0.001) * kp * 0.6 * R;
            float uc = min(ur0, 1.0);
            float2 w = v - lean * uc * uc;
            float r = length(w);
            float ur = r / max(R, 0.001);
            float n = 10.0 + floor(fract(rnd * 2.71) * 7.0);
            float P = TAU / n;
            // 葉は少し弧を描いて開く(株ごとに巻く向きが違う)
            float curl = (fract(rnd * 17.3) - 0.5) * 1.1 * ur;
            float ang = atan(w.y, w.x) - fract(rnd * 7.31) * P - curl;
            float f = ang / P;
            float li = floor(f + 0.5);
            float dA = abs(f - li) * P;
            float dperp = dA * r;
            // 葉ごとの長さのばらつき
            float lenF = 0.62 + 0.38 * hash21(float2(li + 7.0, rnd * 91.0));
            float hw = (0.34 + 0.14 * fract(rnd * 13.7)) * dp * (1.0 - 0.85 * ur / lenF) + 0.07 * dp;
            float leaf = (1.0 - smoothstep(hw * 0.45, hw, dperp)) * (1.0 - smoothstep(lenF - 0.12, lenF, ur));
            float core = 1.0 - smoothstep(0.12, 0.32, ur);
            float cov = max(leaf, core * 0.85) * keep * step(0.001, gt);
            float prio = qc + 0.25 * rnd + front;
            float s = cov * (0.35 + prio);
            if (s > bestS) {
                bestS = s;
                float2 dir = w / max(r, 0.001);
                tA = float4(cov, clamp(ur / lenF, 0.0, 1.0), rnd, 0.0);
                tB = float4(dir, 1.0 - clamp(dperp / max(hw, 0.001), 0.0, 1.0), kp);
                tC.w = mix(0.82, 1.12, fract(rnd * 41.9)) * mix(1.06, 0.86, front * (1.0 - qc));
            }
            // 株の落とす影(右下へずれた柔らかい円)。下の層の上に落ちる
            float2 sv = v - float2(0.35, 0.45) * R;
            float sh = (1.0 - smoothstep(0.35 * R, 1.15 * R, length(sv))) * keep * gt;
            shadow = max(shadow, sh);
            // 水滴: 一部の株の葉先に、小さな玉
            if (fract(rnd * 29.3) > 0.72 && keep > 0.5) {
                float da = fract(rnd * 7.31) * P + floor(fract(rnd * 3.3) * n) * P;
                float2 dpos = float2(cos(da), sin(da)) * R * 0.78 + lean;
                float rd = (0.50 + 0.38 * fract(rnd * 53.1)) * dp * gt;
                float2 dv = (v - dpos) / max(rd, 0.001);
                float dd = length(dv);
                float dc = 1.0 - smoothstep(0.80, 1.0, dd);
                if (dc > drop) {
                    drop = dc;
                    dropV = dv;
                }
            }
        }
    }
    tA.w = shadow;
    tC.x = drop;
    tC.yz = dropV;
}

half4 main(float2 p) {
    float wob = wobble(p);
    float2 F = field(p, wob);
    float q = F.x;

    // ---- 平らな面に落ちる影(群れの形をぼかして右下へずらす)と接地の暗がり ----
    float2 Fs = field(p - float2(2.6, 3.6) * dp, wob * 0.35);
    float shadowA = (1.0 - smoothstep(0.66, 1.18, Fs.x)) * 0.34;
    float contact = (1.0 - smoothstep(0.88, 1.12, q)) * 0.35;
    // 影は生えた分だけ(芯から縁へ、苔と一緒に濃くなる)
    float gS = clamp((growth - 0.42 * min(Fs.x, 1.0) - 0.1) / 0.42, 0.0, 1.0);
    float gQ = clamp((growth - 0.42 * min(q, 1.0) - 0.1) / 0.42, 0.0, 1.0);
    float under = clamp(max(shadowA * gS * gS, contact * gQ * gQ) * smoothstep(0.3, 0.9, growth), 0.0, 1.0);
    if (q > 1.32) {
        return half4(0.0, 0.0, 0.0, half(under));
    }

    // ---- 群れの起伏の向き(左上からの光の当たり方) ----
    float e = 0.9 * dp;
    float hx = field(p + float2(e, 0.0), wob).y - F.y;
    float hy = field(p + float2(0.0, e), wob).y - F.y;
    float3 nC = normalize(float3(-2.2 * hx / e, -2.2 * hy / e, 1.0));
    float3 L = normalize(float3(-0.55, -0.65, 0.62));
    float lamC = clamp(dot(nC, L), 0.0, 1.0);

    // 株のすき間(谷)の黒緑。群れの芯ほど深く暗い
    float3 col = mossRamp(0.05 + 0.10 * lamC) * (0.55 + 0.45 * lamC);
    float gBase = clamp((growth - 0.42 * min(q, 1.0) - 0.08) / 0.42, 0.0, 1.0);
    float alpha = (1.0 - smoothstep(0.62, 0.86, q)) * gBase;

    float3 V = float3(0.0, 0.0, 1.0);
    float3 Hh = normalize(L + V);
    float dropA = 0.0;
    float2 dropV = float2(0.0);
    float glint = 0.0;
    float3 dropUnder = float3(0.0);

    for (int k = 0; k < 3; k++) {
        float kk = float(k);
        float front = kk * 0.5;
        float cs = (4.4 + 1.1 * kk) * dp;
        float4 tA;
        float4 tB;
        float4 tC;
        tuftLayer(p, cs, float2(seed * 19.3 + kk * 37.1, seed * 11.7 + kk * 21.9), front, tA, tB, tC);
        // 手前の株が奥へ落とす影(株の間の暗がり)
        col *= 1.0 - 0.62 * tA.w * (1.0 - tA.x);
        float ur = tA.y;
        // 株を小さな丸屋根として、群れの起伏と合わせた向き
        float3 nT = normalize(float3(tB.xy * (0.25 + 0.95 * ur), 1.0));
        float3 n = normalize(float3(nC.xy + nT.xy, 1.0));
        float dif = clamp(dot(n, L), 0.0, 1.0);
        // 曇天の柔らかい光 + 少しの直射
        float light = 0.42 + 0.30 * lamC + 0.55 * dif;
        float idx = 0.30 + 0.60 * pow(ur, 1.1) + 0.22 * (lamC - 0.5) + 0.10 * (tA.z - 0.5) + 0.05 * kk;
        float3 lc = mossRamp(clamp(idx, 0.0, 1.0)) * light * tC.w;
        float hueV = fract(tA.z * 23.1);
        lc = mix(lc, lc * float3(1.08, 0.92, 0.62), 0.45 * step(0.82, hueV) * (1.0 - ur));
        lc = mix(lc, lc * float3(1.06, 1.04, 0.70), 0.5 * step(hueV, 0.22) * ur);
        // 葉の中心線のつや
        float spec = pow(clamp(dot(n, Hh), 0.0, 1.0), 18.0) * tB.z * tB.z;
        lc += float3(0.55, 0.62, 0.40) * spec * 0.22;
        // 群れの縁の葉先は光が透けて黄緑に光る(逆光ぎみ)
        float rim = smoothstep(0.55, 1.0, q) * pow(ur, 2.0);
        lc += float3(0.50, 0.66, 0.10) * rim * 0.55 * (0.5 + 0.5 * dif);
        // 押されて沈んだ株は暗い
        lc *= 1.0 - 0.5 * tB.w;
        col = mix(col, lc, tA.x);
        alpha = max(alpha, tA.x);
        // 水滴は手前の層ほど優先
        if (tC.x > dropA * 0.7) {
            dropA = tC.x;
            dropV = tC.yz;
            dropUnder = lc;
        }
        glint = max(glint, tB.w);
    }
    col *= 1.0 - 0.18 * (1.0 - smoothstep(0.0, 0.5, q)) * (1.0 - lamC);

    // ---- 水滴(小さな玉): 下の葉が透け、左上に鋭い光、縁は暗い ----
    if (dropA > 0.0) {
        float dz = sqrt(max(1.0 - dot(dropV, dropV), 0.0));
        float3 dn = normalize(float3(dropV, dz));
        float dspec = pow(clamp(dot(dn, Hh), 0.0, 1.0), 90.0);
        float fres = pow(1.0 - dz, 2.0);
        float3 dcol = dropUnder * (0.75 + 0.5 * (1.0 - fres)) * mix(1.0, 0.55, fres);
        // 下側に集まる屈折の明るみ
        dcol += mossRamp(0.85) * 0.35 * smoothstep(0.1, 0.9, dropV.y) * (1.0 - fres);
        dcol += float3(1.0, 1.0, 0.97) * dspec * (1.6 + 1.4 * glint);
        dcol += float3(0.85, 0.9, 0.95) * fres * 0.25;
        col = mix(col, dcol, dropA);
        alpha = max(alpha, dropA);
    }

    col = clamp(col, 0.0, 1.0);
    float a = clamp(alpha, 0.0, 1.0);
    // 苔の下に影を重ねる(苔が覆っていない所だけ影が見える)
    float outA = a + under * (1.0 - a);
    return half4(half3(col * a), half(outA));
}
"""
}
