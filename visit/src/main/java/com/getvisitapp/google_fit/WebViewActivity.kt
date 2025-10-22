//package com.getvisitapp.google_fit
//
//import android.Manifest
//import android.content.Intent
//import android.graphics.Bitmap
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.view.ViewGroup
//import android.widget.RelativeLayout
//import androidx.activity.result.contract.ActivityResultContract
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.health.connect.client.PermissionController
//import com.getvisitapp.google_fit.data.GoogleFitStatusListener
//import com.getvisitapp.google_fit.data.VisitStepSyncHelper
//import com.getvisitapp.google_fit.data.WebAppInterface
//import com.getvisitapp.google_fit.healthConnect.OnActivityResultImplementation
//import com.getvisitapp.google_fit.healthConnect.activity.HealthConnectUtil
//import com.getvisitapp.google_fit.healthConnect.contants.Contants
//import com.getvisitapp.google_fit.healthConnect.enums.HealthConnectConnectionState
//import im.delight.android.webview.AdvancedWebView
//import kotlinx.coroutines.launch
//import timber.log.Timber
//
//
////This file is used only for testing purpose.
////Comment this entire file and build the .aar file.
//
////Consider this file similar to the module file that we have (VisitFitnessModule)
//class WebViewActivity : AppCompatActivity(), AdvancedWebView.Listener, GoogleFitStatusListener,
//    HealthConnectListener {
//
//
//    lateinit var mWebView: AdvancedWebView
//
//    private val LOCATION_PERMISSION_REQUEST_CODE = 787
//
//    lateinit var visitStepSyncHelper: VisitStepSyncHelper
//
//    lateinit var healthConnectUtil: HealthConnectUtil
//
//    var syncDataWithServer = false
//
//    lateinit var webAppInterface: WebAppInterface
//
//
//    private val requestPermissionActivityContract: ActivityResultContract<Set<String>, Set<String>> =
//        PermissionController.createRequestPermissionResultContract()
//
//    var onActivityResultImplementation: OnActivityResultImplementation<Set<String>, Set<String>>? =
//        null
//
//
//    val requestPermissions =
//        registerForActivityResult(requestPermissionActivityContract) { granted: Set<String> ->
//            onActivityResultImplementation?.execute(granted)
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        //Initializing it programmatically, as we don't want any layout file, because when building the .aar file,
//        //the layout file will also get shipped which react native won't accept it.
//
//        val relativeLayout = RelativeLayout(this)
//        val lp = RelativeLayout.LayoutParams(
//            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
//        )
//        relativeLayout.layoutParams = lp
//
//        mWebView = AdvancedWebView(this)
//        mWebView.layoutParams = ViewGroup.LayoutParams(
//            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
//        )
//
//        relativeLayout.addView(mWebView)
//
//        setContentView(relativeLayout)
//
//
//
//        mWebView.setListener(this, this);
//        mWebView.setMixedContentAllowed(false);
//        mWebView.settings.javaScriptEnabled = true
//        webAppInterface = WebAppInterface(this)
//
//        val magicLink =
//            "https://star-health.getvisitapp.net/?mluib7c=%5B%7B%22policyNumber%22:%2211240005416800%22,%22policyId%22:1480,%22userId%22:1004565,%22magicUserId%22:41213,%22userMagicCode%22:%22S7fFT4ci%22,%22policyName%22:%22Young%20Star%20Insurance%20Policy%22,%22policyStartDate%22:%222024-08-02T18:30:00.000Z%22,%22policyEndDate%22:%222025-07-03T18:29:59.000Z%22,%22isHospiCash%22:false,%22isAlreadyOnboarded%22:true%7D,%7B%22policyNumber%22:%2211240005434900%22,%22policyId%22:1481,%22userId%22:1004565,%22magicUserId%22:41213,%22userMagicCode%22:%22S7fFT4ci%22,%22policyName%22:%22Star%20Family%20Health%20Optima%22,%22policyStartDate%22:%222024-12-02T18:30:00.000Z%22,%22policyEndDate%22:%222025-11-03T18:29:59.000Z%22,%22isHospiCash%22:false,%22isAlreadyOnboarded%22:true%7D,%7B%22policyNumber%22:%2211240006100000%22,%22policyId%22:1503,%22userId%22:1004565,%22magicUserId%22:41213,%22userMagicCode%22:%22S7fFT4ci%22,%22policyName%22:%22Star%20Out%20Patient%20Care%20Insurance%20Policy%22,%22policyStartDate%22:%222024-03-28T18:30:00.000Z%22,%22policyEndDate%22:%222025-03-28T18:29:59.000Z%22,%22isHospiCash%22:false,%22isAlreadyOnboarded%22:true%7D,%7B%22policyNumber%22:%2211240006101300%22,%22policyId%22:1505,%22userId%22:1004565,%22magicUserId%22:41213,%22userMagicCode%22:%22S7fFT4ci%22,%22policyName%22:%22Star%20Women%20Care%20Insurance%20-%202021%22,%22policyStartDate%22:%222024-03-28T18:30:00.000Z%22,%22policyEndDate%22:%222025-03-28T18:29:59.000Z%22,%22isHospiCash%22:false,%22isAlreadyOnboarded%22:true%7D,%7B%22policyNumber%22:%2211250007707700%22,%22policyId%22:1563,%22userId%22:1004565,%22magicUserId%22:41213,%22userMagicCode%22:%22S7fFT4ci%22,%22policyName%22:%22Young%20Star%20Insurance%20Policy%22,%22policyStartDate%22:%222024-06-25T18:30:00.000Z%22,%22policyEndDate%22:%222024-12-22T18:29:59.000Z%22,%22isHospiCash%22:false,%22isAlreadyOnboarded%22:true%7D,%7B%22policyNumber%22:%2218250007708000%22,%22policyId%22:1564,%22userId%22:2083479,%22magicUserId%22:41652,%22userMagicCode%22:%22yS1PVPF6%22,%22policyName%22:%22Star%20Travel%20Protect%20Insurance%20Policy%22,%22policyStartDate%22:%222024-06-25T18:30:00.000Z%22,%22policyEndDate%22:%222024-12-22T18:29:59.000Z%22,%22isHospiCash%22:false,%22isAlreadyOnboarded%22:true%7D%5D"
//
//        ActivityCompat.requestPermissions(
//            this, arrayOf<String>(
//                Manifest.permission.POST_NOTIFICATIONS
//            ), 101
//        )
//
//
//
//
//
//        visitStepSyncHelper = VisitStepSyncHelper(this)
//        healthConnectUtil = HealthConnectUtil(this, this)
//
//
//
//
//
//
//        mWebView.addJavascriptInterface(webAppInterface, "Android")
//        mWebView.loadUrl(magicLink)
//
//        healthConnectUtil.initialize()
//
//
//        val activity = this as WebViewActivity
//
//        activity.onActivityResultImplementation =
//            object : OnActivityResultImplementation<Set<String>, Set<String>> {
//                override fun execute(granted: Set<String>): Set<String> {
//                    Timber.d("onActivityResultImplementation execute: result: $granted")
//
//                    if (granted.containsAll(healthConnectUtil.PERMISSIONS)) {
//                        Contants.previouslyRevoked = false
//
//                        Timber.d("Permissions successfully granted")
//                        healthConnectUtil.scope.launch {
//                            healthConnectUtil.checkPermissionsAndRun(afterRequestingPermission = true)
//                        }
//
//                    } else {
//                        Timber.d(" Lack of required permissions")
//
//                        //Currently the Health Connect SDK, only asks for the remaining permission was the NOT granted in the first time, and when it return,
//                        //it also send the granted permission (and not the permission that was previously granted), so the control flow comes inside the else statement.
//                        //So we need to check for permission again.
//                        healthConnectUtil.scope.launch {
//                            healthConnectUtil.checkPermissionsAndRun(afterRequestingPermission = true)
//                        }
//                    }
//                    return granted
//                }
//            }
//
//
//    }
//
//    override fun onResume() {
//        super.onResume()
//        mWebView.onResume();
//        getHealthConnectStatus();
//    }
//
//    override fun onPause() {
//        mWebView.onPause();
//        super.onPause()
//    }
//
//    override fun onDestroy() {
//        mWebView.onDestroy();
//        super.onDestroy()
//    }
//
//    override fun onPageStarted(url: String?, favicon: Bitmap?) {
//        Timber.d("mytag: onPageStarted: $url")
//    }
//
//    override fun onPageFinished(url: String?) {
//        Timber.d("mytag: onPageFinished: $url")
//    }
//
//    override fun onPageError(errorCode: Int, description: String?, failingUrl: String?) {}
//
//    override fun onDownloadRequested(
//        url: String?,
//        suggestedFilename: String?,
//        mimeType: String?,
//        contentLength: Long,
//        contentDisposition: String?,
//        userAgent: String?
//    ) {
//    }
//
//    override fun onExternalPageRequest(url: String?) {}
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//
//        super.onActivityResult(requestCode, resultCode, data)
//        mWebView.onActivityResult(requestCode, resultCode, intent);
//
//    }
//
//    override fun onBackPressed() {
//        if (!mWebView.onBackPressed()) {
//            return; }
//        super.onBackPressed()
//
//    }
//
//    override fun askForPermissions() {
//        if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.CONNECTED) {
//            healthConnectUtil.getVisitDashboardGraph()
//        } else {
//            healthConnectUtil.requestPermission()
//        }
//    }
//
//    override fun requestActivityData(type: String?, frequency: String?, timestamp: Long) {
//        Timber.d("mytag: requestActivityData() called.")
//        if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.CONNECTED) {
//            //Health Connect Implementation
//            healthConnectUtil.getActivityData(type, frequency, timestamp)
//        } else {
//            Timber.d("mytag: permission not available healthConnectConnectionState: ${healthConnectUtil.healthConnectConnectionState}")
//            healthConnectUtil.requestPermission()
//        }
//    }
//
//    override fun syncDataWithServer(
//        baseUrl: String?, authToken: String?, googleFitLastSync: Long, gfHourlyLastSync: Long
//    ) {
//        Timber.d("mytag: baseUrl: $baseUrl")
//        if (!syncDataWithServer) {
//            Timber.d("mytag: syncDataWithServer() called")
//
//            visitStepSyncHelper.sendDataToVisitServer(
//                healthConnectUtil, googleFitLastSync, gfHourlyLastSync, "$baseUrl/", authToken!!
//            )
//
//            syncDataWithServer = true
//        }
//    }
//
//    override fun getHealthConnectStatus() {
//        Timber.d("mytag: ${healthConnectUtil.healthConnectConnectionState}")
//        healthConnectUtil.scope.launch {
//            val status: HealthConnectConnectionState = healthConnectUtil.checkAvailability()
//
//            if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.NOT_SUPPORTED) {
//                Handler(Looper.getMainLooper()).post {
//                    val finalString = "window.healthConnectNotSupported()"
//                    mWebView.evaluateJavascript(
//                        finalString, null
//                    )
//                }
//            } else if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.NOT_INSTALLED) {
//                Handler(Looper.getMainLooper()).post {
//                    val finalString = "window.healthConnectNotInstall()"
//                    mWebView.evaluateJavascript(
//                        finalString, null
//                    )
//
//                    mWebView.evaluateJavascript(
//                        "window.updateFitnessPermissions(false,0,0)", null
//                    )
//                }
//            } else if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.INSTALLED) {
//                Handler(Looper.getMainLooper()).post {
//                    val finalString = "window.healthConnectAvailable()"
//                    mWebView.evaluateJavascript(
//                        finalString, null
//                    )
//
//                    mWebView.evaluateJavascript(
//                        "window.updateFitnessPermissions(false,0,0)", null
//                    )
//                }
//            } else if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.CONNECTED) {
//                healthConnectUtil.getVisitDashboardGraph()
//            }
//        }
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when (requestCode) {
//
//            LOCATION_PERMISSION_REQUEST_CODE -> {}
//        }
//    }
//
//    override fun updateHealthConnectConnectionStatus(
//        status: HealthConnectConnectionState, text: String
//    ) {
//        Timber.d("updateHealthConnectConnectionStatus: $status")
//
//        when (status) {
//            HealthConnectConnectionState.CONNECTED -> {
//                healthConnectUtil.getVisitDashboardGraph()
//            }
//
//            HealthConnectConnectionState.NOT_SUPPORTED -> {
//
//            }
//
//            HealthConnectConnectionState.NOT_INSTALLED -> {
//
//            }
//
//            HealthConnectConnectionState.INSTALLED -> {
//                //don't do anything here for the webView.
//            }
//
//            HealthConnectConnectionState.NONE -> {
//
//            }
//        }
//    }
//
//    //This handles both dashboard graph and detailed graph page.
//    override fun loadVisitWebViewGraphData(webUrl: String) {
//        Timber.d("loadVisitWebViewGraphData: webUrl: $webUrl")
//
//        Handler(Looper.getMainLooper()).post {
//            mWebView.evaluateJavascript(
//                webUrl, null
//            )
//        }
//
//    }
//
//    override fun userDeniedHealthConnectPermission() {
//        //write the promise here.
//
//        Timber.d("mytag : userDeniedHealthConnectPermission")
//    }
//
//    override fun userAcceptedHealthConnectPermission() {
//        //write the promise here.
//
//        Timber.d("mytag : userAcceptedHealthConnectPermission")
//
//    }
//
//    override fun requestPermission() {
//        try {
//            requestPermissions.launch(healthConnectUtil.PERMISSIONS)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    override fun logHealthConnectError(throwable: Throwable) {
//        Timber.d("mytag: logHealthConnectError + ${throwable.message}")
//    }
//
//
//}
//
