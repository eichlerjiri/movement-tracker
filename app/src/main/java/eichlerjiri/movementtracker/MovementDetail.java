package eichlerjiri.movementtracker;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import eichlerjiri.movementtracker.db.HistoryItem;
import eichlerjiri.movementtracker.db.LocationDb;
import eichlerjiri.movementtracker.utils.Failure;
import eichlerjiri.movementtracker.utils.GeoUtils;

public class MovementDetail extends Activity {

    private Model m;

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m = Model.getInstance();
        m.registerMovementDetail(this);
        mapView = new MapView(this);
        mapView.onCreate(savedInstanceState);

        try {
            doCreate();
        } catch (Failure ignored) {
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m.unregisterMovementDetail(this);
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    private void doCreate() throws Failure {
        LinearLayout detailView = (LinearLayout) getLayoutInflater().inflate(R.layout.detail, null);
        detailView.addView(mapView);
        setContentView(detailView);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        TextView tv = detailView.findViewById(R.id.detailText);

        Bundle b = getIntent().getExtras();
        if (b == null) {
            tv.setText("Detail not available");
            return;
        }

        long id = b.getLong("id");
        if (id <= 0) {
            tv.setText("Detail not available");
            return;
        }

        HistoryItem item = m.getDatabase().getHistoryItem(id);
        if (item == null) {
            tv.setText("Detail not available");
            return;
        }

        long duration = item.getTsTo() - item.getTsFrom();
        double distance = 0.0;

        final ArrayList<LocationDb> locations = m.getDatabase().getLocations(item.getTsFrom(), item.getTsTo());

        LocationDb prev = null;
        for (LocationDb location : locations) {
            if (prev != null) {
                distance += GeoUtils.distance(prev.getLat(), prev.getLon(), location.getLat(), location.getLon());
            }
            prev = location;
        }

        double avgSpeed = -1;
        if (duration > 0) {
            avgSpeed = distance / (duration / 1000.0);
        }

        SimpleDateFormat formatter = new SimpleDateFormat("d.M.yyyy HH:mm", Locale.US);

        String text = "from: " + formatter.format(new Date(item.getTsFrom())) + "\n" +
                "to: " + formatter.format(new Date(item.getTsTo())) + "\n" +
                "locations: " + locations.size() + "\n" +
                "type: " + item.getMovementType() + "\n" +
                "duration: " + formatDuration(duration) + "\n" +
                "distance: " + formatDistance(distance) + "\n";

        if (avgSpeed >= 0) {
            text += "avg. speed: " + formatSpeed(avgSpeed) + "\n";
        }

        tv.setText(text);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                if (locations.isEmpty()) {
                    return;
                }
                drawLine(googleMap, locations);
            }
        });
    }

    private void drawLine(GoogleMap googleMap, ArrayList<LocationDb> locations) {
        double minLat = Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double maxLon = Double.MIN_VALUE;

        PolylineOptions polyline = new PolylineOptions();
        for (LocationDb location : locations) {
            // TODO fix longitude +-180 degrees
            minLat = Math.min(location.getLat(), minLat);
            minLon = Math.min(location.getLon(), minLon);
            maxLat = Math.max(location.getLat(), maxLat);
            maxLon = Math.max(location.getLon(), maxLon);

            polyline.add(new LatLng(location.getLat(), location.getLon()));
        }
        googleMap.addPolyline(polyline);

        LatLng southwest = new LatLng(minLat, minLon);
        LatLng northeast = new LatLng(maxLat, maxLon);
        if (southwest.equals(northeast)) {
            return;
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southwest, northeast), 0));
    }

    private String formatDuration(long millis) {
        long secondsFull = millis / 1000;
        long seconds = secondsFull % 60;
        long minutesFull = secondsFull / 60;
        long minutes = minutesFull % 60;
        long hours = minutesFull / 60;

        DecimalFormat df = new DecimalFormat("00", new DecimalFormatSymbols(Locale.US));
        return df.format(hours) + ":" + df.format(minutes) + ":" + df.format(seconds);
    }

    private String formatDistance(double distance) {
        if (distance >= 1000) {
            DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
            return df.format(distance / 1000) + "km";
        } else {
            DecimalFormat df = new DecimalFormat("0", new DecimalFormatSymbols(Locale.US));
            return df.format(distance) + "m";
        }
    }

    private String formatSpeed(double speed) {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        return df.format(speed * 3.6) + "km/h";
    }
}
