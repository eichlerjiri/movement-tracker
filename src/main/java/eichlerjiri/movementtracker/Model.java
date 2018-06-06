package eichlerjiri.movementtracker;

import android.content.Context;
import android.location.Location;

import java.util.ArrayList;

import eichlerjiri.movementtracker.utils.Failure;
import eichlerjiri.mapcomponent.utils.GeoBoundary;
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

    private Location lastKnownLocation;
    private Location lastLocation;
    private boolean receivingLocations;
    private final ArrayList<MovementTracker> startedMovementTrackers = new ArrayList<>();

    private long activeRecording;
    private String activeRecordingType = "";

    private long activeTsFrom;
    private long activeTsTo;
    private long activeLocations;
    private double activeDistance;
    private GeoBoundary activeGeoBoundary;
    private Location lastRecordedLocation;

    private int notificationCounter;

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

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public String getActiveRecordingType() {
        return activeRecordingType;
    }

    public long getActiveRecording() {
        return activeRecording;
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

    public GeoBoundary getActiveGeoBoundary() {
        return activeGeoBoundary;
    }

    public void locationArrived(Location location) throws Failure {
        boolean recorded = recordLocation(location, false);
        lastLocation = location;

        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.lastLocationUpdated(recorded);
        }
    }

    public void lastKnownLocationArrived(Location location) {
        lastKnownLocation = location;

        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.lastKnownLocationUpdated();
        }
    }

    private boolean recordLocation(Location location, boolean last) throws Failure {
        if (activeRecordingType.isEmpty()) {
            return false;
        }

        if (activeLocations == 0) {
            doRecordLocation(location);
            return true;
        }

        double distance = GeoUtils.distance(lastRecordedLocation.getLatitude(), lastRecordedLocation.getLongitude(),
                location.getLatitude(), location.getLongitude());

        if (distance >= lastRecordedLocation.getAccuracy() + location.getAccuracy() || (last && distance > 0)) {
            doRecordLocation(location);
            activeDistance += distance;
            return true;
        }

        return false;
    }

    private void doRecordLocation(Location location) throws Failure {
        long now = System.currentTimeMillis();

        // using device-time, not location time
        getDatabase().saveLocation(activeRecording, now, location.getLatitude(), location.getLongitude());

        activeTsTo = now;
        activeLocations++;
        activeGeoBoundary.addPoint(location.getLatitude(), location.getLongitude());
        lastRecordedLocation = location;
    }

    public void startRecording(String movementType) throws Failure {
        long now = System.currentTimeMillis();

        activeRecording = getDatabase().startRecording(now, movementType);
        activeRecordingType = movementType;

        activeTsFrom = now;
        activeTsTo = now;
        activeLocations = 0;
        activeDistance = 0.0;
        activeGeoBoundary = new GeoBoundary();
        lastRecordedLocation = null;

        refreshReceiving();

        if (trackingService != null) {
            trackingService.startRecording();
        }
        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.recordingStarted();
        }
    }

    public void stopRecording(boolean delete) throws Failure {
        if (lastRecordedLocation != null && lastRecordedLocation != lastLocation) {
            recordLocation(lastLocation, true);
        }

        if (delete) {
            getDatabase().deleteRecording(activeRecording);
        } else {
            getDatabase().finishRecording(System.currentTimeMillis(), activeRecording, activeDistance);
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

    public void deleteRecording(long id) throws Failure {
        getDatabase().deleteRecording(id);
        for (MovementTracker movementTracker : movementTrackers) {
            movementTracker.reloadHistoryList();
        }
    }

    public void startedMovementTracker(MovementTracker movementTracker) throws Failure {
        startedMovementTrackers.add(movementTracker);
        refreshReceiving();
    }

    public void stoppedMovementTracker(MovementTracker movementTracker) throws Failure {
        startedMovementTrackers.remove(movementTracker);
        refreshReceiving();
    }

    private void refreshReceiving() throws Failure {
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

    public int getNotificationsId() {
        return ++notificationCounter;
    }
}
