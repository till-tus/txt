package com.example.textlauncher.domain

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusModeTest {
    private val zoneId = ZoneId.of("Europe/Berlin")

    @Test
    fun disabledFeature_neverResolvesAMode() {
        val mode = scheduledMode("work", days = setOf(1), start = 9 * 60, end = 17 * 60)

        val active = FocusModeResolver.resolve(
            focusModesEnabled = false,
            focusModes = listOf(mode),
            manuallyActiveFocusModeId = mode.id,
            schedulesPausedUntilEpochMillis = 0L,
            nowEpochMillis = epochMillis(2026, 8, 3, 10, 0),
            zoneId = zoneId,
        )

        assertNull(active)
    }

    @Test
    fun manualModeOverridesScheduledMode() {
        val scheduled = scheduledMode("scheduled", days = setOf(1), start = 9 * 60, end = 17 * 60)
        val manual = FocusMode("manual", "Manual")

        val active = FocusModeResolver.resolve(
            focusModesEnabled = true,
            focusModes = listOf(scheduled, manual),
            manuallyActiveFocusModeId = manual.id,
            schedulesPausedUntilEpochMillis = 0L,
            nowEpochMillis = epochMillis(2026, 8, 3, 10, 0),
            zoneId = zoneId,
        )

        assertEquals(manual.id, active?.mode?.id)
        assertEquals(FocusActivationSource.Manual, active?.source)
    }

    @Test
    fun overlappingSchedules_useFirstModeInList() {
        val first = scheduledMode("first", days = setOf(1), start = 9 * 60, end = 17 * 60)
        val second = scheduledMode("second", days = setOf(1), start = 8 * 60, end = 18 * 60)

        val active = FocusModeResolver.resolve(
            focusModesEnabled = true,
            focusModes = listOf(first, second),
            manuallyActiveFocusModeId = null,
            schedulesPausedUntilEpochMillis = 0L,
            nowEpochMillis = epochMillis(2026, 8, 3, 10, 0),
            zoneId = zoneId,
        )

        assertEquals(first.id, active?.mode?.id)
        assertEquals(FocusActivationSource.Schedule, active?.source)
    }

    @Test
    fun overnightSchedule_usesTheStartDaysSelection() {
        val fridayNight = scheduledMode("sleep", days = setOf(5), start = 22 * 60, end = 7 * 60)

        assertTrue(fridayNight.schedule.isActiveAt(epochMillis(2026, 8, 8, 2, 0), zoneId))
        assertEquals(
            false,
            fridayNight.schedule.isActiveAt(epochMillis(2026, 8, 9, 2, 0), zoneId),
        )
    }

    @Test
    fun pausedSchedules_doNotResolve() {
        val mode = scheduledMode("work", days = setOf(1), start = 9 * 60, end = 17 * 60)
        val now = epochMillis(2026, 8, 3, 10, 0)

        val active = FocusModeResolver.resolve(
            focusModesEnabled = true,
            focusModes = listOf(mode),
            manuallyActiveFocusModeId = null,
            schedulesPausedUntilEpochMillis = now + 60_000L,
            nowEpochMillis = now,
            zoneId = zoneId,
        )

        assertNull(active)
    }

    private fun scheduledMode(id: String, days: Set<Int>, start: Int, end: Int): FocusMode {
        return FocusMode(
            id = id,
            name = id,
            schedule = FocusSchedule(
                enabled = true,
                daysOfWeek = days,
                startMinuteOfDay = start,
                endMinuteOfDay = end,
            ),
        )
    }

    private fun epochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
