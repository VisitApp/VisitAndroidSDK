package com.getvisitapp.google_fit.model;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class RoomDetails implements Serializable {

    public int consultationId;
    public String roomName;
    public String token;
    public String userName;
    public String doctorName;
    public String doctorProfileImage;
    public String duration;
    public String createdAt;

}
