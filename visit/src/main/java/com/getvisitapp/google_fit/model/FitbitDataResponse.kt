package com.getvisitapp.google_fit.model

import androidx.annotation.Keep

@Keep
data class FitbitDataResponse(
    val `data`: List<Data>,
    val status: Boolean,
    val message:String,
    val errorMessage:String?,
)

