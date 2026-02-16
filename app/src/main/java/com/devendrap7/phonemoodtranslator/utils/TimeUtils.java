package com.devendrap7.phonemoodtranslator.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

    public static long getMidnightMillis() {
        // Automatically detects if the user is in NYC, London, or Delhi
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        return sdf.format(new Date());
    }
}