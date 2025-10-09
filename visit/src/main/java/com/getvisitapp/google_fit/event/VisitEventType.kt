package com.getvisitapp.google_fit.event

import androidx.annotation.Keep

@Keep
sealed class VisitEventType {
    /**
     * Called when user clicked on Connect to Google Fit
     */
    object AskForFitnessPermission : VisitEventType()

    /**
     * Called after the user has successfully connected to Google Fit or FitBit is connected
     * true in case of google fit is connected
     * false is case of fitbit is connected
     */
    class FitnessPermissionGranted(var isGoogleFit: Boolean) : VisitEventType()


    class FitnessPermissionRevoked(var isGoogleFit: Boolean) : VisitEventType()


    class FitnessPermissionError(var message: String?) : VisitEventType()

    class StepSyncError(var message: String?) : VisitEventType()

    /**
     * Called when the PWA request for health data for certain type.
     * @param type can be 'day' or 'month' or 'week'
     * @param frequency can be 'steps' or 'distance' or 'calories' or 'sleep'
     * @param timestamp is a type epoch timestamp
     */


    class RequestHealthDataForDetailedGraph(
        var type: String?,
        var frequency: String?,
        var timestamp: Long
    ) :
        VisitEventType()

    /**
     * Called when the PWA request for Location permission
     */
    object AskForLocationPermission : VisitEventType()

    /**
     * Called when the PWA request for video call.
     */
    class StartVideoCall(var sessionId: Int, var consultationId: Int, var authToken: String?) :
        VisitEventType()

    /**
     * Called after the user has completed the HRA questions
     */

    class HRA_Completed() : VisitEventType()

    class GoogleFitConnectedAndSavedInPWA() : VisitEventType()

    class HRAQuestionAnswered(val current: Int, val total: Int) : VisitEventType()

    object ConsultationBooked : VisitEventType()

    object CouponRedeemed : VisitEventType()

    class NetworkError(val errStatus: Int?, val error: String?) : VisitEventType()

    class VisitCallBack(val message: String, val failureReason: String?) : VisitEventType()


}
