package eichlerjiri.movementtracker.utils;

public class GeoUtils {

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
}
