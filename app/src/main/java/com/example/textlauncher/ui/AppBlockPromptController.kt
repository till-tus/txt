package com.example.textlauncher.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import com.example.textlauncher.R
import com.example.textlauncher.databinding.ActivityMainBinding
import com.example.textlauncher.domain.AppShortcut

internal data class AppBudgetOverrun(
    val budgetMinutes: Int,
    val usageMillis: Long,
)

internal class AppBlockPromptController(
    private val context: Context,
    private val binding: ActivityMainBinding,
    private val formatDuration: (Long) -> String,
    private val onProceed: (AppShortcut, Int) -> Unit,
) {
    private var pendingShortcut: AppShortcut? = null
    private var selectedIntentMinutes: Int? = null
    private var isCountdownComplete = false
    private var countdownTimer: CountDownTimer? = null

    val isVisible: Boolean
        get() = binding.appBlockPromptRoot.visibility == View.VISIBLE

    fun configure() {
        binding.appBlockCancelButton.setOnClickListener {
            hide()
        }
        binding.appBlockProceedButton.setOnClickListener {
            val shortcut = pendingShortcut ?: return@setOnClickListener
            val minutes = selectedIntentMinutes ?: return@setOnClickListener
            hide()
            onProceed(shortcut, minutes)
        }
        binding.appBlockIntent5Button.setOnClickListener {
            selectIntentMinutes(5)
        }
        binding.appBlockIntent10Button.setOnClickListener {
            selectIntentMinutes(10)
        }
        binding.appBlockIntent15Button.setOnClickListener {
            selectIntentMinutes(15)
        }
    }

    fun show(shortcut: AppShortcut, budgetOverrun: AppBudgetOverrun?) {
        pendingShortcut = shortcut
        selectedIntentMinutes = null
        isCountdownComplete = false
        countdownTimer?.cancel()
        binding.appBlockPromptTitle.text = context.getString(R.string.app_blocking_prompt_title, shortcut.label)
        if (budgetOverrun == null) {
            binding.appBlockPromptMessage.text = context.getString(R.string.app_blocking_prompt_message)
            binding.appBlockPromptMessage.setTextColor(context.getColor(R.color.launcher_text_secondary))
        } else {
            binding.appBlockPromptMessage.text = context.getString(
                R.string.app_budget_prompt_message,
                formatDuration(budgetOverrun.budgetMinutes * MILLIS_PER_MINUTE),
                formatDuration(budgetOverrun.usageMillis),
            )
            binding.appBlockPromptMessage.setTextColor(context.getColor(R.color.launcher_warning))
        }
        renderIntentSelection()
        renderProceedState()
        binding.appBlockPromptRoot.alpha = 0f
        binding.appBlockPromptRoot.visibility = View.VISIBLE
        binding.appBlockPromptRoot.animate()
            .alpha(1f)
            .setDuration(APP_BLOCK_PROMPT_FADE_MS)
            .start()
        startCountdown()
    }

    fun hide() {
        countdownTimer?.cancel()
        countdownTimer = null
        pendingShortcut = null
        selectedIntentMinutes = null
        isCountdownComplete = false
        binding.appBlockPromptRoot.animate().cancel()
        binding.appBlockPromptRoot.visibility = View.GONE
        binding.appBlockPromptRoot.alpha = 1f
        binding.appBlockProceedButton.isEnabled = false
        binding.appBlockProceedButton.alpha = DISABLED_ACTION_ALPHA
    }

    fun cancel() {
        countdownTimer?.cancel()
        countdownTimer = null
    }

    private fun startCountdown() {
        renderProceedCountdown(APP_BLOCK_WAIT_SECONDS)
        countdownTimer = object : CountDownTimer(APP_BLOCK_WAIT_MS, APP_BLOCK_COUNTDOWN_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = ((millisUntilFinished + 999L) / 1_000L).toInt()
                renderProceedCountdown(secondsRemaining)
            }

            override fun onFinish() {
                isCountdownComplete = true
                renderProceedState()
            }
        }.start()
    }

    private fun renderProceedCountdown(secondsRemaining: Int) {
        binding.appBlockProceedButton.text = context.getString(
            R.string.app_blocking_proceed_countdown,
            secondsRemaining.coerceAtLeast(1),
        )
    }

    private fun selectIntentMinutes(minutes: Int) {
        selectedIntentMinutes = minutes
        renderIntentSelection()
        renderProceedState()
    }

    private fun renderIntentSelection() {
        renderIntentOption(binding.appBlockIntent5Button, 5)
        renderIntentOption(binding.appBlockIntent10Button, 10)
        renderIntentOption(binding.appBlockIntent15Button, 15)
    }

    private fun renderIntentOption(view: TextView, minutes: Int) {
        val isSelected = selectedIntentMinutes == minutes
        view.alpha = if (isSelected) 1f else 0.68f
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            setStroke(
                context.dp(1),
                context.getColor(if (isSelected) R.color.launcher_text else R.color.settings_option_divider),
            )
        }
    }

    private fun renderProceedState() {
        val canProceed = isCountdownComplete && selectedIntentMinutes != null
        binding.appBlockProceedButton.isEnabled = canProceed
        binding.appBlockProceedButton.alpha = if (canProceed) 1f else DISABLED_ACTION_ALPHA
        if (isCountdownComplete) {
            binding.appBlockProceedButton.text = context.getString(R.string.app_blocking_proceed)
        }
    }

    private fun Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private companion object {
        const val APP_BLOCK_WAIT_SECONDS = 5
        const val APP_BLOCK_WAIT_MS = 5_000L
        const val APP_BLOCK_COUNTDOWN_INTERVAL_MS = 1_000L
        const val APP_BLOCK_PROMPT_FADE_MS = 160L
        const val DISABLED_ACTION_ALPHA = 0.34f
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
