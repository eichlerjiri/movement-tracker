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

public class TrackingService extends Service {

    public App app;

    public LocationManager locationManager;
    public LocationListener locationListener;

    @Override
    public void onCreate() {
        app = App.get(this);
        app.registerTrackingService(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            throw new Error("Location service not available.");
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                app.locationArrived(location);
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

        if (app.receivingLocations) {
            startReceiving();
        }
        if (app.activeRecordingType != null) {
            startRecording();
        }
    }

    @Override
    public void onDestroy() {
        app.unregisterTrackingService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public void startReceiving() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            throw new Error("Missing permissions for location service.");
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            app.lastKnownLocationArrived(location);
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

        startForeground(app.getNotificationsId(), notification);
    }

    public void stopRecording() {
        stopForeground(true);
    }
}
