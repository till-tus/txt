package com.example.textlauncher.ui

import com.example.textlauncher.data.LauncherSettingsStore
import com.example.textlauncher.data.NoteStore
import com.example.textlauncher.data.NoteStoreState
import com.example.textlauncher.data.ShortcutStore
import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.FocusMode
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
    fun pendingSettings_areFlushedBeforeDebounceCompletes() = runTest(dispatcher) {
        val settings = FakeSettingsStore()
        val viewModel = HomeViewModel(
            shortcutRepository = FakeShortcutStore(),
            settingsRepository = settings,
            noteRepository = FakeNoteStore(),
            persistenceDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.setAppBlocked("blocked.package", isBlocked = true)
        advanceTimeBy(199)
        assertEquals(emptyList<LauncherSettings>(), settings.saved)

        viewModel.flushPendingSettings()
        assertEquals(
            listOf(setOf("blocked.package")),
            settings.saved.map { it.blockedAppPackageNames },
        )

        advanceUntilIdle()
        assertEquals(1, settings.saved.size)
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

    @Test
    fun activeFocusMode_usesItsShortcutsAndAddsRestrictionsToGlobalSettings() = runTest(dispatcher) {
        val standardShortcut = AppShortcut("Phone", "phone.app", "PhoneActivity")
        val focusShortcut = AppShortcut("Docs", "docs.app", "DocsActivity")
        val focusMode = FocusMode(
            id = "focus",
            name = "Focus",
            blockedAppPackageNames = setOf("social.app"),
            appBudgetMinutesByPackage = mapOf("mail.app" to 15),
            shortcuts = listOf(focusShortcut),
        )
        val settings = FakeSettingsStore(
            LauncherSettings(
                blockedAppPackageNames = setOf("games.app"),
                appBudgetMinutesByPackage = mapOf("mail.app" to 30, "video.app" to 60),
                focusModesEnabled = true,
                focusModes = listOf(focusMode),
                manuallyActiveFocusModeId = focusMode.id,
            ),
        )
        val shortcuts = FakeShortcutStore(listOf(standardShortcut))
        val viewModel = HomeViewModel(
            shortcutRepository = shortcuts,
            settingsRepository = settings,
            noteRepository = FakeNoteStore(),
            persistenceDispatcher = dispatcher,
        )

        val state = viewModel.uiState.value
        assertEquals(listOf(focusShortcut), state.visibleShortcuts)
        assertEquals(setOf("games.app", "social.app"), state.effectiveBlockedAppPackageNames)
        assertEquals(mapOf("mail.app" to 15, "video.app" to 60), state.effectiveAppBudgetMinutesByPackage)

        viewModel.addShortcut(AppShortcut("Calendar", "calendar.app", "CalendarActivity"))
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.activeFocusMode?.mode?.shortcuts?.size)
        assertEquals(listOf(standardShortcut), viewModel.uiState.value.shortcuts)
    }

    private class FakeShortcutStore(private val initial: List<AppShortcut> = emptyList()) : ShortcutStore {
        val saved = mutableListOf<List<AppShortcut>>()
        override fun loadShortcuts(): List<AppShortcut> = initial
        override fun saveShortcuts(shortcuts: List<AppShortcut>) {
            saved += shortcuts
        }
    }

    private class FakeSettingsStore(private val initial: LauncherSettings = LauncherSettings()) : LauncherSettingsStore {
        val saved = mutableListOf<LauncherSettings>()
        override fun loadSettings(): LauncherSettings = initial
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
