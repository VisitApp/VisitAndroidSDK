package com.getvisitapp.google_fit.healthConnect.helper

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

class ActivityTimeHelper {

    //It uses Steps to calculate activity time.
    suspend fun getTotalActivityTimeForDay(
        healthConnectClient: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ): Duration {
        var finalDuration = Duration.ofSeconds(0L, 0L)

        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )


            for (stepRecord in response.records) {
                val count = stepRecord.count
                val recordStartTime = stepRecord.startTime
                val recordEndTime = stepRecord.endTime

                val startZoneOffset = stepRecord.startZoneOffset
                val endZoneOffset = stepRecord.endZoneOffset


                val result = Duration.between(recordStartTime, recordEndTime)


                finalDuration = finalDuration.plus(result)


//                Timber.d(
//                    "stepRecord: count: $count ," + "startTime: $recordStartTime (${recordStartTime.toEpochMilli()}) ," + "endTime $recordEndTime (${recordEndTime.toEpochMilli()}), finalDuration: ${finalDuration.seconds} "
//                )


//                Timber.d("count: $count, finalDuration: ${finalDuration.seconds}")


                // Process each step record
            }
        } catch (e: Exception) {
//            Timber.d("error: ${e.message}")
            // Run error handling here.
        }

        return finalDuration

    }
}