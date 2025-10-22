package com.getvisitapp.google_fit.healthConnect.model.internal

import androidx.annotation.Keep
import java.time.LocalDate

@Keep
data class MonthProperties(
    val firstDayOfMonth: LocalDate,
    val lastDayOfMonth: LocalDate,
    val numberOfDaysInMonth: Int
)