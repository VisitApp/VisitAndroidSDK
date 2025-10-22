package com.getvisitapp.google_fit.healthConnect.learningMaterial

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getvisitapp.google_fit.healthConnect.TimeUtil.convertToLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId

class Tutorials(val healthConnectClient: HealthConnectClient) {

    fun fetchData() {


        CoroutineScope(Dispatchers.IO).launch {

            val now = Instant.now()
            val zoneId = ZoneId.systemDefault()

//            Timber.d("Instant.now(): $now, zoneId: $zoneId")
//
            val startTime =
                LocalDateTime.of(LocalDate.parse("2024-08-25"), LocalTime.MIN)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()

            val endTime =
                LocalDateTime.of(LocalDate.parse("2024-08-25"), LocalTime.MAX)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()


            Timber.d("startTime: $startTime, endTime: $endTime")


            //Common function to explore how Health Connect works.

//            readStepsByTimeRange(
//                healthConnectClient = healthConnectClient,
//                startTime = startTime,
//                endTime = endTime,
//            )

            aggregateSteps(
                healthConnectClient = healthConnectClient,
                startTime = startTime,
                endTime = endTime
            )

//            readExerciseSessionByTimeRange(
//                healthConnectClient = healthConnectClient,
//                startTime = startTime,
//                endTime = endTime
//            )

//            aggregateActivityByBucketBasedOnDuration(
//                healthConnectClient = healthConnectClient,
//                startTime = startTime,
//                endTime = endTime
//            )

//            aggregateActivityByBucketBasedOnPeriod(
//                healthConnectClient = healthConnectClient,
//                startTime = startTime,
//                endTime = endTime
//            )


        }
    }

    suspend fun readStepsByTimeRange(
        healthConnectClient: HealthConnectClient, startTime: Instant, endTime: Instant
    ) {

        //this returns all the records individually. Same as the one shown in the Health Connect App inside Step section.

        try {

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            Timber.d("record: ${response.records}")


            for (stepRecord in response.records) {
                val count = stepRecord.count
                val recordStartTime: Instant = stepRecord.startTime
                val recordEndTime = stepRecord.endTime
                val startZoneOffset = stepRecord.startZoneOffset
                val endZoneOffset = stepRecord.endZoneOffset

                val metaData = stepRecord.metadata
                val metaDataPackageName = metaData.dataOrigin.packageName
                val metaDataId = metaData.id
                val metaDataClientRecordId = metaData.clientRecordId


                Timber.d(
                    "stepRecord: count: $count ," + "startTime: $recordStartTime (${recordStartTime.toEpochMilli()}) ," + "endTime: $recordEndTime (${recordEndTime.toEpochMilli()})," + "startTime (Local): ${recordStartTime.convertToLocalDateTime()}, " + "endTime (Local): ${recordEndTime.convertToLocalDateTime()}, " + "startZoneOffset: $startZoneOffset ," + "endZoneOffset: $endZoneOffset ," + "metadata: (dataOrigin: ${metaDataPackageName}, id: ${metaDataId}, clientRecordId: ${metaDataClientRecordId})"
                )


                // Process each step record
            }
        } catch (e: Exception) {
            Timber.d("error: ${e.message}")
            // Run error handling here.
        }
    }


    private suspend fun aggregateSteps(
        healthConnectClient: HealthConnectClient, startTime: Instant, endTime: Instant
    ) {
        try {

            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                        SleepSessionRecord.SLEEP_DURATION_TOTAL
                    ), timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            // The result may be null if no data is available in the time range
            val stepCount = response[StepsRecord.COUNT_TOTAL]
            val distanceTotal = response[DistanceRecord.DISTANCE_TOTAL]?.inKilometers
            val totalCalorie = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
            val activeCalorie = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
            val sleep = response[SleepSessionRecord.SLEEP_DURATION_TOTAL]
            val activityTime = response[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]


            Timber.d("stepCount: $stepCount, " + "distanceTotal:$distanceTotal, " + "activeCalorie: $activeCalorie, " + "totalCalorie: $totalCalorie, " + "sleep: ${sleep?.toMinutes()}," + "activityTime: $activityTime " + ",dataOrigins: ${
                response.dataOrigins.joinToString(separator = ",") { dataOrigin ->
                    dataOrigin.packageName
                }
            }")


        } catch (e: Exception) {
            // Run error handling here
        }
    }

    suspend fun readExerciseSessionByTimeRange(
        healthConnectClient: HealthConnectClient, startTime: Instant, endTime: Instant
    ) {

        //this returns all the records individually. Same as the one shown in the Health Connect App inside Step section.

        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

//            Timber.d("record: ${response.records}")

            for (exerciseRecord in response.records) {


                val startTime = exerciseRecord.startTime
                val endTime = exerciseRecord.endTime
                val exerciseType = exerciseRecord.exerciseType
                val segments = exerciseRecord.segments
                val title = exerciseRecord.title
                val metaData = exerciseRecord.metadata
                val metaDataPackageName = metaData.dataOrigin.packageName
                val metaDataId = metaData.id
                val metaDataClientRecordId = metaData.clientRecordId
                val startZoneOffset = exerciseRecord.startZoneOffset
                val endZoneOffset = exerciseRecord.endZoneOffset


                Timber.d(
                    "readExerciseSessionByTimeRange: Start Time: $startTime," + " End Time: $endTime," + " Exercise Type: $exerciseType," + " Segments: $segments, " + "Title: $title," + " metaDataPackageName: $metaDataPackageName," + " metaDataId: $metaDataId," + " metaDataClientRecordId: $metaDataClientRecordId," + " Start Zone Offset: $startZoneOffset," + " End Zone Offset: $endZoneOffset"
                )


                // Process each step record
            }
        } catch (e: Exception) {
            Timber.d("error: ${e.message}")
            // Run error handling here.
        }
    }

    private suspend fun aggregateActivityByBucketBasedOnDuration(
        healthConnectClient: HealthConnectClient, startTime: Instant, endTime: Instant
    ) {
        try {

            val response = healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                    timeRangeSlicer = Duration.ofDays(1)
                )

            )

            response.forEach { result: AggregationResultGroupedByDuration ->
                Timber.d(
                    "steps total: ${result.result[StepsRecord.COUNT_TOTAL]} " + ",distance total: ${result.result[DistanceRecord.DISTANCE_TOTAL]} ," + "calorie total: ${result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]} ," + "startTime:${result.startTime} ," + "endTime: ${result.endTime}"
                )
            }


        } catch (e: Exception) {
            // Run error handling here
        }
    }

    private suspend fun aggregateActivityByBucketBasedOnPeriod(
        healthConnectClient: HealthConnectClient, startTime: Instant, endTime: Instant
    ) {
        try {

            val response = healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                    timeRangeSlicer = Period.ofDays(1)
                )

            )

            response.forEach { result: AggregationResultGroupedByPeriod ->
                Timber.d(
                    "steps total: ${result.result[StepsRecord.COUNT_TOTAL]} " + ",distance total: ${result.result[DistanceRecord.DISTANCE_TOTAL]} ," + "calorie total: ${result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]} ," + "startTime: ${result.startTime} ," + "endTime: ${result.endTime}"
                )
            }


        } catch (e: Exception) {
            // Run error handling here
        }
    }
}