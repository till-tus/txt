package com.example.textlauncher.ui

import androidx.lifecycle.ViewModel
import com.example.textlauncher.data.LauncherSettingsRepository
import com.example.textlauncher.data.NoteRepository
import com.example.textlauncher.data.NoteStoreState
import com.example.textlauncher.data.ShortcutRepository
import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.ClockDisplayMode
import com.example.textlauncher.domain.GestureAction
import com.example.textlauncher.domain.LauncherGesture
import com.example.textlauncher.domain.LauncherSettings
import com.example.textlauncher.domain.QuickNote
import com.example.textlauncher.domain.ShortcutTextAlignment
import com.example.textlauncher.domain.TrashedNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel(
    private val shortcutRepository: ShortcutRepository,
    private val settingsRepository: LauncherSettingsRepository,
    private val noteRepository: NoteRepository,
) : ViewModel() {
    private val initialSettings = settingsRepository.loadSettings()
    private val initialNoteState = noteRepository.loadState()
    private val _uiState = MutableStateFlow(
        HomeUiState(
            shortcuts = shortcutRepository.loadShortcuts(),
            notes = initialNoteState.notes,
            trashedNotes = initialNoteState.trash,
            showDate = initialSettings.showDate,
            clockDisplayMode = initialSettings.clockDisplayMode,
            showQuickAccess = initialSettings.showQuickAccess,
            wallpaperDimPercent = initialSettings.wallpaperDimPercent,
            shortcutTextAlignment = initialSettings.shortcutTextAlignment,
            maxShortcuts = initialSettings.maxShortcuts,
            openAppListKeyboardAutomatically = initialSettings.openAppListKeyboardAutomatically,
            openScreenTimeGesture = initialSettings.openScreenTimeGesture,
            lockScreenGesture = initialSettings.lockScreenGesture,
            showNotesPage = initialSettings.showNotesPage,
            showCalendarPage = initialSettings.showCalendarPage,
            showTodayPage = initialSettings.showTodayPage,
            selectedCalendarIds = initialSettings.selectedCalendarIds,
            blockedAppPackageNames = initialSettings.blockedAppPackageNames,
            appBudgetMinutesByPackage = initialSettings.appBudgetMinutesByPackage,
            hasRequestedCalendarPermission = initialSettings.hasRequestedCalendarPermission,
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun addShortcut(shortcut: AppShortcut) {
        _uiState.update { state ->
            if (state.shortcuts.size >= state.maxShortcuts) return@update state

            val updated = state.shortcuts + shortcut
            shortcutRepository.saveShortcuts(updated)
            state.copy(shortcuts = updated)
        }
    }

    fun canAddShortcut(): Boolean {
        return _uiState.value.shortcuts.size < _uiState.value.maxShortcuts
    }

    fun deleteShortcut(shortcut: AppShortcut) {
        _uiState.update { state ->
            val updated = state.shortcuts.toMutableList()
            if (!updated.remove(shortcut)) return@update state

            shortcutRepository.saveShortcuts(updated)
            state.copy(shortcuts = updated)
        }
    }

    fun deleteShortcutsForPackage(packageName: String) {
        _uiState.update { state ->
            val updated = state.shortcuts.filterNot { it.packageName == packageName }
            if (updated.size == state.shortcuts.size) return@update state

            shortcutRepository.saveShortcuts(updated)
            state.copy(shortcuts = updated)
        }
    }

    fun moveShortcut(fromPosition: Int, toPosition: Int) {
        _uiState.update { state ->
            val updated = state.shortcuts.toMutableList()
            if (fromPosition !in updated.indices || toPosition !in updated.indices) return@update state

            val moved = updated.removeAt(fromPosition)
            updated.add(toPosition, moved)
            shortcutRepository.saveShortcuts(updated)
            state.copy(shortcuts = updated)
        }
    }

    fun setShowDate(showDate: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(showDate = showDate)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setClockDisplayMode(clockDisplayMode: ClockDisplayMode) {
        _uiState.update { state ->
            val updated = state.copy(clockDisplayMode = clockDisplayMode)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setShowQuickAccess(showQuickAccess: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(showQuickAccess = showQuickAccess)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setWallpaperDimPercent(wallpaperDimPercent: Int) {
        _uiState.update { state ->
            val updated = state.copy(wallpaperDimPercent = wallpaperDimPercent.coerceIn(0, 100))
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setShortcutTextAlignment(shortcutTextAlignment: ShortcutTextAlignment) {
        _uiState.update { state ->
            val updated = state.copy(shortcutTextAlignment = shortcutTextAlignment)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setMaxShortcuts(maxShortcuts: Int) {
        _uiState.update { state ->
            val updated = state.copy(maxShortcuts = maxShortcuts.coerceIn(3, 7))
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setOpenAppListKeyboardAutomatically(openAutomatically: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(openAppListKeyboardAutomatically = openAutomatically)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setShowNotesPage(showNotesPage: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(showNotesPage = showNotesPage)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setGesture(action: GestureAction, gesture: LauncherGesture) {
        _uiState.update { state ->
            val updated = when (action) {
                GestureAction.OpenScreenTime -> state.copy(
                    openScreenTimeGesture = gesture,
                    lockScreenGesture = state.lockScreenGesture.clearIfConflicting(gesture),
                )
                GestureAction.LockScreen -> state.copy(
                    openScreenTimeGesture = state.openScreenTimeGesture.clearIfConflicting(gesture),
                    lockScreenGesture = gesture,
                )
            }
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    private fun LauncherGesture.clearIfConflicting(selectedGesture: LauncherGesture): LauncherGesture {
        return if (selectedGesture != LauncherGesture.None && this == selectedGesture) {
            LauncherGesture.None
        } else {
            this
        }
    }

    fun setShowCalendarPage(showCalendarPage: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(showCalendarPage = showCalendarPage)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setShowTodayPage(showTodayPage: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(showTodayPage = showTodayPage)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setCalendarSelected(calendarId: Long, isSelected: Boolean) {
        _uiState.update { state ->
            val updatedIds = if (isSelected) {
                state.selectedCalendarIds + calendarId
            } else {
                state.selectedCalendarIds - calendarId
            }
            val updated = state.copy(selectedCalendarIds = updatedIds)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setAppBlocked(packageName: String, isBlocked: Boolean) {
        _uiState.update { state ->
            val updatedPackageNames = if (isBlocked) {
                state.blockedAppPackageNames + packageName
            } else {
                state.blockedAppPackageNames - packageName
            }
            val updated = state.copy(blockedAppPackageNames = updatedPackageNames)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun setAppBudget(packageName: String, minutes: Int?) {
        _uiState.update { state ->
            val updatedBudgets = state.appBudgetMinutesByPackage.toMutableMap()
            if (minutes == null) {
                updatedBudgets.remove(packageName)
            } else {
                updatedBudgets[packageName] = minutes
            }
            val updated = state.copy(appBudgetMinutesByPackage = updatedBudgets)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun markCalendarPermissionRequested() {
        _uiState.update { state ->
            if (state.hasRequestedCalendarPermission) return@update state

            val updated = state.copy(hasRequestedCalendarPermission = true)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    fun addNote(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        _uiState.update { state ->
            val updatedNotes = state.notes + QuickNote(
                id = state.nextNoteId(),
                text = trimmedText,
            )
            val sortedNotes = updatedNotes.sortedForNotesPage()
            noteRepository.saveState(
                NoteStoreState(notes = sortedNotes, trash = state.trashedNotes),
            )
            state.copy(notes = sortedNotes)
        }
    }

    fun addVoiceNote(audioFileName: String, durationMillis: Long, waveform: List<Int>) {
        if (audioFileName.isBlank() || durationMillis <= 0L) return

        _uiState.update { state ->
            val updatedNotes = state.notes + QuickNote(
                id = state.nextNoteId(),
                text = "",
                audioFileName = audioFileName,
                audioDurationMillis = durationMillis,
                audioWaveform = waveform,
            )
            val sortedNotes = updatedNotes.sortedForNotesPage()
            noteRepository.saveState(
                NoteStoreState(notes = sortedNotes, trash = state.trashedNotes),
            )
            state.copy(notes = sortedNotes)
        }
    }

    fun updateNote(note: QuickNote, text: String): TrashedNote? {
        if (note.audioFileName != null) return null
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            return deleteNote(note)
        }

        _uiState.update { state ->
            val updatedNotes = state.notes.map { currentNote ->
                if (currentNote.id == note.id) currentNote.copy(text = trimmedText) else currentNote
            }
            val sortedNotes = updatedNotes.sortedForNotesPage()
            noteRepository.saveState(
                NoteStoreState(notes = sortedNotes, trash = state.trashedNotes),
            )
            state.copy(notes = sortedNotes)
        }
        return null
    }

    fun deleteNote(note: QuickNote, deletedAtMillis: Long = System.currentTimeMillis()): TrashedNote? {
        var deletedNote: TrashedNote? = null
        _uiState.update { state ->
            val currentStoreState = NoteStoreState(
                notes = state.notes,
                trash = state.trashedNotes,
            )
            val updatedStoreState = currentStoreState.moveToTrash(note.id, deletedAtMillis)
            if (updatedStoreState === currentStoreState) return@update state

            deletedNote = updatedStoreState.trash.firstOrNull { it.note.id == note.id }
            noteRepository.saveState(updatedStoreState)
            state.copy(
                notes = updatedStoreState.notes,
                trashedNotes = updatedStoreState.trash,
            )
        }
        return deletedNote
    }

    fun restoreNote(noteId: Long): Boolean {
        var didRestore = false
        _uiState.update { state ->
            val currentStoreState = NoteStoreState(
                notes = state.notes,
                trash = state.trashedNotes,
            )
            val updatedStoreState = currentStoreState.restoreFromTrash(noteId)
            if (updatedStoreState === currentStoreState) return@update state

            didRestore = true
            noteRepository.saveState(updatedStoreState)
            state.copy(
                notes = updatedStoreState.notes,
                trashedNotes = updatedStoreState.trash,
            )
        }
        return didRestore
    }

    fun permanentlyDeleteNote(noteId: Long): QuickNote? {
        var deletedNote: QuickNote? = null
        _uiState.update { state ->
            val currentStoreState = NoteStoreState(
                notes = state.notes,
                trash = state.trashedNotes,
            )
            deletedNote = currentStoreState.trash.firstOrNull { it.note.id == noteId }?.note
                ?: return@update state
            val updatedStoreState = currentStoreState.permanentlyDeleteFromTrash(noteId)
            noteRepository.saveState(updatedStoreState)
            state.copy(trashedNotes = updatedStoreState.trash)
        }
        return deletedNote
    }

    fun setNotePinned(note: QuickNote, isPinned: Boolean) {
        _uiState.update { state ->
            if (state.notes.none { it.id == note.id }) return@update state
            val updatedNotes = state.notes.map { currentNote ->
                when {
                    currentNote.id == note.id -> currentNote.copy(isPinned = isPinned)
                    isPinned -> currentNote.copy(isPinned = false)
                    else -> currentNote
                }
            }.sortedForNotesPage()
            noteRepository.saveState(
                NoteStoreState(notes = updatedNotes, trash = state.trashedNotes),
            )
            state.copy(notes = updatedNotes)
        }
    }

    fun toggleClockDisplayMode() {
        _uiState.update { state ->
            val updatedMode = when (state.clockDisplayMode) {
                ClockDisplayMode.Analog -> ClockDisplayMode.Digital
                ClockDisplayMode.Digital -> ClockDisplayMode.Analog
            }
            val updated = state.copy(clockDisplayMode = updatedMode)
            settingsRepository.saveSettings(updated.toSettings())
            updated
        }
    }

    private fun HomeUiState.toSettings(): LauncherSettings {
        return LauncherSettings(
            showDate = showDate,
            clockDisplayMode = clockDisplayMode,
            showQuickAccess = showQuickAccess,
            wallpaperDimPercent = wallpaperDimPercent,
            shortcutTextAlignment = shortcutTextAlignment,
            maxShortcuts = maxShortcuts,
            openAppListKeyboardAutomatically = openAppListKeyboardAutomatically,
            openScreenTimeGesture = openScreenTimeGesture,
            lockScreenGesture = lockScreenGesture,
            showNotesPage = showNotesPage,
            showCalendarPage = showCalendarPage,
            showTodayPage = showTodayPage,
            selectedCalendarIds = selectedCalendarIds,
            blockedAppPackageNames = blockedAppPackageNames,
            appBudgetMinutesByPackage = appBudgetMinutesByPackage,
            hasRequestedCalendarPermission = hasRequestedCalendarPermission,
        )
    }

    private fun List<QuickNote>.sortedForNotesPage(): List<QuickNote> {
        return sortedWith(compareByDescending<QuickNote> { it.isPinned }.thenBy { it.id })
    }

    private fun HomeUiState.nextNoteId(): Long {
        val greatestStoredId = (notes.asSequence().map { it.id } +
            trashedNotes.asSequence().map { it.note.id })
            .maxOrNull()
            ?: Long.MIN_VALUE
        return maxOf(System.currentTimeMillis(), greatestStoredId + 1L)
    }
}
