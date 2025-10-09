package com.getvisitapp.google_fit.view;


import androidx.annotation.Keep;

import com.getvisitapp.google_fit.model.RoomDetails;

@Keep
public interface TwillioVideoView {

    void roomDetails(RoomDetails roomDetails);
    void setError(String message);
}

