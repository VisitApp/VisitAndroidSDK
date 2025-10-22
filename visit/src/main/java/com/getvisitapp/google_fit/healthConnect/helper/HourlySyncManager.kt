package com.getvisitapp.google_fit.healthConnect.helper

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getvisitapp.google_fit.healthConnect.TimeUtil.convertInstantToEpochMillis
import com.getvisitapp.google_fit.healthConnect.TimeUtil.convertToLocalDateTime
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.BulkHealthData
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.HourlyRecord
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HourlySyncManager(private val healthConnectClient: HealthConnectClient) {

    suspend fun getHourlySyncData(hourlyLastSyncTimestamp: Long): List<BulkHealthData> {

        //Case 1: If the timestamp is 0, then take last 30day timestamp and sync it from there.
        //Case 2: If the timestamp is older then 30 days, then only sync the data for last 30 days.
        //Case 3: else sync from the hourlyLastSyncTimestamp

        //Case 1:
        val normalizedStartDateTime: LocalDateTime = if (hourlyLastSyncTimestamp == 0L) {
            LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusDays(30)
        } else {
            LocalDateTime.of(
                Instant.ofEpochMilli(hourlyLastSyncTimestamp).convertToLocalDateTime()
                    .toLocalDate(),
                LocalTime.MIN
            )
        }

        val normalizedEndDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)


        var startDate = normalizedStartDateTime
        val endDate = normalizedEndDateTime

        var daysInBetween = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

        //Case 2:
        if (daysInBetween > 30) {
            startDate = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusDays(30)

            daysInBetween = ChronoUnit.DAYS.between(startDate, endDate)
                .toInt() + 1 // "+1" because current days is not included
        }

        Timber.d("startTime: $startDate, endTime: $endDate, normalizedDateTime: $normalizedStartDateTime, daysBetween: $daysInBetween")

        //Test

        val bulkHealthDataForEachDate: List<BulkHealthData> = (0..<daysInBetween).map { day ->
            val bulkHealthData =
                getHourlyDataForSingleDay(startDate.toLocalDate().plusDays(day.toLong()))

            bulkHealthData
        }



        return bulkHealthDataForEachDate

    }

    private suspend fun getHourlyDataForSingleDay(
        date: LocalDate
    ): BulkHealthData {


        val startOfTheDay = LocalDateTime.of(date, LocalTime.MIN)

        val startDateInstant: Instant =
            LocalDateTime.of(date, LocalTime.MIN).atZone(ZoneId.systemDefault()).toInstant()


        val endDateInstant: Instant =
            LocalDateTime.of(date, LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()


        //creating list with empty data points.
        val hourlyRecord: List<HourlyRecord> = (0..23L).map { hour ->
            HourlyRecord(h = hour, dateTime = startOfTheDay.plusHours(hour))
        }


        Timber.d("startDateInstant: $startDateInstant, endDateInstant: $endDateInstant, hourlyRecord: $hourlyRecord \n \n ")

        val response = healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    DistanceRecord.DISTANCE_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL
                ),
                timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant),
                timeRangeSlicer = Duration.ofHours(1)
            )
        )



        response.forEach { result: AggregationResultGroupedByDuration ->


            val bucketStartDateTime: LocalDateTime =
                LocalDateTime.ofInstant(result.startTime, ZoneId.systemDefault())

            val bucketEndDateTime = LocalDateTime.ofInstant(result.endTime, ZoneId.systemDefault())

            val logMessage = StringBuilder()
            logMessage.append("bucketStartDateTime: $bucketStartDateTime | ")
            logMessage.append("bucketEndDateTime: $bucketEndDateTime | ")
            logMessage.append("steps total: ${result.result[StepsRecord.COUNT_TOTAL]} | ")
            logMessage.append("distance total: ${result.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters} | ")
            logMessage.append("exercise total: ${result.result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()} | ")
            logMessage.append("calorie total: ${result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories} | ")
            logMessage.append("sleep total: ${result.result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()} | ")
            logMessage.append("startTime: ${result.startTime} | ")
            logMessage.append("endTime: ${result.endTime}")


//            Timber.d(logMessage.toString())

            //Find the hour record from the array which matches with the bucket timestamp
            //The returned list can be spared.
            val record = hourlyRecord.find { it.dateTime == bucketStartDateTime }

            if (record != null) {

                record.st = result.result[StepsRecord.COUNT_TOTAL] ?: 0
                record.d = result.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters?.toInt() ?: 0
                record.c =
                    result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toInt()
                        ?: 0
                record.s = result.result.dataOrigins?.joinToString(",") { it.packageName }
            }


        }

        val totalStepsOfTheDay = hourlyRecord.sumOf { it.st }
        val totalDistanceOfTheDay = hourlyRecord.sumOf { it.d }
        val totalCalorieOfTheDay = hourlyRecord.sumOf { it.c }


        val bulkHealthData = BulkHealthData(
            hourlyRecord = hourlyRecord, dt = startDateInstant.convertInstantToEpochMillis()
        )

        Timber.d("hourlyRecord: $hourlyRecord, totalStepsOfTheDay: $totalStepsOfTheDay, totalDistanceOfTheDay: $totalDistanceOfTheDay, totalCalorieOfTheDay: $totalCalorieOfTheDay \n \n")

        return bulkHealthData
    }
}