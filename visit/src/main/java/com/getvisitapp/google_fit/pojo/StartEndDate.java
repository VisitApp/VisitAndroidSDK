package com.getvisitapp.google_fit.pojo;

import androidx.annotation.Keep;

@Keep
public class StartEndDate {
    long startTime;
    long endTime;

    public StartEndDate(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
