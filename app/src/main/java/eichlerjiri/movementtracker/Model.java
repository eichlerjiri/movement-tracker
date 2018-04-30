package eichlerjiri.movementtracker;

import android.content.Context;
import android.location.Location;

import java.util.ArrayList;

public class Model {

    private static final Model instance = new Model();

    public static Model getInstance() {
        return instance;
    }

    private TrackingService trackingService;
    private final ArrayList<MovementTracker> movementTrackers = new ArrayList<>();
    private final ArrayList<MovementDetail> movementDetails = new ArrayList<>();

    private Database database;

    private String locationStatus = "";
    private String databaseError = "";
    private Location lastLocation;
    private long activeRecording;
    private String activeRecordingType = "";

    public Context getContext() {
        if (trackingService != null) {
            return trackingService;
        } else if (!movementTrackers.isEmpty()) {
            return movementTrackers.get(0);
        } else {
            return movementDetails.get(0);
        }
    }

    public void registerTrackingService(TrackingService service) {
        trackingService = service;
        start();
    }

    public void unregisterTrackingService() {
        trackingService = null;
        stop();
    }

    public void registerMovementTracker(MovementTracker movementTracker) {
        movementTrackers.add(movementTracker);
        start();
    }

    public void unregisterMovementTracker(MovementTracker movementTracker) {
        movementTrackers.remove(movementTracker);
        stop();
    }

    public void registerMovementDetail(MovementDetail movementDetail) {
        movementDetails.add(movementDetail);
        start();
    }

    public void unregisterMovementDetail(MovementDetail movementDetail) {
        movementDetails.remove(movementDetail);
        stop();
    }

    private void start() {
        if (database == null) {
            database = new Database();
        }
    }

    private void stop() {
        if (database != null && trackingService == null && movementTrackers.isEmpty() && movementDetails.isEmpty()) {
            database.close();
            database = null;
        }
    }

    public Database getDatabase() {
        return database;
    }

    public String getLocationStatus() {
        return locationStatus;
    }

    public String getDatabaseError() {
        return databaseError;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public String getActiveRecordingType() {
        return activeRecordingType;
    }

    public void setLocationStatus(String locationStatus) {
        this.locationStatus = locationStatus;
        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.locationStatusUpdated();
        }
    }

    public void setDatabaseError(String databaseError) {
        if (!this.databaseError.equals(databaseError)) {
            this.databaseError = databaseError;
            for (MovementTracker movementTracker : movementTrackers) {
                movementTracker.databaseErrorUpdated();
            }
        }
    }

    public void locationArrived(Location location) {
        lastLocation = location;
        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.lastLocationUpdated();
        }

        // nepouzivam location cas, zajima me soucasny cas zarizeni
        database.saveLocation(System.currentTimeMillis(), location.getLatitude(), location.getLongitude());
    }

    public boolean startRecording(String movementType) {
        long id = database.startRecording(System.currentTimeMillis(), movementType);
        if (id > 0) {
            activeRecording = id;
            activeRecordingType = movementType;
            return true;
        } else {
            return false;
        }
    }

    public void stopRecording() {
        if (activeRecording > 0) {
            database.stopRecording(System.currentTimeMillis(), activeRecording);
            activeRecording = 0;
            activeRecordingType = "";
            for (MovementTracker movementTracker : movementTrackers) {
                movementTracker.recordingStopped();
            }
        }
    }
}
