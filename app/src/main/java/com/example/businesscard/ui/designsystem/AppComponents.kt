package com.example.businesscard.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.ui.component.BusinessCardView
import com.example.businesscard.ui.component.GlassButton
import com.example.businesscard.ui.component.GlassButtonStyle
import com.example.businesscard.ui.component.GlassConfirmDialog
import com.example.businesscard.ui.component.GlassGlow
import com.example.businesscard.ui.component.GlassIconButton
import com.example.businesscard.ui.component.GlassTextField
import com.example.businesscard.ui.component.SoftGlassBackground
import com.example.businesscard.ui.component.SoftGlassScaffold
import com.example.businesscard.ui.component.SoftGlassTopBar
import com.example.businesscard.ui.component.glassSurface
import com.example.businesscard.ui.glass.LocalGlassScene
import com.example.businesscard.ui.moss.MossBackground
import com.example.businesscard.ui.moss.MossBusinessCard
import com.example.businesscard.ui.moss.MossButton
import com.example.businesscard.ui.moss.MossChoice
import com.example.businesscard.ui.moss.MossConfirmDialog
import com.example.businesscard.ui.moss.MossDialog
import com.example.businesscard.ui.moss.MossIconButton
import com.example.businesscard.ui.moss.MossMessage
import com.example.businesscard.ui.moss.MossScaffold
import com.example.businesscard.ui.moss.MossTextField
import com.example.businesscard.ui.moss.MossTheme
import com.example.businesscard.ui.moss.MossTone
import com.example.businesscard.ui.moss.MossTopBar
import com.example.businesscard.ui.moss.MossType
import com.example.businesscard.ui.porcelain.PorcelainBackground
import com.example.businesscard.ui.porcelain.PorcelainBusinessCard
import com.example.businesscard.ui.porcelain.PorcelainButton
import com.example.businesscard.ui.porcelain.PorcelainChoice
import com.example.businesscard.ui.porcelain.PorcelainConfirmDialog
import com.example.businesscard.ui.porcelain.PorcelainDialog
import com.example.businesscard.ui.porcelain.PorcelainIconButton
import com.example.businesscard.ui.porcelain.PorcelainMessage
import com.example.businesscard.ui.porcelain.PorcelainScaffold
import com.example.businesscard.ui.porcelain.PorcelainTextField
import com.example.businesscard.ui.porcelain.PorcelainTheme
import com.example.businesscard.ui.porcelain.PorcelainTone
import com.example.businesscard.ui.porcelain.PorcelainTopBar
import com.example.businesscard.ui.porcelain.PorcelainType
import com.example.businesscard.ui.theme.SoftGlassShapes
import com.example.businesscard.ui.theme.SoftGlassTheme
import com.example.businesscard.ui.theme.SoftGlassType

/*
 * 画面が使う共通部品の窓口。画面はここの App* だけを使い、見た目(Edgelit / Porcelain / Moss)を知らない。
 * 中身は LocalThemeStyle を見て、それぞれのデザインシステムの部品に振り分ける。
 *
 * 見た目を増やすときは、ThemeStyle に種類を足し、ここの when に分岐を足す(画面は変えなくてよい)。
 */

/**
 * ボタンの役割。
 *  - Primary: その画面の主操作(1画面に1つまで)。Edgelit では強く光り、Porcelain では濃い板、Moss では苔のクッション
 *  - Secondary: 副操作。Edgelit では中くらいの光、Porcelain では明るい板、Moss では磨いた石
 *  - Danger: 取り消せない操作。危険色の文字(Moss では杉の枯れ葉の赤茶)
 */
enum class AppButtonStyle { Primary, Secondary, Danger }

private fun AppButtonStyle.toGlass() = when (this) {
    AppButtonStyle.Primary -> GlassButtonStyle.Primary
    AppButtonStyle.Secondary -> GlassButtonStyle.Secondary
    AppButtonStyle.Danger -> GlassButtonStyle.Danger
}

