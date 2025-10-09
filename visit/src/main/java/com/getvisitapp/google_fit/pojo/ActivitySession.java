package com.getvisitapp.google_fit.pojo;

import androidx.annotation.Keep;

@Keep
public class ActivitySession {

    private long sessionStart;
    private long sessionEnd;
    private long value;
    private long totalActivityTime;

    public ActivitySession(){

    }

    public void setSession(long sessionStart, long sessionEnd, long value){
       this.sessionStart = sessionStart;
       this.sessionEnd =  sessionEnd;
       this.value = value;
    }

    public void setTotalActivityTime(long totalActivityTime){
        this.totalActivityTime = totalActivityTime;
    }

    public long getSessionStart(){
        return sessionStart;
    }

    public long getSessionEnd(){
        return sessionEnd;
    }

    public long getValue(){
        return value;
    }


    public long getTotalActivityTime(){
        return totalActivityTime;
    }
}

