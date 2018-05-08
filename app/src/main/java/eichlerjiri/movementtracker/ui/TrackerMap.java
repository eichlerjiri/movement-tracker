package eichlerjiri.movementtracker.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.List;

import eichlerjiri.movementtracker.Model;
import eichlerjiri.movementtracker.db.LocationRow;
import eichlerjiri.movementtracker.utils.Failure;
import eichlerjiri.movementtracker.utils.GeoUtils;

public class TrackerMap {

    private final Model m;
    private final MapView mapView;
    private final GoogleMap googleMap;

    private LocationSource.OnLocationChangedListener googleLocationChangedListener;
    private boolean keepMapCentered = true;
    private Polyline polyline;
    private Marker marker;

    public TrackerMap(Context c, MapView mapView, GoogleMap googleMap) throws Failure {
        m = Model.getInstance();
        this.mapView = mapView;
        this.googleMap = googleMap;

        googleMap.setLocationSource(new LocationSource() {
            @Override
            public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
                googleLocationChangedListener = onLocationChangedListener;
            }

            @Override
            public void deactivate() {
                googleLocationChangedListener = null;
            }
        });

        googleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int var1) {
                if (var1 == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    keepMapCentered = false;
                }
            }
        });

        googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                keepMapCentered = true;
                centerMap();
                return true;
            }
        });

        if (!m.getActiveRecordingType().isEmpty()) {
            ArrayList<LocationRow> locs = m.getDatabase().getLocations(m.getActiveRecording());

            polyline = googleMap.addPolyline(GeoUtils.createPolyline(locs));
            if (!locs.isEmpty()) {
                marker = googleMap.addMarker(GeoUtils.createMarker(locs.get(0), BitmapDescriptorFactory.HUE_GREEN));
            }
        }

        centerMap();
        tryEnableSelfLocations(c);
    }

    public void updateLocation() {
        Location l = m.getLastLocation();

        if (googleLocationChangedListener != null) {
            googleLocationChangedListener.onLocationChanged(l);
        }

        if (!m.getActiveRecordingType().isEmpty()) {
            LatLng latLng = new LatLng(l.getLatitude(), l.getLongitude());

            List<LatLng> points = polyline.getPoints();
            points.add(latLng);
            polyline.setPoints(points);

            if (marker == null) {
                marker = googleMap.addMarker(GeoUtils.createMarker(latLng, BitmapDescriptorFactory.HUE_GREEN));
            }
        }

        if (keepMapCentered) {
            centerMap();
        }
    }

    private void centerMap() {
        if (!m.getActiveRecordingType().isEmpty()) {
            if (m.getActiveMinLat() != Double.MAX_VALUE) {
                GeoUtils.moveToRect(mapView, googleMap, m.getActiveMinLat(), m.getActiveMinLon(),
                        m.getActiveMaxLat(), m.getActiveMaxLon());
            }
        } else {
            Location l = m.getLastLocation();
            if (l != null) {
                GeoUtils.moveToPoint(googleMap, l.getLatitude(), l.getLongitude());
            }
        }
    }

    public void recordingStarted() {
        polyline = googleMap.addPolyline(GeoUtils.createPolyline());
    }

    public void recordingStopped() {
        polyline.remove();
        if (marker != null) {
            marker.remove();
            marker = null;
        }
    }

    public void tryEnableSelfLocations(Context c) {
        if (Build.VERSION.SDK_INT >= 23 && c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }
}
