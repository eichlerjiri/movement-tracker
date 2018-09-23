package eichlerjiri.movementtracker.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Common {

    public static double lonToMercatorX(double lon) {
        return (lon + 180) / 360;
    }

    public static double latToMercatorY(double lat) {
        double y = Math.log(Math.tan((lat + 90) * (Math.PI / 360)));
        return 0.5 - y / (2 * Math.PI);
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double thetaRad = Math.toRadians(lon1 - lon2);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        double d = Math.sin(lat1Rad) * Math.sin(lat2Rad) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.cos(thetaRad);
        double ret = Math.toDegrees(Math.acos(d)) * 60 * 1.1515 * 1.609344 * 1000;
        if (Double.isNaN(ret)) {
            return 0;
        }
        return ret;
    }

    public static double avgSpeed(double distance, long duration) {
        if (duration == 0) {
            return 0;
        }
        return distance / (duration / 1000.0);
    }

    public static ArrayList<String> splitNonEmpty(String delimiter, String value) {
        ArrayList<String> ret = new ArrayList<>();

        int startIndex = 0;
        while (true) {
            int index = value.indexOf(delimiter, startIndex);
            if (index == -1) {
                addIfNonEmpty(ret, value.substring(startIndex));
                break;
            }

            addIfNonEmpty(ret, value.substring(startIndex, index));
            startIndex = index + delimiter.length();
        }

        return ret;
    }

    public static void addIfNonEmpty(ArrayList<String> ret, String value) {
        value = value.trim();
        if (!value.isEmpty()) {
            ret.add(value);
        }
    }

    public static String formatDateTime(long millis) {
        return format(millis, "d.M.yyyy HH:mm");
    }

    public static String formatDateTimeISO(long millis) {
        return format(millis, "yyyy-MM-dd HH:mm:ss");
    }

    public static String formatDateTimeTZ(long millis) {
        return formatUTC(millis, "yyyy-MM-dd'T'HH:mm:ss'Z'");
    }

    public static String formatTime(long millis) {
        return format(millis, "HH:mm");
    }

    public static String format(long millis, String template) {
        return new SimpleDateFormat(template, Locale.US).format(new Date(millis));
    }

    public static String formatUTC(long millis, String template) {
        SimpleDateFormat formatter = new SimpleDateFormat(template, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(new Date(millis));
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
        return prepareDecimalFormat("0.000000").format(coord);
    }

    public static String formatAccuracy(float accuracy) {
        return prepareDecimalFormat("0").format(accuracy) + "m";
    }

    public static String formatDuration(long millis) {
        long secondsFull = millis / 1000;
        long seconds = secondsFull % 60;
        long minutesFull = secondsFull / 60;
        long minutes = minutesFull % 60;
        long hours = minutesFull / 60;

        DecimalFormat df = prepareDecimalFormat("00");
        return df.format(hours) + ":" + df.format(minutes) + ":" + df.format(seconds);
    }

    public static String formatDistance(double distance) {
        if (distance >= 1000) {
            return prepareDecimalFormat("0.00").format(distance / 1000) + "km";
        } else {
            return prepareDecimalFormat("0").format(distance) + "m";
        }
    }

    public static String formatSpeed(double speed) {
        return prepareDecimalFormat("0.00").format(speed * 3.6) + "km/h";
    }

    public static DecimalFormat prepareDecimalFormat(String format) {
        return new DecimalFormat(format, new DecimalFormatSymbols(Locale.US));
    }
}
