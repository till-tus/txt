package com.example.textlauncher.ui

import com.example.textlauncher.domain.AppShortcut
import java.util.Locale

object FuzzyAppSearch {
    fun filter(apps: List<AppShortcut>, query: String): List<AppShortcut> {
        if (query.isBlank()) return apps

        return apps
            .mapNotNull { app ->
                val score = FuzzyTextSearch.score(app.label, query) ?: return@mapNotNull null
                ScoredApp(app, score)
            }
            .sortedWith(compareBy<ScoredApp> { it.score }.thenBy { it.app.label.lowercase(Locale.getDefault()) })
            .map { it.app }
    }

    private data class ScoredApp(
        val app: AppShortcut,
        val score: Int,
    )

}
