package com.getvisitapp.google_fit.data;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Keep;

import com.getvisitapp.google_fit.pojo.HealthDataGraphValues;
import com.getvisitapp.google_fit.util.GoogleFitConnector;
import com.getvisitapp.google_fit.util.StepsCounter;
import com.getvisitapp.google_fit.view.GenericListener;
import com.getvisitapp.google_fit.view.GoogleFitStatusListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@Keep
public class GoogleFitUtil implements GenericListener {
    String default_web_client_id;
    String TAG = "mytag";


    private Activity context;
    WebAppInterface webAppInterface;

    GoogleFitStatusListener listener;
    private Subscriber<SleepStepsData> sleepStepsDataSubscriber;
    private Subscriber<HealthDataGraphValues> healthDataGraphValuesSubscriber;
    private SyncStepHelper syncStepHelper;


    public GoogleFitUtil(Activity context, GoogleFitStatusListener listener, String default_web_client_id) {
        this.context = context;
        this.listener = listener;
        this.webAppInterface = new WebAppInterface(listener);
        this.default_web_client_id = default_web_client_id;
    }

    private StepsCounter stepsCounter;
    private GoogleFitConnector googleFitConnector;

    public StepsCounter getStepsCounter() {
        return stepsCounter;
    }

    public GoogleFitConnector getGoogleFitConnector() {
        return googleFitConnector;
    }

    public WebAppInterface getWebAppInterface() {
        return webAppInterface;
    }

