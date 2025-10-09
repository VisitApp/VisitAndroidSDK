package com.getvisitapp.google_fit.model

import androidx.annotation.Keep

@Keep
data class Data(
    val activity_data: List<ActivityData>,
    val activity_date: String
)