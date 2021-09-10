package eichlerjiri.movementtracker.utils;

import eichlerjiri.mapcomponent.utils.ObjectList;
import static java.lang.Math.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Common {

    public static double lonToMercatorX(double lon) {
        return (lon + 180) / 360;
    }

    public static double latToMercatorY(double lat) {
        double y = log(tan((lat + 90) * (PI / 360)));
        return 0.5 - y / (2 * PI);
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double thetaRad = toRadians(lon1 - lon2);
        double lat1Rad = toRadians(lat1);
        double lat2Rad = toRadians(lat2);

        double d = sin(lat1Rad) * sin(lat2Rad) + cos(lat1Rad) * cos(lat2Rad) * cos(thetaRad);
        double ret = toDegrees(acos(d)) * 60 * 1.1515 * 1.609344 * 1000;
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

    public static ObjectList<String> splitNonEmpty(String delimiter, String value) {
        ObjectList<String> ret = new ObjectList<>(String.class);

        int startIndex = 0;
        int index;
        while ((index = value.indexOf(delimiter, startIndex)) != -1) {
            addIfNonEmpty(ret, value.substring(startIndex, index));
            startIndex = index + delimiter.length();
        }
        addIfNonEmpty(ret, value.substring(startIndex));

        return ret;
    }

    public static void addIfNonEmpty(ObjectList<String> ret, String value) {
        value = value.trim();
        if (!value.isEmpty()) {
            ret.add(value);
        }
    }

    public static String formatDateTime(long timestamp) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(timestamp);
        return cal.get(Calendar.DAY_OF_MONTH)
                + "." + (cal.get(Calendar.MONTH) + 1)
                + "." + formatToSize(cal.get(Calendar.YEAR), 4)
                + " " + formatToSize(cal.get(Calendar.HOUR_OF_DAY), 2)
                + ":" + formatToSize(cal.get(Calendar.MINUTE), 2);
    }

    public static String formatDateShort(long timestamp) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(timestamp);
        return formatToSize(cal.get(Calendar.YEAR), 4)
                + formatToSize(cal.get(Calendar.MONTH) + 1, 2)
                + formatToSize(cal.get(Calendar.DAY_OF_MONTH), 2);
    }

    public static String formatDateTimeISOUTC(long timestamp) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(timestamp);
        return formatToSize(cal.get(Calendar.YEAR), 4)
                + "-" + formatToSize(cal.get(Calendar.MONTH) + 1, 2)
                + "-" + formatToSize(cal.get(Calendar.DAY_OF_MONTH), 2)
                + "T" + formatToSize(cal.get(Calendar.HOUR_OF_DAY), 2)
                + ":" + formatToSize(cal.get(Calendar.MINUTE), 2)
                + ":" + formatToSize(cal.get(Calendar.SECOND), 2)
                + "Z";
    }

    public static String formatTime(long timestamp) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(timestamp);
        return formatToSize(cal.get(Calendar.HOUR_OF_DAY), 2)
                + ":" + formatToSize(cal.get(Calendar.MINUTE), 2);
    }

    public static boolean isSameDay(long millis1, long millis2) {
        GregorianCalendar cal1 = new GregorianCalendar();
        cal1.setTimeInMillis(millis1);

        GregorianCalendar cal2 = new GregorianCalendar();
        cal2.setTimeInMillis(millis2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static String formatCoord(double coord) {
        return formatDecimal(coord, 6);
    }

    public static String formatAccuracy(float accuracy) {
        return round(accuracy) + "m";
    }

    public static String formatDuration(long millis) {
        long secondsFull = millis / 1000;
        long seconds = secondsFull % 60;
        long minutesFull = secondsFull / 60;
        long minutes = minutesFull % 60;
        long hours = minutesFull / 60;

        return formatToSize(hours, 2) + ":" + formatToSize(minutes, 2) + ":" + formatToSize(seconds, 2);
    }

    public static String formatDistance(double distance) {
        if (distance >= 1000) {
            return formatDecimal(distance / 1000, 2) + "km";
        } else {
            return round(distance) + "m";
        }
    }

    public static String formatSpeed(double speed) {
        return formatDecimal(speed * 3.6, 2) + "km/h";
    }

    public static String formatToSize(long num, int size) {
        String str = Long.toString(num);
        if (str.length() > size) {
            return str.substring(0, size);
        } else {
            while (str.length() < size) {
                str = "0" + str;
            }
        }
        return str;
    }

    public static String formatDecimal(double value, int decimalPlaces) {
        long pow = 10;
        for (int i = 0; i < decimalPlaces; i++) {
            pow *= 10;
        }

        long leftPart = (long) floor(value);
        long rightPart = round((value - leftPart) * pow);

        return leftPart + "." + formatToSize(rightPart, decimalPlaces);
    }
}
