package eichlerjiri.movementtracker;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

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

        long duration = item.getTsTo() - item.getTsFrom();
        double distance = computeDistance(locations);

        String text = "from: " + formatDate(item.getTsFrom()) + "\n" +
                "to: " + formatDate(item.getTsTo()) + "\n" +
                "locations: " + locations.size() + "\n" +
                "type: " + item.getMovementType() + "\n" +
                "duration: " + formatDuration(duration) + "\n" +
                "distance: " + formatDistance(distance) + "\n";

        if (duration > 0) {
            double avgSpeed = distance / (duration / 1000.0);
            text += "avg. speed: " + formatSpeed(avgSpeed) + "\n";
        }

        tv.setText(text);

        if (locations.isEmpty()) {
            return;
        }

        detailView.addView(mapView);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                waitForViewToBeReady(new Runnable() {
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

    private void waitForViewToBeReady(final Runnable callback) {
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

        LatLng southwest = new LatLng(minLat, minLon);
        LatLng northeast = new LatLng(maxLat, maxLon);

        int padding = (int) (mapView.getWidth() * 0.1f);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southwest, northeast), padding));
    }

    private String formatDate(long millis) {
        SimpleDateFormat formatter = new SimpleDateFormat("d.M.yyyy HH:mm", Locale.US);
        return formatter.format(new Date(millis));
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
