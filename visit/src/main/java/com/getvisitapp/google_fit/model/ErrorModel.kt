package com.getvisitapp.google_fit.model

import androidx.annotation.Keep

@Keep
data class ErrorModel(
    val errorMessage: String?,
    val message: String?,
    val status: Int
)