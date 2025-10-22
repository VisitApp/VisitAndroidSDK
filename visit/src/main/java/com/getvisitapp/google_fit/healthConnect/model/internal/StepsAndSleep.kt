package com.getvisitapp.google_fit.healthConnect.model.internal

import androidx.annotation.Keep

@Keep
data class StepsAndSleep(
    val steps: Long?, val sleepMetric: SleepMetric
)