package com.getvisitapp.google_fit.util;


import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Keep
public class DateHelper {

    public static final String TAG = DateHelper.class.getSimpleName();
   // private Calendar cal = Calendar.getInstance();

    public static long[] getWeeklyTime(Calendar cal){

        long startOfWeek = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        long endOfWeek = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -1);

        long [] time = new long[2];
        time[0] = startOfWeek;
        time[1] = endOfWeek;
        
        return time;
    }



    public static long[] getMonthlyTime(Calendar cal){

        long startOfMonth = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        long endOfMonth = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, -1);

        long [] time = new long[2];
        time[0] = startOfMonth;
        time[1] = endOfMonth;

        return time;
    }


    public static List<Long> convertSessioninHour(long sessionStart, long sessionEnd){

        List<Long> time = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(sessionStart);


        while (compareTime(cal.getTimeInMillis(), sessionEnd)){
            time.add(cal.getTimeInMillis());
            cal.add(Calendar.MINUTE , 60);
        }

        return  time;
    }


    public static List<Long> convertSessioninDays(long sessionStart, long sessionEnd){

        List<Long> time = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(sessionStart);


       while (compareTime(cal.getTimeInMillis(), sessionEnd)){
            time.add(cal.getTimeInMillis());
            cal.add(Calendar.DATE , 1);
        }

        return time;
    }


    public static boolean compareTime(long sessionStart, long sessionEnd){

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();

        cal1.setTimeInMillis(sessionStart);
        cal2.setTimeInMillis(sessionEnd);
        int i = cal2.compareTo(cal1);
        if (i > 0) return true;
        else if (i < 0) return false;
        else return false;

    }


    public static int getDifferenceBetweenTwoDays(long sessionStart, long sessionEnd){


        long difference = sessionEnd - sessionStart;
        int days = (int) (difference/ (1000*60*60*24));
        return days;
    }


    public static Date addDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }






}
