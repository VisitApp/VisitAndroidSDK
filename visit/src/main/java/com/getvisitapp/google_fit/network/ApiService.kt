package com.getvisitapp.google_fit.network

import androidx.annotation.Keep
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.DailyStepSyncRequest
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.HourlyDataSyncRequest
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.SyncResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

@Keep
interface ApiService {

    @POST("users/data-sync")
    suspend fun uploadDailyHealthData(
        @Query("isPWA") isPWA: String = "yes",
        @Body requestBody: DailyStepSyncRequest
    ): SyncResponse?

    @POST(" users/embellish-sync")
    suspend fun uploadHourlyHealthData(
        @Query("isPWA") isPWA: String = "yes",
        @Body requestBody: HourlyDataSyncRequest
    ): SyncResponse?

}