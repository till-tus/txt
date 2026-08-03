package com.example.textlauncher.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import com.example.textlauncher.R
import com.google.android.material.button.MaterialButton

internal enum class OnboardingSurface {
    Home,
    AppList,
    HomeEdit,
    Notes,
    NoteEditor,
    Calendar,
    Today,
    TodayEdit,
    ScreenTime,
    Settings,
    AppearanceSettings,
    NotesSettings,
    CalendarSettings,
    GesturesSettings,
    ScreenTimeSettings,
}

internal data class OnboardingStep(
    val title: CharSequence,
    val body: CharSequence,
    val target: () -> View? = { null },
)

/** Coordinates persisted, per-surface guidance with a single overlay attached above the real UI. */
internal class OnboardingController(
    context: Context,
    host: FrameLayout,
) {
    private val store = OnboardingStore(context)
    private val overlay = OnboardingOverlay(context).also { view ->
        host.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    val isShowing: Boolean
        get() = overlay.visibility == View.VISIBLE

    fun needsGuidance(surface: OnboardingSurface): Boolean {
        return store.hasSeenWelcome && !store.isComplete(surface)
    }

    fun maybeShowWelcome(onExplore: () -> Unit): Boolean {
        if (store.hasSeenWelcome || isShowing) return false
        overlay.showWelcome(
            onExplore = {
                store.hasSeenWelcome = true
                onExplore()
            },
            onSkipAll = {
                store.skipAll()
            },
        )
        return true
    }

    fun show(
        surface: OnboardingSurface,
        steps: List<OnboardingStep>,
        onWillShow: () -> Unit = {},
        onClosed: () -> Unit = {},
    ): Boolean {
        if (!store.hasSeenWelcome || store.isComplete(surface) || isShowing || steps.isEmpty()) return false
        onWillShow()
        overlay.showSteps(
            steps = steps,
            onFinished = {
                store.complete(surface)
                onClosed()
            },
            onSkipped = {
                store.complete(surface)
                onClosed()
            },
        )
        return true
    }

    fun skipCurrent(): Boolean = overlay.skipCurrent()

    fun reset() {
        overlay.hide()
        store.reset()
    }
}

private class OnboardingStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var hasSeenWelcome: Boolean
        get() = preferences.getBoolean(KEY_WELCOME_SEEN, false)
        set(value) = preferences.edit { putBoolean(KEY_WELCOME_SEEN, value) }

    fun isComplete(surface: OnboardingSurface): Boolean {
        return surface.name in preferences.getStringSet(KEY_COMPLETED_SURFACES, emptySet()).orEmpty()
    }

    fun complete(surface: OnboardingSurface) {
        val completed = preferences.getStringSet(KEY_COMPLETED_SURFACES, emptySet()).orEmpty() + surface.name
        preferences.edit { putStringSet(KEY_COMPLETED_SURFACES, completed) }
    }

    fun skipAll() {
        preferences.edit {
            putBoolean(KEY_WELCOME_SEEN, true)
            putStringSet(KEY_COMPLETED_SURFACES, OnboardingSurface.entries.map { it.name }.toSet())
        }
    }

    fun reset() {
        preferences.edit { clear() }
    }

    private companion object {
        const val PREFERENCES_NAME = "onboarding_state"
        const val KEY_WELCOME_SEEN = "welcomeSeen"
        const val KEY_COMPLETED_SURFACES = "completedSurfaces"
    }
}

private class OnboardingOverlay(context: Context) : FrameLayout(context) {
    private val scrim = SpotlightScrimView(context)
    private val card = LinearLayout(context)
    private val stepLabel = TextView(context)
    private val title = TextView(context)
    private val body = TextView(context)
    private val skipButton = MaterialButton(context)
    private val nextButton = MaterialButton(context)
    private var steps = emptyList<OnboardingStep>()
    private var stepIndex = 0
    private var onFinished: (() -> Unit)? = null
    private var onSkipped: (() -> Unit)? = null

