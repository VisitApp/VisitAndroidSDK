package com.getvisitapp.google_fit.model

import androidx.annotation.Keep

@Keep
data class TataAIGFitnessPayload(
    val `data`: List<Data>,
    val member_id: String
)