package eichlerjiri.movementtracker.utils;

import static eichlerjiri.mapcomponent.utils.Common.*;
import eichlerjiri.mapcomponent.utils.ObjectList;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import static java.lang.Math.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import javax.net.ssl.HttpsURLConnection;

public class Common {

    public static String exceptionToString(Throwable t) {
        String ret = t.getMessage();
        if (ret == null) {
            ret = t.getClass().getCanonicalName();
        }
        return ret;
    }

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

    public static String formatDateTimeShort(long timestamp) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(timestamp);
        return formatToSize(cal.get(Calendar.YEAR), 4)
                + formatToSize(cal.get(Calendar.MONTH) + 1, 2)
                + formatToSize(cal.get(Calendar.DAY_OF_MONTH), 2)
                + "-" + formatToSize(cal.get(Calendar.HOUR_OF_DAY), 2)
                + formatToSize(cal.get(Calendar.MINUTE), 2)
                + formatToSize(cal.get(Calendar.SECOND), 2);
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

    public static String formatDateTimeCzech(long timestamp) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(timestamp);
        return cal.get(Calendar.DAY_OF_MONTH)
                + "." + (cal.get(Calendar.MONTH) + 1)
                + "." + formatToSize(cal.get(Calendar.YEAR), 4)
                + " " + formatToSize(cal.get(Calendar.HOUR_OF_DAY), 2)
                + ":" + formatToSize(cal.get(Calendar.MINUTE), 2);
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

    public static String formatHexToSize(int num, int size) {
        String str = Integer.toHexString(num);
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
        long pow = 1;
        for (int i = 0; i < decimalPlaces; i++) {
            pow *= 10;
        }
        long full = round(value * pow);

        return (full / pow) + "." + formatToSize(full % pow, decimalPlaces);
    }

    public static byte[] strToBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static String bytesToHexStr(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            sb.append(formatHexToSize(data[i] & 0xFF, 2));
        }
        return sb.toString();
    }

    public static byte[] randomBytes(int length) {
        byte[] random = new byte[length];
        new SecureRandom().nextBytes(random);
        return random;
    }

    public static boolean equalsByteArrayAt(byte[] array, byte[] target, int index) {
        for (int i = 0; i < target.length; i++) {
            if (array[index + i] != target[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsByteArray(byte[] array, byte[] target) {
        for (int i = 0; i < array.length - target.length + 1; i++) {
            if (equalsByteArrayAt(array, target, i)) {
                return true;
            }
        }
        return false;
    }

    public static String uploadMultipartFile(String url, byte[] file, String filename) throws InterruptedIOException {
        if (filename.contains("\"") || filename.contains("\r") || filename.contains("\n")) {
            return "Invalid filename";
        }

        String boundary;
        do {
            boundary = "----------------" + bytesToHexStr(randomBytes(8));
        } while (containsByteArray(file, strToBytes(boundary)));

        try {
            URLConnection urlConn = new URL(url).openConnection();
            if (!(urlConn instanceof HttpURLConnection)) {
                return "Unknown protocol";
            }
            HttpURLConnection conn = (HttpURLConnection) urlConn;

            if (conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(prepareSSLSocketFactory());
            }

            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(strToBytes("--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
                        "\r\n"));
                os.write(file);
                os.write(strToBytes("\r\n--" + boundary + "--\r\n"));
            }

            readHTTPResponse(conn);
            int code = conn.getResponseCode();
            if (code != 200) {
                return code + ": " + conn.getResponseMessage();
            }
            return "";
        } catch (InterruptedIOException e) {
            throw e;
        } catch (UnknownHostException e) {
            return "Unknown hostname";
        } catch (IOException e) {
            return exceptionToString(e);
        }
    }
}
