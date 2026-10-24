package com.example.businesscard.ui.kinu

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.businesscard.ui.glass.LocalGlowHeadroom

/**
 * 画面全体の背景。黒い塔と、足元に重なる金の帯・琥珀の窓を BACKGROUND シェーダーで描く。
 * 下の [KinuDimens.FACADE_FRACTION] が足元(帯と窓)。画面を開くと光の線が左から灯る。
 */
@Composable
fun KinuBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = KinuTheme.colors
    val hdr = LocalGlowHeadroom.current
    val ignite = rememberKinuIgnite()
    val shader = remember { RuntimeShader(KinuShaders.BACKGROUND) }
    CompositionLocalProvider(LocalKinuIgnite provides ignite) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawWithCache {
                    val brush = ShaderBrush(shader)
                    shader.setFloatUniform("size", size.width, size.height)
                    shader.setFloatUniform("dp", 1.dp.toPx())
                    shader.setFloatUniform("facadeTop", size.height * (1f - KinuDimens.FACADE_FRACTION))
                    shader.setFloatUniform("hdr", hdr)
                    shader.setColorUniform("tower", c.tower.toArgb())
                    shader.setColorUniform("fin", c.fin.toArgb())
                    shader.setColorUniform("goldLit", c.goldLit.toArgb())
                    shader.setColorUniform("goldShade", c.goldShade.toArgb())
                    shader.setColorUniform("amber", c.amber.toArgb())
                    shader.setColorUniform("lineCore", c.lineCore.toArgb())
                    onDrawBehind {
                        shader.setFloatUniform("ignite", ignite.value)
                        drawRect(brush)
                    }
                },
            content = content,
        )
    }
}

/**
 * 全画面共通の骨格。背景は画面いっぱい(端まで描く)、中身はシステムバーを避けて置く。
 */
@Composable
fun KinuScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    KinuBackground(modifier) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = Color.Transparent,
            contentColor = KinuTheme.colors.champagne,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = topBar,
            bottomBar = bottomBar,
            content = content,
        )
    }
}
