package com.getvisitapp.google_fit.healthConnect

import com.getvisitapp.google_fit.healthConnect.model.internal.MonthProperties
import timber.log.Timber
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object TimeUtil {

    //Tested
    fun Instant.convertToLocalDateTime(): LocalDateTime {
        return LocalDateTime.ofInstant(this, ZoneId.systemDefault())
    }

    //Tested
    fun LocalDateTime.convertLocalDateTimeToEpochMillis(): Long {
        return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    //Tested
    fun Instant.convertInstantToEpochMillis(): Long {
        return this.toEpochMilli()
    }

    fun Long.convertEpochMillisToLocalDateTime(): LocalDateTime {
        if (this == 0L) {
            throw Exception("Requested Epoch Timestamp shouldn't be zero")
        }

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
    }


    fun getSelectedDateStartTimeAndEndTimeInstant(selectedDate: LocalDate): Pair<Instant, Instant> {

        //12:00AM
        val startTime =
            LocalDateTime.of(selectedDate, LocalTime.MIN).atZone(ZoneId.systemDefault())
                .toInstant()

        //11:59PM
        val endTime =
            LocalDateTime.of(selectedDate, LocalTime.MAX).atZone(ZoneId.systemDefault())
                .toInstant()


        Timber.d("getSelectedDateStartTimeAndEndTimeInstant startTime: $startTime, endTime: $endTime")
        return Pair(startTime, endTime)
    }

    fun getWeekInstant(localDateTime: LocalDateTime): Pair<Instant, Instant> {

        //Monday
        val startTime =
            LocalDateTime.of(localDateTime.toLocalDate(), LocalTime.MIN)
                .atZone(ZoneId.systemDefault())
                .toInstant()

        //Sunday
        val endTime =
            LocalDateTime.of(localDateTime.toLocalDate().plusDays(7), LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toInstant()

        return Pair(startTime, endTime)
    }

    fun getFirstDayOfWeek(date: LocalDate): LocalDateTime {

        // Find the first day of the current week (Monday)
        val firstDayOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        // Combine with the start of the day (00:00) to get LocalDateTime
        val firstDayOfWeekDateTime = LocalDateTime.of(firstDayOfWeek, LocalTime.MIDNIGHT)
        return firstDayOfWeekDateTime
    }

    fun getMonthInstant(
        firstDayOfMonth: LocalDate,
        lastDayOfMonth: LocalDate
    ): Pair<Instant, Instant> {
        val startTime =
            LocalDateTime.of(firstDayOfMonth, LocalTime.MIN)
                .atZone(ZoneId.systemDefault())
                .toInstant()

        val endTime =
            LocalDateTime.of(lastDayOfMonth, LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toInstant()

        return Pair(startTime, endTime)
    }


    fun getMonthProperties(monthCount: Int, year: Int): MonthProperties {

        // Get the current date
        val currentDate = LocalDate.of(year, monthCount, 1)

        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        val lastDayOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth())
        val numberOfDaysInMonth = currentDate.lengthOfMonth()

        return MonthProperties(
            firstDayOfMonth = firstDayOfMonth,
            lastDayOfMonth = lastDayOfMonth,
            numberOfDaysInMonth = numberOfDaysInMonth
        )

    }
}