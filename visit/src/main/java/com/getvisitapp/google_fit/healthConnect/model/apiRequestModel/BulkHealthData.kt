package com.getvisitapp.google_fit.healthConnect.model.apiRequestModel

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

//dt is the data in epoch time. Ex: 1724869800519

@Keep
data class BulkHealthData(
    @SerializedName("data")
    val hourlyRecord: List<HourlyRecord>,
    val dt: Long
)