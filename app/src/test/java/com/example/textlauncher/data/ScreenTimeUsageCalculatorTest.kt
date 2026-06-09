package com.example.textlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTimeUsageCalculatorTest {
    @Test
    fun calculatePackageUsage_clampsSessionThatStartedBeforeWindow() {
        val usage = ScreenTimeUsageCalculator.calculatePackageUsage(
            events = listOf(
                foreground(APP, timestampMillis = 900L),
                background(APP, timestampMillis = 1_300L),
            ),
            startMillis = 1_000L,
            endMillis = 2_000L,
        )

        assertEquals(mapOf(APP to 300L), usage)
    }

    @Test
    fun calculatePackageUsage_closesActiveSessionAtWindowEnd() {
        val usage = ScreenTimeUsageCalculator.calculatePackageUsage(
            events = listOf(
                foreground(APP, timestampMillis = 1_200L),
            ),
            startMillis = 1_000L,
            endMillis = 2_000L,
        )

        assertEquals(mapOf(APP to 800L), usage)
    }

    @Test
    fun calculatePackageUsage_sumsMultipleSessionsByPackage() {
        val usage = ScreenTimeUsageCalculator.calculatePackageUsage(
            events = listOf(
                foreground(APP, timestampMillis = 1_100L),
                background(APP, timestampMillis = 1_300L),
                foreground(OTHER_APP, timestampMillis = 1_350L),
                background(OTHER_APP, timestampMillis = 1_450L),
                foreground(APP, timestampMillis = 1_500L),
                background(APP, timestampMillis = 1_750L),
            ),
            startMillis = 1_000L,
            endMillis = 2_000L,
        )

        assertEquals(mapOf(APP to 450L, OTHER_APP to 100L), usage)
    }

    @Test
    fun calculatePackageUsage_ignoresPackagesExcludedFromScreenTime() {
        val usage = ScreenTimeUsageCalculator.calculatePackageUsage(
            events = listOf(
                foreground(LAUNCHER_APP, timestampMillis = 1_100L),
                background(LAUNCHER_APP, timestampMillis = 1_900L),
                foreground(APP, timestampMillis = 1_200L),
                background(APP, timestampMillis = 1_500L),
            ),
            startMillis = 1_000L,
            endMillis = 2_000L,
            ignoredPackageNames = setOf(LAUNCHER_APP),
        )

        assertEquals(mapOf(APP to 300L), usage)
    }

    private fun foreground(packageName: String, timestampMillis: Long): ScreenTimeUsageEvent {
        return ScreenTimeUsageEvent(packageName, timestampMillis, ScreenTimeUsageEventType.Foreground)
    }

    private fun background(packageName: String, timestampMillis: Long): ScreenTimeUsageEvent {
        return ScreenTimeUsageEvent(packageName, timestampMillis, ScreenTimeUsageEventType.Background)
    }

    private companion object {
        const val APP = "com.example.app"
        const val LAUNCHER_APP = "com.example.textlauncher"
        const val OTHER_APP = "com.example.other"
    }
}
