package com.example.textlauncher.domain

enum class LauncherPage {
    Notes,
    Today,
    Calendar,
}

enum class PagePosition {
    Left,
    Right,
    Down,
}

data class PageArrangement(
    val notesPosition: PagePosition = PagePosition.Right,
    val todayPosition: PagePosition = PagePosition.Down,
    val calendarPosition: PagePosition = PagePosition.Left,
) {
    fun positionOf(page: LauncherPage): PagePosition {
        return when (page) {
            LauncherPage.Notes -> notesPosition
            LauncherPage.Today -> todayPosition
            LauncherPage.Calendar -> calendarPosition
        }
    }

    fun pageAt(position: PagePosition): LauncherPage? {
        return LauncherPage.entries.firstOrNull { positionOf(it) == position }
    }

    fun move(page: LauncherPage, target: PagePosition): PageArrangement {
        if (target !in AllowedPositions) return this
        val current = positionOf(page)
        if (current == target) return this
        val displaced = pageAt(target)
        return withPosition(page, target).let { moved ->
            if (displaced == null) moved else moved.withPosition(displaced, current)
        }
    }

    fun isValid(): Boolean {
        val positions = LauncherPage.entries.map(::positionOf)
        return positions.all { it in AllowedPositions } &&
            positions.distinct().size == LauncherPage.entries.size
    }

    private fun withPosition(page: LauncherPage, position: PagePosition): PageArrangement {
        return when (page) {
            LauncherPage.Notes -> copy(notesPosition = position)
            LauncherPage.Today -> copy(todayPosition = position)
            LauncherPage.Calendar -> copy(calendarPosition = position)
        }
    }

    companion object {
        val AllowedPositions = listOf(
            PagePosition.Left,
            PagePosition.Right,
            PagePosition.Down,
        )
        val Default = PageArrangement()

        fun validatedOrDefault(
            notesPosition: PagePosition?,
            todayPosition: PagePosition?,
            calendarPosition: PagePosition?,
        ): PageArrangement {
            val arrangement = PageArrangement(
                notesPosition = notesPosition ?: Default.notesPosition,
                todayPosition = todayPosition ?: Default.todayPosition,
                calendarPosition = calendarPosition ?: Default.calendarPosition,
            )
            return arrangement.takeIf(PageArrangement::isValid) ?: Default
        }
    }
}
