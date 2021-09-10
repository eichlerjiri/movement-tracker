package eichlerjiri.movementtracker.utils;

import eichlerjiri.mapcomponent.utils.ObjectList;
import static java.lang.Math.*;

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
}
