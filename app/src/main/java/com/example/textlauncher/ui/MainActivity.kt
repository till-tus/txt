package com.example.textlauncher.ui

import android.animation.ValueAnimator
import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.CountDownTimer
import android.os.Process
import android.provider.MediaStore
import android.provider.CalendarContract
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.textlauncher.R
import com.example.textlauncher.data.AppUsageIntentionRepository
import com.example.textlauncher.data.CalendarRepository
import com.example.textlauncher.data.InstalledAppsRepository
import com.example.textlauncher.data.LauncherSettingsRepository
import com.example.textlauncher.data.NoteRepository
import com.example.textlauncher.data.ScreenTimeRepository
import com.example.textlauncher.data.ShortcutRepository
import com.example.textlauncher.databinding.ActivityMainBinding
import com.example.textlauncher.databinding.ItemAppBudgetSelectionBinding
import com.example.textlauncher.databinding.ItemAppBlockSelectionBinding
import com.example.textlauncher.databinding.ItemCalendarSelectionBinding
import com.example.textlauncher.domain.ClockDisplayMode
import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.CalendarEvent
import com.example.textlauncher.domain.DeviceCalendar
import com.example.textlauncher.domain.QuickNote
import com.example.textlauncher.domain.ScreenTimeAppUsage
import com.example.textlauncher.domain.ScreenTimeDayUsage
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var shortcutAdapter: ShortcutAdapter
    private lateinit var appPickerAdapter: AppPickerAdapter
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var calendarEventAdapter: CalendarEventAdapter
    private lateinit var screenTimeAdapter: ScreenTimeAdapter
    private var shouldHandleBlankAreaLongPress = false
    private var isAppPickerVisible = false
    private var isEditMode = false
    private var isSettingsVisible = false
    private var isNotesVisible = false
    private var isCalendarVisible = false
    private var isScreenTimeVisible = false
    private var isNoteEditorVisible = false
    private var isScreenTimeExpanded = false
    private var isScreenTimeIntentionsExpanded = false
    private var appListMode = AppListMode.AddShortcut
    private var availableApps = emptyList<AppShortcut>()
    private var screenTimeUsages = emptyList<ScreenTimeAppUsage>()
    private var screenTimeWeekUsages = emptyList<ScreenTimeDayUsage>()
    private var blockableApps = emptyList<AppShortcut>()
    private var currentBlockedAppPackageNames = emptySet<String>()
    private var currentAppBudgetMinutesByPackage = emptyMap<String, Int>()
    private var editingNote: QuickNote? = null
    private var pendingBlockedShortcut: AppShortcut? = null
    private var selectedAppIntentMinutes: Int? = null
    private var isAppBlockCountdownComplete = false
    private var pageSwipeStartX = 0f
    private var pageSwipeStartY = 0f
    private var activePageSwipeTarget: PageSwipeTarget? = null
    private var pageSwipeVelocityTracker: VelocityTracker? = null
    private var didCancelPageSwipeChildren = false
    private var screenTimeGestureStartX = 0f
    private var screenTimeGestureStartY = 0f
    private var isTrackingScreenTimeGesture = false
    private var hasTriggeredScreenTimeGesture = false
    private var isFormattingNoteBullets = false
    private var availableCalendars = emptyList<DeviceCalendar>()
    private var currentSelectedCalendarIds = emptySet<Long>()
    private var hasRequestedCalendarPermission = false
    private var isCalendarSelectionExpanded = false
    private var isAppBlockingExpanded = false
    private var isAppBudgetsExpanded = false
    private var isRenderingSettingsState = false
    private var wasSettingsImeVisible = false
    private var editModePulseAnimator: ValueAnimator? = null
    private var appBlockCountdownTimer: CountDownTimer? = null
    private var renderedShortcutCount = 0
    private val installedAppsRepository by lazy { InstalledAppsRepository(applicationContext) }
    private val appUsageIntentionRepository by lazy { AppUsageIntentionRepository(applicationContext) }
    private val calendarRepository by lazy { CalendarRepository(applicationContext) }
    private val screenTimeRepository by lazy { ScreenTimeRepository(applicationContext) }
    private val requestCalendarPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        renderCalendarPermissionState()
        if (isGranted) {
            refreshCalendars()
            refreshCalendarEvents()
        }
    }
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            ShortcutRepository(applicationContext),
            LauncherSettingsRepository(applicationContext),
            NoteRepository(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shortcutAdapter = ShortcutAdapter(::handleShortcutClick, ::showShortcutContextMenu)
        binding.shortcutList.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.shortcutList.adapter = shortcutAdapter

        appPickerAdapter = AppPickerAdapter(
            onAppClick = { shortcut ->
                when (appListMode) {
                    AppListMode.AddShortcut -> {
                        if (viewModel.canAddShortcut()) {
                            viewModel.addShortcut(shortcut)
                            hideAppPicker()
                        } else {
                            showShortcutLimitReached()
                        }
                    }
                    AppListMode.LaunchApp -> {
                        hideAppPicker()
                        launchShortcutWithAppBlocking(shortcut)
                    }
                }
            },
            onAppLongClick = { anchor, shortcut ->
                if (appListMode == AppListMode.LaunchApp) {
                    showAppBlockContextMenu(anchor, shortcut)
                }
            },
        )
        binding.appPickerList.layoutManager = LinearLayoutManager(this)
        binding.appPickerList.adapter = appPickerAdapter

        noteAdapter = NoteAdapter(::showNoteEditor, ::copyNote, ::showNoteContextMenu)
        binding.notesList.layoutManager = LinearLayoutManager(this)
        binding.notesList.adapter = noteAdapter

        calendarEventAdapter = CalendarEventAdapter(::openCalendarEventDay)
        binding.calendarEventList.layoutManager = LinearLayoutManager(this)
        binding.calendarEventList.adapter = calendarEventAdapter

        screenTimeAdapter = ScreenTimeAdapter()
        binding.screenTimeList.layoutManager = LinearLayoutManager(this)
        binding.screenTimeList.adapter = screenTimeAdapter

        configureSystemInsets()
        bindCurrentDate()
        configureAppSearch()
        configureSettings()
        configureEditControls()
        configureQuickAccess()
        configureNotes()
        configureCalendar()
        configureScreenTime()
        configureShortcutReordering()
        configureHomeLongPress()
        configureBackNavigation()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderHomeState(state)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasCalendarPermission()) {
            refreshCalendars()
            if (isCalendarVisible) {
                refreshCalendarEvents()
            }
        } else {
            renderCalendarPermissionState()
        }
        if (isScreenTimeVisible) {
            refreshScreenTime()
        }
    }

    override fun onDestroy() {
        appBlockCountdownTimer?.cancel()
        super.onDestroy()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (handleScreenTimeGesture(event)) {
            return true
        }
        if (handlePageDrag(event)) {
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    private fun handleScreenTimeGesture(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == SCREEN_TIME_GESTURE_POINTERS && canHandleScreenTimeGesture()) {
                    screenTimeGestureStartX = averagePointerX(event)
                    screenTimeGestureStartY = averagePointerY(event)
                    isTrackingScreenTimeGesture = true
                    hasTriggeredScreenTimeGesture = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isTrackingScreenTimeGesture || event.pointerCount < SCREEN_TIME_GESTURE_POINTERS) {
                    return false
                }
                val deltaX = averagePointerX(event) - screenTimeGestureStartX
                val deltaY = averagePointerY(event) - screenTimeGestureStartY
                val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
                val isVerticalDrag = deltaY > touchSlop && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) * PAGE_SWIPE_AXIS_RATIO
                if (!isVerticalDrag) {
                    return false
                }
                if (!hasTriggeredScreenTimeGesture && deltaY > SCREEN_TIME_SWIPE_DOWN_DISTANCE_DP.dp) {
                    hasTriggeredScreenTimeGesture = true
                    showScreenTimePage()
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                val wasTracking = isTrackingScreenTimeGesture
                isTrackingScreenTimeGesture = false
                hasTriggeredScreenTimeGesture = false
                return wasTracking && event.actionMasked != MotionEvent.ACTION_POINTER_UP
            }
        }
        return false
    }

    private fun canHandleScreenTimeGesture(): Boolean {
        return !isAppPickerVisible &&
            !isSettingsVisible &&
            !isNotesVisible &&
            !isCalendarVisible &&
            !isScreenTimeVisible &&
            !isNoteEditorVisible &&
            !isEditMode &&
            binding.showScreenTimePageSwitch.isChecked
    }

    private fun averagePointerX(event: MotionEvent): Float {
        return (0 until event.pointerCount).sumOf { event.getX(it).toDouble() }.toFloat() / event.pointerCount
    }

    private fun averagePointerY(event: MotionEvent): Float {
        return (0 until event.pointerCount).sumOf { event.getY(it).toDouble() }.toFloat() / event.pointerCount
    }

    private fun handlePageDrag(event: MotionEvent): Boolean {
        if (!canHandlePageDrag()) {
            cancelPageDrag()
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pageSwipeStartX = event.rawX
                pageSwipeStartY = event.rawY
                activePageSwipeTarget = null
                didCancelPageSwipeChildren = false
                pageSwipeVelocityTracker?.recycle()
                pageSwipeVelocityTracker = VelocityTracker.obtain().apply {
                    addMovement(event)
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                pageSwipeVelocityTracker?.addMovement(event)
                val deltaX = event.rawX - pageSwipeStartX
                val deltaY = event.rawY - pageSwipeStartY
                val target = activePageSwipeTarget ?: detectPageSwipeTarget(deltaX, deltaY) ?: return false

                if (activePageSwipeTarget == null) {
                    activePageSwipeTarget = target
                    cancelChildGesturesForPageSwipe(event)
                    preparePageDrag(target)
                }
                applyPageDrag(target, deltaX)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val target = activePageSwipeTarget ?: run {
                    cancelPageDrag()
                    return false
                }
                pageSwipeVelocityTracker?.addMovement(event)
                pageSwipeVelocityTracker?.computeCurrentVelocity(1_000)
                val velocityX = pageSwipeVelocityTracker?.xVelocity ?: 0f
                val deltaX = event.rawX - pageSwipeStartX
                settlePageDrag(
                    target = target,
                    shouldComplete = shouldCompletePageDrag(target, deltaX, velocityX),
                )
                cancelPageDrag()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                activePageSwipeTarget?.let { target ->
                    settlePageDrag(target = target, shouldComplete = false)
                }
                cancelPageDrag()
                return false
            }
        }
        return false
    }

    private fun canHandlePageDrag(): Boolean {
        return !isAppPickerVisible && !isSettingsVisible && !isScreenTimeVisible && !isNoteEditorVisible && !isEditMode
    }

    private fun canEnterEditModeFromLongPress(): Boolean {
        return !isAppPickerVisible && !isScreenTimeVisible && activePageSwipeTarget == null
    }

    private fun detectPageSwipeTarget(deltaX: Float, deltaY: Float): PageSwipeTarget? {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        if (kotlin.math.abs(deltaX) < touchSlop || kotlin.math.abs(deltaX) < kotlin.math.abs(deltaY) * PAGE_SWIPE_AXIS_RATIO) {
            return null
        }
        return when {
            !isNotesVisible && !isCalendarVisible && deltaX < 0 && binding.showNotesPageSwitch.isChecked -> {
                PageSwipeTarget.HomeToNotes
            }
            !isNotesVisible && !isCalendarVisible && deltaX > 0 && binding.showCalendarPageSwitch.isChecked -> {
                PageSwipeTarget.HomeToCalendar
            }
            isNotesVisible && deltaX > 0 -> PageSwipeTarget.NotesToHome
            isCalendarVisible && deltaX < 0 -> PageSwipeTarget.CalendarToHome
            else -> null
        }
    }

    private fun preparePageDrag(target: PageSwipeTarget) {
        val width = pageWidth()
        binding.homeContent.animate().cancel()
        binding.notesRoot.animate().cancel()
        binding.calendarRoot.animate().cancel()
        when (target) {
            PageSwipeTarget.HomeToNotes -> {
                binding.notesRoot.translationX = width
                binding.notesRoot.visibility = View.VISIBLE
            }
            PageSwipeTarget.HomeToCalendar -> {
                binding.calendarRoot.translationX = -width
                binding.calendarRoot.visibility = View.VISIBLE
            }
            PageSwipeTarget.NotesToHome -> {
                binding.homeContent.translationX = -width
                binding.homeContent.visibility = View.VISIBLE
            }
            PageSwipeTarget.CalendarToHome -> {
                binding.homeContent.translationX = width
                binding.homeContent.visibility = View.VISIBLE
            }
        }
    }

    private fun applyPageDrag(target: PageSwipeTarget, deltaX: Float) {
        val width = pageWidth()
        when (target) {
            PageSwipeTarget.HomeToNotes -> {
                val drag = deltaX.coerceIn(-width, 0f)
                binding.homeContent.translationX = drag
                binding.notesRoot.translationX = width + drag
            }
            PageSwipeTarget.HomeToCalendar -> {
                val drag = deltaX.coerceIn(0f, width)
                binding.homeContent.translationX = drag
                binding.calendarRoot.translationX = -width + drag
            }
            PageSwipeTarget.NotesToHome -> {
                val drag = deltaX.coerceIn(0f, width)
                binding.notesRoot.translationX = drag
                binding.homeContent.translationX = -width + drag
            }
            PageSwipeTarget.CalendarToHome -> {
                val drag = deltaX.coerceIn(-width, 0f)
                binding.calendarRoot.translationX = drag
                binding.homeContent.translationX = width + drag
            }
        }
    }

    private fun shouldCompletePageDrag(target: PageSwipeTarget, deltaX: Float, velocityX: Float): Boolean {
        val distancePasses = kotlin.math.abs(deltaX) > pageWidth() * PAGE_SWIPE_COMPLETE_FRACTION
        val velocityPasses = when (target) {
            PageSwipeTarget.HomeToNotes,
            PageSwipeTarget.CalendarToHome -> velocityX < -PAGE_SWIPE_COMPLETE_VELOCITY
            PageSwipeTarget.HomeToCalendar,
            PageSwipeTarget.NotesToHome -> velocityX > PAGE_SWIPE_COMPLETE_VELOCITY
        }
        return distancePasses || velocityPasses
    }

    private fun settlePageDrag(target: PageSwipeTarget, shouldComplete: Boolean) {
        val width = pageWidth()
        when (target) {
            PageSwipeTarget.HomeToNotes -> {
                animatePagePair(
                    outgoing = binding.homeContent,
                    incoming = binding.notesRoot,
                    outgoingEnd = if (shouldComplete) -width else 0f,
                    incomingEnd = if (shouldComplete) 0f else width,
                ) {
                    isNotesVisible = shouldComplete
                    if (!shouldComplete) binding.notesRoot.visibility = View.GONE
                }
            }
            PageSwipeTarget.HomeToCalendar -> {
                animatePagePair(
                    outgoing = binding.homeContent,
                    incoming = binding.calendarRoot,
                    outgoingEnd = if (shouldComplete) width else 0f,
                    incomingEnd = if (shouldComplete) 0f else -width,
                ) {
                    isCalendarVisible = shouldComplete
                    if (shouldComplete) {
                        onCalendarPageVisible()
                    } else {
                        binding.calendarRoot.visibility = View.GONE
                    }
                }
            }
            PageSwipeTarget.NotesToHome -> {
                animatePagePair(
                    outgoing = binding.notesRoot,
                    incoming = binding.homeContent,
                    outgoingEnd = if (shouldComplete) width else 0f,
                    incomingEnd = if (shouldComplete) 0f else -width,
                ) {
                    isNotesVisible = !shouldComplete
                    if (shouldComplete) binding.notesRoot.visibility = View.GONE
                }
            }
            PageSwipeTarget.CalendarToHome -> {
                animatePagePair(
                    outgoing = binding.calendarRoot,
                    incoming = binding.homeContent,
                    outgoingEnd = if (shouldComplete) -width else 0f,
                    incomingEnd = if (shouldComplete) 0f else width,
                ) {
                    isCalendarVisible = !shouldComplete
                    if (shouldComplete) binding.calendarRoot.visibility = View.GONE
                }
            }
        }
    }

    private fun animatePagePair(
        outgoing: View,
        incoming: View,
        outgoingEnd: Float,
        incomingEnd: Float,
        onEnd: () -> Unit,
    ) {
        outgoing.animate()
            .translationX(outgoingEnd)
            .setDuration(PAGE_SETTLE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        incoming.animate()
            .translationX(incomingEnd)
            .setDuration(PAGE_SETTLE_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction(onEnd)
            .start()
    }

    private fun cancelPageDrag() {
        activePageSwipeTarget = null
        didCancelPageSwipeChildren = false
        pageSwipeVelocityTracker?.recycle()
        pageSwipeVelocityTracker = null
    }

    private fun cancelChildGesturesForPageSwipe(event: MotionEvent) {
        if (didCancelPageSwipeChildren) return

        didCancelPageSwipeChildren = true
        val cancelEvent = MotionEvent.obtain(event).apply {
            action = MotionEvent.ACTION_CANCEL
        }
        super.dispatchTouchEvent(cancelEvent)
        cancelEvent.recycle()
        binding.homeRoot.cancelLongPress()
        binding.homeContent.cancelLongPress()
        binding.clockView.cancelLongPress()
        binding.shortcutList.cancelLongPress()
    }

    private fun pageWidth(): Float {
        return (binding.homeRoot.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels).toFloat()
    }

    private fun renderHomeState(state: HomeUiState) {
        val shouldScrollToBottom = state.shortcuts.size > renderedShortcutCount
        renderedShortcutCount = state.shortcuts.size
        val visibleShortcuts = state.shortcuts.take(state.maxShortcuts)
        shortcutAdapter.submitList(visibleShortcuts) {
            if (shouldScrollToBottom && visibleShortcuts.isNotEmpty()) {
                binding.shortcutList.scrollToPosition(visibleShortcuts.lastIndex)
            }
        }
        noteAdapter.submitList(state.notes)
        binding.notesList.visibility = if (state.notes.isEmpty()) View.GONE else View.VISIBLE
        binding.notesEmpty.visibility = if (state.notes.isEmpty()) View.VISIBLE else View.GONE
        binding.dateText.visibility = if (state.showDate) View.VISIBLE else View.GONE
        binding.clockView.setDisplayMode(state.clockDisplayMode)
        if (binding.showDateSwitch.isChecked != state.showDate) {
            binding.showDateSwitch.isChecked = state.showDate
        }
        val isDigitalClock = state.clockDisplayMode == ClockDisplayMode.Digital
        if (binding.defaultDigitalClockSwitch.isChecked != isDigitalClock) {
            binding.defaultDigitalClockSwitch.isChecked = isDigitalClock
        }
        if (binding.showQuickAccessSwitch.isChecked != state.showQuickAccess) {
            binding.showQuickAccessSwitch.isChecked = state.showQuickAccess
        }
        if (binding.maxShortcutsSlider.value.toInt() != state.maxShortcuts) {
            binding.maxShortcutsSlider.value = state.maxShortcuts.toFloat()
        }
        binding.maxShortcutsValue.text = state.maxShortcuts.toString()
        val canAddShortcut = state.shortcuts.size < state.maxShortcuts
        binding.addShortcutButton.isEnabled = canAddShortcut
        binding.addShortcutButton.alpha = if (canAddShortcut) 1f else DISABLED_ACTION_ALPHA
        if (binding.showNotesPageSwitch.isChecked != state.showNotesPage) {
            binding.showNotesPageSwitch.isChecked = state.showNotesPage
        }
        isRenderingSettingsState = true
        if (binding.showCalendarPageSwitch.isChecked != state.showCalendarPage) {
            binding.showCalendarPageSwitch.isChecked = state.showCalendarPage
        }
        isRenderingSettingsState = false
        if (binding.showScreenTimePageSwitch.isChecked != state.showScreenTimePage) {
            binding.showScreenTimePageSwitch.isChecked = state.showScreenTimePage
        }
        currentSelectedCalendarIds = state.selectedCalendarIds
        currentBlockedAppPackageNames = state.blockedAppPackageNames
        currentAppBudgetMinutesByPackage = state.appBudgetMinutesByPackage
        hasRequestedCalendarPermission = state.hasRequestedCalendarPermission
        if (!state.showScreenTimePage && isScreenTimeVisible) {
            hideScreenTimePage()
        }
        if (!state.showNotesPage && isNotesVisible) {
            hideNotesPage()
        }
        if (!state.showCalendarPage && isCalendarVisible) {
            hideCalendarPage()
        }
        renderCalendarSelection()
        renderAppBlockingSelection()
        renderAppBudgetsSelection()
        if (isCalendarVisible && hasCalendarPermission()) {
            refreshCalendarEvents()
        }
        val quickAccessVisibility = if (state.showQuickAccess) View.VISIBLE else View.GONE
        if (binding.quickAccessBar.visibility != quickAccessVisibility) {
            binding.quickAccessBar.visibility = quickAccessVisibility
            ViewCompat.requestApplyInsets(binding.homeRoot)
        }
    }

    private fun bindCurrentDate() {
        binding.dateText.text = DateFormat.getDateInstance(DateFormat.FULL).format(Date())
    }

    private fun configureSystemInsets() {
        val appSearchBaseTopPadding = binding.appSearchInput.paddingTop
        val appPickerBaseBottomPadding = binding.appPickerList.paddingBottom
        val shortcutBaseBottomPadding = binding.shortcutList.paddingBottom
        val quickAccessBaseBottomPadding = binding.quickAccessBar.paddingBottom
        val notesListBaseBottomPadding = binding.notesList.paddingBottom
        val calendarEventListBaseBottomPadding = binding.calendarEventList.paddingBottom
        val screenTimeScrollBaseBottomPadding = binding.screenTimeScroll.paddingBottom
        val settingsScrollBaseBottomPadding = binding.settingsScroll.paddingBottom
        val noteEditorBaseTopPadding = binding.noteEditorRoot.paddingTop
        val noteEditorBaseBottomPadding = binding.noteEditorRoot.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.homeRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(systemBars.bottom, ime.bottom)
            binding.homeContent.updatePadding(top = systemBars.top)
            binding.calendarRoot.updatePadding(top = systemBars.top)
            binding.notesRoot.updatePadding(top = systemBars.top)
            binding.screenTimeRoot.updatePadding(top = systemBars.top)
            binding.editControls.updatePadding(top = systemBars.top)
            binding.settingsRoot.updatePadding(top = systemBars.top)
            binding.noteEditorRoot.updatePadding(
                top = noteEditorBaseTopPadding + systemBars.top,
                bottom = noteEditorBaseBottomPadding + systemBars.bottom,
            )
            binding.shortcutList.updatePadding(
                bottom = shortcutBaseBottomPadding +
                    if (binding.quickAccessBar.visibility == View.VISIBLE) 0 else systemBars.bottom,
            )
            binding.quickAccessBar.updatePadding(bottom = quickAccessBaseBottomPadding + systemBars.bottom)
            binding.notesList.updatePadding(bottom = notesListBaseBottomPadding + systemBars.bottom)
            binding.calendarEventList.updatePadding(bottom = calendarEventListBaseBottomPadding + systemBars.bottom)
            binding.screenTimeScroll.updatePadding(bottom = screenTimeScrollBaseBottomPadding + systemBars.bottom)
            binding.settingsScroll.updatePadding(bottom = settingsScrollBaseBottomPadding + bottomInset)
            binding.appSearchInput.updatePadding(top = appSearchBaseTopPadding + systemBars.top)
            binding.appPickerList.updatePadding(bottom = appPickerBaseBottomPadding + systemBars.bottom)
            binding.appPickerEmpty.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom,
            )
            if (isImeVisible && !wasSettingsImeVisible && shouldScrollSettingsForFocusedSearch()) {
                scrollSettingsToFocusedSearch()
            }
            wasSettingsImeVisible = isImeVisible
            insets
        }
    }

    private fun shouldScrollSettingsForFocusedSearch(): Boolean {
        return isSettingsVisible &&
            (
                isAppBlockingExpanded &&
                    binding.appBlockingSearchInput.hasFocus() ||
                    isAppBudgetsExpanded &&
                    binding.appBudgetsSearchInput.hasFocus()
                )
    }

    private fun scrollSettingsToFocusedSearch() {
        binding.settingsScroll.post {
            val focusedSearch = if (binding.appBudgetsSearchInput.hasFocus()) {
                binding.appBudgetsSearchInput
            } else {
                binding.appBlockingSearchInput
            }
            val targetTop = (focusedSearch.top - SETTINGS_KEYBOARD_SCROLL_TOP_OFFSET_DP.dp)
                .coerceAtLeast(0)
            binding.settingsScroll.smoothScrollTo(0, targetTop)
        }
    }

    private fun configureSettings() {
        binding.showDateSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowDate(isChecked)
        }
        binding.showDateRow.setOnClickListener {
            viewModel.setShowDate(!binding.showDateSwitch.isChecked)
        }
        binding.defaultDigitalClockSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setClockDisplayMode(
                if (isChecked) ClockDisplayMode.Digital else ClockDisplayMode.Analog,
            )
        }
        binding.defaultDigitalClockRow.setOnClickListener {
            viewModel.setClockDisplayMode(
                if (binding.defaultDigitalClockSwitch.isChecked) {
                    ClockDisplayMode.Analog
                } else {
                    ClockDisplayMode.Digital
                },
            )
        }
        binding.showQuickAccessSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowQuickAccess(isChecked)
        }
        binding.showQuickAccessRow.setOnClickListener {
            viewModel.setShowQuickAccess(!binding.showQuickAccessSwitch.isChecked)
        }
        binding.maxShortcutsSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.setMaxShortcuts(value.toInt())
            }
        }
        binding.showScreenTimePageSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowScreenTimePage(isChecked)
        }
        binding.showScreenTimePageRow.setOnClickListener {
            viewModel.setShowScreenTimePage(!binding.showScreenTimePageSwitch.isChecked)
        }
        binding.showNotesPageSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowNotesPage(isChecked)
        }
        binding.showNotesPageRow.setOnClickListener {
            viewModel.setShowNotesPage(!binding.showNotesPageSwitch.isChecked)
        }
        binding.showCalendarPageSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isRenderingSettingsState) {
                handleCalendarPageSettingChanged(isChecked)
            }
        }
        binding.showCalendarPageRow.setOnClickListener {
            handleCalendarPageSettingChanged(!binding.showCalendarPageSwitch.isChecked)
        }
        binding.calendarSelectionHeaderRow.setOnClickListener {
            isCalendarSelectionExpanded = !isCalendarSelectionExpanded
            renderCalendarSelection()
        }
        binding.appBlockingHeaderRow.setOnClickListener {
            isAppBlockingExpanded = !isAppBlockingExpanded
            if (isAppBlockingExpanded && blockableApps.isEmpty()) {
                refreshBlockableApps()
            } else {
                renderAppBlockingSelection()
            }
        }
        binding.appBlockingSearchInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                    renderAppBlockingSelection()
                }

                override fun afterTextChanged(text: Editable?) = Unit
            },
        )
        binding.appBlockingSearchInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && shouldScrollSettingsForFocusedSearch()) {
                scrollSettingsToFocusedSearch()
            }
        }
        binding.appBudgetsHeaderRow.setOnClickListener {
            isAppBudgetsExpanded = !isAppBudgetsExpanded
            if (isAppBudgetsExpanded && blockableApps.isEmpty()) {
                refreshBlockableApps()
            } else {
                renderAppBudgetsSelection()
            }
        }
        binding.appBudgetsSearchInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                    renderAppBudgetsSelection()
                }

                override fun afterTextChanged(text: Editable?) = Unit
            },
        )
        binding.appBudgetsSearchInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && shouldScrollSettingsForFocusedSearch()) {
                scrollSettingsToFocusedSearch()
            }
        }
        binding.resetIntentionsDataRow.setOnClickListener {
            appUsageIntentionRepository.resetIntentions()
            renderScreenTimeIntentionSummary()
            Toast.makeText(this, R.string.intentions_data_reset, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCalendarPageSettingChanged(isEnabled: Boolean) {
        viewModel.setShowCalendarPage(isEnabled)
        if (isEnabled && !hasCalendarPermission()) {
            viewModel.markCalendarPermissionRequested()
            requestCalendarPermission.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private fun configureEditControls() {
        binding.addShortcutButton.setOnClickListener {
            if (viewModel.canAddShortcut()) {
                showAppList(AppListMode.AddShortcut)
            } else {
                showShortcutLimitReached()
            }
        }
        binding.settingsButton.setOnClickListener {
            showSettings()
        }
    }

    private fun configureQuickAccess() {
        binding.cameraQuickAccessButton.setOnClickListener {
            launchQuickAccessIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        }
        binding.keepQuickAccessButton.setOnClickListener {
            val keepIntent = packageManager.getLaunchIntentForPackage(GOOGLE_KEEP_PACKAGE)
            if (keepIntent == null) {
                showQuickAccessUnavailable()
            } else {
                launchQuickAccessIntent(keepIntent)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureNotes() {
        binding.addNoteButton.setOnClickListener {
            showNoteEditor(note = null)
        }
        binding.saveNoteButton.setOnClickListener {
            saveCurrentNote()
        }
        binding.cancelNoteEditButton.setOnClickListener {
            hideNoteEditor()
        }
        binding.noteEditorInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(text: Editable?) {
                    formatNoteBulletAfterNewline(text ?: return)
                }
            },
        )

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureCalendar() {
        binding.calendarPermissionButton.setOnClickListener {
            if (hasRequestedCalendarPermission && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR)) {
                openAppPermissionSettings()
            } else {
                viewModel.markCalendarPermissionRequested()
                requestCalendarPermission.launch(Manifest.permission.READ_CALENDAR)
            }
        }
    }

    private fun configureScreenTime() {
        binding.screenTimeHeaderRow.setOnClickListener {
            isScreenTimeExpanded = !isScreenTimeExpanded
            renderScreenTimeList()
        }
        binding.screenTimePermissionButton.setOnClickListener {
            openUsageAccessSettings()
        }
        binding.screenTimeIntentionsHeaderRow.setOnClickListener {
            isScreenTimeIntentionsExpanded = !isScreenTimeIntentionsExpanded
            renderScreenTimeIntentionSummary()
        }
        binding.appBlockCancelButton.setOnClickListener {
            hideAppBlockPrompt()
        }
        binding.appBlockProceedButton.setOnClickListener {
            pendingBlockedShortcut?.let { shortcut ->
                selectedAppIntentMinutes?.let { minutes ->
                    appUsageIntentionRepository.addTodayIntention(shortcut.packageName, minutes)
                }
                hideAppBlockPrompt()
                forceLaunchShortcut(shortcut)
            }
        }
        binding.appBlockIntent5Button.setOnClickListener {
            selectAppIntentMinutes(5)
        }
        binding.appBlockIntent10Button.setOnClickListener {
            selectAppIntentMinutes(10)
        }
        binding.appBlockIntent15Button.setOnClickListener {
            selectAppIntentMinutes(15)
        }
    }

    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun renderCalendarPermissionState() {
        val hasPermission = hasCalendarPermission()
        binding.calendarPermissionPrompt.visibility = if (hasPermission) View.GONE else View.VISIBLE
        binding.calendarEventList.visibility = if (hasPermission) View.VISIBLE else View.GONE
        binding.calendarEmpty.visibility = View.GONE
        binding.calendarSelectionExpandIcon.setImageResource(
            if (isCalendarSelectionExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
        )
        binding.calendarSelectionHint.visibility = if (!hasPermission && isCalendarSelectionExpanded) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.calendarSelectionList.visibility = if (
            hasPermission &&
            isCalendarSelectionExpanded &&
            availableCalendars.isNotEmpty()
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun refreshCalendars() {
        if (!hasCalendarPermission()) {
            renderCalendarPermissionState()
            return
        }
        availableCalendars = calendarRepository.loadCalendars()
        renderCalendarSelection()
        renderCalendarPermissionState()
    }

    private fun renderCalendarSelection() {
        binding.calendarSelectionList.removeAllViews()
        availableCalendars.forEach { calendar ->
            val row = ItemCalendarSelectionBinding.inflate(
                layoutInflater,
                binding.calendarSelectionList,
                false,
            )
            row.calendarName.text = calendar.name
            row.calendarAccount.text = calendar.accountName
            row.calendarCheckbox.setOnCheckedChangeListener(null)
            row.calendarCheckbox.isChecked = calendar.id in currentSelectedCalendarIds
            row.root.setOnClickListener {
                viewModel.setCalendarSelected(calendar.id, !row.calendarCheckbox.isChecked)
            }
            row.calendarCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setCalendarSelected(calendar.id, isChecked)
            }
            binding.calendarSelectionList.addView(row.root)
        }
        renderCalendarPermissionState()
    }

    private fun refreshBlockableApps() {
        blockableApps = installedAppsRepository.loadLaunchableApps()
            .distinctBy { it.packageName }
        renderAppBlockingSelection()
        renderAppBudgetsSelection()
    }

    private fun renderAppBlockingSelection() {
        binding.appBlockingCount.text = getString(
            R.string.app_blocking_count,
            currentBlockedAppPackageNames.size,
        )
        binding.appBlockingExpandIcon.setImageResource(
            if (isAppBlockingExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
        )
        binding.appBlockingSearchInput.visibility = if (isAppBlockingExpanded) View.VISIBLE else View.GONE
        binding.appBlockingList.visibility = if (isAppBlockingExpanded) View.VISIBLE else View.GONE
        if (!isAppBlockingExpanded) return

        binding.appBlockingList.removeAllViews()
        if (blockableApps.isEmpty()) {
            blockableApps = installedAppsRepository.loadLaunchableApps()
                .distinctBy { it.packageName }
        }
        val query = binding.appBlockingSearchInput.text?.toString().orEmpty().trim()
        val displayedApps = if (query.isEmpty()) {
            blockableApps.filter { it.packageName in currentBlockedAppPackageNames }
        } else {
            FuzzyAppSearch.filter(blockableApps, query)
        }
        displayedApps.forEach { shortcut ->
            val row = ItemAppBlockSelectionBinding.inflate(
                layoutInflater,
                binding.appBlockingList,
                false,
            )
            row.blockedAppName.text = shortcut.label
            row.blockedAppCheckbox.setOnCheckedChangeListener(null)
            row.blockedAppCheckbox.isChecked = shortcut.packageName in currentBlockedAppPackageNames
            row.root.setOnClickListener {
                viewModel.setAppBlocked(shortcut.packageName, !row.blockedAppCheckbox.isChecked)
            }
            row.blockedAppCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setAppBlocked(shortcut.packageName, isChecked)
            }
            binding.appBlockingList.addView(row.root)
        }
    }

    private fun renderAppBudgetsSelection() {
        binding.appBudgetsCount.text = getString(
            R.string.app_budgets_count,
            currentAppBudgetMinutesByPackage.size,
        )
        binding.appBudgetsExpandIcon.setImageResource(
            if (isAppBudgetsExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
        )
        binding.appBudgetsSearchInput.visibility = if (isAppBudgetsExpanded) View.VISIBLE else View.GONE
        binding.appBudgetsList.visibility = if (isAppBudgetsExpanded) View.VISIBLE else View.GONE
        if (!isAppBudgetsExpanded) return

        binding.appBudgetsList.removeAllViews()
        if (blockableApps.isEmpty()) {
            blockableApps = installedAppsRepository.loadLaunchableApps()
                .distinctBy { it.packageName }
        }
        val query = binding.appBudgetsSearchInput.text?.toString().orEmpty().trim()
        val displayedApps = if (query.isEmpty()) {
            blockableApps.filter { it.packageName in currentAppBudgetMinutesByPackage }
        } else {
            FuzzyAppSearch.filter(blockableApps, query)
        }
        displayedApps.forEach { shortcut ->
            val row = ItemAppBudgetSelectionBinding.inflate(
                layoutInflater,
                binding.appBudgetsList,
                false,
            )
            row.budgetAppName.text = shortcut.label
            val selectedMinutes = currentAppBudgetMinutesByPackage[shortcut.packageName]
            renderBudgetOption(row.budget15Button, selectedMinutes, 15, shortcut.packageName)
            renderBudgetOption(row.budget30Button, selectedMinutes, 30, shortcut.packageName)
            renderBudgetOption(row.budget60Button, selectedMinutes, 60, shortcut.packageName)
            binding.appBudgetsList.addView(row.root)
        }
    }

    private fun renderBudgetOption(view: TextView, selectedMinutes: Int?, minutes: Int, packageName: String) {
        val isSelected = selectedMinutes == minutes
        view.alpha = if (isSelected) 1f else 0.68f
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            setStroke(1.dp, getColor(if (isSelected) R.color.launcher_text else R.color.settings_option_divider))
        }
        view.setOnClickListener {
            viewModel.setAppBudget(packageName, if (isSelected) null else minutes)
        }
    }

    private fun hasScreenTimePermission(): Boolean {
        val appOpsManager = getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun refreshScreenTime() {
        if (!hasScreenTimePermission()) {
            screenTimeUsages = emptyList()
            screenTimeWeekUsages = emptyList()
            renderScreenTimePermissionState()
            return
        }
        screenTimeWeekUsages = screenTimeRepository.loadCurrentWeekUsage()
        screenTimeUsages = screenTimeRepository.loadTodayUsage()
        renderScreenTimeRecap()
        renderScreenTimeWeekSummary()
        renderScreenTimeIntentionSummary()
        renderScreenTimeList()
    }

    private fun renderScreenTimePermissionState() {
        val hasPermission = hasScreenTimePermission()
        binding.screenTimeRecap.visibility = if (hasPermission) View.VISIBLE else View.GONE
        binding.screenTimeRecapDivider.visibility = if (hasPermission) View.VISIBLE else View.GONE
        binding.screenTimeWeekSummary.visibility = if (hasPermission) View.VISIBLE else View.GONE
        binding.screenTimeGraphDivider.visibility = if (hasPermission) View.VISIBLE else View.GONE
        binding.screenTimeIntentionsSummary.visibility = if (hasPermission) View.VISIBLE else View.GONE
        binding.screenTimeIntentionsDivider.visibility = if (hasPermission) View.VISIBLE else View.GONE
        binding.screenTimeHeaderRow.visibility = if (hasPermission) View.VISIBLE else View.GONE
        binding.screenTimePermissionPrompt.visibility = if (hasPermission) View.GONE else View.VISIBLE
        binding.screenTimeList.visibility = if (hasPermission && screenTimeUsages.isNotEmpty()) View.VISIBLE else View.GONE
        binding.screenTimeEmpty.visibility = if (hasPermission && screenTimeUsages.isEmpty()) View.VISIBLE else View.GONE
        binding.screenTimeExpandIcon.setImageResource(
            if (isScreenTimeExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
        )
    }

    private fun renderScreenTimeRecap() {
        val todayTotalMillis = screenTimeUsages.sumOf { it.usageMillis }
        val topApp = screenTimeUsages.firstOrNull()
        val weeklyAverageMillis = elapsedWeekAverageMillis()

        if (topApp == null) {
            binding.screenTimeTopApp.text = getString(R.string.screen_time_recap_no_top_app)
            binding.screenTimeTopAppUsage.text = formatScreenTimeDuration(0)
        } else {
            binding.screenTimeTopApp.text = topApp.label
            binding.screenTimeTopAppUsage.text = formatScreenTimeDuration(topApp.usageMillis)
        }
        binding.screenTimeVsAverage.text = formatScreenTimeAverageComparison(
            todayMillis = todayTotalMillis,
            averageMillis = weeklyAverageMillis,
        )
    }

    private fun formatScreenTimeAverageComparison(todayMillis: Long, averageMillis: Long): String {
        if (averageMillis <= 0L) return getString(R.string.screen_time_recap_percent, "→", 0)

        val percentDifference = (((todayMillis - averageMillis).toDouble() / averageMillis) * 100.0).toInt()
        val arrow = when {
            percentDifference >= SIGNIFICANT_AVERAGE_DIFFERENCE_PERCENT -> "↑"
            percentDifference >= SLIGHT_AVERAGE_DIFFERENCE_PERCENT -> "↗"
            percentDifference <= -SIGNIFICANT_AVERAGE_DIFFERENCE_PERCENT -> "↓"
            percentDifference <= -SLIGHT_AVERAGE_DIFFERENCE_PERCENT -> "↘"
            else -> "→"
        }
        return getString(R.string.screen_time_recap_percent, arrow, kotlin.math.abs(percentDifference))
    }

    private fun renderScreenTimeWeekSummary() {
        val averageUsageMillis = elapsedWeekAverageMillis()
        binding.screenTimeAverage.text = getString(
            R.string.screen_time_average,
            formatScreenTimeDuration(averageUsageMillis),
        )
        binding.screenTimeWeekGraph.setWeekUsage(screenTimeWeekUsages)
    }

    private fun elapsedWeekAverageMillis(): Long {
        val elapsedUsages = screenTimeWeekUsages.filter { it.isElapsed }
        return if (elapsedUsages.isEmpty()) {
            0L
        } else {
            elapsedUsages.sumOf { it.usageMillis } / elapsedUsages.size
        }
    }

    private fun renderScreenTimeIntentionSummary() {
        val intentionsByPackage = appUsageIntentionRepository.loadTodayIntentionsByPackage()
        val intendedMinutes = intentionsByPackage.values.sum()
        val actualMillis = screenTimeUsages
            .filter { it.packageName in intentionsByPackage }
            .sumOf { it.usageMillis }
        binding.screenTimeIntendedToday.text = formatScreenTimeDuration(intendedMinutes * MILLIS_PER_MINUTE)
        binding.screenTimeActualToday.text = formatScreenTimeDuration(actualMillis)
        binding.screenTimeIntentionsExpandIcon.setImageResource(
            if (isScreenTimeIntentionsExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
        )
        binding.screenTimeIntentionsList.visibility = if (
            isScreenTimeIntentionsExpanded &&
            intentionsByPackage.isNotEmpty()
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.screenTimeIntentionsList.removeAllViews()
        if (!isScreenTimeIntentionsExpanded) return

        intentionsByPackage.toList()
            .sortedBy { (packageName, _) -> appLabelForPackage(packageName).lowercase() }
            .forEach { (packageName, minutes) ->
                val appUsageMillis = screenTimeUsages
                    .firstOrNull { it.packageName == packageName }
                    ?.usageMillis
                    ?: 0L
                binding.screenTimeIntentionsList.addView(
                    android.widget.LinearLayout(this).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        minimumHeight = 34.dp
                        addView(
                            TextView(this@MainActivity).apply {
                                text = appLabelForPackage(packageName)
                                setTextColor(getColor(R.color.launcher_text_secondary))
                                textSize = 14f
                                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
                                maxLines = 1
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                layoutParams = android.widget.LinearLayout.LayoutParams(
                                    0,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    1f,
                                )
                            },
                        )
                        addView(
                            TextView(this@MainActivity).apply {
                                text = "${formatScreenTimeDuration(appUsageMillis)} / ${formatScreenTimeDuration(minutes * MILLIS_PER_MINUTE)}"
                                setTextColor(getColor(R.color.launcher_text_secondary))
                                textSize = 14f
                                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
                                layoutParams = android.widget.LinearLayout.LayoutParams(
                                    INTENTION_TIME_WIDTH_DP.dp,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                            },
                        )
                    },
                )
            }
    }

    private fun appLabelForPackage(packageName: String): String {
        return screenTimeUsages.firstOrNull { it.packageName == packageName }?.label
            ?: blockableApps.firstOrNull { it.packageName == packageName }?.label
            ?: packageName
    }

    private fun renderScreenTimeList() {
        renderScreenTimePermissionState()
        val visibleUsages = if (isScreenTimeExpanded) {
            screenTimeUsages
        } else {
            screenTimeUsages.take(COLLAPSED_SCREEN_TIME_APP_COUNT)
        }
        updateScreenTimeListHeight(visibleUsages.size)
        screenTimeAdapter.submitList(visibleUsages) {
            binding.screenTimeList.requestLayout()
            binding.screenTimeContent.requestLayout()
            binding.screenTimeScroll.requestLayout()
        }
    }

    private fun updateScreenTimeListHeight(itemCount: Int) {
        val targetHeight = binding.screenTimeList.paddingTop +
            binding.screenTimeList.paddingBottom +
            itemCount * SCREEN_TIME_APP_ROW_HEIGHT_DP.dp
        binding.screenTimeList.layoutParams = binding.screenTimeList.layoutParams.apply {
            height = targetHeight
        }
    }

    private fun formatScreenTimeDuration(usageMillis: Long): String {
        val totalMinutes = (usageMillis / MILLIS_PER_MINUTE).coerceAtLeast(if (usageMillis > 0) 1 else 0)
        val hours = totalMinutes / MINUTES_PER_HOUR
        val minutes = totalMinutes % MINUTES_PER_HOUR
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    private fun refreshCalendarEvents() {
        if (!hasCalendarPermission()) {
            renderCalendarPermissionState()
            return
        }
        val events = calendarRepository.loadUpcomingEvents(currentSelectedCalendarIds)
        calendarEventAdapter.submitList(events)
        binding.calendarEmpty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        binding.calendarEventList.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
        binding.calendarPermissionPrompt.visibility = View.GONE
    }

    private fun onCalendarPageVisible() {
        if (hasCalendarPermission()) {
            refreshCalendars()
            refreshCalendarEvents()
        } else {
            renderCalendarPermissionState()
            if (!hasRequestedCalendarPermission) {
                viewModel.markCalendarPermissionRequested()
                requestCalendarPermission.launch(Manifest.permission.READ_CALENDAR)
            }
        }
    }

    private fun openAppPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        startActivity(intent)
    }

    private fun openUsageAccessSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun formatNoteBulletAfterNewline(text: Editable) {
        if (isFormattingNoteBullets) return

        val cursorPosition = binding.noteEditorInput.selectionStart
        if (cursorPosition <= 0 || cursorPosition > text.length || text[cursorPosition - 1] != '\n') {
            return
        }

        val previousLineEnd = cursorPosition - 1
        val previousLineStart = text.lastIndexOf('\n', previousLineEnd - 1).let { index ->
            if (index == -1) 0 else index + 1
        }
        val previousLine = text.subSequence(previousLineStart, previousLineEnd).toString()
        val previousLineContent = previousLine.trimStart()
        if (!previousLineContent.startsWith(BULLET_PREFIX.trimEnd())) return

        isFormattingNoteBullets = true
        try {
            if (previousLine.trim() == BULLET_PREFIX.trimEnd()) {
                text.delete(previousLineStart, previousLineEnd)
                binding.noteEditorInput.setSelection(previousLineStart + 1)
            } else {
                text.insert(cursorPosition, BULLET_PREFIX)
                binding.noteEditorInput.setSelection(cursorPosition + BULLET_PREFIX.length)
            }
        } finally {
            isFormattingNoteBullets = false
        }
    }

    private fun configureShortcutReordering() {
        ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                override fun isLongPressDragEnabled(): Boolean {
                    return isEditMode
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    source: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder,
                ): Boolean {
                    viewModel.moveShortcut(source.bindingAdapterPosition, target.bindingAdapterPosition)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        viewHolder?.itemView?.alpha = DRAG_ACTIVE_ALPHA
                    }
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    viewHolder.itemView.alpha = 1f
                }
            },
        ).attachToRecyclerView(binding.shortcutList)
    }

    private fun configureAppSearch() {
        binding.appSearchInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                    renderFilteredApps(text?.toString().orEmpty())
                }

                override fun afterTextChanged(text: Editable?) = Unit
            },
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureHomeLongPress() {
        val detector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent): Boolean {
                    return true
                }

                override fun onLongPress(event: MotionEvent) {
                    if (canEnterEditModeFromLongPress()) {
                        enterEditMode()
                    }
                }

                override fun onFling(
                    firstEvent: MotionEvent?,
                    secondEvent: MotionEvent,
                    velocityX: Float,
                    velocityY: Float,
                ): Boolean {
                    if (
                        isAppPickerVisible ||
                        isNotesVisible ||
                        isCalendarVisible ||
                        isScreenTimeVisible ||
                        firstEvent == null
                    ) {
                        return false
                    }

                    val deltaX = secondEvent.x - firstEvent.x
                    val deltaY = secondEvent.y - firstEvent.y
                    val isSwipeDown = deltaY > SWIPE_DOWN_DISTANCE_DP.dp &&
                        velocityY > SWIPE_DOWN_VELOCITY_DP.dp &&
                        kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX)

                    if (isSwipeDown && !binding.shortcutList.canScrollVertically(-1)) {
                        showAppList(AppListMode.LaunchApp)
                        return true
                    }
                    return false
                }
            },
        )

        binding.homeRoot.setOnLongClickListener {
            if (canEnterEditModeFromLongPress()) {
                enterEditMode()
            }
            true
        }
        binding.clockView.setOnLongClickListener {
            if (canEnterEditModeFromLongPress()) {
                enterEditMode()
            }
            true
        }
        binding.clockView.setOnClickListener {
            viewModel.toggleClockDisplayMode()
        }
        binding.shortcutList.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                shouldHandleBlankAreaLongPress = !isAppPickerVisible &&
                    binding.shortcutList.findChildViewUnder(event.x, event.y) == null
            }
            if (shouldHandleBlankAreaLongPress) {
                detector.onTouchEvent(event)
            }
            false
        }
        binding.homeContent.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            false
        }
        binding.clockView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            false
        }
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.appBlockPromptRoot.visibility == View.VISIBLE) {
                        hideAppBlockPrompt()
                    } else if (isNoteEditorVisible) {
                        hideNoteEditor()
                    } else if (isSettingsVisible) {
                        hideSettings()
                } else if (isNotesVisible) {
                    hideNotesPage()
                } else if (isCalendarVisible) {
                    hideCalendarPage()
                } else if (isScreenTimeVisible) {
                    hideScreenTimePage()
                } else if (isAppPickerVisible) {
                    hideAppPicker()
                    } else if (isEditMode) {
                        exitEditMode()
                    } else {
                        // Consume back on the launcher home screen so the default HOME app does not close and reopen.
                    }
                }
            },
        )
    }

    private fun showShortcutContextMenu(anchor: View, shortcut: AppShortcut) {
        showActionContextMenu(
            anchor = anchor,
            actions = listOf(
                ContextMenuAction(getString(R.string.delete_shortcut)) {
                    viewModel.deleteShortcut(shortcut)
                },
                appBlockContextMenuAction(shortcut),
            ),
        )
    }

    private fun showNoteContextMenu(anchor: View, note: QuickNote) {
        showActionContextMenu(
            anchor = anchor,
            actions = listOf(
                ContextMenuAction(getString(R.string.delete_shortcut)) {
                    viewModel.deleteNote(note)
                },
            ),
        )
    }

    private fun showAppBlockContextMenu(anchor: View, shortcut: AppShortcut) {
        showActionContextMenu(
            anchor = anchor,
            actions = listOf(appBlockContextMenuAction(shortcut)),
        )
    }

    private fun appBlockContextMenuAction(shortcut: AppShortcut): ContextMenuAction {
        val isBlocked = shortcut.packageName in currentBlockedAppPackageNames
        return ContextMenuAction(
            label = getString(if (isBlocked) R.string.blocked_app else R.string.block_app),
            isEnabled = !isBlocked,
        ) {
            if (!isBlocked) {
                viewModel.setAppBlocked(shortcut.packageName, true)
            }
        }
    }

    private fun showActionContextMenu(anchor: View, actions: List<ContextMenuAction>) {
        val menuView = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(getColor(R.color.launcher_background))
                setStroke(1.dp, getColor(R.color.launcher_text))
            }
        }

        val popup = PopupWindow(
            menuView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 0f
        }

        actions.forEach { action ->
            val row = TextView(this).apply {
                text = action.label
                setTextColor(
                    getColor(if (action.isEnabled) R.color.launcher_text else R.color.launcher_text_secondary),
                )
                textSize = 16f
                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
                isEnabled = action.isEnabled
                alpha = if (action.isEnabled) 1f else DISABLED_ACTION_ALPHA
                minWidth = MENU_WIDTH_DP.dp
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    MENU_HEIGHT_DP.dp,
                )
                setPadding(MENU_HORIZONTAL_PADDING_DP.dp, 0, MENU_HORIZONTAL_PADDING_DP.dp, 0)
                setOnClickListener {
                    action.onClick()
                    popup.dismiss()
                }
            }
            menuView.addView(row)
        }
        popup.showAsDropDown(anchor, 0, 0)
    }

    private fun handleShortcutClick(anchor: View, shortcut: AppShortcut) {
        if (isEditMode) {
            showShortcutContextMenu(anchor, shortcut)
        } else {
            launchShortcutWithAppBlocking(shortcut)
        }
    }

    private fun enterEditMode() {
        if (isEditMode) return
        isEditMode = true
        shortcutAdapter.isEditMode = true
        binding.clockDateContent.visibility = View.GONE
        binding.editModeText.visibility = View.VISIBLE
        startEditModePulse()
        binding.editControls.alpha = 0f
        binding.editControls.visibility = View.VISIBLE
        binding.editControls.animate()
            .alpha(1f)
            .setDuration(EDIT_CONTROLS_FADE_MS)
            .start()
    }

    private fun exitEditMode() {
        isEditMode = false
        shortcutAdapter.isEditMode = false
        stopEditModePulse()
        binding.editModeText.visibility = View.GONE
        binding.clockDateContent.visibility = View.VISIBLE
        binding.editControls.animate().cancel()
        binding.editControls.visibility = View.GONE
        binding.editControls.alpha = 1f
    }

    private fun startEditModePulse() {
        editModePulseAnimator?.cancel()
        binding.editModeText.alpha = EDIT_TEXT_MAX_ALPHA
        editModePulseAnimator = ValueAnimator.ofFloat(EDIT_TEXT_MAX_ALPHA, EDIT_TEXT_MIN_ALPHA).apply {
            duration = EDIT_TEXT_PULSE_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                binding.editModeText.alpha = animator.animatedValue as Float
            }
            start()
        }
    }

    private fun showSettings() {
        isSettingsVisible = true
        refreshCalendars()
        if (blockableApps.isEmpty()) {
            refreshBlockableApps()
        } else {
            renderAppBlockingSelection()
            renderAppBudgetsSelection()
        }
        binding.settingsRoot.alpha = 0f
        binding.settingsRoot.visibility = View.VISIBLE
        binding.settingsRoot.animate()
            .alpha(1f)
            .setDuration(SETTINGS_FADE_MS)
            .start()
    }

    private fun showNotesPage() {
        if (isNotesVisible || isCalendarVisible || !binding.showNotesPageSwitch.isChecked || isEditMode) return
        isNotesVisible = true
        val width = binding.homeRoot.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        binding.notesRoot.translationX = width.toFloat()
        binding.notesRoot.visibility = View.VISIBLE
        binding.homeContent.animate().cancel()
        binding.notesRoot.animate().cancel()
        binding.homeContent.animate()
            .translationX(-width.toFloat())
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        binding.notesRoot.animate()
            .translationX(0f)
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideNotesPage() {
        if (!isNotesVisible) return
        isNotesVisible = false
        val width = binding.homeRoot.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        binding.homeContent.animate().cancel()
        binding.notesRoot.animate().cancel()
        binding.homeContent.animate()
            .translationX(0f)
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        binding.notesRoot.animate()
            .translationX(width.toFloat())
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                if (!isNotesVisible) {
                    binding.notesRoot.visibility = View.GONE
                    binding.notesRoot.translationX = 0f
                }
            }
            .start()
    }

    private fun showCalendarPage() {
        if (isCalendarVisible || isNotesVisible || !binding.showCalendarPageSwitch.isChecked || isEditMode) return
        isCalendarVisible = true
        val width = binding.homeRoot.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        binding.calendarRoot.translationX = -width.toFloat()
        binding.calendarRoot.visibility = View.VISIBLE
        binding.homeContent.animate().cancel()
        binding.calendarRoot.animate().cancel()
        binding.homeContent.animate()
            .translationX(width.toFloat())
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        binding.calendarRoot.animate()
            .translationX(0f)
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        onCalendarPageVisible()
    }

    private fun hideCalendarPage() {
        if (!isCalendarVisible) return
        isCalendarVisible = false
        val width = binding.homeRoot.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        binding.homeContent.animate().cancel()
        binding.calendarRoot.animate().cancel()
        binding.homeContent.animate()
            .translationX(0f)
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        binding.calendarRoot.animate()
            .translationX(-width.toFloat())
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                if (!isCalendarVisible) {
                    binding.calendarRoot.visibility = View.GONE
                    binding.calendarRoot.translationX = 0f
                }
            }
            .start()
    }

    private fun showScreenTimePage() {
        if (isScreenTimeVisible || isEditMode || !binding.showScreenTimePageSwitch.isChecked) return
        isScreenTimeVisible = true
        refreshScreenTime()
        binding.screenTimeRoot.animate().cancel()
        binding.screenTimeRoot.alpha = 0f
        binding.screenTimeRoot.visibility = View.VISIBLE
        binding.screenTimeRoot.animate()
            .alpha(1f)
            .setDuration(SCREEN_TIME_FADE_MS)
            .start()
    }

    private fun hideScreenTimePage() {
        if (!isScreenTimeVisible) return
        isScreenTimeVisible = false
        binding.screenTimeRoot.animate().cancel()
        binding.screenTimeRoot.visibility = View.GONE
        binding.screenTimeRoot.alpha = 1f
    }

    private fun showNoteEditor(note: QuickNote?) {
        editingNote = note
        isNoteEditorVisible = true
        binding.noteEditorInput.setText(note?.text.orEmpty())
        binding.noteEditorInput.setSelection(binding.noteEditorInput.text?.length ?: 0)
        binding.noteEditorRoot.alpha = 0f
        binding.noteEditorRoot.visibility = View.VISIBLE
        binding.noteEditorRoot.animate()
            .alpha(1f)
            .setDuration(SETTINGS_FADE_MS)
            .start()
        binding.noteEditorInput.requestFocus()
        showNoteKeyboard()
    }

    private fun hideNoteEditor() {
        isNoteEditorVisible = false
        editingNote = null
        hideNoteKeyboard()
        binding.noteEditorRoot.animate().cancel()
        binding.noteEditorRoot.visibility = View.GONE
        binding.noteEditorRoot.alpha = 1f
        binding.noteEditorInput.text?.clear()
    }

    private fun saveCurrentNote() {
        val note = editingNote
        val text = binding.noteEditorInput.text?.toString().orEmpty()
        if (note == null) {
            viewModel.addNote(text)
        } else {
            viewModel.updateNote(note, text)
        }
        hideNoteEditor()
    }

    private fun copyNote(note: QuickNote) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(getString(R.string.copy_note), note.text))
        Toast.makeText(this, R.string.note_copied, Toast.LENGTH_SHORT).show()
    }

    private fun openCalendarEventDay(event: CalendarEvent) {
        val calendarUri = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .appendPath(event.startMillis.toString())
            .build()
        val googleCalendarIntent = Intent(Intent.ACTION_VIEW, calendarUri).apply {
            setPackage(GOOGLE_CALENDAR_PACKAGE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(googleCalendarIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW, calendarUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    },
                )
            } catch (_: ActivityNotFoundException) {
                showQuickAccessUnavailable()
            }
        }
    }

    private fun hideSettings() {
        isSettingsVisible = false
        binding.settingsRoot.animate().cancel()
        binding.settingsRoot.visibility = View.GONE
        binding.settingsRoot.alpha = 1f
    }

    private fun stopEditModePulse() {
        editModePulseAnimator?.cancel()
        editModePulseAnimator = null
        binding.editModeText.alpha = 1f
    }

    private fun showAppList(mode: AppListMode) {
        appListMode = mode
        availableApps = installedAppsRepository.loadLaunchableApps()
        binding.appSearchInput.text?.clear()
        renderFilteredApps(query = "")
        binding.appPickerRoot.visibility = View.VISIBLE
        binding.homeContent.visibility = View.GONE
        isAppPickerVisible = true
        if (mode == AppListMode.LaunchApp) {
            animateAppListEntrance()
        } else {
            binding.appPickerRoot.alpha = 1f
            binding.appPickerRoot.translationY = 0f
        }
        binding.appSearchInput.requestFocus()
        showKeyboard()
    }

    private fun hideAppPicker() {
        binding.appPickerRoot.animate().cancel()
        hideKeyboard()
        binding.appPickerRoot.visibility = View.GONE
        binding.appPickerRoot.alpha = 1f
        binding.appPickerRoot.translationY = 0f
        binding.homeContent.visibility = View.VISIBLE
        binding.appSearchInput.text?.clear()
        appPickerAdapter.submitList(emptyList())
        availableApps = emptyList()
        isAppPickerVisible = false
    }

    private fun animateAppListEntrance() {
        binding.appPickerRoot.animate().cancel()
        binding.appPickerRoot.alpha = APP_LIST_START_ALPHA
        binding.appPickerRoot.translationY = -APP_LIST_ENTER_OFFSET_DP.dp.toFloat()
        binding.appPickerRoot.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(APP_LIST_ENTER_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun renderFilteredApps(query: String) {
        val filteredApps = FuzzyAppSearch.filter(availableApps, query)
        appPickerAdapter.submitList(filteredApps)
        binding.appPickerList.visibility = if (filteredApps.isEmpty()) View.GONE else View.VISIBLE
        binding.appPickerEmpty.visibility = if (filteredApps.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showKeyboard() {
        binding.appSearchInput.post {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(binding.appSearchInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun showNoteKeyboard() {
        binding.noteEditorInput.post {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(binding.noteEditorInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.appSearchInput.windowToken, 0)
        binding.appSearchInput.clearFocus()
    }

    private fun hideNoteKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.noteEditorInput.windowToken, 0)
        binding.noteEditorInput.clearFocus()
    }

    private fun launchShortcutWithAppBlocking(shortcut: AppShortcut) {
        val budgetOverrun = findBudgetOverrun(shortcut.packageName)
        if (shortcut.packageName in currentBlockedAppPackageNames || budgetOverrun != null) {
            showAppBlockPrompt(shortcut, budgetOverrun)
        } else {
            forceLaunchShortcut(shortcut)
        }
    }

    private fun findBudgetOverrun(packageName: String): AppBudgetOverrun? {
        val budgetMinutes = currentAppBudgetMinutesByPackage[packageName] ?: return null
        if (!hasScreenTimePermission()) return null

        val usageMillis = screenTimeRepository.loadTodayUsage()
            .firstOrNull { it.packageName == packageName }
            ?.usageMillis
            ?: 0L
        val budgetMillis = budgetMinutes * MILLIS_PER_MINUTE
        return if (usageMillis >= budgetMillis) {
            AppBudgetOverrun(
                budgetMinutes = budgetMinutes,
                usageMillis = usageMillis,
            )
        } else {
            null
        }
    }

    private fun showAppBlockPrompt(shortcut: AppShortcut, budgetOverrun: AppBudgetOverrun?) {
        pendingBlockedShortcut = shortcut
        selectedAppIntentMinutes = null
        isAppBlockCountdownComplete = false
        appBlockCountdownTimer?.cancel()
        binding.appBlockPromptTitle.text = getString(R.string.app_blocking_prompt_title, shortcut.label)
        if (budgetOverrun == null) {
            binding.appBlockPromptMessage.text = getString(R.string.app_blocking_prompt_message)
            binding.appBlockPromptMessage.setTextColor(getColor(R.color.launcher_text_secondary))
        } else {
            binding.appBlockPromptMessage.text = getString(
                R.string.app_budget_prompt_message,
                formatScreenTimeDuration(budgetOverrun.budgetMinutes * MILLIS_PER_MINUTE),
                formatScreenTimeDuration(budgetOverrun.usageMillis),
            )
            binding.appBlockPromptMessage.setTextColor(getColor(R.color.launcher_warning))
        }
        renderAppIntentSelection()
        renderAppBlockProceedState()
        binding.appBlockPromptRoot.alpha = 0f
        binding.appBlockPromptRoot.visibility = View.VISIBLE
        binding.appBlockPromptRoot.animate()
            .alpha(1f)
            .setDuration(APP_BLOCK_PROMPT_FADE_MS)
            .start()
        startAppBlockCountdown()
    }

    private fun startAppBlockCountdown() {
        renderAppBlockProceedCountdown(APP_BLOCK_WAIT_SECONDS)
        appBlockCountdownTimer = object : CountDownTimer(APP_BLOCK_WAIT_MS, APP_BLOCK_COUNTDOWN_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = ((millisUntilFinished + 999L) / 1_000L).toInt()
                renderAppBlockProceedCountdown(secondsRemaining)
            }

            override fun onFinish() {
                isAppBlockCountdownComplete = true
                renderAppBlockProceedState()
            }
        }.start()
    }

    private fun renderAppBlockProceedCountdown(secondsRemaining: Int) {
        binding.appBlockProceedButton.text = getString(
            R.string.app_blocking_proceed_countdown,
            secondsRemaining.coerceAtLeast(1),
        )
    }

    private fun selectAppIntentMinutes(minutes: Int) {
        selectedAppIntentMinutes = minutes
        renderAppIntentSelection()
        renderAppBlockProceedState()
    }

    private fun renderAppIntentSelection() {
        renderAppIntentOption(binding.appBlockIntent5Button, 5)
        renderAppIntentOption(binding.appBlockIntent10Button, 10)
        renderAppIntentOption(binding.appBlockIntent15Button, 15)
    }

    private fun renderAppIntentOption(view: TextView, minutes: Int) {
        val isSelected = selectedAppIntentMinutes == minutes
        view.alpha = if (isSelected) 1f else 0.68f
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            setStroke(1.dp, getColor(if (isSelected) R.color.launcher_text else R.color.settings_option_divider))
        }
    }

    private fun renderAppBlockProceedState() {
        val canProceed = isAppBlockCountdownComplete && selectedAppIntentMinutes != null
        binding.appBlockProceedButton.isEnabled = canProceed
        binding.appBlockProceedButton.alpha = if (canProceed) 1f else DISABLED_ACTION_ALPHA
        if (isAppBlockCountdownComplete) {
            binding.appBlockProceedButton.text = getString(R.string.app_blocking_proceed)
        }
    }

    private fun hideAppBlockPrompt() {
        appBlockCountdownTimer?.cancel()
        appBlockCountdownTimer = null
        pendingBlockedShortcut = null
        selectedAppIntentMinutes = null
        isAppBlockCountdownComplete = false
        binding.appBlockPromptRoot.animate().cancel()
        binding.appBlockPromptRoot.visibility = View.GONE
        binding.appBlockPromptRoot.alpha = 1f
        binding.appBlockProceedButton.isEnabled = false
        binding.appBlockProceedButton.alpha = DISABLED_ACTION_ALPHA
    }

    private fun forceLaunchShortcut(shortcut: AppShortcut) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(shortcut.packageName, shortcut.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        startActivity(intent)
    }

    private fun launchQuickAccessIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showQuickAccessUnavailable()
        }
    }

    private fun showQuickAccessUnavailable() {
        Toast.makeText(this, R.string.quick_access_unavailable, Toast.LENGTH_SHORT).show()
    }

    private fun showShortcutLimitReached() {
        Toast.makeText(this, R.string.shortcut_limit_reached, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val GOOGLE_KEEP_PACKAGE = "com.google.android.keep"
        const val GOOGLE_CALENDAR_PACKAGE = "com.google.android.calendar"
        const val MENU_WIDTH_DP = 112
        const val MENU_HEIGHT_DP = 48
        const val MENU_HORIZONTAL_PADDING_DP = 18
        const val INTENTION_TIME_WIDTH_DP = 132
        const val SETTINGS_KEYBOARD_SCROLL_TOP_OFFSET_DP = 12
        const val SLIGHT_AVERAGE_DIFFERENCE_PERCENT = 5
        const val SIGNIFICANT_AVERAGE_DIFFERENCE_PERCENT = 25
        const val SWIPE_DOWN_DISTANCE_DP = 96
        const val SWIPE_DOWN_VELOCITY_DP = 450
        const val SCREEN_TIME_GESTURE_POINTERS = 2
        const val SCREEN_TIME_SWIPE_DOWN_DISTANCE_DP = 96
        const val COLLAPSED_SCREEN_TIME_APP_COUNT = 3
        const val SCREEN_TIME_APP_ROW_HEIGHT_DP = 58
        const val PAGE_SWIPE_AXIS_RATIO = 1.15f
        const val PAGE_SWIPE_COMPLETE_FRACTION = 0.28f
        const val PAGE_SWIPE_COMPLETE_VELOCITY = 700f
        const val BULLET_PREFIX = "- "
        const val PAGE_SETTLE_MS = 180L
        const val PAGE_SLIDE_MS = 220L
        const val APP_LIST_ENTER_OFFSET_DP = 24
        const val APP_LIST_ENTER_DURATION_MS = 220L
        const val APP_LIST_START_ALPHA = 0.35f
        const val APP_BLOCK_WAIT_SECONDS = 5
        const val APP_BLOCK_WAIT_MS = 5_000L
        const val APP_BLOCK_COUNTDOWN_INTERVAL_MS = 1_000L
        const val APP_BLOCK_PROMPT_FADE_MS = 160L
        const val EDIT_CONTROLS_FADE_MS = 160L
        const val EDIT_TEXT_PULSE_MS = 1_200L
        const val EDIT_TEXT_MIN_ALPHA = 0.38f
        const val EDIT_TEXT_MAX_ALPHA = 1f
        const val DRAG_ACTIVE_ALPHA = 0.65f
        const val DISABLED_ACTION_ALPHA = 0.34f
        const val MILLIS_PER_MINUTE = 60_000L
        const val MINUTES_PER_HOUR = 60L
        const val SETTINGS_FADE_MS = 160L
        const val SCREEN_TIME_FADE_MS = 180L
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private enum class AppListMode {
        AddShortcut,
        LaunchApp,
    }

    private enum class PageSwipeTarget {
        HomeToNotes,
        NotesToHome,
        HomeToCalendar,
        CalendarToHome,
    }

    private data class ContextMenuAction(
        val label: String,
        val isEnabled: Boolean = true,
        val onClick: () -> Unit,
    )

    private data class AppBudgetOverrun(
        val budgetMinutes: Int,
        val usageMillis: Long,
    )
}
