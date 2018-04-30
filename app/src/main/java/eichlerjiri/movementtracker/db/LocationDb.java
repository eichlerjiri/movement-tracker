package eichlerjiri.movementtracker.db;

public class LocationDb {

    private final long ts;
    private final double lat;
    private final double lon;

    public LocationDb(long ts, double lat, double lon) {
        this.ts = ts;
        this.lat = lat;
        this.lon = lon;
    }

    public long getTs() {
        return ts;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
