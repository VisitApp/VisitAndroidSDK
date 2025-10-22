package com.getvisitapp.google_fit.healthConnect.data

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.records.StepsRecord
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

class StepHelper(private val healthConnectClient: HealthConnectClient) {

    val activityTimeHelper = ActivityTimeHelper()

    suspend fun getDailyStepsData(
        selectedDate: LocalDate,
    ): HealthMetricData {
        val selectedDateTimeInstant =
            TimeUtil.getSelectedDateStartTimeAndEndTimeInstant(selectedDate)

        val healthMetricData: HealthMetricData = aggregateStepsForTheDayForEachHoursBasedOnDuration(
            selectedDateTimeInstant.first, selectedDateTimeInstant.second
        )
        return healthMetricData
    }


    //1. Find the no. of steps that happens in each hour of the day.
    //2. Find the total activity time
    //3. Find the total steps.
    private suspend fun aggregateStepsForTheDayForEachHoursBasedOnDuration(
        startDateInstant: Instant,
        endDateInstant: Instant
    ): HealthMetricData {

        val finalHealthMetricData = HealthMetricData()


        val startDateTimeOfTheDay =
            LocalDateTime.ofInstant(startDateInstant, ZoneId.systemDefault()).withHour(0)
                .withMinute(0).withSecond(0).withNano(0)


        //Here we are making a list which will contain steps for each hour of the day.
        val healthMetricsWithDateTimeList = mutableListOf<HealthMetricsWithDateTime>()


        // Loop to add each hour of the day
        //Prefilling the steps as 0.
        for (i in 0..23) {
            // Add the LocalDateTime to the list
            healthMetricsWithDateTimeList.add(
                HealthMetricsWithDateTime(
                    dateTime = startDateTimeOfTheDay.plusHours(i.toLong())
                )
            )
        }


//            Timber.d("stepsWithDateTimeList: $stepsWithDateTimeList")


        //Running aggregation function to get the steps for each hour of the day.
        val response = healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                ),
                timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant),
                timeRangeSlicer = Duration.ofHours(1)
            )
        )


        //The returned list can be sparse, so the days for steps are 0 may not be included in this list.
        response.forEach { result: AggregationResultGroupedByDuration ->


            val bucketStartDateTime: LocalDateTime =
                LocalDateTime.ofInstant(result.startTime, ZoneId.systemDefault())

            val bucketEndDateTime =
                LocalDateTime.ofInstant(result.endTime, ZoneId.systemDefault())

//                Timber.d(
//                    "bucketStartDateTime: $bucketStartDateTime ," + "bucketEndDateTime: $bucketEndDateTime, " + "steps total: ${result.result[StepsRecord.COUNT_TOTAL]} " + "startTime:${result.startTime} ," + "endTime: ${result.endTime}"
//                )

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

                //assigning the step count for that date.
                healthMetricsWithDateTimeList[index].steps =
                    result.result[StepsRecord.COUNT_TOTAL]
            }
        }



        finalHealthMetricData.healthMetricWithDateTime = healthMetricsWithDateTimeList


        val aggregatedData = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                ), timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant)
            )
        )

        val totalSteps: Long? = aggregatedData[StepsRecord.COUNT_TOTAL]

        finalHealthMetricData.totalSteps = totalSteps


        //Here we are finding the activity time for that day.
        val duration = activityTimeHelper.getTotalActivityTimeForDay(
            healthConnectClient,
            startTime = startDateInstant,
            endTime = endDateInstant,
        )

        finalHealthMetricData.totalActivityTime = duration

//            Timber.d("totalSteps:$totalSteps, distanceTotal: $distanceTotal, activityDurationTotal: $activityDurationTotal")

