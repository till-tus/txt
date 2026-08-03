package com.example.textlauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.textlauncher.data.LauncherSettingsStore
import com.example.textlauncher.data.NoteStore
import com.example.textlauncher.data.NoteStoreState
import com.example.textlauncher.data.ShortcutStore
import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.ClockDisplayMode
import com.example.textlauncher.domain.GestureAction
import com.example.textlauncher.domain.LauncherGesture
import com.example.textlauncher.domain.LauncherSettings
import com.example.textlauncher.domain.PageArrangement
import com.example.textlauncher.domain.QuickNote
import com.example.textlauncher.domain.QuickAccessPosition
import com.example.textlauncher.domain.QuickAccessTarget
import com.example.textlauncher.domain.ShortcutTextAlignment
import com.example.textlauncher.domain.TrashedNote
import com.example.textlauncher.domain.sortedForNotesPage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val shortcutRepository: ShortcutStore,
    private val settingsRepository: LauncherSettingsStore,
    private val noteRepository: NoteStore,
    persistenceDispatcher: CoroutineDispatcher = Dispatchers.IO,
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
            leftQuickAccess = initialSettings.leftQuickAccess,
            rightQuickAccess = initialSettings.rightQuickAccess,
            quickAccessPosition = initialSettings.quickAccessPosition,
            wallpaperDimPercent = initialSettings.wallpaperDimPercent,
            shortcutTextAlignment = initialSettings.shortcutTextAlignment,
            maxShortcuts = initialSettings.maxShortcuts,
            openAppListKeyboardAutomatically = initialSettings.openAppListKeyboardAutomatically,
            universalCommandPaletteEnabled = initialSettings.universalCommandPaletteEnabled,
            openScreenTimeGesture = initialSettings.openScreenTimeGesture,
            lockScreenGesture = initialSettings.lockScreenGesture,
            showNotesPage = initialSettings.showNotesPage,
            showCalendarPage = initialSettings.showCalendarPage,
            showTodayPage = initialSettings.showTodayPage,
            pageArrangement = initialSettings.pageArrangement,
            selectedCalendarIds = initialSettings.selectedCalendarIds,
            blockedAppPackageNames = initialSettings.blockedAppPackageNames,
            appBudgetMinutesByPackage = initialSettings.appBudgetMinutesByPackage,
            excludedScreenTimePackageNames = initialSettings.excludedScreenTimePackageNames,
            hasRequestedCalendarPermission = initialSettings.hasRequestedCalendarPermission,
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val settingsPersistenceLock = Any()
    private var lastPersistedSettings = initialSettings

    init {
        uiState
            .map { it.shortcuts }
            .distinctUntilChanged()
            .onEach(shortcutRepository::saveShortcuts)
            .flowOn(persistenceDispatcher)
            .launchIn(viewModelScope)
        uiState
            .map { it.toSettings() }
            .distinctUntilChanged()
            .debounce(SETTINGS_PERSISTENCE_DEBOUNCE_MS)
            .onEach(::persistSettings)
            .flowOn(persistenceDispatcher)
            .launchIn(viewModelScope)
        uiState
            .map { it.toNoteStoreState() }
            .distinctUntilChanged()
            .onEach(noteRepository::saveState)
            .flowOn(persistenceDispatcher)
            .launchIn(viewModelScope)
    }

    internal fun flushPendingSettings() {
        persistSettings(_uiState.value.toSettings())
    }

    override fun onCleared() {
        flushPendingSettings()
    }

    private fun persistSettings(settings: LauncherSettings) {
        synchronized(settingsPersistenceLock) {
            if (settings == lastPersistedSettings) return
            settingsRepository.saveSettings(settings)
            lastPersistedSettings = settings
        }
    }

    fun addShortcut(shortcut: AppShortcut) {
        _uiState.update { state ->
            if (state.shortcuts.size >= state.maxShortcuts) return@update state

            val updated = state.shortcuts + shortcut
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

            state.copy(shortcuts = updated)
        }
    }

    fun deleteShortcutsForPackage(packageName: String) {
        _uiState.update { state ->
            val updated = state.shortcuts.filterNot { it.packageName == packageName }
            if (updated.size == state.shortcuts.size) return@update state

            state.copy(shortcuts = updated)
        }
    }

    fun moveShortcut(fromPosition: Int, toPosition: Int) {
        _uiState.update { state ->
            val updated = state.shortcuts.toMutableList()
            if (fromPosition !in updated.indices || toPosition !in updated.indices) return@update state

            val moved = updated.removeAt(fromPosition)
            updated.add(toPosition, moved)
            state.copy(shortcuts = updated)
        }
    }

    fun setShowDate(showDate: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(showDate = showDate)
            updated
        }
    }

    fun setClockDisplayMode(clockDisplayMode: ClockDisplayMode) {
        _uiState.update { state ->
            val updated = state.copy(clockDisplayMode = clockDisplayMode)
            updated
        }
    }

    fun setQuickAccess(left: Boolean, target: QuickAccessTarget?) {
        _uiState.update { state ->
            val updated = if (left) {
                state.copy(leftQuickAccess = target)
            } else {
                state.copy(rightQuickAccess = target)
            }
            updated
        }
    }

    fun setQuickAccessPosition(position: QuickAccessPosition) {
        _uiState.update { state ->
            val updated = state.copy(quickAccessPosition = position)
            updated
        }
    }

    fun clearQuickAccessForPackage(packageName: String) {
        _uiState.update { state ->
            val updated = state.copy(
                leftQuickAccess = state.leftQuickAccess?.takeUnless { it.packageName == packageName },
                rightQuickAccess = state.rightQuickAccess?.takeUnless { it.packageName == packageName },
            )
            if (updated == state) return@update state
            updated
        }
    }

    fun setPageArrangement(pageArrangement: PageArrangement) {
        if (!pageArrangement.isValid()) return
        _uiState.update { state ->
            val updated = state.copy(pageArrangement = pageArrangement)
            updated
        }
    }

    fun setWallpaperDimPercent(wallpaperDimPercent: Int) {
        _uiState.update { state ->
            val updated = state.copy(wallpaperDimPercent = wallpaperDimPercent.coerceIn(0, 100))
            updated
        }
    }

    fun setShortcutTextAlignment(shortcutTextAlignment: ShortcutTextAlignment) {
        _uiState.update { state ->
            val updated = state.copy(shortcutTextAlignment = shortcutTextAlignment)
            updated
        }
    }

    fun setMaxShortcuts(maxShortcuts: Int) {
        _uiState.update { state ->
            val updated = state.copy(maxShortcuts = maxShortcuts.coerceIn(3, 7))
            updated
        }
    }

    fun setOpenAppListKeyboardAutomatically(openAutomatically: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(openAppListKeyboardAutomatically = openAutomatically)
            updated
        }
    }

    fun setUniversalCommandPaletteEnabled(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(universalCommandPaletteEnabled = enabled)
        }
    }

    fun setShowNotesPage(showNotesPage: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(showNotesPage = showNotesPage)
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
            updated
        }
    }

    fun setShowTodayPage(showTodayPage: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(showTodayPage = showTodayPage)
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
            updated
        }
    }

    fun setScreenTimeAppExcluded(packageName: String, isExcluded: Boolean) {
        _uiState.update { state ->
            val updatedPackageNames = if (isExcluded) {
                state.excludedScreenTimePackageNames + packageName
            } else {
                state.excludedScreenTimePackageNames - packageName
            }
            val updated = state.copy(excludedScreenTimePackageNames = updatedPackageNames)
            updated
        }
    }

    fun removePackageReferences(packageName: String) {
        _uiState.update { state ->
            val updated = state.copy(
                blockedAppPackageNames = state.blockedAppPackageNames - packageName,
                appBudgetMinutesByPackage = state.appBudgetMinutesByPackage - packageName,
                excludedScreenTimePackageNames = state.excludedScreenTimePackageNames - packageName,
                leftQuickAccess = state.leftQuickAccess?.takeUnless { it.packageName == packageName },
                rightQuickAccess = state.rightQuickAccess?.takeUnless { it.packageName == packageName },
            )
            if (updated == state) return@update state
            updated
        }
    }

    fun markCalendarPermissionRequested() {
        _uiState.update { state ->
            if (state.hasRequestedCalendarPermission) return@update state

            val updated = state.copy(hasRequestedCalendarPermission = true)
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
            updated
        }
    }

    private fun HomeUiState.toSettings(): LauncherSettings {
        return LauncherSettings(
            showDate = showDate,
            clockDisplayMode = clockDisplayMode,
            leftQuickAccess = leftQuickAccess,
            rightQuickAccess = rightQuickAccess,
            quickAccessPosition = quickAccessPosition,
            wallpaperDimPercent = wallpaperDimPercent,
            shortcutTextAlignment = shortcutTextAlignment,
            maxShortcuts = maxShortcuts,
            openAppListKeyboardAutomatically = openAppListKeyboardAutomatically,
            universalCommandPaletteEnabled = universalCommandPaletteEnabled,
            openScreenTimeGesture = openScreenTimeGesture,
            lockScreenGesture = lockScreenGesture,
            showNotesPage = showNotesPage,
            showCalendarPage = showCalendarPage,
            showTodayPage = showTodayPage,
            pageArrangement = pageArrangement,
            selectedCalendarIds = selectedCalendarIds,
            blockedAppPackageNames = blockedAppPackageNames,
            appBudgetMinutesByPackage = appBudgetMinutesByPackage,
            excludedScreenTimePackageNames = excludedScreenTimePackageNames,
            hasRequestedCalendarPermission = hasRequestedCalendarPermission,
        )
    }

    private fun HomeUiState.toNoteStoreState(): NoteStoreState {
        return NoteStoreState(notes = notes, trash = trashedNotes)
    }

    private fun HomeUiState.nextNoteId(): Long {
        val greatestStoredId = (notes.asSequence().map { it.id } +
            trashedNotes.asSequence().map { it.note.id })
            .maxOrNull()
            ?: Long.MIN_VALUE
        return maxOf(System.currentTimeMillis(), greatestStoredId + 1L)
    }

    private companion object {
        const val SETTINGS_PERSISTENCE_DEBOUNCE_MS = 200L
    }
}
