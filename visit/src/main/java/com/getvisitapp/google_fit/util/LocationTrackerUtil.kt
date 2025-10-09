package com.getvisitapp.google_fit.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat


@Keep
class LocationTrackerUtil(
    private var context: Context,
) {

    private var TAG = "LocationTrackerUtil"

    private lateinit var locationManager: LocationManager


    fun isGPSEnabled(): Boolean {
        //checking if GPS is enabled or not
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun isLocationPermissionAllowed(): Boolean {
        //checking if both fine and coarse location is present or not
        return (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }


    fun showGPS_NotEnabledDialog() {
        val alertDialog = AlertDialog.Builder(context)

        // Setting Dialog Title
        alertDialog.setTitle("Turn on GPS")

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?")

        // On pressing Settings button
        alertDialog.setPositiveButton(
            "Settings"
        ) { dialog, which ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(intent)
        }

        // on pressing cancel button
        alertDialog.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.cancel() }
        alertDialog.show()
    }

    fun showLocationPermissionDeniedAlertDialog() {
        val alertDialog = AlertDialog.Builder(context)

        // Setting Dialog Title
        alertDialog.setTitle("Allow Location Permission")

        // Setting Dialog Message
        alertDialog.setMessage("Location Permission is not enabled. Do you want to go to settings menu?")

        // On pressing Settings button
        alertDialog.setPositiveButton(
            "Settings"
        ) { dialog, which ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts(
                "package", context.applicationContext.packageName, null
            )
            intent.data = uri
            context.startActivity(intent)
        }

        // on pressing cancel button
        alertDialog.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.cancel() }
        alertDialog.show()
    }




}