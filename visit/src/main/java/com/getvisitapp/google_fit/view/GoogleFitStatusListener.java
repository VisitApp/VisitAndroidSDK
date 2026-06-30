package com.getvisitapp.google_fit.view;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@Keep
public interface GoogleFitStatusListener {
    void connectToGoogleFit(boolean redirectUserToGoogleFitStatusPage);

    void disconnectFromGoogleFit();

    void connectToFitbit(String url, String authToken);

    void onFitnessPermissionGranted();

    void loadWebUrl(String urlString);

    void requestActivityData(String type, String frequency, long timestamp);

    void loadGraphDataUrl(String url);


    void updateApiBaseUrlV3(String apiBaseUrl, String authtoken, long googleFitLastSync, long gfHourlyLastSync, String memberId, boolean isFitBitConnected, long fitbitLastSyncTimeStamp,String policyNumber);

    void visitCredentialCallback(String visitApiBaseUrl, String visitAuthToken);

    void askForLocationPermission();

    void startVideoCall(int sessionId, int consultationId, String authToken);

    void hraCompleted();

    void googleFitConnectedAndSavedInPWA();

    void inHraEndPage();

    void hraQuestionAnswered(int current, int total);

    void downloadHraLink(String link, boolean toShare);

    void inFitSelectScreen();

    void openDependentLink(String link);

    void closeView(boolean tataUser);

    void pendingHraUpdation();

    void hraInComplete(String jsonObject, boolean isIncomplete);

    void consultationBooked();

    void loadDailyFitnessData(long steps, long sleep);

    void disconnectFromFitbit();

    void couponRedeemed();

    void internetErrorHandler(@Nullable String jsonObject);

    void openLink(String url);

    void openExternalLink(String url);


    void visitCallback(@Nullable String jsonObject);
    void visitEvent(String eventName, @Nullable String properties);

    void downloadPdf(@NonNull String link);

    void setAuthToken(@NonNull String authToken);

}
