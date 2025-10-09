package com.getvisitapp.google_fit.okhttp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import rx.Observable;
import rx.functions.Func1;

@Keep
public class MainActivityPresenter {
    private final String baseUrl;
    private final String tataAIG_base_url;
    private final String authToken;
    OkHttpRequests okHttpRequests;
    OkHttpRequests tata_AIG_okHttpRequests;

    public MainActivityPresenter(String baseUrl, String authToken, String tataAIG_base_url, String tata_aig_authToken, Context context) {
        this.baseUrl = baseUrl;
        this.authToken = authToken;
        this.okHttpRequests = new OkHttpRequests(authToken, context);

        this.tataAIG_base_url = tataAIG_base_url;
        this.tata_AIG_okHttpRequests = new OkHttpRequests(tata_aig_authToken, context);
    }

    public Observable<ApiResponse> sendData(JsonObject payload) {
        Log.d("mytag", "sendData() authToken: " + authToken);

        String url = baseUrl + "users/data-sync?isPWA=yes";

        Log.d("mytag", "dataSync URL: " + url);
        return okHttpRequests.postRequest(url, payload, ApiResponse.class);
    }


    /**
     * This is getting used for hourly sync
     */
    public Observable<Boolean> syncDayWithServer(JSONObject payload) {
        Log.d("mytag", "syncDayWithServer: " + payload.toString());
        String url = baseUrl + "users/embellish-sync?isPWA=yes";

        return okHttpRequests.postRequestHandler(url, payload, "SYNC_FITNESS")
                .concatMap(new Func1<JSONObject, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(JSONObject jsonObject) {
                        // If the server returns a positive response
                        Log.d("mytag", "call: API CALL SUCCESSFULL" + jsonObject);
                        try {
                            if (jsonObject.getString("message").equalsIgnoreCase("success")) {
                                return Observable.just(true);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return Observable.just(false);
                    }
                });
    }

    /**
     * This is getting used for hourly sync in tata AIG
     */
    public Observable<Boolean> syncDayWithTATA_AIG_Server(JSONObject payload) {
        Log.d("mytag", "syncDayWithServer: " + payload.toString());
        String url = tataAIG_base_url + "/fitness-activity";

        return tata_AIG_okHttpRequests.postRequestHandler(url, payload, "SYNC_FITNESS")
                .concatMap(new Func1<JSONObject, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(JSONObject jsonObject) {
                        // If the server returns a positive response
                        Log.d("mytag", "tata AIG server response:" + jsonObject);
                        try {
                            if (jsonObject.getString("action").equalsIgnoreCase("SUCCESS")) {
                                return Observable.just(true);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return Observable.just(false);
                    }
                });
    }


    /**
     * Sending HRA incomplete status to TATA AIG
     */
    public Observable<Boolean> sendHRAInCompleteStatus(JSONObject payload) {
        Log.d("mytag", "sendHRAInCompleteStatus: " + payload.toString());
        String url = tataAIG_base_url + "/hra-fitness-details";

        return tata_AIG_okHttpRequests.putRequestHandler(url, payload)
                .concatMap(new Func1<JSONObject, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(JSONObject jsonObject) {
                        // If the server returns a positive response
                        Log.d("mytag", "tata AIG send data response:" + jsonObject);
                        try {
                            if (jsonObject.getString("action").equalsIgnoreCase("SUCCESS")) {
                                return Observable.just(true);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return Observable.just(false);
                    }
                });
    }
}
