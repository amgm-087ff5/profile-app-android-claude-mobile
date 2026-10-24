package com.example.businesscard.ui.kinu

import android.graphics.BlurMaskFilter
import android.graphics.RuntimeShader
import android.graphics.Paint as NativePaint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.businesscard.ui.glass.GlassFeedback
import com.example.businesscard.ui.glass.LocalGlowHeadroom
import com.example.businesscard.ui.glass.prefersReducedMotion
import com.example.businesscard.ui.glass.rememberGlassFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 指が触れていないときの位置(シェーダーの touch)。 */
internal val KinuTouchNone = Offset(-1f, -1f)

/**
 * 押している状態。[value] は 0(休み)→1(押しきった)。[touch] は指の位置(部品の左上からの px)、触れていなければ (-1,-1)。
 * どちらも描くときに読むので、動いても組み立て直しは起きない。
 */
@Stable
class KinuPress internal constructor() {
    internal val depth = Animatable(0f)
    internal var touchPoint by mutableStateOf(KinuTouchNone)

    val value: Float get() = depth.value
    val touch: Offset get() = touchPoint
}

/**
 * [interactionSource] の押す・離すに合わせて [KinuPress] を動かし、触覚を返す。
 * 押す・離すの時間は [KinuMotion.PRESS_MILLIS] / [KinuMotion.RELEASE_MILLIS]。大きさや位置は動かさない。
 */
@Composable
fun rememberKinuPress(
    interactionSource: InteractionSource,
    feedback: GlassFeedback? = rememberGlassFeedback(),
): KinuPress {
    val press = remember { KinuPress() }
    LaunchedEffect(interactionSource, feedback) {
        var pressing: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    feedback?.pressDown()
                    pressing = launch { press.depth.animateTo(1f, tween(KinuMotion.PRESS_MILLIS)) }
                }
                is PressInteraction.Release -> {
                    feedback?.pressUp()
                    val down = pressing
                    launch {
                        // 押しきってから戻す(素早く叩いても押した形が見えるように)
                        down?.join()
                        press.depth.animateTo(0f, tween(KinuMotion.RELEASE_MILLIS))
                    }
                }
                is PressInteraction.Cancel -> launch {
                    press.depth.animateTo(0f, tween(KinuMotion.RELEASE_MILLIS))
                }
            }
        }
    }
    return press
}

/**
 * 指の位置を [press] へ送る(触れている間だけ。離すと (-1,-1))。
 * イベントは消費しないので、クリックや標準の押下の処理はそのまま動く。
 */
fun Modifier.kinuTouch(press: KinuPress): Modifier = pointerInput(press) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        press.touchPoint = down.position
        var pressed = true
        while (pressed) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            pressed = change.pressed
            if (pressed) press.touchPoint = change.position
        }
        press.touchPoint = KinuTouchNone
    }
}

/** 画面の登場(光の線が左から灯る)。0..1。 */
@Stable
class KinuIgnite internal constructor(start: Float) {
    internal val progress = Animatable(start)

    val value: Float get() = progress.value
}

private val KinuIgniteDone = KinuIgnite(1f)

/** 登場の値。[KinuBackground] が提供し、中の部品はこれを読む。 */
val LocalKinuIgnite = staticCompositionLocalOf { KinuIgniteDone }

/**
 * 登場の値を作る。0 から [KinuMotion.IGNITE_MILLIS] で 1 へ。
 * 「アニメーションを削除」設定のときは最初から 1。
 */
@Composable
fun rememberKinuIgnite(): KinuIgnite {
    val context = LocalContext.current
    val ignite = remember { KinuIgnite(if (context.prefersReducedMotion()) 1f else 0f) }
    LaunchedEffect(ignite) {
        if (ignite.value < 1f) {
            ignite.progress.animateTo(1f, tween(KinuMotion.IGNITE_MILLIS, easing = LinearEasing))
        }
    }
    return ignite
}

/**
 * 金の帯(主ボタン・名刺の縁・選択中の印)。矩形いっぱいに描く。形で clip する。
 * @param line 光の線の強さ 0..1.6(描くときに読む)
 * @param touch 指の位置(px)。(-1,-1) で触れていない
 */
