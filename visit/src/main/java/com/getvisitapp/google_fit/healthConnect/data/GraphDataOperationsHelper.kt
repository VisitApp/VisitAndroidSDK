package com.getvisitapp.google_fit.healthConnect.data

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getvisitapp.google_fit.healthConnect.TimeUtil.convertEpochMillisToLocalDateTime
import com.getvisitapp.google_fit.healthConnect.model.internal.HealthMetricData
import com.getvisitapp.google_fit.healthConnect.model.internal.SleepModel
import com.getvisitapp.google_fit.healthConnect.model.internal.StepsAndSleep
import com.google.gson.Gson
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class GraphDataOperationsHelper(healthConnectClient: HealthConnectClient) {

    val stepsHelper by lazy { StepHelper(healthConnectClient) }
    val distanceHelper by lazy { DistanceHelper(healthConnectClient) }
    val calorieHelper by lazy { CalorieHelper(healthConnectClient) }
    val sleepHelper by lazy { SleepHelper(healthConnectClient) }


    suspend fun getTodayStepsAndSleepData(
        healthConnectClient: HealthConnectClient
    ): String {

        val stepsStartTime =
            LocalDateTime.of(LocalDate.now(), LocalTime.MIN).atZone(ZoneId.systemDefault())
                .toInstant()


        val stepsEndTime =
            LocalDateTime.of(LocalDate.now(), LocalTime.MAX).atZone(ZoneId.systemDefault())
                .toInstant()

        val stepsResponse = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL
                ), timeRangeFilter = TimeRangeFilter.between(stepsStartTime, stepsEndTime)
            )
        )


        // The result may be null if no data is available in the time range
        val stepCount: Long? = stepsResponse[StepsRecord.COUNT_TOTAL]

        val sleepHelper = SleepHelper(healthConnectClient)

        val sleepMetric = sleepHelper.getDailySleepData(LocalDate.now())

        Timber.d("getDailySleepData() sleepDuration: ${sleepMetric.sleepDuration.toMinutes()}")

        val stepsAndSleep = StepsAndSleep(stepCount, sleepMetric)

        Timber.d(
            "Steps: ${stepsAndSleep.steps}, formattedSleepDuration: ${stepsAndSleep.sleepMetric.formattedSleepDuration}, sleepDuration: ${stepsAndSleep.sleepMetric.sleepDuration.toMinutes()} "
        )
        val finalString =
            "window.updateFitnessPermissions(true,${stepsAndSleep.steps ?: 0},${stepsAndSleep.sleepMetric.sleepDuration.toMinutes()})"

        Timber.d(finalString)


        return finalString
    }

    suspend fun getTodaySteps(
        healthConnectClient: HealthConnectClient
    ): Long {

        val stepsStartTime =
            LocalDateTime.of(LocalDate.now(), LocalTime.MIN).atZone(ZoneId.systemDefault())
                .toInstant()


        val stepsEndTime =
            LocalDateTime.of(LocalDate.now(), LocalTime.MAX).atZone(ZoneId.systemDefault())
                .toInstant()

        val stepsResponse = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL
                ), timeRangeFilter = TimeRangeFilter.between(stepsStartTime, stepsEndTime)
            )
        )


        // The result may be null if no data is available in the time range
        val stepCount: Long = stepsResponse[StepsRecord.COUNT_TOTAL] ?: 0L

        Timber.d(
            "Steps: $stepCount"
        )

        return stepCount
    }

    /**
     * Steps Reading Functions
     */

