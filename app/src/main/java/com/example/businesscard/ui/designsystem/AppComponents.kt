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
import com.example.businesscard.ui.kinu.KinuBackground
import com.example.businesscard.ui.kinu.KinuBusinessCard
import com.example.businesscard.ui.kinu.KinuButton
import com.example.businesscard.ui.kinu.KinuChoice
import com.example.businesscard.ui.kinu.KinuConfirmDialog
import com.example.businesscard.ui.kinu.KinuDialog
import com.example.businesscard.ui.kinu.KinuIconButton
import com.example.businesscard.ui.kinu.KinuMessage
import com.example.businesscard.ui.kinu.KinuScaffold
import com.example.businesscard.ui.kinu.KinuTextField
import com.example.businesscard.ui.kinu.KinuTheme
import com.example.businesscard.ui.kinu.KinuTone
import com.example.businesscard.ui.kinu.KinuTopBar
import com.example.businesscard.ui.kinu.KinuType
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
 * 画面が使う共通部品の窓口。画面はここの App* だけを使い、見た目(Edgelit / Porcelain)を知らない。
 * 中身は LocalThemeStyle を見て、それぞれのデザインシステムの部品に振り分ける。
 *
 * 見た目を増やすときは、ThemeStyle に種類を足し、ここの when に分岐を足す(画面は変えなくてよい)。
 */

/**
 * ボタンの役割。
 *  - Primary: その画面の主操作(1画面に1つまで)。Edgelit では強く光り、Porcelain では濃い板
 *  - Secondary: 副操作。Edgelit では中くらいの光、Porcelain では明るい板
 *  - Danger: 取り消せない操作。危険色の文字
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

private fun AppButtonStyle.toKinuTone() = when (this) {
    AppButtonStyle.Primary -> KinuTone.Gold
    AppButtonStyle.Secondary, AppButtonStyle.Danger -> KinuTone.Night
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
        ThemeStyle.KINU -> KinuScaffold(modifier, topBar, bottomBar, content)
    }
}

/** 骨格を使わない画面(表示画面)の背景。 */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> SoftGlassBackground(modifier, content)
        ThemeStyle.PORCELAIN -> PorcelainBackground(modifier, content = content)
        ThemeStyle.KINU -> KinuBackground(modifier, content)
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
        ThemeStyle.KINU -> KinuTopBar(title = title, modifier = modifier, onBack = onBack, actions = actions)
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
        ThemeStyle.KINU -> KinuButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            tone = style.toKinuTone(),
            leadingIcon = icon,
            contentColor = when (style) {
                AppButtonStyle.Primary -> KinuTheme.colors.ink
                AppButtonStyle.Secondary -> KinuTheme.colors.champagne
                AppButtonStyle.Danger -> KinuTheme.colors.danger
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
        ThemeStyle.KINU -> KinuIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            tone = style.toKinuTone(),
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
        ThemeStyle.KINU -> KinuTextField(
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
        ThemeStyle.KINU -> KinuBusinessCard(card = card, modifier = modifier, onClick = onClick)
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
        ThemeStyle.KINU -> KinuMessage(text = text, modifier = modifier)
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
        ThemeStyle.KINU -> KinuConfirmDialog(title, message, confirmLabel, dismissLabel, onConfirm, onDismiss)
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
        ThemeStyle.KINU -> KinuDialog(title = title, onDismiss = onDismiss, message = message, content = content)
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
        ThemeStyle.KINU -> KinuChoice(text = text, selected = selected, onClick = onClick, modifier = modifier)
    }
}

/** 補足の小さな文字(選択肢の説明など)。 */
@Composable
fun AppCaption(text: String, modifier: Modifier = Modifier) {
    when (LocalThemeStyle.current) {
        ThemeStyle.EDGELIT -> Text(text = text, style = SoftGlassType.caption, color = SoftGlassTheme.colors.inkMuted, modifier = modifier)
        ThemeStyle.PORCELAIN -> Text(text = text, style = PorcelainType.caption, color = PorcelainTheme.colors.inkMuted, modifier = modifier)
        ThemeStyle.KINU -> Text(text = text, style = KinuType.caption, color = KinuTheme.colors.champagneMuted, modifier = modifier)
    }
}
