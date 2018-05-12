package eichlerjiri.movementtracker;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
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
import eichlerjiri.movementtracker.utils.AndroidUtils;
import eichlerjiri.movementtracker.utils.Failure;
import eichlerjiri.movementtracker.utils.FormatUtils;
import eichlerjiri.movementtracker.utils.GeoBoundary;
import eichlerjiri.movementtracker.utils.GeoUtils;

public class MovementDetail extends MapViewActivity {

    private Model m;
    private HistoryRow recording;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (recording != null) {
            menu.add("delete recording").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    confirmDeleteRecording();
                    return true;
                }
            });
        }
        return true;
    }

    private void doCreate() throws Failure {
        TextView detailText = new TextView(this);

        int padding = AndroidUtils.spToPix(this, 4.0f);
        detailText.setPadding(padding, 0, padding, 0);

        LinearLayout detailView = new LinearLayout(this);
        detailView.setOrientation(LinearLayout.VERTICAL);

        detailView.addView(detailText);
        setContentView(detailView);

        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            throw new Failure("Action bar not available");
        }

        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        recording = getHistoryItem();
        if (recording == null) {
            detailText.setText("Detail not available");
            return;
        }

        setTitle(recording.movementType);
        detailView.addView(mapView); // after availability check

        long from = recording.ts;
        long to = recording.tsEnd;
        long duration = to - from;
        double distance = recording.distance;

        boolean sameDay = FormatUtils.isSameDay(from, to);

        final ArrayList<LocationRow> locations = m.getDatabase().getLocations(recording.id);

        String text = "from " + FormatUtils.formatDateTime(from) +
                " to " + (sameDay ? FormatUtils.formatTime(to) : FormatUtils.formatDateTime(to)) + "\n" +
                "locations: " + locations.size() + "\n" +
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
                GeoUtils.waitForMapViewToBeReady(mapView, new Runnable() {
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

        googleMap.addPolyline(GeoUtils.createPolyline(locs));
        googleMap.addMarker(GeoUtils.createMarker(locs.get(0), BitmapDescriptorFactory.HUE_GREEN));
        googleMap.addMarker(GeoUtils.createMarker(locs.get(locs.size() - 1), BitmapDescriptorFactory.HUE_RED));

        GeoBoundary geoBoundary = new GeoBoundary();
        for (LocationRow location : locs) {
            geoBoundary.addPoint(location.lat, location.lon);
        }
        GeoUtils.moveToRect(mapView, googleMap, geoBoundary);
    }

    private void confirmDeleteRecording() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage("Really delete recording?")
                .setTitle("Delete recording?");

        AlertDialog alertDialog = builder.create();
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            doDeleteRecording();
                        } catch (Failure ignored) {
                        }
                    }
                });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", (DialogInterface.OnClickListener) null);
        alertDialog.show();
    }

    private void doDeleteRecording() throws Failure {
        m.deleteRecording(recording.id);
        finish();
    }
}
