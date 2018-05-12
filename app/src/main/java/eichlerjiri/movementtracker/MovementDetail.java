package eichlerjiri.movementtracker;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.ArrayList;

import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.db.LocationRow;
import eichlerjiri.movementtracker.ui.MapViewActivity;
import eichlerjiri.movementtracker.utils.Failure;
import eichlerjiri.movementtracker.utils.FormatUtils;
import eichlerjiri.movementtracker.utils.GeoUtils;

public class MovementDetail extends MapViewActivity {

    private Model m;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m = Model.getInstance();
        m.registerMovementDetail(this);

        try {
            doCreate();
        } catch (Failure ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m.unregisterMovementDetail(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    private void doCreate() throws Failure {
        TextView detailText = new TextView(this);

        LinearLayout detailView = new LinearLayout(this);
        detailView.setOrientation(LinearLayout.VERTICAL);

        detailView.addView(detailText);
        detailView.addView(mapView);
        setContentView(detailView);

        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            throw new Failure("Action bar not available");
        }

        actionBar.setDisplayHomeAsUpEnabled(true);

        HistoryRow item = getHistoryItem();
        if (item == null) {
            detailText.setText("Detail not available");
            return;
        }

        long from = item.ts;
        long to = item.tsEnd;
        long duration = to - from;
        double distance = item.distance;

        boolean sameDay = FormatUtils.isSameDay(from, to);

        final ArrayList<LocationRow> locations = m.getDatabase().getLocations(item.id);

        String text = "from " + FormatUtils.formatDateTime(from) +
                " to " + (sameDay ? FormatUtils.formatTime(to) : FormatUtils.formatDateTime(to)) + "\n" +
                "locations: " + locations.size() + "\n" +
                "type: " + item.movementType + "\n" +
                "duration: " + FormatUtils.formatDuration(duration) + "\n" +
                "distance: " + FormatUtils.formatDistance(distance);

        double avgSpeed = GeoUtils.avgSpeed(distance, duration);
        if (avgSpeed != 0.0) {
            text += "\navg. speed: " + FormatUtils.formatSpeed(avgSpeed);
        }

        detailText.setText(text);

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

    private HistoryRow getHistoryItem() throws Failure {
        Bundle b = getIntent().getExtras();
        if (b != null) {
            long id = b.getLong("id");
            if (id > 0) {
                return m.getDatabase().getHistoryItem(id);
            }
        }
        return null;
    }

    private void drawLine(GoogleMap googleMap, ArrayList<LocationRow> locs) {
        if (locs.isEmpty()) {
            return;
        }

        double minLat = Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double maxLon = Double.MIN_VALUE;

        for (LocationRow location : locs) {
            minLat = Math.min(location.lat, minLat);
            minLon = Math.min(location.lon, minLon);
            maxLat = Math.max(location.lat, maxLat);
            maxLon = Math.max(location.lon, maxLon);
        }

        googleMap.addPolyline(GeoUtils.createPolyline(locs));
        googleMap.addMarker(GeoUtils.createMarker(locs.get(0), BitmapDescriptorFactory.HUE_GREEN));
        googleMap.addMarker(GeoUtils.createMarker(locs.get(locs.size() - 1), BitmapDescriptorFactory.HUE_RED));

        GeoUtils.moveToRect(mapView, googleMap, minLat, minLon, maxLat, maxLon);
    }
}
