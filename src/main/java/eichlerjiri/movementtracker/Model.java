package eichlerjiri.movementtracker;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import eichlerjiri.mapcomponent.utils.ObjectList;
import eichlerjiri.movementtracker.utils.FormatTools;
import eichlerjiri.movementtracker.utils.GeoBoundary;

import static eichlerjiri.movementtracker.utils.Common.*;

public class Model {

    public static Model instance;

    public static Model getInstance(Context c) {
        if (instance == null) {
            instance = new Model(c);
        }
        return instance;
    }

    public final Context appContext;
    public final Database database;
    public final FormatTools ft = new FormatTools();

    public TrackingService trackingService;
    public final ObjectList<MovementTracker> movementTrackers = new ObjectList<>(MovementTracker.class);
    public final ObjectList<MovementTracker> startedMovementTrackers = new ObjectList<>(MovementTracker.class);

    public Location lastKnownLocation;
    public Location lastLocation;
    public boolean receivingLocations;

    public long activeRecording;
    public String activeRecordingType = "";

    public long activeTsFrom;
    public long activeTsTo;
    public long activeLocations;
    public double activeDistance;
    public GeoBoundary activeGeoBoundary;

    public Location lastRecordedLocation;
    public int notificationCounter;

    public Model(Context c) {
        appContext = c.getApplicationContext();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Log.e("Model", e.getMessage(), e);

                Intent intent = new Intent(appContext, ErrorActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("msg", e.getMessage());
                appContext.startActivity(intent);

                System.exit(1);
            }
        });

        database = new Database(c);
    }

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

    public void locationArrived(Location location) {
        boolean recorded = recordLocation(location, false);
        lastLocation = location;

        for (int i = 0; i < movementTrackers.size; i++) {
            movementTrackers.data[i].lastLocationUpdated(recorded);
        }
    }

    public void lastKnownLocationArrived(Location location) {
        lastKnownLocation = location;

        for (int i = 0; i < movementTrackers.size; i++) {
            movementTrackers.data[i].lastKnownLocationUpdated();
        }
    }

    public boolean recordLocation(Location location, boolean last) {
        if (activeRecordingType.isEmpty()) {
            return false;
        }

        if (activeLocations == 0) {
            doRecordLocation(location);
            return true;
        }

        double distance = distance(lastRecordedLocation.getLatitude(), lastRecordedLocation.getLongitude(),
                location.getLatitude(), location.getLongitude());

        if (distance >= lastRecordedLocation.getAccuracy() + location.getAccuracy() || (last && distance > 0)) {
            doRecordLocation(location);
            activeDistance += distance;
            return true;
        }

        return false;
    }

    public void doRecordLocation(Location location) {
        long now = System.currentTimeMillis();

        // using device-time, not location time
        database.saveLocation(activeRecording, now, location.getLatitude(), location.getLongitude());

        activeTsTo = now;
        activeLocations++;
        activeGeoBoundary.addPoint(lonToMercatorX(location.getLongitude()), latToMercatorY(location.getLatitude()));
        lastRecordedLocation = location;
    }

    public void startRecording(String movementType) {
        long now = System.currentTimeMillis();

        activeRecording = database.startRecording(now, movementType);
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
        for (int i = 0; i < movementTrackers.size; i++) {
            movementTrackers.data[i].recordingStarted();
        }
    }

    public void stopRecording(boolean delete) {
        if (lastRecordedLocation != null && lastRecordedLocation != lastLocation) {
            recordLocation(lastLocation, true);
        }

        if (delete) {
            database.deleteRecording(activeRecording);
        } else {
            database.finishRecording(System.currentTimeMillis(), activeRecording, activeDistance);
        }

        activeRecordingType = "";

        refreshReceiving();

        if (trackingService != null) {
            trackingService.stopRecording();
        }
        for (int i = 0; i < movementTrackers.size; i++) {
            movementTrackers.data[i].recordingStopped();
        }
    }

    public void deleteRecording(long id) {
        database.deleteRecording(id);
        for (int i = 0; i < movementTrackers.size; i++) {
            movementTrackers.data[i].reloadHistoryList();
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

    public void refreshReceiving() {
        boolean requested = startedMovementTrackers.size != 0 || !activeRecordingType.isEmpty();

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
