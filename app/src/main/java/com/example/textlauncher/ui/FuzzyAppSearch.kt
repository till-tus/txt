package com.example.textlauncher.ui

import com.example.textlauncher.domain.AppShortcut
import java.util.Locale
import kotlin.math.min

object FuzzyAppSearch {
    fun filter(apps: List<AppShortcut>, query: String): List<AppShortcut> {
        val normalizedQuery = query.normalized()
        if (normalizedQuery.isBlank()) return apps

        return apps
            .mapNotNull { app ->
                val label = app.label.normalized()
                val score = score(label, normalizedQuery) ?: return@mapNotNull null
                ScoredApp(app, score)
            }
            .sortedWith(compareBy<ScoredApp> { it.score }.thenBy { it.app.label.lowercase(Locale.getDefault()) })
            .map { it.app }
    }

    private fun score(label: String, query: String): Int? {
        val substringIndex = label.indexOf(query)
        if (substringIndex >= 0) return substringIndex

        val subsequenceScore = subsequenceScore(label, query)
        val distance = levenshteinDistance(label, query)
        val maxDistance = maxDistanceFor(query)

        return when {
            distance <= maxDistance -> EXACT_MISS_BASE + distance
            subsequenceScore != null -> SUBSEQUENCE_BASE + subsequenceScore
            else -> null
        }
    }

    private fun subsequenceScore(label: String, query: String): Int? {
        var labelIndex = 0
        var score = 0
        query.forEach { queryChar ->
            val matchIndex = label.indexOf(queryChar, labelIndex)
            if (matchIndex < 0) return null
            score += matchIndex - labelIndex
            labelIndex = matchIndex + 1
        }
        return score
    }

    private fun levenshteinDistance(left: String, right: String): Int {
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        val previous = IntArray(right.length + 1) { it }
        val current = IntArray(right.length + 1)

        for (leftIndex in left.indices) {
            current[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val substitutionCost = if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = min(
                    min(current[rightIndex] + 1, previous[rightIndex + 1] + 1),
                    previous[rightIndex] + substitutionCost,
                )
            }
            current.copyInto(previous)
        }

        return previous[right.length]
    }

    private fun maxDistanceFor(query: String): Int {
        return when {
            query.length <= 3 -> 1
            query.length <= 6 -> 2
            else -> 3
        }
    }

    private fun String.normalized(): String {
        return lowercase(Locale.getDefault()).filter { it.isLetterOrDigit() }
    }

    private data class ScoredApp(
        val app: AppShortcut,
        val score: Int,
    )

    private const val EXACT_MISS_BASE = 100
    private const val SUBSEQUENCE_BASE = 200
}