//  1.
//  Expected format: DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[0, 0, 0, 0, 0, 0, 0, 43, 462, 0, 104, 269, 188, 0, 10, 0, 0, 513, 63, 0, 11, 440, 0, 0], 'steps', 'day','57')

    suspend fun getDailyStepsData(timeStamp: Long): String {
        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()


        val healthMetricData: HealthMetricData =
            stepsHelper.getDailyStepsData(selectedDate = requestedTimeStamp.toLocalDate())

        Timber.d("getDailyStepsData: ${healthMetricData.healthMetricWithDateTime?.size}")

//        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, hourlyStepsWithDateTime ->
//            Timber.d("index: $index, steps: ${hourlyStepsWithDateTime.steps}, hour:${hourlyStepsWithDateTime.dateTime}")
//        }

        Timber.d("totalSteps: ${healthMetricData.totalSteps}")

        val duration = healthMetricData.totalActivityTime
//        duration?.let {
//            Timber.d("duration: ${duration.toHoursPart()}::${duration.toMinutesPart()}::${duration.toSecondsPart()}")
//        }


        //Formatting the string before sending it to webapp.
        val stepsSeparated = healthMetricData.healthMetricWithDateTime?.map { it.steps }
            ?.joinToString(separator = ",") { step -> if (step == null) "0" else "$step" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[$stepsSeparated], 'steps', 'day','${duration?.toMinutes()}')"


        Timber.d("value: $webString")

        return webString
    }

    //2.
    //Expected Output: DetailedGraph.updateData([1,2,3,4,5,6,7],[88, 0, 0, 0, 0, 0, 0],'steps', 'week','1')

    suspend fun getWeeklyStepsData(timeStamp: Long): String {

        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()

        val healthMetricData: HealthMetricData = stepsHelper.getWeeklyStepsData(
            selectedDate = requestedTimeStamp.toLocalDate()
        )

        Timber.d("getWeeklyStepsData: $healthMetricData")
//
        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, stepsOfEachDateOfTheWeek ->
            Timber.d("index: $index, steps: ${stepsOfEachDateOfTheWeek.steps}, hour:${stepsOfEachDateOfTheWeek.dateTime}")
        }

        Timber.d("totalSteps: ${healthMetricData.totalSteps}, averageSteps:${healthMetricData.averageSteps}")

        val totalActivityTime = healthMetricData.totalActivityTime
        totalActivityTime?.let {
            Timber.d("totalActivityTime: ${totalActivityTime.toHoursPart()}::${totalActivityTime.toMinutesPart()}::${totalActivityTime.toSecondsPart()}")
        }

        val averageActivityTime = healthMetricData.averageActivityTime
        averageActivityTime?.let {
            Timber.d("averageActivityTime: ${averageActivityTime.toHoursPart()}::${averageActivityTime.toMinutesPart()}::${averageActivityTime.toSecondsPart()}")
        }


        //Formatting the string before sending it to webapp.
        val stepsSeparated = healthMetricData.healthMetricWithDateTime?.map { it.steps }
            ?.joinToString(separator = ",") { step -> if (step == null) "0" else "$step" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7],[$stepsSeparated], 'steps', 'week','${averageActivityTime?.toMinutes()}')"


        Timber.d("value: $webString")

        return webString
    }

