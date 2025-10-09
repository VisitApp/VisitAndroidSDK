package com.getvisitapp.google_fit.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;

import com.getvisitapp.google_fit.event.MessageEvent;
import com.getvisitapp.google_fit.event.VisitEventType;
import com.getvisitapp.google_fit.okhttp.ApiResponse;
import com.getvisitapp.google_fit.okhttp.MainActivityPresenter;
import com.getvisitapp.google_fit.okhttp.Transformers;
import com.getvisitapp.google_fit.pojo.ActivitySummaryGoal;
import com.getvisitapp.google_fit.pojo.HealthDataGraphValues;
import com.getvisitapp.google_fit.pojo.StartEndDate;
import com.getvisitapp.google_fit.util.DateHelper;
import com.getvisitapp.google_fit.util.GoogleFitConnector;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Emitter;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

@Keep
public class SyncStepHelper {
    String TAG = "SyncStepHelper";
    private GoogleFitConnector googleFitConnector;
    private CompositeSubscription compositeSubscription;
    private Subscriber<ActivitySummaryGoal> activitySummarySubscriber;
    private MainActivityPresenter mainActivityPresenter;

    private long startSyncTime;
    private long endSyncTime;

    private Context context;
    private SimpleDateFormat readableFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

    JSONArray tataAIG_sync_data = new JSONArray();
    private String memberId;
    private boolean syncWithTataAIGServerOnly;

    private SharedPrefUtil sharedPrefUtil;

    public SyncStepHelper(GoogleFitConnector connector, String baseUrl, String authToken, String tata_aig_baseURL, String tata_aig_authToken, String memberId, Context context) {
        this.googleFitConnector = connector;
        this.compositeSubscription = new CompositeSubscription();
        this.mainActivityPresenter = new MainActivityPresenter(baseUrl, authToken, tata_aig_baseURL, tata_aig_authToken, context);
        this.context = context;
        this.memberId = memberId;
        this.sharedPrefUtil = new SharedPrefUtil(context);
    }

    public void dailySync(long googleFitLastSync) {


        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date()); // compute start of the day for the timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // long startOfDay = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        final long endOfDay = cal.getTimeInMillis();


        long startTime = googleFitLastSync;
        Log.d(TAG, "GoogleFitLastSync: " + startTime);

        Calendar last30Days;
        last30Days = Calendar.getInstance();
        last30Days.setTime(new Date());
        last30Days.set(Calendar.HOUR_OF_DAY, 0);
        last30Days.set(Calendar.MINUTE, 0);
        last30Days.set(Calendar.SECOND, 0);
        last30Days.set(Calendar.MILLISECOND, 0);
        last30Days.add(Calendar.DATE, -15);


        if (startTime == 0) {

            startTime = last30Days.getTimeInMillis();
        } else {

            int noOfDays = DateHelper.getDifferenceBetweenTwoDays(startTime, last30Days.getTimeInMillis());

            if (noOfDays > 15) {
                startTime = last30Days.getTimeInMillis();
            } else {

                // If the user has not updated his steps count for the first challenge then manually update his steps
                // This will update the step count for his previous 10 days
                // First challenge went live on 15th, so assuming that the user updates the app on 25th also, it will update his step count
                Calendar calendar;
                calendar = Calendar.getInstance();
                calendar.setTimeInMillis(startTime);
                calendar.add(Calendar.DATE, -1);
                startTime = calendar.getTimeInMillis();

            }
        }

        getSyncData(startTime, endOfDay);
        Log.d(TAG, "Start Time: " + startTime);
        Log.d(TAG, "End Of day: " + endOfDay);

