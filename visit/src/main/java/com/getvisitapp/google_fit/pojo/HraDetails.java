package com.getvisitapp.google_fit.pojo;

import androidx.annotation.Keep;

@Keep
public class HraDetails {
    public String color;
    public int score;

    @Override
    public String toString() {
        return "HraDetails{" +
                "color='" + color + '\'' +
                ", score=" + score +
                '}';
    }
}
