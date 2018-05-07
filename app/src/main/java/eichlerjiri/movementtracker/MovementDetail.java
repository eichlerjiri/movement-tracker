package eichlerjiri.movementtracker;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import eichlerjiri.movementtracker.db.HistoryItem;
import eichlerjiri.movementtracker.db.LocationDb;
import eichlerjiri.movementtracker.utils.Failure;
import eichlerjiri.movementtracker.utils.FormatUtils;
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
        super.onLowMemory();
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
        setContentView(detailView);

        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            throw new Failure("Action bar not available");
        }

        actionBar.setDisplayHomeAsUpEnabled(true);

        TextView tv = detailView.findViewById(R.id.detailText);

        HistoryItem item = getHistoryItem();
        if (item == null) {
            tv.setText("Detail not available");
            return;
        }

        final ArrayList<LocationDb> locations = m.getDatabase().getLocations(item.getTsFrom(), item.getTsTo());

        long from = item.getTsFrom();
        long to = item.getTsTo();
        long duration = to - from;
        double distance = computeDistance(locations);

        boolean sameDay = FormatUtils.isSameDay(from, to);

        String text = "from " + FormatUtils.formatDateTime(from) +
                " to " + (sameDay ? FormatUtils.formatTime(to) : FormatUtils.formatDateTime(to)) + "\n" +
                "locations: " + locations.size() + "\n" +
                "type: " + item.getMovementType() + "\n" +
                "duration: " + FormatUtils.formatDuration(duration) + "\n" +
                "distance: " + FormatUtils.formatDistance(distance);

        double avgSpeed = GeoUtils.avgSpeed(distance, duration);
        if (avgSpeed != 0.0) {
            text += "\navg. speed: " + FormatUtils.formatSpeed(avgSpeed);
        }

        tv.setText(text);

        detailView.addView(mapView);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                GeoUtils.waitForViewToBeReady(mapView, new Runnable() {
                    @Override
                    public void run() {
                        drawLine(googleMap, locations);
                    }
                });
            }
        });
    }

    private HistoryItem getHistoryItem() throws Failure {
        Bundle b = getIntent().getExtras();
        if (b != null) {
            long id = b.getLong("id");
            if (id > 0) {
                return m.getDatabase().getHistoryItem(id);
            }
        }
        return null;
    }

    private double computeDistance(ArrayList<LocationDb> locations) {
        double ret = 0;
        LocationDb prev = null;
        for (LocationDb location : locations) {
            if (prev != null) {
                ret += GeoUtils.distance(prev.getLat(), prev.getLon(), location.getLat(), location.getLon());
            }
            prev = location;
        }
        return ret;
    }

    private void drawLine(GoogleMap googleMap, ArrayList<LocationDb> locations) {
        double minLat = Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double maxLon = Double.MIN_VALUE;

        PolylineOptions polyline = new PolylineOptions();
        for (LocationDb location : locations) {
            minLat = Math.min(location.getLat(), minLat);
            minLon = Math.min(location.getLon(), minLon);
            maxLat = Math.max(location.getLat(), maxLat);
            maxLon = Math.max(location.getLon(), maxLon);

            polyline.add(new LatLng(location.getLat(), location.getLon()));
        }
        googleMap.addPolyline(polyline);

        GeoUtils.moveToRect(mapView, googleMap, minLat, minLon, maxLat, maxLon);
    }
}
