package eichlerjiri.movementtracker.utils;

public class GeoBoundary {

    private double minLat = Double.MAX_VALUE;
    private double minLon = Double.MAX_VALUE;
    private double maxLat = Double.MIN_VALUE;
    private double maxLon = Double.MIN_VALUE;

    public GeoBoundary() {
    }

    public GeoBoundary(GeoBoundary geoBoundary) {
        minLat = geoBoundary.minLat;
        minLon = geoBoundary.minLon;
        maxLat = geoBoundary.maxLat;
        maxLon = geoBoundary.maxLon;
    }

    public void addPoint(double lat, double lon) {
        minLat = Math.min(minLat, lat);
        maxLat = Math.max(maxLat, lat);
        if (minLon == Double.MAX_VALUE) {
            minLon = lon;
            maxLon = lon;
        } else if (!isLonContained(lon)) {
            if (normalizeLonDiff(minLon, lon) < normalizeLonDiff(lon, maxLon)) {
                minLon = lon;
            } else {
                maxLon = lon;
            }
        }
    }

    private boolean isLonContained(double lon) {
        return minLon <= maxLon ? minLon <= lon && lon <= maxLon : minLon <= lon || lon <= maxLon;
    }

    private double normalizeLonDiff(double lon1, double lon2) {
        return (lon1 - lon2 + 360.0) % 360.0;
    }

    public double getMinLat() {
        return minLat;
    }

    public double getMinLon() {
        return minLon;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public double getMaxLon() {
        return maxLon;
    }
}
