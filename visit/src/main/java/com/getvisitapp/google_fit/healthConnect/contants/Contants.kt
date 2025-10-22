package com.getvisitapp.google_fit.healthConnect.contants

object Contants {


    //This variable stores the information if the user have revoked the permission in android 14 and above,
    //because android 14, has a bug where even after revoking the permission is give the status of Health Connect as connected.
    //This prevent the Stay Active Page (where Google Fit and FitBit option comes) from showing Health Connect is connected
    //after user have revoke the permission and come back to that page.
    var previouslyRevoked: Boolean = false
}