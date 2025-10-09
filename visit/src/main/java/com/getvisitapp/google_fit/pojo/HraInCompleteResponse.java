package com.getvisitapp.google_fit.pojo;

import androidx.annotation.Keep;

@Keep
public class HraInCompleteResponse {

    public String member_id;
    public HraDetails hra_details;

    @Override
    public String toString() {
        return "HraInCompleteResponse{" +
                "member_id='" + member_id + '\'' +
                ", hra_details=" + hra_details +
                '}';
    }
}
