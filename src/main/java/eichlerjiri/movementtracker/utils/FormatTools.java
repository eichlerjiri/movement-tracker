package eichlerjiri.movementtracker.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FormatTools {

    public Date date;
    public SimpleDateFormat formatDateTime;
    public SimpleDateFormat formatDateTimeISO;
    public SimpleDateFormat formatDateTimeUTC;
    public SimpleDateFormat formatTime;

    public Calendar cal1;
    public Calendar cal2;

    public DecimalFormatSymbols symbols;
    public DecimalFormat formatCoord;
    public DecimalFormat formatOne;
    public DecimalFormat formatTwo;
    public DecimalFormat formatTwoDec;

    public String formatDateTime(long millis) {
        if (formatDateTime == null) {
            formatDateTime = prepareDateFormat("d.M.yyyy HH:mm");
        }
        return format(formatDateTime, millis);
    }

    public String formatDateTimeISO(long millis) {
        if (formatDateTimeISO == null) {
            formatDateTimeISO = prepareDateFormat("yyyy-MM-dd HH:mm:ss");
        }
        return format(formatDateTimeISO, millis);
    }

    public String formatDateTimeUTC(long millis) {
        if (formatDateTimeUTC == null) {
            formatDateTimeUTC = prepareDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            formatDateTimeUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        return format(formatDateTimeUTC, millis);
    }

    public String formatTime(long millis) {
        if (formatTime == null) {
            formatTime = prepareDateFormat("HH:mm");
        }
        return format(formatTime, millis);
    }

    public static SimpleDateFormat prepareDateFormat(String template) {
        return new SimpleDateFormat(template, Locale.US);
    }

    public String format(SimpleDateFormat format, long millis) {
        if (date == null) {
            date = new Date();
        }
        date.setTime(millis);
        return format.format(date);
    }

    public boolean isSameDay(long millis1, long millis2) {
        if (cal1 == null) {
            cal1 = Calendar.getInstance(Locale.US);
            cal2 = Calendar.getInstance(Locale.US);
        }
        cal1.setTimeInMillis(millis1);
        cal2.setTimeInMillis(millis2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public String formatCoord(double coord) {
        if (formatCoord == null) {
            formatCoord = prepareDecimalFormat("0.000000");
        }
        return formatCoord.format(coord);
    }

    public String formatAccuracy(float accuracy) {
        if (formatOne == null) {
            formatOne = prepareDecimalFormat("0");
        }
        return formatOne.format(accuracy) + "m";
    }

    public String formatDuration(long millis) {
        long secondsFull = millis / 1000;
        long seconds = secondsFull % 60;
        long minutesFull = secondsFull / 60;
        long minutes = minutesFull % 60;
        long hours = minutesFull / 60;

        if (formatTwo == null) {
            formatTwo = prepareDecimalFormat("00");
        }
        return formatTwo.format(hours) + ":" + formatTwo.format(minutes) + ":" + formatTwo.format(seconds);
    }

    public String formatDistance(double distance) {
        if (distance >= 1000) {
            if (formatTwoDec == null) {
                formatTwoDec = prepareDecimalFormat("0.00");
            }
            return formatTwoDec.format(distance / 1000) + "km";
        } else {
            if (formatOne == null) {
                formatOne = prepareDecimalFormat("0");
            }
            return formatOne.format(distance) + "m";
        }
    }

    public String formatSpeed(double speed) {
        if (formatTwoDec == null) {
            formatTwoDec = prepareDecimalFormat("0.00");
        }
        return formatTwoDec.format(speed * 3.6) + "km/h";
    }

    public DecimalFormat prepareDecimalFormat(String format) {
        if (symbols == null) {
            symbols = new DecimalFormatSymbols(Locale.US);
        }
        return new DecimalFormat(format, symbols);
    }
}
