package eichlerjiri.movementtracker;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.DoubleList;
import eichlerjiri.mapcomponent.utils.ObjectList;
import eichlerjiri.movementtracker.models.RecordingModel;
import eichlerjiri.movementtracker.models.RecordingModel.RecordingRow;
import eichlerjiri.movementtracker.models.RecordingModel.LocationRow;
import eichlerjiri.movementtracker.ui.Exporter;
import static eichlerjiri.movementtracker.utils.Common.*;
import eichlerjiri.movementtracker.utils.GeoBoundary;
import static java.lang.Math.*;

public class MovementDetail extends Activity {

    public App app;
    public RecordingRow recording;
    public MapComponent map;
    public GeoBoundary geoBoundary;

    public boolean donePositionInit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getApplicationContext();

        RecordingRow recording = null;
        long id = getIntent().getLongExtra("id", 0);
        if (id > 0) {
            recording = RecordingModel.getRecording(app, id);
        }
        if (recording == null) {
            Toast.makeText(this, "Detail not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView detailText = new TextView(this);

        int padding = round(4 * getResources().getDisplayMetrics().scaledDensity);
        detailText.setPadding(padding, 0, padding, 0);

        LinearLayout detailView = new LinearLayout(this);
        detailView.setOrientation(LinearLayout.VERTICAL);

        map = new MapComponent(this, splitNonEmpty(" ", getText(R.string.map_urls).toString())) {
            @Override
            public void centerMap() {
                doCenterMap();
            }
        };

        detailView.addView(detailText);
        detailView.addView(map);
        setContentView(detailView);

        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            throw new Error("Action bar not available");
        }

        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setTitle(recording.movementType);

        long from = recording.ts;
        long to = recording.tsEnd;
        long duration = to - from;
        double distance = recording.distance;

        boolean sameDay = isSameDay(from, to);

        ObjectList<LocationRow> locations = RecordingModel.getLocations(app, recording.id);

        String text = "from " + formatDateTimeCzech(from) + " to " + (sameDay ? formatTime(to) : formatDateTimeCzech(to)) + "\n"
                + "locations: " + locations.size + "\n"
                + "duration: " + formatDuration(duration) + "\n"
                + "distance: " + formatDistance(distance);

        double avgSpeed = avgSpeed(distance, duration);
        if (avgSpeed != 0.0) {
            text += "\navg. speed: " + formatSpeed(avgSpeed);
        }

        detailText.setText(text);

        drawLine(locations);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        map.close();
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
        menu.add("export GPX").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                new Exporter(MovementDetail.this, recording, "gpx").exportTracks();
                return true;
            }
        });
        menu.add("export TCX").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                new Exporter(MovementDetail.this, recording, "tcx").exportTracks();
                return true;
            }
        });

        menu.add("delete recording").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                confirmDeleteRecording();
                return true;
            }
        });
        return true;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        map.restoreInstanceState(savedInstanceState.getBundle("map"));
        donePositionInit = !map.d.centered;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBundle("map", map.saveInstanceState());
    }

    public void drawLine(ObjectList<LocationRow> locations) {
        if (locations.size == 0) {
            return;
        }

        LocationRow start = locations.data[0];
        map.setStartPosition(lonToMercatorX(start.lon), latToMercatorY(start.lat));

        LocationRow end = locations.data[locations.size - 1];
        map.setEndPosition(lonToMercatorX(end.lon), latToMercatorY(end.lat));

        DoubleList positions = new DoubleList();
        geoBoundary = new GeoBoundary();
        for (int i = 0; i < locations.size; i++) {
            LocationRow row = locations.data[i];
            double x = lonToMercatorX(row.lon);
            double y = latToMercatorY(row.lat);

            positions.add(x, y);
            geoBoundary.addPoint(x, y);
        }
        map.setPath(positions.data, 0, positions.size);

        map.commit();

        map.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!donePositionInit) {
                    donePositionInit = true;
                    doCenterMap();
                    map.commit();
                }
            }
        });
    }

    public void doCenterMap() {
        map.moveToBoundary(geoBoundary.minX, geoBoundary.minY, geoBoundary.maxX, geoBoundary.maxY, 18, 30);
    }

    public void confirmDeleteRecording() {
        new AlertDialog.Builder(this)
                .setMessage("Really delete recording?")
                .setTitle("Delete recording?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doDeleteRecording();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void doDeleteRecording() {
        app.deleteRecording(recording.id);
        finish();
    }
}
