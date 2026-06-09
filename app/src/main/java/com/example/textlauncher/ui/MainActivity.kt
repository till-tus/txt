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
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Process
import android.provider.MediaStore
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
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
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
import com.example.textlauncher.domain.QuickNote
import com.example.textlauncher.domain.ScreenTimeAppUsage
import com.example.textlauncher.domain.ScreenTimeDayUsage
import com.example.textlauncher.domain.ShortcutTextAlignment
import com.example.textlauncher.domain.TodayWidget
import com.example.textlauncher.domain.TodayWidgetType
import com.example.textlauncher.domain.TodayNotificationItem
import com.google.android.material.checkbox.MaterialCheckBox
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
    private lateinit var actionContextMenu: ActionContextMenu
    private lateinit var noteBulletFormatter: NoteBulletFormatter
    private lateinit var appBlockPromptController: AppBlockPromptController
    private var shouldHandleBlankAreaLongPress = false
    private var shouldHandleHomeContentLongPress = false
    private var isAppPickerVisible = false
    private var isEditMode = false
    private var isSettingsVisible = false
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
    private var screenTimeUsages = emptyList<ScreenTimeAppUsage>()
    private var screenTimeWeekUsages = emptyList<ScreenTimeDayUsage>()
    private var blockableApps = emptyList<AppShortcut>()
    private var currentBlockedAppPackageNames = emptySet<String>()
    private var currentAppBudgetMinutesByPackage = emptyMap<String, Int>()
    private var todayWidgets = emptyList<TodayWidget>()
    private var todayNextEvent: CalendarEvent? = null
    private var todayNotifications = emptyList<TodayNotificationItem>()
    private var currentNotes = emptyList<QuickNote>()
    private var editingNote: QuickNote? = null
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
    private var editModePulseAnimator: ValueAnimator? = null
    private var appListHomeTreatmentAnimator: ValueAnimator? = null
    private var renderedShortcutCount = 0
    private val installedAppsRepository by lazy { InstalledAppsRepository(applicationContext) }
    private val appUsageIntentionRepository by lazy { AppUsageIntentionRepository(applicationContext) }
    private val calendarRepository by lazy { CalendarRepository(applicationContext) }
    private val screenTimeRepository by lazy { ScreenTimeRepository(applicationContext) }
    private val todayWidgetRepository by lazy { TodayWidgetRepository(applicationContext) }
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

        noteAdapter = NoteAdapter(::showNoteEditor, ::copyNote, ::showNoteContextMenu)
        binding.notesList.layoutManager = LinearLayoutManager(this)
        binding.notesList.addItemDecoration(NotesDividerDecoration(this))
        binding.notesList.adapter = noteAdapter

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
                    if (isTodayVisible) {
                        renderTodayWidgets()
                    }
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
            if (isTodayVisible) {
                refreshTodayWidgets()
            }
        } else {
            renderCalendarPermissionState()
            renderTodayWidgets()
        }
        if (isScreenTimeVisible) {
            refreshScreenTime()
        }
        if (isTodayVisible) {
            renderTodayWidgets()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.isHomeLaunchIntent()) {
            routeToHomeScreen()
        }
    }

    override fun onDestroy() {
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
        if (isAppPickerVisible) {
            availableApps = installedAppsRepository.loadLaunchableApps()
            renderFilteredApps(binding.appSearchInput.text?.toString().orEmpty())
        }
        if (isSettingsVisible && (isAppBlockingExpanded || isAppBudgetsExpanded)) {
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
        availableApps = installedAppsRepository.loadLaunchableApps()
        binding.appSearchInput.text?.clear()
        renderFilteredApps(query = "")
        binding.appPickerRoot.animate().cancel()
        binding.homeContent.animate().cancel()
        binding.homeTransitionDimOverlay.animate().cancel()
        appListHomeTreatmentAnimator?.cancel()
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
        return deltaY > SWIPE_DOWN_DISTANCE_DP.dp || velocityY > SWIPE_DOWN_VELOCITY_DP
    }

    private fun settleAppListDrag(shouldComplete: Boolean) {
        binding.appPickerRoot.animate().cancel()
        if (shouldComplete) {
            isAppPickerVisible = true
            binding.appPickerRoot.visibility = View.VISIBLE
            binding.appPickerRoot.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(PAGE_SETTLE_MS)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    if (isAppPickerVisible) {
                        updateLauncherLayerVisibility()
                        binding.appSearchInput.requestFocus()
                        showKeyboard()
                    }
                }
                .start()
            animateAppListHomeTreatment(toProgress = 1f)
        } else {
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
                        resetAppListHomeTreatment()
                    }
                }
                .start()
            animateAppListHomeTreatment(toProgress = 0f)
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

    private fun animateAppListHomeTreatment(toProgress: Float) {
        appListHomeTreatmentAnimator?.cancel()
        appListHomeTreatmentAnimator = ValueAnimator.ofFloat(
            binding.homeTransitionDimOverlay.alpha / APP_LIST_HOME_DIM_ALPHA,
            toProgress,
        ).apply {
            duration = PAGE_SETTLE_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                if (progress > 0f) {
                    binding.homeTransitionDimOverlay.visibility = View.VISIBLE
                }
                applyAppListHomeTreatment(progress)
            }
            doOnEnd {
                appListHomeTreatmentAnimator = null
            }
            start()
        }
    }

    private fun resetAppListHomeTreatment() {
        binding.homeTransitionDimOverlay.animate().cancel()
        appListHomeTreatmentAnimator?.cancel()
        appListHomeTreatmentAnimator = null
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

        return when {
            isHorizontalDrag &&
                !isNotesVisible &&
                !isCalendarVisible &&
                !isTodayVisible &&
                deltaX < 0 &&
                binding.showNotesPageSwitch.isChecked -> {
                    PageSwipeTarget.HomeToNotes
            }
            isHorizontalDrag &&
                !isNotesVisible &&
                !isCalendarVisible &&
                !isTodayVisible &&
                deltaX > 0 &&
                binding.showCalendarPageSwitch.isChecked -> {
                    PageSwipeTarget.HomeToCalendar
            }
            isHorizontalDrag && isNotesVisible && deltaX > 0 -> PageSwipeTarget.NotesToHome
            isHorizontalDrag && isCalendarVisible && deltaX < 0 -> PageSwipeTarget.CalendarToHome
            isVerticalDrag &&
                !isNotesVisible &&
                !isCalendarVisible &&
                !isTodayVisible &&
                deltaY < 0 &&
                binding.showTodayPageSwitch.isChecked -> {
                    PageSwipeTarget.HomeToToday
            }
            isVerticalDrag && isTodayVisible && deltaY > 0 -> PageSwipeTarget.TodayToHome
            else -> null
        }
    }

    private fun preparePageDrag(target: PageSwipeTarget) {
        val width = pageWidth()
        val height = pageHeight()
        binding.homeContent.animate().cancel()
        binding.notesRoot.animate().cancel()
        binding.calendarRoot.animate().cancel()
        binding.todayRoot.animate().cancel()
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
            PageSwipeTarget.HomeToToday -> {
                binding.todayRoot.translationY = height
                binding.todayRoot.visibility = View.VISIBLE
            }
            PageSwipeTarget.TodayToHome -> {
                binding.homeContent.translationY = -height
                binding.homeContent.visibility = View.VISIBLE
            }
        }
    }

    private fun applyPageDrag(target: PageSwipeTarget, deltaX: Float, deltaY: Float) {
        val width = pageWidth()
        val height = pageHeight()
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
            PageSwipeTarget.HomeToToday -> {
                val drag = deltaY.coerceIn(-height, 0f)
                binding.homeContent.translationY = drag
                binding.todayRoot.translationY = height + drag
            }
            PageSwipeTarget.TodayToHome -> {
                val drag = deltaY.coerceIn(0f, height)
                binding.todayRoot.translationY = drag
                binding.homeContent.translationY = -height + drag
            }
        }
    }

    private fun shouldCompletePageDrag(
        target: PageSwipeTarget,
        deltaX: Float,
        deltaY: Float,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        val pageSize = when (target) {
            PageSwipeTarget.HomeToToday,
            PageSwipeTarget.TodayToHome -> pageHeight()
            else -> pageWidth()
        }
        val dragDistance = when (target) {
            PageSwipeTarget.HomeToToday,
            PageSwipeTarget.TodayToHome -> deltaY
            else -> deltaX
        }
        val distancePasses = kotlin.math.abs(dragDistance) > pageSize * PAGE_SWIPE_COMPLETE_FRACTION
        val velocityPasses = when (target) {
            PageSwipeTarget.HomeToNotes,
            PageSwipeTarget.CalendarToHome -> velocityX < -PAGE_SWIPE_COMPLETE_VELOCITY
            PageSwipeTarget.HomeToCalendar,
            PageSwipeTarget.NotesToHome -> velocityX > PAGE_SWIPE_COMPLETE_VELOCITY
            PageSwipeTarget.HomeToToday -> velocityY < -PAGE_SWIPE_COMPLETE_VELOCITY
            PageSwipeTarget.TodayToHome -> velocityY > PAGE_SWIPE_COMPLETE_VELOCITY
        }
        return distancePasses || velocityPasses
    }

    private fun settlePageDrag(target: PageSwipeTarget, shouldComplete: Boolean) {
        val width = pageWidth()
        val height = pageHeight()
        when (target) {
            PageSwipeTarget.HomeToNotes -> {
                animatePagePair(
                    outgoing = binding.homeContent,
                    incoming = binding.notesRoot,
                    outgoingEnd = if (shouldComplete) -width else 0f,
                    incomingEnd = if (shouldComplete) 0f else width,
                ) {
                    isNotesVisible = shouldComplete
                    if (shouldComplete) {
                        binding.homeContent.visibility = View.GONE
                    } else {
                        binding.notesRoot.visibility = View.GONE
                    }
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
                        binding.homeContent.visibility = View.GONE
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
                    if (shouldComplete) {
                        binding.notesRoot.visibility = View.GONE
                    } else {
                        binding.homeContent.visibility = View.GONE
                    }
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
                    if (shouldComplete) {
                        binding.calendarRoot.visibility = View.GONE
                    } else {
                        binding.homeContent.visibility = View.GONE
                    }
                }
            }
            PageSwipeTarget.HomeToToday -> {
                animateVerticalPagePair(
                    outgoing = binding.homeContent,
                    incoming = binding.todayRoot,
                    outgoingEnd = if (shouldComplete) -height else 0f,
                    incomingEnd = if (shouldComplete) 0f else height,
                ) {
                    isTodayVisible = shouldComplete
                    if (shouldComplete) {
                        binding.homeContent.visibility = View.GONE
                        refreshTodayWidgets()
                    } else {
                        binding.todayRoot.visibility = View.GONE
                    }
                }
            }
            PageSwipeTarget.TodayToHome -> {
                animateVerticalPagePair(
                    outgoing = binding.todayRoot,
                    incoming = binding.homeContent,
                    outgoingEnd = if (shouldComplete) height else 0f,
                    incomingEnd = if (shouldComplete) 0f else -height,
                ) {
                    isTodayVisible = !shouldComplete
                    if (shouldComplete) {
                        binding.todayRoot.visibility = View.GONE
                    } else {
                        binding.homeContent.visibility = View.GONE
                    }
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
        if (isTodayVisible && todayWidgets.any { it.type == TodayWidgetType.PinnedNote }) {
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
        if (binding.showQuickAccessSwitch.isChecked != state.showQuickAccess) {
            binding.showQuickAccessSwitch.isChecked = state.showQuickAccess
        }
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
        currentOpenScreenTimeGesture = state.openScreenTimeGesture
        currentLockScreenGesture = state.lockScreenGesture
        binding.openScreenTimeGestureValue.text = gestureLabel(state.openScreenTimeGesture)
        binding.lockScreenGestureValue.text = gestureLabel(state.lockScreenGesture)
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
        if (!state.showTodayPage && isTodayVisible) {
            hideTodayPage()
        }
        renderCalendarSelection()
        renderAppBlockingSelection()
        renderAppBudgetsSelection()
        if (isCalendarVisible && hasCalendarPermission()) {
            refreshCalendarEvents()
        }
        if (isTodayVisible) {
            refreshTodayWidgets()
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
            binding.todayRoot.updatePadding(top = systemBars.top)
            binding.screenTimeRoot.updatePadding(top = systemBars.top)
            binding.editControls.updatePadding(top = systemBars.top)
            binding.settingsRoot.updatePadding(top = systemBars.top)
            binding.noteEditorRoot.updatePadding(
                top = noteEditorBaseTopPadding + systemBars.top,
                bottom = noteEditorBaseBottomPadding + systemBars.bottom,
            )
            binding.shortcutList.updatePadding(
                bottom = shortcutBaseBottomPadding +
                    if (binding.quickAccessBar.isVisible) 0 else systemBars.bottom,
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
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                    noteBulletFormatter.onTextChanged(text, start, before, count)
                }

                override fun afterTextChanged(text: Editable?) {
                    noteBulletFormatter.formatAfterTextChanged(binding.noteEditorInput, text ?: return)
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
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
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
        renderTodayWidgets()
    }

    private fun renderTodayWidgets() {
        binding.todayWidgetGrid.isEditingWidgets = isTodayEditMode
        binding.todayWidgetGrid.removeAllViews()
        if (todayWidgets.isEmpty()) return

        todayWidgets.forEach { widget ->
            val widgetView = when (widget.type) {
                TodayWidgetType.NextEvent -> createNextEventWidgetView(widget)
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

    private fun LinearLayout.addNotificationTextRow(title: String, text: String) {
        addView(
            LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
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
                    showNoteEditor(pinnedNote)
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
                addView(
                    TextView(this@MainActivity).apply {
                        text = pinnedNote?.text ?: getString(R.string.today_pinned_note_empty)
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
            else -> event.title
        }
    }

    private fun nextEventSecondaryText(event: CalendarEvent?): String {
        return when {
            !hasCalendarPermission() -> getString(R.string.calendar_permission_button)
            event == null -> ""
            event.isAllDay -> getString(R.string.today_next_event_all_day, event.calendarName)
            else -> getString(
                R.string.today_next_event_time,
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(event.startMillis)),
                event.calendarName,
            )
        }
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
                    } else if (isSettingsVisible) {
                        hideSettings()
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
                    viewModel.deleteNote(note)
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

    private fun showNotesPage() {
        if (isNotesVisible || isCalendarVisible || isTodayVisible || !binding.showNotesPageSwitch.isChecked || isEditMode) return
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
            .withEndAction {
                if (isNotesVisible) {
                    binding.homeContent.visibility = View.GONE
                }
            }
            .start()
    }

    private fun hideNotesPage() {
        if (!isNotesVisible) return
        isNotesVisible = false
        val width = binding.homeRoot.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        binding.homeContent.animate().cancel()
        binding.notesRoot.animate().cancel()
        binding.homeContent.visibility = View.VISIBLE
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
        if (isCalendarVisible || isNotesVisible || isTodayVisible || !binding.showCalendarPageSwitch.isChecked || isEditMode) return
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
            .withEndAction {
                if (isCalendarVisible) {
                    binding.homeContent.visibility = View.GONE
                }
            }
            .start()
        onCalendarPageVisible()
    }

    private fun hideCalendarPage() {
        if (!isCalendarVisible) return
        isCalendarVisible = false
        val width = binding.homeRoot.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        binding.homeContent.animate().cancel()
        binding.calendarRoot.animate().cancel()
        binding.homeContent.visibility = View.VISIBLE
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

    private fun showTodayPage() {
        if (isTodayVisible || isNotesVisible || isCalendarVisible || !binding.showTodayPageSwitch.isChecked || isEditMode) return
        isTodayVisible = true
        refreshTodayWidgets()
        val height = binding.homeRoot.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        binding.todayRoot.translationY = height.toFloat()
        binding.todayRoot.visibility = View.VISIBLE
        binding.homeContent.animate().cancel()
        binding.todayRoot.animate().cancel()
        binding.homeContent.animate()
            .translationY(-height.toFloat())
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        binding.todayRoot.animate()
            .translationY(0f)
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                if (isTodayVisible) {
                    binding.homeContent.visibility = View.GONE
                }
            }
            .start()
    }

    private fun hideTodayPage() {
        if (!isTodayVisible) return
        exitTodayEditMode()
        isTodayVisible = false
        val height = binding.homeRoot.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        binding.homeContent.animate().cancel()
        binding.todayRoot.animate().cancel()
        binding.homeContent.visibility = View.VISIBLE
        binding.homeContent.animate()
            .translationY(0f)
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        binding.todayRoot.animate()
            .translationY(height.toFloat())
            .setDuration(PAGE_SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                if (!isTodayVisible) {
                    binding.todayRoot.visibility = View.GONE
                    binding.todayRoot.translationY = 0f
                }
            }
            .start()
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
        availableApps = installedAppsRepository.loadLaunchableApps()
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
        binding.appSearchInput.requestFocus()
        showKeyboard()
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

    private fun launchQuickAccessIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showQuickAccessUnavailable()
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
        const val GOOGLE_KEEP_PACKAGE = "com.google.android.keep"
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
        const val MINUTES_PER_HOUR = 60L
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
