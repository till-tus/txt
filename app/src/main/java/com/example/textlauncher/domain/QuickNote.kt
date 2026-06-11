package com.example.textlauncher.domain

data class QuickNote(
    val id: Long,
    val text: String,
    val isPinned: Boolean = false,
    val audioFileName: String? = null,
    val audioDurationMillis: Long = 0L,
    val audioWaveform: List<Int> = emptyList(),
)
