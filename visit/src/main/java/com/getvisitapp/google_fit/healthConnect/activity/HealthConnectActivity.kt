//package com.getvisitapp.google_fit.healthConnect.activity
//
//import android.content.ActivityNotFoundException
//import android.content.Intent
//import android.net.Uri
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.activity.result.contract.ActivityResultContract
//import androidx.appcompat.app.AppCompatActivity
//import androidx.databinding.DataBindingUtil
//import androidx.health.connect.client.HealthConnectClient
//import androidx.health.connect.client.PermissionController
//import androidx.health.connect.client.permission.HealthPermission
//import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
//import androidx.health.connect.client.records.DistanceRecord
//import androidx.health.connect.client.records.ExerciseSessionRecord
//import androidx.health.connect.client.records.SleepSessionRecord
//import androidx.health.connect.client.records.StepsRecord
//import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
//import com.getvisitapp.google_fit.R
//import com.getvisitapp.google_fit.databinding.HealthConnectActivityBinding
//import com.getvisitapp.google_fit.healthConnect.data.GraphDataOperationsHelper
//import com.getvisitapp.google_fit.healthConnect.enums.HealthConnectConnectionState
//import com.getvisitapp.google_fit.healthConnect.helper.DailySyncManager
//import com.getvisitapp.google_fit.healthConnect.helper.HourlySyncManager
//import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.DailyStepSyncRequest
//import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.DailySyncHealthMetric
//import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.HourlyDataSyncRequest
//
//import com.google.gson.Gson
//import kotlinx.coroutines.CoroutineExceptionHandler
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import timber.log.Timber
//
////https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/package-summary#classes
//
//class HealthConnectActivity : AppCompatActivity() {
//
//    val TAG = "HealthConnectActivity"
//    lateinit var binding: HealthConnectActivityBinding
//
//    val graphDataOperationsHelper by lazy { GraphDataOperationsHelper(getHealthConnectClient()) }
//
//
//    private val PERMISSIONS = setOf(
//        HealthPermission.getReadPermission(StepsRecord::class),
//        HealthPermission.getReadPermission(DistanceRecord::class),
//        HealthPermission.getReadPermission(SleepSessionRecord::class),
//        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
//        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
//        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
//    )
//
//    private val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
//        Timber.d("HealthConnectActivity coroutineExceptionHandler")
//        throwable.printStackTrace()
//
//
//        val formattedMessage = "${throwable.javaClass}: ${throwable.message}"
//        Timber.d("mytag: $formattedMessage")
//
//
//    }
//
//    val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
//
//
//    val requestPermissionActivityContract: ActivityResultContract<Set<String>, Set<String>> =
//        PermissionController.createRequestPermissionResultContract()
//
//
//    val requestPermissions =
//        registerForActivityResult(requestPermissionActivityContract) { granted: Set<String> ->
//            if (granted.containsAll(PERMISSIONS)) {
//                Timber.d("Permissions successfully granted")
//
//                updateButtonState(HealthConnectConnectionState.CONNECTED)
//                scope.launch {
//                    checkPermissionsAndRun()
//                }
//
//            } else {
//                Timber.d(" Lack of required permissions")
//
//                //Currently the Health Connect SDK, only asks for the remaining permission was the NOT granted in the first time, and when it return,
//                //it also send the granted permission (and not the permission that was previously granted), so the control flow comes inside the else statement.
//                //So we need to check for permission again
//                scope.launch {
//                    checkPermissionsAndRun()
//                }
//            }
//        }
//
//    var healthConnectConnectionState: HealthConnectConnectionState =
//        HealthConnectConnectionState.NONE
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        binding = DataBindingUtil.setContentView(this, R.layout.activity_health_connect)
//
//        updateButtonState(HealthConnectConnectionState.NONE)
//
//
//
//        binding.checkHealthConnectAvailabilityStatus.setOnClickListener {
//            checkAvailability()
//        }
//
//        binding.initialHealthConnect.setOnClickListener {
//
//            when (healthConnectConnectionState) {
//                HealthConnectConnectionState.NOT_INSTALLED -> {
//                    try {
//                        startActivity(
//                            Intent(
//                                Intent.ACTION_VIEW,
//                                Uri.parse("market://details?id=com.google.android.apps.healthdata")
//                            )
//                        )
//                    } catch (exception: ActivityNotFoundException) {
//                        startActivity(
//                            Intent(
//                                Intent.ACTION_VIEW,
//                                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
//                            )
//                        )
//                    }
//                }
//
//
//                HealthConnectConnectionState.INSTALLED -> {
//                    requestPermissions.launch(PERMISSIONS)
//                }
//
//                HealthConnectConnectionState.CONNECTED -> {
//                    scope.launch {
//                        checkPermissionsAndRun()
//                    }
//
//                }
//
//                HealthConnectConnectionState.NOT_SUPPORTED, HealthConnectConnectionState.NONE -> {
//                    //do nothing for now.
//                }
//
//            }
//        }
//
//        binding.openHealthConnectApp.setOnClickListener {
//            val settingsIntent = Intent()
//            settingsIntent.action = HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
//            startActivity(settingsIntent)
//        }
//
//        binding.removeHealthConnectPermission.setOnClickListener {
//            scope.launch {
//                if (healthConnectClient != null) {
//                    healthConnectClient!!.permissionController.revokeAllPermissions()
//                    checkAvailability()
//                } else {
//                    Handler(Looper.getMainLooper()).post {
//                        Toast.makeText(
//                            this@HealthConnectActivity,
//                            "healthConnectClient not initialized",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }
//        }
//    }
//
//    fun checkAvailability() {
//        val availabilityStatus = checkHealthConnectAvailabilityStatus()
//
//        when (availabilityStatus) {
//
//            HealthConnectClient.SDK_UNAVAILABLE -> {
//                Timber.d("SDK_UNAVAILABLE")
//                updateButtonState(HealthConnectConnectionState.NOT_SUPPORTED)
//            }
//
//            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
//                Timber.d("SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED")
//                updateButtonState(HealthConnectConnectionState.NOT_INSTALLED)
//            }
//
//            HealthConnectClient.SDK_AVAILABLE -> {
//                Timber.d("SDK_AVAILABLE")
//                scope.launch {
//                    checkPermissionsAndRun()
//                }
//            }
//
//            else -> {
//                Timber.d("availabilityStatus else")
//            }
//        }
//    }
//
//    fun checkHealthConnectAvailabilityStatus(): Int {
//        //Health Connect is not available for android version below Pie (< 28)
//
//        val availabilityStatus = HealthConnectClient.getSdkStatus(this)
//        Timber.d("availabilityStatus: $availabilityStatus")
//
//        return availabilityStatus
//
//    }
//
//    fun updateButtonState(healthConnectConnectionState: HealthConnectConnectionState): String {
//        this.healthConnectConnectionState = healthConnectConnectionState
//
//        val text = when (healthConnectConnectionState) {
//            HealthConnectConnectionState.NOT_SUPPORTED -> "Not Supported"
//            HealthConnectConnectionState.NOT_INSTALLED -> "Install Health Connect"
//            HealthConnectConnectionState.CONNECTED -> "Connected"
//            HealthConnectConnectionState.INSTALLED -> "Not Connected"
//            else -> "Unknown"
//        }
//
//        Handler(Looper.getMainLooper()).post {
//            binding.initialHealthConnect.text = text
//        }
//
//        return text
//    }
//
//
//    @Volatile
//    private var healthConnectClient: HealthConnectClient? = null
//
//    private fun getHealthConnectClient(): HealthConnectClient {
//        return healthConnectClient ?: synchronized(this) {
//            healthConnectClient ?: HealthConnectClient.getOrCreate(this)
//                .also { healthConnectClient = it }
//        }
//    }
//
//
//    private suspend fun checkPermissionsAndRun() {
//
//        healthConnectClient = getHealthConnectClient()
//
//        Timber.d("healthConnectClient hashcode: ${healthConnectClient.hashCode()}")
//
//        val granted = healthConnectClient!!.permissionController.getGrantedPermissions()
//        if (granted.containsAll(PERMISSIONS)) {
//
//            updateButtonState(HealthConnectConnectionState.CONNECTED)
//
//            // Permissions already granted; proceed with inserting or reading data
//
//            Timber.d("All Permission Allowed")
//
//            var timeStamp = 1732165563000L //current time
////            var timeStamp = 1724424597000L // one week before time
//
//            scope.launch {
//
////                Tutorials(healthConnectClient!!).fetchData()
//
////                getDailySyncData(timeStamp)
////                getHourlySyncData(timeStamp)
//                exhaustHealthConnectQueryLimitTest(timeStamp)
//
////                getDailyStepAndSleepData()
//
////                getActivityData(type = "steps", frequency = "day", timeStamp = timeStamp)
////                getActivityData(type = "steps", frequency = "week", timeStamp = timeStamp)
////                getActivityData(type = "steps", frequency = "month", timeStamp = timeStamp)
////
////                getActivityData(type = "distance", frequency = "day", timeStamp = timeStamp)
////                getActivityData(type = "distance", frequency = "week", timeStamp = timeStamp)
////                getActivityData(type = "distance", frequency = "month", timeStamp = timeStamp)
////
////                getActivityData(type = "calories", frequency = "day", timeStamp = timeStamp)
////                getActivityData(type = "calories", frequency = "week", timeStamp = timeStamp)
////                getActivityData(type = "calories", frequency = "month", timeStamp = timeStamp)
////
////                getActivityData(type = "sleep", frequency = "day", timeStamp = timeStamp)
////                getActivityData(type = "sleep", frequency = "week", timeStamp = timeStamp)
//            }
//        } else {
//            Timber.d("Permission Not present")
//            updateButtonState(HealthConnectConnectionState.INSTALLED)
//        }
//    }
//
//    // 1. For the dashboard graph.
////           Today's steps and sleep data.
//    private fun getDailyStepAndSleepData() {
//        scope.launch {
//            try {
//                val resultString =
//                    graphDataOperationsHelper.getTodayStepsAndSleepData(getHealthConnectClient())
//
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//
//    //2.For all the details graph.
//    private fun getActivityData(type: String?, frequency: String?, timeStamp: Long) {
//        if (type != null && frequency != null) {
//            when (type) {
//                "steps" -> {
//                    when (frequency) {
//                        "day" -> {
//                            scope.launch {
//                                try {
//                                    val resultString =
//                                        graphDataOperationsHelper.getDailyStepsData(timeStamp)
//
//
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//
//
//                        }
//
//                        "week" -> {
//                            scope.launch {
//                                try {
//                                    val resultString =
//                                        graphDataOperationsHelper.getWeeklyStepsData(timeStamp)
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//
//                        }
//
//                        "month" -> {
//                            scope.launch {
//                                try {
//
//                                    val resultString =
//                                        graphDataOperationsHelper.getMonthlyStepsData(timeStamp)
//
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//
//                        }
//                    }
//                }
//
//                "distance" -> {
//                    when (frequency) {
//                        "day" -> {
//                            scope.launch {
//                                try {
//                                    val resultString =
//                                        graphDataOperationsHelper.getDailyDistanceData(timeStamp)
//
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//
//                        }
//
//                        "week" -> {
//                            scope.launch {
//                                try {
//                                    val resultString =
//                                        graphDataOperationsHelper.getWeeklyDistanceData(timeStamp)
//
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//
//                        }
//
//                        "month" -> {
//                            scope.launch {
//                                try {
//
//                                    val resultString =
//                                        graphDataOperationsHelper.getMonthlyDistanceData(timeStamp)
//
//
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//
//                        }
//                    }
//                }
//
//                "calories" -> {
//                    when (frequency) {
//                        "day" -> {
//
//                            scope.launch {
//                                try {
//
//                                    val resultString =
//                                        graphDataOperationsHelper.getDailyCalorieData(timeStamp)
//
//
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//                        }
//
//                        "week" -> {
//                            scope.launch {
//                                try {
//                                    val resultString =
//                                        graphDataOperationsHelper.getWeeklyCalorieData(timeStamp)
//
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//                        }
//
//                        "month" -> {
//                            scope.launch {
//                                try {
//
//                                    val resultString =
//                                        graphDataOperationsHelper.getMonthlyCalorieData(timeStamp)
//
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//
//                        }
//                    }
//                }
//
//                "sleep" -> {
//                    when (frequency) {
//                        "day" -> {
//                            scope.launch {
//                                try {
//
//                                    val resultString =
//                                        graphDataOperationsHelper.getDailySleepData(timeStamp)
//
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//
//                        }
//
//                        "week" -> {
//                            scope.launch {
//                                try {
//
//                                    val resultString =
//                                        graphDataOperationsHelper.getWeeklySleepData(timeStamp)
//
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//
//                        }
//                    }
//                }
//            }
//        }
//
//    }
//
//
//    suspend fun getDailySyncData(timeStamp: Long): DailyStepSyncRequest {
//
//        val dailySyncManager = DailySyncManager(getHealthConnectClient())
//        val dailySyncData: List<DailySyncHealthMetric> =
//            dailySyncManager.getDailySyncData(timeStamp)
//        val requestBody = DailyStepSyncRequest(fitnessData = dailySyncData, platform = "ANDROID")
//
//        Timber.d("getDailySyncData: requestBody: ${Gson().toJson(requestBody)}")
//
//        return requestBody
//    }
//
//    suspend fun exhaustHealthConnectQueryLimitTest(timeStamp: Long) {
//        (1..1000).forEachIndexed { index, i ->
//            Timber.d("exhaustHealthConnectQueryLimitTest: index: $index")
//            getHourlySyncData(timeStamp)
//        }
//    }
//
//    suspend fun getHourlySyncData(timeStamp: Long): HourlyDataSyncRequest {
//
//        val hourlySyncManager = HourlySyncManager(getHealthConnectClient())
//
//        val hourlyRecords = hourlySyncManager.getHourlySyncData(hourlyLastSyncTimestamp = timeStamp)
//
//        val requestBody =
//            HourlyDataSyncRequest(bulkHealthData = hourlyRecords, platform = "ANDROID")
//
//
//        Timber.d("getHourlySyncData: requestBody: ${Gson().toJson(requestBody)}")
//
//        return requestBody
//    }
//
//
//}
///** aggregateActivityByBucketBasedOnDuration
// * steps total: 6602 ,distance total: 3784.0138429299027 meters ,calorie total: 1714.9387687454107 kcal ,startTime:2024-08-16T18:30:00Z ,endTime: 2024-08-17T18:30:00Z
// * steps total: 2153 ,distance total: 946.9942807499685 meters ,calorie total: 1544.0814161167207 kcal ,startTime:2024-08-17T18:30:00Z ,endTime: 2024-08-18T18:30:00Z
// * steps total: 2181 ,distance total: 1217.345914414582 meters ,calorie total: 1534.8714048548898 kcal ,startTime:2024-08-18T18:30:00Z ,endTime: 2024-08-19T18:30:00Z
// * steps total: 1028 ,distance total: 414.3971366935903 meters ,calorie total: 1477.0066373640257 kcal ,startTime:2024-08-19T18:30:00Z ,endTime: 2024-08-20T18:30:00Z
// * steps total: 7444 ,distance total: 6496.074988999614 meters ,calorie total: 1941.2634706186748 kcal ,startTime:2024-08-20T18:30:00Z ,endTime: 2024-08-21T18:29:59.999Z
// */
//
///** aggregateActivityByBucketBasedOnPeriod
// * steps total: 3771 ,distance total: 2075.2305739754383 meters ,calorie total: 1613.5464919679039 kcal ,startTime: 2024-08-16T18:30 ,endTime: 2024-08-17T18:30
// * steps total: 6607 ,distance total: 3612.4783093918018 meters ,calorie total: 1715.328791728457 kcal ,startTime: 2024-08-17T18:30 ,endTime: 2024-08-18T18:30
// * steps total: 2150 ,distance total: 1203.0787563831184 meters ,calorie total: 1532.6618118549654 kcal ,startTime: 2024-08-18T18:30 ,endTime: 2024-08-19T18:30
// * steps total: 549 ,distance total: 234.74553567468138 meters ,calorie total: 1474.9617246142407 kcal ,startTime: 2024-08-19T18:30 ,endTime: 2024-08-20T18:30
// * steps total: 7916 ,distance total: 6671.767744403136 meters ,calorie total: 1945.517986367616 kcal ,startTime: 2024-08-20T18:30 ,endTime: 2024-08-21T18:29:59.999
// */
//
//
//
//
