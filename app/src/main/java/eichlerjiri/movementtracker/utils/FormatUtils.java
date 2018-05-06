package eichlerjiri.movementtracker.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {

    public static String formatDate(long millis) {
        SimpleDateFormat formatter = new SimpleDateFormat("d.M.yyyy HH:mm", Locale.US);
        return formatter.format(new Date(millis));
    }

    public static String formatDuration(long millis) {
        long secondsFull = millis / 1000;
        long seconds = secondsFull % 60;
        long minutesFull = secondsFull / 60;
        long minutes = minutesFull % 60;
        long hours = minutesFull / 60;

        DecimalFormat df = new DecimalFormat("00", new DecimalFormatSymbols(Locale.US));
        return df.format(hours) + ":" + df.format(minutes) + ":" + df.format(seconds);
    }

    public static String formatDistance(double distance) {
        if (distance >= 1000) {
            DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
            return df.format(distance / 1000) + "km";
        } else {
            DecimalFormat df = new DecimalFormat("0", new DecimalFormatSymbols(Locale.US));
            return df.format(distance) + "m";
        }
    }

    public static String formatSpeed(double speed) {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        return df.format(speed * 3.6) + "km/h";
    }
}
