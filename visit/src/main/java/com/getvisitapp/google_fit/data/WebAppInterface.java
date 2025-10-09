package com.getvisitapp.google_fit.data;

import android.util.Log;
import android.webkit.JavascriptInterface;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.getvisitapp.google_fit.view.GoogleFitStatusListener;

@Keep
public class WebAppInterface {
    GoogleFitStatusListener listener;

    public WebAppInterface(GoogleFitStatusListener listener) {
        this.listener = listener;
    }

    @JavascriptInterface
    public void connectToGoogleFit() {

        Log.d("mytag", "connectToGoogleFit() called");

        listener.connectToGoogleFit(true);

    }


    @JavascriptInterface
    public void disconnectFromGoogleFit() {
        Log.d("mytag", "disconnectFromGoogleFit() called");
        listener.disconnectFromGoogleFit();
    }


    @JavascriptInterface
    public void connectToFitbit(String url, String authToken) {
        Log.d("mytag", "connectToFitbit() called");

        listener.connectToFitbit(url, authToken);
    }

    @JavascriptInterface
    public void disconnectFromFitbit() {
        Log.d("mytag", "disconnectFromFitbit() called");

        listener.disconnectFromFitbit();
    }

    @JavascriptInterface
    public void askForGoogleFitGraphData() {

        Log.d("mytag", "askForGoogleFitGraphData() called");

        listener.connectToGoogleFit(false);

    }

    @JavascriptInterface
    public void getDataToGenerateGraph(String type, String frequency, long timestamp) {
        Log.d("mytag", "getDataToGenerateGraph() called. type:" + type + " frequency: " + frequency + " timestamp:" + timestamp);
        listener.requestActivityData(type, frequency, timestamp);
    }

    @JavascriptInterface
    public void updateApiBaseUrlV3(String apiBaseUrl, String authtoken, long googleFitLastSync, long gfHourlyLastSync, String memberId, boolean fitbitConnected, long fitbitLastSyncTimeStamp, String policyNumber) {
        Log.d("mytag", "updateApiBaseUrlV3() called. apiBaseUrl: " + apiBaseUrl + ",authtoken: " + authtoken + ",googleFitLastSync: " + googleFitLastSync + ",gfHourlyLastSync: " + gfHourlyLastSync + " memberId: " + memberId + " fitbitConnected: " + fitbitConnected + " fitbitLastSyncTimeStamp: " + fitbitLastSyncTimeStamp + " policyNumber: " + policyNumber);
        listener.updateApiBaseUrlV3(apiBaseUrl, authtoken, googleFitLastSync, gfHourlyLastSync, memberId, fitbitConnected, fitbitLastSyncTimeStamp, policyNumber);
    }

    @JavascriptInterface
    public void visitCredentialCallback(String apiBaseUrl, String authtoken) {

        Log.d("mytag", "visitCredentialCallback() called, apiBaseUrl: " + apiBaseUrl + " authtoken: " + authtoken);

        listener.visitCredentialCallback(apiBaseUrl, authtoken);

    }


    @JavascriptInterface
    public void getLocationPermissions() {
        Log.d("mytag", "getLocationPermissions() called.");
        listener.askForLocationPermission();

    }

    @JavascriptInterface
    public void initiateVideoCall(int sessionId, int consultationId, String authToken) {
        Log.d("mytag", "initiateVideoCall() called.");
        listener.startVideoCall(sessionId, consultationId, authToken);
    }

    @JavascriptInterface
    public void hraCompleted() {
        Log.d("mytag", "hraCompleted() called.");
        listener.hraCompleted();
    }

    @JavascriptInterface
    public void googleFitConnectedAndSavedInPWA() {
        Log.d("mytag", "googleFitConnectedAndSavedInPWA() called.");
        listener.googleFitConnectedAndSavedInPWA();
    }

    @JavascriptInterface
    public void inHraEndPage() {
        Log.d("mytag", "inHraEndPage() called");
        listener.inHraEndPage();
    }

    @JavascriptInterface
    public void hraQuestionAnswered(int current, int total) {
        Log.d("mytag", "hraQuestionAnswered() called");
        listener.hraQuestionAnswered(current, total);
    }

    @JavascriptInterface
    public void downloadHraLink(String link) {
        Log.d("mytag", "downloadHraLink() called");
        listener.downloadHraLink(link, true);
    }

    @JavascriptInterface
    public void inFitSelectScreen() {
        Log.d("mytag", "inFitSelectScreen() called");
        listener.inFitSelectScreen();
    }

    @JavascriptInterface
    public void openDependentLink(String link) {
        Log.d("mytag", "openDependentLink() called");
        listener.openDependentLink(link);
    }

    @JavascriptInterface
    public void closeView(boolean tataUser) {
        Log.d("mytag", "closeView() called");
        listener.closeView(tataUser);
    }

    @JavascriptInterface
    public void pendingHraUpdation() {
        Log.d("mytag", "pendingHraUpdation() called");
        listener.pendingHraUpdation();
    }

    @JavascriptInterface
    public void hraInComplete(String jsonObject, boolean isIncomplete) {
        Log.d("mytag", "hraInComplete called(). jsonObject:" + jsonObject + " isIncomplete:" + isIncomplete);
        listener.hraInComplete(jsonObject, isIncomplete);
    }

    @JavascriptInterface
    public void openLink(String url) {
        Log.d("mytag", "openLink called(). url:" + url);
        listener.openLink(url);
    }

    @JavascriptInterface
    public void openExternalLink(String url) {
        Log.d("mytag", "openExternalLink called(). url:" + url);
        listener.openExternalLink(url);
    }


    @JavascriptInterface
    public void consultationBooked() {
        Log.d("mytag", "consultationBooked called().");
        listener.consultationBooked();
    }

    @JavascriptInterface
    public void couponRedeemed() {
        Log.d("mytag", "couponRedeemed called().");
        listener.couponRedeemed();
    }

    @JavascriptInterface
    public void internetErrorHandler(String jsonObject) { //this is used for timeout
        Log.d("mytag", "internetErrorHandler called(). jsonObject: " + jsonObject);
        listener.internetErrorHandler(jsonObject);
    }


    @JavascriptInterface
    public void visitCallback(String jsonObject) { //this is used for analytics purpose by TATA AIG.
        Log.d("mytag", "visitCallback called(). jsonObject: " + jsonObject);
        listener.visitCallback(jsonObject);
    }

    @JavascriptInterface
    public void downloadPdf(@NonNull String link) {
        Log.d("mytag", "downloadPdf called(): " + link);
        listener.downloadPdf(link);
    }

    @JavascriptInterface
    public void setAuthToken(@NonNull String authToken) {
        Log.d("mytag", "setUserAuthToken called(): " + authToken);
        listener.setAuthToken(authToken);
    }


}