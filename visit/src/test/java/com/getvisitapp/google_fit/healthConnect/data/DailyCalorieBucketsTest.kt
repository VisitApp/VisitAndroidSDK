package com.getvisitapp.google_fit.healthConnect.data

import com.getvisitapp.google_fit.healthConnect.model.internal.HealthMetricsWithDateTime
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyCalorieBucketsTest {
    private val start = LocalDate.of(2026, 11, 11).atStartOfDay()

    @Test
    fun truncatesEachDisplayedHourBeforeSumming() {
        val hours = listOf(
            HealthMetricsWithDateTime(calorie = 10.9, dateTime = start),
            HealthMetricsWithDateTime(calorie = 10.9, dateTime = start.plusHours(1)),
            HealthMetricsWithDateTime(dateTime = start.plusHours(2)),
        )

        assertEquals(20L, DailyCalorieBuckets(hours, hasReadableCalories = true).displayedTotal())
    }

    @Test
    fun distinguishesAReadableZeroFromMissingRecords() {
        val hours = listOf(HealthMetricsWithDateTime(calorie = 0.0, dateTime = start))

        assertEquals(0L, DailyCalorieBuckets(hours, hasReadableCalories = true).displayedTotal())
        assertNull(DailyCalorieBuckets(hours, hasReadableCalories = false).displayedTotal())
    }
}
