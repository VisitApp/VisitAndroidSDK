package com.getvisitapp.google_fit.util

import android.content.Context
import androidx.annotation.Keep


@Keep
class GoogleFitExtension @JvmOverloads constructor(context: Context,weClientId:String="") :
    GoogleFitConnector(context,weClientId) {

    companion object :
        SingletonHolderForTwoVariable<GoogleFitExtension, Context, String>(::GoogleFitExtension)

}

