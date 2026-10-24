package com.example.businesscard.ui.moss

import android.graphics.BlurMaskFilter
import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.businesscard.ui.glass.GlassFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.graphics.Paint as NativePaint

/** 部品の面の種類。苔のクッション(押せる主役)か、磨いた石の板(土台)か。 */
enum class MossTone { Moss, Stone }

/**
 * 押されたボタンの沈み具合。0 = 浮いている、1 = 石や苔に沈みきった。
 * 離した直後は少しだけ負(浮きすぎ)になってから 0 に戻る。
 */
@Stable
class MossPress internal constructor() {
    internal val depth = Animatable(0f)
    val value: Float get() = depth.value
}

/**
 * [interactionSource] の押す・離すに合わせて [MossPress] を動かし、触覚を返す。
 * 素早く叩いたときも「底に当たった」所までは見せてから戻す。
 */
@Composable
fun rememberMossPress(interactionSource: InteractionSource, feedback: GlassFeedback?): MossPress {
    val press = remember { MossPress() }
    LaunchedEffect(interactionSource, feedback) {
        var pressIn: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    feedback?.pressDown()
                    pressIn = launch { press.depth.animateTo(1f, MossMotion.pressSpring) }
                }
                is PressInteraction.Release -> {
                    feedback?.pressUp()
                    val sinking = pressIn
                    launch {
                        // 底に当たるまでは見せてから戻す
                        sinking?.join()
                        press.depth.animateTo(0f, MossMotion.releaseSpring)
                    }
                }
                is PressInteraction.Cancel -> launch { press.depth.animateTo(0f, MossMotion.releaseSpring) }
            }
        }
    }
    return press
}

/**
 * 苔または石の板を描く Modifier。面の模様と縁の形は AGSL のシェーダー(MossShaders)が描く。
 *
 * 描く順(下から): 縁の苔の淡い光(glow) → 接地の暗がり(石)または深緑の影(苔) → 面(シェーダー)。
 * 面は角丸の形から少しはみ出して描く(苔の縁がほつれるため)。
 *
 * @param shape 形。角丸の半径は形から求める(RoundedCornerShape なら、その角の丸み)
 * @param pressed 押し込まれた深さ(0..1)。描くときに読むので、動いても組み立て直しは起きない
 * @param sunken true なら彫り込まれた溝として描く(石のみ。入力欄)
 * @param glow 縁が淡く光る強さ 0..1(フォーカス中の入力欄など)
 * @param seed 模様をずらす種(部品ごとに違う値にすると、同じ模様が並ばない)
 * @param moss 石の縁に生える苔の量 0..1(石のみ。名刺で使う)
 */
@Composable
fun Modifier.mossSurface(
    shape: Shape,
    tone: MossTone = MossTone.Stone,
    pressed: MossPress? = null,
    sunken: Boolean = false,
    glow: Float = 0f,
    seed: Float = 0f,
    moss: Float = 0f,
): Modifier {
    val c = MossTheme.colors
    val glowNow by animateFloatAsState(glow, tween(MossMotion.CHANGE_MILLIS), label = "glow")
    val shader = remember(tone) {
        RuntimeShader(if (tone == MossTone.Moss) MossShaders.MOSS else MossShaders.STONE)
    }

    val squash = if (pressed != null) {
        Modifier.graphicsLayer {
            val s = 1f - (1f - MossMotion.PRESS_SCALE) * pressed.value.coerceIn(0f, 1f)
            scaleX = s
            scaleY = s
        }
    } else {
        Modifier
    }

    return this.then(squash).drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val native = Path().apply { addOutline(outline) }.asAndroidPath()
        // 角丸の半径(px)。シェーダーが角の丸みに使う
        val radiusPx = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
        val padPx = 6.dp.toPx()
        // シェーダーは縁の外側へ pad の分まで描くので、その大きさの矩形を塗る
        val fringe = Size(size.width + padPx * 2f, size.height + padPx * 2f)
        val brush = ShaderBrush(shader)

        val isMoss = tone == MossTone.Moss
        val shadowDy = if (isMoss) 3.dp.toPx() else 2.dp.toPx()
        val shadowAlpha = if (isMoss) 0.6f else 0.5f
        val shadowColor = if (isMoss) c.mossShadow else Color.Black
        val shadowPaint = blurPaint(6.dp.toPx())
        val glowWide = blurPaint(18.dp.toPx())
        val glowNear = blurPaint(7.dp.toPx())

        onDrawBehind {
            val sink = pressed?.value?.coerceIn(0f, 1f) ?: 0f

            // 縁の淡い光(フォーカス中)
            if (glowNow > 0.001f) {
                drawIntoCanvas { canvas ->
                    val nc = canvas.nativeCanvas
                    nc.drawPath(native, glowWide.tint(c.focusGlow, 0.75f * glowNow))
                    nc.drawPath(native, glowNear.tint(c.focusGlow, 0.9f * glowNow))
                }
            }

            // 接地の暗がり(押すほど消える。彫り込んだ溝には影を付けない)
            val lift = 1f - sink
            if (!sunken && lift > 0.001f) {
                drawIntoCanvas { canvas ->
                    val nc = canvas.nativeCanvas
                    nc.save()
                    nc.translate(0f, shadowDy * lift)
                    nc.drawPath(native, shadowPaint.tint(shadowColor, shadowAlpha * lift))
                    nc.restore()
                }
            }

            shader.setMossUniforms(
                tone = tone,
                size = size,
                dpPx = density,
                seed = seed,
                radiusPx = radiusPx,
                padPx = padPx,
                press = sink,
                sunken = sunken,
                moss = moss,
            )
            drawRect(brush = brush, topLeft = Offset(-padPx, -padPx), size = fringe)
        }
    }
}

/** 押し込まれた深さで、中身(文字・アイコン)をわずかに下げる(板と一緒に沈む)。 */
internal fun Modifier.mossSink(press: MossPress?): Modifier =
    if (press == null) this else this.graphicsLayer { translationY = 1.dp.toPx() * press.value.coerceIn(0f, 1f) }

/** シェーダーに値を渡す。種類ごとに宣言されている uniform だけを設定する(無い名前を設定すると落ちる)。 */
private fun RuntimeShader.setMossUniforms(
    tone: MossTone,
    size: Size,
    dpPx: Float,
    seed: Float,
    radiusPx: Float,
    padPx: Float,
    press: Float,
    sunken: Boolean,
    moss: Float,
) {
    setFloatUniform("size", size.width, size.height)
    setFloatUniform("dp", dpPx)
    setFloatUniform("seed", seed)
    setFloatUniform("radius", radiusPx)
    setFloatUniform("pad", padPx)
    setFloatUniform("press", press)
    if (tone == MossTone.Stone) {
        setFloatUniform("sunken", if (sunken) 1f else 0f)
        setFloatUniform("moss", moss.coerceIn(0f, 1f))
    }
}

/** ぼかしの付いた絵筆。ぼかしの半径はピクセルで受け取る。 */
private fun blurPaint(radiusPx: Float): NativePaint =
    NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        if (radiusPx > 0.5f) maskFilter = BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL)
    }

private fun NativePaint.tint(color: Color, alpha: Float): NativePaint {
    this.color = color.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()
    return this
}
