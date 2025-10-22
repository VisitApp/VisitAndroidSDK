package com.getvisitapp.google_fit.healthConnect.model.internal

import androidx.annotation.Keep
import java.time.Duration

@Keep
data class SleepMetric(
    val sleepDuration: Duration,
    val formattedSleepDuration: String,
    val sleepStartTimeMillis: Long,
    val sleepEndTimeMillis: Long,
    val sleepDurationInMillis: Long,
    val sleepDateInMillis: Long,
) {
    override fun toString(): String {
        return "SleepMetric( sleepDuration: ${sleepDuration.toMinutes()}, " +
                "formattedSleepDuration: $formattedSleepDuration, " +
                "sleepStartTimeMillis: $sleepStartTimeMillis, " +
                "sleepEndTimeMillis: $sleepEndTimeMillis, " +
                "sleepDurationInMillis: $sleepDurationInMillis," +
                "sleepDateInMillis: $sleepDateInMillis )"

    }
}