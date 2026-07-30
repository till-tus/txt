package com.example.textlauncher.ui

import android.animation.ValueAnimator
import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.location.Location
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.CalendarContract
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
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
import com.example.textlauncher.data.OpenMeteoWeatherRepository
import com.example.textlauncher.data.ScreenTimeRepository
import com.example.textlauncher.data.ShortcutRepository
import com.example.textlauncher.data.TodayNotificationCenter
import com.example.textlauncher.data.TodayWidgetRepository
import com.example.textlauncher.databinding.ActivityMainBinding
import com.example.textlauncher.databinding.ItemAppBudgetSelectionBinding
import com.example.textlauncher.databinding.ItemAppBlockSelectionBinding
import com.example.textlauncher.databinding.ItemCalendarSelectionBinding
import com.example.textlauncher.domain.ClockDisplayMode
import com.example.textlauncher.domain.AppShortcut
import com.example.textlauncher.domain.CalendarEvent
import com.example.textlauncher.domain.DeviceCalendar
import com.example.textlauncher.domain.GestureAction
import com.example.textlauncher.domain.LauncherGesture
import com.example.textlauncher.domain.LauncherPage
import com.example.textlauncher.domain.PageArrangement
import com.example.textlauncher.domain.PagePosition
import com.example.textlauncher.domain.QuickAccessIcon
import com.example.textlauncher.domain.QuickAccessPosition
import com.example.textlauncher.domain.QuickAccessTarget
import com.example.textlauncher.domain.QuickNote
import com.example.textlauncher.domain.ScreenTimeAppUsage
import com.example.textlauncher.domain.ScreenTimeDayUsage
import com.example.textlauncher.domain.ShortcutTextAlignment
import com.example.textlauncher.domain.TodayWidget
import com.example.textlauncher.domain.TodayWidgetType
import com.example.textlauncher.domain.TodayNotificationItem
import com.example.textlauncher.domain.TrashedNote
import com.example.textlauncher.domain.WeatherSnapshot
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsPage(val titleRes: Int) {
    Index(R.string.launcher_settings),
    Appearance(R.string.settings_category_appearance),
    Notes(R.string.settings_category_notes),
    Calendar(R.string.settings_category_calendar),
    Gestures(R.string.settings_category_gestures),
    ScreenTime(R.string.settings_category_screen_time),
}

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var shortcutAdapter: ShortcutAdapter
    private lateinit var appPickerAdapter: AppPickerAdapter
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var trashNoteAdapter: TrashNoteAdapter
    private lateinit var calendarEventAdapter: CalendarEventAdapter
    private lateinit var screenTimeAdapter: ScreenTimeAdapter
    private lateinit var actionContextMenu: ActionContextMenu
    private lateinit var noteBulletFormatter: NoteBulletFormatter
    private lateinit var appBlockPromptController: AppBlockPromptController
    private var shouldHandleBlankAreaLongPress = false
    private var shouldHandleHomeContentLongPress = false
    private var isAppPickerVisible = false
    private var isEditMode = false
    private var isSettingsVisible = false
    private var currentSettingsPage = SettingsPage.Index
    private var isNoteTrashVisible = false
    private var isNotesVisible = false
    private var isCalendarVisible = false
    private var isTodayVisible = false
    private var isTodayEditMode = false
    private var isScreenTimeVisible = false
    private var isNoteEditorVisible = false
    private var didOpenNoteEditorFromToday = false
    private var isScreenTimeExpanded = false
    private var isScreenTimeIntentionsExpanded = false
    private var appListMode = AppListMode.AddShortcut
    private var availableApps = emptyList<AppShortcut>()
    private var cachedLaunchableApps = emptyList<AppShortcut>()
    private var screenTimeUsages = emptyList<ScreenTimeAppUsage>()
    private var screenTimeWeekUsages = emptyList<ScreenTimeDayUsage>()
    private var blockableApps = emptyList<AppShortcut>()
    private var currentBlockedAppPackageNames = emptySet<String>()
    private var currentAppBudgetMinutesByPackage = emptyMap<String, Int>()
    private var currentExcludedScreenTimePackageNames = emptySet<String>()
    private var currentPageArrangement = PageArrangement.Default
    private var currentLeftQuickAccess: QuickAccessTarget? = null
    private var currentRightQuickAccess: QuickAccessTarget? = null
    private var currentQuickAccessPosition = QuickAccessPosition.BothCenter
    private var todayWidgets = emptyList<TodayWidget>()
    private var todayNextEvent: CalendarEvent? = null
    private var todayWeather: WeatherSnapshot? = null
    private var todayWeatherError: String? = null
    private var isTodayWeatherLoading = false
    private var todayWeatherLoadedAtMillis = 0L
    private var hasRequestedWeatherLocationPermission = false
    private var todayNotifications = emptyList<TodayNotificationItem>()
    private var currentNotes = emptyList<QuickNote>()
    private var currentTrashedNotes = emptyList<TrashedNote>()
    private var editingNote: QuickNote? = null
    private var noteInputMode = NoteInputMode.Text
    private var voiceNoteRecorder: MediaRecorder? = null
    private var voiceNoteRecordingFile: File? = null
    private var voiceNoteRecordingStartedAtMillis = 0L
    private var voiceNoteSamples = mutableListOf<Int>()
    private var voiceNotePlayer: MediaPlayer? = null
    private var playingVoiceNoteId: Long? = null
    private var isVoicePlaybackPlaying = false
    private var voicePlaybackProgressFraction = 0f
    private var todayPinnedVoiceWaveform: VoiceWaveformView? = null
    private var todayPinnedVoicePlayButton: android.widget.ImageButton? = null
    private var pageSwipeStartX = 0f
    private var pageSwipeStartY = 0f
    private var activePageSwipeTarget: PageSwipeTarget? = null
    private var pageSwipeVelocityTracker: VelocityTracker? = null
    private var didCancelPageSwipeChildren = false
    private var appListDragStartX = 0f
    private var appListDragStartY = 0f
    private var isDraggingAppList = false
    private var didCancelAppListDragChildren = false
    private var appListDragVelocityTracker: VelocityTracker? = null
    private var screenTimeGestureStartX = 0f
    private var screenTimeGestureStartY = 0f
    private var isTrackingTwoFingerSwipeDown = false
    private var hasTriggeredTwoFingerSwipeDown = false
    private var availableCalendars = emptyList<DeviceCalendar>()
    private var currentSelectedCalendarIds = emptySet<Long>()
    private var currentOpenScreenTimeGesture = LauncherGesture.TwoFingerSwipeDown
    private var currentLockScreenGesture = LauncherGesture.DoubleTap
    private var currentGesturePickerAction: GestureAction? = null
    private var hasRequestedCalendarPermission = false
    private var isCalendarSelectionExpanded = false
    private var isAppBlockingExpanded = false
    private var isAppBudgetsExpanded = false
    private var isScreenTimeExclusionsExpanded = false
    private val packageRemovedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_PACKAGE_REMOVED) return
            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

            val packageName = intent.data?.schemeSpecificPart ?: return
            handlePackageRemoved(packageName)
        }
    }
    private var isRenderingSettingsState = false
    private var wasSettingsImeVisible = false
    private var openAppListKeyboardAutomatically = true
    private var editModePulseAnimator: ValueAnimator? = null
    private var renderedShortcutCount = 0
    private val voiceNoteSampleHandler = Handler(Looper.getMainLooper())
    private val voiceNoteSampleRunnable = object : Runnable {
        override fun run() {
            sampleVoiceNoteAmplitude()
            voiceNoteSampleHandler.postDelayed(this, VOICE_NOTE_SAMPLE_INTERVAL_MS)
        }
    }
    private val voiceNotePlaybackProgressHandler = Handler(Looper.getMainLooper())
    private val voiceNotePlaybackProgressRunnable = object : Runnable {
        override fun run() {
            updateVoiceNotePlaybackProgress()
            voiceNotePlaybackProgressHandler.postDelayed(this, VOICE_NOTE_PLAYBACK_PROGRESS_INTERVAL_MS)
        }
    }
    private val noteUndoHandler = Handler(Looper.getMainLooper())
    private var pendingUndoNoteId: Long? = null
    private val noteUndoDismissRunnable = Runnable {
        hideNoteUndo()
    }
    private val installedAppsRepository by lazy { InstalledAppsRepository(applicationContext) }
    private val appUsageIntentionRepository by lazy { AppUsageIntentionRepository(applicationContext) }
    private val calendarRepository by lazy { CalendarRepository(applicationContext) }
    private val screenTimeRepository by lazy { ScreenTimeRepository(applicationContext) }
    private val todayWidgetRepository by lazy { TodayWidgetRepository(applicationContext) }
    private val weatherRepository by lazy { OpenMeteoWeatherRepository() }
    private val requestCalendarPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        renderCalendarPermissionState()
        if (isGranted) {
            refreshCalendars()
            refreshCalendarEvents()
            refreshTodayWidgets()
        }
    }
    private val requestWeatherLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            refreshTodayWeather(force = true)
        } else {
            todayWeatherError = getString(R.string.today_weather_location_needed)
            renderTodayWidgets()
        }
    }
    private val requestMicrophonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            startVoiceNoteRecording()
        } else {
            Toast.makeText(this, R.string.microphone_permission_prompt, Toast.LENGTH_LONG).show()
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actionContextMenu = ActionContextMenu(this)
        noteBulletFormatter = NoteBulletFormatter()
        appBlockPromptController = AppBlockPromptController(
            context = this,
            binding = binding,
            formatDuration = ::formatScreenTimeDuration,
        ) { shortcut, minutes ->
            appUsageIntentionRepository.addTodayIntention(shortcut.packageName, minutes)
            forceLaunchShortcut(shortcut)
        }
        registerPackageRemovedReceiver()

        shortcutAdapter = ShortcutAdapter(::handleShortcutClick, ::showShortcutContextMenu)
        binding.shortcutList.layoutManager = object : LinearLayoutManager(this) {
            override fun canScrollVertically(): Boolean = false
        }.apply {
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
                    showLauncherAppContextMenu(anchor, shortcut)
                }
            },
        )
        binding.appPickerList.layoutManager = LinearLayoutManager(this)
        binding.appPickerList.adapter = appPickerAdapter

        noteAdapter = NoteAdapter(
            ::showNoteEditor,
            ::copyNote,
            ::showNoteContextMenu,
            ::toggleVoiceNotePlayback,
            ::resetVoiceNotePlaybackFromNotesList,
        )
        binding.notesList.layoutManager = LinearLayoutManager(this)
        binding.notesList.addItemDecoration(NotesDividerDecoration(this))
        binding.notesList.adapter = noteAdapter

        trashNoteAdapter = TrashNoteAdapter(
            onRestoreClick = { trashedNote ->
                restoreTrashedNote(trashedNote.note.id)
            },
            onDeletePermanentlyClick = ::confirmPermanentNoteDeletion,
        )
        binding.noteTrashList.layoutManager = LinearLayoutManager(this)
        binding.noteTrashList.addItemDecoration(NotesDividerDecoration(this))
        binding.noteTrashList.adapter = trashNoteAdapter

        calendarEventAdapter = CalendarEventAdapter(::openCalendarEventDay)
        binding.calendarEventList.layoutManager = LinearLayoutManager(this)
        binding.calendarEventList.adapter = calendarEventAdapter

        screenTimeAdapter = ScreenTimeAdapter()
        binding.screenTimeList.layoutManager = LinearLayoutManager(this)
        binding.screenTimeList.adapter = screenTimeAdapter
        todayWidgets = todayWidgetRepository.loadWidgets()

        configureSystemInsets()
        bindCurrentDate()
        configureAppSearch()
        configureSettings()
        configureEditControls()
        configureQuickAccess()
        configureNotes()
        configureCalendar()
        configureToday()
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                TodayNotificationCenter.notifications.collect { notifications ->
                    todayNotifications = notifications
                    renderTodayWidgets()
                }
            }
        }
        refreshLaunchableAppCache()
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
        refreshTodayWidgets()
        if (isScreenTimeVisible) {
            refreshScreenTime()
        }
        refreshLaunchableAppCache()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.isHomeLaunchIntent()) {
            routeToHomeScreen()
        }
    }

    override fun onStop() {
        stopVoiceNoteRecording(save = true)
        stopVoiceNotePlayback()
        resetInFlightAppListDrag()
        super.onStop()
    }

    override fun onDestroy() {
        stopVoiceNoteRecording(save = false)
        releaseVoiceNotePlayer()
        resetInFlightAppListDrag()
        noteUndoHandler.removeCallbacks(noteUndoDismissRunnable)
        unregisterReceiver(packageRemovedReceiver)
        appBlockPromptController.cancel()
        super.onDestroy()
    }

    private fun registerPackageRemovedReceiver() {
        val filter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            this,
            packageRemovedReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun handlePackageRemoved(packageName: String) {
        viewModel.deleteShortcutsForPackage(packageName)
        viewModel.removePackageReferences(packageName)
        if (isAppPickerVisible) {
            availableApps = loadCachedLaunchableApps()
            renderFilteredApps(binding.appSearchInput.text?.toString().orEmpty())
        }
        refreshLaunchableAppCache()
        if (
            isSettingsVisible &&
            (isAppBlockingExpanded || isAppBudgetsExpanded || isScreenTimeExclusionsExpanded)
        ) {
            refreshBlockableApps()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (handleTwoFingerSwipeDownGesture(event)) {
            return true
        }
        if (handleAppListDrag(event)) {
            return true
        }
        if (handlePageDrag(event)) {
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    private fun handleTwoFingerSwipeDownGesture(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (
                    event.pointerCount == TWO_FINGER_SWIPE_DOWN_POINTERS &&
                    canHandleConfigurableGesture(LauncherGesture.TwoFingerSwipeDown)
                ) {
                    screenTimeGestureStartX = averagePointerX(event)
                    screenTimeGestureStartY = averagePointerY(event)
                    isTrackingTwoFingerSwipeDown = true
                    hasTriggeredTwoFingerSwipeDown = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isTrackingTwoFingerSwipeDown || event.pointerCount < TWO_FINGER_SWIPE_DOWN_POINTERS) {
                    return false
                }
                val deltaX = averagePointerX(event) - screenTimeGestureStartX
                val deltaY = averagePointerY(event) - screenTimeGestureStartY
                val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
                val isVerticalDrag = deltaY > touchSlop && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) * PAGE_SWIPE_AXIS_RATIO
                if (!isVerticalDrag) {
                    return false
                }
                if (!hasTriggeredTwoFingerSwipeDown && deltaY > TWO_FINGER_SWIPE_DOWN_DISTANCE_DP.dp) {
                    hasTriggeredTwoFingerSwipeDown = true
                    performGestureAction(LauncherGesture.TwoFingerSwipeDown)
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                val wasTracking = isTrackingTwoFingerSwipeDown
                isTrackingTwoFingerSwipeDown = false
                hasTriggeredTwoFingerSwipeDown = false
                return wasTracking && event.actionMasked != MotionEvent.ACTION_POINTER_UP
            }
        }
        return false
    }

    private fun canHandleConfigurableGesture(gesture: LauncherGesture): Boolean {
        return !isAppPickerVisible &&
            !isSettingsVisible &&
            !isNotesVisible &&
            !isCalendarVisible &&
            !isTodayVisible &&
            !isScreenTimeVisible &&
            !isNoteEditorVisible &&
            !isEditMode &&
            actionForGesture(gesture) != null
    }

    private fun performGestureAction(gesture: LauncherGesture) {
        when (actionForGesture(gesture)) {
            GestureAction.OpenScreenTime -> showScreenTimePage()
            GestureAction.LockScreen -> lockScreen()
            null -> Unit
        }
    }

    private fun actionForGesture(gesture: LauncherGesture): GestureAction? {
        return GestureAction.entries.firstOrNull { action ->
            gestureForAction(action) == gesture
        }
    }

    private fun gestureForAction(action: GestureAction): LauncherGesture {
        return when (action) {
            GestureAction.OpenScreenTime -> currentOpenScreenTimeGesture
            GestureAction.LockScreen -> currentLockScreenGesture
        }
    }

    private fun actionLabel(action: GestureAction): String {
        return getString(
            when (action) {
                GestureAction.OpenScreenTime -> R.string.gesture_action_open_screen_time
                GestureAction.LockScreen -> R.string.gesture_action_lock_screen
            },
        )
    }

    private fun gestureLabel(gesture: LauncherGesture): String {
        return getString(
            when (gesture) {
                LauncherGesture.None -> R.string.gesture_none
                LauncherGesture.TwoFingerSwipeDown -> R.string.gesture_two_finger_swipe_down
                LauncherGesture.DoubleTap -> R.string.gesture_double_tap
            },
        )
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
                applyPageDrag(target, deltaX, deltaY)
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
                val velocityY = pageSwipeVelocityTracker?.yVelocity ?: 0f
                val deltaX = event.rawX - pageSwipeStartX
                val deltaY = event.rawY - pageSwipeStartY
                settlePageDrag(
                    target = target,
                    shouldComplete = shouldCompletePageDrag(target, deltaX, deltaY, velocityX, velocityY),
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

    private fun handleAppListDrag(event: MotionEvent): Boolean {
        if (!canHandleAppListDrag(event)) {
            if (isDraggingAppList) {
                settleAppListDrag(shouldComplete = false)
            }
            cancelAppListDrag()
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                appListDragStartX = event.rawX
                appListDragStartY = event.rawY
                isDraggingAppList = false
                didCancelAppListDragChildren = false
                appListDragVelocityTracker?.recycle()
                appListDragVelocityTracker = VelocityTracker.obtain().apply {
                    addMovement(event)
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                appListDragVelocityTracker?.addMovement(event)
                val deltaX = event.rawX - appListDragStartX
                val deltaY = event.rawY - appListDragStartY
                if (!isDraggingAppList) {
                    if (!isAppListDragGesture(deltaX, deltaY)) return false

                    isDraggingAppList = true
                    cancelChildGesturesForAppListDrag(event)
                    prepareAppListDrag()
                }

                applyAppListDrag(deltaY)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDraggingAppList) {
                    cancelAppListDrag()
                    return false
                }

                appListDragVelocityTracker?.addMovement(event)
                appListDragVelocityTracker?.computeCurrentVelocity(1_000)
                val velocityY = appListDragVelocityTracker?.yVelocity ?: 0f
                val deltaY = event.rawY - appListDragStartY
                settleAppListDrag(shouldComplete = shouldCompleteAppListDrag(deltaY, velocityY))
                cancelAppListDrag()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (isDraggingAppList) {
                    settleAppListDrag(shouldComplete = false)
                }
                cancelAppListDrag()
                return false
            }
        }
        return false
    }

    private fun canHandleAppListDrag(event: MotionEvent): Boolean {
        if (
            isAppPickerVisible ||
            isSettingsVisible ||
            isNotesVisible ||
            isCalendarVisible ||
            isTodayVisible ||
            isScreenTimeVisible ||
            isNoteEditorVisible ||
            isEditMode ||
            activePageSwipeTarget != null
        ) {
            return false
        }
        val pageAboveHome = currentPageArrangement.pageAt(PagePosition.Up)
        if (pageAboveHome != null && isPageEnabled(pageAboveHome)) {
            return false
        }

        val isTouchInShortcutList = isTouchInsideView(binding.shortcutList, event.rawX, event.rawY)
        return !isTouchInShortcutList || !binding.shortcutList.canScrollVertically(-1)
    }

    private fun isAppListDragGesture(deltaX: Float, deltaY: Float): Boolean {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        return deltaY >= touchSlop &&
            deltaY >= kotlin.math.abs(deltaX) * PAGE_SWIPE_AXIS_RATIO
    }

    private fun prepareAppListDrag() {
        appListMode = AppListMode.LaunchApp
        availableApps = loadCachedLaunchableApps()
        binding.appSearchInput.text?.clear()
        renderFilteredApps(query = "")
        binding.appPickerRoot.animate().cancel()
        binding.homeContent.animate().cancel()
        binding.homeTransitionDimOverlay.animate().cancel()
        binding.homeTransitionDimOverlay.visibility = View.VISIBLE
        binding.appPickerRoot.visibility = View.VISIBLE
        binding.appPickerRoot.alpha = 0f
        binding.appPickerRoot.translationY = -APP_LIST_ENTER_OFFSET_DP.dp.toFloat()
        applyAppListHomeTreatment(progress = 0f)
    }

    private fun applyAppListDrag(deltaY: Float) {
        val progress = (deltaY / SWIPE_DOWN_DISTANCE_DP.dp).coerceIn(0f, 1f)
        binding.appPickerRoot.alpha = progress
        binding.appPickerRoot.translationY = -APP_LIST_ENTER_OFFSET_DP.dp * (1f - progress)
        applyAppListHomeTreatment(progress)
    }

    private fun shouldCompleteAppListDrag(deltaY: Float, velocityY: Float): Boolean {
        return deltaY > SWIPE_DOWN_DISTANCE_DP.dp || velocityY > SWIPE_DOWN_VELOCITY_DP.dp
    }

    private fun settleAppListDrag(shouldComplete: Boolean) {
        binding.appPickerRoot.animate().cancel()
        if (shouldComplete) {
            isAppPickerVisible = true
            updateLauncherLayerVisibility()
            applyAppListHomeTreatment(progress = 1f)
            binding.appPickerRoot.visibility = View.VISIBLE
            binding.appPickerRoot.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(PAGE_SETTLE_MS)
                .setInterpolator(DecelerateInterpolator())
                .start()
            applyAppListKeyboardPreference()
        } else {
            resetAppListHomeTreatment()
            binding.appPickerRoot.animate()
                .alpha(0f)
                .translationY(-APP_LIST_ENTER_OFFSET_DP.dp.toFloat())
                .setDuration(PAGE_SETTLE_MS)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    if (!isAppPickerVisible) {
                        binding.appPickerRoot.visibility = View.GONE
                        binding.appPickerRoot.alpha = 1f
                        binding.appPickerRoot.translationY = 0f
                        binding.appSearchInput.text?.clear()
                        appPickerAdapter.submitList(emptyList())
                        availableApps = emptyList()
                    }
                }
                .start()
        }
    }

    private fun applyAppListHomeTreatment(progress: Float) {
        val easedProgress = 1f - (1f - progress) * (1f - progress)
        binding.homeTransitionDimOverlay.alpha = APP_LIST_HOME_DIM_ALPHA * easedProgress
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurRadius = APP_LIST_HOME_BLUR_RADIUS_DP.dp * easedProgress
            binding.homeContent.setRenderEffect(
                if (blurRadius > 0f) {
                    RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
                } else {
                    null
                },
            )
        }
    }

    private fun resetAppListHomeTreatment() {
        binding.homeTransitionDimOverlay.animate().cancel()
        binding.homeTransitionDimOverlay.alpha = 0f
        binding.homeTransitionDimOverlay.visibility = View.GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.homeContent.setRenderEffect(null)
        }
    }

    private fun cancelAppListDrag() {
        isDraggingAppList = false
        didCancelAppListDragChildren = false
        appListDragVelocityTracker?.recycle()
        appListDragVelocityTracker = null
    }

    private fun resetInFlightAppListDrag() {
        if (isAppPickerVisible) return

        binding.appPickerRoot.animate().cancel()
        resetAppListHomeTreatment()
        binding.appPickerRoot.visibility = View.GONE
        binding.appPickerRoot.alpha = 1f
        binding.appPickerRoot.translationY = 0f
        binding.appSearchInput.text?.clear()
        appPickerAdapter.submitList(emptyList())
        availableApps = emptyList()
        cancelAppListDrag()
    }

    private fun cancelChildGesturesForAppListDrag(event: MotionEvent) {
        if (didCancelAppListDragChildren) return

        didCancelAppListDragChildren = true
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

    private fun canHandlePageDrag(): Boolean {
        return !isAppPickerVisible &&
            !isSettingsVisible &&
            !isScreenTimeVisible &&
            !isNoteEditorVisible &&
            !isEditMode &&
            !isTodayEditMode
    }

    private fun canEnterEditModeFromLongPress(): Boolean {
        return !isAppPickerVisible && !isScreenTimeVisible && !isTodayVisible && activePageSwipeTarget == null
    }

    private fun detectPageSwipeTarget(deltaX: Float, deltaY: Float): PageSwipeTarget? {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        val isHorizontalDrag = kotlin.math.abs(deltaX) >= touchSlop &&
            kotlin.math.abs(deltaX) >= kotlin.math.abs(deltaY) * PAGE_SWIPE_AXIS_RATIO
        val isVerticalDrag = kotlin.math.abs(deltaY) >= touchSlop &&
            kotlin.math.abs(deltaY) >= kotlin.math.abs(deltaX) * PAGE_SWIPE_AXIS_RATIO
        val direction = when {
            isHorizontalDrag && deltaX < 0 -> PageSwipeDirection.Left
            isHorizontalDrag && deltaX > 0 -> PageSwipeDirection.Right
            isVerticalDrag && deltaY < 0 -> PageSwipeDirection.Up
            isVerticalDrag && deltaY > 0 -> PageSwipeDirection.Down
            else -> return null
        }
        val visiblePage = currentVisiblePage()
        if (visiblePage != null) {
            val position = currentPageArrangement.positionOf(visiblePage)
            return if (direction == position.toSwipeDirection()) {
                PageSwipeTarget(
                    page = visiblePage,
                    position = position,
                    isReturningHome = true,
                )
            } else {
                null
            }
        }

        val position = when (direction.opposite()) {
            PageSwipeDirection.Up -> PagePosition.Up
            PageSwipeDirection.Left -> PagePosition.Left
            PageSwipeDirection.Right -> PagePosition.Right
            PageSwipeDirection.Down -> PagePosition.Down
        }
        val page = currentPageArrangement.pageAt(position) ?: return null
        return page.takeIf(::isPageEnabled)?.let {
            PageSwipeTarget(
                page = it,
                position = position,
                isReturningHome = false,
            )
        }
    }

    private fun preparePageDrag(target: PageSwipeTarget) {
        binding.homeContent.animate().cancel()
        binding.notesRoot.animate().cancel()
        binding.calendarRoot.animate().cancel()
        binding.todayRoot.animate().cancel()
        val pageRoot = pageRoot(target.page)
        val pageOffset = pageOffset(target.position)
        if (target.isReturningHome) {
            setPageAxisTranslation(binding.homeContent, target.isVertical, -pageOffset)
            binding.homeContent.visibility = View.VISIBLE
        } else {
            setPageAxisTranslation(pageRoot, target.isVertical, pageOffset)
            pageRoot.visibility = View.VISIBLE
        }
    }

    private fun applyPageDrag(target: PageSwipeTarget, deltaX: Float, deltaY: Float) {
        val pageSize = if (target.isVertical) pageHeight() else pageWidth()
        val rawDrag = if (target.isVertical) deltaY else deltaX
        val drag = when (target.expectedGestureDirection) {
            PageSwipeDirection.Left,
            PageSwipeDirection.Up -> rawDrag.coerceIn(-pageSize, 0f)
            PageSwipeDirection.Right,
            PageSwipeDirection.Down -> rawDrag.coerceIn(0f, pageSize)
        }
        val pageRoot = pageRoot(target.page)
        val pageOffset = pageOffset(target.position)
        if (target.isReturningHome) {
            setPageAxisTranslation(pageRoot, target.isVertical, drag)
            setPageAxisTranslation(binding.homeContent, target.isVertical, -pageOffset + drag)
        } else {
            setPageAxisTranslation(binding.homeContent, target.isVertical, drag)
            setPageAxisTranslation(pageRoot, target.isVertical, pageOffset + drag)
        }
    }

    private fun shouldCompletePageDrag(
        target: PageSwipeTarget,
        deltaX: Float,
        deltaY: Float,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        val pageSize = if (target.isVertical) pageHeight() else pageWidth()
        val dragDistance = if (target.isVertical) deltaY else deltaX
        val distancePasses = kotlin.math.abs(dragDistance) > pageSize * PAGE_SWIPE_COMPLETE_FRACTION
        val velocity = if (target.isVertical) velocityY else velocityX
        val velocityPasses = when (target.expectedGestureDirection) {
            PageSwipeDirection.Left,
            PageSwipeDirection.Up -> velocity < -PAGE_SWIPE_COMPLETE_VELOCITY
            PageSwipeDirection.Right,
            PageSwipeDirection.Down -> velocity > PAGE_SWIPE_COMPLETE_VELOCITY
        }
        return distancePasses || velocityPasses
    }

    private fun settlePageDrag(target: PageSwipeTarget, shouldComplete: Boolean) {
        val pageRoot = pageRoot(target.page)
        val pageOffset = pageOffset(target.position)
        val outgoing = if (target.isReturningHome) pageRoot else binding.homeContent
        val incoming = if (target.isReturningHome) binding.homeContent else pageRoot
        val outgoingEnd = when {
            !shouldComplete -> 0f
            target.isReturningHome -> pageOffset
            else -> -pageOffset
        }
        val incomingEnd = when {
            shouldComplete -> 0f
            target.isReturningHome -> -pageOffset
            else -> pageOffset
        }
        animateConfiguredPagePair(
            isVertical = target.isVertical,
            outgoing = outgoing,
            incoming = incoming,
            outgoingEnd = outgoingEnd,
            incomingEnd = incomingEnd,
        ) {
            if (target.isReturningHome) {
                setPageVisible(target.page, !shouldComplete)
                if (shouldComplete) {
                    pageRoot.visibility = View.GONE
                    resetPageTranslation(pageRoot)
                } else {
                    binding.homeContent.visibility = View.GONE
                    resetPageTranslation(binding.homeContent)
                }
            } else {
                setPageVisible(target.page, shouldComplete)
                if (shouldComplete) {
                    binding.homeContent.visibility = View.GONE
                    onPageBecameVisible(target.page)
                } else {
                    pageRoot.visibility = View.GONE
                    resetPageTranslation(pageRoot)
                }
            }
        }
    }

    private fun animateConfiguredPagePair(
        isVertical: Boolean,
        outgoing: View,
        incoming: View,
        outgoingEnd: Float,
        incomingEnd: Float,
        onEnd: () -> Unit,
    ) {
        if (isVertical) {
            animateVerticalPagePair(outgoing, incoming, outgoingEnd, incomingEnd, onEnd)
        } else {
            animatePagePair(outgoing, incoming, outgoingEnd, incomingEnd, onEnd)
        }
    }

    private fun pageOffset(position: PagePosition): Float {
        return when (position) {
            PagePosition.Up -> -pageHeight()
            PagePosition.Left -> -pageWidth()
            PagePosition.Right -> pageWidth()
            PagePosition.Down -> pageHeight()
        }
    }

    private fun setPageAxisTranslation(view: View, isVertical: Boolean, value: Float) {
        if (isVertical) {
            view.translationY = value
            view.translationX = 0f
        } else {
            view.translationX = value
            view.translationY = 0f
        }
    }

    private fun resetPageTranslation(view: View) {
        view.translationX = 0f
        view.translationY = 0f
    }

    private fun currentVisiblePage(): LauncherPage? {
        return when {
            isNotesVisible -> LauncherPage.Notes
            isCalendarVisible -> LauncherPage.Calendar
            isTodayVisible -> LauncherPage.Today
            else -> null
        }
    }

    private fun isPageEnabled(page: LauncherPage): Boolean {
        return when (page) {
            LauncherPage.Notes -> binding.showNotesPageSwitch.isChecked
            LauncherPage.Today -> binding.showTodayPageSwitch.isChecked
            LauncherPage.Calendar -> binding.showCalendarPageSwitch.isChecked
        }
    }

    private fun pageRoot(page: LauncherPage): View {
        return when (page) {
            LauncherPage.Notes -> binding.notesRoot
            LauncherPage.Today -> binding.todayRoot
            LauncherPage.Calendar -> binding.calendarRoot
        }
    }

    private fun setPageVisible(page: LauncherPage, isVisible: Boolean) {
        when (page) {
            LauncherPage.Notes -> isNotesVisible = isVisible
            LauncherPage.Today -> isTodayVisible = isVisible
            LauncherPage.Calendar -> isCalendarVisible = isVisible
        }
    }

    private fun onPageBecameVisible(page: LauncherPage) {
        when (page) {
            LauncherPage.Calendar -> onCalendarPageVisible()
            LauncherPage.Today,
            LauncherPage.Notes -> Unit
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

    private fun animateVerticalPagePair(
        outgoing: View,
        incoming: View,
        outgoingEnd: Float,
        incomingEnd: Float,
        onEnd: () -> Unit,
    ) {
        outgoing.animate()
            .translationY(outgoingEnd)
            .setDuration(PAGE_SETTLE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        incoming.animate()
            .translationY(incomingEnd)
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

    private fun pageHeight(): Float {
        return (binding.homeRoot.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels).toFloat()
    }

    private fun renderHomeState(state: HomeUiState) {
        val shouldScrollToBottom = state.shortcuts.size > renderedShortcutCount
        renderedShortcutCount = state.shortcuts.size
        val visibleShortcuts = state.shortcuts.take(state.maxShortcuts)
        shortcutAdapter.shortcutTextAlignment = state.shortcutTextAlignment
        shortcutAdapter.submitList(visibleShortcuts) {
            if (shouldScrollToBottom && visibleShortcuts.isNotEmpty()) {
                binding.shortcutList.scrollToPosition(visibleShortcuts.lastIndex)
            }
        }
        currentNotes = state.notes
        noteAdapter.submitList(currentNotes)
        binding.notesList.visibility = if (currentNotes.isEmpty()) View.GONE else View.VISIBLE
        binding.notesEmpty.visibility = if (currentNotes.isEmpty()) View.VISIBLE else View.GONE
        currentTrashedNotes = state.trashedNotes
        trashNoteAdapter.submitList(currentTrashedNotes)
        binding.noteTrashList.visibility = if (currentTrashedNotes.isEmpty()) View.GONE else View.VISIBLE
        binding.noteTrashEmpty.visibility = if (currentTrashedNotes.isEmpty()) View.VISIBLE else View.GONE
        binding.notesTrashCount.text = resources.getQuantityString(
            R.plurals.notes_trash_count,
            currentTrashedNotes.size,
            currentTrashedNotes.size,
        )
        if (todayWidgets.any { it.type == TodayWidgetType.PinnedNote }) {
            renderTodayWidgets()
        }
        binding.dateText.visibility = if (state.showDate) View.VISIBLE else View.GONE
        binding.clockView.setDisplayMode(state.clockDisplayMode)
        if (binding.showDateSwitch.isChecked != state.showDate) {
            binding.showDateSwitch.isChecked = state.showDate
        }
        val isDigitalClock = state.clockDisplayMode == ClockDisplayMode.Digital
        if (binding.defaultDigitalClockSwitch.isChecked != isDigitalClock) {
            binding.defaultDigitalClockSwitch.isChecked = isDigitalClock
        }
        currentLeftQuickAccess = state.leftQuickAccess
        currentRightQuickAccess = state.rightQuickAccess
        currentQuickAccessPosition = state.quickAccessPosition
        renderQuickAccess()
        val wallpaperDimPercent = state.wallpaperDimPercent.coerceIn(0, 100)
        binding.wallpaperDimOverlay.alpha = wallpaperDimPercent / 100f
        if (binding.wallpaperDimSlider.value.toInt() != wallpaperDimPercent) {
            binding.wallpaperDimSlider.value = wallpaperDimPercent.toFloat()
        }
        binding.wallpaperDimValue.text = getString(R.string.percentage_value, wallpaperDimPercent)
        renderShortcutTextAlignmentOptions(state.shortcutTextAlignment)
        if (binding.maxShortcutsSlider.value.toInt() != state.maxShortcuts) {
            binding.maxShortcutsSlider.value = state.maxShortcuts.toFloat()
        }
        binding.maxShortcutsValue.text = getString(R.string.integer_value, state.maxShortcuts)
        openAppListKeyboardAutomatically = state.openAppListKeyboardAutomatically
        if (
            binding.openAppListKeyboardAutomaticallySwitch.isChecked !=
            state.openAppListKeyboardAutomatically
        ) {
            binding.openAppListKeyboardAutomaticallySwitch.isChecked =
                state.openAppListKeyboardAutomatically
        }
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
        if (binding.showTodayPageSwitch.isChecked != state.showTodayPage) {
            binding.showTodayPageSwitch.isChecked = state.showTodayPage
        }
        currentPageArrangement = state.pageArrangement
        if (binding.pageArrangementView.arrangement != state.pageArrangement) {
            binding.pageArrangementView.arrangement = state.pageArrangement
        }
        val enabledPages = buildSet {
            if (state.showNotesPage) add(LauncherPage.Notes)
            if (state.showTodayPage) add(LauncherPage.Today)
            if (state.showCalendarPage) add(LauncherPage.Calendar)
        }
        if (binding.pageArrangementView.enabledPages != enabledPages) {
            binding.pageArrangementView.enabledPages = enabledPages
        }
        currentOpenScreenTimeGesture = state.openScreenTimeGesture
        currentLockScreenGesture = state.lockScreenGesture
        binding.openScreenTimeGestureValue.text = gestureLabel(state.openScreenTimeGesture)
        binding.lockScreenGestureValue.text = gestureLabel(state.lockScreenGesture)
        currentSelectedCalendarIds = state.selectedCalendarIds
        currentBlockedAppPackageNames = state.blockedAppPackageNames
        currentAppBudgetMinutesByPackage = state.appBudgetMinutesByPackage
        currentExcludedScreenTimePackageNames = state.excludedScreenTimePackageNames
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
        if (!state.showTodayPage && isTodayVisible) {
            hideTodayPage()
        }
        renderCalendarSelection()
        renderAppBlockingSelection()
        renderAppBudgetsSelection()
        renderScreenTimeExclusionsSelection()
        if (isScreenTimeVisible) {
            refreshScreenTime()
        }
        if (isCalendarVisible && hasCalendarPermission()) {
            refreshCalendarEvents()
        }
        refreshTodayWidgets()
        val quickAccessVisibility = if (
            state.leftQuickAccess != null || state.rightQuickAccess != null
        ) View.VISIBLE else View.GONE
        if (binding.quickAccessBar.visibility != quickAccessVisibility) {
            binding.quickAccessBar.visibility = quickAccessVisibility
            ViewCompat.requestApplyInsets(binding.homeRoot)
        }
    }

    private fun bindCurrentDate() {
        binding.dateText.text = DateFormat.getDateInstance(
            DateFormat.FULL,
            resources.configuration.locales[0],
        ).format(Date())
    }

    private fun configureSystemInsets() {
        val topInsetRoots = listOf(
            binding.homeContent,
            binding.calendarRoot,
            binding.notesRoot,
            binding.noteTrashRoot,
            binding.todayRoot,
            binding.screenTimeRoot,
            binding.editControls,
            binding.settingsRoot,
        )
        val topInsetRootBasePaddings = topInsetRoots.associateWith { view ->
            intArrayOf(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
        }
        val safeAreaRoots = listOf(
            binding.appPickerRoot,
            binding.appBlockPromptRoot,
            binding.gesturePickerRoot,
        )
        val safeAreaRootBasePaddings = safeAreaRoots.associateWith { view ->
            intArrayOf(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
        }
        val shortcutBaseBottomPadding = binding.shortcutList.paddingBottom
        val quickAccessBaseBottomPadding = binding.quickAccessBar.paddingBottom
        val notesListBaseBottomPadding = binding.notesList.paddingBottom
        val noteTrashListBaseBottomPadding = binding.noteTrashList.paddingBottom
        val calendarEventListBaseBottomPadding = binding.calendarEventList.paddingBottom
        val screenTimeScrollBaseBottomPadding = binding.screenTimeScroll.paddingBottom
        val settingsScrollBaseBottomPadding = binding.settingsScroll.paddingBottom
        val editControlsBaseHeight = binding.editControls.layoutParams.height
        val noteEditorBaseLeftPadding = binding.noteEditorRoot.paddingLeft
        val noteEditorBaseTopPadding = binding.noteEditorRoot.paddingTop
        val noteEditorBaseRightPadding = binding.noteEditorRoot.paddingRight
        val noteEditorBaseBottomPadding = binding.noteEditorRoot.paddingBottom
        val noteUndoBaseBottomMargin =
            (binding.noteUndoContainer.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin

        ViewCompat.setOnApplyWindowInsetsListener(binding.homeRoot) { _, insets ->
            val safeDrawing = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(safeDrawing.bottom, ime.bottom)
            topInsetRootBasePaddings.forEach { (view, basePadding) ->
                view.updatePadding(
                    left = basePadding[0] + safeDrawing.left,
                    top = basePadding[1] + safeDrawing.top,
                    right = basePadding[2] + safeDrawing.right,
                    bottom = basePadding[3],
                )
            }
            safeAreaRootBasePaddings.forEach { (view, basePadding) ->
                view.updatePadding(
                    left = basePadding[0] + safeDrawing.left,
                    top = basePadding[1] + safeDrawing.top,
                    right = basePadding[2] + safeDrawing.right,
                    bottom = basePadding[3] + safeDrawing.bottom,
                )
            }
            binding.editControls.updateLayoutParams<ViewGroup.LayoutParams> {
                height = editControlsBaseHeight + safeDrawing.top
            }
            binding.noteEditorRoot.updatePadding(
                left = noteEditorBaseLeftPadding + safeDrawing.left,
                top = noteEditorBaseTopPadding + safeDrawing.top,
                right = noteEditorBaseRightPadding + safeDrawing.right,
                bottom = noteEditorBaseBottomPadding + safeDrawing.bottom,
            )
            binding.noteUndoContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = noteUndoBaseBottomMargin + bottomInset
            }
            binding.shortcutList.updatePadding(
                bottom = shortcutBaseBottomPadding +
                    if (binding.quickAccessBar.isVisible) 0 else safeDrawing.bottom,
            )
            binding.quickAccessBar.updatePadding(bottom = quickAccessBaseBottomPadding + safeDrawing.bottom)
            binding.notesList.updatePadding(bottom = notesListBaseBottomPadding + safeDrawing.bottom)
            binding.noteTrashList.updatePadding(bottom = noteTrashListBaseBottomPadding + safeDrawing.bottom)
            binding.calendarEventList.updatePadding(bottom = calendarEventListBaseBottomPadding + safeDrawing.bottom)
            binding.screenTimeScroll.updatePadding(bottom = screenTimeScrollBaseBottomPadding + safeDrawing.bottom)
            binding.settingsScroll.updatePadding(bottom = settingsScrollBaseBottomPadding + bottomInset)
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
                    binding.appBudgetsSearchInput.hasFocus() ||
                    isScreenTimeExclusionsExpanded &&
                    binding.screenTimeExclusionsSearchInput.hasFocus()
                )
    }

    private fun scrollSettingsToFocusedSearch() {
        binding.settingsScroll.post {
            val focusedSearch = when {
                binding.appBudgetsSearchInput.hasFocus() -> binding.appBudgetsSearchInput
                binding.screenTimeExclusionsSearchInput.hasFocus() -> binding.screenTimeExclusionsSearchInput
                else -> binding.appBlockingSearchInput
            }
            val targetTop = (focusedSearch.top - SETTINGS_KEYBOARD_SCROLL_TOP_OFFSET_DP.dp)
                .coerceAtLeast(0)
            binding.settingsScroll.smoothScrollTo(0, targetTop)
        }
    }

    private fun configureSettings() {
        binding.settingsBackButton.setOnClickListener {
            showSettingsPage(SettingsPage.Index)
        }
        binding.settingsAppearanceCategory.setOnClickListener {
            showSettingsPage(SettingsPage.Appearance)
        }
        binding.settingsNotesCategory.setOnClickListener {
            showSettingsPage(SettingsPage.Notes)
        }
        binding.settingsCalendarCategory.setOnClickListener {
            showSettingsPage(SettingsPage.Calendar)
        }
        binding.settingsGesturesCategory.setOnClickListener {
            showSettingsPage(SettingsPage.Gestures)
        }
        binding.settingsScreenTimeCategory.setOnClickListener {
            showSettingsPage(SettingsPage.ScreenTime)
        }
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
        binding.leftQuickAccessRow.setOnClickListener {
            showQuickAccessAppPicker(left = true)
        }
        binding.rightQuickAccessRow.setOnClickListener {
            showQuickAccessAppPicker(left = false)
        }
        binding.quickAccessPositionRow.setOnClickListener {
            showQuickAccessPositionPicker()
        }
        binding.pageArrangementView.setLabels(
            pageLabels = mapOf(
                LauncherPage.Notes to getString(R.string.notes_page_title),
                LauncherPage.Today to getString(R.string.today_page_title),
                LauncherPage.Calendar to getString(R.string.calendar_page_title),
            ),
            homeLabel = getString(R.string.page_arrangement_home),
            fixedLabel = getString(R.string.page_arrangement_fixed),
            enabledLabel = getString(R.string.page_arrangement_enabled),
            disabledLabel = getString(R.string.page_arrangement_disabled),
        )
        binding.pageArrangementView.onArrangementChanged = viewModel::setPageArrangement
        binding.pageArrangementView.onPageEnabledChanged = { page, isEnabled ->
            when (page) {
                LauncherPage.Notes -> viewModel.setShowNotesPage(isEnabled)
                LauncherPage.Today -> viewModel.setShowTodayPage(isEnabled)
                LauncherPage.Calendar -> handleCalendarPageSettingChanged(isEnabled)
            }
        }
        binding.resetPageArrangementButton.setOnClickListener {
            viewModel.setPageArrangement(PageArrangement.Default)
        }
        binding.wallpaperDimSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.setWallpaperDimPercent(value.toInt())
            }
        }
        binding.shortcutAlignLeftOption.setOnClickListener {
            viewModel.setShortcutTextAlignment(ShortcutTextAlignment.Left)
        }
        binding.shortcutAlignCenterOption.setOnClickListener {
            viewModel.setShortcutTextAlignment(ShortcutTextAlignment.Center)
        }
        binding.shortcutAlignRightOption.setOnClickListener {
            viewModel.setShortcutTextAlignment(ShortcutTextAlignment.Right)
        }
        binding.maxShortcutsSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.setMaxShortcuts(value.toInt())
            }
        }
        binding.openAppListKeyboardAutomaticallySwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setOpenAppListKeyboardAutomatically(isChecked)
        }
        binding.openAppListKeyboardAutomaticallyRow.setOnClickListener {
            viewModel.setOpenAppListKeyboardAutomatically(
                !binding.openAppListKeyboardAutomaticallySwitch.isChecked,
            )
        }
        binding.openScreenTimeGestureRow.setOnClickListener {
            showGesturePicker(GestureAction.OpenScreenTime, currentOpenScreenTimeGesture)
        }
        binding.lockScreenGestureRow.setOnClickListener {
            showGesturePicker(GestureAction.LockScreen, currentLockScreenGesture)
        }
        binding.gesturePickerRoot.setOnClickListener {
            hideGesturePicker()
        }
        binding.gesturePickerPanel.setOnClickListener {
            // Consume clicks inside the picker so only the scrim closes it.
        }
        binding.gestureNoneOption.setOnClickListener {
            handleGesturePickerSelection(LauncherGesture.None)
        }
        binding.gestureTwoFingerSwipeDownOption.setOnClickListener {
            handleGesturePickerSelection(LauncherGesture.TwoFingerSwipeDown)
        }
        binding.gestureDoubleTapOption.setOnClickListener {
            handleGesturePickerSelection(LauncherGesture.DoubleTap)
        }
        binding.showNotesPageSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowNotesPage(isChecked)
        }
        binding.showNotesPageRow.setOnClickListener {
            viewModel.setShowNotesPage(!binding.showNotesPageSwitch.isChecked)
        }
        binding.notesTrashRow.setOnClickListener {
            showNoteTrash()
        }
        binding.showCalendarPageSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isRenderingSettingsState) {
                handleCalendarPageSettingChanged(isChecked)
            }
        }
        binding.showCalendarPageRow.setOnClickListener {
            handleCalendarPageSettingChanged(!binding.showCalendarPageSwitch.isChecked)
        }
        binding.showTodayPageSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowTodayPage(isChecked)
        }
        binding.showTodayPageRow.setOnClickListener {
            viewModel.setShowTodayPage(!binding.showTodayPageSwitch.isChecked)
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
        binding.screenTimeExclusionsHeaderRow.setOnClickListener {
            isScreenTimeExclusionsExpanded = !isScreenTimeExclusionsExpanded
            if (isScreenTimeExclusionsExpanded && blockableApps.isEmpty()) {
                refreshBlockableApps()
            } else {
                renderScreenTimeExclusionsSelection()
            }
        }
        binding.screenTimeExclusionsSearchInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                    renderScreenTimeExclusionsSelection()
                }

                override fun afterTextChanged(text: Editable?) = Unit
            },
        )
        binding.screenTimeExclusionsSearchInput.setOnFocusChangeListener { _, hasFocus ->
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

    private fun showSettingsPage(page: SettingsPage) {
        if (currentSettingsPage == SettingsPage.ScreenTime && page != SettingsPage.ScreenTime) {
            hideSettingsSearchKeyboard()
        }
        currentSettingsPage = page

        val isIndex = page == SettingsPage.Index
        binding.settingsTitle.setText(page.titleRes)
        binding.settingsBackButton.visibility = if (isIndex) View.INVISIBLE else View.VISIBLE
        binding.settingsBackButton.isClickable = !isIndex
        binding.settingsIndex.isVisible = isIndex
        binding.settingsAppearancePage.isVisible = page == SettingsPage.Appearance
        binding.settingsNotesPage.isVisible = page == SettingsPage.Notes
        binding.settingsCalendarPage.isVisible = page == SettingsPage.Calendar
        binding.settingsGesturesPage.isVisible = page == SettingsPage.Gestures
        binding.settingsScreenTimePage.isVisible = page == SettingsPage.ScreenTime
        binding.settingsScroll.scrollTo(0, 0)

        when (page) {
            SettingsPage.Calendar -> refreshCalendars()
            SettingsPage.ScreenTime -> {
                if (blockableApps.isEmpty()) {
                    refreshBlockableApps()
                } else {
                    renderAppBlockingSelection()
                    renderAppBudgetsSelection()
                    renderScreenTimeExclusionsSelection()
                }
            }
            else -> Unit
        }
    }

    private fun hideSettingsSearchKeyboard() {
        val focusedSearch = when {
            binding.appBlockingSearchInput.hasFocus() -> binding.appBlockingSearchInput
            binding.appBudgetsSearchInput.hasFocus() -> binding.appBudgetsSearchInput
            binding.screenTimeExclusionsSearchInput.hasFocus() -> binding.screenTimeExclusionsSearchInput
            else -> null
        } ?: return
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(focusedSearch.windowToken, 0)
        focusedSearch.clearFocus()
    }

    private fun handleCalendarPageSettingChanged(isEnabled: Boolean) {
        viewModel.setShowCalendarPage(isEnabled)
        if (isEnabled && !hasCalendarPermission()) {
            viewModel.markCalendarPermissionRequested()
            requestCalendarPermission.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private fun showGesturePicker(action: GestureAction, currentGesture: LauncherGesture) {
        currentGesturePickerAction = action
        binding.gesturePickerTitle.text = actionLabel(action)
        renderGesturePickerOptions(currentGesture)
        binding.gesturePickerRoot.alpha = 0f
        binding.gesturePickerRoot.visibility = View.VISIBLE
        binding.gesturePickerRoot.animate()
            .alpha(1f)
            .setDuration(SETTINGS_FADE_MS)
            .start()
    }

    private fun hideGesturePicker() {
        currentGesturePickerAction = null
        binding.gesturePickerRoot.animate().cancel()
        binding.gesturePickerRoot.visibility = View.GONE
        binding.gesturePickerRoot.alpha = 1f
    }

    private fun handleGesturePickerSelection(selectedGesture: LauncherGesture) {
        val action = currentGesturePickerAction ?: return
        conflictActionFor(action, selectedGesture)?.let { previousAction ->
            Toast.makeText(
                this,
                getString(
                    R.string.gesture_conflict_warning,
                    gestureLabel(selectedGesture),
                    actionLabel(previousAction),
                ),
                Toast.LENGTH_LONG,
            ).show()
        }
        viewModel.setGesture(action, selectedGesture)
        hideGesturePicker()
    }

    private fun renderGesturePickerOptions(currentGesture: LauncherGesture) {
        renderGesturePickerOption(binding.gestureNoneOption, LauncherGesture.None, currentGesture)
        renderGesturePickerOption(
            binding.gestureTwoFingerSwipeDownOption,
            LauncherGesture.TwoFingerSwipeDown,
            currentGesture,
        )
        renderGesturePickerOption(binding.gestureDoubleTapOption, LauncherGesture.DoubleTap, currentGesture)
    }

    private fun renderGesturePickerOption(optionView: TextView, gesture: LauncherGesture, currentGesture: LauncherGesture) {
        val isSelected = gesture == currentGesture
        optionView.setTextColor(getColor(if (isSelected) R.color.launcher_text else R.color.launcher_text_secondary))
        optionView.background = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = 8.dp.toFloat()
            setStroke(
                1.dp,
                getColor(if (isSelected) R.color.launcher_text else R.color.settings_option_divider),
            )
        }
    }

    private fun conflictActionFor(action: GestureAction, gesture: LauncherGesture): GestureAction? {
        if (gesture == LauncherGesture.None) return null
        return GestureAction.entries.firstOrNull { otherAction ->
            otherAction != action && gestureForAction(otherAction) == gesture
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
        binding.leftQuickAccessButton.setOnClickListener {
            currentLeftQuickAccess?.let(::launchQuickAccessTarget)
        }
        binding.rightQuickAccessButton.setOnClickListener {
            currentRightQuickAccess?.let(::launchQuickAccessTarget)
        }
    }

    private fun renderQuickAccess() {
        renderQuickAccessButton(binding.leftQuickAccessButton, currentLeftQuickAccess)
        renderQuickAccessButton(binding.rightQuickAccessButton, currentRightQuickAccess)
        binding.leftQuickAccessValue.text = quickAccessSummary(currentLeftQuickAccess)
        binding.rightQuickAccessValue.text = quickAccessSummary(currentRightQuickAccess)
        binding.quickAccessPositionValue.text = quickAccessPositionLabel(currentQuickAccessPosition)
        binding.quickAccessSpacer.visibility = if (currentQuickAccessPosition == QuickAccessPosition.SplitEdges) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.quickAccessBar.gravity = when (currentQuickAccessPosition) {
            QuickAccessPosition.BothRight -> android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
            QuickAccessPosition.BothLeft -> android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
            QuickAccessPosition.BothCenter -> android.view.Gravity.CENTER
            QuickAccessPosition.SplitEdges -> android.view.Gravity.CENTER_VERTICAL
        }
    }

    private fun renderQuickAccessButton(
        button: android.widget.ImageButton,
        target: QuickAccessTarget?,
    ) {
        button.visibility = if (target == null) View.GONE else View.VISIBLE
        if (target == null) return
        button.setImageResource(quickAccessIconResource(target.icon))
        button.contentDescription = target.label
    }

    private fun quickAccessSummary(target: QuickAccessTarget?): String {
        return if (target == null) {
            getString(R.string.quick_access_none)
        } else {
            getString(
                R.string.quick_access_value,
                target.label,
                quickAccessIconLabel(target.icon),
            )
        }
    }

    private fun showQuickAccessPositionPicker() {
        val positions = listOf(
            QuickAccessPosition.BothRight,
            QuickAccessPosition.BothLeft,
            QuickAccessPosition.BothCenter,
            QuickAccessPosition.SplitEdges,
        )
        val labels = positions.map(::quickAccessPositionLabel)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.quick_access_position_choose)
            .setSingleChoiceItems(
                labels.toTypedArray(),
                positions.indexOf(currentQuickAccessPosition),
            ) { dialog, index ->
                viewModel.setQuickAccessPosition(positions[index])
                dialog.dismiss()
            }
            .show()
    }

    private fun quickAccessPositionLabel(position: QuickAccessPosition): String {
        return getString(
            when (position) {
                QuickAccessPosition.BothRight -> R.string.quick_access_position_both_right
                QuickAccessPosition.BothLeft -> R.string.quick_access_position_both_left
                QuickAccessPosition.BothCenter -> R.string.quick_access_position_both_center
                QuickAccessPosition.SplitEdges -> R.string.quick_access_position_split_edges
            },
        )
    }

    private fun showQuickAccessAppPicker(left: Boolean) {
        val slotLabel = getString(if (left) R.string.quick_access_left else R.string.quick_access_right)
        val apps = installedAppsRepository.loadLaunchableApps()
            .distinctBy { it.packageName }
        val labels = listOf(getString(R.string.quick_access_none)) + apps.map { it.label }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.quick_access_choose_app, slotLabel))
            .setItems(labels.toTypedArray()) { _, index ->
                if (index == 0) {
                    viewModel.setQuickAccess(left, null)
                } else {
                    showQuickAccessIconPicker(left, apps[index - 1])
                }
            }
            .show()
    }

    private fun showQuickAccessIconPicker(left: Boolean, shortcut: AppShortcut) {
        val slotLabel = getString(if (left) R.string.quick_access_left else R.string.quick_access_right)
        val icons = QuickAccessIcon.entries
        lateinit var dialog: androidx.appcompat.app.AlertDialog
        val grid = GridLayout(this).apply {
            columnCount = QUICK_ACCESS_ICON_GRID_COLUMNS
            rowCount = (icons.size + columnCount - 1) / columnCount
            setPadding(18.dp, 6.dp, 18.dp, 12.dp)
        }
        icons.forEachIndexed { index, icon ->
            val label = quickAccessIconLabel(icon)
            val button = MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle,
            ).apply {
                layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(index / QUICK_ACCESS_ICON_GRID_COLUMNS),
                    GridLayout.spec(index % QUICK_ACCESS_ICON_GRID_COLUMNS, 1f),
                ).apply {
                    width = 0
                    height = QUICK_ACCESS_ICON_BUTTON_SIZE_DP.dp
                    setMargins(6.dp, 6.dp, 6.dp, 6.dp)
                }
                minWidth = 0
                minHeight = 0
                backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(this@MainActivity, R.color.launcher_text_secondary),
                )
                strokeWidth = 1.dp
                this.icon = ContextCompat.getDrawable(this@MainActivity, quickAccessIconResource(icon))
                iconTint = ColorStateList.valueOf(
                    ContextCompat.getColor(this@MainActivity, R.color.launcher_text),
                )
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                iconPadding = 0
                iconSize = QUICK_ACCESS_ICON_SIZE_DP.dp
                rippleColor = ColorStateList.valueOf(
                    ContextCompat.getColor(this@MainActivity, R.color.settings_option_divider),
                )
                text = null
                contentDescription = label
                ViewCompat.setTooltipText(this, label)
                setOnClickListener {
                    viewModel.setQuickAccess(
                        left = left,
                        target = QuickAccessTarget(
                            label = shortcut.label,
                            packageName = shortcut.packageName,
                            activityName = shortcut.activityName,
                            icon = icon,
                        ),
                    )
                    dialog.dismiss()
                }
            }
            grid.addView(button)
        }
        dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.quick_access_choose_icon, slotLabel))
            .setView(grid)
            .setBackground(
                GradientDrawable().apply {
                    setColor(ContextCompat.getColor(this@MainActivity, R.color.launcher_background))
                    cornerRadius = QUICK_ACCESS_ICON_DIALOG_CORNER_RADIUS_DP.dp.toFloat()
                },
            )
            .create()
        dialog.show()
    }

    private fun quickAccessIconLabel(icon: QuickAccessIcon): String {
        return getString(
            when (icon) {
                QuickAccessIcon.Camera -> R.string.quick_access_icon_camera
                QuickAccessIcon.Notes -> R.string.quick_access_icon_notes
                QuickAccessIcon.Calendar -> R.string.quick_access_icon_calendar
                QuickAccessIcon.Phone -> R.string.quick_access_icon_phone
                QuickAccessIcon.Messages -> R.string.quick_access_icon_messages
                QuickAccessIcon.Todos -> R.string.quick_access_icon_todos
            },
        )
    }

    private fun quickAccessIconResource(icon: QuickAccessIcon): Int {
        return when (icon) {
            QuickAccessIcon.Camera -> R.drawable.ic_camera_line
            QuickAccessIcon.Notes -> R.drawable.ic_note_line
            QuickAccessIcon.Calendar -> R.drawable.ic_calendar_line
            QuickAccessIcon.Phone -> R.drawable.ic_phone_line
            QuickAccessIcon.Messages -> R.drawable.ic_messages_line
            QuickAccessIcon.Todos -> R.drawable.ic_todos_line
        }
    }

    private fun launchQuickAccessTarget(target: QuickAccessTarget) {
        val component = ComponentName(target.packageName, target.activityName)
        val isAvailable = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getActivityInfo(component, PackageManager.ComponentInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getActivityInfo(component, 0)
            }
        }.isSuccess
        if (!isAvailable) {
            showQuickAccessUnavailable()
            return
        }
        launchShortcutWithAppBlocking(
            AppShortcut(
                label = target.label,
                packageName = target.packageName,
                activityName = target.activityName,
            ),
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureNotes() {
        binding.addNoteButton.setOnClickListener {
            when {
                noteInputMode == NoteInputMode.Text -> showNoteEditor(note = null)
                voiceNoteRecorder == null -> requestVoiceNoteRecording()
                else -> stopVoiceNoteRecording(save = true)
            }
        }
        binding.addNoteButton.setOnLongClickListener {
            toggleNoteInputMode()
            true
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
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                    noteBulletFormatter.onTextChanged(text, start, before, count)
                }

                override fun afterTextChanged(text: Editable?) {
                    noteBulletFormatter.formatAfterTextChanged(binding.noteEditorInput, text ?: return)
                }
            },
        )
        updateAddNoteButton()

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

    private fun toggleNoteInputMode() {
        if (voiceNoteRecorder != null) return
        noteInputMode = when (noteInputMode) {
            NoteInputMode.Text -> NoteInputMode.Voice
            NoteInputMode.Voice -> NoteInputMode.Text
        }
        performLightHapticFeedback()
        updateAddNoteButton()
    }

    private fun updateAddNoteButton() {
        val icon = when {
            voiceNoteRecorder != null -> R.drawable.ic_stop
            noteInputMode == NoteInputMode.Voice -> R.drawable.ic_mic
            else -> R.drawable.ic_edit
        }
        val description = when {
            voiceNoteRecorder != null -> R.string.stop_voice_note
            noteInputMode == NoteInputMode.Voice -> R.string.start_voice_note
            else -> R.string.add_note
        }
        binding.addNoteButton.setImageResource(icon)
        binding.addNoteButton.contentDescription = getString(description)
        binding.notesTitle.text = when {
            voiceNoteRecorder != null -> getString(R.string.voice_note_recording)
            noteInputMode == NoteInputMode.Voice -> getString(R.string.voice_notes_mode)
            else -> getString(R.string.notes_page_title)
        }
    }

    private fun requestVoiceNoteRecording() {
        if (hasMicrophonePermission()) {
            startVoiceNoteRecording()
        } else {
            requestMicrophonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureToday() {
        binding.addTodayWidgetButton.setOnClickListener {
            showTodayWidgetPicker(it)
        }
        binding.todayWidgetGrid.setOnLongClickListener {
            enterTodayEditMode()
            true
        }
        renderTodayWidgets()
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
        appBlockPromptController.configure()
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
        binding.calendarSelectionHeaderRow.setExpandIcon(isCalendarSelectionExpanded)
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
        renderScreenTimeExclusionsSelection()
    }

    private fun renderAppBlockingSelection() {
        binding.appBlockingCount.text = resources.getQuantityString(
            R.plurals.app_blocking_count,
            currentBlockedAppPackageNames.size,
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
        binding.appBudgetsCount.text = resources.getQuantityString(
            R.plurals.app_budgets_count,
            currentAppBudgetMinutesByPackage.size,
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

    private fun renderScreenTimeExclusionsSelection() {
        binding.screenTimeExclusionsCount.text = resources.getQuantityString(
            R.plurals.screen_time_exclusions_count,
            currentExcludedScreenTimePackageNames.size,
            currentExcludedScreenTimePackageNames.size,
        )
        binding.screenTimeExclusionsExpandIcon.setImageResource(
            if (isScreenTimeExclusionsExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
        )
        binding.screenTimeExclusionsSearchInput.visibility =
            if (isScreenTimeExclusionsExpanded) View.VISIBLE else View.GONE
        binding.screenTimeExclusionsList.visibility =
            if (isScreenTimeExclusionsExpanded) View.VISIBLE else View.GONE
        if (!isScreenTimeExclusionsExpanded) return

        binding.screenTimeExclusionsList.removeAllViews()
        if (blockableApps.isEmpty()) {
            blockableApps = installedAppsRepository.loadLaunchableApps()
                .distinctBy { it.packageName }
        }
        val query = binding.screenTimeExclusionsSearchInput.text?.toString().orEmpty().trim()
        val displayedApps = if (query.isEmpty()) {
            blockableApps.filter { it.packageName in currentExcludedScreenTimePackageNames }
        } else {
            FuzzyAppSearch.filter(blockableApps, query)
        }
        displayedApps.forEach { shortcut ->
            val row = ItemAppBlockSelectionBinding.inflate(
                layoutInflater,
                binding.screenTimeExclusionsList,
                false,
            )
            row.blockedAppName.text = shortcut.label
            row.blockedAppCheckbox.setOnCheckedChangeListener(null)
            row.blockedAppCheckbox.isChecked =
                shortcut.packageName in currentExcludedScreenTimePackageNames
            row.root.setOnClickListener {
                viewModel.setScreenTimeAppExcluded(
                    shortcut.packageName,
                    !row.blockedAppCheckbox.isChecked,
                )
            }
            row.blockedAppCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setScreenTimeAppExcluded(shortcut.packageName, isChecked)
            }
            binding.screenTimeExclusionsList.addView(row.root)
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

    private fun renderShortcutTextAlignmentOptions(selectedAlignment: ShortcutTextAlignment) {
        renderShortcutTextAlignmentOption(
            binding.shortcutAlignLeftOption,
            selectedAlignment,
            ShortcutTextAlignment.Left,
        )
        renderShortcutTextAlignmentOption(
            binding.shortcutAlignCenterOption,
            selectedAlignment,
            ShortcutTextAlignment.Center,
        )
        renderShortcutTextAlignmentOption(
            binding.shortcutAlignRightOption,
            selectedAlignment,
            ShortcutTextAlignment.Right,
        )
    }

    private fun renderShortcutTextAlignmentOption(
        view: TextView,
        selectedAlignment: ShortcutTextAlignment,
        alignment: ShortcutTextAlignment,
    ) {
        val isSelected = selectedAlignment == alignment
        view.alpha = if (isSelected) 1f else 0.68f
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            setStroke(1.dp, getColor(if (isSelected) R.color.launcher_text else R.color.settings_option_divider))
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
        screenTimeWeekUsages = screenTimeRepository.loadCurrentWeekUsage(
            currentExcludedScreenTimePackageNames,
        )
        screenTimeUsages = screenTimeRepository.loadTodayUsage(
            currentExcludedScreenTimePackageNames,
        )
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
        binding.screenTimeHeaderRow.setExpandIcon(isScreenTimeExpanded)
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
        binding.screenTimeTodayTotal.text = formatScreenTimeDuration(todayTotalMillis)
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
        binding.screenTimeIntentionsHeaderRow.setExpandIcon(isScreenTimeIntentionsExpanded)
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
                                text = getString(
                                    R.string.app_budget_usage_progress,
                                    formatScreenTimeDuration(appUsageMillis),
                                    formatScreenTimeDuration(minutes * MILLIS_PER_MINUTE),
                                )
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
            getString(R.string.duration_hours_minutes, hours, minutes)
        } else {
            getString(R.string.duration_minutes, minutes)
        }
    }

    private fun TextView.setExpandIcon(isExpanded: Boolean) {
        setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
            0,
        )
        TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(getColor(R.color.launcher_text)))
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

    private fun refreshTodayWidgets() {
        todayNextEvent = if (hasCalendarPermission()) {
            calendarRepository.loadUpcomingEvents(currentSelectedCalendarIds).firstOrNull()
        } else {
            null
        }
        refreshTodayWeather()
        renderTodayWidgets()
    }

    private fun renderTodayWidgets() {
        binding.todayWidgetGrid.isEditingWidgets = isTodayEditMode
        todayPinnedVoiceWaveform = null
        todayPinnedVoicePlayButton = null
        binding.todayWidgetGrid.removeAllViews()
        if (todayWidgets.isEmpty()) return

        todayWidgets.forEach { widget ->
            val widgetView = when (widget.type) {
                TodayWidgetType.NextEvent -> createNextEventWidgetView(widget)
                TodayWidgetType.Weather -> createWeatherWidgetView(widget)
                TodayWidgetType.NotificationFeed -> createNotificationFeedWidgetView(widget)
                TodayWidgetType.PinnedNote -> createPinnedNoteWidgetView(widget)
            }
            binding.todayWidgetGrid.addView(widgetView)
            binding.todayWidgetGrid.applyGridPosition(
                view = widgetView,
                column = widget.column,
                row = widget.row,
                columnSpan = widget.columnSpan,
                rowSpan = widget.rowSpan,
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createNextEventWidgetView(widget: TodayWidget): View {
        val event = todayNextEvent
        val container = android.widget.FrameLayout(this).apply {
            tag = widget
            background = todayWidgetBackground(isEditing = isTodayEditMode)
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
            isClickable = true
            isLongClickable = true
            setOnClickListener {
                if (!isTodayEditMode) {
                    when {
                        event != null -> openCalendarEventDay(event)
                        !hasCalendarPermission() -> {
                            requestCalendarPermissionFromToday()
                        }
                    }
                }
            }
            setOnLongClickListener {
                if (isTodayEditMode) {
                    showTodayWidgetContextMenu(it, widget)
                } else {
                    enterTodayEditMode()
                }
                true
            }
        }

        container.addView(
            android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                addView(
                    TextView(this@MainActivity).apply {
                        text = getString(R.string.today_next_event_title)
                        setTextColor(getColor(R.color.launcher_text_secondary))
                        textSize = 13f
                        maxLines = 1
                        includeFontPadding = false
                    },
                )
                addView(
                    TextView(this@MainActivity).apply {
                        text = nextEventPrimaryText(event)
                        setTextColor(getColor(R.color.launcher_text))
                        textSize = 18f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        includeFontPadding = false
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            topMargin = 6.dp
                        }
                    },
                )
                addView(
                    TextView(this@MainActivity).apply {
                        text = nextEventSecondaryText(event)
                        setTextColor(getColor(R.color.launcher_text_secondary))
                        textSize = 13f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        includeFontPadding = false
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            topMargin = 6.dp
                        }
                    },
                )
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            },
        )

        if (isTodayEditMode) {
            addTodayResizeIndicator(container)
            configureTodayWidgetDrag(container, widget)
        }
        return container
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createWeatherWidgetView(widget: TodayWidget): View {
        val weather = todayWeather
        val container = android.widget.FrameLayout(this).apply {
            tag = widget
            background = todayWidgetBackground(isEditing = isTodayEditMode)
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
            isClickable = true
            isLongClickable = true
            setOnClickListener {
                if (!isTodayEditMode) {
                    if (hasWeatherLocationPermission()) {
                        refreshTodayWeather(force = true)
                    } else {
                        requestWeatherLocationPermissionFromToday()
                    }
                }
            }
            setOnLongClickListener {
                if (isTodayEditMode) {
                    showTodayWidgetContextMenu(it, widget)
                } else {
                    enterTodayEditMode()
                }
                true
            }
        }

        container.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                addView(
                    TextView(this@MainActivity).apply {
                        text = getString(R.string.today_weather_title)
                        setTextColor(getColor(R.color.launcher_text_secondary))
                        textSize = 13f
                        maxLines = 1
                        includeFontPadding = false
                    },
                )
                addView(
                    TextView(this@MainActivity).apply {
                        text = weatherPrimaryText(weather)
                        setTextColor(getColor(R.color.launcher_text))
                        textSize = 28f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        includeFontPadding = false
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            topMargin = 5.dp
                        }
                    },
                )
                addView(
                    TextView(this@MainActivity).apply {
                        text = weatherSecondaryText(weather)
                        setTextColor(getColor(R.color.launcher_text_secondary))
                        textSize = 13f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        includeFontPadding = false
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            topMargin = 6.dp
                        }
                    },
                )
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            },
        )

        if (isTodayEditMode) {
            addTodayResizeIndicator(container)
            configureTodayWidgetDrag(container, widget)
        }
        return container
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createNotificationFeedWidgetView(widget: TodayWidget): View {
        val displayedNotifications = filteredNotificationsForWidget(widget)
            .take(widget.rowSpan.coerceAtLeast(TodayWidgetGridView.MIN_ROW_SPAN))
        val container = android.widget.FrameLayout(this).apply {
            tag = widget
            background = todayWidgetBackground(isEditing = isTodayEditMode)
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
            isClickable = true
            isLongClickable = true
            setOnClickListener {
                if (!isTodayEditMode && !hasNotificationAccess()) {
                    openNotificationAccessSettings()
                }
            }
            setOnLongClickListener {
                if (isTodayEditMode) {
                    showTodayWidgetContextMenu(it, widget)
                } else {
                    enterTodayEditMode()
                }
                true
            }
        }

        container.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(
                    TextView(this@MainActivity).apply {
                        text = getString(R.string.today_notification_feed_title)
                        setTextColor(getColor(R.color.launcher_text_secondary))
                        textSize = 13f
                        maxLines = 1
                        includeFontPadding = false
                    },
                )
                when {
                    !hasNotificationAccess() -> {
                        addNotificationTextRow(
                            title = getString(R.string.today_notification_access_needed),
                            text = getString(R.string.today_notification_access_action),
                        )
                    }
                    displayedNotifications.isEmpty() -> {
                        addNotificationTextRow(
                            title = getString(R.string.today_notification_feed_empty),
                            text = "",
                        )
                    }
                    else -> {
                        displayedNotifications.forEach { notification ->
                            addNotificationTextRow(
                                title = notification.title,
                                text = notificationSubtitle(notification),
                                notification = notification,
                            )
                        }
                    }
                }
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            },
        )

        if (isTodayEditMode) {
            addTodayResizeIndicator(container)
            configureTodayWidgetDrag(container, widget)
        }
        return container
    }

    private fun LinearLayout.addNotificationTextRow(
        title: String,
        text: String,
        notification: TodayNotificationItem? = null,
    ) {
        addView(
            LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                if (notification != null) {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        if (!isTodayEditMode) {
                            launchNotificationSourceApp(notification)
                        }
                    }
                }
                addView(
                    TextView(this@MainActivity).apply {
                        this.text = title
                        setTextColor(getColor(R.color.launcher_text))
                        textSize = 16f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        includeFontPadding = false
                    },
                )
                if (text.isNotBlank()) {
                    addView(
                        TextView(this@MainActivity).apply {
                            this.text = text
                            setTextColor(getColor(R.color.launcher_text_secondary))
                            textSize = 12f
                            maxLines = 1
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            includeFontPadding = false
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            ).apply {
                                topMargin = 5.dp
                            }
                        },
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                ).apply {
                    topMargin = 8.dp
                }
            },
        )
    }

    private fun launchNotificationSourceApp(notification: TodayNotificationItem) {
        val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
        val componentName = launchIntent?.component ?: launchIntent?.resolveActivity(packageManager)
        if (componentName == null) {
            showQuickAccessUnavailable()
            return
        }
        launchShortcutWithAppBlocking(
            AppShortcut(
                label = notification.appLabel,
                packageName = componentName.packageName,
                activityName = componentName.className,
            ),
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createPinnedNoteWidgetView(widget: TodayWidget): View {
        val pinnedNote = currentNotes.firstOrNull { it.isPinned }
        val container = android.widget.FrameLayout(this).apply {
            tag = widget
            background = todayWidgetBackground(isEditing = isTodayEditMode)
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
            isClickable = true
            isLongClickable = true
            setOnClickListener {
                if (!isTodayEditMode && pinnedNote != null) {
                    if (pinnedNote.audioFileName == null) {
                        showNoteEditor(pinnedNote)
                    } else {
                        toggleVoiceNotePlayback(pinnedNote)
                    }
                }
            }
            setOnLongClickListener {
                if (isTodayEditMode) {
                    showTodayWidgetContextMenu(it, widget)
                } else {
                    enterTodayEditMode()
                }
                true
            }
        }

        container.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                addView(
                    TextView(this@MainActivity).apply {
                        text = getString(R.string.today_pinned_note_title)
                        setTextColor(getColor(R.color.launcher_text_secondary))
                        textSize = 13f
                        maxLines = 1
                        includeFontPadding = false
                    },
                )
                if (pinnedNote?.audioFileName == null) {
                    addView(
                        TextView(this@MainActivity).apply {
                            text = pinnedNote?.displayText() ?: getString(R.string.today_pinned_note_empty)
                            setTextColor(getColor(R.color.launcher_text))
                            textSize = 17f
                            maxLines = (widget.rowSpan * 2).coerceAtLeast(2)
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            includeFontPadding = false
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            ).apply {
                                topMargin = 8.dp
                            }
                        },
                    )
                } else {
                    addView(createPinnedVoiceNotePlaybackRow(pinnedNote))
                }
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            },
        )

        if (isTodayEditMode) {
            addTodayResizeIndicator(container)
            configureTodayWidgetDrag(container, widget)
        }
        return container
    }

    private fun createPinnedVoiceNotePlaybackRow(note: QuickNote): View {
        val isActive = note.id == playingVoiceNoteId
        val isPlaying = isActive && isVoicePlaybackPlaying
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48.dp,
            ).apply {
                topMargin = 8.dp
            }
            val playButton = android.widget.ImageButton(this@MainActivity).apply {
                setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
                contentDescription = getString(if (isPlaying) R.string.pause_voice_note else R.string.play_voice_note)
                background = ColorDrawable(Color.TRANSPARENT)
                setColorFilter(getColor(R.color.launcher_text))
                setPadding(11.dp, 11.dp, 11.dp, 11.dp)
                layoutParams = LinearLayout.LayoutParams(44.dp, 44.dp)
                setOnClickListener {
                    if (!isTodayEditMode) {
                        toggleVoiceNotePlayback(note)
                    }
                }
            }
            val waveform = VoiceWaveformView(this@MainActivity).apply {
                samples = note.audioWaveform
                progressFraction = if (isActive) voicePlaybackProgressFraction else 0f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    32.dp,
                    1f,
                ).apply {
                    marginStart = 6.dp
                }
            }
            addView(playButton)
            addView(waveform)
            addView(
                TextView(this@MainActivity).apply {
                    text = formatVoiceNoteDuration(note.audioDurationMillis)
                    setTextColor(getColor(R.color.launcher_text_secondary))
                    textSize = 13f
                    gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
                    includeFontPadding = false
                    layoutParams = LinearLayout.LayoutParams(48.dp, ViewGroup.LayoutParams.MATCH_PARENT)
                },
            )
            todayPinnedVoicePlayButton = playButton
            todayPinnedVoiceWaveform = waveform
        }
    }

    private fun addTodayResizeIndicator(container: android.widget.FrameLayout) {
        val indicatorColor = getColor(R.color.launcher_text_secondary)
        val indicatorInset = 9.dp
        container.addView(
            View(this).apply {
                background = GradientDrawable().apply {
                    setColor(indicatorColor)
                }
                alpha = TODAY_WIDGET_RESIZE_INDICATOR_ALPHA
                layoutParams = android.widget.FrameLayout.LayoutParams(18.dp, 2.dp).apply {
                    gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                    marginEnd = indicatorInset
                    bottomMargin = indicatorInset
                }
            },
        )
        container.addView(
            View(this).apply {
                background = GradientDrawable().apply {
                    setColor(indicatorColor)
                }
                alpha = TODAY_WIDGET_RESIZE_INDICATOR_ALPHA
                layoutParams = android.widget.FrameLayout.LayoutParams(2.dp, 18.dp).apply {
                    gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                    marginEnd = indicatorInset
                    bottomMargin = indicatorInset
                }
            },
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureTodayWidgetDrag(view: View, widget: TodayWidget) {
        var mode = TodayWidgetDragMode.Move
        var activeWidget = widget
        var startRawX = 0f
        var startRawY = 0f
        var startColumn = widget.column
        var startRow = widget.row
        var startColumnSpan = widget.columnSpan
        var startRowSpan = widget.rowSpan
        var didDrag = false
        view.setOnTouchListener { touchedView, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    activeWidget = (touchedView.tag as? TodayWidget) ?: widget
                    activeWidget = activeWidget.copy(
                        column = binding.todayWidgetGrid.columnForDrag(activeWidget.column, activeWidget.columnSpan),
                        row = binding.todayWidgetGrid.rowForDrag(activeWidget.row, activeWidget.rowSpan),
                    )
                    startRawX = event.rawX
                    startRawY = event.rawY
                    startColumn = activeWidget.column
                    startRow = activeWidget.row
                    startColumnSpan = activeWidget.columnSpan
                    startRowSpan = activeWidget.rowSpan
                    didDrag = false
                    mode = if (
                        event.x >= touchedView.width - TODAY_WIDGET_RESIZE_TOUCH_DP.dp &&
                        event.y >= touchedView.height - TODAY_WIDGET_RESIZE_TOUCH_DP.dp
                    ) {
                        TodayWidgetDragMode.Resize
                    } else {
                        TodayWidgetDragMode.Move
                    }
                    touchedView.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - startRawX
                    val deltaY = event.rawY - startRawY
                    didDrag = didDrag ||
                        kotlin.math.abs(deltaX) > ViewConfiguration.get(this).scaledTouchSlop ||
                        kotlin.math.abs(deltaY) > ViewConfiguration.get(this).scaledTouchSlop
                    val updated = when (mode) {
                        TodayWidgetDragMode.Move -> {
                            val rawColumn = startColumn + binding.todayWidgetGrid.columnDeltaForX(deltaX)
                            val rawRow = startRow + binding.todayWidgetGrid.rowDeltaForY(deltaY)
                            activeWidget.copy(
                                column = binding.todayWidgetGrid.columnForDrag(rawColumn, startColumnSpan),
                                row = binding.todayWidgetGrid.rowForDrag(rawRow, startRowSpan),
                            )
                        }
                        TodayWidgetDragMode.Resize -> {
                            val maxColumnSpan = (TodayWidgetGridView.COLUMN_COUNT - startColumn)
                                .coerceAtLeast(TodayWidgetGridView.MIN_COLUMN_SPAN)
                            val maxRowSpan = (TodayWidgetGridView.ROW_COUNT - startRow)
                                .coerceAtLeast(TodayWidgetGridView.MIN_ROW_SPAN)
                            val columnSpan = (startColumnSpan + binding.todayWidgetGrid.columnDeltaForX(deltaX))
                                .coerceIn(TodayWidgetGridView.MIN_COLUMN_SPAN, maxColumnSpan)
                            val rowSpan = (startRowSpan + binding.todayWidgetGrid.rowDeltaForY(deltaY))
                                .coerceIn(TodayWidgetGridView.MIN_ROW_SPAN, maxRowSpan)
                            activeWidget.copy(columnSpan = columnSpan, rowSpan = rowSpan)
                        }
                    }
                    binding.todayWidgetGrid.applyGridPosition(
                        view = touchedView,
                        column = updated.column,
                        row = updated.row,
                        columnSpan = updated.columnSpan,
                        rowSpan = updated.rowSpan,
                    )
                    touchedView.tag = updated
                    true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    val updated = (touchedView.tag as? TodayWidget) ?: widget
                    val parent = touchedView.parent
                    parent?.requestDisallowInterceptTouchEvent(false)
                    if (didDrag) {
                        updateTodayWidget(updated)
                    } else if (event.actionMasked == MotionEvent.ACTION_UP && mode == TodayWidgetDragMode.Move) {
                        showTodayWidgetContextMenu(touchedView, updated)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun nextEventPrimaryText(event: CalendarEvent?): String {
        return when {
            !hasCalendarPermission() -> getString(R.string.today_calendar_permission_prompt)
            event == null -> getString(R.string.today_next_event_empty)
            else -> event.title.ifBlank { getString(R.string.calendar_event_untitled) }
        }
    }

    private fun nextEventSecondaryText(event: CalendarEvent?): String {
        return when {
            !hasCalendarPermission() -> getString(R.string.calendar_permission_button)
            event == null -> ""
            event.isAllDay -> getString(R.string.today_next_event_all_day, event.calendarName)
            else -> getString(
                R.string.today_next_event_time,
                DateFormat.getTimeInstance(
                    DateFormat.SHORT,
                    resources.configuration.locales[0],
                ).format(Date(event.startMillis)),
                event.calendarName,
            )
        }
    }

    private fun weatherPrimaryText(weather: WeatherSnapshot?): String {
        return when {
            !hasWeatherLocationPermission() -> getString(R.string.today_weather_location_needed)
            isTodayWeatherLoading -> getString(R.string.today_weather_loading)
            weather != null -> getString(
                R.string.today_weather_temp,
                weather.temperatureCelsius.roundToInt(),
            )
            else -> todayWeatherError ?: getString(R.string.today_weather_unavailable)
        }
    }

    private fun weatherSecondaryText(weather: WeatherSnapshot?): String {
        return when {
            !hasWeatherLocationPermission() -> getString(R.string.today_weather_location_action)
            weather?.precipitationChancePercent != null -> getString(
                R.string.today_weather_precipitation,
                weather.precipitationChancePercent,
            )
            weather != null -> getString(R.string.today_weather_precipitation_unknown)
            else -> ""
        }
    }

    private fun refreshTodayWeather(force: Boolean = false) {
        if (todayWidgets.none { it.type == TodayWidgetType.Weather }) return
        if (!hasWeatherLocationPermission()) {
            todayWeatherError = getString(R.string.today_weather_location_needed)
            return
        }
        val now = System.currentTimeMillis()
        if (
            !force &&
            (isTodayWeatherLoading || (todayWeather != null && now - todayWeatherLoadedAtMillis < WEATHER_CACHE_MS))
        ) {
            return
        }
        val location = currentWeatherLocation()
        if (location == null) {
            todayWeatherError = getString(R.string.today_weather_unavailable)
            return
        }
        isTodayWeatherLoading = true
        todayWeatherError = null
        lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    weatherRepository.loadCurrentWeather(location.latitude, location.longitude)
                }
            }
            isTodayWeatherLoading = false
            result.onSuccess { weather ->
                todayWeather = weather
                todayWeatherLoadedAtMillis = System.currentTimeMillis()
                todayWeatherError = null
            }.onFailure {
                todayWeatherError = getString(R.string.today_weather_unavailable)
            }
            renderTodayWidgets()
        }
    }

    private fun hasWeatherLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestWeatherLocationPermissionFromToday() {
        if (
            hasRequestedWeatherLocationPermission &&
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            openAppPermissionSettings()
        } else {
            hasRequestedWeatherLocationPermission = true
            requestWeatherLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun currentWeatherLocation(): Location? {
        if (!hasWeatherLocationPermission()) return null
        val locationManager = getSystemService(LocationManager::class.java)
        return locationManager.getProviders(true)
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
    }

    private fun requestCalendarPermissionFromToday() {
        if (hasRequestedCalendarPermission && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR)) {
            openAppPermissionSettings()
        } else {
            viewModel.markCalendarPermissionRequested()
            requestCalendarPermission.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private fun enterTodayEditMode() {
        if (!isTodayVisible || isTodayEditMode) return
        isTodayEditMode = true
        binding.todayTitle.text = getString(R.string.today_edit_mode_label)
        binding.addTodayWidgetButton.visibility = View.VISIBLE
        performLightHapticFeedback()
        renderTodayWidgets()
    }

    private fun exitTodayEditMode() {
        if (!isTodayEditMode) return
        isTodayEditMode = false
        binding.todayTitle.text = getString(R.string.today_page_title)
        binding.addTodayWidgetButton.visibility = View.GONE
        renderTodayWidgets()
    }

    private fun showTodayWidgetContextMenu(anchor: View, widget: TodayWidget) {
        showActionContextMenu(
            anchor = anchor,
            actions = buildList {
                if (widget.type == TodayWidgetType.NotificationFeed) {
                    add(
                        ContextMenuAction(getString(R.string.today_edit_widget)) {
                            showNotificationWidgetConfig(widget)
                        },
                    )
                }
                add(
                    ContextMenuAction(getString(R.string.today_remove_widget)) {
                        removeTodayWidget(widget)
                    },
                )
            },
        )
    }

    private fun showTodayWidgetPicker(anchor: View) {
        val actions = availableTodayWidgetTypes().map { type ->
            ContextMenuAction(label = todayWidgetTypeLabel(type)) {
                addTodayWidget(type)
            }
        }
        if (actions.isEmpty()) {
            Toast.makeText(this, R.string.today_widget_none_available, Toast.LENGTH_SHORT).show()
            return
        }
        showActionContextMenu(anchor, actions)
    }

    private fun availableTodayWidgetTypes(): List<TodayWidgetType> {
        val visibleTypes = todayWidgets.map { it.type }.toSet()
        return TodayWidgetType.entries.filterNot { it in visibleTypes }
    }

    private fun todayWidgetTypeLabel(type: TodayWidgetType): String {
        return getString(
            when (type) {
                TodayWidgetType.NextEvent -> R.string.today_next_event_title
                TodayWidgetType.Weather -> R.string.today_weather_title
                TodayWidgetType.NotificationFeed -> R.string.today_notification_feed_title
                TodayWidgetType.PinnedNote -> R.string.today_pinned_note_title
            },
        )
    }

    private fun addTodayWidget(type: TodayWidgetType) {
        if (todayWidgets.any { it.type == type }) return
        val placement = findFreeTodayWidgetPlacement()
        if (placement == null) {
            Toast.makeText(this, R.string.today_widget_no_space, Toast.LENGTH_SHORT).show()
            return
        }
        val widget = when (type) {
            TodayWidgetType.NextEvent -> todayWidgetRepository.defaultNextEventWidget()
            TodayWidgetType.Weather -> todayWidgetRepository.defaultWeatherWidget()
            TodayWidgetType.NotificationFeed -> todayWidgetRepository.defaultNotificationFeedWidget()
            TodayWidgetType.PinnedNote -> todayWidgetRepository.defaultPinnedNoteWidget()
        }.copy(
            column = placement.column,
            row = placement.row,
            columnSpan = TodayWidgetGridView.MIN_COLUMN_SPAN,
            rowSpan = TodayWidgetGridView.MIN_ROW_SPAN,
        )
        todayWidgets = todayWidgets + widget
        todayWidgetRepository.saveWidgets(todayWidgets)
        renderTodayWidgets()
        if (type == TodayWidgetType.NotificationFeed && !hasNotificationAccess()) {
            Toast.makeText(this, R.string.today_notification_access_prompt, Toast.LENGTH_LONG).show()
            openNotificationAccessSettings()
        }
        if (type == TodayWidgetType.Weather) {
            if (hasWeatherLocationPermission()) {
                refreshTodayWeather(force = true)
            } else {
                requestWeatherLocationPermissionFromToday()
            }
        }
    }

    private fun findFreeTodayWidgetPlacement(): TodayWidgetPlacement? {
        for (row in 0 until TodayWidgetGridView.ROW_COUNT) {
            for (column in 0..TodayWidgetGridView.COLUMN_COUNT - TodayWidgetGridView.MIN_COLUMN_SPAN) {
                val placement = TodayWidgetPlacement(
                    column = column,
                    row = row,
                    columnSpan = TodayWidgetGridView.MIN_COLUMN_SPAN,
                    rowSpan = TodayWidgetGridView.MIN_ROW_SPAN,
                )
                if (todayWidgets.none { it.overlaps(placement) }) {
                    return placement
                }
            }
        }
        return null
    }

    private fun TodayWidget.overlaps(placement: TodayWidgetPlacement): Boolean {
        val left = column
        val right = column + columnSpan
        val top = row
        val bottom = row + rowSpan
        val placementLeft = placement.column
        val placementRight = placement.column + placement.columnSpan
        val placementTop = placement.row
        val placementBottom = placement.row + placement.rowSpan
        return left < placementRight &&
            right > placementLeft &&
            top < placementBottom &&
            bottom > placementTop
    }

    private fun updateTodayWidget(widget: TodayWidget) {
        todayWidgets = todayWidgets.map { current ->
            if (current.id == widget.id) widget else current
        }
        todayWidgetRepository.saveWidgets(todayWidgets)
    }

    private fun removeTodayWidget(widget: TodayWidget) {
        todayWidgets = todayWidgets.filterNot { it.id == widget.id }
        todayWidgetRepository.saveWidgets(todayWidgets)
        renderTodayWidgets()
    }

    private fun filteredNotificationsForWidget(widget: TodayWidget): List<TodayNotificationItem> {
        val allowedPackages = widget.notificationAppPackageNames
        return if (allowedPackages.isEmpty()) {
            todayNotifications
        } else {
            todayNotifications.filter { it.packageName in allowedPackages }
        }
    }

    private fun notificationSubtitle(notification: TodayNotificationItem): String {
        return listOf(notification.appLabel, notification.text)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
    }

    private fun showNotificationWidgetConfig(widget: TodayWidget) {
        val selectedPackages = widget.notificationAppPackageNames.toMutableSet()
        val appOptions = notificationConfigAppOptions()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(getColor(R.color.launcher_background))
                setStroke(1.dp, getColor(R.color.launcher_text))
            }
            setPadding(22.dp, 18.dp, 22.dp, 12.dp)
        }
        val popup = PopupWindow(
            content,
            (resources.displayMetrics.widthPixels - 48.dp).coerceAtLeast(280.dp),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 0f
        }

        content.addView(
            TextView(this).apply {
                text = getString(R.string.today_notification_config_title)
                setTextColor(getColor(R.color.launcher_text))
                textSize = 22f
                maxLines = 1
                includeFontPadding = false
            },
        )
        content.addView(
            TextView(this).apply {
                text = getString(R.string.today_notification_config_hint)
                setTextColor(getColor(R.color.launcher_text_secondary))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = 10.dp
                    bottomMargin = 10.dp
                }
            },
        )

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        appOptions.forEach { option ->
            list.addView(createNotificationConfigRow(option, selectedPackages))
        }
        content.addView(
            ScrollView(this).apply {
                addView(list)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    360.dp,
                )
            },
        )
        content.addView(
            TextView(this).apply {
                text = getString(R.string.today_notification_config_done)
                setTextColor(getColor(R.color.launcher_text))
                textSize = 17f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    52.dp,
                ).apply {
                    topMargin = 8.dp
                }
                setOnClickListener {
                    updateTodayWidget(widget.copy(notificationAppPackageNames = selectedPackages))
                    renderTodayWidgets()
                    popup.dismiss()
                }
            },
        )

        popup.showAtLocation(binding.root, android.view.Gravity.CENTER, 0, 0)
    }

    private fun createNotificationConfigRow(
        option: NotificationAppOption,
        selectedPackages: MutableSet<String>,
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                56.dp,
            )
        }
        val label = TextView(this).apply {
            text = option.label
            setTextColor(getColor(R.color.launcher_text))
            textSize = 16f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f,
            )
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val checkbox = MaterialCheckBox(this).apply {
            isChecked = option.packageName in selectedPackages
            setUseMaterialThemeColors(true)
        }
        fun toggleSelection() {
            val shouldSelect = option.packageName !in selectedPackages
            if (shouldSelect) {
                selectedPackages.add(option.packageName)
            } else {
                selectedPackages.remove(option.packageName)
            }
            checkbox.isChecked = shouldSelect
        }
        row.setOnClickListener { toggleSelection() }
        checkbox.setOnClickListener { toggleSelection() }
        row.addView(label)
        row.addView(checkbox)
        return row
    }

    private fun notificationConfigAppOptions(): List<NotificationAppOption> {
        val launchableApps = installedAppsRepository.loadLaunchableApps()
            .distinctBy { it.packageName }
            .map { NotificationAppOption(it.packageName, it.label) }
        val notifyingApps = todayNotifications
            .map { NotificationAppOption(it.packageName, it.appLabel) }
            .distinctBy { it.packageName }
        return (launchableApps + notifyingApps)
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    private fun hasNotificationAccess(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val listener = ComponentName(this, TodayNotificationListenerService::class.java)
        val flattened = listener.flattenToString()
        val shortFlattened = listener.flattenToShortString()
        return enabledListeners.split(':').any { enabledListener ->
            enabledListener.equals(flattened, ignoreCase = true) ||
                enabledListener.equals(shortFlattened, ignoreCase = true)
        }
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun todayWidgetBackground(isEditing: Boolean): Drawable {
        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            if (isEditing) {
                setStroke(TODAY_WIDGET_EDIT_STROKE_DP.dp, getColor(R.color.launcher_text))
            }
        }
        return if (isEditing) {
            InsetDrawable(background, TODAY_WIDGET_EDIT_STROKE_DP.dp)
        } else {
            background
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

    private fun openClockApp() {
        val didOpenClock = (
            CLOCK_APP_PACKAGES
                .asSequence()
                .mapNotNull { clockPackage -> packageManager.getLaunchIntentForPackage(clockPackage) }
                .firstOrNull { intent -> tryStartActivity(intent) }
            ) != null

        if (!didOpenClock) {
            Toast.makeText(this, R.string.clock_app_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryStartActivity(intent: Intent): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
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
        binding.appSearchInput.setOnEditorActionListener { _, actionId, event ->
            val isImeSubmit = actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_SEARCH
            val isKeyboardEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_DOWN

            if (isImeSubmit || isKeyboardEnter) {
                launchTopSearchResult()
                true
            } else {
                false
            }
        }
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

                override fun onDoubleTap(event: MotionEvent): Boolean {
                    if (canHandleConfigurableGesture(LauncherGesture.DoubleTap)) {
                        performGestureAction(LauncherGesture.DoubleTap)
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
            openClockApp()
            true
        }
        binding.clockView.setOnClickListener {
            viewModel.toggleClockDisplayMode()
        }
        binding.shortcutList.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                shouldHandleBlankAreaLongPress = !isAppPickerVisible &&
                    !isShortcutTextTargetUnder(event.x, event.y)
            }
            if (shouldHandleBlankAreaLongPress) {
                detector.onTouchEvent(event)
            }
            false
        }
        binding.homeContent.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                shouldHandleHomeContentLongPress = !isTouchInsideView(
                    binding.clockDateContent,
                    event.rawX,
                    event.rawY,
                )
            }
            if (shouldHandleHomeContentLongPress) {
                detector.onTouchEvent(event)
            }
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                shouldHandleHomeContentLongPress = false
            }
            false
        }
    }

    private fun isTouchInsideView(view: View, rawX: Float, rawY: Float): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0].toFloat()
        val top = location[1].toFloat()
        return rawX >= left &&
            rawX <= left + view.width &&
            rawY >= top &&
            rawY <= top + view.height
    }

    private fun isShortcutTextTargetUnder(x: Float, y: Float): Boolean {
        val shortcutRow = binding.shortcutList.findChildViewUnder(x, y) ?: return false
        val shortcutName = shortcutRow.findViewById<TextView>(R.id.shortcutName) ?: return false
        val localX = x - shortcutRow.left - shortcutName.left
        val localY = y - shortcutRow.top - shortcutName.top
        return localX >= 0f &&
            localX <= shortcutName.width &&
            localY >= 0f &&
            localY <= shortcutName.height
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (appBlockPromptController.isVisible) {
                        hideAppBlockPrompt()
                    } else if (binding.gesturePickerRoot.isVisible) {
                        hideGesturePicker()
                    } else if (isNoteEditorVisible) {
                        hideNoteEditor()
                    } else if (isNoteTrashVisible) {
                        hideNoteTrash()
                    } else if (isSettingsVisible) {
                        if (currentSettingsPage == SettingsPage.Index) {
                            hideSettings()
                        } else {
                            showSettingsPage(SettingsPage.Index)
                        }
                    } else if (isTodayEditMode) {
                        exitTodayEditMode()
                    } else if (isNotesVisible) {
                        hideNotesPage()
                    } else if (isCalendarVisible) {
                        hideCalendarPage()
                    } else if (isTodayVisible) {
                        hideTodayPage()
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

    private fun routeToHomeScreen() {
        cancelPageDrag()

        if (appBlockPromptController.isVisible) {
            hideAppBlockPrompt()
        }
        if (binding.gesturePickerRoot.isVisible) {
            hideGesturePicker()
        }
        if (isNoteEditorVisible) {
            hideNoteEditor()
        }
        if (isNoteTrashVisible) {
            hideNoteTrash(returnToSettings = false)
        }
        if (isSettingsVisible) {
            hideSettings()
        }
        if (isNotesVisible) {
            hideNotesPageImmediately()
        }
        if (isCalendarVisible) {
            hideCalendarPageImmediately()
        }
        if (isTodayVisible) {
            hideTodayPageImmediately()
        }
        if (isScreenTimeVisible) {
            hideScreenTimePage()
        }
        if (isAppPickerVisible) {
            hideAppPicker()
        }
        if (isEditMode) {
            exitEditMode()
        }

        binding.homeContent.animate().cancel()
        binding.homeContent.visibility = View.VISIBLE
        binding.homeContent.translationX = 0f
        binding.homeContent.translationY = 0f
    }

    private fun hideNotesPageImmediately() {
        isNotesVisible = false
        binding.homeContent.animate().cancel()
        binding.notesRoot.animate().cancel()
        binding.homeContent.visibility = View.VISIBLE
        binding.homeContent.translationX = 0f
        binding.homeContent.translationY = 0f
        binding.notesRoot.visibility = View.GONE
        binding.notesRoot.translationX = 0f
    }

    private fun hideCalendarPageImmediately() {
        isCalendarVisible = false
        binding.homeContent.animate().cancel()
        binding.calendarRoot.animate().cancel()
        binding.homeContent.visibility = View.VISIBLE
        binding.homeContent.translationX = 0f
        binding.homeContent.translationY = 0f
        binding.calendarRoot.visibility = View.GONE
        binding.calendarRoot.translationX = 0f
    }

    private fun hideTodayPageImmediately() {
        exitTodayEditMode()
        isTodayVisible = false
        binding.homeContent.animate().cancel()
        binding.todayRoot.animate().cancel()
        binding.homeContent.visibility = View.VISIBLE
        binding.homeContent.translationX = 0f
        binding.homeContent.translationY = 0f
        binding.todayRoot.visibility = View.GONE
        binding.todayRoot.translationY = 0f
    }

    private fun showShortcutContextMenu(anchor: View, shortcut: AppShortcut) {
        showActionContextMenu(
            anchor = anchor,
            actions = listOf(
                ContextMenuAction(getString(R.string.remove_shortcut)) {
                    viewModel.deleteShortcut(shortcut)
                },
                appBlockContextMenuAction(shortcut),
                uninstallAppContextMenuAction(shortcut),
            ),
        )
    }

    private fun showNoteContextMenu(anchor: View, note: QuickNote) {
        showActionContextMenu(
            anchor = anchor,
            actions = listOf(
                ContextMenuAction(
                    getString(if (note.isPinned) R.string.unpin_note else R.string.pin_note),
                ) {
                    viewModel.setNotePinned(note, !note.isPinned)
                },
                ContextMenuAction(getString(R.string.delete_note)) {
                    if (note.id == playingVoiceNoteId) {
                        stopVoiceNotePlayback()
                    }
                    viewModel.deleteNote(note)?.let(::showNoteUndo)
                },
            ),
        )
    }

    private fun showLauncherAppContextMenu(anchor: View, shortcut: AppShortcut) {
        showActionContextMenu(
            anchor = anchor,
            actions = listOf(
                appBlockContextMenuAction(shortcut),
                uninstallAppContextMenuAction(shortcut),
            ),
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

    private fun uninstallAppContextMenuAction(shortcut: AppShortcut): ContextMenuAction {
        return ContextMenuAction(label = getString(R.string.uninstall_app)) {
            launchUninstallApp(shortcut.packageName)
        }
    }

    private fun showActionContextMenu(anchor: View, actions: List<ContextMenuAction>) {
        actionContextMenu.show(anchor, actions)
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
        performEditModeHapticFeedback()
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
        updateLauncherLayerVisibility()
        showSettingsPage(SettingsPage.Index)
        binding.settingsRoot.alpha = 0f
        binding.settingsRoot.visibility = View.VISIBLE
        binding.settingsRoot.animate()
            .alpha(1f)
            .setDuration(SETTINGS_FADE_MS)
            .start()
    }

    private fun showNoteTrash() {
        if (!isSettingsVisible || isNoteTrashVisible) return
        isNoteTrashVisible = true
        binding.settingsRoot.animate().cancel()
        binding.settingsRoot.visibility = View.GONE
        binding.noteTrashRoot.alpha = 0f
        binding.noteTrashRoot.visibility = View.VISIBLE
        binding.noteTrashRoot.animate()
            .alpha(1f)
            .setDuration(SETTINGS_FADE_MS)
            .start()
    }

    private fun hideNoteTrash(returnToSettings: Boolean = true) {
        if (!isNoteTrashVisible) return
        isNoteTrashVisible = false
        binding.noteTrashRoot.animate().cancel()
        binding.noteTrashRoot.visibility = View.GONE
        binding.noteTrashRoot.alpha = 1f
        if (returnToSettings && isSettingsVisible) {
            binding.settingsRoot.alpha = 0f
            binding.settingsRoot.visibility = View.VISIBLE
            binding.settingsRoot.animate()
                .alpha(1f)
                .setDuration(SETTINGS_FADE_MS)
                .start()
        }
    }

    private fun updateLauncherLayerVisibility() {
        val shouldShowLauncherLayer = (isAppPickerVisible && appListMode == AppListMode.LaunchApp) ||
            (!isAppPickerVisible &&
                !isSettingsVisible &&
                !isScreenTimeVisible &&
                !isNoteEditorVisible &&
                !isNotesVisible &&
                !isCalendarVisible &&
                !isTodayVisible)
        binding.homeContent.visibility = if (shouldShowLauncherLayer) View.VISIBLE else View.GONE
        binding.editControls.visibility = if (shouldShowLauncherLayer && isEditMode && !isAppPickerVisible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun hideNotesPage() {
        hideConfiguredPage(LauncherPage.Notes)
    }

    private fun hideCalendarPage() {
        hideConfiguredPage(LauncherPage.Calendar)
    }

    private fun hideTodayPage() {
        exitTodayEditMode()
        hideConfiguredPage(LauncherPage.Today)
    }

    private fun hideConfiguredPage(page: LauncherPage) {
        if (currentVisiblePage() != page) return
        val target = PageSwipeTarget(
            page = page,
            position = currentPageArrangement.positionOf(page),
            isReturningHome = true,
        )
        preparePageDrag(target)
        settlePageDrag(target, shouldComplete = true)
    }

    private fun showScreenTimePage() {
        if (isScreenTimeVisible || isEditMode || currentOpenScreenTimeGesture == LauncherGesture.None) return
        isScreenTimeVisible = true
        performLightHapticFeedback()
        updateLauncherLayerVisibility()
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
        updateLauncherLayerVisibility()
    }

    private fun showNoteEditor(note: QuickNote?) {
        if (note?.audioFileName != null) return
        editingNote = note
        isNoteEditorVisible = true
        if (isNotesVisible) {
            binding.notesRoot.visibility = View.GONE
        }
        didOpenNoteEditorFromToday = isTodayVisible
        if (didOpenNoteEditorFromToday) {
            binding.todayRoot.animate().cancel()
            binding.todayRoot.visibility = View.GONE
        }
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
        if (isNotesVisible) {
            binding.notesRoot.visibility = View.VISIBLE
        }
        if (didOpenNoteEditorFromToday && isTodayVisible) {
            binding.todayRoot.visibility = View.VISIBLE
        }
        didOpenNoteEditorFromToday = false
    }

    private fun saveCurrentNote() {
        val note = editingNote
        val text = binding.noteEditorInput.text?.toString().orEmpty()
        val trashedNote = if (note == null) {
            viewModel.addNote(text)
            null
        } else {
            viewModel.updateNote(note, text)
        }
        hideNoteEditor()
        trashedNote?.let(::showNoteUndo)
    }

    private fun showNoteUndo(trashedNote: TrashedNote) {
        pendingUndoNoteId = trashedNote.note.id
        noteUndoHandler.removeCallbacks(noteUndoDismissRunnable)
        binding.noteUndoContainer.animate().cancel()
        binding.noteUndoContainer.alpha = 0f
        binding.noteUndoContainer.translationY = NOTE_UNDO_ENTER_OFFSET_DP.dp.toFloat()
        binding.noteUndoContainer.visibility = View.VISIBLE
        binding.noteUndoButton.setOnClickListener {
            pendingUndoNoteId?.let(::restoreTrashedNote)
        }
        binding.noteUndoContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(NOTE_UNDO_FADE_MS)
            .start()
        noteUndoHandler.postDelayed(noteUndoDismissRunnable, NOTE_UNDO_DURATION_MS)
    }

    private fun hideNoteUndo() {
        pendingUndoNoteId = null
        noteUndoHandler.removeCallbacks(noteUndoDismissRunnable)
        binding.noteUndoContainer.animate().cancel()
        if (!binding.noteUndoContainer.isVisible) return
        binding.noteUndoContainer.animate()
            .alpha(0f)
            .translationY(NOTE_UNDO_ENTER_OFFSET_DP.dp.toFloat())
            .setDuration(NOTE_UNDO_FADE_MS)
            .withEndAction {
                binding.noteUndoContainer.visibility = View.GONE
                binding.noteUndoContainer.alpha = 1f
                binding.noteUndoContainer.translationY = 0f
            }
            .start()
    }

    private fun restoreTrashedNote(noteId: Long) {
        if (!viewModel.restoreNote(noteId)) return
        if (pendingUndoNoteId == noteId) {
            hideNoteUndo()
        }
    }

    private fun confirmPermanentNoteDeletion(trashedNote: TrashedNote) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_note_permanently_title)
            .setMessage(
                if (trashedNote.note.audioFileName == null) {
                    R.string.delete_note_permanently_message
                } else {
                    R.string.delete_voice_note_permanently_message
                },
            )
            .setNegativeButton(R.string.cancel_note_edit, null)
            .setPositiveButton(R.string.delete_note_permanently) { _, _ ->
                val deletedNote = viewModel.permanentlyDeleteNote(trashedNote.note.id) ?: return@setPositiveButton
                if (pendingUndoNoteId == deletedNote.id) {
                    hideNoteUndo()
                }
                deleteVoiceNoteFile(deletedNote)
            }
            .show()
            .getButton(DialogInterface.BUTTON_POSITIVE)
            .setTextColor(getColor(R.color.launcher_warning))
    }

    private fun copyNote(note: QuickNote) {
        if (note.audioFileName != null) return
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(getString(R.string.copy_note), note.text))
        Toast.makeText(this, R.string.note_copied, Toast.LENGTH_SHORT).show()
    }

    private fun QuickNote.displayText(): String {
        return if (audioFileName == null) text else getString(R.string.voice_note_label)
    }

    @SuppressLint("MissingPermission")
    private fun startVoiceNoteRecording() {
        if (voiceNoteRecorder != null || !hasMicrophonePermission()) return
        stopVoiceNotePlayback()
        val outputFile = File(voiceNotesDirectory(), "voice_note_${System.currentTimeMillis()}.m4a")
        val recorder = createVoiceNoteRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)
        }
        runCatching {
            recorder.prepare()
            recorder.start()
        }.onSuccess {
            voiceNoteRecorder = recorder
            voiceNoteRecordingFile = outputFile
            voiceNoteRecordingStartedAtMillis = System.currentTimeMillis()
            voiceNoteSamples = mutableListOf()
            voiceNoteSampleHandler.post(voiceNoteSampleRunnable)
            performLightHapticFeedback()
            updateAddNoteButton()
        }.onFailure {
            recorder.release()
            outputFile.delete()
            Toast.makeText(this, R.string.voice_note_recording_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopVoiceNoteRecording(save: Boolean) {
        val recorder = voiceNoteRecorder ?: return
        val outputFile = voiceNoteRecordingFile
        voiceNoteSampleHandler.removeCallbacks(voiceNoteSampleRunnable)
        voiceNoteRecorder = null
        voiceNoteRecordingFile = null
        val durationMillis = System.currentTimeMillis() - voiceNoteRecordingStartedAtMillis
        voiceNoteRecordingStartedAtMillis = 0L
        val samples = voiceNoteSamples.toList()
        voiceNoteSamples = mutableListOf()
        val didStop = runCatching { recorder.stop() }.isSuccess
        recorder.release()
        if (save && didStop && outputFile != null && outputFile.exists() && durationMillis >= MIN_VOICE_NOTE_DURATION_MS) {
            viewModel.addVoiceNote(
                audioFileName = outputFile.name,
                durationMillis = durationMillis,
                waveform = samples.normalizedVoiceWaveform(),
            )
        } else {
            outputFile?.delete()
        }
        performLightHapticFeedback()
        updateAddNoteButton()
    }

    private fun sampleVoiceNoteAmplitude() {
        val recorder = voiceNoteRecorder ?: return
        val sample = runCatching { recorder.maxAmplitude }.getOrDefault(0)
        val normalized = ((sample.coerceAtLeast(0) / MAX_MEDIA_RECORDER_AMPLITUDE.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
        voiceNoteSamples.add(normalized)
        if (voiceNoteSamples.size > MAX_VOICE_WAVEFORM_SAMPLES) {
            voiceNoteSamples.removeAt(0)
        }
    }

    private fun toggleVoiceNotePlayback(note: QuickNote) {
        val audioFileName = note.audioFileName ?: return
        if (note.id == playingVoiceNoteId) {
            if (voiceNotePlayer?.isPlaying == true) {
                pauseVoiceNotePlayback()
            } else {
                resumeVoiceNotePlayback()
            }
            return
        }
        stopVoiceNotePlayback()
        val audioFile = File(voiceNotesDirectory(), audioFileName)
        if (!audioFile.exists()) {
            Toast.makeText(this, R.string.voice_note_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        val player = MediaPlayer().apply {
            setDataSource(audioFile.absolutePath)
            setOnCompletionListener {
                stopVoiceNotePlayback()
            }
        }
        runCatching {
            player.prepare()
            player.start()
        }.onSuccess {
            voiceNotePlayer = player
            playingVoiceNoteId = note.id
            isVoicePlaybackPlaying = true
            voicePlaybackProgressFraction = 0f
            noteAdapter.playingVoiceNoteId = playingVoiceNoteId
            noteAdapter.isVoicePlaybackPlaying = isVoicePlaybackPlaying
            updateVoiceNotePlaybackProgress()
            voiceNotePlaybackProgressHandler.post(voiceNotePlaybackProgressRunnable)
            updateTodayPinnedVoicePlaybackUi()
        }.onFailure {
            player.release()
            Toast.makeText(this, R.string.voice_note_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseVoiceNotePlayback() {
        val player = voiceNotePlayer ?: return
        updateVoiceNotePlaybackProgress()
        runCatching { player.pause() }
        voiceNotePlaybackProgressHandler.removeCallbacks(voiceNotePlaybackProgressRunnable)
        isVoicePlaybackPlaying = false
        noteAdapter.isVoicePlaybackPlaying = false
        updateTodayPinnedVoicePlaybackUi()
    }

    private fun resumeVoiceNotePlayback() {
        val player = voiceNotePlayer ?: return
        runCatching { player.start() }.onSuccess {
            isVoicePlaybackPlaying = true
            noteAdapter.isVoicePlaybackPlaying = true
            updateVoiceNotePlaybackProgress()
            voiceNotePlaybackProgressHandler.post(voiceNotePlaybackProgressRunnable)
            updateTodayPinnedVoicePlaybackUi()
        }.onFailure {
            stopVoiceNotePlayback()
            Toast.makeText(this, R.string.voice_note_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetVoiceNotePlaybackFromNotesList(note: QuickNote) {
        if (note.id != playingVoiceNoteId) return
        val player = voiceNotePlayer ?: return
        runCatching { player.seekTo(0) }.onFailure { return }
        voicePlaybackProgressFraction = 0f
        noteAdapter.voicePlaybackProgressFraction = 0f
        updateTodayPinnedVoicePlaybackUi()
        if (player.isPlaying) {
            updateVoiceNotePlaybackProgress()
        }
    }

    private fun stopVoiceNotePlayback() {
        voiceNotePlaybackProgressHandler.removeCallbacks(voiceNotePlaybackProgressRunnable)
        releaseVoiceNotePlayer()
        playingVoiceNoteId = null
        isVoicePlaybackPlaying = false
        voicePlaybackProgressFraction = 0f
        if (::noteAdapter.isInitialized) {
            noteAdapter.voicePlaybackProgressFraction = 0f
            noteAdapter.isVoicePlaybackPlaying = false
            noteAdapter.playingVoiceNoteId = null
        }
        updateTodayPinnedVoicePlaybackUi()
    }

    private fun updateVoiceNotePlaybackProgress() {
        val player = voiceNotePlayer ?: return
        val duration = player.duration.takeIf { it > 0 } ?: return
        voicePlaybackProgressFraction = player.currentPosition / duration.toFloat()
        noteAdapter.voicePlaybackProgressFraction = voicePlaybackProgressFraction
        updateTodayPinnedVoicePlaybackUi()
    }

    private fun updateTodayPinnedVoicePlaybackUi() {
        val pinnedNote = currentNotes.firstOrNull { it.isPinned && it.audioFileName != null }
        val isActive = pinnedNote?.id == playingVoiceNoteId
        val isPlaying = isActive && isVoicePlaybackPlaying
        todayPinnedVoiceWaveform?.progressFraction = if (isActive) voicePlaybackProgressFraction else 0f
        todayPinnedVoicePlayButton?.apply {
            setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            contentDescription = getString(if (isPlaying) R.string.pause_voice_note else R.string.play_voice_note)
        }
    }

    private fun releaseVoiceNotePlayer() {
        voiceNotePlayer?.runCatching {
            if (isPlaying) stop()
        }
        voiceNotePlayer?.release()
        voiceNotePlayer = null
    }

    private fun deleteVoiceNoteFile(note: QuickNote) {
        val audioFileName = note.audioFileName ?: return
        File(voiceNotesDirectory(), audioFileName).delete()
    }

    private fun formatVoiceNoteDuration(durationMillis: Long): String {
        val totalSeconds = (durationMillis / 1_000L).coerceAtLeast(1L)
        return getString(
            R.string.voice_note_duration,
            totalSeconds / 60L,
            totalSeconds % 60L,
        )
    }

    private fun voiceNotesDirectory(): File {
        return File(filesDir, VOICE_NOTES_DIRECTORY).apply { mkdirs() }
    }

    private fun List<Int>.normalizedVoiceWaveform(): List<Int> {
        val source = if (isEmpty()) listOf(12, 20, 16, 24) else this
        if (source.size <= MAX_VOICE_WAVEFORM_SAMPLES) return source.map { it.coerceIn(0, 100) }
        val bucketSize = source.size / MAX_VOICE_WAVEFORM_SAMPLES.toFloat()
        return List(MAX_VOICE_WAVEFORM_SAMPLES) { bucket ->
            val start = (bucket * bucketSize).toInt()
            val end = ((bucket + 1) * bucketSize).toInt().coerceAtLeast(start + 1).coerceAtMost(source.size)
            source.subList(start, end).maxOrNull()?.coerceIn(0, 100) ?: 0
        }
    }

    @Suppress("DEPRECATION")
    private fun createVoiceNoteRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }
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
        if (isNoteTrashVisible) {
            hideNoteTrash(returnToSettings = false)
        }
        hideSettingsSearchKeyboard()
        isSettingsVisible = false
        currentSettingsPage = SettingsPage.Index
        hideGesturePicker()
        binding.settingsRoot.animate().cancel()
        binding.settingsRoot.visibility = View.GONE
        binding.settingsRoot.alpha = 1f
        updateLauncherLayerVisibility()
    }

    private fun stopEditModePulse() {
        editModePulseAnimator?.cancel()
        editModePulseAnimator = null
        binding.editModeText.alpha = 1f
    }

    private fun showAppList(mode: AppListMode) {
        appListMode = mode
        availableApps = loadCachedLaunchableApps()
        binding.appSearchInput.text?.clear()
        renderFilteredApps(query = "")
        isAppPickerVisible = true
        updateLauncherLayerVisibility()
        binding.appPickerRoot.visibility = View.VISIBLE
        if (mode == AppListMode.LaunchApp) {
            animateAppListEntrance()
        } else {
            binding.appPickerRoot.alpha = 1f
            binding.appPickerRoot.translationY = 0f
        }
        applyAppListKeyboardPreference()
    }

    private fun hideAppPicker() {
        binding.appPickerRoot.animate().cancel()
        hideKeyboard()
        binding.appPickerRoot.visibility = View.GONE
        binding.appPickerRoot.alpha = 1f
        binding.appPickerRoot.translationY = 0f
        resetAppListHomeTreatment()
        binding.appSearchInput.text?.clear()
        appPickerAdapter.submitList(emptyList())
        availableApps = emptyList()
        isAppPickerVisible = false
        updateLauncherLayerVisibility()
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

    private fun loadCachedLaunchableApps(): List<AppShortcut> {
        return cachedLaunchableApps.ifEmpty {
            installedAppsRepository.loadLaunchableApps().also { apps ->
                cachedLaunchableApps = apps
            }
        }
    }

    private fun refreshLaunchableAppCache() {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.Default) {
                installedAppsRepository.loadLaunchableApps()
            }
            cachedLaunchableApps = apps
            if (isAppPickerVisible) {
                availableApps = apps
                renderFilteredApps(binding.appSearchInput.text?.toString().orEmpty())
            }
        }
    }

    private fun renderFilteredApps(query: String) {
        val filteredApps = FuzzyAppSearch.filter(availableApps, query)
        appPickerAdapter.submitList(filteredApps)
        binding.appPickerList.visibility = if (filteredApps.isEmpty()) View.GONE else View.VISIBLE
        binding.appPickerEmpty.visibility = if (filteredApps.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun launchTopSearchResult() {
        if (appListMode != AppListMode.LaunchApp) return

        val query = binding.appSearchInput.text?.toString().orEmpty()
        if (query.isBlank()) return

        val shortcut = FuzzyAppSearch.filter(availableApps, query).firstOrNull() ?: return
        hideAppPicker()
        launchShortcutWithAppBlocking(shortcut)
    }

    private fun showKeyboard() {
        binding.appSearchInput.post {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(binding.appSearchInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun applyAppListKeyboardPreference() {
        if (openAppListKeyboardAutomatically) {
            binding.appSearchInput.requestFocus()
            showKeyboard()
        } else {
            binding.appSearchInput.clearFocus()
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
        appBlockPromptController.show(shortcut, budgetOverrun)
    }

    private fun hideAppBlockPrompt() {
        appBlockPromptController.hide()
    }

    private fun forceLaunchShortcut(shortcut: AppShortcut) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(shortcut.packageName, shortcut.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        startActivity(intent)
    }

    private fun launchUninstallApp(packageName: String) {
        try {
            val statusIntent = Intent(this, UninstallStatusReceiver::class.java).apply {
                action = UninstallStatusReceiver.ACTION_UNINSTALL_STATUS
                putExtra(UninstallStatusReceiver.EXTRA_PACKAGE_NAME, packageName)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
            val statusReceiver = PendingIntent.getBroadcast(
                this,
                packageName.hashCode(),
                statusIntent,
                flags,
            )
            packageManager.packageInstaller.uninstall(packageName, statusReceiver.intentSender)
        } catch (_: SecurityException) {
            launchUninstallAppFallback(packageName)
        } catch (_: IllegalArgumentException) {
            launchUninstallAppFallback(packageName)
        }
    }

    @Suppress("DEPRECATION")
    private fun launchUninstallAppFallback(packageName: String) {
        val packageUri = Uri.fromParts("package", packageName, null)
        try {
            startActivity(
                Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri).apply {
                    putExtra(Intent.EXTRA_RETURN_RESULT, false)
                },
            )
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_DELETE, packageUri))
            } catch (_: ActivityNotFoundException) {
                showQuickAccessUnavailable()
            }
        }
    }

    private fun lockScreen() {
        if (LockScreenAccessibilityService.lockScreen()) {
            return
        }

        try {
            Toast.makeText(this, R.string.lock_screen_permission_prompt, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.quick_access_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showQuickAccessUnavailable() {
        Toast.makeText(this, R.string.quick_access_unavailable, Toast.LENGTH_SHORT).show()
    }

    private fun showShortcutLimitReached() {
        Toast.makeText(this, R.string.shortcut_limit_reached, Toast.LENGTH_SHORT).show()
    }

    private fun performEditModeHapticFeedback() {
        binding.root.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.CONTEXT_CLICK
            },
        )
    }

    private fun performLightHapticFeedback() {
        binding.root.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                HapticFeedbackConstants.TEXT_HANDLE_MOVE
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            },
        )
    }

    private fun Intent.isHomeLaunchIntent(): Boolean {
        return action == Intent.ACTION_MAIN && hasCategory(Intent.CATEGORY_HOME)
    }

    private data class NotificationAppOption(
        val packageName: String,
        val label: String,
    )

    private companion object {
        const val GOOGLE_CALENDAR_PACKAGE = "com.google.android.calendar"
        val CLOCK_APP_PACKAGES = listOf(
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
        )
        const val INTENTION_TIME_WIDTH_DP = 132
        const val SETTINGS_KEYBOARD_SCROLL_TOP_OFFSET_DP = 12
        const val SLIGHT_AVERAGE_DIFFERENCE_PERCENT = 5
        const val SIGNIFICANT_AVERAGE_DIFFERENCE_PERCENT = 25
        const val SWIPE_DOWN_DISTANCE_DP = 96
        const val SWIPE_DOWN_VELOCITY_DP = 450
        const val TWO_FINGER_SWIPE_DOWN_POINTERS = 2
        const val TWO_FINGER_SWIPE_DOWN_DISTANCE_DP = 96
        const val COLLAPSED_SCREEN_TIME_APP_COUNT = 3
        const val SCREEN_TIME_APP_ROW_HEIGHT_DP = 58
        const val QUICK_ACCESS_ICON_GRID_COLUMNS = 3
        const val QUICK_ACCESS_ICON_BUTTON_SIZE_DP = 72
        const val QUICK_ACCESS_ICON_SIZE_DP = 30
        const val QUICK_ACCESS_ICON_DIALOG_CORNER_RADIUS_DP = 28
        const val PAGE_SWIPE_AXIS_RATIO = 1.15f
        const val PAGE_SWIPE_COMPLETE_FRACTION = 0.28f
        const val PAGE_SWIPE_COMPLETE_VELOCITY = 700f
        const val PAGE_SETTLE_MS = 180L
        const val PAGE_SLIDE_MS = 220L
        const val APP_LIST_ENTER_OFFSET_DP = 24
        const val APP_LIST_ENTER_DURATION_MS = 220L
        const val APP_LIST_START_ALPHA = 0.35f
        const val APP_LIST_HOME_BLUR_RADIUS_DP = 10
        const val APP_LIST_HOME_DIM_ALPHA = 0.42f
        const val EDIT_CONTROLS_FADE_MS = 160L
        const val EDIT_TEXT_PULSE_MS = 1_200L
        const val EDIT_TEXT_MIN_ALPHA = 0.38f
        const val EDIT_TEXT_MAX_ALPHA = 1f
        const val DRAG_ACTIVE_ALPHA = 0.65f
        const val TODAY_WIDGET_RESIZE_TOUCH_DP = 36
        const val TODAY_WIDGET_RESIZE_INDICATOR_ALPHA = 0.72f
        const val TODAY_WIDGET_EDIT_STROKE_DP = 1
        const val DISABLED_ACTION_ALPHA = 0.34f
        const val MILLIS_PER_MINUTE = 60_000L
        const val WEATHER_CACHE_MS = 30 * MILLIS_PER_MINUTE
        const val MINUTES_PER_HOUR = 60L
        const val VOICE_NOTES_DIRECTORY = "voice_notes"
        const val VOICE_NOTE_SAMPLE_INTERVAL_MS = 120L
        const val VOICE_NOTE_PLAYBACK_PROGRESS_INTERVAL_MS = 80L
        const val MIN_VOICE_NOTE_DURATION_MS = 400L
        const val MAX_MEDIA_RECORDER_AMPLITUDE = 32_767
        const val MAX_VOICE_WAVEFORM_SAMPLES = 48
        const val NOTE_UNDO_DURATION_MS = 6_000L
        const val NOTE_UNDO_FADE_MS = 160L
        const val NOTE_UNDO_ENTER_OFFSET_DP = 10
        const val SETTINGS_FADE_MS = 160L
        const val SCREEN_TIME_FADE_MS = 180L
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private class NotesDividerDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val dividerHeight = (context.resources.displayMetrics.density).coerceAtLeast(1f)
        private val paint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.settings_option_divider)
        }

        override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val itemCount = state.itemCount
            if (itemCount <= 1) return

            val left = parent.paddingLeft.toFloat()
            val right = (parent.width - parent.paddingRight).toFloat()

            for (index in 0 until parent.childCount) {
                val child = parent.getChildAt(index)
                val adapterPosition = parent.getChildAdapterPosition(child)
                if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= itemCount - 1) continue

                val top = child.bottom.toFloat()
                canvas.drawRect(left, top, right, top + dividerHeight, paint)
            }
        }
    }

}
