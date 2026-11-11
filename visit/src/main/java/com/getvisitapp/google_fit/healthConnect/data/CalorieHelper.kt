package com.getvisitapp.google_fit.healthConnect.data

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getvisitapp.google_fit.healthConnect.TimeUtil
import com.getvisitapp.google_fit.healthConnect.helper.ActivityTimeHelper
import com.getvisitapp.google_fit.healthConnect.model.internal.HealthMetricData
import com.getvisitapp.google_fit.healthConnect.model.internal.HealthMetricsWithDateTime
import com.getvisitapp.google_fit.healthConnect.model.internal.MonthProperties
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

internal fun HealthMetricsWithDateTime.displayedCalorieKcal(): Int = calorie?.toInt() ?: 0

internal data class DailyCalorieBuckets(
    val hours: List<HealthMetricsWithDateTime>,
    val hasReadableCalories: Boolean,
) {
    fun displayedTotal(): Long? =
        if (hasReadableCalories) hours.sumOf { it.displayedCalorieKcal().toLong() } else null
}

class CalorieHelper(private val healthConnectClient: HealthConnectClient) {
    val activityTimeHelper = ActivityTimeHelper()

    suspend fun getDailyCalorieData(selectedDate: LocalDate): HealthMetricData {
        val (start, end) = TimeUtil.getSelectedDateStartTimeAndEndTimeInstant(selectedDate)
        val buckets = readDailyCalorieBuckets(start, end)
        val aggregatedData = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )

        return HealthMetricData().apply {
            healthMetricWithDateTime = buckets.hours
            totalCalorie = aggregatedData[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
            totalActivityTime = activityTimeHelper.getTotalActivityTimeForDay(
                healthConnectClient,
                startTime = start,
                endTime = end,
            )
        }
    }

    internal suspend fun getDailyDisplayedCalorieTotal(selectedDate: LocalDate): Long? {
        val (start, end) = TimeUtil.getSelectedDateStartTimeAndEndTimeInstant(selectedDate)
        return readDailyCalorieBuckets(start, end).displayedTotal()
    }

