package com.example.businesscard.ui.koke

import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.businesscard.ui.glass.prefersReducedMotion
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** 苔の群れを置く場所。角(TopStart など)か、辺(Edge*、[MossPatch.along] で辺のどこか 0..1)。 */
enum class MossAnchor { TopStart, TopEnd, BottomStart, BottomEnd, EdgeTop, EdgeBottom, EdgeStart, EdgeEnd }

/**
 * 苔の群れ 1 つ。
 * @param spread 縁に沿って広がる長さ(群れの中心からの半径に近い)
 * @param depth 縁から内側へ入り込む深さ(群れの中心ではこの 2 倍近くまで入る)
 * @param height 盛り上がりの高さ(陰影の強さに効く)
 * @param along 辺の群れのとき、辺のどこに置くか(0 = 始まり .. 1 = 終わり)。角の群れでは使わない
 */
@Immutable
data class MossPatch(
    val anchor: MossAnchor,
    val spread: Dp = 40.dp,
    val depth: Dp = 14.dp,
    val height: Dp = 6.dp,
    val along: Float = 0.5f,
)

/** 苔が部品の範囲の外へはみ出してよい最大の幅(縁から垂れる分)。 */
val MossOverhang: Dp = 10.dp

/** 苔の影のために、はみ出しのさらに外へ描く幅。 */
private val ShadowMargin: Dp = 6.dp

/** シェーダーに渡せる群れの数。 */
private const val MAX_PATCHES = 4

/** 生える時間(ms)。 */
private const val GROW_MILLIS = 900

/** 押したときは素早く沈み、離すと少し弾んで戻る。 */
private val PressInSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f)
private val ReleaseSpring = spring<Float>(dampingRatio = 0.42f, stiffness = 260f)

/**
 * 部品に苔を生やす。部品の中身(面と文字)を描いた**後**に、苔と、苔が面に落とす影を重ねて描く。
 * 苔は部品の範囲の外へ最大 [MossOverhang] はみ出してよい(縁から垂れる)。
 * interactionSource を渡すと、押したとき指の近くの株が沈み、外へ倒れる(離すとばねで戻る)。
 * shape は部品の形(角丸の半径を求め、苔が角を回り込むのに使う)。
 *
 * 描くのは群れの範囲(+ はみ出し + 影)の矩形だけ。生える途中と押した前後だけ描き直し、
 * 止まっているときは描き直さない(アニメーションの値は描くときにだけ読む)。
 * 触覚は付けない(部品の側で行う)。「アニメーションを削除」設定では最初から生えている。
 *
 * [view] を渡すと、画面の中の位置と逆向きに苔を少しずらす(手前の高い株ほど大きく、最大 2.6dp)。
 * 苔が岩の面より手前にあるように見える。影は面の上なのでずらさない。値は描くときに読む。
 *
 * @param patches 群れ(最大 4 つ。それより多い分は描かない)
 * @param seed 生え方の種。部品ごとに違う値にすると、同じ形が並ばない
 */
@Composable
fun Modifier.kokeMoss(
    patches: List<MossPatch>,
    shape: Shape,
    seed: Int,
    interactionSource: InteractionSource? = null,
    view: KokeViewOffset? = null,
): Modifier {
    if (patches.isEmpty()) return this
    val context = LocalContext.current
    val reduceMotion = remember(context) { context.prefersReducedMotion() }
    val shader = remember { RuntimeShader(KokeMossShaders.MOSS) }

    // 生える: 0 → 1
    val growth = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (growth.value < 1f) growth.animateTo(1f, tween(GROW_MILLIS, easing = FastOutSlowInEasing))
    }

    // 押す: 強さ 0..1 と指の位置(部品の座標、px)
    val press = remember { Animatable(0f) }
    var touch by remember { mutableStateOf(Offset.Zero) }
    if (interactionSource != null) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        touch = interaction.pressPosition
                        launch { press.animateTo(1f, PressInSpring) }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel ->
                        launch { press.animateTo(0f, ReleaseSpring) }
                }
            }
        }
    }

    // 種を 0..1 に散らす(近い番号でも離れた値になる)
    val seedF = remember(seed) { fract(seed * 0.618034f + 0.137f) }
    val used = patches.take(MAX_PATCHES)

    return this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val rtl = layoutDirection == LayoutDirection.Rtl
        val radiusPx = cornerRadius(outline, used.first().anchor, rtl)
        val vecs = used.map { patchVector(it, size, radiusPx, rtl, density) }
        val heights = FloatArray(MAX_PATCHES)
        used.forEachIndexed { i, p -> heights[i] = p.height.toPx() }
        val area = mossBounds(vecs, size, (MossOverhang + ShadowMargin).toPx(), 4.dp.toPx())
        val brush = ShaderBrush(shader)

        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("dp", density)
        shader.setFloatUniform("seed", seedF)
        shader.setFloatUniform("radius", radiusPx)
        val names = arrayOf("patchA", "patchB", "patchC", "patchD")
        for (i in 0 until MAX_PATCHES) {
            val v = vecs.getOrNull(i) ?: floatArrayOf(0f, 0f, 0f, 0f)
            shader.setFloatUniform(names[i], v[0], v[1], v[2], v[3])
        }
        shader.setFloatUniform("patchH", heights[0], heights[1], heights[2], heights[3])

        onDrawWithContent {
            drawContent()
            // ここで読む値は描くときだけ読む(変わっても組み立て直しは起きない)
            shader.setFloatUniform("growth", growth.value.coerceIn(0f, 1f))
            // 離した後のばねの弾みで少し負(株がふくらむ)になってよい
            shader.setFloatUniform("press", press.value.coerceIn(-0.2f, 1f))
            shader.setFloatUniform("touch", touch.x, touch.y)
            val v = view?.value ?: Offset.Zero
            shader.setFloatUniform("view", v.x, v.y)
            drawRect(brush = brush, topLeft = area.topLeft, size = area.size)
        }
    }
}