        startSyncTime = startTime;
        endSyncTime = endOfDay;


    }

    private void getSyncData(long start, long end) {
        Log.d(TAG, "startTime: " + start + ", endTime: " + end);
        Log.d(TAG, "getSyncData: ");
        activitySummarySubscriber = new Subscriber<ActivitySummaryGoal>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(ActivitySummaryGoal goals) {
                Log.d(TAG, "onNext: " + goals.toString());


                showActivitySummary(goals);


                onCompleted();
            }
        };


        if (googleFitConnector != null) {
            Observable.zip(
                            googleFitConnector.getWeeklySteps(start, end),
                            googleFitConnector.getWeeklyDistance(start, end),
                            googleFitConnector.getWeeklyCalories(start, end),
                            googleFitConnector.getSleepForWeek(start, GoogleFitConnector.getDifferenceBetweenTwoDays(start, end)),
                            new Func4<HealthDataGraphValues, HealthDataGraphValues, HealthDataGraphValues, HealthDataGraphValues, ActivitySummaryGoal>() {
                                @Override
                                public ActivitySummaryGoal call(HealthDataGraphValues healthDataGraphValues, HealthDataGraphValues healthDataGraphValues2, HealthDataGraphValues healthDataGraphValues3, HealthDataGraphValues healthDataGraphValues4) {
                                    ActivitySummaryGoal activitySummaryGoal = new ActivitySummaryGoal(healthDataGraphValues, healthDataGraphValues2, healthDataGraphValues3, healthDataGraphValues4);
                                    return activitySummaryGoal;
                                }

                            }
                    )
                    .flatMap(new Func1<ActivitySummaryGoal, Observable<ActivitySummaryGoal>>() {
                        @Override
                        public Observable<ActivitySummaryGoal> call(ActivitySummaryGoal activitySummaryGoal) {
                            return Observable.just(activitySummaryGoal);
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(activitySummarySubscriber);
            compositeSubscription.add(activitySummarySubscriber);
        }
    }

    private void showActivitySummary(ActivitySummaryGoal goals) {
        ArrayList<Integer> arrayListCalories = new ArrayList<>();
        if (goals.calorie.getValues() == null) {
            for (int i = 0; i < goals.steps.getValues().size(); i++)
                arrayListCalories.add(i);
            goals.calorie.setValues(arrayListCalories);
        }


        ArrayList<Integer> arrayListDistance = new ArrayList<>();
        if (goals.distance.getValues() == null) {
            for (int i = 0; i < goals.distance.getValues().size(); i++)
                arrayListDistance.add(i);
            goals.distance.setValues(arrayListDistance);
        }


        final JsonObject postBody = new JsonObject();

        JsonArray jsonArray = new JsonArray();
        List<Long> time = DateHelper.convertSessioninDays(startSyncTime, endSyncTime);

        for (int i = 0; i < time.size(); i++) {

            long totalValue = 0;
            for (int j = 0; j < goals.steps.getActivitySession().size(); j++) {

                if (DateHelper.compareTime(time.get(i), TimeUnit.SECONDS.toMillis(goals.steps.getActivitySession().get(j).getSessionStart()))) {

                    if (i + 1 < time.size()) {
                        if ((DateHelper.compareTime(TimeUnit.SECONDS.toMillis(goals.steps.getActivitySession().get(j).getSessionEnd()), time.get(i + 1))))
                            totalValue = totalValue + goals.steps.getActivitySession().get(j).getValue();
                    } else {
                        totalValue = totalValue + goals.steps.getActivitySession().get(j).getValue();
                    }
                }
            }

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("steps", goals.steps.getValues().get(i));
            jsonObject.addProperty("calorie", goals.calorie.getValues().get(i));
            jsonObject.addProperty("distance", goals.distance.getValues().get(i));
            jsonObject.addProperty("activity", totalValue);
            if (i < (time.size() - 1))
                jsonObject.addProperty("sleep", goals.sleep.getSleepCards().get(i).getStartSleepTime() + "-" + goals.sleep.getSleepCards().get(i).getEndSleepTime());
            jsonObject.addProperty("date", time.get(i));

            jsonArray.add(jsonObject);


        }

        postBody.add("fitnessData", jsonArray);
        postBody.addProperty("platform", "ANDROID");
        Log.d(TAG, "data sync api called:" + String.valueOf(postBody));
        sendData1(postBody);

    }

    private void sendData1(JsonObject payload) {
        compositeSubscription.add(
                mainActivityPresenter.sendData(payload)
                        .compose(Transformers.<ApiResponse>applySchedulers())
                        .subscribe(
                                new Action1<ApiResponse>() {
                                    @Override
                                    public void call(ApiResponse apiResponse) {
                                        // success
                                        Log.d("mytag", "uploaded successfully");
                                    }
                                },
                                new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        throwable.printStackTrace();
                                    }
                                }));
    }


    /**
     * this is used for hourly sync api
     * For hourly sync there are 2 cases:
     * 1. When user open our PWA, then we need to call Visit server first, then at last call, TATA AIG server api.
     * 2. When user directly open TATA AIG app, then we don't need to call Visit api, only fetch all the steps and call TATA AIG api.
     *
     * @param startTimeStamp is the epoch timestamp in the format 1645166569594
     */
    public void hourlySync(long startTimeStamp, boolean syncWithTataAIGServerOnly) {
        this.syncWithTataAIGServerOnly = syncWithTataAIGServerOnly;

        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(startTimeStamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        startTimeStamp = calendar.getTimeInMillis();
        Log.d(TAG, "onRunJob: " + startTimeStamp);
        Log.d(TAG, "onRunJob: " + readableFormat.format(startTimeStamp));
        // If difference between the timestamp and today is over 30 days, then only fetch recent 30 days data from google
        // And mark the job done once the 30 days data is synced
        // Each day needs to be run sequentially

        int diffDays = GoogleFitConnector.getDifferenceBetweenTwoDays(startTimeStamp, System.currentTimeMillis()) + 1;
        Log.d(TAG, "onRunJob: diffDays: " + diffDays);

        long endTimeStamp = 0;
        if (diffDays > 30) {
            //if diffDays are 30 or more, then we will sync only the recent 30 days.

            Calendar startCalendar = Calendar.getInstance();
            startCalendar.setTimeInMillis(System.currentTimeMillis());

            //doing to remove the hours passed in the today's date.
            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);

            startCalendar.add(Calendar.DATE, -30); //going back to 30 days.
            startTimeStamp = startCalendar.getTimeInMillis();

        }

        endTimeStamp = System.currentTimeMillis();

        Log.d(TAG, "startTimeStamp:" + readableFormat.format(startTimeStamp));
        Log.d(TAG, "endTimeStamp:" + readableFormat.format(endTimeStamp));


        JSONArray jsonArray = new JSONArray();

        syncDataForDay(startTimeStamp, endTimeStamp, context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<JSONObject>() {
                    @Override
                    public void onCompleted() {


                        if (!syncWithTataAIGServerOnly) {
                            Log.d(TAG, "jsonArray: " + jsonArray);

                            JSONObject finalJsonObject = new JSONObject();
                            try {
                                finalJsonObject.put("data", null);
                                finalJsonObject.put("bulkHealthData", jsonArray);
                                finalJsonObject.put("platform", "ANDROID");

                                Log.d(TAG, "Visit Hourly Sync finalRequest: " + finalJsonObject.toString());

                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }

                            syncHourlyDataWithVisit_Server(finalJsonObject);
                        }


                        Log.d(TAG, "********onCompleted**********: syncDataForDay: ");
                        //this is called after all the steps for the days are synced from startTimeStamp to endTimeStamp
                        //call the tataAIG api here.

                        JSONObject finalRequest = new JSONObject();
                        try {
                            finalRequest.put("member_id", String.valueOf(memberId));
                            finalRequest.put("data", tataAIG_sync_data);
                            Log.d(TAG, "tata AIG finalRequest: " + finalRequest.toString());
                            syncDateToTATA_Server(finalRequest);
                        } catch (Exception e) {
                            Log.d(TAG, "exception occured:" + e.getMessage());
                        }


                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(JSONObject jsonObject) {
//                        Log.d(TAG, "onNext: syncDataForDay: " + jsonObject);
                        jsonArray.put(jsonObject);
                    }
                });


        Log.d(TAG, "onRunJob: Finished");


    }

    private rx.Observable<JSONObject> syncDataForDay(long startime, long endTime, Context context) {

        // Make a list of observables with responses as true and false and run them serially
        // If any of the observables return false, then end the chain
        HashMap<Long, Long> days = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        ArrayList<StartEndDate> list = new ArrayList<>();

        int diffDays = GoogleFitConnector.getDifferenceBetweenTwoDays(startime, endTime) + 1;
        for (int i = 0; i < diffDays; i++) {
            calendar.setTimeInMillis(startime);
            calendar.add(Calendar.DATE, 1);
            long endDayTimeStamp = calendar.getTimeInMillis();
            days.put(startime, endDayTimeStamp);
            list.add(new StartEndDate(startime, endDayTimeStamp));
            startime = endDayTimeStamp;
        }

        return Observable.from(list)
                .concatMap(new Func1<StartEndDate, Observable<JSONObject>>() {
                    @Override
                    public Observable<JSONObject> call(StartEndDate startEndDate) {

                        return Observable.create(new Action1<Emitter<JSONObject>>() {
                            @Override
                            public void call(Emitter<JSONObject> emitter) {
                                getPayloadForDay(startEndDate.getStartTime(), startEndDate.getEndTime(), context)
                                        .subscribe(new Subscriber<JSONObject>() {
                                            @Override
                                            public void onCompleted() {
                                                Log.d(TAG, "onCompleted: inside creator: completed");
                                                emitter.onCompleted();
                                            }

                                            @Override
                                            public void onError(Throwable e) {
                                                e.printStackTrace();
                                                emitter.onError(e);
                                            }

                                            @Override
                                            public void onNext(JSONObject jsonObject) {
//                                                Log.d(TAG, "onNext: inside Creator: " + jsonObject);
                                                if (jsonObject != null) {
                                                    emitter.onNext(jsonObject);
                                                } else {
                                                    // If the previous API responded with an error, then stop the future API calls.
                                                    // This is required to make sure that the data is synced in order.
                                                    emitter.onError(new Throwable("API Responded Incorrectly"));
                                                }

                                            }
                                        });
                            }
                        }, Emitter.BackpressureMode.BUFFER);


                    }
                });


    }

    private Observable<JSONObject> getPayloadForDay(long start, long end, Context context) {
//
        Log.d(TAG, "Start: " + start + " End: " + end);
        Log.d(TAG, "getPayloadForDay: " + readableFormat.format(start) + " to " + readableFormat.format(end));

        return Observable.zip(
                googleFitConnector.getDailySteps(start, end),
                googleFitConnector.getDailyDistance(start, end),
                googleFitConnector.getDailyCalories(start, end),
                new Func3<HealthDataGraphValues, HealthDataGraphValues, HealthDataGraphValues, JSONObject>() {
                    @Override
                    public JSONObject call(HealthDataGraphValues steps, HealthDataGraphValues distance, HealthDataGraphValues calories) {
                        //Log.d(TAG, "getPayloadForDay: Response: " + readableFormat.format(start) + " to " + readableFormat.format(end));
                        //Log.d(TAG, "call: steps :" + steps.getValues());
                        //Log.d(TAG, "call: distance :" + distance.getValues());
                        //Log.d(TAG, "call: calories :" + calories.getValues());

                        JSONArray data_TATA_AIG = new JSONArray();
                        JSONObject payloadTATA_AIG = new JSONObject();
                        JSONObject jsonObject_TATA_AIG;

                        JSONArray data = new JSONArray();
                        JSONObject payload = new JSONObject();
                        JSONObject jsonObject;
                        try {
                            for (int i = 0; i < steps.getValues().size(); i++) {
                                jsonObject = new JSONObject();
                                jsonObject.put("st", steps.getValues().get(i));
                                jsonObject.put("c", calories.getValues().get(i));
                                jsonObject.put("d", distance.getValues().get(i));
                                jsonObject.put("h", i);
                                jsonObject.put("s", steps.getAppContributedToGoogleFitValues().get(i));

                                jsonObject_TATA_AIG = new JSONObject();
                                jsonObject_TATA_AIG.put("hour", i);
                                jsonObject_TATA_AIG.put("steps", steps.getValues().get(i));
                                jsonObject_TATA_AIG.put("calories", calories.getValues().get(i));


                                data.put(jsonObject);

                                //for tata_aig
                                data_TATA_AIG.put(jsonObject_TATA_AIG);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            payload.put("data", data);
                            payload.put("dt", start);


                            //for tata_aig
                            payloadTATA_AIG.put("activity_date", Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate().toString());
                            payloadTATA_AIG.put("activity_data", data_TATA_AIG);
                            tataAIG_sync_data.put(payloadTATA_AIG);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return payload;
                    }
                }

        );
    }

    private void syncHourlyDataWithVisit_Server(JSONObject jsonObject) {
        mainActivityPresenter.syncDayWithServer(jsonObject).subscribeOn(Schedulers.io())
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        Log.d("mytag", "Visit Hourly Data Sync Status: " + aBoolean);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }

    private void syncDateToTATA_Server(JSONObject jsonObject) {
        mainActivityPresenter.syncDayWithTATA_AIG_Server(jsonObject).subscribeOn(Schedulers.io())
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                        EventBus.getDefault()
                                .post(new MessageEvent(new VisitEventType.StepSyncError(throwable.getMessage())));

                        EventBus.getDefault()
                                .post(new MessageEvent(
                                                new VisitEventType.VisitCallBack(
                                                        "Sync steps and calories api failed",
                                                        throwable.getMessage()
                                                )
                                        )
                                );

                        throwable.printStackTrace();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        Log.d("mytag", "TATA AIG Sync Status: " + aBoolean);
                        if (aBoolean) {
                            Calendar calendar = Calendar.getInstance();
                            sharedPrefUtil.setTataAIGLastSyncTimeStamp(calendar.getTimeInMillis());

                            EventBus.getDefault()
                                    .post(new MessageEvent(
                                                    new VisitEventType.VisitCallBack(
                                                            "Sync steps and calories api called",
                                                            null
                                                    )
                                            )
                                    );
                        }

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });


    }

    public void sendHRAInCompleteStatusToTataAIG(JSONObject jsonObject) {
        mainActivityPresenter.sendHRAInCompleteStatus(jsonObject).subscribeOn(Schedulers.io())
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        Log.d("mytag", "TATA AIG Sync Status: " + aBoolean);

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }


}
