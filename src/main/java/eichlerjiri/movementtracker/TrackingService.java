package eichlerjiri.movementtracker;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import eichlerjiri.movementtracker.utils.Failure;

public class TrackingService extends Service {

    private Model m;

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    public void onCreate() {
        m = Model.getInstance();
        m.registerTrackingService(this);

        try {
            doCreate();
        } catch (Failure ignored) {
        }
    }

    @Override
    public void onDestroy() {
        m.unregisterTrackingService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    private void doCreate() throws Failure {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            throw new Failure("Location service not available.");
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                try {
                    m.locationArrived(location);
                } catch (Failure ignored) {
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        if (m.isReceivingLocations()) {
            startReceiving();
        }
        if (!m.getActiveRecordingType().isEmpty()) {
            startRecording();
        }
    }

    public void startReceiving() throws Failure {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            throw new Failure("Missing permissions for location service.");
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            m.locationArrived(location);
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    public void stopReceiving() {
        locationManager.removeUpdates(locationListener);
    }

    public void startRecording() {
        Intent notificationIntent = new Intent(this, MovementTracker.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Recording")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .getNotification();

        startForeground(m.getNotificationsId(), notification);
    }

    public void stopRecording() {
        stopForeground(true);
    }
}
