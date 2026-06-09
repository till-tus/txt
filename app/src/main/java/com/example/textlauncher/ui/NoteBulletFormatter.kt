package com.example.textlauncher.ui

import android.text.Editable
import android.widget.EditText

internal class NoteBulletFormatter {
    private var isFormatting = false
    private var didInsertNewline = false

    fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
        if (isFormatting) return
        didInsertNewline = didInsertNewline(text, start, before, count)
    }

    fun formatAfterTextChanged(input: EditText, text: Editable) {
        if (isFormatting) return
        if (!didInsertNewline) return
        didInsertNewline = false

        val cursorPosition = input.selectionStart
        val result = formatAfterNewline(text.toString(), cursorPosition) ?: return

        isFormatting = true
        try {
            when (result) {
                is FormatResult.DeleteEmptyBullet -> {
                    text.delete(result.start, result.end)
                    input.setSelection(result.selection)
                }
                is FormatResult.InsertBullet -> {
                    text.insert(result.index, BULLET_PREFIX)
                    input.setSelection(result.selection)
                }
            }
        } finally {
            isFormatting = false
        }
    }

    internal fun didInsertNewline(text: CharSequence?, start: Int, before: Int, count: Int): Boolean {
        if (text == null || count <= before || start < 0 || start + count > text.length) return false
        return text.subSequence(start, start + count).contains('\n')
    }

    internal fun formatAfterNewline(text: String, cursorPosition: Int): FormatResult? {
        if (cursorPosition <= 0 || cursorPosition > text.length || text[cursorPosition - 1] != '\n') {
            return null
        }

        val previousLineEnd = cursorPosition - 1
        val previousLineStart = text.lastIndexOf('\n', previousLineEnd - 1).let { index ->
            if (index == -1) 0 else index + 1
        }
        val previousLine = text.substring(previousLineStart, previousLineEnd)
        val previousLineContent = previousLine.trimStart()
        if (!previousLineContent.startsWith(BULLET_PREFIX.trimEnd())) return null

        return if (previousLine.trim() == BULLET_PREFIX.trimEnd()) {
            FormatResult.DeleteEmptyBullet(
                start = previousLineStart,
                end = previousLineEnd,
                selection = previousLineStart + 1,
            )
        } else {
            FormatResult.InsertBullet(
                index = cursorPosition,
                selection = cursorPosition + BULLET_PREFIX.length,
            )
        }
    }

    internal sealed interface FormatResult {
        data class InsertBullet(val index: Int, val selection: Int) : FormatResult
        data class DeleteEmptyBullet(val start: Int, val end: Int, val selection: Int) : FormatResult
    }

    private companion object {
        const val BULLET_PREFIX = "- "
    }
}
