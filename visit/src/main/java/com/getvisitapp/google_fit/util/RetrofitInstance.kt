package com.getvisitapp.google_fit.util

import androidx.annotation.Keep
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Keep
class RetrofitInstance {

    companion object {
        val BASE_URL = "https://www.google.com"

        fun getRetroInstance(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
}