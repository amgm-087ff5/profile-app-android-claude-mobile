package com.example.businesscard.ui.kinu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.ui.glass.rememberGlassFeedback
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 名刺の外形。上の角は丸く、下の縁は 1 周期ゆるく波打つ(振幅 [KinuDimens.lipWave])。
 * 下の角は波の縁から続けて丸める。
 */
private class KinuCardShape(private val corner: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { corner.toPx() }
        val amp = with(density) { KinuDimens.lipWave.toPx() }
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, r)
            arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
            lineTo(w - r, 0f)
            arcTo(Rect(w - 2 * r, 0f, w, 2 * r), 270f, 90f, false)
            // 右の縁を下り、下の縁を右から左へ戻る(波と下の角)
            val steps = 64
            for (i in 0..steps) {
                val x = w * (1f - i.toFloat() / steps)
                val wave = h - amp * (0.5f - 0.5f * cos(2f * Math.PI.toFloat() * x / w))
                val d = min(x, w - x)
                val y = if (d < r) {
                    val k = r - d
                    min(wave, h - r + sqrt((r * r - k * k).coerceAtLeast(0f)))
                } else {
                    wave
                }
                lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * 名刺。琥珀に灯る窓の面に、上下の金の縁(リップ)。下の縁は波打つ。
 * 押すと縁の光の線が強まる(大きさは変えない)。
 *
 * @param onClick 指定すると面全体がボタンになる
 */
@Composable
fun KinuBusinessCard(
    card: BusinessCard,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = KinuTheme.colors
    val shape = remember { KinuCardShape(CARD_CORNER) }
    val ignite = LocalKinuIgnite.current
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = rememberKinuPress(interaction, feedback)
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val bottomLipPx = with(density) { (KinuDimens.lipHeight + KinuDimens.lipWave).toPx() }

    // 下の縁は card の下端から測るので、指の y を縁の中の位置へ直す
    val bottomTouch: () -> Offset = {
        val t = press.touch
        if (t.x < 0f) t else Offset(t.x, t.y - (cardSize.height - bottomLipPx))
    }

    Box(
        modifier = modifier
            .aspectRatio(CARD_ASPECT_RATIO)
            .onSizeChanged { cardSize = it }
            .kinuWindow(shape, glow = { ignite.value }, seed = 0.4f)
            .then(
                if (onClick != null) {
                    Modifier
                        .kinuTouch(press)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClick = {
                                feedback.click()
                                onClick()
                            },
                        )
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(KinuDimens.lipHeight)
                .kinuRibbon(
                    RectangleShape,
                    line = { KinuLight.LINE_GOLD + KinuLight.LINE_PRESS_BOOST * press.value },
                    touch = { press.touch },
                    seed = 0.2f,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(KinuDimens.lipHeight + KinuDimens.lipWave)
                .kinuRibbon(
                    RectangleShape,
                    line = { KinuLight.LINE_GOLD + KinuLight.LINE_PRESS_BOOST * press.value },
                    touch = bottomTouch,
                    seed = 0.7f,
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp,
                    top = KinuDimens.lipHeight + 16.dp,
                    end = 24.dp,
                    bottom = KinuDimens.lipHeight + KinuDimens.lipWave + 16.dp,
                ),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                if (card.company.isNotEmpty()) {
                    Text(text = card.company, style = KinuType.cardCompany, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (card.title.isNotEmpty()) {
                    Text(text = card.title, style = KinuType.cardDetail, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(text = card.name, style = KinuType.cardName, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Column {
                if (card.phone.isNotEmpty()) {
                    Text(text = card.phone, style = KinuType.cardDetail, color = c.inkMuted, maxLines = 1)
                }
                if (card.email.isNotEmpty()) {
                    Text(text = card.email, style = KinuType.cardDetail, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** 名刺の角の丸み */
private val CARD_CORNER = 22.dp

/** 日本の名刺の標準サイズ 91mm x 55mm */
private const val CARD_ASPECT_RATIO = 91f / 55f