@Composable
fun Modifier.kinuRibbon(
    shape: Shape,
    line: () -> Float = { KinuLight.LINE_GOLD },
    touch: () -> Offset = { KinuTouchNone },
    seed: Float = 0f,
    ignite: KinuIgnite = LocalKinuIgnite.current,
): Modifier {
    val c = KinuTheme.colors
    val hdr = LocalGlowHeadroom.current
    val shader = remember { RuntimeShader(KinuShaders.RIBBON) }
    // 形の外へ落ちる接地の影を切らないため、clip せず、面だけを形で切り抜く
    return this.drawWithCache {
        val brush = ShaderBrush(shader)
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val native = path.asAndroidPath()
        val shadow = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(8.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            color = Color.Black.copy(alpha = 0.55f).toArgb()
        }
        val shadowDy = 3.dp.toPx()
        val rimWidth = 1.dp.toPx()
        val rimColor = c.ink.copy(alpha = 0.5f)
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("dp", 1.dp.toPx())
        shader.setFloatUniform("hdr", hdr)
        shader.setFloatUniform("seed", seed)
        shader.setColorUniform("goldLit", c.goldLit.toArgb())
        shader.setColorUniform("goldShade", c.goldShade.toArgb())
        shader.setColorUniform("goldDeep", c.goldDeep.toArgb())
        shader.setColorUniform("lineCore", c.lineCore.toArgb())
        onDrawBehind {
            // 接地の影(下へずらしてぼかす。面の下に隠れる分は見えない)
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                nc.save()
                nc.translate(0f, shadowDy)
                nc.drawPath(native, shadow)
                nc.restore()
            }
            clipPath(path) {
                shader.setFloatUniform("line", line())
                shader.setFloatUniform("ignite", ignite.value)
                val t = touch()
                shader.setFloatUniform("touch", t.x, t.y)
                drawRect(brush)
            }
            // 輪郭の暗い線
            drawPath(path, rimColor, style = Stroke(rimWidth))
        }
    }
}

/**
 * 琥珀に灯る窓(名刺の面)。矩形いっぱいに描く。形で clip する。
 * @param glow 灯りの強さ 0..1(登場で 0→1)
 */
@Composable
fun Modifier.kinuWindow(
    shape: Shape,
    glow: () -> Float = { 1f },
    seed: Float = 0f,
): Modifier {
    val c = KinuTheme.colors
    val hdr = LocalGlowHeadroom.current
    val shader = remember { RuntimeShader(KinuShaders.WINDOW) }
    return this.clip(shape).drawWithCache {
        val brush = ShaderBrush(shader)
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("dp", 1.dp.toPx())
        shader.setFloatUniform("hdr", hdr)
        shader.setFloatUniform("seed", seed)
        shader.setColorUniform("amberHi", c.amberHi.toArgb())
        shader.setColorUniform("amber", c.amber.toArgb())
        shader.setColorUniform("amberDeep", c.amberDeep.toArgb())
        shader.setColorUniform("mullion", c.mullion.toArgb())
        onDrawBehind {
            shader.setFloatUniform("glow", glow())
            drawRect(brush)
        }
    }
}

/**
 * 夜のガラス(副ボタン・アイコンボタン・入力欄・ダイアログの面)。形で clip し、1dp の金の縁(rim)を描く。
 * @param line 上の縁の光の線 0..1.6(通常 [KinuLight.LINE_NIGHT])
 * @param ember 奥の琥珀のにじみ 0..1(入力中の欄)
 */
@Composable
fun Modifier.kinuNightGlass(
    shape: Shape,
    line: () -> Float = { KinuLight.LINE_NIGHT },
    ember: () -> Float = { 0f },
    touch: () -> Offset = { KinuTouchNone },
): Modifier {
    val c = KinuTheme.colors
    val hdr = LocalGlowHeadroom.current
    val shader = remember { RuntimeShader(KinuShaders.NIGHT_GLASS) }
    return this.clip(shape).drawWithCache {
        val brush = ShaderBrush(shader)
        // 縁は 2dp で描き、外側の半分を clip で切る(内側に 1dp 残る)
        val rim = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val rimWidth = 2.dp.toPx()
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("dp", 1.dp.toPx())
        shader.setFloatUniform("hdr", hdr)
        shader.setColorUniform("glassTop", c.glassTop.toArgb())
        shader.setColorUniform("glassBottom", c.glassBottom.toArgb())
        shader.setColorUniform("fin", c.fin.toArgb())
        shader.setColorUniform("lineCore", c.lineCore.toArgb())
        shader.setColorUniform("amber", c.amber.toArgb())
        onDrawBehind {
            shader.setFloatUniform("line", line())
            shader.setFloatUniform("ember", ember())
            val t = touch()
            shader.setFloatUniform("touch", t.x, t.y)
            drawRect(brush)
            drawPath(rim, c.rim, style = Stroke(rimWidth))
        }
    }
}
