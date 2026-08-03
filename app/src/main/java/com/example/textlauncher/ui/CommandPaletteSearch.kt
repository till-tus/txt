package com.example.textlauncher.ui

import java.util.Locale

internal object CommandPaletteSearch {
    fun search(items: List<CommandPaletteItem>, rawQuery: String): List<CommandPaletteItem> {
        val parsed = parse(rawQuery)
        return items.asSequence()
            .filter { item -> parsed.category == null || item.category == parsed.category }
            .mapNotNull { item ->
                val score = FuzzyTextSearch.score(item.searchText, parsed.query)
                    ?: FuzzyTextSearch.score(item.title, parsed.query)
                    ?: return@mapNotNull null
                ScoredItem(item, score + item.rankingBoost())
            }
            .sortedWith(
                compareBy<ScoredItem> { it.score }
                    .thenBy { it.item.defaultOrder() }
                    .thenBy { it.item.title.lowercase(Locale.getDefault()) },
            )
            .map { it.item }
            .toList()
    }

    private fun parse(rawQuery: String): ParsedQuery {
        val trimmed = rawQuery.trim()
        val separator = trimmed.indexOf(':')
        if (separator <= 0) return ParsedQuery(query = trimmed)

        val category = when (trimmed.substring(0, separator).lowercase(Locale.ROOT)) {
            "app" -> CommandPaletteItem.Category.App
            "note" -> CommandPaletteItem.Category.Note
            "event" -> CommandPaletteItem.Category.Event
            "settings" -> CommandPaletteItem.Category.Settings
            else -> null
        } ?: return ParsedQuery(query = trimmed)
        return ParsedQuery(
            query = trimmed.substring(separator + 1).trim(),
            category = category,
        )
    }

    private fun CommandPaletteItem.rankingBoost(): Int {
        return if (this is CommandPaletteItem.Note && note.isPinned) -1 else 0
    }

    private fun CommandPaletteItem.defaultOrder(): Int {
        return when (category) {
            CommandPaletteItem.Category.App -> 0
            CommandPaletteItem.Category.Action -> 1
            CommandPaletteItem.Category.Page -> 2
            CommandPaletteItem.Category.Note -> 3
            CommandPaletteItem.Category.Event -> 4
            CommandPaletteItem.Category.Settings -> 5
        }
    }

    private data class ParsedQuery(
        val query: String,
        val category: CommandPaletteItem.Category? = null,
    )

    private data class ScoredItem(
        val item: CommandPaletteItem,
        val score: Int,
    )
}
