package com.getvisitapp.google_fit.util

import androidx.annotation.Keep


@Keep
open class SingletonHolderForTwoVariable<out T: Any, in A,in B>(creator: (A, B) -> T) {
    private var creator: ((A,B) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A,args:B): T {
        val checkInstance = instance
        if (checkInstance != null) {
            return checkInstance
        }

        return synchronized(this) {
            val checkInstanceAgain = instance
            if (checkInstanceAgain != null) {
                checkInstanceAgain
            } else {
                val created = creator!!(arg,args)
                instance = created
                creator = null
                created
            }
        }
    }
}