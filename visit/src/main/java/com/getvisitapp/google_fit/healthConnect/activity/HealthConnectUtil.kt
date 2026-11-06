package com.getvisitapp.google_fit.healthConnect.activity

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import com.getvisitapp.google_fit.HealthConnectListener
import com.getvisitapp.google_fit.healthConnect.contants.Contants
import com.getvisitapp.google_fit.healthConnect.data.GraphDataOperationsHelper
import com.getvisitapp.google_fit.healthConnect.data.GraphKey
import com.getvisitapp.google_fit.healthConnect.data.PendingGraphRequests
import com.getvisitapp.google_fit.healthConnect.data.deliverGraphResult
import com.getvisitapp.google_fit.healthConnect.enums.HealthConnectConnectionState
import com.getvisitapp.google_fit.healthConnect.helper.DailySyncManager
import com.getvisitapp.google_fit.healthConnect.helper.HourlySyncManager
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.DailyStepSyncRequest
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.DailySyncHealthMetric
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.HourlyDataSyncRequest
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


//https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/package-summary#classes

class HealthConnectUtil(val context: Context, val listener: HealthConnectListener) {

    private val TAG = "HealthConnectUtil"

    private val graphDataOperationsHelper by lazy { GraphDataOperationsHelper(getHealthConnectClient()) }


    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, throwable: Throwable ->
            Timber.d("HealthConnectUtil coroutineExceptionHandler")
            throwable.printStackTrace()
            listener.logHealthConnectError(throwable)
        }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)

    private val graphRequests = PendingGraphRequests(scope)

    var healthConnectConnectionState: HealthConnectConnectionState =
        HealthConnectConnectionState.NONE

    fun initialize() {
        updateHealthConnectState(HealthConnectConnectionState.NONE)
        scope.launch {
            checkAvailability()
        }
    }


    fun openHealthConnectApp() {
        val settingsIntent = Intent()
        settingsIntent.action = HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
        context.startActivity(settingsIntent)
    }

    fun revokeHealthConnectAccess() {
        scope.launch {
            if (healthConnectClient != null) {
                healthConnectClient!!.permissionController.revokeAllPermissions()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    Contants.previouslyRevoked = true
                }

                checkAvailability()

            } else {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context, "healthConnectClient not initialized", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    suspend fun checkAvailability(): HealthConnectConnectionState {
        val availabilityStatus = checkHealthConnectAvailabilityStatus()

        when (availabilityStatus) {

            HealthConnectClient.SDK_UNAVAILABLE -> {
                Timber.d("SDK_UNAVAILABLE")
                updateHealthConnectState(HealthConnectConnectionState.NOT_SUPPORTED)
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Timber.d("SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED")
                updateHealthConnectState(HealthConnectConnectionState.NOT_INSTALLED)
            }

            HealthConnectClient.SDK_AVAILABLE -> {
                //Note: don't put updateButtonState(HealthConnectConnectionState.INSTALLED) here. it is the job of checkPermissionsAndRun()
                Timber.d("SDK_AVAILABLE")

                return checkPermissionsAndRun(false)

            }

            else -> {
                Timber.d("availabilityStatus else")
            }
        }

        return healthConnectConnectionState
    }

    fun requestPermission() {
        when (healthConnectConnectionState) {
            HealthConnectConnectionState.NOT_INSTALLED -> {
                try {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=com.google.android.apps.healthdata")
                        )
                    )
                } catch (exception: ActivityNotFoundException) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                        )
                    )
                }
            }


            HealthConnectConnectionState.INSTALLED -> {
                listener.requestPermission()
            }


            HealthConnectConnectionState.CONNECTED,
            HealthConnectConnectionState.NOT_SUPPORTED,
            HealthConnectConnectionState.NONE -> {
                //do nothing for now.
            }

        }
    }

    fun getVisitDashboardGraph() {
        scope.launch { deliverGraphResult(listener) { getDashboardData() } }
    }

    suspend fun getTodayStepData(): Long {
        val steps =
            graphDataOperationsHelper.getTodaySteps(getHealthConnectClient())
        return steps
    }

    fun checkHealthConnectAvailabilityStatus(): Int {
        //Health Connect is not available for android version below Pie (< 28)

        val availabilityStatus = HealthConnectClient.getSdkStatus(context)
        Timber.d("availabilityStatus: $availabilityStatus")

        return availabilityStatus

    }

    private fun updateHealthConnectState(healthConnectConnectionState: HealthConnectConnectionState): String {
        this.healthConnectConnectionState = healthConnectConnectionState

        val text = when (healthConnectConnectionState) {
            HealthConnectConnectionState.NOT_SUPPORTED -> "Not Supported"
            HealthConnectConnectionState.NOT_INSTALLED -> "Install Health Connect"
            HealthConnectConnectionState.CONNECTED -> "Connected"
            HealthConnectConnectionState.INSTALLED -> "Not Connected"
            else -> "Unknown"
        }

        Handler(Looper.getMainLooper()).post {
            listener.updateHealthConnectConnectionStatus(
                status = healthConnectConnectionState,
                text
            )
        }

        return text
    }


    @Volatile
    private var healthConnectClient: HealthConnectClient? = null

    private fun getHealthConnectClient(): HealthConnectClient {
        return healthConnectClient ?: synchronized(this) {
            healthConnectClient ?: HealthConnectClient.getOrCreate(context)
                .also { healthConnectClient = it }
        }
    }

    fun checkPermissionsAndRunForStar(afterRequestingPermission: Boolean) {
        scope.launch {
            try {
                checkPermissionsAndRun(afterRequestingPermission = afterRequestingPermission)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    suspend fun checkPermissionsAndRun(afterRequestingPermission: Boolean): HealthConnectConnectionState {

        healthConnectClient = getHealthConnectClient()

        Timber.d("healthConnectClient hashcode: ${healthConnectClient.hashCode()}")

        val granted = healthConnectClient!!.permissionController.getGrantedPermissions()
        if (granted.containsAll(PERMISSIONS)) {


            if (Contants.previouslyRevoked) { //special case only happens in android 14
                updateHealthConnectState(HealthConnectConnectionState.INSTALLED)
            } else {
                updateHealthConnectState(HealthConnectConnectionState.CONNECTED)
            }

            if (afterRequestingPermission) {
                listener.userAcceptedHealthConnectPermission()
            }
        } else {
            Timber.d("Permission Not present")
            updateHealthConnectState(HealthConnectConnectionState.INSTALLED) //if the user

            if (afterRequestingPermission) {
                listener.userDeniedHealthConnectPermission()
            }
        }

        return healthConnectConnectionState
    }

    /** Request-scoped dashboard response; no listener callback is emitted. */
    suspend fun getDashboardData(): String = graphRequests.read(GraphKey("dashboard")) {
        graphDataOperationsHelper.getTodayStepsAndSleepData(getHealthConnectClient())
    }

    /** Identical pending reads share work; different graph arguments remain independent. */
    suspend fun getGraphData(type: String, frequency: String, timestamp: Long): String {
        val supported = when (type) {
            "steps", "distance", "calories" -> frequency in setOf("day", "week", "month")
            "sleep" -> frequency in setOf("day", "week")
            else -> false
        }
        require(supported) { "Unsupported graph type or frequency." }
        return graphRequests.read(GraphKey(type, frequency, timestamp)) {
            when (type to frequency) {
                "steps" to "day" -> graphDataOperationsHelper.getDailyStepsData(timestamp)
                "steps" to "week" -> graphDataOperationsHelper.getWeeklyStepsData(timestamp)
                "steps" to "month" -> graphDataOperationsHelper.getMonthlyStepsData(timestamp)
                "distance" to "day" -> graphDataOperationsHelper.getDailyDistanceData(timestamp)
                "distance" to "week" -> graphDataOperationsHelper.getWeeklyDistanceData(timestamp)
                "distance" to "month" -> graphDataOperationsHelper.getMonthlyDistanceData(timestamp)
                "calories" to "day" -> graphDataOperationsHelper.getDailyCalorieData(timestamp)
                "calories" to "week" -> graphDataOperationsHelper.getWeeklyCalorieData(timestamp)
                "calories" to "month" -> graphDataOperationsHelper.getMonthlyCalorieData(timestamp)
                "sleep" to "day" -> graphDataOperationsHelper.getDailySleepData(timestamp)
                "sleep" to "week" -> graphDataOperationsHelper.getWeeklySleepData(timestamp)
                else -> error("Graph request was not validated.")
            }
        }
    }

    fun cancelPendingGraphRequests() = graphRequests.cancel()

    // Keep the existing listener-based entry point for other native consumers.
    fun getActivityData(type: String?, frequency: String?, timeStamp: Long) {
        scope.launch {
            deliverGraphResult(listener) {
                requireNotNull(type) { "Missing graph type." }
                requireNotNull(frequency) { "Missing graph frequency." }
                getGraphData(type, frequency, timeStamp)
            }
        }
    }

    suspend fun exhaustHealthConnectQueryLimitTest(timeStamp: Long) {
        (1..1000).forEachIndexed { index, i ->
            Timber.d("exhaustHealthConnectQueryLimitTest: index: $index")
            getHourlySyncData(timeStamp)
        }
    }


    suspend fun getDailySyncData(timeStamp: Long): DailyStepSyncRequest {

        val dailySyncManager = DailySyncManager(getHealthConnectClient())
        val dailySyncData: List<DailySyncHealthMetric> =
            dailySyncManager.getDailySyncData(timeStamp)

        val requestBody = DailyStepSyncRequest(fitnessData = dailySyncData, platform = "ANDROID")

        Timber.d("getDailySyncData: requestBody: ${Gson().toJson(requestBody)}")

        return requestBody
    }


    suspend fun getHourlySyncData(timeStamp: Long): HourlyDataSyncRequest {

        val hourlySyncManager = HourlySyncManager(getHealthConnectClient())

        val hourlyRecords = hourlySyncManager.getHourlySyncData(hourlyLastSyncTimestamp = timeStamp)

        val requestBody =
            HourlyDataSyncRequest(bulkHealthData = hourlyRecords, platform = "ANDROID")


        Timber.d("getHourlySyncData: requestBody: ${Gson().toJson(requestBody)}")

        return requestBody
    }

}
/** aggregateActivityByBucketBasedOnDuration
 * steps total: 6602 ,distance total: 3784.0138429299027 meters ,calorie total: 1714.9387687454107 kcal ,startTime:2024-08-16T18:30:00Z ,endTime: 2024-08-17T18:30:00Z
 * steps total: 2153 ,distance total: 946.9942807499685 meters ,calorie total: 1544.0814161167207 kcal ,startTime:2024-08-17T18:30:00Z ,endTime: 2024-08-18T18:30:00Z
 * steps total: 2181 ,distance total: 1217.345914414582 meters ,calorie total: 1534.8714048548898 kcal ,startTime:2024-08-18T18:30:00Z ,endTime: 2024-08-19T18:30:00Z
 * steps total: 1028 ,distance total: 414.3971366935903 meters ,calorie total: 1477.0066373640257 kcal ,startTime:2024-08-19T18:30:00Z ,endTime: 2024-08-20T18:30:00Z
 * steps total: 7444 ,distance total: 6496.074988999614 meters ,calorie total: 1941.2634706186748 kcal ,startTime:2024-08-20T18:30:00Z ,endTime: 2024-08-21T18:29:59.999Z
 */

/** aggregateActivityByBucketBasedOnPeriod
 * steps total: 3771 ,distance total: 2075.2305739754383 meters ,calorie total: 1613.5464919679039 kcal ,startTime: 2024-08-16T18:30 ,endTime: 2024-08-17T18:30
 * steps total: 6607 ,distance total: 3612.4783093918018 meters ,calorie total: 1715.328791728457 kcal ,startTime: 2024-08-17T18:30 ,endTime: 2024-08-18T18:30
 * steps total: 2150 ,distance total: 1203.0787563831184 meters ,calorie total: 1532.6618118549654 kcal ,startTime: 2024-08-18T18:30 ,endTime: 2024-08-19T18:30
 * steps total: 549 ,distance total: 234.74553567468138 meters ,calorie total: 1474.9617246142407 kcal ,startTime: 2024-08-19T18:30 ,endTime: 2024-08-20T18:30
 * steps total: 7916 ,distance total: 6671.767744403136 meters ,calorie total: 1945.517986367616 kcal ,startTime: 2024-08-20T18:30 ,endTime: 2024-08-21T18:29:59.999
 */



