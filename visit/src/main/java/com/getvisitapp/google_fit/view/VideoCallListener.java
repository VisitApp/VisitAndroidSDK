package com.getvisitapp.google_fit.view;


import androidx.annotation.Keep;

@Keep
public interface VideoCallListener {
    void startVideoCall(int sessionId, int consultationId, String authToken);
}
