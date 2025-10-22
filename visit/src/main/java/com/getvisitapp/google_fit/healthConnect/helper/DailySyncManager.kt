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
import com.getvisitapp.google_fit.healthConnect.TimeUtil.convertLocalDateTimeToEpochMillis
import com.getvisitapp.google_fit.healthConnect.TimeUtil.convertToLocalDateTime
import com.getvisitapp.google_fit.healthConnect.data.SleepHelper
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.DailySyncHealthMetric
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit


class DailySyncManager(private val healthConnectClient: HealthConnectClient) {

    private val activityTimeHelper = ActivityTimeHelper()
    private val sleepHelper = SleepHelper(healthConnectClient)


    suspend fun getDailySyncData(dailyLastSyncTimeStamp: Long): List<DailySyncHealthMetric> {

        //Case 1: If the timestamp is 0, then take last 30day timestamp and sync it from there.
        //Case 2: If the timestamp is older then 30 days, then only sync the data for last 30 days.
        //Case 3: else sync from the dailyLastSyncTimeStamp


        //Case 1:
        val normalizedDateTime: LocalDateTime = if (dailyLastSyncTimeStamp == 0L) {
            LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusDays(30)
        } else {
            Instant.ofEpochMilli(dailyLastSyncTimeStamp).convertToLocalDateTime().withHour(0)
                .withMinute(0).withSecond(0).withNano(0)
        }


        var startDate: Instant =
            LocalDateTime.of(normalizedDateTime.toLocalDate().minusDays(1), LocalTime.MIN)
                .atZone(ZoneId.systemDefault()).toInstant()


        val endDate: Instant =
            LocalDateTime.of(LocalDate.now(), LocalTime.MAX).atZone(ZoneId.systemDefault())
                .toInstant()

        var daysInBetween = ChronoUnit.DAYS.between(startDate, endDate)
            .toInt() + 1 // "+1" because current days is not included

        Timber.d("startDate: $startDate, endDate: $endDate, normalizedDateTime: $normalizedDateTime, daysBetween: $daysInBetween")


        //Case 2:
        if (daysInBetween > 30) {
            startDate = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusDays(30)
                .atZone(ZoneId.systemDefault()).toInstant()

            daysInBetween = ChronoUnit.DAYS.between(startDate, endDate)
                .toInt() + 1 // "+1" because current days is not included
        }


        Timber.d("startTime: $startDate, endTime: $endDate, normalizedDateTime: $normalizedDateTime, daysBetween: $daysInBetween")


        val dailyHealthMetric: List<DailySyncHealthMetric> = aggregateHealthMetricBasedOnDuration(
            startDateInstant = startDate,
            endDateInstant = endDate,
            daysInBetween = daysInBetween,
            startDateTime = startDate.convertToLocalDateTime(),
            endDateTime = endDate.convertToLocalDateTime()
        )

        return dailyHealthMetric


    }

    private suspend fun aggregateHealthMetricBasedOnDuration(
        startDateInstant: Instant,
        endDateInstant: Instant,
        daysInBetween: Int,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): List<DailySyncHealthMetric> {

        val finalHealthMetricDataList = mutableListOf<DailySyncHealthMetric>()

        Timber.d("startDateTime: $startDateTime, endDateTime : $endDateTime")

        for (i in 0..<daysInBetween.toLong()) {
            finalHealthMetricDataList.add(
                DailySyncHealthMetric(
                    dateTime = startDateTime.plusDays(i),
                    date = startDateTime.plusDays(i).convertLocalDateTimeToEpochMillis()
                )
            )
        }

        Timber.d("finalHealthMetricDataList: $finalHealthMetricDataList")

        val response = healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    DistanceRecord.DISTANCE_TOTAL,
                    ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL
                ),
                timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant),
                timeRangeSlicer = Duration.ofDays(1)
            )
        )

        response.forEach { result: AggregationResultGroupedByDuration ->


            val bucketStartDateTime: LocalDateTime =
                LocalDateTime.ofInstant(result.startTime, ZoneId.systemDefault())

            val bucketEndDateTime = LocalDateTime.ofInstant(result.endTime, ZoneId.systemDefault())


            val sleepMetric = sleepHelper.getDailySleepData(bucketStartDateTime.toLocalDate())

            val isPresent =
                finalHealthMetricDataList.contains(DailySyncHealthMetric(dateTime = bucketStartDateTime))

            if (isPresent) {

                val index = finalHealthMetricDataList.indexOf(
                    DailySyncHealthMetric(
                        dateTime = bucketStartDateTime
                    )
                )

                finalHealthMetricDataList[index].steps = result.result[StepsRecord.COUNT_TOTAL] ?: 0

                finalHealthMetricDataList[index].calorie =
                    result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toLong()
                        ?: 0L

                finalHealthMetricDataList[index].distance =
                    result.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters?.toLong() ?: 0

                finalHealthMetricDataList[index].activity =
                    activityTimeHelper.getTotalActivityTimeForDay(
                        healthConnectClient,
                        result.startTime,
                        result.endTime
                    ).toSeconds()

                finalHealthMetricDataList[index].sleep =
                    "${sleepMetric.sleepStartTimeMillis}-${sleepMetric.sleepEndTimeMillis}"

            }

            Timber.d(
                "bucketStartDateTime: $bucketStartDateTime ," + "bucketEndDateTime: $bucketEndDateTime, " + "steps total: ${result.result[StepsRecord.COUNT_TOTAL]} " + "distance total: ${result.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters} " + "exercise total: ${result.result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()} " + "calorie total: ${result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories} " + "sleep total: ${result.result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()} " + "startTime:${result.startTime} ," + "endTime: ${result.endTime}," + "sleepMetric: $sleepMetric"
            )

        }


        return finalHealthMetricDataList
    }
}