//            Timber.d("stepsWithDateTimeList: $hourlyStepsWithDateTimeList")


        return finalHealthMetricData
    }


    /**
     * 2. Get Steps data of a particular week
     *  @param selectedDate: this can be any date for which you want to find the weekly step data.
     */

    suspend fun getWeeklyStepsData(
        selectedDate: LocalDate
    ): HealthMetricData {
        val currentWeekFirstDate = TimeUtil.getFirstDayOfWeek(selectedDate)
        val weekInstance: Pair<Instant, Instant> = TimeUtil.getWeekInstant(currentWeekFirstDate)

        val healthMetricData: HealthMetricData = aggregateStepsForEachDayOfWeekBasedOnDuration(
            startDateInstant = weekInstance.first,
            endDateInstant = weekInstance.second,
            firstDayOfWeek = currentWeekFirstDate,
            currentDayOfWeek = LocalDateTime.of(selectedDate, LocalTime.MIDNIGHT)
        )
        return healthMetricData
    }


    private suspend fun aggregateStepsForEachDayOfWeekBasedOnDuration(
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


        Timber.d("stepsWithDateTimeList: $healthMetricsWithDateTimeList")


        val response = healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
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
                "bucketStartDateTime: $bucketStartDateTime ," + "bucketEndDateTime: $bucketEndDateTime, " + "steps total: ${result.result[StepsRecord.COUNT_TOTAL]} " + "startTime:${result.startTime} ," + "endTime: ${result.endTime}"
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

                healthMetricsWithDateTimeList[index].steps =
                    result.result[StepsRecord.COUNT_TOTAL]
            }


        }

        finalHealthMetricData.healthMetricWithDateTime = healthMetricsWithDateTimeList


        //Aggregating Total Steps
        val aggregatedData = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                ), timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant)
            )
        )

        //Total Steps
        val totalSteps: Long? = aggregatedData[StepsRecord.COUNT_TOTAL]
        finalHealthMetricData.totalSteps = totalSteps

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


        //Average Steps
        if (totalSteps != null) {
            if (differentBetweenCurrentDayAndFirstDay == 0L) {
                finalHealthMetricData.averageSteps = totalSteps
            } else {
                finalHealthMetricData.averageSteps =
                    totalSteps / differentBetweenCurrentDayAndFirstDay
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

        return finalHealthMetricData
    }

    /**
     * 3. Get steps data of a particular month.
     * @param selectedDate: any date of a month.
     * @return steps from start date to end date of the month.
     * //Note: Health Connect only stores the data of last 30 days.
     */

//3. Get Steps Data of a particular month

    suspend fun getMonthlyStepsData(
        selectedDate: LocalDate
    ): HealthMetricData {
        val monthProperties: MonthProperties =
            TimeUtil.getMonthProperties(selectedDate.monthValue, selectedDate.year)

        val monthInstant: Pair<Instant, Instant> =
            TimeUtil.getMonthInstant(
                monthProperties.firstDayOfMonth,
                monthProperties.lastDayOfMonth
            )

        val healthMetricData = aggregateStepsForEachDayOfMonthBasedOnDuration(
            startDateInstant = monthInstant.first,
            endDateInstant = monthInstant.second,
            firstDayOfMonth = LocalDateTime.of(monthProperties.firstDayOfMonth, LocalTime.MIDNIGHT),
            currentDayOfMonth = LocalDateTime.of(selectedDate, LocalTime.MIDNIGHT),
            noOfDaysInCurrentMonth = monthProperties.numberOfDaysInMonth
        )

        return healthMetricData
    }


    private suspend fun aggregateStepsForEachDayOfMonthBasedOnDuration(
        startDateInstant: Instant,
        endDateInstant: Instant,
        firstDayOfMonth: LocalDateTime,
        currentDayOfMonth: LocalDateTime,
        noOfDaysInCurrentMonth: Int
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


        Timber.d("stepsWithDateTimeList: $healthMetricsWithDateTimeList")


        val response = healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
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
                "bucketStartDateTime: $bucketStartDateTime ," + "bucketEndDateTime: $bucketEndDateTime, " + "steps total: ${result.result[StepsRecord.COUNT_TOTAL]} " + "startTime:${result.startTime} ," + "endTime: ${result.endTime}"
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

                healthMetricsWithDateTimeList[index].steps =
                    result.result[StepsRecord.COUNT_TOTAL]
            }


        }

        finalHealthMetricData.healthMetricWithDateTime = healthMetricsWithDateTimeList

        //Aggregating Total Steps
        val aggregatedData = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                ), timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant)
            )
        )

        //Total Steps
        val totalSteps: Long? = aggregatedData[StepsRecord.COUNT_TOTAL]
        finalHealthMetricData.totalSteps = totalSteps

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


        //Average Steps
        if (totalSteps != null) {
            if (differentBetweenCurrentDayAndFirstDay == 0L) {
                finalHealthMetricData.averageSteps = totalSteps
            } else {
                finalHealthMetricData.averageSteps =
                    totalSteps / differentBetweenCurrentDayAndFirstDay
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


//            Timber.d("stepsWithDateTimeList: $hourlyStepsWithDateTimeList")

        return finalHealthMetricData
    }
}