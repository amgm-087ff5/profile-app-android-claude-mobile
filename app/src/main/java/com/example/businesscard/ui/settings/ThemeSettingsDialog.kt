package com.example.businesscard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.businesscard.R
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.ui.designsystem.AppButton
import com.example.businesscard.ui.designsystem.AppButtonStyle
import com.example.businesscard.ui.designsystem.AppCaption
import com.example.businesscard.ui.designsystem.AppChoice
import com.example.businesscard.ui.designsystem.AppDialog
import com.example.businesscard.ui.designsystem.BusinessCardAppTheme

@Composable
fun ThemeSettingsRoute(
    onDismiss: () -> Unit,
    viewModel: ThemeSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ThemeSettingsDialog(uiState = uiState, onSelect = viewModel::select, onDismiss = onDismiss)
}

/**
 * 見た目を選ぶダイアログ。選ぶとすぐに切り替わる(ダイアログもその見た目で描き直される)。
 * 選んでいる見た目には印が付く。
 */
@Composable
fun ThemeSettingsDialog(
    uiState: ThemeSettingsUiState,
    onSelect: (ThemeStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        title = stringResource(R.string.theme_settings),
        message = stringResource(R.string.theme_settings_message),
        onDismiss = onDismiss,
    ) {
        uiState.options.forEach { style ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppChoice(
                    text = stringResource(style.titleRes),
                    selected = style == uiState.selected,
                    onClick = { onSelect(style) },
                    modifier = Modifier.fillMaxWidth(),
                )
                AppCaption(text = stringResource(style.descriptionRes), modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
        AppButton(
            text = stringResource(R.string.close),
            onClick = onDismiss,
            style = AppButtonStyle.Secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

private val ThemeStyle.titleRes: Int
    get() = when (this) {
        ThemeStyle.EDGELIT -> R.string.theme_edgelit
        ThemeStyle.PORCELAIN -> R.string.theme_porcelain
        ThemeStyle.KOKE -> R.string.theme_koke
    }

private val ThemeStyle.descriptionRes: Int
    get() = when (this) {
        ThemeStyle.EDGELIT -> R.string.theme_edgelit_description
        ThemeStyle.PORCELAIN -> R.string.theme_porcelain_description
        ThemeStyle.KOKE -> R.string.theme_koke_description
    }

@Preview(name = "Porcelain")
@Composable
private fun ThemeSettingsDialogPreview() {
    BusinessCardAppTheme(ThemeStyle.PORCELAIN) {
        ThemeSettingsDialog(uiState = ThemeSettingsUiState(selected = ThemeStyle.PORCELAIN), onSelect = {}, onDismiss = {})
    }
}

@Preview(name = "Koke")
@Composable
private fun ThemeSettingsDialogKokePreview() {
    BusinessCardAppTheme(ThemeStyle.KOKE) {
        ThemeSettingsDialog(uiState = ThemeSettingsUiState(selected = ThemeStyle.KOKE), onSelect = {}, onDismiss = {})
    }
}
