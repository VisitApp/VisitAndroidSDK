package com.getvisitapp.google_fit.activity

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Browser
import android.text.Html
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.getvisitapp.google_fit.R
import com.getvisitapp.google_fit.connectivity.ConnectivityObserver
import com.getvisitapp.google_fit.connectivity.NetworkConnectivityObserver
import com.getvisitapp.google_fit.data.GoogleFitUtil
import com.getvisitapp.google_fit.data.SharedPrefUtil
import com.getvisitapp.google_fit.data.VisitStepSyncHelper
import com.getvisitapp.google_fit.databinding.SdkWebView
import com.getvisitapp.google_fit.event.ClosePWAEvent
import com.getvisitapp.google_fit.event.MessageEvent
import com.getvisitapp.google_fit.event.VisitEventType
import com.getvisitapp.google_fit.event.VisitEventType.VisitCallBack
import com.getvisitapp.google_fit.util.Constants.DEFAULT_CLIENT_ID
import com.getvisitapp.google_fit.util.Constants.IS_DEBUG
import com.getvisitapp.google_fit.util.Constants.TATA_AIG_AUTH_TOKEN
import com.getvisitapp.google_fit.util.Constants.TATA_AIG_BASE_URL
import com.getvisitapp.google_fit.util.Constants.WEB_URL
import com.getvisitapp.google_fit.util.GoogleFitAccessChecker
import com.getvisitapp.google_fit.util.LocationTrackerUtil
import com.getvisitapp.google_fit.util.PdfDownloader
import com.getvisitapp.google_fit.view.GoogleFitStatusListener
import com.getvisitapp.google_fit.view.VideoCallListener
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONException
import org.json.JSONObject
import tvi.webrtc.ContextUtils
import java.util.*


/**
 *
 *
1. Fitbit success URL:
tataaig://visitsdkactivity&message=success&fitbit=true

2.
These events are there present in the sdk:

Download HRA report clicked (done)
Download HRA report failed (not possible from sdk)
Google fit clicked (done)
Google fit connection failed (done)
Fitbit clicked (done)
Fitbit connection failed (done)
Sync steps and calories api called (done)
Sync steps and calories api failed (done)

 */
@Keep
class SdkWebviewActivity : AppCompatActivity(), VideoCallListener, GoogleFitStatusListener {

    var TAG = "mytag"

    lateinit var binding: SdkWebView


    val ACTIVITY_RECOGNITION_REQUEST_CODE = 490
    val LOCATION_PERMISSION_REQUEST_CODE = 787
    val REQUEST_CODE_FILE_PICKER = 51426

    lateinit var googleFitUtil: GoogleFitUtil

    var isDebug: Boolean = false
    lateinit var magicLink: String
    lateinit var default_web_client_id: String


    var dailyDataSynced = false
    var syncDataWithServer = false

    private lateinit var googleFitStepChecker: GoogleFitAccessChecker
    private lateinit var tataAIG_base_url: String
    private lateinit var tataAIG_auth_token: String

    lateinit var sharedPrefUtil: SharedPrefUtil
    lateinit var pdfDownloader: PdfDownloader


    var visitApiBaseUrl: String? = null
    var authtoken: String? = null
    var googleFitLastSync: Long = 0L
    var gfHourlyLastSync = 0L
    var fitbitLastSyncTimeStamp = 0L
    var memberId: String? = null

    var redirectUserToGoogleFitStatusPage: Boolean =
        false //this flag acts a check with which we should redirect user to google connected successfully page or not.

    lateinit var locationTrackerUtil: LocationTrackerUtil
    lateinit var visitSyncStepSyncHelper: VisitStepSyncHelper
    private val AUTHORITY_SUFFIX = ".googlefitsdk.fileprovider"

    lateinit var connectivityObserver: ConnectivityObserver
    var mFileUploadCallbackSecond: ValueCallback<Array<Uri>>? = null


