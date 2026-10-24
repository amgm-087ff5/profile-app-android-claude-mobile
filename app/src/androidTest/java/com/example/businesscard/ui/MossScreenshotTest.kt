package com.example.businesscard.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.ui.designsystem.BusinessCardAppTheme
import com.example.businesscard.ui.detail.DetailScreen
import com.example.businesscard.ui.detail.DetailUiState
import com.example.businesscard.ui.edit.EditScreen
import com.example.businesscard.ui.edit.EditUiState
import com.example.businesscard.ui.list.ListScreen
import com.example.businesscard.ui.list.ListUiState
import com.example.businesscard.ui.moss.MossCatalog
import com.example.businesscard.ui.preview.SampleCards
import com.example.businesscard.ui.settings.ThemeSettingsDialog
import com.example.businesscard.ui.settings.ThemeSettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Moss の見た目の確認用に、カタログと各画面を実機(またはエミュレータ)で描いてPNGに保存する。
 * 背景や面(苔と石)は GPU のシェーダー(AGSL)で描くので、実際の Android の描画で撮る。
 * 画像は `files/screenshots/moss_*.png`。CI(scripts/device-screenshots.sh)が取り出して公開する。
 */
@RunWith(AndroidJUnit4::class)
class MossScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun catalog() = capture("moss_catalog") { MossCatalog() }

    @Test
    fun list() = capture("moss_list") {
        ListScreen(uiState = ListUiState(cards = SampleCards.all, isLoading = false), onAddClick = {}, onCardClick = {})
    }

    @Test
    fun listEmpty() = capture("moss_list_empty") {
        ListScreen(uiState = ListUiState(cards = emptyList(), isLoading = false), onAddClick = {}, onCardClick = {})
    }

    @Test
    fun edit() = capture("moss_edit") {
        val card = SampleCards.taro
        EditScreen(
            uiState = EditUiState(
                name = card.name,
                company = card.company,
                title = card.title,
                phone = card.phone,
                email = card.email,
                isNew = false,
            ),
            onBack = {},
            onNameChange = {},
            onCompanyChange = {},
            onTitleChange = {},
            onPhoneChange = {},
            onEmailChange = {},
            onSave = {},
            onDelete = {},
        )
    }

    @Test
    fun editNewWithError() = capture("moss_edit_new_error") {
        EditScreen(
            uiState = EditUiState(isNew = true, nameError = true),
            onBack = {},
            onNameChange = {},
            onCompanyChange = {},
            onTitleChange = {},
            onPhoneChange = {},
            onEmailChange = {},
            onSave = {},
            onDelete = {},
        )
    }

    @Test
    fun detail() = capture("moss_detail") {
        DetailScreen(uiState = DetailUiState(card = SampleCards.taro, isLoading = false), onBack = {}, onEdit = {})
    }

    /** 見た目を選ぶダイアログ(別の窓なので、画面全体を撮る) */
    @Test
    fun themeDialog() {
        composeRule.setContent {
            BusinessCardAppTheme(ThemeStyle.MOSS) {
                ListScreen(uiState = ListUiState(cards = SampleCards.all, isLoading = false), onAddClick = {}, onCardClick = {})
                ThemeSettingsDialog(
                    uiState = ThemeSettingsUiState(selected = ThemeStyle.MOSS),
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }
        settle()
        save("moss_theme_dialog", InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
    }

    /** 濃いボタンを押したまま撮る(沈み込み・内側の影の確認用) */
    @Test
    fun catalogPressed() {
        composeRule.setContent { BusinessCardAppTheme(ThemeStyle.MOSS) { MossCatalog() } }
        settle()
        composeRule.onNode(hasText("Press") and hasClickAction())
            .performTouchInput { down(Offset(width * 0.5f, height * 0.5f)) }
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()
        save("moss_catalog_pressed", captureRoot())
    }

    private fun settle() {
        composeRule.waitForIdle()
        Thread.sleep(1_500)
        composeRule.waitForIdle()
        // 苔と石の光が落ち着くまで進める
        composeRule.mainClock.advanceTimeBy(1_200)
        composeRule.waitForIdle()
    }

    private fun screenshotDir(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File(context.filesDir, "screenshots").apply { mkdirs() }
    }

    private fun save(name: String, bitmap: Bitmap) {
        File(screenshotDir(), "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeRule.setContent { BusinessCardAppTheme(ThemeStyle.MOSS, content) }
        settle()
        save(name, captureRoot())
    }

    /** エミュレータのソフトウェアGPUでは描画に時間がかかり、撮影の待ち時間を超えることがあるので、何度かやり直す。 */
    private fun captureRoot(): Bitmap {
        repeat(CAPTURE_ATTEMPTS) { attempt ->
            try {
                return composeRule.onRoot().captureToImage().asAndroidBitmap()
            } catch (e: ComposeTimeoutException) {
                composeRule.waitForIdle()
                Thread.sleep(1_000)
            }
        }
        // それでも間に合わないときは、画面全体をそのまま撮る(描き終わるのを待たない撮り方)
        Thread.sleep(3_000)
        return InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
    }

    private companion object {
        const val CAPTURE_ATTEMPTS = 4
    }
}