/** 群れの置かれた角の丸み(px)。辺の群れや角丸でない形では、左上の丸みを使う。 */
private fun cornerRadius(outline: Outline, anchor: MossAnchor, rtl: Boolean): Float {
    val rr = (outline as? Outline.Rounded)?.roundRect ?: return 0f
    val left = (anchor == MossAnchor.TopStart || anchor == MossAnchor.BottomStart) != rtl
    return when (anchor) {
        MossAnchor.TopStart, MossAnchor.TopEnd ->
            if (left) rr.topLeftCornerRadius.x else rr.topRightCornerRadius.x
        MossAnchor.BottomStart, MossAnchor.BottomEnd ->
            if (left) rr.bottomLeftCornerRadius.x else rr.bottomRightCornerRadius.x
        else -> rr.topLeftCornerRadius.x
    }
}

/**
 * 群れ 1 つをシェーダーに渡す値にする: (縁の上の基準点 x, y, 縁に沿った広がり, 内側への深さ)、すべて px。
 * 角の群れの基準点は、角丸の 45 度の点(角を回り込んで生える)。Start / End は文字の向きに従う。
 */
private fun patchVector(p: MossPatch, size: Size, radius: Float, rtl: Boolean, density: Float): FloatArray {
    val w = size.width
    val h = size.height
    val k = radius * (1f - sqrt(0.5f))
    val a = p.along.coerceIn(0f, 1f)
    val startX = if (rtl) w - k else k
    val endX = if (rtl) k else w - k
    val (x, y) = when (p.anchor) {
        MossAnchor.TopStart -> startX to k
        MossAnchor.TopEnd -> endX to k
        MossAnchor.BottomStart -> startX to h - k
        MossAnchor.BottomEnd -> endX to h - k
        MossAnchor.EdgeTop -> (if (rtl) w * (1f - a) else w * a) to 0f
        MossAnchor.EdgeBottom -> (if (rtl) w * (1f - a) else w * a) to h
        MossAnchor.EdgeStart -> (if (rtl) w else 0f) to h * a
        MossAnchor.EdgeEnd -> (if (rtl) 0f else w) to h * a
    }
    return floatArrayOf(x, y, p.spread.value * density, p.depth.value * density)
}

/**
 * 苔と影を描く矩形(部品の座標)。群れの広がりの 1.6 倍 + 少しの余白を、
 * 部品のまわり [MossOverhang] + 影の幅までに収める。
 */
private fun mossBounds(vecs: List<FloatArray>, size: Size, outside: Float, extra: Float): Rect {
    var l = Float.MAX_VALUE
    var t = Float.MAX_VALUE
    var r = -Float.MAX_VALUE
    var b = -Float.MAX_VALUE
    for (v in vecs) {
        val e = v[2] * 1.6f + extra
        l = min(l, v[0] - e)
        t = min(t, v[1] - e)
        r = max(r, v[0] + e)
        b = max(b, v[1] + e)
    }
    return Rect(
        left = max(l, -outside),
        top = max(t, -outside),
        right = min(r, size.width + outside),
        bottom = min(b, size.height + outside),
    )
}

private fun fract(x: Float): Float = x - kotlin.math.floor(x)
