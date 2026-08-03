package com.example.textlauncher.ui

import java.util.Locale
import kotlin.math.min

internal object FuzzyTextSearch {
    fun score(text: String, query: String): Int? {
        val normalizedText = text.normalizedForSearch()
        val normalizedQuery = query.normalizedForSearch()
        if (normalizedQuery.isBlank()) return 0

        val substringIndex = normalizedText.indexOf(normalizedQuery)
        if (substringIndex >= 0) return substringIndex

        val subsequenceScore = subsequenceScore(normalizedText, normalizedQuery)
        val distance = levenshteinDistance(normalizedText, normalizedQuery)
        val maxDistance = maxDistanceFor(normalizedQuery)

        return when {
            distance <= maxDistance -> EXACT_MISS_BASE + distance
            subsequenceScore != null -> SUBSEQUENCE_BASE + subsequenceScore
            else -> null
        }
    }

    private fun subsequenceScore(text: String, query: String): Int? {
        var textIndex = 0
        var score = 0
        query.forEach { queryChar ->
            val matchIndex = text.indexOf(queryChar, textIndex)
            if (matchIndex < 0) return null
            score += matchIndex - textIndex
            textIndex = matchIndex + 1
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

    private fun String.normalizedForSearch(): String {
        return lowercase(Locale.getDefault()).filter { it.isLetterOrDigit() }
    }

    private const val EXACT_MISS_BASE = 100
    private const val SUBSEQUENCE_BASE = 200
}
