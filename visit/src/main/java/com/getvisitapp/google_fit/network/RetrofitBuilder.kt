package com.getvisitapp.google_fit.network

import android.content.Context
import androidx.annotation.Keep
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


@Keep
object RetrofitBuilder {


    fun getRetrofit(
        baseURL: String,
        applicationContext: Context,
        authToken: String,
        isTimeOutEnable: Boolean
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseURL)
            .client(getOkHttpClient(applicationContext, authToken, isTimeOutEnable))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun getOkHttpClient(
        applicationContext: Context,
        authToken: String,
        isTimeOutEnable: Boolean
    ): OkHttpClient {

        val builder = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .addInterceptor(Interceptor { chain ->
                val originalRequest = chain.request()
                val newRequest: Request.Builder = originalRequest.newBuilder()
                    .addHeader("Content-type", "application/json")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("platform", "ANDROID");

                if (authToken.isNotEmpty()) {
                    newRequest.addHeader("Authorization", authToken)
                }

                newRequest.method(originalRequest.method, originalRequest.body)

                chain.proceed(newRequest.build())
            })

        if (isTimeOutEnable) {
            //this is used for normal network calls
            builder.connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
        } else {
            //this is for file upload
            builder.connectTimeout(20, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .readTimeout(40, TimeUnit.MINUTES)
        }

        return builder.build()
    }


}
