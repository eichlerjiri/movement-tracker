package eichlerjiri.movementtracker.utils;

import android.view.ViewTreeObserver;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import eichlerjiri.movementtracker.db.LocationRow;

public class GeoUtils {

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double thetaRad = deg2rad(lon1 - lon2);
        double lat1Rad = deg2rad(lat1);
        double lat2Rad = deg2rad(lat2);

        double d = Math.sin(lat1Rad) * Math.sin(lat2Rad) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.cos(thetaRad);
        double ret = rad2deg(Math.acos(d)) * 60 * 1.1515 * 1.609344 * 1000;
        if (Double.isNaN(ret)) {
            return 0;
        }
        return ret;
    }

    public static double avgSpeed(double distance, long duration) {
        if (duration == 0) {
            return 0;
        }
        return distance / (duration / 1000.0);
    }

    public static void waitForViewToBeReady(final MapView mapView, final Runnable callback) {
        if (mapView.getWidth() != 0 && mapView.getHeight() != 0) {
            callback.run();
        } else {
            mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    callback.run();
                }
            });
        }
    }

    public static void moveToRect(MapView mapView, GoogleMap mapInterface, double minLat, double minLon,
                                  double maxLat, double maxLon) {
        int padding = (int) (mapView.getWidth() * 0.1f);

        mapInterface.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(
                new LatLng(minLat, minLon), new LatLng(maxLat, maxLon)), padding));
    }

    public static void moveToPoint(GoogleMap mapInterface, double lat, double lon) {
        mapInterface.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15));
    }

    public static PolylineOptions createPolyline(ArrayList<LocationRow> locs) {
        PolylineOptions polyline = createPolyline();
        for (LocationRow location : locs) {
            polyline.add(new LatLng(location.lat, location.lon));
        }
        return polyline;
    }

    public static PolylineOptions createPolyline() {
        return new PolylineOptions().width(5.0f);
    }

    public static MarkerOptions createMarker(LocationRow loc, float marker) {
        return createMarker(new LatLng(loc.lat, loc.lon), marker);
    }

    public static MarkerOptions createMarker(LatLng latLng, float marker) {
        return new MarkerOptions().position(latLng).icon(
                BitmapDescriptorFactory.defaultMarker(marker));
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
}
