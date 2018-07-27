package eichlerjiri.movementtracker.db;

public class LocationRow {

    public final long ts;
    public final double lat;
    public final double lon;

    public LocationRow(long ts, double lat, double lon) {
        this.ts = ts;
        this.lat = lat;
        this.lon = lon;
    }
}
