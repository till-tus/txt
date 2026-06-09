package com.example.textlauncher.data

data class ScreenTimeUsageEvent(
    val packageName: String,
    val timestampMillis: Long,
    val type: ScreenTimeUsageEventType,
)

enum class ScreenTimeUsageEventType {
    Foreground,
    Background,
}

object ScreenTimeUsageCalculator {
    fun calculatePackageUsage(
        events: List<ScreenTimeUsageEvent>,
        startMillis: Long,
        endMillis: Long,
    ): Map<String, Long> {
        if (endMillis <= startMillis) return emptyMap()

        val activeSinceByPackage = mutableMapOf<String, Long>()
        val usageByPackage = mutableMapOf<String, Long>()

        events.sortedBy { it.timestampMillis }.forEach { event ->
            when (event.type) {
                ScreenTimeUsageEventType.Foreground -> {
                    activeSinceByPackage.putIfAbsent(event.packageName, event.timestampMillis)
                }
                ScreenTimeUsageEventType.Background -> {
                    val activeSince = activeSinceByPackage.remove(event.packageName) ?: return@forEach
                    addClampedUsage(
                        usageByPackage = usageByPackage,
                        packageName = event.packageName,
                        activeSince = activeSince,
                        inactiveAt = event.timestampMillis,
                        startMillis = startMillis,
                        endMillis = endMillis,
                    )
                }
            }
        }

        activeSinceByPackage.forEach { (packageName, activeSince) ->
            addClampedUsage(
                usageByPackage = usageByPackage,
                packageName = packageName,
                activeSince = activeSince,
                inactiveAt = endMillis,
                startMillis = startMillis,
                endMillis = endMillis,
            )
        }

        return usageByPackage.filterValues { it > 0 }
    }

    private fun addClampedUsage(
        usageByPackage: MutableMap<String, Long>,
        packageName: String,
        activeSince: Long,
        inactiveAt: Long,
        startMillis: Long,
        endMillis: Long,
    ) {
        val clampedStart = maxOf(activeSince, startMillis)
        val clampedEnd = minOf(inactiveAt, endMillis)
        if (clampedEnd > clampedStart) {
            usageByPackage[packageName] = usageByPackage.getOrDefault(packageName, 0L) + clampedEnd - clampedStart
        }
    }
}
