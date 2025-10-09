package com.getvisitapp.google_fit.pojo;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;

import java.text.SimpleDateFormat;

/**
 * Created by shashvat on 11/05/18.
 */
@Keep
public class SleepCard implements Parcelable {
    private int sleepSeconds;
    private long startSleepTime;
    private long endSleepTime;
    private SimpleDateFormat format = new SimpleDateFormat("hh:mm aaa");
    public long getEpochOfDay() {
        return epochOfDay;
    }

    private long epochOfDay;
    // If the sleep is from 11th to 12 ie 11th night to 12th morning, then epoch will contain 12th date

    public SleepCard() {
    }

    // Sets the time in seconds
    public int getSleepSeconds() {
        return sleepSeconds;
    }

    // Sets the time in seconds
    public void setSleepSeconds(int sleepSeconds) {
        this.sleepSeconds = sleepSeconds;
    }

    // In milliseconds
    public long getStartSleepTime() {
        return startSleepTime;
    }

    // In milliseconds
    public void setStartSleepTime(long startSleepTime) {
        this.startSleepTime = startSleepTime;
    }

    // In milliseconds
    public long getEndSleepTime() {
        return endSleepTime;
    }

    // In milliseconds
    public void setEndSleepTime(long endSleepTime) {
        this.endSleepTime = endSleepTime;
    }

    @Override
    public String toString() {
        try {
            int minutes = sleepSeconds/60;

            return "Slept " + minutes/(60) + " hrs" + minutes % 60 + " mins";
        }catch(Exception e ) {
            return super.toString();
        }
    }

    public String getFormattedSleep() {
        return secondsToFormattedDate(sleepSeconds);
    }

    public static String secondsToFormattedDate(int sleepSeconds) {
        if(sleepSeconds == 0) {
            return "0";
        }
        int minutes = sleepSeconds / 60;
        int hours = minutes / 60;
        return hours + "hr " + (minutes % 60)+ "mins";
    }

    // If the sleep is from 11th to 12 ie 11th night to 12th morning, then epoch will contain 12th date
    public void setEpochOfDay(long epochOfDay) {
        this.epochOfDay = epochOfDay;
    }

    public String getStartSleepTimeFormatted(long time) {
        if(time == 0) {
            return "No data";
        }

        return format.format(time);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.sleepSeconds);
        dest.writeLong(this.startSleepTime);
        dest.writeLong(this.endSleepTime);
        dest.writeSerializable(this.format);
        dest.writeLong(this.epochOfDay);
    }

    protected SleepCard(Parcel in) {
        this.sleepSeconds = in.readInt();
        this.startSleepTime = in.readLong();
        this.endSleepTime = in.readLong();
        this.format = (SimpleDateFormat) in.readSerializable();
        this.epochOfDay = in.readLong();
    }

    public static final Parcelable.Creator<SleepCard> CREATOR = new Parcelable.Creator<SleepCard>() {
        @Override
        public SleepCard createFromParcel(Parcel source) {
            return new SleepCard(source);
        }

        @Override
        public SleepCard[] newArray(int size) {
            return new SleepCard[size];
        }
    };
}
