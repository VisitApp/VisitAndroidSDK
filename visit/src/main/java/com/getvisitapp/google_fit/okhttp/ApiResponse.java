package com.getvisitapp.google_fit.okhttp;

import androidx.annotation.Keep;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Headers;

/**
 * Created by Ghost on 29/06/17.
 */

@Keep
public class ApiResponse {
    private Headers headers;
    private int responseCode;
    private String response;
    private String requestUrl;
    public String message;

    public ApiResponse(Headers headers, int responseCode, String response, String requestUrl) {
        this.headers = headers;
        this.requestUrl = requestUrl;
        this.responseCode = responseCode;
        this.response = response;
    }

    public ApiResponse(){

    }

    public Headers getHeaders() {
        return headers;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponse() {
        return response;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setMessage(String message){
        this.message =  message;
    }

    public String getMessage(){
        return message;
    }


    public static ApiResponse parseJson(JSONObject jsonObject){

        ApiResponse apiResponse ;
        apiResponse = new ApiResponse();

        try {
            if (jsonObject.getString("status").equals("200")){
                apiResponse.setMessage(jsonObject.getString("message"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return apiResponse;
    }
}
