package com.getvisitapp.google_fit.network

import androidx.annotation.Keep
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

@Keep
interface RetroServiceInterface {

    @GET
    fun downloadPdfFile(@Url pdfUrl: String, @Header("Authorization") authorization:String): Call<ResponseBody>
}