    public void init() {
        stepsCounter = StepsCounter.getInstance(context);
        googleFitConnector = new GoogleFitConnector(context, default_web_client_id, new GoogleFitConnector.GoogleConnectorFitListener() {
            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete() called");
            }

            @Override
            public void onError() {
                Log.d(TAG, "onError() called");
            }

            @Override
            public void onServerAuthCodeFound(String s) {
                Log.d(TAG, "error Occured: " + s);
            }
        });


    }

    public void askForGoogleFitPermission() {
        Log.d("mytag", "askForPermission() called");
        stepsCounter.run(default_web_client_id, new GenericListener() {
            @Override
            public void onJobDone(String s) {
                Log.d(TAG, "onJobDone() called Email Id: " + s);

                listener.onFitnessPermissionGranted();
            }


        });

    }

    public void fetchDataFromFit() {

        sleepStepsDataSubscriber = new Subscriber<SleepStepsData>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(SleepStepsData sleepStepsData) {
                Log.d("mytag", "steps:" + sleepStepsData.steps + " , sleep=" + sleepStepsData.sleepCard);
                listener.loadDailyFitnessData(sleepStepsData.steps , TimeUnit.SECONDS.toMinutes(sleepStepsData.sleepCard.getSleepSeconds()));
            }
        };

        Observable.zip(googleFitConnector.getTotalStepsForToday(),
                googleFitConnector.getSleepForToday(),
                (integers, sleepCard) -> {
                    SleepStepsData sleepStepsData;
                    if (!integers.isEmpty()) {
                        sleepStepsData = new SleepStepsData(sleepCard, integers.get(0));
                    } else {
                        sleepStepsData = new SleepStepsData(sleepCard, 0);
                    }
                    return sleepStepsData;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sleepStepsDataSubscriber);
    }

    public void getActivityData(String type, String frequency, long timeStamp) {

        SimpleDateFormat readableFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

        addHealthDataSubscriber(type, frequency);


        Calendar cal = Calendar.getInstance();


        cal.setTimeInMillis(timeStamp);


        cal.setFirstDayOfWeek(Calendar.MONDAY);


        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();


        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfDay = cal.getTimeInMillis();


        cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        long startOfWeek = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        long endOfWeek = cal.getTimeInMillis();


        // get start of the month
        cal.set(Calendar.DAY_OF_MONTH, 1);
        long startOfMonth = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        long endOfMonth = cal.getTimeInMillis();

//        Log.d("mytag", "getDataFromGoogleFitStore: start of week: " + readableFormat.format(startOfWeek));
//        Log.d("mytag", "getDataFromGoogleFitStore: end of week: " + readableFormat.format(endOfWeek));

        if (type != null && frequency != null) {

            if (type.equals("steps")) {
                switch (frequency) {
                    case "day": {
                        googleFitConnector.getDailySteps(startOfDay, endOfDay)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(healthDataGraphValuesSubscriber);
                        break;
                    }
                    case "week": {
                        googleFitConnector.getWeeklySteps(startOfWeek, endOfWeek)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(healthDataGraphValuesSubscriber);
                        break;
                    }
                    case "month": {
                        googleFitConnector.getWeeklySteps(startOfMonth, endOfMonth)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(healthDataGraphValuesSubscriber);
                        break;
                    }
                }
            } else if (type.equals("distance")) {
                switch (frequency) {
                    case "day": {
                        googleFitConnector.getDailyDistance(startOfDay, endOfDay)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(healthDataGraphValuesSubscriber);
                        break;
                    }
                    case "week": {
                        googleFitConnector.getWeeklyDistance(startOfWeek, endOfWeek)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(healthDataGraphValuesSubscriber);
                        break;
                    }
                    case "month": {
                        googleFitConnector.getWeeklyDistance(startOfMonth, endOfMonth)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(healthDataGraphValuesSubscriber);
                        break;
                    }

                }
            } else if (type.equals("calories")) {
                switch (frequency) {
                    case "day": {
                        googleFitConnector.getDailyCalories(startOfDay, endOfDay)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(healthDataGraphValuesSubscriber);
                        break;
                    }
                    case "week": {
                        googleFitConnector.getWeeklyCalories(startOfWeek, endOfWeek)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(healthDataGraphValuesSubscriber);
                        break;
                    }
                    case "month": {
                        googleFitConnector.getWeeklyCalories(startOfMonth, endOfMonth)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(healthDataGraphValuesSubscriber);
                        break;
                    }

                }
            } else if (type.equals("sleep")) {
                switch (frequency) {
                    case "day": {
                        googleFitConnector.getSleepForTheDay(startOfDay)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(healthDataGraphValuesSubscriber);
                        break;
                    }
                    case "week": {
                        googleFitConnector.getSleepForWeek(startOfWeek, 7)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(healthDataGraphValuesSubscriber);
                        break;
                    }
                }
            }
        }


    }

    private void addHealthDataSubscriber(String type, String frequency) {
        if (healthDataGraphValuesSubscriber != null && !healthDataGraphValuesSubscriber.isUnsubscribed()) {
            healthDataGraphValuesSubscriber.unsubscribe();
        }
        healthDataGraphValuesSubscriber = new Subscriber<HealthDataGraphValues>() {
            @Override
            public void onCompleted() {
                Log.d("mytag", "onCompleted: healthDataGraphValuesSubscriber");
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(HealthDataGraphValues graphValues) {
                if (type != null && frequency != null) {
                    if (type.equals("sleep")) {
                        switch (frequency) {
                            case "day": {

                                String value = "DetailedGraph.updateDailySleep(" + graphValues.getSleepCard().getStartSleepTime() + "," + graphValues.getSleepCard().getEndSleepTime() + ")";

                                Log.d(TAG, "run: getSleep minutes daily: " + value);
                                listener.loadGraphDataUrl(value);
                                break;
                            }
                            case "week": {
                                String value = "DetailedGraph.updateSleepData(JSON.stringify(" + graphValues.getSleepDataForWeeklyGraphInJson() + "));";


                                Log.d(TAG, "run: getSleep minutes daily: " + value);
                                listener.loadGraphDataUrl(value);
                                break;
                            }
                        }
                    } else {
                        switch (frequency) {
                            case "day": {
                                String value = "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24]," + graphValues.getValues() + ", '" + type + "', 'day','" + graphValues.getTotalActivityTimeInMinutes() + "')";
                                Log.d(TAG, "valueString: " + value);
                                listener.loadGraphDataUrl(value);
                                break;
                            }
                            case "week": {
                                String value = "DetailedGraph.updateData([1,2,3,4,5,6,7]," + graphValues.getValues() + ",'" + type + "', 'week','" + graphValues.getTotalActivityTimeInMinutes() + "')";
                                Log.d(TAG, "valueString: " + value);
                                listener.loadGraphDataUrl(value);
                                break;
                            }
                            case "month": {
                                String value = "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31]," + graphValues.getValues() + ",'" + type + "', 'month','" + graphValues.getTotalActivityTimeInMinutes() + "')";
                                Log.d(TAG, "valueString: " + value);
                                listener.loadGraphDataUrl(value);
                                break;
                            }
                        }
                        Log.d(TAG, "updateGraph() called. " + type + " frequency: " + frequency + " values:" + graphValues.getValues());

                    }
                }

            }
        };
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d("mytag", "onActivityResult called. requestCode: " + requestCode + " resultCode: " + resultCode);

        if (this.stepsCounter != null) {
            this.stepsCounter.onActivityResult(requestCode, resultCode, intent, this);
        }

    }


    @Override
    public void onJobDone(String s) {
        Log.d(TAG, "onJobDone() called email: " + s);

        if (s != null && !s.isEmpty() && s.equals("UPDATE THE UI")) {
            listener.onFitnessPermissionGranted();
        } else {
            this.askForGoogleFitPermission();
        }
    }

    public void sendDataToServer(String baseUrl, String authToken, long googleFitLastSync, long gfHourlyLastSync, String memberId, String tataAIG_base_url, String tata_aig_authToken) {
        if (stepsCounter.hasAccess()) {
            syncStepHelper = new SyncStepHelper(getGoogleFitConnector(), baseUrl, authToken, tataAIG_base_url, tata_aig_authToken, memberId, context);
            if (googleFitLastSync != 0) {
                syncStepHelper.dailySync(googleFitLastSync);
            }
            if (gfHourlyLastSync != 0) {
                syncStepHelper.hourlySync(gfHourlyLastSync,false);
            }


        }

    }

}
