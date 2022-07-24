package eichlerjiri.movementtracker;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
        app = (App) getApplicationContext();
        app.registerTrackingService(this);

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw new Error("Missing permissions for location service.");
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

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            throw new Error("Location service not available.");
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            app.lastKnownLocationArrived(location);
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        if (app.activeRecordingType != null) {
            startRecording();
        }
    }

    @Override
    public void onDestroy() {
        app.unregisterTrackingService(this);

        locationManager.removeUpdates(locationListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public void startRecording() {
        Intent notificationIntent = new Intent(this, MovementTracker.class);

        int flag = 0;
        if (Build.VERSION.SDK_INT >= 23) {
            flag |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flag);

        Notification notification;
        if (Build.VERSION.SDK_INT >= 26) {
            String id = "eichlerjiri.movementtracker";

            NotificationChannel channel = new NotificationChannel(id, "Movement Tracker", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            notification = new Notification.Builder(this, id)
                    .setContentTitle("Recording")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("Recording")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .getNotification();
        }

        startForeground(app.getNotificationsId(), notification);
    }

    public void stopRecording() {
        stopForeground(true);
    }
}
