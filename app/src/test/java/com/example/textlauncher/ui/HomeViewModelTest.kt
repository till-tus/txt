package com.example.textlauncher.ui

import com.example.textlauncher.data.LauncherSettingsStore
import com.example.textlauncher.data.NoteStore
import com.example.textlauncher.data.NoteStoreState
import com.example.textlauncher.data.ShortcutStore
import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun rapidSettingsChanges_persistOnlyLatestStateAfterDebounce() = runTest(dispatcher) {
        val settings = FakeSettingsStore()
        val viewModel = HomeViewModel(
            shortcutRepository = FakeShortcutStore(),
            settingsRepository = settings,
            noteRepository = FakeNoteStore(),
            persistenceDispatcher = dispatcher,
        )
        advanceUntilIdle()
        settings.saved.clear()

        viewModel.setWallpaperDimPercent(10)
        viewModel.setWallpaperDimPercent(20)
        viewModel.setWallpaperDimPercent(30)
        advanceTimeBy(199)
        assertEquals(emptyList<LauncherSettings>(), settings.saved)

        advanceUntilIdle()
        assertEquals(listOf(30), settings.saved.map { it.wallpaperDimPercent })
    }

    @Test
    fun shortcutAndNoteMutations_arePersistedFromStateChanges() = runTest(dispatcher) {
        val shortcuts = FakeShortcutStore()
        val notes = FakeNoteStore()
        val viewModel = HomeViewModel(
            shortcutRepository = shortcuts,
            settingsRepository = FakeSettingsStore(),
            noteRepository = notes,
            persistenceDispatcher = dispatcher,
        )
        advanceUntilIdle()
        shortcuts.saved.clear()
        notes.saved.clear()

        viewModel.addShortcut(AppShortcut("Maps", "maps.package", "MapsActivity"))
        viewModel.addNote("remember this")
        advanceUntilIdle()

        assertEquals("maps.package", shortcuts.saved.single().single().packageName)
        assertEquals("remember this", notes.saved.single().notes.single().text)
    }

    private class FakeShortcutStore : ShortcutStore {
        val saved = mutableListOf<List<AppShortcut>>()
        override fun loadShortcuts(): List<AppShortcut> = emptyList()
        override fun saveShortcuts(shortcuts: List<AppShortcut>) {
            saved += shortcuts
        }
    }

    private class FakeSettingsStore : LauncherSettingsStore {
        val saved = mutableListOf<LauncherSettings>()
        override fun loadSettings(): LauncherSettings = LauncherSettings()
        override fun saveSettings(settings: LauncherSettings) {
            saved += settings
        }
    }

    private class FakeNoteStore : NoteStore {
        val saved = mutableListOf<NoteStoreState>()
        override fun loadState(): NoteStoreState = NoteStoreState()
        override fun saveState(state: NoteStoreState) {
            saved += state
        }
    }
}
