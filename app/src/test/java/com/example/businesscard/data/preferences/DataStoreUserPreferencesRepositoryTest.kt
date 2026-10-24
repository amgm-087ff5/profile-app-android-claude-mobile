package com.example.businesscard.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.businesscard.domain.model.ThemeStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 本物の DataStore(一時フォルダのファイル)で、保存と読み出しを確かめる。 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreUserPreferencesRepositoryTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun createRepository() = DataStoreUserPreferencesRepository(
        PreferenceDataStoreFactory.create(scope = testScope.backgroundScope) {
            tmpFolder.newFile("user_preferences_test.preferences_pb")
        },
    )

    @Test
    fun nothingSaved_returnsDefault() = testScope.runTest {
        val repository = createRepository()

        assertEquals(ThemeStyle.DEFAULT, repository.themeStyle.first())
    }

    @Test
    fun setThemeStyle_isReadBack() = testScope.runTest {
        val repository = createRepository()

        repository.setThemeStyle(ThemeStyle.EDGELIT)
        assertEquals(ThemeStyle.EDGELIT, repository.themeStyle.first())

        repository.setThemeStyle(ThemeStyle.PORCELAIN)
        assertEquals(ThemeStyle.PORCELAIN, repository.themeStyle.first())

        repository.setThemeStyle(ThemeStyle.KINU)
        assertEquals(ThemeStyle.KINU, repository.themeStyle.first())
    }

    @Test
    fun unknownName_fallsBackToDefault() {
        assertEquals(ThemeStyle.DEFAULT, "REMOVED_STYLE".toThemeStyle())
        assertEquals(ThemeStyle.DEFAULT, null.toThemeStyle())
        assertEquals(ThemeStyle.EDGELIT, "EDGELIT".toThemeStyle())
        assertEquals(ThemeStyle.KINU, "KINU".toThemeStyle())
    }
}
