package eichlerjiri.movementtracker.db;

public class HistoryItem {

    private final long id;
    private final String movementType;
    private final long tsFrom;
    private final long tsTo;

    public HistoryItem(long id, String movementType, long tsFrom, long tsTo) {
        this.id = id;
        this.movementType = movementType;
        this.tsFrom = tsFrom;
        this.tsTo = tsTo;
    }

    public long getId() {
        return id;
    }

    public String getMovementType() {
        return movementType;
    }

    public long getTsFrom() {
        return tsFrom;
    }

    public long getTsTo() {
        return tsTo;
    }
}
