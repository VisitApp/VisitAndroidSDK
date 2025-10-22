package com.getvisitapp.google_fit.healthConnect.model.apiRequestModel

import androidx.annotation.Keep
import java.time.LocalDateTime

@Keep
data class DailySyncHealthMetric(
    var steps: Long = 0,
    var calorie: Long = 0,
    var distance: Long = 0,
    var activity: Long = 0,
    var sleep: String? = null, //default value is null because we don't want to send sleep data if sleep data is not present. Not even 0
    var date: Long? = null,

    @Transient
    val dateTime: LocalDateTime
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DailySyncHealthMetric) return false

        return dateTime == other.dateTime
    }

    override fun hashCode(): Int {
        return 31 * dateTime.hashCode()
    }
}