    init {
        id = R.id.onboardingOverlay
        visibility = View.GONE
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES

        addView(
            scrim,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.START
        card.setPadding(20.dp, 18.dp, 20.dp, 12.dp)
        card.elevation = 12.dp.toFloat()
        card.isClickable = true
        card.isFocusable = true
        card.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 22.dp.toFloat()
            setColor(Color.rgb(24, 24, 24))
            setStroke(1.dp, Color.argb(72, 255, 255, 255))
        }

        stepLabel.id = R.id.onboardingStepLabel
        stepLabel.setTextColor(Color.argb(185, 255, 255, 255))
        stepLabel.textSize = 12f
        stepLabel.typeface = Typeface.DEFAULT_BOLD
        stepLabel.letterSpacing = 0.08f
        card.addView(
            stepLabel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        title.id = R.id.onboardingTitle
        title.setTextColor(Color.WHITE)
        title.textSize = 22f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setPadding(0, 8.dp, 0, 0)
        ViewCompat.setAccessibilityHeading(title, true)
        card.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        body.id = R.id.onboardingBody
        body.setTextColor(Color.argb(220, 255, 255, 255))
        body.textSize = 16f
        body.setLineSpacing(3.dp.toFloat(), 1f)
        body.setPadding(0, 8.dp, 0, 8.dp)
        card.addView(
            body,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        skipButton.id = R.id.onboardingSkipButton
        skipButton.text = context.getString(R.string.onboarding_skip)
        skipButton.contentDescription = context.getString(R.string.onboarding_skip)
        skipButton.setTextColor(Color.argb(210, 255, 255, 255))
        skipButton.setBackgroundColor(Color.TRANSPARENT)
        actions.addView(
            skipButton,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 48.dp),
        )

        nextButton.id = R.id.onboardingNextButton
        nextButton.setTextColor(Color.BLACK)
        nextButton.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        actions.addView(
            nextButton,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 48.dp).apply {
                marginStart = 8.dp
            },
        )
        card.addView(
            actions,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        addView(card, defaultCardLayoutParams())
    }

    fun showWelcome(onExplore: () -> Unit, onSkipAll: () -> Unit) {
        steps = emptyList()
        stepIndex = 0
        onFinished = null
        onSkipped = {
            hide()
            onSkipAll()
        }
        scrim.setHighlight(null)
        stepLabel.text = context.getString(R.string.onboarding_welcome_eyebrow)
        title.text = context.getString(R.string.onboarding_welcome_title)
        body.text = context.getString(R.string.onboarding_welcome_body)
        skipButton.text = context.getString(R.string.onboarding_skip_all)
        skipButton.contentDescription = context.getString(R.string.onboarding_skip_all)
        nextButton.text = context.getString(R.string.onboarding_explore)
        skipButton.setOnClickListener { onSkipped?.invoke() }
        nextButton.setOnClickListener {
            hide()
            onExplore()
        }
        showCard(centered = true)
    }

    fun showSteps(
        steps: List<OnboardingStep>,
        onFinished: () -> Unit,
        onSkipped: () -> Unit,
    ) {
        this.steps = steps
        stepIndex = 0
        this.onFinished = onFinished
        this.onSkipped = onSkipped
        skipButton.text = context.getString(R.string.onboarding_skip)
        skipButton.contentDescription = context.getString(R.string.onboarding_skip)
        skipButton.setOnClickListener {
            val callback = this.onSkipped
            hide()
            callback?.invoke()
        }
        nextButton.setOnClickListener { advance() }
        visibility = View.VISIBLE
        renderStep()
    }

    fun skipCurrent(): Boolean {
        if (visibility != View.VISIBLE) return false
        onSkipped?.let { callback ->
            hide()
            callback()
        } ?: hide()
        return true
    }

    fun hide() {
        visibility = View.GONE
        scrim.setHighlight(null)
        steps = emptyList()
        stepIndex = 0
        onFinished = null
        onSkipped = null
    }

    private fun advance() {
        if (stepIndex < steps.lastIndex) {
            stepIndex += 1
            renderStep()
        } else {
            val callback = onFinished
            hide()
            callback?.invoke()
        }
    }

    private fun renderStep() {
        val step = steps[stepIndex]
        stepLabel.text = context.getString(R.string.onboarding_step_count, stepIndex + 1, steps.size)
        title.text = step.title
        body.text = step.body
        nextButton.text = context.getString(
            if (stepIndex == steps.lastIndex) R.string.onboarding_done else R.string.onboarding_next,
        )
        visibility = View.VISIBLE
        post {
            val targetRect = targetRect(step.target())
            scrim.setHighlight(targetRect)
            positionCard(targetRect)
            card.requestFocus()
            card.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }

    private fun showCard(centered: Boolean) {
        visibility = View.VISIBLE
        val params = defaultCardLayoutParams()
        params.gravity = if (centered) Gravity.CENTER else Gravity.BOTTOM
        card.layoutParams = params
        card.requestFocus()
        card.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    private fun targetRect(target: View?): RectF? {
        if (target == null || !target.isShown || target.width <= 0 || target.height <= 0) return null
        val overlayLocation = IntArray(2)
        val targetLocation = IntArray(2)
        getLocationInWindow(overlayLocation)
        target.getLocationInWindow(targetLocation)
        val padding = 8.dp.toFloat()
        return RectF(
            targetLocation[0] - overlayLocation[0] - padding,
            targetLocation[1] - overlayLocation[1] - padding,
            targetLocation[0] - overlayLocation[0] + target.width + padding,
            targetLocation[1] - overlayLocation[1] + target.height + padding,
        ).apply {
            left = left.coerceAtLeast(8.dp.toFloat())
            top = top.coerceAtLeast(8.dp.toFloat())
            right = right.coerceAtMost(this@OnboardingOverlay.width.toFloat() - 8.dp)
            bottom = bottom.coerceAtMost(this@OnboardingOverlay.height.toFloat() - 8.dp)
        }
    }

    private fun positionCard(targetRect: RectF?) {
        val params = defaultCardLayoutParams()
        params.gravity = when {
            targetRect == null -> Gravity.CENTER
            targetRect.centerY() < height / 2f -> Gravity.BOTTOM
            else -> Gravity.TOP
        }
        card.layoutParams = params
    }

    private fun defaultCardLayoutParams(): LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            marginStart = 18.dp
            marginEnd = 18.dp
            topMargin = 52.dp
            bottomMargin = 36.dp
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}

private class SpotlightScrimView(context: Context) : View(context) {
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(205, 0, 0, 0)
    }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.dp.toFloat()
        color = Color.WHITE
    }
    private var highlight: RectF? = null
    private var pulse = 0f
    private var animator: ValueAnimator? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setHighlight(rect: RectF?) {
        highlight = rect
        animator?.cancel()
        animator = null
        if (rect != null) {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1_100L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    pulse = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        val rect = highlight ?: return
        val cornerRadius = 18.dp.toFloat()
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, clearPaint)
        ringPaint.alpha = (180 + (75 * pulse)).toInt()
        val expansion = 1.dp + (2.dp * pulse)
        val ring = RectF(rect).apply { inset(-expansion, -expansion) }
        canvas.drawRoundRect(ring, cornerRadius + expansion, cornerRadius + expansion, ringPaint)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
