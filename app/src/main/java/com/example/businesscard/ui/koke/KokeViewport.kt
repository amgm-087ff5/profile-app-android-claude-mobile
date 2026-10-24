package com.example.businesscard.ui.koke

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.example.businesscard.ui.glass.prefersReducedMotion

/**
 * 画面(窓)の中での部品の中心の位置。
 * 画面の中心が (0, 0)、上端・左端が -1、下端・右端が +1 になる。
 * 部品の中心が画面の外へ出ていても値は -1.5 から 1.5 の範囲に収まる。
 *
 * 値は描くときに読むこと(drawWithCache の onDraw の中など)。
 * 組み立て直し(recomposition)は起きないので、読んでも重くならない。
 * スクロールのたびに更新される。「アニメーションを削除」設定のときは常に (0, 0)。
 */
@Stable
class KokeViewOffset internal constructor() {
    private var offset by mutableStateOf(Offset.Zero)

    /** 窓の中での部品の中心の位置。-1.5 から 1.5 に収まる。描画の中で読むこと。 */
    val value: Offset
        get() = offset

    /** 位置を書き換える。[trackKokeViewOffset] から呼ばれる。 */
    internal fun update(o: Offset) {
        offset = o
    }
}

/** 画面の中の位置を覚えておく入れ物を作る。部品と同じ場所で一つ作って渡す。 */
@Composable
fun rememberKokeViewOffset(): KokeViewOffset = remember { KokeViewOffset() }

/**
 * 部品の位置を [state] に書き込む。
 * 窓の幅と高さから、部品の中心の位置を -1 から +1 の割合にして、-1.5 から 1.5 に収める。
 * 窓の大きさがまだ 0 のときは更新しない。
 * 「アニメーションを削除」設定のときは何もしない(位置は常に (0, 0) のまま)。
 */
@Composable
fun Modifier.trackKokeViewOffset(state: KokeViewOffset): Modifier {
    if (LocalContext.current.prefersReducedMotion()) return this
    val view = LocalView.current
    return onGloballyPositioned { coordinates ->
        val windowWidth = view.width.toFloat()
        val windowHeight = view.height.toFloat()
        if (windowWidth <= 0f || windowHeight <= 0f) return@onGloballyPositioned
        val center = coordinates.boundsInWindow().center
        val x = ((center.x / windowWidth) * 2f - 1f).coerceIn(-1.5f, 1.5f)
        val y = ((center.y / windowHeight) * 2f - 1f).coerceIn(-1.5f, 1.5f)
        state.update(Offset(x, y))
    }
}