    private suspend fun readDailyCalorieBuckets(start: Instant, end: Instant): DailyCalorieBuckets {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDateTime.ofInstant(start, zone).toLocalDate().atStartOfDay()
        val hours = List(24) { hour ->
            HealthMetricsWithDateTime(dateTime = startOfDay.plusHours(hour.toLong()))
        }
        val response = healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
                timeRangeSlicer = Duration.ofHours(1)
            )
        )

        var hasReadableCalories = false
        // Preserve the graph's cutoff for hours that have yet to finish.
        val cutoff = LocalDateTime.now().minusHours(1)
        for (result in response) {
            val calories = result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
            if (calories != null) hasReadableCalories = true

            val bucketTime = LocalDateTime.ofInstant(result.startTime, zone)
            val index = hours.indexOf(HealthMetricsWithDateTime(dateTime = bucketTime))
            if (index >= 0) {
                hours[index].calorie = if (bucketTime.isBefore(cutoff)) calories else 0.0
            }
        }

        return DailyCalorieBuckets(hours, hasReadableCalories)
    }

    suspend fun getWeeklyCalorieData(
        selectedDate: LocalDate
    ): HealthMetricData {
        val currentWeekFirstDate = TimeUtil.getFirstDayOfWeek(selectedDate)
        val weekInstance: Pair<Instant, Instant> = TimeUtil.getWeekInstant(currentWeekFirstDate)

        val healthMetricData = aggregateCalorieForEachDayOfWeekBasedOnDuration(
            startDateInstant = weekInstance.first,
            endDateInstant = weekInstance.second,
            firstDayOfWeek = currentWeekFirstDate,
            currentDayOfWeek = LocalDateTime.of(selectedDate, LocalTime.MIDNIGHT)
        )
        return healthMetricData
    }


    private suspend fun aggregateCalorieForEachDayOfWeekBasedOnDuration(
        startDateInstant: Instant,
        endDateInstant: Instant,
        firstDayOfWeek: LocalDateTime,
        currentDayOfWeek: LocalDateTime
    ): HealthMetricData {

        val finalHealthMetricData = HealthMetricData()

        val startTime =
            LocalDateTime.ofInstant(startDateInstant, ZoneId.systemDefault()).withHour(0)
                .withMinute(0).withSecond(0).withNano(0)

        val healthMetricsWithDateTimeList = mutableListOf<HealthMetricsWithDateTime>()


        // Loop to add each day of the week
        for (i in 0..6) {
            healthMetricsWithDateTimeList.add(
                HealthMetricsWithDateTime(
                    dateTime = startTime.plusDays(i.toLong())
                )
            )
        }


        Timber.d("healthMetricsWithDateTimeList: $healthMetricsWithDateTimeList")


        val response = healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                ),
                timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant),
                timeRangeSlicer = Duration.ofDays(1)
            )

        )

        response.forEach { result: AggregationResultGroupedByDuration ->


            val bucketStartDateTime: LocalDateTime =
                LocalDateTime.ofInstant(result.startTime, ZoneId.systemDefault())

            val bucketEndDateTime =
                LocalDateTime.ofInstant(result.endTime, ZoneId.systemDefault())

            Timber.d(
                "calorie total: ${result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories}, bucketStartDateTime: $bucketStartDateTime ," + "bucketEndDateTime: $bucketEndDateTime, startTime :$ { result.startTime } , " + "endTime: ${result.endTime}"
            )

            // Check if bucketStartDateTime is in the list
            val isPresent = healthMetricsWithDateTimeList.contains(
                HealthMetricsWithDateTime(dateTime = bucketStartDateTime)
            )

            if (isPresent) {
                val index = healthMetricsWithDateTimeList.indexOf(
                    HealthMetricsWithDateTime(
                        dateTime = bucketStartDateTime
                    )
                )


                //NOTE: Health Connect give the future calorie count by dividing the total count equally.
                //We need to prevent this while showing user the data.
                //Simply makes the calorie as 0, if the time has not been passed yet.

                healthMetricsWithDateTimeList[index].calorie =
                    if (healthMetricsWithDateTimeList[index].dateTime.isBefore(
                            LocalDateTime.now().minusHours(1)
                        )
                    ) {
                        result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
                    } else {
                        0.0
                    }
            }


        }

        finalHealthMetricData.healthMetricWithDateTime = healthMetricsWithDateTimeList


        //Aggregating Total calorie
        val aggregatedData = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                ), timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant)
            )
        )

        //Total calorie
        val totalCalorie: Double? =
            aggregatedData[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
        finalHealthMetricData.totalCalorie = totalCalorie

        //Total Activity Duration
        val duration = activityTimeHelper.getTotalActivityTimeForDay(
            healthConnectClient,
            startTime = startDateInstant,
            endTime = endDateInstant,
        )
        finalHealthMetricData.totalActivityTime = duration


        //Finding the different of the days between start of week and current day of week.
        // 0<<differentBetweenCurrentDayAndFirstDay<<6
        val differentBetweenCurrentDayAndFirstDay: Long =
            Duration.between(firstDayOfWeek, currentDayOfWeek).toDays()

        Timber.d("differentBetweenCurrentDayAndFirstDay: ${differentBetweenCurrentDayAndFirstDay}")


        //Average calorie
        if (totalCalorie != null) {
            if (differentBetweenCurrentDayAndFirstDay == 0L) {
                finalHealthMetricData.averageCalorie = totalCalorie
            } else {
                finalHealthMetricData.averageCalorie =
                    totalCalorie / differentBetweenCurrentDayAndFirstDay
            }
        }


        //Average Duration
        if (differentBetweenCurrentDayAndFirstDay == 0L) {
            finalHealthMetricData.averageActivityTime = duration
        } else {
            finalHealthMetricData.averageActivityTime = Duration.ofSeconds(
                duration.toSeconds() / differentBetweenCurrentDayAndFirstDay
            )
        }

        return finalHealthMetricData;
    }


    /**
     * 3. Get distance data of a particular month.
     * @param selectedDate: any date of a month.
     * @return calorie from start date to end date of the month.
     * //Note: Health Connect only stores the data of last 30 days.
     */

    //3. Get calorie Data of a particular month

    suspend fun getMonthlyCalorieData(
        selectedDate: LocalDate,
    ): HealthMetricData {
        val monthProperties: MonthProperties =
            TimeUtil.getMonthProperties(selectedDate.monthValue, selectedDate.year)

        val monthInstant: Pair<Instant, Instant> =
            TimeUtil.getMonthInstant(
                monthProperties.firstDayOfMonth,
                monthProperties.lastDayOfMonth
            )

        val healthMetricData = aggregateCalorieForEachDayOfMonthBasedOnDuration(
            startDateInstant = monthInstant.first,
            endDateInstant = monthInstant.second,
            firstDayOfMonth = LocalDateTime.of(monthProperties.firstDayOfMonth, LocalTime.MIDNIGHT),
            currentDayOfMonth = LocalDateTime.of(selectedDate, LocalTime.MIDNIGHT),
            noOfDaysInCurrentMonth = monthProperties.numberOfDaysInMonth
        )
        return healthMetricData
    }


    private suspend fun aggregateCalorieForEachDayOfMonthBasedOnDuration(
        startDateInstant: Instant,
        endDateInstant: Instant,
        firstDayOfMonth: LocalDateTime,
        currentDayOfMonth: LocalDateTime,
        noOfDaysInCurrentMonth: Int,
    ): HealthMetricData {

        val finalHealthMetricData = HealthMetricData()


        val startTime =
            LocalDateTime.ofInstant(startDateInstant, ZoneId.systemDefault()).withHour(0)
                .withMinute(0).withSecond(0).withNano(0)

        val healthMetricsWithDateTimeList = mutableListOf<HealthMetricsWithDateTime>()


        // Loop to add each day of the week
        for (i in 0..noOfDaysInCurrentMonth - 1) {
            healthMetricsWithDateTimeList.add(
                HealthMetricsWithDateTime(
                    dateTime = startTime.plusDays(i.toLong())
                )
            )
        }


        Timber.d("healthMetricsWithDateTimeList: $healthMetricsWithDateTimeList")


        val response = healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                ),
                timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant),
                timeRangeSlicer = Duration.ofDays(1)
            )

        )

        response.forEach { result: AggregationResultGroupedByDuration ->


            val bucketStartDateTime: LocalDateTime =
                LocalDateTime.ofInstant(result.startTime, ZoneId.systemDefault())

            val bucketEndDateTime =
                LocalDateTime.ofInstant(result.endTime, ZoneId.systemDefault())

            Timber.d(
                "calorie total: ${result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories}, bucketStartDateTime: $bucketStartDateTime ," + "bucketEndDateTime: $bucketEndDateTime startTime:${result.startTime} ," + "endTime: ${result.endTime}"
            )

            // Check if bucketStartDateTime is in the list
            val isPresent = healthMetricsWithDateTimeList.contains(
                HealthMetricsWithDateTime(dateTime = bucketStartDateTime)
            )


            if (isPresent) {

                val index = healthMetricsWithDateTimeList.indexOf(
                    HealthMetricsWithDateTime(
                        dateTime = bucketStartDateTime
                    )
                )


                //NOTE: Health Connect give the future calorie count by dividing the total count equally.
                //We need to prevent this while showing user the data.
                //Simply makes the calorie as 0, if the time has not been passed yet.

                healthMetricsWithDateTimeList[index].calorie =
                    if (healthMetricsWithDateTimeList[index].dateTime.isBefore(
                            LocalDateTime.now().minusHours(1)
                        )
                    ) {
                        result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
                    } else {
                        0.0
                    }
            }
        }

        finalHealthMetricData.healthMetricWithDateTime = healthMetricsWithDateTimeList

        //Aggregating Total Calorie
        val aggregatedData = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                ), timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant)
            )
        )

        //Total calorie
        val totalCalorie: Double? =
            aggregatedData[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inCalories
        finalHealthMetricData.totalCalorie = totalCalorie

        //Total Activity Duration
        val duration = activityTimeHelper.getTotalActivityTimeForDay(
            healthConnectClient,
            startTime = startDateInstant,
            endTime = endDateInstant,
        )

        finalHealthMetricData.totalActivityTime = duration


        //Finding the different of the days between start of month and current day of the month.
        // 0<<differentBetweenCurrentDayAndFirstDay<<28..31
        val differentBetweenCurrentDayAndFirstDay: Long =
            Duration.between(firstDayOfMonth, currentDayOfMonth).toDays()

        Timber.d("differentBetweenCurrentDayAndFirstDay: ${differentBetweenCurrentDayAndFirstDay}")


        //Average Calorie
        if (totalCalorie != null) {
            if (differentBetweenCurrentDayAndFirstDay == 0L) {
                finalHealthMetricData.averageCalorie = totalCalorie
            } else {
                finalHealthMetricData.averageCalorie =
                    totalCalorie / differentBetweenCurrentDayAndFirstDay
            }
        }


        //Average Duration
        if (differentBetweenCurrentDayAndFirstDay == 0L) {
            finalHealthMetricData.averageActivityTime = duration
        } else {
            finalHealthMetricData.averageActivityTime = Duration.ofSeconds(
                duration.toSeconds() / differentBetweenCurrentDayAndFirstDay
            )
        }


//            Timber.d("finalHealthMetricData: $finalHealthMetricData")

        return finalHealthMetricData

    }
}