private fun AppButtonStyle.toPorcelainTone() = when (this) {
    AppButtonStyle.Primary -> PorcelainTone.Dark
    AppButtonStyle.Secondary, AppButtonStyle.Danger -> PorcelainTone.Light
}

private fun AppButtonStyle.toMossTone() = when (this) {
    AppButtonStyle.Primary -> MossTone.Moss
    AppButtonStyle.Secondary, AppButtonStyle.Danger -> MossTone.Stone
}

/** 全画面共通の骨格(背景・上のバー・下の操作・中身)。 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> SoftGlassScaffold(modifier, topBar, bottomBar, content)
        ThemeStyle.PORCELAIN -> PorcelainScaffold(modifier, topBar, bottomBar, content)
        ThemeStyle.MOSS -> MossScaffold(modifier, topBar, bottomBar, content)
    }
}

/** 骨格を使わない画面(表示画面)の背景。 */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> SoftGlassBackground(modifier, content)
        ThemeStyle.PORCELAIN -> PorcelainBackground(modifier, content = content)
        ThemeStyle.MOSS -> MossBackground(modifier, content = content)
    }
}

/** 画面上部。左に戻る、タイトル、右端に [actions](アイコンボタン)。 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> Box(modifier.fillMaxWidth()) {
            // Edgelit の上部バーには右端の操作の場所が無いので、タイトルの右を空けて重ねる
            SoftGlassTopBar(
                title = title,
                onBack = onBack,
                modifier = Modifier.padding(end = if (actions != null) 72.dp else 0.dp),
            )
            if (actions != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    content = actions,
                )
            }
        }
        ThemeStyle.PORCELAIN -> PorcelainTopBar(title = title, modifier = modifier, onBack = onBack, actions = actions)
        ThemeStyle.MOSS -> MossTopBar(title = title, modifier = modifier, onBack = onBack, actions = actions)
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    icon: ImageVector? = null,
) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> GlassButton(text = text, onClick = onClick, modifier = modifier, style = style.toGlass(), icon = icon)
        ThemeStyle.PORCELAIN -> PorcelainButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            tone = style.toPorcelainTone(),
            leadingIcon = icon,
            contentColor = when (style) {
                AppButtonStyle.Primary -> PorcelainTheme.colors.onDark
                AppButtonStyle.Secondary -> PorcelainTheme.colors.ink
                AppButtonStyle.Danger -> PorcelainTheme.colors.danger
            },
        )
        ThemeStyle.MOSS -> MossButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            tone = style.toMossTone(),
            leadingIcon = icon,
            contentColor = when (style) {
                AppButtonStyle.Primary -> MossTheme.colors.onMoss
                AppButtonStyle.Secondary -> MossTheme.colors.ink
                AppButtonStyle.Danger -> MossTheme.colors.danger
            },
        )
    }
}

/** アイコンだけのボタン。既定は副操作([AppButtonStyle.Secondary])。 */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Secondary,
) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> GlassIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            style = style.toGlass(),
        )
        ThemeStyle.PORCELAIN -> PorcelainIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            tone = style.toPorcelainTone(),
            contentColor = when (style) {
                AppButtonStyle.Primary -> PorcelainTheme.colors.onDark
                AppButtonStyle.Secondary -> PorcelainTheme.colors.ink
                AppButtonStyle.Danger -> PorcelainTheme.colors.danger
            },
        )
        ThemeStyle.MOSS -> MossIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            tone = style.toMossTone(),
            contentColor = when (style) {
                AppButtonStyle.Primary -> MossTheme.colors.onMoss
                AppButtonStyle.Secondary -> MossTheme.colors.ink
                AppButtonStyle.Danger -> MossTheme.colors.danger
            },
        )
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> GlassTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            isError = isError,
            errorText = errorText,
            keyboardOptions = keyboardOptions,
        )
        ThemeStyle.PORCELAIN -> PorcelainTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            isError = isError,
            errorText = errorText,
            keyboardOptions = keyboardOptions,
        )
        ThemeStyle.MOSS -> MossTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            isError = isError,
            errorText = errorText,
            keyboardOptions = keyboardOptions,
        )
    }
}

