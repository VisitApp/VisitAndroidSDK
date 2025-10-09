package com.getvisitapp.google_fit.model

import androidx.annotation.Keep

@Keep
data class ActivityData(
    val calories: Int,
    val hour: Int,
    val steps: Int
)