    var webChromeClient: WebChromeClient = MyChrome()
    var webViewClient: WebViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            Log.d(TAG, "onPageStarted: $url")
            binding.progressBar.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            Log.d(TAG, "onPageFinished: $url")
            binding.progressBar.visibility = View.GONE

        }


        override fun onReceivedError(
            view: WebView?, request: WebResourceRequest?, error: WebResourceError?
        ) {
//            Log.d(TAG, "errorCode: $errorCode description: $description failingUrl: $failingUrl")
            binding.progressBar.visibility = View.GONE
            Log.d("mytag", "onReceivedError")
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            Log.d("mytag", "shouldOverrideUrlLoading")

            url?.let {
                binding.webview.loadUrl(url)
            }
            return true;

        }
    }


    companion object {
        fun getIntent(
            context: Context,
            isDebug: Boolean,
            magicLink: String,
            tataAIG_base_url: String,
            tataAIG_auth_token: String,
            default_web_client_id: String
        ): Intent {
            val intent = Intent(context, SdkWebviewActivity::class.java);
            intent.putExtra(IS_DEBUG, isDebug)
            intent.putExtra(WEB_URL, magicLink)
            intent.putExtra(TATA_AIG_BASE_URL, tataAIG_base_url)
            intent.putExtra(TATA_AIG_AUTH_TOKEN, tataAIG_auth_token)
            intent.putExtra(DEFAULT_CLIENT_ID, default_web_client_id)
            return intent
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sdk)
        binding.progressBar.setVisibility(View.GONE)
        magicLink = intent.extras!!.getString(WEB_URL)!!
        isDebug = intent.extras!!.getBoolean(IS_DEBUG);
        tataAIG_base_url = intent.extras!!.getString(TATA_AIG_BASE_URL)!!
        tataAIG_auth_token = intent.extras!!.getString(TATA_AIG_AUTH_TOKEN)!!
        default_web_client_id = intent.extras!!.getString(DEFAULT_CLIENT_ID)!!
        visitSyncStepSyncHelper = VisitStepSyncHelper(this, default_web_client_id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webview, true)
        binding.webview.settings.setGeolocationEnabled(true)
        binding.webview.settings.setDomStorageEnabled(true);
        binding.webview.settings.setAllowFileAccessFromFileURLs(true)
        binding.webview.settings.setAllowUniversalAccessFromFileURLs(true)


        binding.webview.webChromeClient = webChromeClient
        binding.webview.webViewClient = webViewClient

        binding.webview.setDownloadListener(object : DownloadListener {
            override fun onDownloadStart(
                url: String?,
                userAgent: String?,
                contentDisposition: String?,
                mimetype: String?,
                contentLength: Long
            ) {

                Log.d("mytag", "onDownloadRequested() url:$url, mimeType:$mimetype");

                url?.let {
                    pdfDownloader.downloadPdfFile(fileDir = filesDir,
                        pdfUrl = url,
                        authorization = authtoken!!,
                        onDownloadComplete = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                        applicationContext,
                                        applicationContext.packageName + AUTHORITY_SUFFIX,
                                        it
                                    )
                                )
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                type = "application/pdf"
                            }
                            val sendIntent = Intent.createChooser(shareIntent, null)
                            startActivity(sendIntent)
                        },
                        onDownloadFailed = {
                            Log.d(
                                TAG, "onDownloadRequested() download failed, opening it in chrome"
                            )
                            try {
                                val uri = Uri.parse(url)
                                startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        })
                }

            }
        })

        binding.webview.loadUrl(magicLink)


        googleFitUtil = GoogleFitUtil(this, this, default_web_client_id)
        binding.webview.addJavascriptInterface(googleFitUtil.webAppInterface, "Android")
        googleFitUtil.init()

        googleFitStepChecker = GoogleFitAccessChecker(this)
        sharedPrefUtil = SharedPrefUtil(this)
        pdfDownloader = PdfDownloader()
        locationTrackerUtil = LocationTrackerUtil(this)

        connectivityObserver = NetworkConnectivityObserver(this)

        connectivityObserver.observe().onEach { networkStatus ->
            when (networkStatus) {
                ConnectivityObserver.Status.Available -> {
                    binding.noNetworkConnectionLayout.visibility = View.GONE
                }

                ConnectivityObserver.Status.Unavailable -> {
                    binding.noNetworkConnectionLayout.visibility = View.VISIBLE
                }

                ConnectivityObserver.Status.Losing -> {
                    binding.noNetworkConnectionLayout.visibility = View.VISIBLE
                }

                ConnectivityObserver.Status.Lost -> {
                    binding.noNetworkConnectionLayout.visibility = View.VISIBLE
                }
            }
            Log.d("mytag", "network status: $networkStatus")
        }.launchIn(lifecycleScope)

        ViewCompat.setOnApplyWindowInsetsListener(binding.parentLayout) { view, windowInsets ->

            val statusBarInsets =
                windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())

            val navigationBarInsets =
                windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.webview.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = statusBarInsets.top
                bottomMargin = navigationBarInsets.bottom
            }

            binding.noNetworkConnectionLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = statusBarInsets.top
            }

            WindowInsetsCompat.CONSUMED
        }


    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent!!.data != null) {
            val uri = intent.data

            Log.d(
                TAG, "onNewIntent: Getting URI: $uri"
            )

            if (uri!!.queryParameterNames.contains("fitbit")) {
                val message = uri!!.getQueryParameter("message")

                Handler(Looper.getMainLooper()).postDelayed({ //Do something here
                    if (message != null && message.equals("success", ignoreCase = true)) {
                        runOnUiThread {

                            Log.d("mytag", "window.fitbitConnectSuccessfully(true)")

                            binding.webview.evaluateJavascript(
                                "window.fitbitConnectSuccessfully(true)", null
                            )
                            EventBus.getDefault()
                                .post(MessageEvent(VisitEventType.FitnessPermissionGranted(false)))
                            Toast.makeText(
                                applicationContext, "Fitbit is connected", Toast.LENGTH_LONG
                            ).show()
                            sharedPrefUtil.setFitBitConnectedStatus(true)
                            visitSyncStepSyncHelper.syncFitbitSteps(
                                tataAIG_base_url, tataAIG_auth_token
                            )

                        }

                    } else if (message != null && message.equals(
                            "failed", ignoreCase = true
                        )
                    ) {
                        Toast.makeText(
                            applicationContext,
                            "Failed to connect Fitbit device. Please retry or contact support",
                            Toast.LENGTH_LONG
                        ).show()

                        EventBus.getDefault().post(
                            MessageEvent(
                                VisitEventType.VisitCallBack(
                                    "Fitbit connection failed",
                                    failureReason = "Failed to connect to Fitbit."
                                )
                            )
                        )

                    } else if (message != null && message.equals(
                            "accessDenied", ignoreCase = true
                        )
                    ) {
                        Toast.makeText(
                            applicationContext,
                            "You have denied access to connect to Fitbit",
                            Toast.LENGTH_LONG
                        ).show()

                        EventBus.getDefault().post(
                            MessageEvent(
                                VisitEventType.VisitCallBack(
                                    "Fitbit connection failed",
                                    failureReason = "Failed to connect Fitbit. Access Denied"
                                )
                            )
                        )
                    }
                }, 1000)
            } else if (uri.queryParameterNames.contains("feedback")) {

                Log.d("mytag", "opened the app from feedback")

            }
        } else {
            Log.d(
                TAG, "onNewIntent: getData is null"
            )
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webview.onResume();
    }

    override fun onPause() {
        binding.webview.onPause();
        super.onPause()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Log.d(
            TAG, "onActivityResult called. requestCode: $requestCode resultCode: $resultCode"
        )

        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == 4097 || requestCode == 1900) {

            googleFitUtil.onActivityResult(requestCode, resultCode, intent)
            if (resultCode == 0) {
                if (requestCode == 1900) {
                    EventBus.getDefault()
                        .post(MessageEvent(VisitEventType.FitnessPermissionError("Google SignIn Cancelled")))

                    EventBus.getDefault().post(
                        MessageEvent(
                            VisitEventType.VisitCallBack(
                                "Google fit connection failed",
                                failureReason = "Google SignIn Cancelled"
                            )
                        )
                    )
                }
            }
        } else if (requestCode == 1000 && resultCode == RESULT_OK) {
            Log.d("mytag", "resultCode: $requestCode")

            binding.webview.webChromeClient = webChromeClient
            binding.webview.webViewClient = webViewClient


        } else if (requestCode == REQUEST_CODE_FILE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                var dataUris: Array<Uri>? = null

                try {
                    if (intent!!.dataString != null) {
                        dataUris = arrayOf(Uri.parse(intent.dataString))
                    } else {
                        if (intent.clipData != null) {
                            val count = intent.clipData!!.itemCount
                            dataUris = Array(count) { index ->
                                intent.clipData!!.getItemAt(index).uri
                            }
                        }
                    }
                } catch (ignored: java.lang.Exception) {

                }

                mFileUploadCallbackSecond!!.onReceiveValue(dataUris)
                mFileUploadCallbackSecond = null
            } else {
                if (mFileUploadCallbackSecond != null) {
                    mFileUploadCallbackSecond!!.onReceiveValue(null)
                    mFileUploadCallbackSecond = null
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun connectToGoogleFit(redirectUserToGoogleFitStatusPage: Boolean) {
        this.redirectUserToGoogleFitStatusPage = redirectUserToGoogleFitStatusPage

        //if we are redirecting user to google fit status page, then it means it is a fresh connection request.
        //so here i am resetting the flag.


        Log.d(
            TAG,
            "redirectUserToGoogleFitStatusPage: $redirectUserToGoogleFitStatusPage, googleFitStepChecker.checkGoogleFitAccess() :  " + googleFitStepChecker.checkGoogleFitAccess()
        )

        if (!redirectUserToGoogleFitStatusPage && !googleFitStepChecker.checkGoogleFitAccess()) {
            Log.d(TAG, "window.googleFitStatus(false) called")

            runOnUiThread {
                binding.webview.evaluateJavascript(
                    "window.googleFitStatus(false)", null
                )
            }
            return
        }

        EventBus.getDefault().post(MessageEvent(VisitEventType.AskForFitnessPermission))
        EventBus.getDefault().post(
            MessageEvent(
                VisitEventType.VisitCallBack(
                    message = "Google fit clicked", failureReason = null
                )
            )
        )

        if (dailyDataSynced) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    ACTIVITY_RECOGNITION_REQUEST_CODE
                )
            } else {
                googleFitUtil.askForGoogleFitPermission()
            }
        } else {
            googleFitUtil.askForGoogleFitPermission()
        }
    }

    override fun disconnectFromGoogleFit() {

        googleFitStepChecker.revokeGoogleFitPermission(default_web_client_id)
        visitSyncStepSyncHelper.revokeFitbitAccess()

        EventBus.getDefault().post(MessageEvent(VisitEventType.FitnessPermissionRevoked(true)))

    }

    override fun connectToFitbit(url: String, authToken: String) {


        EventBus.getDefault().post(
            MessageEvent(
                VisitEventType.VisitCallBack(
                    "Fitbit clicked", failureReason = null
                )
            )
        )

        Log.d(TAG, "connectToFitbit: $url, authToken: $authToken")

        runOnUiThread {
            val browserIntent = Intent(
                Intent.ACTION_VIEW, Uri.parse(url)
            )

            val bundle = Bundle()
            bundle.putString(
                "Authorization", authToken
            )
            browserIntent.putExtra(Browser.EXTRA_HEADERS, bundle)

            startActivity(browserIntent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onFitnessPermissionGranted() {

        googleFitUtil.fetchDataFromFit()
        Log.d(
            TAG,
            "onFitnessPermissionGranted() called , redirectUserToGoogleFitPage: $redirectUserToGoogleFitStatusPage"
        )

        //here i am not calling "window.googleFitnessConnectedSuccessfully(true)" because
        // i don't want to user to redirect to separate page,
        if (redirectUserToGoogleFitStatusPage) {
            Log.d(TAG, "window.googleFitnessConnectedSuccessfully() called")

            runOnUiThread {
                binding.webview.evaluateJavascript(
                    "window.googleFitnessConnectedSuccessfully(true)", null
                )
            }
        }


        //manually calling sync steps here because we are not getting sync step event after the google fit is connected
        if (visitApiBaseUrl != null && authtoken != null && googleFitLastSync != 0L && gfHourlyLastSync != 0L && memberId != null) {
            runOnUiThread {
                googleFitUtil.sendDataToServer(
                    visitApiBaseUrl + "/",
                    authtoken,
                    googleFitLastSync,
                    gfHourlyLastSync,
                    memberId,
                    tataAIG_base_url,
                    tataAIG_auth_token
                )
                syncDataWithServer = true
            }
        }

        EventBus.getDefault().post(MessageEvent(VisitEventType.FitnessPermissionGranted(true)))


    }

    override fun loadWebUrl(urlString: String?) {
        Log.d("mytag", "daily Fitness Data url:$urlString")
        if (urlString != null) {
            binding.webview.loadUrl(urlString)
        }
    }

    override fun requestActivityData(type: String?, frequency: String?, timestamp: Long) {
        Log.d(TAG, "requestActivityData() called.")

        EventBus.getDefault().post(
            MessageEvent(
                VisitEventType.RequestHealthDataForDetailedGraph(
                    type, frequency, timestamp
                )
            )
        )

        runOnUiThread(Runnable {
            if (type != null && frequency != null) {
                googleFitUtil.getActivityData(type, frequency, timestamp)
            }
        })
    }

    override fun loadGraphDataUrl(url: String?) {
        if (url != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                binding.webview.evaluateJavascript(
                    url, null
                )
            }
        }
        dailyDataSynced = true
    }

    override fun updateApiBaseUrlV3(
        visitApiBaseUrl: String?,
        authtoken: String?,
        googleFitLastSync: Long,
        gfHourlyLastSync: Long,
        memberId: String,
        isFitBitConnected: Boolean,
        fitbitLastSyncTimeStamp: Long,
        policyNumber: String
    ) {

        this.visitApiBaseUrl = visitApiBaseUrl
        this.authtoken = authtoken
        this.googleFitLastSync = googleFitLastSync
        this.gfHourlyLastSync = gfHourlyLastSync
        this.memberId = memberId
        this.fitbitLastSyncTimeStamp = fitbitLastSyncTimeStamp

        //For the first time, when the logs in the PWA, this will comes zero, so in that case just make it today's date
        if (this.googleFitLastSync == 0L) {
            this.googleFitLastSync = getTodayDateTimeStamp()
        }
        if (this.gfHourlyLastSync == 0L) {
            this.gfHourlyLastSync = getTodayDateTimeStamp()
        }

        sharedPrefUtil.setFitBitConnectedStatus(isFitBitConnected)
        sharedPrefUtil.setFitBitLastSyncTimeStamp(fitbitLastSyncTimeStamp)
        sharedPrefUtil.setPolicyNumber(policyNumber)

        Log.d("mytag", "apiBaseUrl: $visitApiBaseUrl $memberId")
        if (!syncDataWithServer) {
            Log.d(TAG, "syncDataWithServer() called")

            visitApiBaseUrl?.let {
                sharedPrefUtil.setVisitBaseUrl(visitApiBaseUrl + "/")
            }
            authtoken?.let {
                sharedPrefUtil.setVisitAuthToken(authtoken)
            }

            sharedPrefUtil.setTataAIGLastSyncTimeStamp(this.gfHourlyLastSync)// adding this here because there might be a case where the user just connected to google fit and
            // closed TATA AIG app immediately, in that case take this timestamp and start syncing from there end.

            sharedPrefUtil.setTATA_AIG_MemberId(memberId)

            if (sharedPrefUtil.getFitBitConnectionStatus()) {
                visitSyncStepSyncHelper.syncFitbitSteps(tataAIG_base_url, tataAIG_auth_token)
            } else {
                runOnUiThread(Runnable {
                    googleFitUtil.sendDataToServer(
                        visitApiBaseUrl + "/",
                        authtoken,
                        this.googleFitLastSync,
                        this.gfHourlyLastSync,
                        memberId,
                        tataAIG_base_url,
                        tataAIG_auth_token
                    )
                    syncDataWithServer = true
                })
            }
        }
    }

    override fun visitCredentialCallback(visitApiBaseUrl: String?, visitAuthToken: String?) {
        this.visitApiBaseUrl = visitApiBaseUrl + "/"
        this.authtoken = visitAuthToken


        visitApiBaseUrl?.let {
            sharedPrefUtil.setVisitBaseUrl(visitApiBaseUrl + "/")
        }
        visitAuthToken?.let {
            sharedPrefUtil.setVisitAuthToken(visitAuthToken)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun askForLocationPermission() {
        EventBus.getDefault().post(MessageEvent(VisitEventType.AskForLocationPermission))

        runOnUiThread {
            if (locationTrackerUtil.isLocationPermissionAllowed()) {
                if (locationTrackerUtil.isGPSEnabled()) {
                    runOnUiThread {
                        binding.webview.evaluateJavascript(
                            "window.checkTheGpsPermission(true)", null
                        )
                        Log.d("mytag", "window.checkTheGpsPermission(true) called")
                    }
                } else {
                    locationTrackerUtil.showGPS_NotEnabledDialog()
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }


    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onRestart() {
        super.onRestart()
        if (locationTrackerUtil.isLocationPermissionAllowed() && locationTrackerUtil.isGPSEnabled()) {
            runOnUiThread {
                binding.webview.evaluateJavascript(
                    "window.checkTheGpsPermission(true)", null
                )
                Log.d("mytag", "window.checkTheGpsPermission(true) called")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACTIVITY_RECOGNITION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val fitnessPermissionGranted =
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    if (fitnessPermissionGranted) {
                        Log.d(TAG, "ACTIVITY_RECOGNITION_REQUEST_CODE permission granted")
                        googleFitUtil.askForGoogleFitPermission()
                    }
                }

            }

            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val locationPermissionGranted =
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)

                    if (locationPermissionGranted) {
                        if (!locationTrackerUtil.isGPSEnabled()) {
                            locationTrackerUtil.showGPS_NotEnabledDialog()
                        }
                    } else {
                        locationTrackerUtil.showLocationPermissionDeniedAlertDialog()
                    }
                }
            }
        }
    }

    override fun startVideoCall(sessionId: Int, consultationId: Int, authToken: String?) {

        EventBus.getDefault()
            .post(MessageEvent(VisitEventType.StartVideoCall(sessionId, consultationId, authToken)))

        val intent = Intent(
            this, TwillioVideoCallActivity::class.java
        )
        intent.putExtra("isDebug", isDebug)
        intent.putExtra("sessionId", sessionId)
        intent.putExtra("consultationId", consultationId)
        intent.putExtra("authToken", authToken)
        startActivity(intent)
    }

    override fun hraCompleted() {
        EventBus.getDefault().post(MessageEvent(VisitEventType.HRA_Completed()))
    }

    override fun googleFitConnectedAndSavedInPWA() {
        EventBus.getDefault().post(MessageEvent(VisitEventType.GoogleFitConnectedAndSavedInPWA()))
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun inHraEndPage() {
        Log.d("mytag", "inHraEndPage() called")
        runOnUiThread {
            //check for google fit has access and call this event

            if (googleFitStepChecker.checkGoogleFitAccess()) {
                binding.webview.evaluateJavascript(
                    "window.showConnectToGoogleFit(false)", null
                )
                Log.d("mytag", "showConnectToGoogleFit(false) called")
            } else {
                binding.webview.evaluateJavascript(
                    "window.showConnectToGoogleFit(true)", null
                )
                Log.d("mytag", "showConnectToGoogleFit(true) called")
            }
        }
    }

    override fun hraQuestionAnswered(current: Int, total: Int) {
        runOnUiThread {
            EventBus.getDefault()
                .post(MessageEvent(VisitEventType.HRAQuestionAnswered(current, total)))
        }
    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun inFitSelectScreen() {
        runOnUiThread {
            //check for google fit has access and call this event

            if (googleFitStepChecker.checkGoogleFitAccess()) {
                binding.webview.evaluateJavascript(
                    "window.googleFitStatus(true)", null
                )
                Log.d("mytag", "googleFitStatus(true) called")
            } else {
                binding.webview.evaluateJavascript(
                    "window.googleFitStatus(false)", null
                )
                Log.d("mytag", "googleFitStatus(false) called")
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    Log.d(TAG, "webview.canGoBack(): ${binding.webview.canGoBack()}")

                    Log.d(TAG, binding.webview.url.toString())

                    if (binding.webview.canGoBack()) {
                        binding.webview.goBack()
                        if (binding.webview.url!!.endsWith("consultation/online/preview")) {
                            finish()
                        } else if (binding.webview.url!!.endsWith("/weight-management")) {
                            finish()
                        } else if (binding.webview.url!!.endsWith("op-benefits")) {
                            finish()
                        } else if (binding.webview.url!!.endsWith("/home/rewards")) {
                            finish()
                        } else if (binding.webview.url!!.endsWith("/wellness-management")) {
                            finish()
                        } else if (binding.webview.url!!.endsWith("/health-data") || binding.webview.url!!.endsWith(
                                "/hra/question"
                            ) || binding.webview.url!!.contains("stay-active")
                        ) {
                            Log.d(TAG, "window.hardwareBackPressed() called")
                            runOnUiThread {
                                binding.webview.evaluateJavascript(
                                    "window.hardwareBackPressed()", null
                                ) // this is a workaround to close the PWA, when the user lands to the details graph page directly,
                                // so this event acts as a gateway to check if the user has directly landed on the page, and close the PWA.
                            }
                        }
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }


    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }


    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(PWAEvent: ClosePWAEvent?) {
        Log.d("mytag", "onMessageEvent pwa close event triggered.")
        if (!this.isFinishing) {
            closePWA()
        }
    }

    fun closePWA() {
        finish()
        overridePendingTransition(R.anim.slide_from_top, R.anim.slide_in_top);
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        super.onDestroy()
    }

    fun getTodayDateTimeStamp(): Long {
        val startCalendar: Calendar = Calendar.getInstance()
        startCalendar.setTimeInMillis(System.currentTimeMillis())

        //doing to remove the hours passed in the today's date.

        //doing to remove the hours passed in the today's date.
        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startCalendar.set(Calendar.MINUTE, 0)
        startCalendar.set(Calendar.SECOND, 0)

        return startCalendar.getTimeInMillis()
    }


    override fun downloadHraLink(url: String, toShare: Boolean) {
        Log.d("mytag", "downloadHraLink() link:$url, toShare: $toShare")

        EventBus.getDefault().post(
            MessageEvent(
                VisitCallBack(
                    "Download HRA report clicked", null
                )
            )
        )


        pdfDownloader.downloadPdfFile(fileDir = filesDir, pdfUrl = url,
            authorization = authtoken!!,
            onDownloadComplete = {
                if (toShare) {
                    val uri = FileProvider.getUriForFile(
                        applicationContext,
                        applicationContext.packageName + AUTHORITY_SUFFIX,
                        it
                    )
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newUri(contentResolver, it.name, uri)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        type = "application/pdf"
                    }
                    val sendIntent = Intent.createChooser(shareIntent, null)
                    startActivity(sendIntent)
                } else {
                    try {
                        val uri = FileProvider.getUriForFile(
                            applicationContext,
                            applicationContext.packageName + AUTHORITY_SUFFIX,
                            it
                        )
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(uri, "application/pdf")
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }


            }, onDownloadFailed = {
                Log.d(TAG, "downloadHraLink() download failed, opening it in chrome")
                try {
                    val uri = Uri.parse(url)
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
    }

    override fun openDependentLink(link: String?) {
        Log.d("mytag", "openDependentLink link:$link")
        val customIntent = CustomTabsIntent.Builder()
        customIntent.setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        customIntent.setShowTitle(false)
        customIntent.setToolbarColor(
            ContextCompat.getColor(
                this, R.color.tata_aig_brand_color
            )
        )
        try {
            val uri = Uri.parse(link)
            openCustomTab(this, customIntent.build(), uri = uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun closeView(tataUser: Boolean) {
        if (tataUser) {
            finish()
            overridePendingTransition(R.anim.slide_from_top, R.anim.slide_in_top);
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun pendingHraUpdation() {
        runOnUiThread {
            binding.webview.evaluateJavascript(
                "window.updateHraToAig()", null
            )
        }
    }

    override fun hraInComplete(jsonObject: String?, isIncomplete: Boolean) {
        jsonObject?.let {
            sharedPrefUtil.setHRAIncompleteStatusRequest(jsonObject);
            sharedPrefUtil.setHRAIncompleteStatus(isIncomplete)
        }
    }


    fun openCustomTab(activity: Activity, customTabsIntent: CustomTabsIntent, uri: Uri) {
        val packageName = "com.android.chrome"
        customTabsIntent.intent.setPackage(packageName)
        customTabsIntent.launchUrl(activity, uri)
    }

    override fun consultationBooked() {
        runOnUiThread {
            EventBus.getDefault().post(MessageEvent(VisitEventType.ConsultationBooked))
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun loadDailyFitnessData(steps: Long, sleep: Long) {
        val finalString = "window.updateFitnessPermissions(true,$steps,$sleep)"

        Log.d(TAG, finalString)

        runOnUiThread {
            binding.webview.evaluateJavascript(
                finalString, null
            )
        }
    }

    override fun disconnectFromFitbit() {
        visitSyncStepSyncHelper.revokeFitbitAccess()
        EventBus.getDefault().post(MessageEvent(VisitEventType.FitnessPermissionRevoked(false)))

    }

    override fun couponRedeemed() {
        EventBus.getDefault().post(MessageEvent(VisitEventType.CouponRedeemed))
    }

    override fun internetErrorHandler(jsonObject: String?) {
        jsonObject?.let {
            val decodedObject: JSONObject? = decodeString(jsonObject)

            val errStatus = decodedObject?.getInt("errStatus")
            val error = decodedObject?.getString("error")

            EventBus.getDefault().post(MessageEvent(VisitEventType.NetworkError(errStatus, error)))
        }
    }

    override fun openLink(url: String?) {
        runOnUiThread {
            try {
                val uri = Uri.parse(url)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun openExternalLink(url: String?) {
        runOnUiThread {
            try {
                val feedBackActivity = FeedBackActivity.getIntent(this, url!!)
                startActivity(feedBackActivity)
                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun visitCallback(jsonObject: String?) {
        jsonObject?.let {
            val decodedObject: JSONObject = decodeString(jsonObject)

            val message: String = decodedObject.getString("message")
            val failureReason: String? =
                if (decodedObject.has("failureReason")) decodedObject.getString("failureReason") else null


            EventBus.getDefault()
                .post(MessageEvent(VisitEventType.VisitCallBack(message, failureReason)))
        }
    }

    override fun downloadPdf(link: String) {
        Log.d("mytag", "downloadPdf called(): $link")
        downloadHraLink(link, false)
    }

    override fun setAuthToken(authToken: String) {

        this.authtoken = authToken
    }


    inner class MyChrome internal constructor() : WebChromeClient() {
        private var mCustomView: View? = null
        private var mCustomViewCallback: CustomViewCallback? = null
        private var mOriginalOrientation = 0
        private var mOriginalSystemUiVisibility = 0
        override fun getDefaultVideoPoster(): Bitmap? {
            return if (mCustomView == null) {
                null
            } else BitmapFactory.decodeResource(
                ContextUtils.getApplicationContext().resources, 2130837573
            )
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            if (mFileUploadCallbackSecond != null) {
                mFileUploadCallbackSecond!!.onReceiveValue(null)
            }
            mFileUploadCallbackSecond = filePathCallback

            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)

            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

            i.type = "*/*"

            startActivityForResult(
                Intent.createChooser(i, "Choose a file"), REQUEST_CODE_FILE_PICKER
            )

            return true

        }

        override fun onHideCustomView() {
            (window.decorView as FrameLayout).removeView(mCustomView)
            mCustomView = null
            window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
            requestedOrientation = mOriginalOrientation
            mCustomViewCallback!!.onCustomViewHidden()
            mCustomViewCallback = null
        }

        override fun onShowCustomView(
            paramView: View, paramCustomViewCallback: CustomViewCallback
        ) {
            if (mCustomView != null) {
                onHideCustomView()
                return
            }
            mCustomView = paramView
            mOriginalSystemUiVisibility = window.decorView.getSystemUiVisibility()
            mOriginalOrientation = requestedOrientation
            mCustomViewCallback = paramCustomViewCallback
            (window.decorView as FrameLayout).addView(
                mCustomView, FrameLayout.LayoutParams(-1, -1)
            )
            window.decorView.systemUiVisibility = 3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            Log.d("mytag", "onGeolocationPermissionsShowPrompt called")
            super.onGeolocationPermissionsShowPrompt(origin, callback);
            callback?.invoke(origin, true, false);
        }

        override fun onGeolocationPermissionsHidePrompt() {
            Log.d("mytag", "onGeolocationPermissionsHidePrompt called")
            super.onGeolocationPermissionsHidePrompt()

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webview.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.webview.restoreState(savedInstanceState)
    }

    @Throws(JSONException::class)
    private fun decodeString(response: String): JSONObject {
        return if (Build.VERSION.SDK_INT >= 24) {
            JSONObject(Html.fromHtml(response, Html.FROM_HTML_MODE_LEGACY).toString())
        } else {
            JSONObject(Html.fromHtml(response).toString())
        }
    }


}




