package eichlerjiri.movementtracker;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import eichlerjiri.movementtracker.models.RecordingModel;
import static eichlerjiri.movementtracker.utils.Common.*;
import eichlerjiri.movementtracker.utils.GeoBoundary;

public class App extends Application {

    public TrackingService trackingService;
    public MovementTracker movementTracker;
    public boolean movementTrackerForeground;

    public SQLiteDatabase sqlite;

    public boolean locationPermissionDone;
    public Intent locationServiceIntent;
    public ServiceConnection locationServiceConnection;

    public Location lastKnownLocation;
    public Location lastLocation;

    public long activeRecording;
    public String activeRecordingType;

    public long activeTsFrom;
    public long activeTsTo;
    public long activeLocations;
    public double activeDistance;
    public GeoBoundary activeGeoBoundary;

    public Location lastRecordedLocation;
    public int notificationCounter;

    public App() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Log.e("App", exceptionToString(e), e);

                Intent intent = new Intent(App.this, ErrorActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("msg", exceptionToString(e));
                startActivity(intent);

                System.exit(1);
            }
        });
    }

    public void registerTrackingService(TrackingService service) {
        trackingService = service;
    }

    public void unregisterTrackingService() {
        trackingService = null;
    }

    public void registerMovementTracker(MovementTracker movementTracker) {
        this.movementTracker = movementTracker;
    }

    public void unregisterMovementTracker() {
        this.movementTracker = null;
    }

    public SQLiteDatabase sqlite() {
        if (sqlite == null) {
            sqlite = new SQLiteOpenHelper(this, "movement-tracker", null, 1) {
                @Override
                public void onCreate(SQLiteDatabase db) {
                    sqlite = db;
                    createDatabase();
                }

                @Override
                public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                }
            }.getWritableDatabase();

            RecordingModel.finishLastRecording(this);
        }
        return sqlite;
    }

    public void createDatabase() {
        sqlite.execSQL("CREATE TABLE recording(id INTEGER PRIMARY KEY,"
                + "ts INTEGER,"
                + "ts_end INTEGER,"
                + "movement_type VARCHAR(20),"
                + "distance DOUBLE)"
        );

        sqlite.execSQL("CREATE TABLE location(id INTEGER PRIMARY KEY,"
                + "id_recording INTEGER,"
                + "ts INTEGER,"
                + "lat DOUBLE,"
                + "lon DOUBLE)"
        );

        sqlite.execSQL("CREATE INDEX recording_ts ON recording(ts)");
        sqlite.execSQL("CREATE INDEX location_recording_ts ON location(id_recording,ts)");
    }

    public void locationArrived(Location location) {
        boolean recorded = recordLocation(location, false);
        lastLocation = location;

        if (movementTracker != null) {
            movementTracker.lastLocationUpdated(recorded);
        }
    }

    public void lastKnownLocationArrived(Location location) {
        lastKnownLocation = location;

        if (movementTracker != null) {
            movementTracker.lastKnownLocationUpdated();
        }
    }

    public boolean recordLocation(Location location, boolean last) {
        if (activeRecordingType == null) {
            return false;
        }

        if (activeLocations == 0) {
            doRecordLocation(location);
            return true;
        }

        double distance = distance(lastRecordedLocation.getLatitude(), lastRecordedLocation.getLongitude(), location.getLatitude(), location.getLongitude());

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
        RecordingModel.saveLocation(this, activeRecording, now, location.getLatitude(), location.getLongitude());

        activeTsTo = now;
        activeLocations++;
        activeGeoBoundary.addPoint(lonToMercatorX(location.getLongitude()), latToMercatorY(location.getLatitude()));
        lastRecordedLocation = location;
    }

    public void startRecording(String movementType) {
        long now = System.currentTimeMillis();

        activeRecording = RecordingModel.startRecording(this, now, movementType);
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
        if (movementTracker != null) {
            movementTracker.recordingStarted();
        }
    }

    public void stopRecording(boolean delete) {
        if (lastRecordedLocation != null && lastRecordedLocation != lastLocation) {
            recordLocation(lastLocation, true);
        }

        if (delete) {
            RecordingModel.deleteRecording(this, activeRecording);
        } else {
            RecordingModel.finishRecording(this, System.currentTimeMillis(), activeRecording, activeDistance);
        }

        activeRecordingType = null;

        refreshReceiving();

        if (trackingService != null) {
            trackingService.stopRecording();
        }
        if (movementTracker != null) {
            movementTracker.recordingStopped();
        }
    }

    public void deleteRecording(long id) {
        RecordingModel.deleteRecording(this, id);
        if (movementTracker != null) {
            movementTracker.reloadHistoryList();
        }
    }

    public void resumedMovementTracker() {
        movementTrackerForeground = true;
        refreshReceiving();
    }

    public void pausedMovementTracker() {
        movementTrackerForeground = false;
        refreshReceiving();
    }

    public void refreshReceiving() {
        if (!locationPermissionDone) {
            return;
        }

        boolean requested = movementTrackerForeground || activeRecordingType != null;

        if (requested && locationServiceIntent == null) {
            locationServiceIntent = new Intent(this, TrackingService.class);
            startService(locationServiceIntent);

        } else if (!requested && locationServiceIntent != null) {
            stopService(locationServiceIntent);
            locationServiceIntent = null;
        }

        if (movementTrackerForeground && locationServiceConnection == null) {
            locationServiceConnection = prepareServiceConnection();
            movementTracker.bindService(locationServiceIntent, locationServiceConnection, 0);

        } else if (!movementTrackerForeground && locationServiceConnection != null) {
            movementTracker.unbindService(locationServiceConnection);
            locationServiceConnection = null;
        }
    }

    public int getNotificationsId() {
        return ++notificationCounter;
    }

    public ServiceConnection prepareServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
    }
}
