package eichlerjiri.movementtracker.db;

public class LocationDb {

    private final double lat;
    private final double lon;

    public LocationDb(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
