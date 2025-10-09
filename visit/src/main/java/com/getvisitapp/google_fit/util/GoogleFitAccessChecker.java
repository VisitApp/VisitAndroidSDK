package com.getvisitapp.google_fit.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.getvisitapp.google_fit.data.SharedPrefUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * https://github.com/android/fit-samples/issues/28
 */

@Keep
public class GoogleFitAccessChecker {
    Context context;

    public GoogleFitAccessChecker(Context context) {
        this.context = context;
    }

    public void revokeGoogleFitPermission(String default_client_id) {
        Fitness.getConfigClient(context, GoogleSignIn.getAccountForExtension(context, getFitnessOptions()))
                .disableFit()
                .continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(@NonNull Task<Void> task) throws Exception {

                        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestScopes(new Scope(Scopes.EMAIL),
                                        new Scope(Scopes.PROFILE),
                                        new Scope(Scopes.PLUS_ME))
                                .requestServerAuthCode(default_client_id, false)
                                .requestIdToken(default_client_id)
                                .addExtension(getFitnessOptions())
                                .build();

                        GoogleSignIn.getClient(context, signInOptions)
                                .revokeAccess().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d("mytag", "googleFit permission revoked successfully");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        e.printStackTrace();
                                        Log.d("mytag", "googleFit permission revoked failed #1");
                                    }
                                });
                        return null;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("mytag", "googleFit permission revoked failed #2");
                    }
                });
    }

    public boolean checkGoogleFitAccess() {
        if (GoogleSignIn.getLastSignedInAccount(context) != null) {
            if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(context), getFitnessOptions())) {
                return false;
            } else {
                return true;
            }
        } else return false;
    }


    FitnessOptions getFitnessOptions() {
        return FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)

                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)

                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)



                .addDataType(DataType.TYPE_DISTANCE_DELTA)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA)
                .build();
    }

}
