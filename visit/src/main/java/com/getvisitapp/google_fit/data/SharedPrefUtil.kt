package com.getvisitapp.google_fit.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import com.getvisitapp.google_fit.util.Constants
import com.getvisitapp.google_fit.util.Constants.FITBIT_CONNECTION_STATUS
import com.getvisitapp.google_fit.util.Constants.FITBIT_LAST_SYNC_TIMESTAMP
import com.getvisitapp.google_fit.util.Constants.HRA_INCOMPLETE_RESPONSE
import com.getvisitapp.google_fit.util.Constants.IS_HRA_INCOMPLETE
import com.getvisitapp.google_fit.util.Constants.POLICY_NUMBER

@Keep
class SharedPrefUtil(context: Context) {

    var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("VisitSdkPref", AppCompatActivity.MODE_PRIVATE)
    var sharedPreferencesEditor: SharedPreferences.Editor = sharedPreferences.edit()


    fun setVisitBaseUrl(baseUrl: String) {
        sharedPreferencesEditor.putString(Constants.VISIT_BASE_URL, baseUrl).commit()
    }

    fun getVisitBaseUrl(): String {
        return sharedPreferences.getString(Constants.VISIT_BASE_URL, "")!!;
    }

    fun setTATA_AIG_MemberId(memberId: String) {
        sharedPreferencesEditor.putString(Constants.TATA_AIG_MEMBER_ID, memberId).commit()
    }

    fun getTATA_AIG_MemberId(): String {
        return sharedPreferences.getString(Constants.TATA_AIG_MEMBER_ID, "")!!
    }

    fun setVisitAuthToken(authToken: String) {
        sharedPreferencesEditor.putString(Constants.VISIT_AUTH_TOKEN, authToken).commit()
    }

    fun getVisitAuthToken(): String {
        return sharedPreferences.getString(Constants.VISIT_AUTH_TOKEN, "")!!
    }


    fun setTataAIGLastSyncTimeStamp(tata_aig_last_sync_time_stamp: Long) {
        sharedPreferencesEditor.putLong(
            Constants.TATA_AIG_LAST_SYNC_TIME_STAMP,
            tata_aig_last_sync_time_stamp
        ).commit()
    }

    fun getTataAIGLastSyncTimeStamp(): Long {
        return sharedPreferences.getLong(Constants.TATA_AIG_LAST_SYNC_TIME_STAMP, 0L)
    }


    fun setHRAIncompleteStatusRequest(jsonObject: String) {
        sharedPreferencesEditor.putString(HRA_INCOMPLETE_RESPONSE, jsonObject).commit();
    }

    fun getHRAInCompleteStatusResponse(): String? {
        return sharedPreferences.getString(HRA_INCOMPLETE_RESPONSE, null);
    }

    fun setHRAIncompleteStatus(isInComplete: Boolean) {
        sharedPreferencesEditor.putBoolean(IS_HRA_INCOMPLETE, isInComplete).commit()
    }

    fun getHRAIncompleteStatus(): Boolean {
        return sharedPreferences.getBoolean(IS_HRA_INCOMPLETE, true)
    }

    fun setFitBitLastSyncTimeStamp(timestamp: Long) {
        sharedPreferencesEditor.putLong(FITBIT_LAST_SYNC_TIMESTAMP, timestamp).commit()
    }

    fun getFitbitLastSyncTimestamp(): Long {
        return sharedPreferences.getLong(FITBIT_LAST_SYNC_TIMESTAMP, 0L);
    }

    fun setFitBitConnectedStatus(status: Boolean) {
        sharedPreferencesEditor.putBoolean(FITBIT_CONNECTION_STATUS, status).commit()
    }

    fun getFitBitConnectionStatus(): Boolean {
        return sharedPreferences.getBoolean(FITBIT_CONNECTION_STATUS, false)
    }

    fun setPolicyNumber(policyNumber: String) {
        sharedPreferencesEditor.putString(POLICY_NUMBER, policyNumber).commit()
    }

    fun getPolicyNumber(): String {
        return sharedPreferences.getString(POLICY_NUMBER, "")!!;
    }


    fun clear() {
        sharedPreferencesEditor.clear().commit();
    }


}