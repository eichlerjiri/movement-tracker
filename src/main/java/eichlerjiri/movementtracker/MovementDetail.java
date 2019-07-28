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

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.DoubleList;
import eichlerjiri.mapcomponent.utils.ObjectList;
import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.db.LocationRow;
import eichlerjiri.movementtracker.utils.GeoBoundary;

import static eichlerjiri.mapcomponent.utils.Common.*;
import static eichlerjiri.movementtracker.utils.Common.*;
import static java.lang.Math.*;

public class MovementDetail extends Activity {

    public Model m;
    public HistoryRow recording;
    public MapComponent map;
    public GeoBoundary geoBoundary;

    public boolean donePositionInit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m = Model.getInstance(this);

        TextView detailText = new TextView(this);

        int padding = round(4 * spSize(this));
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

        recording = getHistoryItem();
        if (recording == null) {
            detailText.setText("Detail not available");
            return;
        }

        setTitle(recording.movementType);

        long from = recording.ts;
        long to = recording.tsEnd;
        long duration = to - from;
        double distance = recording.distance;

        boolean sameDay = isSameDay(from, to);

        ObjectList<LocationRow> locations = m.database.getLocations(recording.id);

        String text = "from " + formatDateTime(from) +
                " to " + (sameDay ? formatTime(to) : formatDateTime(to)) + "\n" +
                "locations: " + locations.size + "\n" +
                "duration: " + formatDuration(duration) + "\n" +
                "distance: " + formatDistance(distance);

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

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        map.restoreInstanceState(savedInstanceState.getBundle("map"));
        donePositionInit = !map.centered;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBundle("map", map.saveInstanceState());
    }

    public HistoryRow getHistoryItem() {
        Bundle b = getIntent().getExtras();
        if (b != null) {
            long id = b.getLong("id");
            if (id > 0) {
                return m.database.getHistoryItem(id);
            }
        }
        return null;
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
                }
            }
        });
    }

    public void doCenterMap() {
        map.moveToBoundary(geoBoundary.minX, geoBoundary.minY, geoBoundary.maxX, geoBoundary.maxY, 18, 30);
        map.commit();
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
        m.deleteRecording(recording.id);
        finish();
    }
}
