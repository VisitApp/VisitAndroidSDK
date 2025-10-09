package com.getvisitapp.google_fit.okhttp;

import androidx.annotation.Keep;

@Keep
public class BaseException extends Exception {


    public String code;
    public String message;

    public BaseException() {}

    public BaseException(String message) {
        this.message = message;
    }

    //Added this method to access message parameter from kotlin files
    //This is done to avoid overload ambiguity with kotlin's Throwable class message parameter
    public String getErrorMessage(){
        return message;
    }
}
