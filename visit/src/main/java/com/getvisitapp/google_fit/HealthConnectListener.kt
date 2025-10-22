package com.getvisitapp.google_fit

import com.getvisitapp.google_fit.healthConnect.enums.HealthConnectConnectionState


interface HealthConnectListener {
    fun updateHealthConnectConnectionStatus(status: HealthConnectConnectionState, text: String)

    //This callback is used for both dashboard graph and detailed graph.
    fun loadVisitWebViewGraphData(webUrl: String)

    fun userDeniedHealthConnectPermission()
    fun userAcceptedHealthConnectPermission()


    fun requestPermission()

    fun logHealthConnectError(throwable: Throwable)
}