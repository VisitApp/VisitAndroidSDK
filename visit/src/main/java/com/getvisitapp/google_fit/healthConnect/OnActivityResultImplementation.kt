package com.getvisitapp.google_fit.healthConnect

//https://stackoverflow.com/a/75144449/9469119
interface OnActivityResultImplementation<S, T> {
    fun execute(granted: T): S
}