package com.getvisitapp.google_fit.data;

import androidx.annotation.Keep;

import com.getvisitapp.google_fit.pojo.SleepCard;

@Keep
public class SleepStepsData {
    public SleepCard sleepCard;
    public int steps;

    public SleepStepsData(SleepCard sleepCard, int steps) {
        this.sleepCard = sleepCard;
        this.steps = steps;
    }
}
