package com.getvisitapp.google_fit.okhttp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;

//import com.chuckerteam.chucker.api.ChuckerInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;


@Keep
public class OkHttpRequests {
    private final OkHttpClient client;
    private final String authToken;


    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();


    public OkHttpRequests(String authToken, Context context) {
        client = new OkHttpClient.Builder()
//                .addInterceptor(new ChuckerInterceptor(context))
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .build();
        this.authToken = authToken;
    }


    public <P> Observable<P> postRequest(
            final String url, final JsonObject jsonObject, final Class<P> clazz) {
        return postRequest(url, jsonObject.toString(), clazz);
    }

    public <P> Observable<P> postRequest(
            final String url, final String postParams, final Class<P> clazz) {

        Log.d("mytag", "postRequest() authToken: " + authToken);
        return Observable.fromCallable(
                new Callable<P>() {
                    @Override
                    public P call() throws Exception {
                        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                        RequestBody requestBody = RequestBody.create(mediaType, postParams);
                        Request request =
                                addHeadersToBuilder(new Request.Builder().url(url).post(requestBody));

                        Response response = client.newCall(request).execute();
                        String responseString = response.body().string();

                        Log.d(
                                "mytag",
                                "OkHttpRequests postRequest :" + response);

                        if (response.isSuccessful()) {
                            return gson.fromJson(responseString, clazz);
                        } else {
                            BaseException baseException =
                                    gson.fromJson(responseString, BaseException.class);
                            if (baseException.message != null) {
                                throw baseException;
                            } else {
                                throw new Exception("Error occurred");
                            }
                        }
                    }
                });
    }


    private Request addHeadersToBuilder(Request.Builder builder) {

        if (builder == null) return null;
        builder.addHeader("Content-type", "application/json");
        builder.addHeader("Authorization", authToken);
        builder.addHeader("platform", "ANDROID");
        return builder.build();

    }

    public Observable<JSONObject> postRequestHandler(final String url, final JSONObject payload, final String TAG) {
        return postRequestHandler(url, payload, TAG, null);
    }

    public Observable<JSONObject> postRequestHandler(final String url, final JSONObject payload, final String TAG, Map<String, String> bundle) {
        //Log.d(TAG, "postRequestHandler: " + authToken);

        return Observable.fromCallable(new Callable<JSONObject>() {
            @Override
            public JSONObject call() throws Exception {
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(mediaType, String.valueOf(payload));
                Request request =
                        addHeadersToBuilder(new Request.Builder().url(url).post(requestBody));

                Response response = client.newCall(request).execute();

                String responseString = response.body().string();

                Log.d(
                        "mytag",
                        "OkHttpRequests postRequestHandler :" + response + "\n payload:" + payload);


                if (response.isSuccessful()) {
                    return new JSONObject(responseString);
                } else {

                    BaseException baseException =
                            new Gson().fromJson(responseString, BaseException.class);
                    if (baseException.message != null) {
                        throw baseException;
                    } else {
                        throw new Exception("Error occurred");
                    }
                }
            }
        });
    }

    public Observable<JSONObject> putRequestHandler(final String url, final JSONObject payload) {
        //Log.d(TAG, "postRequestHandler: " + authToken);

        return Observable.fromCallable(new Callable<JSONObject>() {
            @Override
            public JSONObject call() throws Exception {
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(mediaType, String.valueOf(payload));
                Request request =
                        addHeadersToBuilder(new Request.Builder().url(url).put(requestBody));

                Response response = client.newCall(request).execute();

                String responseString = response.body().string();

                Log.d(
                        "mytag",
                        "OkHttpRequests putRequestHandler:" + response + "\n payload:" + payload);

                if (response.isSuccessful()) {
                    return new JSONObject(responseString);
                } else {
                    BaseException baseException =
                            new Gson().fromJson(responseString, BaseException.class);
                    if (baseException.message != null) {
                        throw baseException;
                    } else {
                        throw new Exception("Error occurred");
                    }
                }
            }
        });
    }


}