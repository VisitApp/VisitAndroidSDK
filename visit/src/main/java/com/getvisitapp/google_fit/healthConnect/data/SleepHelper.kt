package com.getvisitapp.google_fit.healthConnect.data

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getvisitapp.google_fit.healthConnect.TimeUtil
import com.getvisitapp.google_fit.healthConnect.TimeUtil.convertInstantToEpochMillis
import com.getvisitapp.google_fit.healthConnect.TimeUtil.convertLocalDateTimeToEpochMillis
import com.getvisitapp.google_fit.healthConnect.model.internal.SleepMetric
import com.getvisitapp.google_fit.healthConnect.model.internal.SleepModel
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

class SleepHelper(private val healthConnectClient: HealthConnectClient) {

    @Throws(Exception::class)
    suspend fun getDailySleepData(
        selectedDate: LocalDate
    ): SleepMetric {


        var sleepStartTimeMillis = 0L
        var sleepEndTimeMillis = 0L
        var sleepDurationInMillis = 0L


        //Here we need to calculate sleep from previous day 10PM to next day 7AM.
        val sleepStartTime = LocalDateTime.of(selectedDate, LocalTime.MIN).minusHours(2)
            .atZone(ZoneId.systemDefault()).toInstant()

        val sleepEndTime = LocalDateTime.of(selectedDate, LocalTime.MIN).plusHours(7)
            .atZone(ZoneId.systemDefault()).toInstant()

//        Timber.d("#1 sleepStartTime: $sleepStartTime, sleepEndTime: $sleepEndTime")

        val sleepResponse = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(
                    SleepSessionRecord.SLEEP_DURATION_TOTAL
                ), timeRangeFilter = TimeRangeFilter.between(sleepStartTime, sleepEndTime)
            )
        )

        //if `isSleepDataPresent` is present, that means the user have logged the have sleep session stored in health connect.
        //Note: don't use his duration to find the sleep data, because in Health Connect there may is more than one session, and we need to find the first session duration if present.
        // `isSleepDataPresent` can be thought of a metrics to infer that user sleep session are present or not.
        val isSleepDataPresent: Duration? =
            sleepResponse[SleepSessionRecord.SLEEP_DURATION_TOTAL]


        //Normally the sleep data is not present in Health Connect,
        //so the value of it will be null.
        //You can logs sleep data from Google Fit app, then the sleep duration will come as not null.
        val normalizedSleep: Duration =
            if (isSleepDataPresent == null || isSleepDataPresent.isZero) {

                val currentDateStartTime =
                    LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusHours(2)

                val selectedDateStartTime =
                    LocalDateTime.of(selectedDate, LocalTime.MIN).minusHours(2)

//                Timber.d("#2 currentDateStartTime: $currentDateStartTime, selectedDateStartTime: $selectedDateStartTime")

                //If the user is checking the sleep duration of current date, then we just to show the time that has passed sleeping.

                if (selectedDateStartTime == currentDateStartTime) {

                    //There are 2 cases here:
//                        1. User is checking the sleep after 7 AM
//                        2. User is checking the sleep before waking up at 7AM.

                    val endTime = if (LocalDateTime.now()
                            .isAfter(LocalDateTime.of(selectedDate, LocalTime.MIN).plusHours(7))
                    ) {
                        LocalDateTime.of(selectedDate, LocalTime.MIN)
                            .plusHours(7)
                    } else {
                        LocalDateTime.now()
                    }

                    sleepStartTimeMillis =
                        selectedDateStartTime.convertLocalDateTimeToEpochMillis()
                    sleepEndTimeMillis = endTime.convertLocalDateTimeToEpochMillis()

//                    Timber.d("#3 sleepStartTimeMillis: $sleepStartTimeMillis, sleepEndTimeMillis: $sleepEndTimeMillis")

                    Duration.between(selectedDateStartTime, endTime)

                } else {

                    sleepStartTimeMillis =
                        selectedDateStartTime.convertLocalDateTimeToEpochMillis()

                    sleepEndTimeMillis =
                        sleepEndTime.convertInstantToEpochMillis()

//                    Timber.d("#4 sleepStartTimeMillis: $sleepStartTimeMillis, sleepEndTimeMillis: $sleepEndTimeMillis")


                    Duration.ofHours(8)
                }

            } else {
                //Here the actual sleep data is present.
                //1. Find sleep Sessions
                //2. Pick the first sleep session and get the start and end time and find the duration from it.


                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(sleepStartTime, sleepEndTime)
                    )
                )