/** 名刺(レイアウト固定)。[onClick] を渡すと面全体がボタンになる。 */
@Composable
fun AppBusinessCard(card: BusinessCard, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> BusinessCardView(card = card, modifier = modifier, onClick = onClick)
        ThemeStyle.PORCELAIN -> PorcelainBusinessCard(card = card, modifier = modifier, onClick = onClick)
        ThemeStyle.MOSS -> MossBusinessCard(card = card, modifier = modifier, onClick = onClick)
    }
}

/** 文字だけの案内(一覧が空のときなど)。 */
@Composable
fun AppMessage(text: String, modifier: Modifier = Modifier) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> Text(
            text = text,
            style = SoftGlassType.body,
            color = SoftGlassTheme.colors.ink,
            textAlign = TextAlign.Center,
            modifier = modifier
                .glassSurface(SoftGlassShapes.card)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        )
        ThemeStyle.PORCELAIN -> PorcelainMessage(text = text, modifier = modifier)
        ThemeStyle.MOSS -> MossMessage(text = text, modifier = modifier)
    }
}

/** 取り消せない操作の確認ダイアログ。 */
@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> GlassConfirmDialog(title, message, confirmLabel, dismissLabel, onConfirm, onDismiss)
        ThemeStyle.PORCELAIN -> PorcelainConfirmDialog(title, message, confirmLabel, dismissLabel, onConfirm, onDismiss)
        ThemeStyle.MOSS -> MossConfirmDialog(title, message, confirmLabel, dismissLabel, onConfirm, onDismiss)
    }
}

/** 中身を自由に並べるダイアログ。 */
@Composable
fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    message: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> Dialog(onDismissRequest = onDismiss) {
            // ダイアログは別の窓なので、画面の壁を透かせない(すりガラスはダイアログの色で描く)
            CompositionLocalProvider(LocalGlassScene provides null) {
                Box(modifier = Modifier.padding(24.dp)) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .glassSurface(shape = SoftGlassShapes.dialog, fill = SoftGlassTheme.colors.dialog, glow = GlassGlow.Soft)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(text = title, style = SoftGlassType.cardCompany, color = SoftGlassTheme.colors.ink)
                        if (message != null) {
                            Text(text = message, style = SoftGlassType.body, color = SoftGlassTheme.colors.inkMuted)
                        }
                        content()
                    }
                }
            }
        }
        ThemeStyle.PORCELAIN -> PorcelainDialog(title = title, onDismiss = onDismiss, message = message, content = content)
        ThemeStyle.MOSS -> MossDialog(title = title, onDismiss = onDismiss, message = message, content = content)
    }
}

/** 選択肢の1つ。選んでいるものには印が付く。 */
@Composable
fun AppChoice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> GlassButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            style = if (selected) GlassButtonStyle.Primary else GlassButtonStyle.Secondary,
            icon = if (selected) Icons.Filled.Check else null,
        )
        ThemeStyle.PORCELAIN -> PorcelainChoice(text = text, selected = selected, onClick = onClick, modifier = modifier)
        ThemeStyle.MOSS -> MossChoice(text = text, selected = selected, onClick = onClick, modifier = modifier)
    }
}

/** 補足の小さな文字(選択肢の説明など)。 */
@Composable
fun AppCaption(text: String, modifier: Modifier = Modifier) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> Text(text = text, style = SoftGlassType.caption, color = SoftGlassTheme.colors.inkMuted, modifier = modifier)
        ThemeStyle.PORCELAIN -> Text(text = text, style = PorcelainType.caption, color = PorcelainTheme.colors.inkMuted, modifier = modifier)
        ThemeStyle.MOSS -> Text(text = text, style = MossType.caption, color = MossTheme.colors.inkMuted, modifier = modifier)
    }
}
