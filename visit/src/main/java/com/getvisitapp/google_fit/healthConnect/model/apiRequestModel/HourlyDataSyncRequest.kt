package com.getvisitapp.google_fit.healthConnect.model.apiRequestModel

import androidx.annotation.Keep
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.BulkHealthData


@Keep
data class HourlyDataSyncRequest(
    val bulkHealthData: List<BulkHealthData>,
    val platform: String
)


//st:steps
//c:calorie
//d:distance
//h:hour
//s:package name of the app which contributed record. Ex: `com.healthconnectexample`

//Ex: Request Body
/**
 * {
 *   "bulkHealthData": [
 *     {
 *       "data": [
 *         {
 *           "st": 0,
 *           "c": 61,
 *           "d": 0,
 *           "h": 0,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 61,
 *           "d": 0,
 *           "h": 1,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 61,
 *           "d": 0,
 *           "h": 2,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 61,
 *           "d": 0,
 *           "h": 3,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 61,
 *           "d": 0,
 *           "h": 4,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 61,
 *           "d": 0,
 *           "h": 5,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 61,
 *           "d": 0,
 *           "h": 6,
 *           "s": ""
 *         },
 *         {
 *           "st": 213,
 *           "c": 68,
 *           "d": 88,
 *           "h": 7,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 61,
 *           "d": 0,
 *           "h": 8,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 61,
 *           "d": 0,
 *           "h": 9,
 *           "s": ""
 *         },
 *         {
 *           "st": 171,
 *           "c": 66,
 *           "d": 66,
 *           "h": 10,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 11,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 12,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 13,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 14,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 15,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 16,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 17,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 18,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 19,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 20,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 21,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 22,
 *           "s": ""
 *         },
 *         {
 *           "st": 0,
 *           "c": 0,
 *           "d": 0,
 *           "h": 23,
 *           "s": ""
 *         }
 *       ],
 *       "dt": 1725042600519
 *     }
 *   ],
 *   "platform": "ANDROID"
 * }
 */