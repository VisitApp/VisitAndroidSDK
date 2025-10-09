package com.getvisitapp.google_fit.presenter

import androidx.annotation.Keep
import com.getvisitapp.google_fit.network.ApiService
import com.getvisitapp.google_fit.util.ErrorHandler
import com.getvisitapp.google_fit.view.TwillioVideoView
import kotlinx.coroutines.*

@Keep
class TwillioVideoPresenter(var apiService: ApiService, var twillioVideoView: TwillioVideoView) {

    var TAG = this.javaClass.simpleName

    var coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
//        FirebaseCrashlytics.getInstance().recordException(throwable);

        val errorMessage: String? = ErrorHandler().parseException(throwable)

        CoroutineScope(Dispatchers.Main).launch {
            errorMessage?.let {
                twillioVideoView.setError(it)
            }
        }
    }

    fun getRoomDetails(sessionId: Int, consultationId: Int) {
        CoroutineScope(Dispatchers.IO).launch(coroutineExceptionHandler) {
            var response = apiService.getRoomDetails(sessionId, consultationId)

            withContext(Dispatchers.Main) {
                if (response.message == "success") {
                    twillioVideoView.roomDetails(response.roomDetails)
                } else {
                    response.errorMessage?.let {
                        twillioVideoView.setError(response.errorMessage)
                    }
                }
            }
        }
    }

}