//                Timber.d("#5 recordCount: ${response.records}")



                if (response.records.isNotEmpty()) {
                    val sleepRecord = response.records[0]
                    val startTime: Instant = sleepRecord.startTime
                    val endTime = sleepRecord.endTime

                    sleepStartTimeMillis = startTime.convertInstantToEpochMillis()
                    sleepEndTimeMillis = endTime.convertInstantToEpochMillis()


//                    Timber.d("#6 sleepStartTimeMillis: $sleepStartTimeMillis, sleepEndTimeMillis: $sleepEndTimeMillis")
                } else {

                    sleepStartTimeMillis =
                        sleepStartTime.convertInstantToEpochMillis()
                    sleepEndTimeMillis = sleepEndTime.convertInstantToEpochMillis()
                }


                //For debug purpose only
//                    for (sleepRecord in response.records) {
//                        val startTime: Instant = sleepRecord.startTime
//                        val endTime = sleepRecord.endTime
//                        val title = sleepRecord.title
//                        val metaData = sleepRecord.metadata
//                        val metaDataPackageName = metaData.dataOrigin.packageName
//                        val metaDataId = metaData.id
//                        val metaDataClientRecordId = metaData.clientRecordId
//                        val startZoneOffset = sleepRecord.startZoneOffset
//                        val endZoneOffset = sleepRecord.endZoneOffset
//
//                        Timber.d(
//                            "getSleepStartAndEndTime: Start Time: ${startTime.convertToLocalDateTime()}, " +
//                                    "End Time: ${endTime.convertToLocalDateTime()}, " +
//                                    "Title: $title, metaDataPackageName: $metaDataPackageName, " +
//                                    "metaDataId: $metaDataId, metaDataClientRecordId: $metaDataClientRecordId, " +
//                                    "Start Zone Offset: $startZoneOffset, " +
//                                    "End Zone Offset: $endZoneOffset \n\n"
//                        )
//                    }

                Duration.ofMillis(sleepEndTimeMillis - sleepStartTimeMillis)
            }

        sleepDurationInMillis = (sleepEndTimeMillis - sleepStartTimeMillis).absoluteValue

        val sleepDuration = Duration.ofMillis(sleepDurationInMillis)

        val formattedSleep: String = if (sleepDuration.toHoursPart() > 0) {
            "${sleepDuration.toHoursPart()}hr, ${sleepDuration.toMinutesPart()}min"
        } else {
            "${sleepDuration.toMinutesPart()}min"
        }

        val sleepMetric = SleepMetric(
            sleepDuration = sleepDuration,
            formattedSleepDuration = formattedSleep,
            sleepStartTimeMillis = sleepStartTimeMillis,
            sleepEndTimeMillis = sleepEndTimeMillis,
            sleepDurationInMillis = sleepDurationInMillis,
            sleepDateInMillis = LocalDateTime.of(selectedDate, LocalTime.MIDNIGHT)
                .convertLocalDateTimeToEpochMillis()
        )


//        Timber.d(
//            "#7 sleep: isSleepDataPresent: ${isSleepDataPresent}, " +
//                    "normalizedSleep: ${sleepMetric.sleepDuration.toMinutes()}, " +
//                    "formattedSleep: ${sleepMetric.formattedSleepDuration}, " +
//                    "sleepMetric: $sleepMetric"
//        )

        return sleepMetric

    }

    suspend fun getSleepDataForAWeek(selectedDate: LocalDate): List<SleepModel> {

        val sleepMetricList = mutableListOf<SleepModel>()

        val currentWeekFirstDate = TimeUtil.getFirstDayOfWeek(selectedDate)

        Timber.d("currentWeekFirstDate: $currentWeekFirstDate")


        val dateTimeFormatter = DateTimeFormatter.ofPattern("E")
        //Prepare the list for which you want to find the sleep data.
        // Loop to add each day of the week
        for (i in 0..6) {
            val currentDate = currentWeekFirstDate.plusDays(i.toLong())

            sleepMetricList.add(
                SleepModel(
                    day = currentDate.format(dateTimeFormatter),
                    sleepDateTime = currentDate,
                    startTimestamp = 0L,
                    wakeupTime = 0L,
                    sleepTime = 0L
                )
            )
        }

        val todayDate = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)

        sleepMetricList.forEach { sleepModel: SleepModel ->

            if (sleepModel.sleepDateTime.isAfter(todayDate)) {
                //don't do anything
            } else {
                val sleepMetric: SleepMetric =
                    getDailySleepData(sleepModel.sleepDateTime.toLocalDate())

                sleepModel.startTimestamp = sleepMetric.sleepDateInMillis
                sleepModel.wakeupTime = sleepMetric.sleepEndTimeMillis
                sleepModel.sleepTime = sleepMetric.sleepStartTimeMillis
            }
        }

        return sleepMetricList
    }


}