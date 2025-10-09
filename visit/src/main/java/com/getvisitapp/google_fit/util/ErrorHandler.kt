package com.getvisitapp.google_fit.util

import androidx.annotation.Keep
import com.getvisitapp.google_fit.model.ErrorModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.lang.reflect.Type

@Keep
class ErrorHandler {

    fun parseException(throwable: Throwable): String? {
        if (throwable is HttpException) {
            val body: ResponseBody? = (throwable as HttpException).response()?.errorBody();

            if (body != null) {
                try {
                    val type: Type = object : TypeToken<ErrorModel>() {}.type
                    val response: ErrorModel? = Gson().fromJson<ErrorModel>(body.string(), type)

                    if (response != null) {
                        if (response.status == 500 || response.status == 400) {
                            if (response.errorMessage != null) {
                                return response.errorMessage
                            }

                        }
                    }

                } catch (e: Exception) {
                }

            }
        }
        return null
    }
}