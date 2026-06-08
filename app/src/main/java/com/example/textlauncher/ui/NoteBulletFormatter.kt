package com.example.textlauncher.ui

import android.text.Editable
import android.widget.EditText

internal class NoteBulletFormatter {
    private var isFormatting = false

    fun formatAfterNewline(input: EditText, text: Editable) {
        if (isFormatting) return

        val cursorPosition = input.selectionStart
        if (cursorPosition <= 0 || cursorPosition > text.length || text[cursorPosition - 1] != '\n') {
            return
        }

        val previousLineEnd = cursorPosition - 1
        val previousLineStart = text.lastIndexOf('\n', previousLineEnd - 1).let { index ->
            if (index == -1) 0 else index + 1
        }
        val previousLine = text.subSequence(previousLineStart, previousLineEnd).toString()
        val previousLineContent = previousLine.trimStart()
        if (!previousLineContent.startsWith(BULLET_PREFIX.trimEnd())) return

        isFormatting = true
        try {
            if (previousLine.trim() == BULLET_PREFIX.trimEnd()) {
                text.delete(previousLineStart, previousLineEnd)
                input.setSelection(previousLineStart + 1)
            } else {
                text.insert(cursorPosition, BULLET_PREFIX)
                input.setSelection(cursorPosition + BULLET_PREFIX.length)
            }
        } finally {
            isFormatting = false
        }
    }

    private companion object {
        const val BULLET_PREFIX = "- "
    }
}
