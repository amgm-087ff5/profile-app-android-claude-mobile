package com.example.businesscard.ui.moss

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ShaderBrush

/** 岩の背景(画面の中ほどに、くっきりする位置)。見本の写真で、手前の岩肌がはっきり見える高さ。 */
private const val ROCK_FOCUS = 0.35f

/** 岩の模様をずらす種。背景は 1 枚だけなので固定値でよい。 */
private const val ROCK_SEED = 0.37f

/**
 * 画面全体の背景。濡れた黒い岩の面を、AGSL のシェーダー(MossShaders.ROCK)で描く。
 * 上ほど奥でぼけた苔庭、下ほど手前でくっきりした岩になる。
 *
 * 岩は画面の大きさに合わせて 1 回描けばよいので、ここでは毎回の描き直しを避けて、
 * 描く内容(シェーダー)だけを作り置きする。
 */
@Composable
fun MossBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val rock = remember { RuntimeShader(MossShaders.ROCK) }
    Box(modifier = modifier.fillMaxSize()) {
        // 岩は重いシェーダーなので、別のレイヤー(オフスクリーン)に描いて使い回す。
        // 上に載る部品が動いても、大きさが変わらない限り岩は描き直さない
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    val brush = ShaderBrush(rock)
                    rock.setFloatUniform("size", size.width, size.height)
                    rock.setFloatUniform("dp", density)
                    rock.setFloatUniform("seed", ROCK_SEED)
                    rock.setFloatUniform("focus", ROCK_FOCUS)
                    onDrawBehind { drawRect(brush = brush) }
                },
        )
        content()
    }
}

/**
 * 全画面共通の骨格。上のバーと中身を、岩の背景の上に置く。
 * Scaffold は透明にし、システムバーを避ける余白は Scaffold の既定のまま(重ねて避けない)。
 */
@Composable
fun MossScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    MossBackground(modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MossTheme.colors.ink,
            topBar = topBar,
            bottomBar = bottomBar,
            content = content,
        )
    }
}
