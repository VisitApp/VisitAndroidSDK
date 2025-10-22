package com.getvisitapp.google_fit.healthConnect.model.apiRequestModel

import androidx.annotation.Keep
import java.time.LocalDateTime

//st:steps
//c:calorie
//d:distance
//h:hour of the day
//s:package name of the app which contributed record. Ex: `com.healthconnectexample`

@Keep
data class HourlyRecord(
    var st: Long = 0, //steps
    var c: Int = 0, //calorie
    var d: Int = 0, //distance
    val h: Long, //hour of the day
    var s: String? = null, //source

    @Transient
    val dateTime: LocalDateTime
)