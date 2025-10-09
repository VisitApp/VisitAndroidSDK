package com.getvisitapp.google_fit.util;

import static java.text.DateFormat.getTimeInstance;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.getvisitapp.google_fit.pojo.ActivitySession;
import com.getvisitapp.google_fit.pojo.HealthDataGraphValues;
import com.getvisitapp.google_fit.pojo.SleepCard;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;

/**
 * Created by shashvat on 09/05/18.
 */

/**
 * dp.getOriginalDataSource().getAppPackageName() return null or com.google.android.gms, if the data is originally from google fit.
 */

@Keep
public class GoogleFitConnector {

    private static final int REQUEST_GOOGLE_SIGNIN = 101;
    private static final int REQUEST_FIT_PERMISSIONS = 201;
    private static final String TAG = GoogleFitConnector.class.getSimpleName();

    final Context context;
    private GoogleConnectorFitListener listener;

    private ArrayList<String> fitDataTypeList = new ArrayList<>();


    private SimpleDateFormat readableFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

    private int SLEEP_START_HOUR = 22;
    private int SLEEP_END_HOUR = 7;

    String authToken;
    List<String> blacklistedApps= new ArrayList<>();



    public GoogleFitConnector(Context context, String webClientId, GoogleConnectorFitListener listener) {
        this.context = context;
        this.listener = listener;
        setFitDataTypes();
    }

    public GoogleFitConnector(Context context, String webClientId) {
        this.context = context;
        this.listener = null;
        setFitDataTypes();
    }

    public static long compareTwoTimeStamps(long currentTime, long oldTime) {
        long milliseconds1 = oldTime;
        long milliseconds2 = currentTime;

        long diff = milliseconds2 - milliseconds1;
        long diffSeconds = diff / 1000;
        long diffMinutes = diff / (60 * 1000);
        long diffHours = diff / (60 * 60 * 1000);
        long diffDays = diff / (24 * 60 * 60 * 1000);

        return diffDays;
    }


    public static int getDifferenceBetweenTwoDays(long sessionStart, long sessionEnd) {

        // If session start is 12AM 1st Jan and session end is 2nd Jan 12AM, the difference comes out to be 2 days
        // If session start is 12AM 1st Jan and session end is 1st Jan 11:59PM, the difference comes out to be 1 days
        // Because of this, the results may vary depending on what is being passed
        // Subtracting 1 from the difference takes care of when the start and end date are two difference dates like 1st Jan 12AM and 2nd Jan 12AM

        long difference = sessionEnd - sessionStart - 1;
        int days = (int) ((difference) / (1000 * 60 * 60 * 24));
        return days;
    }

    private void setFitDataTypes() {
        fitDataTypeList.add("com.google.step_count.delta");
        fitDataTypeList.add("com.google.distance.delta");
        fitDataTypeList.add("com.google.calories.expended");
    }

    public void disconnect() {
        GoogleSignInAccount account = getLastSignedInGoogleAccount(context);
        Fitness.getConfigClient(context, account).disableFit();
    }


