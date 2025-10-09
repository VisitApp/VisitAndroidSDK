package com.getvisitapp.google_fit.pojo;

import android.util.Log;


import androidx.annotation.Keep;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by shashvat on 10/05/18.
 */
@Keep
public class HealthDataGraphValues {
    private static final String TAG = HealthDataGraphValues.class.getSimpleName();
//    private ArrayList<Integer> xAxis;
//    private ArrayList<Integer> yAxis;

    private ArrayList<Integer> values;
    private ArrayList<String> appContributedToGoogleFitValues;
    private int activityType;
    public static final int ACTIVITY_TYPE_STEPS = 1;
    public static final int ACTIVITY_TYPE_DISTANCE = 2;

    public ArrayList<String> getAppContributedToGoogleFitValues() {
        return appContributedToGoogleFitValues;
    }

    public void setAppContributedToGoogleFitValues(ArrayList<String> appContributedToGoogleFitValues) {
        this.appContributedToGoogleFitValues = appContributedToGoogleFitValues;
    }

    public static final int ACTIVITY_TYPE_CALORIES_BURNT = 3;
    public static final int ACTIVITY_TYPE_SLEEP = 4;
    private int totalActivityTime;
    private List<Long> activityTime;
    private Integer averageActivity;
    private ArrayList<SleepCard> sleepCards;
    private List<ActivitySession> activitySession = new ArrayList<>();



    public SleepCard getSleepCard() {
        return sleepCard;
    }

    public void setSleepCard(SleepCard sleepCard) {
        this.sleepCard = sleepCard;
    }

    private SleepCard sleepCard;

    public HealthDataGraphValues() {
    }

    public HealthDataGraphValues(int activityType) {
        this.activityType = activityType;
    }

    public int getActivityType() {
        return activityType;
    }

    public void setActivityType(int activityType) {
        this.activityType = activityType;
    }

//    public ArrayList<Integer> getxAxis() {
//        return xAxis;
//    }
//
//    public void setxAxis(ArrayList<Integer> xAxis) {
//        this.xAxis = xAxis;
//    }
//
//    public ArrayList<Integer> getyAxis() {
//        return yAxis;
//    }
//
//    public void setyAxis(ArrayList<Integer> yAxis) {
//        this.yAxis = yAxis;
//    }

    public ArrayList<Integer> getValues() {
        return values;
    }

    public void setValues(ArrayList<Integer> values) {
        this.values = values;
    }

    public int getAverageActivityInMinutes() {
        if(averageActivity == null) {
            return 0;
        }
        return averageActivity;
    }

    // For Weekly and Monthly graphs, avergae of values is also required to be shown in the view.
    // 
    // This will not be used for daily graphs.
    // If this is used for daily graphs, it'll show the averge steps per hour
    public String getAvgValues() {
        return calculateAverage(values);
    }

    private String calculateAverage(ArrayList <Integer> values) {
        int sum = 0;
        int totalCounted = 0;
        if(values!=null &&!values.isEmpty()) {
            for (Integer v : values) {
                if(v != 0) {
                    totalCounted++;
                }
                sum += v;
            }
            if(activityType == HealthDataGraphValues.ACTIVITY_TYPE_DISTANCE) {
                float inMeters = sum;
                if(totalCounted == 0) {
                    return "0m";
                }
                if(inMeters > 1000f) {
                    return formatDouble((inMeters/(1000*totalCounted))) + "km";
                } else {
                    return inMeters/totalCounted + "m";
                }
            } else if(activityType == HealthDataGraphValues.ACTIVITY_TYPE_STEPS) {
                if(totalCounted == 0) {
                    return "0";
                }
                return String.valueOf(sum / totalCounted);
            } else if(activityType == HealthDataGraphValues.ACTIVITY_TYPE_CALORIES_BURNT) {
                if(totalCounted == 0) {
                    return "0kcal";
                }
                return sum/totalCounted + "kcal";
            }

        }
        return String.valueOf(sum);
    }

    public String getValuesAsCSV() {
        if (values!=null && values.size()>0)
            return android.text.TextUtils.join(",", values);
        else  return "";
    }

    public String getValuesIndexAsCSV() {
        ArrayList<Integer> indexes = new ArrayList<>();
        if (values!=null) {
            for (int i = 0; i < values.size(); i++) {
                indexes.add(i + 1);
            }
        }
        return android.text.TextUtils.join(",", indexes);
    }

