package eichlerjiri.movementtracker;

import android.location.Location;

import java.util.ArrayList;

public class Model {

    private static final Model instance = new Model();

    public static Model getInstance() {
        return instance;
    }

    private final ArrayList<MovementTracker> movementTrackers = new ArrayList<>();
    private TrackingService trackingService;

    private String status = "";
    private Location lastLocation;

    public void registerMovementTracker(MovementTracker movementTracker) {
        movementTrackers.add(movementTracker);
    }

    public void unregisterMovementTracker(MovementTracker movementTracker) {
        movementTrackers.remove(movementTracker);
    }

    public void registerTrackingService(TrackingService service) {
        trackingService = service;
    }

    public void unregisterTrackingService() {
        trackingService = null;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.statusUpdated();
        }
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.lastLocationUpdated();
        }
    }
}
