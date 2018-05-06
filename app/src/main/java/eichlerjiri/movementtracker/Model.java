package eichlerjiri.movementtracker;

import android.content.Context;
import android.location.Location;

import java.util.ArrayList;

import eichlerjiri.movementtracker.utils.Failure;
import eichlerjiri.movementtracker.utils.GeoUtils;

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
    private boolean receivingLocations;
    private final ArrayList<MovementTracker> startedMovementTrackers = new ArrayList<>();

    private String activeRecordingType = "";

    private long activeRecording;
    private long activeTsFrom;
    private long activeTsTo;
    private long activeLocations;
    private double activeDistance;

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

    public boolean isReceivingLocations() {
        return receivingLocations;
    }

    public long getActiveTsFrom() {
        return activeTsFrom;
    }

    public long getActiveTsTo() {
        return activeTsTo;
    }

    public long getActiveLocations() {
        return activeLocations;
    }

    public double getActiveDistance() {
        return activeDistance;
    }

    public void locationArrived(Location location) throws Failure {
        long now = System.currentTimeMillis();

        activeTsTo = now;
        if (activeLocations != 0) {
            activeDistance += GeoUtils.distance(lastLocation.getLatitude(), lastLocation.getLongitude(),
                    location.getLatitude(), location.getLongitude());
        }
        activeLocations++;

        // nepouzivam location cas, zajima me soucasny cas zarizeni
        getDatabase().saveLocation(now, location.getLatitude(), location.getLongitude());
        lastLocation = location;

        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.lastLocationUpdated();
        }
    }

    public void startRecording(String movementType) throws Failure {
        long now = System.currentTimeMillis();

        activeRecording = getDatabase().startRecording(now, movementType);
        activeRecordingType = movementType;

        activeTsFrom = now;
        activeTsTo = now;
        activeLocations = 0;
        activeDistance = 0.0;

        refreshReceiving();

        if (trackingService != null) {
            trackingService.startRecording();
        }
        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.recordingStarted();
        }
    }

    public void stopRecording() throws Failure {
        if (activeDistance != 0) {
            getDatabase().finishRecording(System.currentTimeMillis(), activeRecording);
        } else {
            getDatabase().deleteRecording(activeRecording);
        }
        activeRecordingType = "";

        refreshReceiving();

        if (trackingService != null) {
            trackingService.stopRecording();
        }
        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.recordingStopped();
        }
    }

    public void startedMovementTracker(MovementTracker movementTracker) {
        startedMovementTrackers.add(movementTracker);
        refreshReceiving();
    }

    public void stoppedMovementTracker(MovementTracker movementTracker) {
        startedMovementTrackers.remove(movementTracker);
        refreshReceiving();
    }

    private void refreshReceiving() {
        boolean requested = !startedMovementTrackers.isEmpty() || !activeRecordingType.isEmpty();

        if (requested && !receivingLocations) {
            receivingLocations = true;
            if (trackingService != null) {
                trackingService.startReceiving();
            }
        } else if (!requested && receivingLocations) {
            receivingLocations = false;
            if (trackingService != null) {
                trackingService.stopReceiving();
            }
        }
    }
}