//  3.
//  Expected Output : DetailedGraph . updateData ([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31], [8728, 1220, 5633, 2231, 696, 2482, 6081, 8087, 3207, 9807, 1690, 2917, 1267, 4786, 1193, 1871, 6603, 2154, 2182, 1029, 2445, 3183, 549, 3458, 2103, 88, 0, 0, 0, 0, 0], 'steps', 'month', '36')

    suspend fun getMonthlyStepsData(timeStamp: Long): String {

        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()

        val healthMetricData: HealthMetricData = stepsHelper.getMonthlyStepsData(
            requestedTimeStamp.toLocalDate()
        )

        Timber.d("stepsGraphData: $healthMetricData")
//
        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, stepsOfEachDateOfTheMonth ->
            Timber.d("index: $index, steps: ${stepsOfEachDateOfTheMonth.steps}, hour:${stepsOfEachDateOfTheMonth.dateTime}")
        }

        Timber.d("totalSteps: ${healthMetricData.totalSteps}, averageSteps:${healthMetricData.averageSteps}")

        val totalActivityTime = healthMetricData.totalActivityTime
        totalActivityTime?.let {
            Timber.d("totalActivityTime: ${totalActivityTime.toHours()}::${totalActivityTime.toMinutesPart()}::${totalActivityTime.toSecondsPart()}")
        }

        val averageActivityTime = healthMetricData.averageActivityTime
        averageActivityTime?.let {
            Timber.d("averageActivityTime: ${averageActivityTime.toHours()}::${averageActivityTime.toMinutesPart()}::${averageActivityTime.toSecondsPart()}")
        }


        //Formatting the string before sending it to webapp.
        val stepsSeparated = healthMetricData.healthMetricWithDateTime?.map { it.steps }
            ?.joinToString(separator = ",") { step -> if (step == null) "0" else "$step" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31],[$stepsSeparated], 'steps', 'month','${averageActivityTime?.toMinutes()}')"


        Timber.d("value: $webString")

        return webString
    }


    /**
     * Distance Reading Functions
     */

    //1.
    //Expected Output: DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], 'distance', 'day','0')
    //Actual Output:   DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0], 'distance', 'day','0')

    suspend fun getDailyDistanceData(timeStamp: Long): String {

        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()

        val healthMetricData: HealthMetricData =
            distanceHelper.getDailyDistanceData(selectedDate = requestedTimeStamp.toLocalDate())

        Timber.d("getDailyDistanceData: ${healthMetricData.healthMetricWithDateTime?.size}")
//
        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, healthMetricWithDateTime ->
            Timber.d("index: $index, distance: ${healthMetricWithDateTime.distance}, hour:${healthMetricWithDateTime.dateTime}")
        }

        Timber.d("totalDistance: ${healthMetricData.totalDistance}")

        val duration = healthMetricData.totalActivityTime
        duration?.let {
            Timber.d("duration: ${duration.toHoursPart()}::${duration.toMinutesPart()}::${duration.toSecondsPart()}")
        }


        //Formatting the string before sending it to webapp.
        val distanceSeparated = healthMetricData.healthMetricWithDateTime?.map { it.distance }
            ?.joinToString(separator = ",") { distance -> if (distance == null) "0" else "${distance.toInt()}" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[$distanceSeparated], 'distance', 'day','${duration?.toMinutes()}')"


        Timber.d("value: $webString")

        return webString
    }

    //2.
    //Expected Output: DetailedGraph.updateData([1,2,3,4,5,6,7],[76, 7, 0, 0, 0, 0, 0],'distance', 'week','1')
    //Actual Output:   DetailedGraph.updateData([1,2,3,4,5,6,7],[76,7,0,0,0,0,0], 'distance', 'week','3')

    suspend fun getWeeklyDistanceData(timeStamp: Long): String {

        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()

        val healthMetricData: HealthMetricData = distanceHelper.getWeeklyDistanceData(
            selectedDate = requestedTimeStamp.toLocalDate()
        )

        Timber.d("getDailyDistanceData: $healthMetricData")
//
        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, distanceOfEachDateOfTheWeek ->
            Timber.d("index: $index, distance: ${distanceOfEachDateOfTheWeek.distance}, hour:${distanceOfEachDateOfTheWeek.dateTime}")
        }

        Timber.d("totalDistance: ${healthMetricData.totalDistance}, averageDistance:${healthMetricData.averageDistance}")

        val totalActivityTime = healthMetricData.totalActivityTime
        totalActivityTime?.let {
            Timber.d("totalActivityTime: ${totalActivityTime.toHoursPart()}::${totalActivityTime.toMinutesPart()}::${totalActivityTime.toSecondsPart()}")
        }

        val averageActivityTime = healthMetricData.averageActivityTime
        averageActivityTime?.let {
            Timber.d("averageActivityTime: ${averageActivityTime.toHoursPart()}::${averageActivityTime.toMinutesPart()}::${averageActivityTime.toSecondsPart()}")
        }


        //Formatting the string before sending it to webapp.
        val distanceSeparated = healthMetricData.healthMetricWithDateTime?.map { it.distance }
            ?.joinToString(separator = ",") { distance -> if (distance == null) "0" else "${distance.toInt()}" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7],[$distanceSeparated], 'distance', 'week','${averageActivityTime?.toMinutes()}')"


        Timber.d("value: $webString")

        return webString
    }

    //3.
    //Expected Output: DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31],[5010, 620, 2957, 1211, 339, 1293, 3925, 5098, 1781, 5796, 837, 1788, 655, 1582, 644, 1036, 3784, 947, 1217, 414, 6496, 1969, 280, 2000, 1007, 76, 7, 0, 0, 0, 0],'distance', 'month','36')
    //Actual Output:   DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31],[5010,620,2957,1211,339,1293,3925,5097,1781,5796,837,1788,655,1582,644,1036,3784,946,1217,414,6496,1969,280,2000,1007,76,7,0,0,0,0], 'distance', 'month','30')
    suspend fun getMonthlyDistanceData(timeStamp: Long): String {
        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()


        val healthMetricData = distanceHelper.getMonthlyDistanceData(
            requestedTimeStamp.toLocalDate()
        )

        Timber.d("getMonthlyDistanceData: $healthMetricData")
//
        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, distanceOfEachDateOfTheMonth ->
            Timber.d("index: $index, distance: ${distanceOfEachDateOfTheMonth.distance}, hour:${distanceOfEachDateOfTheMonth.dateTime}")
        }

        Timber.d("totalDistance: ${healthMetricData.totalDistance}, averageDistance:${healthMetricData.averageDistance}")

        val totalActivityTime = healthMetricData.totalActivityTime
        totalActivityTime?.let {
            Timber.d("totalActivityTime: ${totalActivityTime.toHours()}::${totalActivityTime.toMinutesPart()}::${totalActivityTime.toSecondsPart()}")
        }

        val averageActivityTime = healthMetricData.averageActivityTime
        averageActivityTime?.let {
            Timber.d("averageActivityTime: ${averageActivityTime.toHours()}::${averageActivityTime.toMinutesPart()}::${averageActivityTime.toSecondsPart()}")
        }

        //Formatting the string before sending it to webapp.
        val distanceSeparated = healthMetricData.healthMetricWithDateTime?.map { it.distance }
            ?.joinToString(separator = ",") { distance -> if (distance == null) "0" else "${distance.toInt()}" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31],[$distanceSeparated], 'distance', 'month','${averageActivityTime?.toMinutes()}')"


        Timber.d("value: $webString")

        return webString
    }

    /**
     * Calorie Reading Functions
     */

    //1.
    //Expected Output: DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[61, 61, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], 'calories', 'day','0')
    //Actual Output: DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[61,61,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0], 'calories', 'day','1')

    suspend fun getDailyCalorieData(timeStamp: Long): String {

        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()

        val healthMetricData: HealthMetricData =
            calorieHelper.getDailyCalorieData(selectedDate = requestedTimeStamp.toLocalDate())

        Timber.d("getDailyCalorieData: ${healthMetricData.healthMetricWithDateTime?.size}")
//
        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, healthMetricWithDateTime ->
            Timber.d("index: $index, calorie: ${healthMetricWithDateTime.calorie}, hour:${healthMetricWithDateTime.dateTime}")
        }

        Timber.d("totalCalorie: ${healthMetricData.totalCalorie}")

        val duration = healthMetricData.totalActivityTime
        duration?.let {
            Timber.d("duration: ${duration.toHoursPart()}::${duration.toMinutesPart()}::${duration.toSecondsPart()}")
        }


        //Formatting the string before sending it to webapp.
        val calorieSeparated = healthMetricData.healthMetricWithDateTime?.map { it.calorie }
            ?.joinToString(separator = ",") { calorie -> if (calorie == null) "0" else "${calorie.toInt()}" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[$calorieSeparated], 'calories', 'day','${duration?.toMinutes()}')"

        Timber.d("value: $webString")

        return webString
    }

    //2.
    //Expected Output: DetailedGraph.updateData([1,2,3,4,5,6,7],[1470, 1464, 0, 0, 0, 0, 0],'calories', 'week','1')
    //Actually Output: DetailedGraph.updateData([1,2,3,4,5,6,7],[1470,1464,0,0,0,0,0], 'calories', 'week','11')

    suspend fun getWeeklyCalorieData(timeStamp: Long): String {

        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()

        val healthMetricData: HealthMetricData = calorieHelper.getWeeklyCalorieData(
            selectedDate = requestedTimeStamp.toLocalDate()
        )

        Timber.d("getWeeklyCalorieData: $healthMetricData")
//
        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, caloriesOfEachDateOfTheWeek ->

            Timber.d("index: $index, calorie: ${caloriesOfEachDateOfTheWeek.calorie}, hour:${caloriesOfEachDateOfTheWeek.dateTime}")
        }

        Timber.d("totalCalorie: ${healthMetricData.totalCalorie}, averageCalorie:${healthMetricData.averageCalorie}")

        val totalActivityTime = healthMetricData.totalActivityTime
        totalActivityTime?.let {
            Timber.d("totalActivityTime: ${totalActivityTime.toHoursPart()}::${totalActivityTime.toMinutesPart()}::${totalActivityTime.toSecondsPart()}")
        }

        val averageActivityTime = healthMetricData.averageActivityTime
        averageActivityTime?.let {
            Timber.d("averageActivityTime: ${averageActivityTime.toHoursPart()}::${averageActivityTime.toMinutesPart()}::${averageActivityTime.toSecondsPart()}")
        }


        //Formatting the string before sending it to webapp.
        val calorieSeparated = healthMetricData.healthMetricWithDateTime?.map { it.calorie }
            ?.joinToString(separator = ",") { calorie -> if (calorie == null) "0" else "${calorie.toInt()}" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7],[$calorieSeparated], 'calories', 'week','${averageActivityTime?.toMinutes()}')"


        Timber.d("value: $webString")

        return webString
    }


    //3.
    //Expected Output: DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31],[1655, 1655, 1655, 1655, 1655, 1655, 1655, 1655, 1655, 1655, 1655, 1655, 1583, 1559, 1509, 1534, 1714, 1544, 1534, 1477, 1941, 1570, 1484, 1576, 1652, 1470, 1464, 0, 0, 0, 0],'calories', 'month','36')
    //Actual Output:   DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31],[1655,1655,1655,1655,1655,1655,1655,1655,1655,1655,1655,1655,1583,1559,1509,1534,1714,1544,1534,1477,1941,1570,1484,1576,1652,1470,1464,0,0,0,0], 'calories', 'month','30')
    suspend fun getMonthlyCalorieData(timeStamp: Long): String {

        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()

        val healthMetricData: HealthMetricData = calorieHelper.getMonthlyCalorieData(
            requestedTimeStamp.toLocalDate()
        )


        Timber.d("getMonthlyCalorieData: $healthMetricData")
//
        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, healthMetricWithDateTime ->
            Timber.d("index: $index, calorie: ${healthMetricWithDateTime.calorie}, hour:${healthMetricWithDateTime.dateTime}")
        }

        Timber.d("totalCalorie: ${healthMetricData.totalCalorie}, averageCalorie:${healthMetricData.averageCalorie}")

        val totalActivityTime = healthMetricData.totalActivityTime
        totalActivityTime?.let {
            Timber.d("totalActivityTime: ${totalActivityTime.toHours()}::${totalActivityTime.toMinutesPart()}::${totalActivityTime.toSecondsPart()}")
        }

        val averageActivityTime = healthMetricData.averageActivityTime
        averageActivityTime?.let {
            Timber.d("averageActivityTime: ${averageActivityTime.toHours()}::${averageActivityTime.toMinutesPart()}::${averageActivityTime.toSecondsPart()}")
        }

        //Formatting the string before sending it to webapp.
        val calorieSeparated = healthMetricData.healthMetricWithDateTime?.map { it.calorie }
            ?.joinToString(separator = ",") { calorie -> if (calorie == null) "0" else "${calorie.toInt()}" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31],[$calorieSeparated], 'calories', 'month','${averageActivityTime?.toMinutes()}')"


        Timber.d("value: $webString")

        return webString
    }

    /**
     * Sleep Reading Function
     */

    //1. Daily Sleep
    //Expected Output: DetailedGraph.updateDailySleep(1724689800000,1724712378235)

    suspend fun getDailySleepData(timeStamp: Long): String {

        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()

        val sleepMetric = sleepHelper.getDailySleepData(requestedTimeStamp.toLocalDate())

        Timber.d("getDailySleepData() sleepDuration: ${sleepMetric?.sleepDuration?.toHoursPart()}::${sleepMetric?.sleepDuration?.toMinutesPart()}")

        val webString =
            "DetailedGraph.updateDailySleep(${sleepMetric?.sleepStartTimeMillis},${sleepMetric?.sleepEndTimeMillis})"

        Timber.d("value: $webString")

        return webString
    }


    //        2. Weekly Sleep
