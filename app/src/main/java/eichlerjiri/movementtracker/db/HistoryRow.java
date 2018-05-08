package eichlerjiri.movementtracker.db;

public class HistoryRow {

    public final long id;
    public final long ts;
    public final long tsEnd;
    public final String movementType;
    public final double distance;

    public HistoryRow(long id, long ts, long tsEnd, String movementType, double distance) {
        this.id = id;
        this.ts = ts;
        this.tsEnd = tsEnd;
        this.movementType = movementType;
        this.distance = distance;
    }
}
