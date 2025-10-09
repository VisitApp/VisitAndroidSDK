package com.getvisitapp.google_fit.util;

import android.content.Context;
import android.util.Log;

import com.getvisitapp.google_fit.pojo.ActivitySession;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static java.text.DateFormat.getTimeInstance;

import androidx.annotation.Keep;

/**
 * Created by shashvat on 12/05/18.
 */

@Keep
public class ActivityTimeHelper {

    private static final String TAG = "ActivityTimeHelper";

    public static Observable<List<ActivitySession>> getTotalActivityTime(final Context context, final long startTime, final long endTime) {
        return getActivitySegments(context, startTime, endTime)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .flatMap(new Func1<DataReadResponse, Observable<List<ActivitySession>>>() {
                    @Override
                    public Observable<List<ActivitySession>> call(DataReadResponse dataReadResponse) {
                        return getActivityTime(dataReadResponse);
                    }
                });
    }

    // This gives the average activity time in minutes averaged of @numberOfDays
    public static Observable<List<ActivitySession>> getAverageActivityTime(final Context context, final long startTime, final long endTime) {
        return getActivitySegments(context, startTime, endTime)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .flatMap(new Func1<DataReadResponse, Observable<List<ActivitySession>>>() {
                    @Override
                    public Observable<List<ActivitySession>> call(DataReadResponse dataReadResponse) {
                        return getActivityTime(dataReadResponse);
                    }
                }).flatMap(new Func1<List<ActivitySession>, Observable<List<ActivitySession>>>() {
                    @Override
                    public Observable<List<ActivitySession>> call(List<ActivitySession> activitySessions) {
                        return Observable.just(activitySessions);
                    }

//                    @Override
//                    public Observable<Integer> call(Integer integer) {
//                        return Observable.just(integer/numberOfDays);
//                    }
                });
    }

    private static Observable<DataReadResponse> getActivitySegments(final Context context, final long startTime, final long endTime) {
        return Observable.create(new Observable.OnSubscribe<DataReadResponse>() {
            @Override
            public void call(Subscriber<? super DataReadResponse> subscriber) {
//                GoogleApiClient client = new GoogleApiClient.Builder(context)
//                        .addApi(Fitness.RECORDING_API)
//                        .addApi(Fitness.HISTORY_API)
//                        .build();
//
//                client.connect();
//
                DataSource ESTIMATED_ACTIVITY_SEGMENT = new DataSource.Builder()
                        .setDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                        .setType(DataSource.TYPE_DERIVED)
                        .setStreamName("estimated_activity_segment")
                        .setAppPackageName("com.google.android.gms")
                        .build();
//
//                DataReadRequest readRequest =
//                        new DataReadRequest.Builder()
//                                .aggregate(ESTIMATED_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
//                                .bucketByActivitySegment(1, TimeUnit.SECONDS)
//                                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
//                                .build();

                DataReadRequest readRequest2 = new DataReadRequest.Builder()
                        .aggregate(ESTIMATED_ACTIVITY_SEGMENT)
                        .bucketByActivitySegment(1, TimeUnit.SECONDS)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();


//                DataReadResult result = Fitness.HistoryApi.readData(client, readRequest)
//                        .await(100, TimeUnit.SECONDS);


                Task<DataReadResponse> dataReadResponseTask = Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context))
                        .readData(readRequest2);

                DataReadResponse result2 = null;
                try {
                    result2 = Tasks.await(dataReadResponseTask, 100, TimeUnit.SECONDS);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

                if (result2.getStatus().isSuccess()) {
                    subscriber.onNext(result2);
                    subscriber.onCompleted();
                } else {
                    subscriber.onNext(null);
                    subscriber.onCompleted();
//                    Crashlytics.log("Something went wrong retrieving activity the day " + result.getStatus());
                    //  subscriber.onError(new Error("Something went wrong retrieving activity the day " + result.getStatus().getStatusMessage()));
                }
            }
        });
    }


    private static Observable<List<ActivitySession>> getActivityTime(DataReadResponse dataReadResponse) {
        List<ActivitySession> activitySessionsList = new ArrayList<>();
        long totalActivityTime = 0;
        if (dataReadResponse != null && dataReadResponse.getBuckets().size() > 0) {
//            Log.d(
//                    TAG, "Number of returned buckets of DataSets is: " + dataReadResponse.getBuckets().size());
            for (Bucket bucket : dataReadResponse.getBuckets()) {
                String activityType = bucket.getActivity();



//                Log.d(TAG, "getActivityType: " + bucket.getActivity());
                if (activityType.equalsIgnoreCase("walking")
                        || activityType.equalsIgnoreCase("running")
                        || activityType.equalsIgnoreCase("biking")) {

                    ActivitySession activitySession = new ActivitySession();
                    activitySession.setSession(bucket.getStartTime(TimeUnit.SECONDS), bucket.getEndTime(TimeUnit.SECONDS), bucket.getEndTime(TimeUnit.SECONDS) - bucket.getStartTime(TimeUnit.SECONDS));
                    // need to make json for data sync here
                    totalActivityTime += (bucket.getEndTime(TimeUnit.SECONDS) - bucket.getStartTime(TimeUnit.SECONDS));
                    activitySessionsList.add(activitySession);
                }
            }

            return Observable.just(activitySessionsList);
        }
        return Observable.just(activitySessionsList);
    }

    private static long calculateActivityTimeForDataSet(DataSet dataSet) {
        Log.d(TAG, "calculateActivityTimeForDataSet: Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = getTimeInstance();
        long totalActivityTime = 0;
        for (DataPoint dp : dataSet.getDataPoints()) {
//            Log.d(TAG, "Data point:");

            for (Field field : dp.getDataType().getFields()) {

                // https://developers.google.com/fit/rest/v1/reference/activity-types
                // 7 = walking, 8 = running, 2 = On foot, 1 = Biking
                Log.d(TAG, "calculateActivityTimeForDataSet: dp.getValue(field): " + dp.getValue(field).asString());
                if (dp.getValue(field).asInt() == 7 || dp.getValue(field).asInt() == 8 || dp.getValue(field).asInt() == 1) {
                    Log.d(TAG, "\tType: " + dp.getDataType().getName());
                    Log.d(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
                    Log.d(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                    Log.d(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));

                    Log.d(TAG, "calculateActivityTimeForDataSet: totalActivityMinutes: " +
                            (dp.getEndTime(TimeUnit.MINUTES) - dp.getStartTime(TimeUnit.MINUTES)));
                    totalActivityTime += (dp.getEndTime(TimeUnit.MINUTES) - dp.getStartTime(TimeUnit.MINUTES));
                }
            }
        }
        return totalActivityTime;
    }
}