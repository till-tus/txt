package com.example.textlauncher.domain

data class QuickNote(
    val id: Long,
    val text: String,
    val isPinned: Boolean = false,
)
