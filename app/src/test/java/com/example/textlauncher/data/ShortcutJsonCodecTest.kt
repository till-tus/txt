package com.example.textlauncher.data

import com.example.textlauncher.domain.AppShortcut
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutJsonCodecTest {
    @Test
    fun decodeOrEmpty_malformedOrIncompleteDataDoesNotCrash() {
        assertEquals(emptyList<AppShortcut>(), ShortcutJsonCodec.decodeOrEmpty("not-json"))
        assertEquals(emptyList<AppShortcut>(), ShortcutJsonCodec.decodeOrEmpty("""[{"label":"Missing fields"}]"""))
    }

    @Test
    fun encodeDecode_roundTripsShortcuts() {
        val shortcuts = listOf(AppShortcut("Maps", "maps.package", "MapsActivity"))

        assertEquals(shortcuts, ShortcutJsonCodec.decodeOrEmpty(ShortcutJsonCodec.encode(shortcuts)))
    }
}
