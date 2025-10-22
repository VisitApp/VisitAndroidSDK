package com.getvisitapp.google_fit.healthConnect.model.internal

import androidx.annotation.Keep
import java.time.LocalDateTime


//This model class is only for pushing graph data to the webView.
@Keep
data class SleepModel(
    @Transient val sleepDateTime: LocalDateTime,
    val day: String,
    var sleepTime: Long,
    var startTimestamp: Long,
    var wakeupTime: Long
)