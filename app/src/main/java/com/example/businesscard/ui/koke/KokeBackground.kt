package com.example.businesscard.ui.koke

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 画面の背景。平らな霧の灰だけ(苔も岩も置かない。黒い岩の部品と、そこに生える苔を主役にするため)。
 * システムバーは避けない(避けるのは骨格 [KokeScaffold] と、各画面の役目)。
 */
@Composable
fun KokeBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KokeTheme.colors.ground),
        content = content,
    )
}

/**
 * 全画面共通の骨格。背景は画面ごとに不透明で描く
 * (画面遷移のクロスフェード中に、2画面が重なって見えないようにするため)。
 * システムバーを避ける余白は Scaffold の既定のまま(上のバーは自分で状態バーを避ける)。
 */
@Composable
fun KokeScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    KokeBackground(modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = KokeTheme.colors.groundInk,
            topBar = topBar,
            bottomBar = bottomBar,
            content = content,
        )
    }
}
