package eichlerjiri.movementtracker;

import android.content.Context;
import android.location.Location;

import java.util.ArrayList;

import eichlerjiri.movementtracker.utils.Failure;

public class Model {

    private static final Model instance = new Model();

    public static Model getInstance() {
        return instance;
    }

    private TrackingService trackingService;
    private final ArrayList<MovementTracker> movementTrackers = new ArrayList<>();
    private final ArrayList<MovementDetail> movementDetails = new ArrayList<>();

    private Database database;

    private Location lastLocation;
    private long activeRecording;
    private String activeRecordingType = "";

    public void registerTrackingService(TrackingService service) {
        trackingService = service;
    }

    public void unregisterTrackingService() {
        trackingService = null;
    }

    public void registerMovementTracker(MovementTracker movementTracker) {
        movementTrackers.add(movementTracker);
    }

    public void unregisterMovementTracker(MovementTracker movementTracker) {
        movementTrackers.remove(movementTracker);
    }

    public void registerMovementDetail(MovementDetail movementDetail) {
        movementDetails.add(movementDetail);
    }

    public void unregisterMovementDetail(MovementDetail movementDetail) {
        movementDetails.remove(movementDetail);
    }

    public Context getAnyContext() throws Failure {
        if (!movementTrackers.isEmpty()) {
            return movementTrackers.get(0);
        } else if (!movementDetails.isEmpty()) {
            return movementDetails.get(0);
        } else if (trackingService != null) {
            return trackingService;
        } else {
            throw new Failure("No context found.");
        }
    }

    public Database getDatabase() throws Failure {
        if (database == null) {
            database = new Database();
        }
        return database;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public String getActiveRecordingType() {
        return activeRecordingType;
    }

    public void locationArrived(Location location) throws Failure {
        lastLocation = location;
        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.lastLocationUpdated();
        }

        // nepouzivam location cas, zajima me soucasny cas zarizeni
        getDatabase().saveLocation(System.currentTimeMillis(), location.getLatitude(), location.getLongitude());
    }

    public void startRecording(String movementType) throws Failure {
        activeRecording = getDatabase().startRecording(System.currentTimeMillis(), movementType);
        activeRecordingType = movementType;
    }

    public void stopRecording() throws Failure {
        if (activeRecording > 0) {
            getDatabase().stopRecording(System.currentTimeMillis(), activeRecording);
            activeRecording = 0;
            activeRecordingType = "";
            for (MovementTracker movementTracker : movementTrackers) {
                movementTracker.recordingStopped();
            }
        }
    }
}
