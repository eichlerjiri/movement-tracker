package eichlerjiri.movementtracker.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {

    public static String formatDateTime(long millis) {
        return format(millis, "d.M.yyyy HH:mm");
    }

    public static String formatDateTimeISO(long millis) {
        return format(millis, "yyyy-MM-dd HH:mm:ss");
    }

    public static String formatDateTimeTZ(long millis) {
        return format(millis, "yyyy-MM-dd'T'HH:mm:ss'Z'");
    }

    public static String formatTime(long millis) {
        return format(millis, "HH:mm");
    }

    private static String format(long millis, String template) {
        return new SimpleDateFormat(template, Locale.US).format(new Date(millis));
    }

    public static boolean isSameDay(long millis1, long millis2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(millis1);
        cal2.setTimeInMillis(millis2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static String formatCoord(double coord) {
        return new DecimalFormat("0.000000", symbols()).format(coord);
    }

    public static String formatAccuracy(float accuracy) {
        return new DecimalFormat("0", symbols()).format(accuracy) + "m";
    }

    public static String formatDuration(long millis) {
        long secondsFull = millis / 1000;
        long seconds = secondsFull % 60;
        long minutesFull = secondsFull / 60;
        long minutes = minutesFull % 60;
        long hours = minutesFull / 60;

        DecimalFormat df = new DecimalFormat("00", symbols());
        return df.format(hours) + ":" + df.format(minutes) + ":" + df.format(seconds);
    }

    public static String formatDistance(double distance) {
        if (distance >= 1000) {
            return new DecimalFormat("0.00", symbols()).format(distance / 1000) + "km";
        } else {
            return new DecimalFormat("0", symbols()).format(distance) + "m";
        }
    }

    public static String formatSpeed(double speed) {
        return new DecimalFormat("0.00", symbols()).format(speed * 3.6) + "km/h";
    }

    private static DecimalFormatSymbols symbols() {
        return new DecimalFormatSymbols(Locale.US);
    }
}
