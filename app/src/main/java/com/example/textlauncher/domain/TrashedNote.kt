package com.example.textlauncher.domain

data class TrashedNote(
    val note: QuickNote,
    val originalPosition: Int,
    val deletedAtMillis: Long,
)
