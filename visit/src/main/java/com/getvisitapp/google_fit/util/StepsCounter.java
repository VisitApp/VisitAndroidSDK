package com.getvisitapp.google_fit.util;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.getvisitapp.google_fit.view.GenericListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.lang.ref.SoftReference;

/**
 * Created by Ghost on 05/01/18.
 */

@Keep
public class StepsCounter {
    public static final String TAG = StepsCounter.class.getSimpleName();
    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    private static final int GM_SIGN_IN = 1900;
    private static StepsCounter sInstance;
    private SoftReference<Activity> softContext;
    private GoogleSignInAccount googleSignInAccount;
    private GoogleSignInClient mGoogleSignInClient;
    private GenericListener genericListener;
    FitnessOptions fitnessOptions;


    private StepsCounter(Activity activity) {
        this.softContext = new SoftReference<>(activity);
    }

    public static StepsCounter getInstance(Activity activity) {
        if (sInstance == null)
            return new StepsCounter(activity);
        return sInstance;
    }

    public void run(String defaultWebClientId, GenericListener genericListener) {
        //Log.d(TAG, "run: google fit");
        this.genericListener = genericListener;
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.EMAIL),
                        new Scope(Scopes.PROFILE),
                        new Scope(Scopes.PLUS_ME))
                .requestServerAuthCode(defaultWebClientId, false)
                .requestIdToken(defaultWebClientId)
                .build();

        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(softContext.get());

        if (googleSignInAccount != null) {
            mGoogleSignInClient = GoogleSignIn.getClient(softContext.get(), gso);
            askFitnessPermissions(googleSignInAccount, true);
        } else {
            mGoogleSignInClient = GoogleSignIn.getClient(softContext.get(), gso);
            mGoogleSignInClient.signOut();
            loginUsingGoogle();

        }

    }

    private void askFitnessPermissions(GoogleSignInAccount googleSignInAccount, boolean isLastSignedIn) {
        if (googleSignInAccount != null) {
            fitnessOptions = FitnessOptions.builder()
                    .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)

                    .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)

                    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)


                    .addDataType(DataType.TYPE_DISTANCE_DELTA)
                    .addDataType(DataType.AGGREGATE_DISTANCE_DELTA)
                    .build();
            if (!GoogleSignIn.hasPermissions(googleSignInAccount, fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                        softContext.get(),
                        REQUEST_OAUTH_REQUEST_CODE,
                        googleSignInAccount,
                        fitnessOptions);

            } else {
                handleFitnessPermission();
                if (isLastSignedIn) {
                    if (genericListener != null) {
                        genericListener.onJobDone(googleSignInAccount.getEmail());
                    }
                }
            }
        }
    }


    private void loginUsingGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        (softContext.get()).startActivityForResult(signInIntent, GM_SIGN_IN);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data, GenericListener genericListener1) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                //Log.d(TAG, "onActivity result: google fit 1");
                handleFitnessPermission();
            }
            if (requestCode == GM_SIGN_IN) {
                //Log.d(TAG, "onActivity result: google fit 2");
                askFitnessPermissions(handleGoogleSignIn(data), false);
                genericListener1.onJobDone("");
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (googleSignInAccount != null)
                genericListener.onJobDone(googleSignInAccount.getEmail());
        }
    }

    private void handleFitnessPermission() {
        //Log.d(TAG, "handleFitnessPermission: 1 : " + googleSignInAccount.getIdToken());
        //Log.d(TAG, "handleFitnessPermission: 2 : " + googleSignInAccount.getServerAuthCode());
        //Log.d(TAG, "handleFitnessPermission: 3 : " + googleSignInAccount.getGrantedScopes());
        try {
            subscribe();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private GoogleSignInAccount handleGoogleSignIn(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            return googleSignInAccount = task.getResult(ApiException.class);

        } catch (ApiException e) {
            e.printStackTrace();
            Log.w(TAG, "signInResult:failed code=" + e.getMessage());
        }
        return null;
    }

    private void subscribe() {
        recordFitnessData();
        Fitness.getRecordingClient(softContext.get(), googleSignInAccount)
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    //Log.d(TAG, "Successfully subscribed!");
                                } else {
                                    //Log.d(TAG, "There was a problem subscribing.", task.getException());
                                }
                            }
                        })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Log.d(TAG, "onSuccess: " + aVoid.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                        Crashlytics.logException(e);
                        e.printStackTrace();
                    }
                });


        Fitness.getRecordingClient(softContext.get(), googleSignInAccount)
                .subscribe(DataType.TYPE_ACTIVITY_SEGMENT)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    //Log.d(TAG, "Successfully subscribed!");
                                } else {
                                    //Log.d(TAG, "There was a problem subscribing.", task.getException());
                                }
                            }
                        })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Log.d(TAG, "onSuccess: " + aVoid.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                        Crashlytics.logException(e);
                        e.printStackTrace();
                    }
                });

        Fitness.getRecordingClient(softContext.get(), googleSignInAccount)
                .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    //Log.d(TAG, "Successfully subscribed!");
                                } else {
                                    //Log.d(TAG, "There was a problem subscribing.", task.getException());
                                }
                            }
                        })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Log.d(TAG, "onSuccess: " + aVoid.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                        Crashlytics.logException(e);
                        e.printStackTrace();
                    }
                });
    }

    public void recordFitnessData() {
        Log.d(TAG, "recordFitnessData: ALWAYS ON google fit recording API");

        Fitness.getRecordingClient(softContext.get(), GoogleSignIn.getAccountForExtension(softContext.get(), fitnessOptions))
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, " TYPE_STEP_COUNT_CUMULATIVE Existing subscription for activity detected.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "TYPE_STEP_COUNT_CUMULATIVE There was a problem subscribing.");
                    }
                });


        Fitness.getRecordingClient(softContext.get(), GoogleSignIn.getAccountForExtension(softContext.get(), fitnessOptions))
                .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "TYPE_STEP_COUNT_DELTA Successfully subscribed!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "TYPE_STEP_COUNT_DELTA There was a problem subscribing.");
                    }
                });


        Fitness.getRecordingClient(softContext.get(), GoogleSignIn.getAccountForExtension(softContext.get(), fitnessOptions))
                .subscribe(DataType.TYPE_ACTIVITY_SEGMENT)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "TYPE_ACTIVITY_SEGMENT Successfully subscribed!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "TYPE_ACTIVITY_SEGMENT There was a problem subscribing.");
                    }
                });

    }


    public boolean hasAccess() {

        if (GoogleSignIn.getLastSignedInAccount(softContext.get()) != null) {
            FitnessOptions fitnessOptions =
                    FitnessOptions.builder()
                            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)

                            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)

                            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)


                            .addDataType(DataType.TYPE_DISTANCE_DELTA)
                            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA)
                            .build();
            if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(softContext.get()), fitnessOptions)) {
                return false;
            } else {
                return true;
            }
        } else return false;

    }

}