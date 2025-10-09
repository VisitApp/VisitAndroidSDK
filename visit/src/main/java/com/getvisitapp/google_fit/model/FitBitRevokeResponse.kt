package com.getvisitapp.google_fit.model

import androidx.annotation.Keep

@Keep
data class FitBitRevokeResponse(val status: Int, val message: String, val errorMessage: String)