    public GoogleSignInAccount getLastSignedInGoogleAccount(Context context) {
        return GoogleSignIn.getLastSignedInAccount(context);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_FIT_PERMISSIONS) {
                Log.d(TAG, "onActivityResult: REQUEST_FIT_PERMISSIONS");
                if (getLastSignedInGoogleAccount(context).getServerAuthCode() != null) {
                    listener.onComplete();
                } else {
                    listener.onError();
                }
            }
            if (requestCode == REQUEST_GOOGLE_SIGNIN) {
                Log.d(TAG, "onActivityResult: REQUEST_GOOGLE_SIGNIN");
            }
        } else {
            Log.d(TAG, "onActivityResult: RESULT CODE NOT OK PERMISSIONS DENIED");
        }
    }


    // Returns the total activity time for this data set in milliseconds
    private long calculateActivityTimeForDataSet(DataSet dataSet) {
//        Log.d(TAG, "calculateActivityTimeForDataSet: Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = getTimeInstance();
        long totalActivityTime = 0;
        for (DataPoint dp : dataSet.getDataPoints()) {

            for (Field field : dp.getDataType().getFields()) {

                if (dp.getValue(field).asInt() == 7) {
//                    Log.d(TAG, "\tType: " + dp.getDataType().getName());
//                    Log.d(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
//                    Log.d(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
//                    Log.d(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));

//                    Log.d(TAG, "calculateActivityTimeForDataSet: totalActivityMinutes: " +
//                            (dp.getEndTime(TimeUnit.MILLISECONDS) - dp.getStartTime(TimeUnit.MILLISECONDS)) / (1000 * 60));
                    totalActivityTime += (dp.getEndTime(TimeUnit.MILLISECONDS) - dp.getStartTime(TimeUnit.MILLISECONDS));
                }
            }
        }
        return totalActivityTime;
    }

    // This method returns one dp.getValue()
    // So if I want pass in a dataset where
    private int parseWeeklyData(DataSet dataSet) {

        // DataSets are fundamental units in Fit API.
        // Each Data set can contain multiple data points
        // dataSet.getDataType().getName() gives the name of the data point. It could be com.google.step_count.delta or something else like distance, calories expended etc

        // Inside each DataPoint there are fields such as distance, steps etc. It depends on the kind of data being queries.
        // field.getName() gives the name of the field - it would be steps, distance something like this
        // dp.getValue(field) gives the value of the field - it would be integer for steps, long/float for distance and so on...

//        Log.d(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
//        DateFormat dateFormat = getTimeInstance();

        for (DataPoint dp : dataSet.getDataPoints()) {
//            Log.d(TAG, "Data point:");
//            Log.d(TAG, "Original getOriginalDataSource: dp.getOriginalDataSource().getName(): " + dp.getOriginalDataSource().getName());
//            Log.d(TAG, "Original getOriginalDataSource: dp.getOriginalDataSource().getAppPackageName(): " + dp.getOriginalDataSource().getAppPackageName());
//            Log.d(TAG, "Original getOriginalDataSource: dp.getOriginalDataSource().getStreamIdentifier(): " + dp.getOriginalDataSource().getStreamIdentifier());
//            Log.d(TAG, "Original getOriginalDataSource: dp.getOriginalDataSource().getDevice(): " + dp.getOriginalDataSource().getDevice());
//            Log.d(TAG, "Original getDataSource: dp.getDataSource().getName(): " + dp.getDataSource().getName());
//            Log.d(TAG, "Original getDataSource: dp.getDataSource().getAppPackageName(): " + dp.getDataSource().getAppPackageName());
//            Log.d(TAG, "Original getDataSource: dp.getDataSource().getStreamIdentifier(): " + dp.getDataSource().getStreamIdentifier());
//            Log.d(TAG, "Original getDataSource: dp.getDataSource().getDevice(): " + dp.getDataSource().getDevice());
//            Log.d(TAG, "\tType: " + dp.getDataType().getName());
//            Log.d(TAG, "parseWeeklyData: Start datapoint: " + readableFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
//            Log.d(TAG, "parseWeeklyData: End datapoint:" + readableFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));

            if (isUserInput(dp.getOriginalDataSource().getStreamIdentifier())) {
                continue;
            }


            //checking which app inserted data to google fit store.
            String appContributedToGoogleFitPackageName = dp.getOriginalDataSource().getAppPackageName();
            if (appContributedToGoogleFitPackageName != null && blacklistedApps.contains(appContributedToGoogleFitPackageName)) {
                continue;
            }

            for (Field field : dp.getDataType().getFields()) {
//                Log.d(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));
                return dp.getValue(field).asInt();
            }
        }
        return 0;
    }

    private boolean isUserInput(String streamName) {
        if (streamName == null) {
            return false;
        }
        return streamName.contains("user_input");
    }

    private float parseWeeklyDataForFloat(DataSet dataSet) {

        // DataSets are fundamental units in Fit API.
        // Each Data set can contain multiple data points
        // dataSet.getDataType().getName() gives the name of the data point. It could be com.google.step_count.delta or something else like distance, calories expended etc

        // Inside each DataPoint there are fields such as distance, steps etc. It depends on the kind of data being queries.
        // field.getName() gives the name of the field - it would be steps, distance something like this
        // dp.getValue(field) gives the value of the field - it would be integer for steps, long/float for distance and so on...

//        Log.d(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = getTimeInstance();
        for (DataPoint dp : dataSet.getDataPoints()) {
//            Log.d(TAG, "Data point:");
//            Log.d(TAG, "\tType: " + dp.getDataType().getName());
//            Log.d(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
//            Log.d(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));



            //checking which app inserted data to google fit store.
            String appContributedToGoogleFitPackageName = dp.getOriginalDataSource().getAppPackageName();
            if (appContributedToGoogleFitPackageName != null && blacklistedApps.contains(appContributedToGoogleFitPackageName)) {
                continue;
            }


            for (Field field : dp.getDataType().getFields()) {
//                Log.d(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));
                return dp.getValue(field).asFloat();
            }
        }
        return 0;
    }


    public Observable<HealthDataGraphValues> getWeeklySteps(final long startTime, final long endTime) {
//        Log.d(TAG, "getWeeklySteps: " + readableFormat.format(startTime));
//        Log.d(TAG, "getWeeklySteps: " + readableFormat.format(endTime));
        return Observable.zip(
                getWeekStepCountData(startTime, endTime),
                getAverageActivityInMinutes(startTime, endTime),
                new Func2<DataReadResponse, List<ActivitySession>, HealthDataGraphValues>() {
                    @Override
                    public HealthDataGraphValues call(DataReadResponse dataReadResponse, List<ActivitySession> activitySessionList) {
                        HealthDataGraphValues healthDataGraphValues = convertDataReadResultToHealthDataStepsAndActivity(dataReadResponse, true, HealthDataGraphValues.ACTIVITY_TYPE_STEPS, startTime, endTime);
                        if (healthDataGraphValues != null) {

                            Calendar calendar = Calendar.getInstance();
                            if ((activitySessionList.size() - 1) > 0)
                                calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(activitySessionList.get(activitySessionList.size() - 1).getSessionEnd()));
                            calendar.set(Calendar.HOUR_OF_DAY, 0);
                            calendar.set(Calendar.MINUTE, 0);
                            calendar.set(Calendar.SECOND, 0);
                            calendar.set(Calendar.MILLISECOND, 0);

                            int noOfDays = getDifferenceBetweenTwoDays(startTime, calendar.getTimeInMillis()) + 1;
                            long totalActivityTime = 0;
                            for (int i = 0; i < activitySessionList.size(); i++) {
                                totalActivityTime += activitySessionList.get(i).getValue();
                            }
                            healthDataGraphValues.setTotalActivityTime((int) (totalActivityTime / noOfDays));// change this, 7 is no of days
                            healthDataGraphValues.setActivitySessions(activitySessionList);
                        }


                        return healthDataGraphValues;

                    }
                }
        )
                .flatMap(new Func1<HealthDataGraphValues, Observable<HealthDataGraphValues>>() {
                    @Override
                    public Observable<HealthDataGraphValues> call(HealthDataGraphValues graphValues) {
                        return Observable.just(graphValues);
                    }
                })
                .subscribeOn(Schedulers.io());


    }

    private Observable<DataReadResponse> getWeekStepCountData(final long startTime, final long endTime) {
        return Observable.create(new Observable.OnSubscribe<DataReadResponse>() {
            @Override
            public void call(Subscriber<? super DataReadResponse> subscriber) {


                DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
                        .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                        .setType(DataSource.TYPE_DERIVED)
                        .setStreamName("estimated_steps")
                        .setAppPackageName("com.google.android.gms")
                        .build();

                DataReadRequest readRequest2 = new DataReadRequest.Builder()
                        .aggregate(ESTIMATED_STEP_DELTAS)
                        .aggregate(DataType.TYPE_ACTIVITY_SEGMENT)
                        .bucketByTime(1, TimeUnit.HOURS)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();


                Task<DataReadResponse> dataReadResponseTask = Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, getFitnessOptions()))
                        .readData(readRequest2);

                DataReadResponse result = null;
                try {
                    result = Tasks.await(dataReadResponseTask, 30, TimeUnit.SECONDS);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

                if (result == null) {
                    subscriber.onError(new Error("Something went wrong retrieving total steps for the week"));
                }


                if (result.getStatus().isSuccess()) {
                    subscriber.onNext(result);
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(new Error("Something went wrong retrieving total steps for the week " + result.getStatus()));
                }
            }
        });
    }

    @NonNull
    public Observable<HealthDataGraphValues> getDailySteps(final long startTime, final long endTime) {
//        Log.d(TAG, "getDailySteps: " + readableFormat.format(startTime));
//        Log.d(TAG, "getDailySteps: " + readableFormat.format(endTime));


        return Observable.zip(
                getDailyStepCountData(startTime, endTime),
                getTotalActivityInMinutes(startTime, endTime),
                new Func2<DataReadResponse, List<ActivitySession>, HealthDataGraphValues>() {
                    @Override
                    public HealthDataGraphValues call(DataReadResponse dataReadResponse, List<ActivitySession> activitySessions) {
                        HealthDataGraphValues graphValues = null;
                        graphValues = convertDataReadResultToHealthDataStepsAndActivity(dataReadResponse, true, HealthDataGraphValues.ACTIVITY_TYPE_STEPS, startTime, endTime);
                        if (graphValues != null) {
                            long totalActivityTime = 0;
                            for (int i = 0; i < activitySessions.size(); i++) {
                                totalActivityTime += activitySessions.get(i).getValue();
                            }
                            graphValues.setTotalActivityTime((int) totalActivityTime);
                            graphValues.setActivitySessions(activitySessions);
                        }
                        return graphValues;
                    }
                }
        ).flatMap(new Func1<HealthDataGraphValues, Observable<HealthDataGraphValues>>() {
            @Override
            public Observable<HealthDataGraphValues> call(HealthDataGraphValues graphValues) {
                return Observable.just(graphValues);
            }
        }).subscribeOn(Schedulers.io());

    }

    private Observable<DataReadResponse> getDailyStepCountData(final long startTime, final long endTime) {
        return Observable.create(new Observable.OnSubscribe<DataReadResponse>() {
            @Override
            public void call(Subscriber<? super DataReadResponse> subscriber) {


                DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
                        .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                        .setType(DataSource.TYPE_DERIVED)
                        .setStreamName("estimated_steps")
                        .setAppPackageName("com.google.android.gms")
                        .build();


                DataReadRequest readRequest2;
                if (isServerQueryRequired(startTime)) {
                    readRequest2 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_ACTIVITY_SEGMENT)
                            .aggregate(ESTIMATED_STEP_DELTAS)
                            .bucketByTime(1, TimeUnit.HOURS)
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .enableServerQueries()
                            .build();
                } else {
                    readRequest2 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_ACTIVITY_SEGMENT)
                            .aggregate(ESTIMATED_STEP_DELTAS)
                            .bucketByTime(1, TimeUnit.HOURS)
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .build();
                }

                Task<DataReadResponse> dataReadResponseTask = Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, getFitnessOptions()))
                        .readData(readRequest2);

                DataReadResponse result2 = null;
                try {
                    result2 = Tasks.await(dataReadResponseTask, 30, TimeUnit.SECONDS);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

                if (result2 == null) {
                    subscriber.onError(new Error("Something went wrong retrieving total steps for the day"));
                }

                if (result2.getStatus().isSuccess()) {
                    subscriber.onNext(result2);
                    subscriber.onCompleted();
                } else {
//                    Log.d(TAG, "call: " + result.getStatus().getStatusMessage());
//                    Log.d(TAG, "call: " + result.getStatus().getStatusCode());
                    subscriber.onError(new Error("Something went wrong retrieving total steps for the day " + result2.getStatus()));
                }
            }
        });
    }

    private boolean isServerQueryRequired(long queriedStartTime) {
        // Both times are in milliseconds
        // 6 WEEKS =
//        Log.d(TAG, "isServerQueryRequired: queriesStartTime: " + readableFormat.format(queriedStartTime));
        final long nowTime = System.currentTimeMillis();
        final long DIFFERENCE_IN_MILLIS = 1000 * 60 * 60 * 24 * 7; // If query is more than 5 weeks old
        if (queriedStartTime >= nowTime) {
//            Log.d(TAG, "isServerQueryRequired: difference in weeks: " + DIFFERENCE_IN_MILLIS);
//            Log.d(TAG, "isServerQueryRequired: serverQuerie required?  no");
            return false;
        }
//        final long difference = nowTime - queriedStartTime;
        if (compareTwoTimeStamps(nowTime, queriedStartTime) >= 60) {
//            Log.d(TAG, "isServerQueryRequired: difference in weeks: " + DIFFERENCE_IN_MILLIS);
//            Log.d(TAG, "isServerQueryRequired: serverQuerie required?  yes");
            return true;
        } else {
//            Log.d(TAG, "isServerQueryRequired: difference in weeks: " + DIFFERENCE_IN_MILLIS);
//            Log.d(TAG, "isServerQueryRequired: serverQuerie required?  no");
            return false;
        }
    }

    public Observable<HealthDataGraphValues> getWeeklyDistance(final long startTime, final long endTime) {
//        Log.d(TAG, "getWeeklyDistance: " + readableFormat.format(startTime));
//        Log.d(TAG, "getWeeklyDistance: " + readableFormat.format(endTime));


        return Observable.zip(
                getWeekDistanceData(startTime, endTime),
                getAverageActivityInMinutes(startTime, endTime),
                new Func2<DataReadResponse, List<ActivitySession>, HealthDataGraphValues>() {
                    @Override
                    public HealthDataGraphValues call(DataReadResponse dataReadResponse, List<ActivitySession> activitySessionList) {
                        HealthDataGraphValues healthDataGraphValues = null;
                        healthDataGraphValues = convertDataReadResultToHealthDataStepsAndActivity(dataReadResponse, false, HealthDataGraphValues.ACTIVITY_TYPE_DISTANCE, startTime, endTime);
                        if (healthDataGraphValues != null) {
                            Calendar calendar = Calendar.getInstance();
                            if ((activitySessionList.size() - 1) > 0)
                                calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(activitySessionList.get(activitySessionList.size() - 1).getSessionEnd()));
                            calendar.set(Calendar.HOUR_OF_DAY, 0);
                            calendar.set(Calendar.MINUTE, 0);
                            calendar.set(Calendar.SECOND, 0);
                            calendar.set(Calendar.MILLISECOND, 0);

                            int noOfDays = getDifferenceBetweenTwoDays(startTime, calendar.getTimeInMillis()) + 1;
                            long totalActivityTime = 0;
                            for (int i = 0; i < activitySessionList.size(); i++) {
                                totalActivityTime += activitySessionList.get(i).getValue();
                            }
                            healthDataGraphValues.setTotalActivityTime((int) (totalActivityTime / noOfDays));// change this, 7 is no of days
                            healthDataGraphValues.setActivitySessions(activitySessionList);
                        }

                        return healthDataGraphValues;
                    }

//                    @Override
//                    public HealthDataGraphValues call(DataReadResult dataReadResult, Integer integer) {
//                        HealthDataGraphValues healthDataGraphValues = convertDataReadResultToHealthDataStepsAndActivity(dataReadResult, false, HealthDataGraphValues.ACTIVITY_TYPE_DISTANCE);
//                        if(healthDataGraphValues != null) {
//                            healthDataGraphValues.setAverageActivity(integer);
//                        }
//
//                        return healthDataGraphValues;
//                    }
                }
        )
                .flatMap(new Func1<HealthDataGraphValues, Observable<HealthDataGraphValues>>() {
                    @Override
                    public Observable<HealthDataGraphValues> call(HealthDataGraphValues graphValues) {
                        return Observable.just(graphValues);
                    }
                })
                .subscribeOn(Schedulers.io());


    }

    private Observable<DataReadResponse> getWeekDistanceData(final long startTime, final long endTime) {
        return Observable.create(new Observable.OnSubscribe<DataReadResponse>() {
            @Override
            public void call(Subscriber<? super DataReadResponse> subscriber) {


                // If the ready request is before 3 months, then .enableServerQueries() add this to fetch from server as local data may not contain very old data
                // https://stackoverflow.com/questions/32899686/google-fit-api-history-limit

                DataReadRequest readRequest2;


                if (isServerQueryRequired(startTime)) {


                    readRequest2 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_DISTANCE_DELTA)
                            .bucketByTime(1, TimeUnit.DAYS)
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .enableServerQueries()
                            .build();
                } else {

                    readRequest2 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_DISTANCE_DELTA)
                            .bucketByTime(1, TimeUnit.DAYS)
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .build();
                }


                Task<DataReadResponse> dataReadResponseTask = Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, getFitnessOptions()))
                        .readData(readRequest2);

                DataReadResponse result2 = null;
                try {
                    result2 = Tasks.await(dataReadResponseTask, 30, TimeUnit.SECONDS);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

                if (result2 == null) {
                    subscriber.onError(new Error("Something went wrong retrieving total distance for the week"));
                }

                if (result2.getStatus().isSuccess()) {
                    subscriber.onNext(result2);
                    subscriber.onCompleted();
                } else {
                    subscriber.onNext(result2);
                    subscriber.onCompleted();
//                    Crashlytics.log("Something went wrong retrieving total distance for the week " + result.getStatus());
                    subscriber.onError(new Error("Something went wrong retrieving total distance for the week"));
                }
            }
        });
    }

    public Observable<HealthDataGraphValues> getDailyDistance(final long startTime, final long endTime) {
//        Log.d(TAG, "getDailyDistance: " + readableFormat.format(startTime));
//        Log.d(TAG, "getDailyDistance: " + readableFormat.format(endTime));


        return Observable.zip(
                getDailyDistanceData(startTime, endTime),
                getTotalActivityInMinutes(startTime, endTime),
                new Func2<DataReadResponse, List<ActivitySession>, HealthDataGraphValues>() {
                    @Override
                    public HealthDataGraphValues call(DataReadResponse dataReadResponse, List<ActivitySession> activitySessions) {
                        HealthDataGraphValues graphValues = null;
                        graphValues = convertDataReadResultToHealthDataStepsAndActivity(dataReadResponse, false, HealthDataGraphValues.ACTIVITY_TYPE_DISTANCE, startTime, endTime);
                        if (graphValues != null) {
                            long totalActivityTime = 0;
                            for (int i = 0; i < activitySessions.size(); i++) {
                                totalActivityTime += activitySessions.get(i).getValue();
                            }
                            graphValues.setTotalActivityTime((int) totalActivityTime);// change this
                            graphValues.setActivitySessions(activitySessions);
                        }
                        return graphValues;
                    }
                }
        ).flatMap(new Func1<HealthDataGraphValues, Observable<HealthDataGraphValues>>() {
            @Override
            public Observable<HealthDataGraphValues> call(HealthDataGraphValues graphValues) {
                return Observable.just(graphValues);
            }
        }).subscribeOn(Schedulers.io());
    }

    private Observable<DataReadResponse> getDailyDistanceData(final long startTime, final long endTime) {
        return Observable.create(new Observable.OnSubscribe<DataReadResponse>() {
            @Override
            public void call(Subscriber<? super DataReadResponse> subscriber) {

                // Since this gives the daily distance is buckets of 1 hour, we will receive 1 bucket per hour
                // If the startTime and endTime correspond to 12AM and 11:59PM, then the there will be 24 buckets provided
//                DataReadRequest readRequest;

                DataReadRequest readRequest1;
                if (isServerQueryRequired(startTime)) {
                    readRequest1 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_DISTANCE_DELTA)
                            .aggregate(DataType.TYPE_ACTIVITY_SEGMENT)
                            .bucketByTime(1, TimeUnit.HOURS)
                            .enableServerQueries()
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .build();
                } else {
                    readRequest1 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_DISTANCE_DELTA)
                            .aggregate(DataType.TYPE_ACTIVITY_SEGMENT)
                            .bucketByTime(1, TimeUnit.HOURS)
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .build();
                }

                Task<DataReadResponse> dataReadResponseTask = Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, getFitnessOptions()))
                        .readData(readRequest1);

                DataReadResponse result2 = null;
                try {
                    result2 = Tasks.await(dataReadResponseTask, 30, TimeUnit.SECONDS);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

                if (result2 == null) {
                    subscriber.onError(new Error("Something went wrong retrieving total distance for the day "));
                }

                if (result2.getStatus().isSuccess()) {
                    subscriber.onNext(result2);
                    subscriber.onCompleted();
                } else {
                    Log.d(TAG, "call: " + result2.getStatus());
                    Log.d(TAG, "call: " + result2.getStatus().getStatusMessage());
                    subscriber.onError(new Error("Something went wrong retrieving total distance for the day " + result2.getStatus()));
                }
            }
        });
    }

    public Observable<HealthDataGraphValues> getWeeklyCalories(final long startTime, final long endTime) {

        return Observable.zip(
                getWeekCaloriesData(startTime, endTime),
                getAverageActivityInMinutes(startTime, endTime),
                new Func2<DataReadResponse, List<ActivitySession>, HealthDataGraphValues>() {
                    @Override
                    public HealthDataGraphValues call(DataReadResponse dataReadResponse, List<ActivitySession> activitySessionList) {
                        if (dataReadResponse != null) {
                            HealthDataGraphValues graphValues = null;
                            graphValues = convertDataReadResultToHealthDataStepsAndActivity(dataReadResponse, false, HealthDataGraphValues.ACTIVITY_TYPE_CALORIES_BURNT, startTime, endTime);
                            if (graphValues != null) {

                                Calendar calendar = Calendar.getInstance();
                                if ((activitySessionList.size() - 1) > 0)
                                    calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(activitySessionList.get(activitySessionList.size() - 1).getSessionEnd()));
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);

                                Calendar rightNow = Calendar.getInstance();
                                rightNow.setFirstDayOfWeek(Calendar.MONDAY);
                                int day = rightNow.get(Calendar.DAY_OF_WEEK);
                                int dayOfMonth = rightNow.get(Calendar.DAY_OF_MONTH);
                                int diff = getDifferenceBetweenTwoDays(startTime, rightNow.getTimeInMillis());
                                int diff1 = getDifferenceBetweenTwoDays(startTime, endTime);
                                if (diff1 > 7) {
                                    int diff2 = getDifferenceBetweenTwoDays(startTime, rightNow.getTimeInMillis());
                                    if (diff2 <= 30) {
                                        ArrayList<Integer> values = new ArrayList<>();
                                        for (int i = 0; i < graphValues.getValues().size(); i++) {
                                            if (i < dayOfMonth) {
                                                values.add(graphValues.getValues().get(i));
                                            } else {
                                                values.add(0);
                                            }
                                        }
                                        graphValues.setValues(values);
                                    }
                                } else {
                                    if (diff <= 7) {
                                        ArrayList<Integer> values = new ArrayList<>();
                                        for (int i = 0; i < graphValues.getValues().size(); i++) {
                                            if (i < day - 1) {
                                                values.add(graphValues.getValues().get(i));
                                            } else {
                                                values.add(0);
                                            }
                                        }
                                        graphValues.setValues(values);
                                    }
                                }


                                int noOfDays = getDifferenceBetweenTwoDays(startTime, calendar.getTimeInMillis()) + 1;
                                long totalActivityTime = 0;
                                for (int i = 0; i < activitySessionList.size(); i++) {
                                    totalActivityTime += activitySessionList.get(i).getValue();
                                }
                                graphValues.setTotalActivityTime((int) (totalActivityTime / noOfDays));// change this, 7 is no of days
                                graphValues.setActivitySessions(activitySessionList);
                            }
                            return graphValues;
                        } else {
                            return new HealthDataGraphValues();
                        }
                    }
                }
        ).flatMap(new Func1<HealthDataGraphValues, Observable<HealthDataGraphValues>>() {
            @Override
            public Observable<HealthDataGraphValues> call(HealthDataGraphValues graphValues) {
                return Observable.just(graphValues);
            }
        })
                .subscribeOn(Schedulers.io());

    }

    private Observable<DataReadResponse> getWeekCaloriesData(final long startTime, final long endTime) {
        return Observable.create(new Observable.OnSubscribe<DataReadResponse>() {
            @Override
            public void call(Subscriber<? super DataReadResponse> subscriber) {
                DataReadRequest readRequest1;
                if (isServerQueryRequired(startTime)) {

                    readRequest1 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                            .bucketByTime(1, TimeUnit.DAYS)
                            .enableServerQueries()
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .build();

                } else {
                    readRequest1 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                            .bucketByTime(1, TimeUnit.DAYS)
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .build();
                }

                Task<DataReadResponse> dataReadResponseTask = Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, getFitnessOptions()))
                        .readData(readRequest1);

                DataReadResponse result2 = null;
                try {
                    result2 = Tasks.await(dataReadResponseTask, 30, TimeUnit.SECONDS);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

                if (result2 == null) {
                    subscriber.onError(new Error("Something went wrong retrieving calories for the week"));
                }

                if (result2.getStatus().isSuccess()) {
                    subscriber.onNext(result2);
                    subscriber.onCompleted();
                } else {
                    subscriber.onNext(null);
                    subscriber.onCompleted();
//                    Crashlytics.log("Something went wrong retrieving calories for the week " + result.getStatus());
                    subscriber.onError(new Error("Something went wrong retrieving calories for the week"));
                }
            }
        });
    }

    public Observable<HealthDataGraphValues> getDailyCalories(final long startTime, final long endTime) {
//        Log.d(TAG, "getDailyCalories: " + readableFormat.format(startTime));
//        Log.d(TAG, "getDailyCalories: " + readableFormat.format(endTime));

        return Observable.zip(
                getDailyCaloriesData(startTime, endTime),
                getTotalActivityInMinutes(startTime, endTime),
                new Func2<DataReadResponse, List<ActivitySession>, HealthDataGraphValues>() {
                    @Override
                    public HealthDataGraphValues call(DataReadResponse dataReadResponse, List<ActivitySession> activitySessions) {
                        HealthDataGraphValues graphValues = null;
                        graphValues = convertDataReadResultToHealthDataStepsAndActivity(dataReadResponse, false, HealthDataGraphValues.ACTIVITY_TYPE_CALORIES_BURNT, startTime, endTime);
                        if (graphValues != null) {
                            Calendar rightNow = Calendar.getInstance();
                            int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
                            int diff = getDifferenceBetweenTwoDays(startTime, rightNow.getTimeInMillis());
                            if (diff <= 0) {
                                ArrayList<Integer> values = new ArrayList<>();
                                for (int i = 0; i < graphValues.getValues().size(); i++) {
                                    if (i < currentHour) {
                                        values.add(graphValues.getValues().get(i));
                                    } else {
                                        values.add(0);
                                    }
                                    // graphValues.setValues();
                                }
                                graphValues.setValues(values);
                            }
                            long totalActivityTime = 0;
                            for (int i = 0; i < activitySessions.size(); i++) {
                                totalActivityTime += activitySessions.get(i).getValue();
                            }
                            graphValues.setTotalActivityTime((int) totalActivityTime);// change this
                            graphValues.setActivitySessions(activitySessions);
                        }
                        return graphValues;
                    }
                }
        ).flatMap(new Func1<HealthDataGraphValues, Observable<HealthDataGraphValues>>() {
            @Override
            public Observable<HealthDataGraphValues> call(HealthDataGraphValues graphValues) {
                return Observable.just(graphValues);
            }
        }).subscribeOn(Schedulers.io());

    }

    @NonNull
    private Observable<DataReadResponse> getDailyCaloriesData(final long startTime, final long endTime) {
        return Observable.create(new Observable.OnSubscribe<DataReadResponse>() {
            @Override
            public void call(Subscriber<? super DataReadResponse> subscriber) {

                DataReadRequest readRequest1;
                if (isServerQueryRequired(startTime)) {

                    readRequest1 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                            .bucketByTime(1, TimeUnit.HOURS)
                            .enableServerQueries()
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .build();
                } else {
                    readRequest1 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                            .bucketByTime(1, TimeUnit.HOURS)
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .build();
                }

                Task<DataReadResponse> dataReadResponseTask = Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, getFitnessOptions()))
                        .readData(readRequest1);

                DataReadResponse result2 = null;
                try {
                    result2 = Tasks.await(dataReadResponseTask, 30, TimeUnit.SECONDS);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

                if (result2 == null) {
                    subscriber.onError(new Error("Something went wrong retrieving total calories for the day "));
                }

                if (result2.getStatus().isSuccess()) {
                    subscriber.onNext(result2);
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(new Error("Something went wrong retrieving total calories for the day " + result2.getStatus()));
                }
            }
        });
    }

    public HealthDataGraphValues convertDataReadResultToHealthDataStepsAndActivity(DataReadResponse dataReadResponse, boolean isDataTypeInt, int healthDataGraphType, final long startTime, final long endTime) {
//        Log.d(TAG, "blacklistedApps: " + blacklistedApps.toString());

        Set<String> fraudApps = new HashSet<>();


        if (dataReadResponse.getBuckets().size() > 0) {
//            Log.i(
//                    TAG, "Number of returned buckets of DataSets is: " + dataReadResponse.getBuckets().size());
            HealthDataGraphValues healthDataGraphValues = new HealthDataGraphValues();
            healthDataGraphValues.setActivityType(healthDataGraphType);

            int diffDays = getDifferenceBetweenTwoDays(startTime, endTime) + 1;
//            Log.d(TAG, "convertDataReadResultToHealthDataStepsAndActivity: startTime: " + readableFormat.format(startTime));
//            Log.d(TAG, "convertDataReadResultToHealthDataStepsAndActivity: endTime: " + readableFormat.format(endTime));
//            Log.d(TAG, "convertDataReadResultToHealthDataStepsAndActivity: number of days: " + diffDays);

            // Number of items in the values array will NOT be decided by the number of buckets but by the different in days between startTime and endTime
            ArrayList<Integer> values = new ArrayList<>();
            ArrayList<String> appContributedToGoogleFitValues = new ArrayList<>();
            long totalActivityTime = 0;
            List<Long> activityTime = new ArrayList<>();

            // Each bucket is a time frame. This could be hourly bucket or daily bucket
            // If the query has been made for Daily then there will be 24 buckets
            // If the query has been made for Weekly, there will be 7 buckets
            // If the query has been made for Monthly, there will be 30/31/28/29 buckets depending on the number of days in that month


            for (Bucket bucket : dataReadResponse.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                fraudApps = new HashSet<>(); //for every bucket i want a new set.

                //Log.d(TAG, "dataSet size: " + dataSets.size());

                for (DataSet dataSet : dataSets) {

                    //adding all the package name that contributed to this dataPoint
                    for (DataPoint dp : dataSet.getDataPoints()) {
                        if (dp.getOriginalDataSource().getAppPackageName() != null) { //skipping the null value
                            fraudApps.add(dp.getOriginalDataSource().getAppPackageName());
                        }
                    }

                    //converting into a comma separated String
                    String appContributedToGoogleFitDateString = fraudApps.stream().map(String::valueOf)
                            .collect(Collectors.joining(","));

                    List<String> appsInstalled = new ArrayList<String>(fraudApps);

                    boolean blackListedAppInstalled = Collections.disjoint(appsInstalled, blacklistedApps);
                    //will return false, if the user has installed blacklisted apps

                    if (blackListedAppInstalled == false) {
                        //user have installed a fraud app to insert data to Google Fit
                    }
//                    Log.d(TAG, "appContributedToGoogleFitDate: " + appContributedToGoogleFitDateString);


                    // values array fills the y-axis of the graphs
                    // x-axis items are buckets. So for 10 buckets, there are 10 labels on the x-axis
                    // each value in values corresponds to the y-value of the graph
                    if (fitDataTypeList.contains(dataSet.getDataType().getName())) {
                        //Log.d(TAG, "convertDataReadResultToHealthDataStepsAndActivity: ");
                        if (isDataTypeInt) {
                            if (blackListedAppInstalled == false) { //in case of blacklisted app, send 0 instead of actual steps
                                values.add(0);
                            } else {
                                values.add(parseWeeklyData(dataSet));
                            }
                            appContributedToGoogleFitValues.add(appContributedToGoogleFitDateString);
                        } else {
                            if (blackListedAppInstalled == false) { //in case of blacklisted app, send 0 instead of actual steps
                                values.add(0);
                            } else {
                                values.add((int) parseWeeklyDataForFloat(dataSet));
                            }
                            appContributedToGoogleFitValues.add(appContributedToGoogleFitDateString);
                        }

                    }

                    // This is used to calculate the totalActivityTime for steps, distances, calories burnt
                    if (dataSet.getDataType().getName().equals("com.google.activity.summary")) {
                        totalActivityTime += calculateActivityTimeForDataSet(dataSet);
                        activityTime.add(calculateActivityTimeForDataSet(dataSet));

                    }
                }
            }
//            Log.d(TAG, "convertDataReadResultToHealthDataStepsAndActivity: before: size of values array: " + values.size());
            healthDataGraphValues.setValues(values);
            healthDataGraphValues.setAppContributedToGoogleFitValues(appContributedToGoogleFitValues);
            if (diffDays > 1) {
                healthDataGraphValues.setValues(healthDataGraphValues.groupValuesInto(diffDays));
            }
//            Log.d(TAG, "convertDataReadResultToHealthDataStepsAndActivity: after: size of values array: " + healthDataGraphValues.getValues().size());
//            healthDataGraphValues.setTotalActivityTime((int) (totalActivityTime/1000));
//            healthDataGraphValues.setActivityTimeinHours(activityTime);
//            Log.d(TAG, "convertDataReadResultToHealthDataStepsAndActivity: " + totalActivityTime);
            return healthDataGraphValues;
//
        }
        return null;
    }

    public Observable<HealthDataGraphValues> convertDataReadResultToHealthData(DataReadResult dataReadResult) {
        if (dataReadResult.getBuckets().size() > 0) {
//            Log.i(
//                    TAG, "Number of returned buckets of DataSets is: " + dataReadResult.getBuckets().size());
            HealthDataGraphValues healthDataGraphValues = new HealthDataGraphValues();
            ArrayList<Integer> values = new ArrayList<>();

            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();

                for (DataSet dataSet : dataSets) {
                    values.add((int) parseWeeklyDataForFloat(dataSet));
                }
            }
            healthDataGraphValues.setValues(values);
            return Observable.just(healthDataGraphValues);

        }
        return Observable.just(null);
    }


    // Get Sleep Methods
    // start time here should be 10PM of the previous day, and 9AM of the current day
    // Get all the gaps in times which are over 1 hour and sum them

    public Observable<List<Integer>> getTotalStepsForToday() {
//        Log.d(TAG, "getTotalStepsForToday: ");
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date()); // compute start of the day for the timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfDay = cal.getTimeInMillis();
        return getDailySteps(startOfDay, endOfDay)
                .flatMap(new Func1<HealthDataGraphValues, Observable<List<Integer>>>() {
                    @Override
                    public Observable<List<Integer>> call(HealthDataGraphValues graphValues) {
                        List<Integer> integerList = new ArrayList<>();
                        integerList.add(0, graphValues.getValuesSum());
                        integerList.add(1, graphValues.getTotalActivityTimeInSeconds());
                        //return Observable.just(graphValues.getValuesSum(), 1);
                        return Observable.just(integerList);
                    }
                });
    }

    public Observable<SleepCard> getSleepForToday() {
//        Log.d(TAG, "getSleepForToday: ");
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date()); // compute start of the day for the timestamp
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.HOUR_OF_DAY, SLEEP_START_HOUR);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startSleepTime = cal.getTimeInMillis();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, SLEEP_END_HOUR);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long endSleepTime = cal.getTimeInMillis();
        // If user sees his sleep at 6AM, it should show the user the time he spent sleeping from 11PM previous night to 6AM.
        // It should not show 0, it should not extrapolate sleep.
        if (endSleepTime > System.currentTimeMillis()) {
            endSleepTime = System.currentTimeMillis();
        }
        return getSleepFromFit(startSleepTime, endSleepTime);
    }

    private boolean isSleepForToday(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);

        Calendar today = Calendar.getInstance();
        today.setTime(new Date());
        today.set(Calendar.HOUR_OF_DAY, SLEEP_END_HOUR);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        long currentTime = System.currentTimeMillis();
        // Say the user check his sleep at 3AM on 4th Jan
        // currentTime would be of 3AM of 4th Jan since he is checking at 3AM
        // time is of 3rd Jan, 8PM
        // today.getTimeInMillis will give the time of 11AM on 4th Jan
        if (currentTime > time && currentTime < today.getTimeInMillis()) {
            return true;
        }

        // Say if the user checks his sleep at 10PM on 3rd Jan
        // Current time would be of 10PM 3rd Jan
        // time is of 8PM 3rd Jan
        // today.getTimeInMillis would be 11AM of 3rd Jan

        int dayOfTheYearToday = today.get(Calendar.DAY_OF_YEAR);
        int dayOfTheYearForGivenTime = calendar.get(Calendar.DAY_OF_YEAR);
        if ((dayOfTheYearToday == dayOfTheYearForGivenTime) && currentTime > time) {
            return true;
        }

        return false;

    }

    public Observable<HealthDataGraphValues> getSleepForTheDay(long startTimeOfDay) {
        Log.d(TAG, "getSleepForToday: ");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startTimeOfDay);
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.HOUR_OF_DAY, SLEEP_START_HOUR);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startSleepTime = cal.getTimeInMillis();
        Log.d(TAG, "getSleepForTheDay: " + readableFormat.format(startSleepTime));

        cal.setTimeInMillis(startSleepTime);
        cal.add(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, SLEEP_END_HOUR);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long endSleepTime = cal.getTimeInMillis();

        // Check if this is for today
        if (isSleepForToday(startSleepTime)) {
            if (endSleepTime > System.currentTimeMillis()) {
                endSleepTime = System.currentTimeMillis();
            }
        }

        Log.d(TAG, "getSleepForTheDay: " + readableFormat.format(endSleepTime));
        return getSleepFromFit(startSleepTime, endSleepTime).flatMap(new Func1<SleepCard, Observable<HealthDataGraphValues>>() {
            @Override
            public Observable<HealthDataGraphValues> call(SleepCard sleepCard) {
                HealthDataGraphValues graphValues = new HealthDataGraphValues();
                graphValues.setActivityType(HealthDataGraphValues.ACTIVITY_TYPE_SLEEP);
                graphValues.setSleepCard(sleepCard);
                return Observable.just(graphValues);
            }
        });
    }

    public Observable<HealthDataGraphValues> getSleepForWeek(long startTime, int noOfDays) {

        // Break down days between startTime and endTime
        // Number of days between these two timestamps
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTimeInMillis(startTime);
        startCalendar.add(Calendar.DATE, -1);


        List<Observable<SleepCard>> observableList = new ArrayList<>();


        for (int i = 0; i < noOfDays; i++) {
            startCalendar.set(Calendar.HOUR_OF_DAY, SLEEP_START_HOUR);
            startCalendar.set(Calendar.MINUTE, 0);

            long dayStartSleepRecordTime = startCalendar.getTimeInMillis();

            startCalendar.add(Calendar.DATE, 1);
            startCalendar.set(Calendar.HOUR_OF_DAY, SLEEP_END_HOUR);

            long dayEndSleepRecordTime = startCalendar.getTimeInMillis();
            Log.d(TAG, "getSleepForWeek: " + readableFormat.format(dayStartSleepRecordTime) + " to " + readableFormat.format(dayEndSleepRecordTime));
            observableList.add(
                    getSleepFromFit(dayStartSleepRecordTime, dayEndSleepRecordTime)
                            .subscribeOn(Schedulers.io()));

        }

        return Observable.zip(observableList, new FuncN<HealthDataGraphValues>() {
            @Override
            public HealthDataGraphValues call(Object... args) {
                Log.d(TAG, "call: ");

                // Make a HealthDataGraphValues and return it as an observable
                HealthDataGraphValues graphValues = new HealthDataGraphValues();
                graphValues.setActivityType(HealthDataGraphValues.ACTIVITY_TYPE_SLEEP);
                ArrayList<SleepCard> sleepCards = new ArrayList<>();
                SleepCard sleepCard;
                for (Object obj : args) {
                    sleepCard = (SleepCard) obj;
                    sleepCards.add(sleepCard);
//                    Log.d(TAG, "call: SleepCard to String: " + sleepCard.toString());
//                    Log.d(TAG, "call: SleepCard to String: last time: " + readableFormat.format(sleepCard.getEpochOfDay()));
//                    Log.d(TAG, "call: SleepCard to String: last time: " + dayOfTheWeekFormat.format(sleepCard.getEpochOfDay()));
                }
                Log.d(TAG, "call: number of cards: " + sleepCards.size());
                graphValues.setSleepCards(sleepCards);
                Log.d(TAG, "call: " + graphValues.getSleepDataForWeeklyGraphInJson());

                return graphValues;
            }
        })
                .flatMap(new Func1<HealthDataGraphValues, Observable<HealthDataGraphValues>>() {
                    @Override
                    public Observable<HealthDataGraphValues> call(HealthDataGraphValues values) {
                        return Observable.just(values);
                    }
                });


//        Log.d(TAG, "getSleepForWeek: size of obs list : " + observableList.size());
//        return observableList;


    }

    // Gives total sleep between two timestamps in SECONDS
    private Observable<SleepCard> getSleepFromFit(final long startTime, final long endTime) {
        Log.d(TAG, "getSleepFromFit: " + readableFormat.format(startTime) + " to " + readableFormat.format(endTime));
        return getDailySleepData(startTime, endTime)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .flatMap(new Func1<DataReadResponse, Observable<SleepCard>>() {
                    @Override
                    public Observable<SleepCard> call(DataReadResponse dataReadResponse) {
                        return Observable.just(calculateSleepFromSteps(dataReadResponse, startTime, endTime));
                    }
                });
    }

    private Observable<DataReadResponse> getDailySleepData(final long startTime, final long endTime) {
//        Log.d(TAG, "getDailySleepData: ");
        return Observable.create(new Observable.OnSubscribe<DataReadResponse>() {
            @Override
            public void call(Subscriber<? super DataReadResponse> subscriber) {

                DataReadRequest readRequest1;
                if (isServerQueryRequired(startTime)) {

                    readRequest1 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_ACTIVITY_SEGMENT)
                            .bucketByActivitySegment(60 * 1000, TimeUnit.MILLISECONDS)
                            .enableServerQueries()
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .build();
                } else {
                    readRequest1 = new DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_ACTIVITY_SEGMENT)
                            .bucketByActivitySegment(60 * 1000, TimeUnit.MILLISECONDS)
                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                            .build();
                }


                Task<DataReadResponse> dataReadResponseTask = Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context, getFitnessOptions()))
                        .readData(readRequest1);

                DataReadResponse result2 = null;
                try {
                    result2 = Tasks.await(dataReadResponseTask, 30, TimeUnit.SECONDS);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

                if (result2 == null) {
                    subscriber.onError(new Error("Something went wrong retrieving total sleep for the day "));
                }

                if (result2.getStatus().isSuccess()) {
                    subscriber.onNext(result2);
                    subscriber.onCompleted();
                } else {
                    subscriber.onNext(null);
                    subscriber.onCompleted();
//                    Crashlytics.log("Something went wrong retrieving total sleep for the day " + result.getStatus());
                    // subscriber.onError(new Error("Something went wrong retrieving total sleep for the day " + result.getStatus().getStatusMessage()));
                }
            }
        });
    }


    private SleepCard calculateSleepFromSteps(DataReadResponse dataReadResponse, long sleepStartsAt, long sleepEndsAt) {
//        Log.i(
//                TAG, "Number of returned buckets of DataSets is: " + dataReadResult.getBuckets().size());
//        Log.i(
//                TAG, "Number of buckets =  number of hours between the two timestamps provided: " + dataReadResult.getBuckets().size());
//        Log.i(
//                TAG, "Count the number of buckets that are empty: " + dataReadResult.getBuckets().size());

        // If asked sleep is for tomorrow or later on show no data in sleep
        Calendar todayCalendar = Calendar.getInstance();
        todayCalendar.set(Calendar.HOUR_OF_DAY, 23);
        todayCalendar.set(Calendar.MINUTE, 59);
        todayCalendar.set(Calendar.SECOND, 59);
        long tomorrow = todayCalendar.getTimeInMillis();


        if (tomorrow < sleepEndsAt) {
            Log.d(TAG, "calculateSleepFromSteps: Future date: No sleep for this day");
            SleepCard noSleep = new SleepCard();
            noSleep.setSleepSeconds(0);
            noSleep.setStartSleepTime(0);
            noSleep.setEndSleepTime(0);
            noSleep.setEpochOfDay(sleepStartsAt);
            return noSleep;

        }

        if (dataReadResponse != null) {
            SleepCard sleepCard = isAsleep(dataReadResponse.getBuckets());
            if (sleepCard.getSleepSeconds() != 0) {
                return sleepCard;
            } else {
                Log.d(TAG, "calculateSleepFromSteps: No Sleep recorded found. Moving to resting logic on Fit");
                return isRestingSegment(dataReadResponse.getBuckets());
            }
        } else return new SleepCard();
    }

    private SleepCard isAsleep(List<Bucket> buckets) {
        // If activity value is one of the following the user is asleep
        // 72, 109, 110, 111, 112
        long totalSleepSeconds = 0;
        long firstSleepTimeStamp = 0;
        long lastSleepTimeStamp = 0;
        long lastTimeStampOfDay = 0;
        for (Bucket bucket : buckets) {
            long totalSleepSecondsInThisBucket = 0;
            List<DataSet> dataSets = bucket.getDataSets();
            DateFormat dateFormat = getTimeInstance();
            for (DataSet dataSet : dataSets) {
                for (DataPoint dp : dataSet.getDataPoints()) {
//                    Log.d(TAG, "Data point:");
//                    Log.d(TAG, "\tType: " + dp.getDataType().getName());
//                    Log.d(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
//                    Log.d(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                    boolean sleeping = false;

                    for (Field field : dp.getDataType().getFields()) {
//                        Log.d(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));

                        if (field.getName().equals("activity") && (
                                dp.getValue(field).asInt() == 72 ||
                                        dp.getValue(field).asInt() == 109 ||
                                        dp.getValue(field).asInt() == 110 ||
                                        dp.getValue(field).asInt() == 111 ||
                                        dp.getValue(field).asInt() == 112)) {
                            if (totalSleepSeconds == 0) {
                                firstSleepTimeStamp = dp.getStartTime(TimeUnit.MILLISECONDS);
                            }
                            lastSleepTimeStamp = dp.getEndTime(TimeUnit.MILLISECONDS);
                            sleeping = true;
                            break;
                        }
                    }
                    if (sleeping) {
                        totalSleepSecondsInThisBucket += dp.getEndTime(TimeUnit.MILLISECONDS) - dp.getStartTime(TimeUnit.MILLISECONDS);
//                        Log.d(TAG, "isAsleep: " + totalSleepSecondsInThisBucket);
                    }
                    lastTimeStampOfDay = dp.getEndTime(TimeUnit.MILLISECONDS);

                }
            }

            totalSleepSeconds += totalSleepSecondsInThisBucket;
        }

        SleepCard sleepCard = new SleepCard();
        sleepCard.setSleepSeconds((int) (totalSleepSeconds / 1000));
        sleepCard.setStartSleepTime(firstSleepTimeStamp);
        sleepCard.setEndSleepTime(lastSleepTimeStamp);
        // If the sleep is from 11th to 12 ie 11th night to 12th morning, then epoch will contain 12th date
        sleepCard.setEpochOfDay(lastTimeStampOfDay);
        return sleepCard;
    }

    public Observable<List<ActivitySession>> getTotalActivityInMinutes(long startTime, long endTime) {
        return ActivityTimeHelper.getTotalActivityTime(context, startTime, endTime);
    }

    public Observable<List<ActivitySession>> getAverageActivityInMinutes(long startTime, long endTime) {
        return ActivityTimeHelper.getAverageActivityTime(context, startTime, endTime);
    }

    public Observable<Integer> getTotalDistanceForToday() {
        Log.d(TAG, "getTotalStepsForToday: ");
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date()); // compute start of the day for the timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfDay = cal.getTimeInMillis();
        return getDailyDistance(startOfDay, endOfDay)
                .flatMap(new Func1<HealthDataGraphValues, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(HealthDataGraphValues graphValues) {
                        return Observable.just(graphValues.getValuesSum());
                    }
                });
    }

    public SleepCard isRestingSegment(List<Bucket> buckets) {
        // If activity value is one of the following the user is asleep
        // 72, 109, 110, 111, 112

        // Assume that sleep will happen in one segment
        // So find the largest segment with sleep/still activity types and get its duration
        long totalSleepSeconds = 0;
        long firstSleepTimeStamp = 0;
        long lastSleepTimeStamp = 0;
        long lastTimeStampOfDay = 0;
        for (Bucket bucket : buckets) {
//            long totalSleepSecondsInThisBucket = 0;
            List<DataSet> dataSets = bucket.getDataSets();
            DateFormat dateFormat = getTimeInstance();
            for (DataSet dataSet : dataSets) {
//                Log.d(TAG, "isRestingSegment: DAta Set: ");
                for (DataPoint dp : dataSet.getDataPoints()) {
//                    Log.d(TAG, "Data point:");
//                    Log.d(TAG, "\tType: " + dp.getDataType().getName());
//                    Log.d(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
//                    Log.d(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                    boolean sleeping = false;

//                    Log.d(TAG, "Data point:");
//                    Log.d(TAG, "\tType: " + dp.getDataType().getName());
//                    Log.d(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
//                    Log.d(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));

                    for (Field field : dp.getDataType().getFields()) {
//                        Log.d(TAG, "isRestingSegment: Field Name : " + field.getName());
//                        Log.d(TAG, "isRestingSegment: " + dp.getValue(field));

                        if (
                                field.getName().equals("activity") &&
                                        ((dp.getValue(field).asInt() == 3) || (dp.getValue(field).asInt() == 4))) {
                            sleeping = true;
                            break;
                        }
                    }
                    if (sleeping) {
                        if (totalSleepSeconds < dp.getEndTime(TimeUnit.MILLISECONDS) - dp.getStartTime(TimeUnit.MILLISECONDS)) {
                            firstSleepTimeStamp = dp.getStartTime(TimeUnit.MILLISECONDS);
                            totalSleepSeconds = dp.getEndTime(TimeUnit.MILLISECONDS) - dp.getStartTime(TimeUnit.MILLISECONDS);
                            lastSleepTimeStamp = dp.getEndTime(TimeUnit.MILLISECONDS);
                        }
                    }
//                    totalSleepSeconds+=totalSleepSecondsInThisBucket;
                    lastTimeStampOfDay = dp.getEndTime(TimeUnit.MILLISECONDS);
                }
            }

//            totalSleepSeconds = totalSleepSecondsInThisBucket;
        }
//        Log.d(TAG, "isAsleep: total sleep mins: " + totalSleepSeconds/(1000*60));
        SleepCard sleepCard = new SleepCard();
        sleepCard.setSleepSeconds((int) (totalSleepSeconds / 1000));
        sleepCard.setStartSleepTime(firstSleepTimeStamp);
        sleepCard.setEndSleepTime(lastSleepTimeStamp);
        // If the sleep is from 11th to 12 ie 11th night to 12th morning, then epoch will contain 12th date
        sleepCard.setEpochOfDay(lastTimeStampOfDay);
        return sleepCard;
    }


    //    private boolean wasAsleep(DataSet dataSet) {
//
//        Log.d(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
//        DateFormat dateFormat = getTimeInstance();
//        boolean didSleep = false;
//        for (DataPoint dp : dataSet.getDataPoints()) {
//           didSleep = true;
//        }
//        return didSleep;
//    }


    FitnessOptions getFitnessOptions() {
        return FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)

                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)

                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)


                .addDataType(DataType.TYPE_DISTANCE_DELTA)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA)
                .build();
    }

    public interface GoogleConnectorFitListener {
        void onComplete();

        void onError();

        void onServerAuthCodeFound(String serverAuthCode);

    }
}
