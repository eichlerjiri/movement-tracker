package eichlerjiri.movementtracker.utils;

import static java.lang.Math.*;

public class GeoBoundary {

    public double minX = Double.POSITIVE_INFINITY;
    public double maxX = Double.NEGATIVE_INFINITY;
    public double minY = Double.POSITIVE_INFINITY;
    public double maxY = Double.NEGATIVE_INFINITY;

    public GeoBoundary() {
    }

    public GeoBoundary(GeoBoundary geoBoundary) {
        minX = geoBoundary.minX;
        maxX = geoBoundary.maxX;
        minY = geoBoundary.minY;
        maxY = geoBoundary.maxY;
    }

    public void addPoint(double x, double y) {
        minX = min(minX, x);
        maxX = max(maxX, x);
        minY = min(minY, y);
        maxY = max(maxY, y);
    }
}
