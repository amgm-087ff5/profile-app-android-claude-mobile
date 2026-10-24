package com.example.businesscard.ui.koke

import android.graphics.RuntimeShader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * 部品の面を黒い濡れた岩にする(面と文字の後ろに描く)。描き方は KokeRockShaders.kt の ROCK。
 *
 *  - 輪郭: 角の欠けた 12 角形。欠けの大きさと頂点の揺れは [seed] で部品ごとに変わる
 *  - 側面と接地の影: 部品の外へ最大 [KokeDepth.drawMargin] はみ出して描く
 *  - [view] を渡すと、画面の中の位置に合わせて、中心側の側面が少し見える(上にある部品は下の側面が長い)
 *
 * 面そのもののずれ(パララクス)は [kokeParallax] で、文字と苔ごと動かす。
 * 値(画面の中の位置・押した強さ・縁の線)は描くときにだけ読むので、変わっても組み立て直しは起きない。
 *
 * @param rim 縁の線の色(透明度ごと)。透明なら線なし
 * @param rimWidth 縁の線の太さ
 */
@Composable
internal fun Modifier.kokeRock(
    style: KokeRockStyle,
    seed: Int,
    view: KokeViewOffset? = null,
    press: KokePress? = null,
    rim: () -> Color = { Color.Transparent },
    rimWidth: () -> Dp = { KokeSize.line },
): Modifier {
    val c = KokeTheme.colors
    val shader = remember { RuntimeShader(KokeRockShaders.ROCK) }
    val seedF = remember(seed) { fract(seed * 0.618034 + 0.291).toFloat() }
    return drawWithCache {
        val poly = rockPolygon(size, seedF, style.chip.toPx(), this)
        val margin = KokeDepth.drawMargin.toPx()
        val brush = ShaderBrush(shader)
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("dp", density)
        shader.setFloatUniform("seed", seedF)
        shader.setFloatUniform("rockLo", c.rockLo.red, c.rockLo.green, c.rockLo.blue)
        shader.setFloatUniform("rockHi", c.rockHi.red, c.rockHi.green, c.rockHi.blue)
        shader.setFloatUniform("relief", style.relief)
        shader.setFloatUniform("facet", style.facet.toPx())
        val names = arrayOf("polyA", "polyB", "polyC", "polyD", "polyE", "polyF")
        for (i in names.indices) {
            shader.setFloatUniform(names[i], poly[i * 4], poly[i * 4 + 1], poly[i * 4 + 2], poly[i * 4 + 3])
        }
        onDrawBehind {
            val b = rockBase(view?.value ?: Offset.Zero, this)
            shader.setFloatUniform("base", b.x, b.y)
            shader.setFloatUniform("press", (press?.value ?: 0f).coerceIn(0f, 1f))
            val r = rim()
            shader.setFloatUniform("rim", r.red, r.green, r.blue, r.alpha)
            shader.setFloatUniform("rimWidth", rimWidth().toPx())
            drawRect(
                brush = brush,
                topLeft = Offset(-margin, -margin),
                size = Size(size.width + 2 * margin, size.height + 2 * margin),
            )
        }
    }
}

/**
 * 面(文字・苔ごと)を、画面の中心から遠ざかる向きにわずかにずらす(最大 [KokeDepth.faceShift])。
 * 底(側面の端)は [kokeRock] がその分を見込んで描くので、面だけが浮いて見える。
 */
internal fun Modifier.kokeParallax(view: KokeViewOffset?): Modifier =
    if (view == null) {
        this
    } else {
        graphicsLayer {
            val v = view.value
            val k = KokeDepth.faceShift.toPx()
            translationX = v.x.coerceIn(-1f, 1f) * k
            translationY = v.y.coerceIn(-1f, 1f) * k
        }
    }

/** 岩の底の、面からのずれ(px)。中央では下に [KokeDepth.side]、上にあるほど下の側面が長く、左右は中心側が見える。 */
private fun rockBase(v: Offset, d: Density): Offset = with(d) {
    val vx = v.x.coerceIn(-1f, 1f)
    val vy = v.y.coerceIn(-1f, 1f)
    Offset(
        x = -vx * KokeDepth.sideTiltX.toPx(),
        y = KokeDepth.side.toPx() - vy * KokeDepth.sideTilt.toPx(),
    )
}

/**
 * 角の欠けた凸の 12 角形(時計回り、px を x, y の順に 24 個)。
 * 角ごとに欠けを 0.75〜1.15 倍に散らし、辺の途中の頂点を 0.3〜0.7 の所に、角の近くの頂点を少し内へずらす。
 * docs/koke/tools/koke_rock.py の rock_polygon と同じ。
 */
private fun rockPolygon(size: Size, seed: Float, chip: Float, d: Density): FloatArray {
    val w = size.width
    val h = size.height
    val dp = d.density
    val cap = 0.42f * min(w, h)
    val c = FloatArray(4) { i -> min(chip * (0.75f + 0.4f * h01(seed, i)), cap) }
    fun j(i: Int) = (0.25f + 1.0f * h01(seed, 10 + i)) * dp
    fun m(i: Int) = 0.3f + 0.4f * h01(seed, 30 + i)
    return floatArrayOf(
        c[0], j(0), w * m(0), 0f, w - c[1], j(1),
        w - j(2), c[1], w, h * m(1), w - j(3), h - c[2],
        w - c[2], h - j(4), w * m(2), h, c[3], h - j(5),
        j(6), h - c[3], 0f, h * m(3), j(7), c[0],
    )
}

private fun h01(seed: Float, i: Int): Float = fract(sin(seed * 12.9898 + i * 78.233) * 43758.5453).toFloat()

private fun fract(x: Double): Double = x - floor(x)