    public int getValuesSum() {
        if(values == null) {
            return 0;
        }
        int sum = 0;
        for (int i = 0 ;i<values.size();i++) {
            sum+= values.get(i);
        }
        return sum;
    }

    private String formatDouble(double value) {
        DecimalFormat numberFormat = new DecimalFormat("#.0");
        return numberFormat.format(value);
    }

    public String getValuesSumWithUnit() {
        switch (activityType) {
            case HealthDataGraphValues.ACTIVITY_TYPE_STEPS:
                return String.valueOf(getValuesSum());
            case HealthDataGraphValues.ACTIVITY_TYPE_DISTANCE:
                int s = getValuesSum();
                if(s > 1000) {
                    return formatDouble(((float)s/1000)) + "km";
                } else {
                    return s + "m";
                }
            case HealthDataGraphValues.ACTIVITY_TYPE_CALORIES_BURNT:
                return getValuesSum() + " kcal";
            default:
                return String.valueOf(getValuesSum());
        }
    }

    public int getTotalActivityTimeInSeconds() {
        return totalActivityTime;
    }

    public int getTotalActivityTimeInMinutes() {
        return (totalActivityTime/(60));
    }

    public ArrayList<Integer> getActivityTime(){

        ArrayList<Integer> activity = new ArrayList<>();
        for (int i = 0; i<activityTime.size(); i++){
            activity.add((int) TimeUnit.MILLISECONDS.toSeconds(activityTime.get(i)));
        }
        return activity;
    }

    public void setTotalActivityTime(int totalActivityTime) {
        this.totalActivityTime = totalActivityTime;
    }

    public void setActivitySessions(List<ActivitySession> activitySessions){
        this.activitySession.addAll(activitySessions);
    }

    public List<ActivitySession> getActivitySession() {
        return this.activitySession;
    }

    public void setAverageActivity(Integer averageActivity) {
        this.averageActivity = averageActivity;
        this.averageActivity = averageActivity;
    }

    public void setActivityTimeinHours(List<Long> activityTime){
        this.activityTime = activityTime;
    }

    public String getSleepDataForWeeklyGraphInJson() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("E");
        JSONObject object;
        JSONArray array = new JSONArray();
        for (SleepCard sleepCard: sleepCards) {
            object = new JSONObject();
            try {
                object.put("sleepTime", sleepCard.getStartSleepTime());
                object.put("wakeupTime", sleepCard.getEndSleepTime());

                Log.d(TAG, "getSleepDataForWeeklyGraphInJson: endSleepTime: " + sleepCard.getEpochOfDay());
                Log.d(TAG, "getSleepDataForWeeklyGraphInJson: Day of the week" + dayFormat.format(sleepCard.getEpochOfDay()));

                object.put("day", dayFormat.format(sleepCard.getEpochOfDay()));

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(sleepCard.getEpochOfDay());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                object.put("startTimestamp", + calendar.getTimeInMillis());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            array.put(object);
        }

        Log.d(TAG, "getSleepDataForWeeklyGraphInJson: " + array.toString());
        return array.toString();
    }


    public void setSleepCards(ArrayList<SleepCard> sleepCards) {
        this.sleepCards = sleepCards;
    }

    public ArrayList<SleepCard> getSleepCards(){
        return sleepCards;
    }

    public String getAverageSleep() {
        int totalSeconds = 0;
        int totalDaysSleepWasRecorded = 0;
        for(SleepCard sleepCard: sleepCards) {
            totalSeconds += sleepCard.getSleepSeconds();
            if (sleepCard.getSleepSeconds() > 0) {
                totalDaysSleepWasRecorded ++;
            }
        }

        // Ignore days where no sleep was recorded
        if(totalSeconds == 0 || totalDaysSleepWasRecorded == 0) {
            return "No Data";
        }
        return SleepCard.secondsToFormattedDate(totalSeconds/totalDaysSleepWasRecorded);
    }

    public ArrayList<Integer> groupValuesInto(int diffDays) {
        // This method is used to divide values into equal sets of diffDays
        // Ex: if diffDays = 1, then
        // values = sum of all values
        // If diffDays = 7, then
        // values = values.count/7 and then sum each group

        ArrayList<Integer> buckets = new ArrayList<>();
        int counter = 0;
        for (int i = 0 ; i < diffDays ; i++) {

            int daySum = 0 ;
            for (int j = 0 ; j < values.size()/diffDays ; j++) {
                // This will run 24 times
                daySum += values.get(counter);
                counter++;
            }
            buckets.add(daySum);
//            counter = i*(values.size()/diffDays);
        }
        return buckets;
    }
}