//        Expected Output: DetailedGraph.updateSleepData(JSON.stringify([{"sleepTime":1723395642384,"wakeupTime":1723426200000,"day":"Mon","startTimestamp":1723401000000},{"sleepTime":1723480200000,"wakeupTime":1723512600000,"day":"Tue","startTimestamp":1723487400000},{"sleepTime":1723570618608,"wakeupTime":1723599000000,"day":"Wed","startTimestamp":1723573800000},{"sleepTime":1723661950089,"wakeupTime":1723685400000,"day":"Thu","startTimestamp":1723660200000},{"sleepTime":1723739400000,"wakeupTime":1723771800000,"day":"Fri","startTimestamp":1723746600000},{"sleepTime":1723825800000,"wakeupTime":1723858200000,"day":"Sat","startTimestamp":1723833000000},{"sleepTime":1723923449337,"wakeupTime":1723944600000,"day":"Sun","startTimestamp":1723919400000}]));
//        Actual Output:   DetailedGraph.updateSleepData(JSON.stringify([{"day":"Mon","sleepTime":32400000,"startTimestamp":1723998600000,"wakeupTime":1724031000000},{"day":"Tue","sleepTime":32400000,"startTimestamp":1724085000000,"wakeupTime":1724117400000},{"day":"Wed","sleepTime":28800000,"startTimestamp":1724171640000,"wakeupTime":1724200440000},{"day":"Thu","sleepTime":30600000,"startTimestamp":1724263331060,"wakeupTime":1724293931060},{"day":"Fri","sleepTime":32400000,"startTimestamp":1724344200000,"wakeupTime":1724376600000},{"day":"Sat","sleepTime":22140000,"startTimestamp":1724438160000,"wakeupTime":1724460300000},{"day":"Sun","sleepTime":32400000,"startTimestamp":1724517000000,"wakeupTime":1724549400000}]));
    suspend fun getWeeklySleepData(timeStamp: Long): String {

        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()

        val weeklySleepData: List<SleepModel> =
            sleepHelper.getSleepDataForAWeek(requestedTimeStamp.toLocalDate())
        Timber.d("weeklySleepData() $weeklySleepData")

        val weeklySleepJson = Gson().toJson(weeklySleepData)

        val webString = "DetailedGraph.updateSleepData(JSON.stringify(${weeklySleepJson}));"


        Timber.d("weeklySleepJson: $weeklySleepJson")
        Timber.d("webString: $webString")

        return webString

    }

}