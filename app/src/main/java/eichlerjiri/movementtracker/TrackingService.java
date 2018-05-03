package eichlerjiri.movementtracker;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

        locationManager.removeUpdates(locationListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void doCreate() throws Failure {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            throw new Failure("Location service not available.");
        }

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            throw new Failure("Missing permissions for location service.");
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

        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }
}
