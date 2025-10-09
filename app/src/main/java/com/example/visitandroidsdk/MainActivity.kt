package com.example.visitandroidsdk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Button
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.getvisitapp.google_fit.IntiateSdk
import com.getvisitapp.google_fit.data.VisitStepSyncHelper
import com.getvisitapp.google_fit.data.VisitStepSyncHelper.Companion.openGoogleFit
import com.getvisitapp.google_fit.event.ClosePWAEvent
import com.getvisitapp.google_fit.event.MessageEvent
import com.getvisitapp.google_fit.event.VisitEventType
import com.getvisitapp.google_fit.util.GoogleFitAccessChecker
import com.google.android.material.switchmaterial.SwitchMaterial
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {

    private var TAG = "mytag10"

    private val default_client_id =
        "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"

    private lateinit var googleFitAccessChecker: GoogleFitAccessChecker
    private lateinit var googleFitSwitch: SwitchMaterial

    private val tataAIG_base_url = BuildConfig.TATA_AIG_BASE_URL
    private val tataAIG_auth_token = BuildConfig.TATA_AIG_AUTH_TOKEN

    private lateinit var syncStepHelper: VisitStepSyncHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            init()
        }


        googleFitSwitch = findViewById(R.id.googleFitSwitch)
        googleFitAccessChecker = GoogleFitAccessChecker(this)

        syncStepHelper = VisitStepSyncHelper(context = this, default_client_id)


        findViewById<Button>(R.id.manualSyncStepButton).setOnClickListener {
            syncStepHelper.syncSteps(tataAIG_base_url, tataAIG_auth_token)
        }


        //Open Google Fit if installed else return false.
        findViewById<Button>(R.id.openGoogleFitApp).setOnClickListener {
            val result = openGoogleFit()
            if (result) {
                //do nothing
            } else {
                Toast.makeText(this, "Google Fit app is not installed.", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.hraIncomplete).setOnClickListener {
            syncStepHelper.sendHRAInComplete(tataAIG_base_url, tataAIG_auth_token)
        }

        findViewById<Button>(R.id.revokeFitBitAccessButton).setOnClickListener {
            syncStepHelper.revokeFitbitAccess()
        }

        checkForNotificationPermission()
    }

    fun checkForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 202)
            }
        }
    }

    fun setHealthAppStatus(googleFitEnabled: Boolean, fitbitEnabled: Boolean) {

        googleFitSwitch.setText("Google Fit / Fitbit", TextView.BufferType.SPANNABLE)


        val spannable = googleFitSwitch.text as Spannable


        spannable.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    googleFitSwitch.context,
                    R.color.redColor
                )
            ), 0, googleFitSwitch.text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (googleFitEnabled) {
            spannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        googleFitSwitch.context,
                        R.color.greenColor
                    )
                ), 0, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (fitbitEnabled) {
            spannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        googleFitSwitch.context,
                        R.color.greenColor
                    )
                ), 11, googleFitSwitch.text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        spannable.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    googleFitSwitch.context,
                    R.color.greyColor
                )
            ), 11, 12, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        event?.let { eventType ->
            Log.d(TAG, "event:${event.eventType}")
            when (eventType.eventType) {
                VisitEventType.AskForFitnessPermission -> {

                }

                VisitEventType.AskForLocationPermission -> {


                }

                is VisitEventType.FitnessPermissionGranted -> {
                    val data = event.eventType as VisitEventType.FitnessPermissionGranted

                    Log.d(
                        "mytag",
                        "MainActivity onMessageEvent() FitnessPermissionGranted called, isGoogleFit/Fitbit: ${data.isGoogleFit}"
                    )


                    var isGoogleFitConnected = data.isGoogleFit


                }

                is VisitEventType.FitnessPermissionRevoked -> {
                    val data = event.eventType as VisitEventType.FitnessPermissionRevoked

                    Log.d(
                        "mytag",
                        "MainActivity onMessageEvent() FitnessPermissionRevoked called, isGoogleFit: ${data.isGoogleFit}"
                    )

                }

                is VisitEventType.RequestHealthDataForDetailedGraph -> {

                    val graphEvent =
                        event.eventType as VisitEventType.RequestHealthDataForDetailedGraph

                }

                is VisitEventType.StartVideoCall -> {
                    val callEvent =
                        event.eventType as VisitEventType.StartVideoCall


                }

                is VisitEventType.HRA_Completed -> {


                }

                is VisitEventType.GoogleFitConnectedAndSavedInPWA -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        //passing event to Visit PWA to close itself
                        EventBus.getDefault().post(ClosePWAEvent())


                    }, 200)
                }

                is VisitEventType.HRAQuestionAnswered -> {
                    // can be used for analytics events
                    val hraQuestionEvent = event.eventType as VisitEventType.HRAQuestionAnswered
                    Log.d(
                        "mytag",
                        "current:${hraQuestionEvent.current} total:${hraQuestionEvent.total}"
                    )
                }

                VisitEventType.ConsultationBooked -> {
                    Log.d("mytag", "MainActivity ConsultationBooked event")
                }

                VisitEventType.CouponRedeemed -> {
                    Log.d("mytag", "MainActivity CouponRedeemed event")
                }

                is VisitEventType.FitnessPermissionError -> {
                    val eventData = event.eventType as VisitEventType.FitnessPermissionError

                    Log.d("mytag", "eventData: ${eventData.message}")

                }

                is VisitEventType.StepSyncError -> {
                    val eventData = event.eventType as VisitEventType.StepSyncError

                    Log.d("mytag", "eventData: ${eventData.message}")
                }

                is VisitEventType.NetworkError -> {
                    val eventData = event.eventType as VisitEventType.NetworkError


                    val errStatus = eventData.errStatus
                    val error = eventData.error

                    Log.d("mytag", "errStatus: $errStatus, error: $error")

                }

                is VisitEventType.VisitCallBack -> {
                    val eventData = event.eventType as VisitEventType.VisitCallBack

                    val message: String = eventData.message

                    val failureReason: String? = eventData.failureReason

                    Log.d("mytag", "VisitCallBack message: $message, failureReason: $failureReason")

                }
            }

        }

    }

    fun init() {


        val magicLink =
            BuildConfig.MAGIC_LINK


        IntiateSdk.s(
            this,
            false,
            magicLink,
            tataAIG_base_url,
            tataAIG_auth_token,
            default_client_id
        )
    }

    override fun onStart() {
        super.onStart()

        //unregister first before registering again.
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        EventBus.getDefault().register(this)
    }


    override fun onResume() {
        super.onResume()

        googleFitSwitch.setOnCheckedChangeListener(null)

        val googleFitAccess = googleFitAccessChecker.checkGoogleFitAccess();
        val fitBitAccess = syncStepHelper.getFitbitCurrentStatus()

        Log.d("mytag", "googleFitAccess: $googleFitAccess && fitBitAccess:$fitBitAccess")

        googleFitSwitch.isChecked = googleFitAccess || fitBitAccess

        //updating the UI
        setHealthAppStatus(
            googleFitAccess, fitBitAccess,
        )

        googleFitSwitch.setOnCheckedChangeListener(googleFitCheckChangeListener)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    var googleFitCheckChangeListener = object : OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            if (isChecked) {
                init()
            } else {
                if (syncStepHelper.getFitbitCurrentStatus()) {
                    syncStepHelper.revokeFitbitAccess()
                } else {
                    googleFitAccessChecker.revokeGoogleFitPermission(default_client_id)
                }

                //updating the UI.
                setHealthAppStatus(false, false)
            }
        }
    }
}