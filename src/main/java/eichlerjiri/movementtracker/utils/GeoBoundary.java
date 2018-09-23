package eichlerjiri.movementtracker.utils;

public class GeoBoundary {

    public double minX = Double.MAX_VALUE;
    public double maxX = Double.MIN_VALUE;
    public double minY = Double.MAX_VALUE;
    public double maxY = Double.MIN_VALUE;

    public GeoBoundary() {
    }

    public GeoBoundary(GeoBoundary geoBoundary) {
        minX = geoBoundary.minX;
        maxX = geoBoundary.maxX;
        minY = geoBoundary.minY;
        maxY = geoBoundary.maxY;
    }

    public void addPoint(double x, double y) {
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
    }
}
