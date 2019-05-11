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

import java.util.ArrayList;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.DoubleArrayList;
import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.db.LocationRow;
import eichlerjiri.movementtracker.utils.GeoBoundary;

import static eichlerjiri.mapcomponent.utils.Common.*;
import static eichlerjiri.movementtracker.utils.Common.*;

public class MovementDetail extends Activity {

    private Model m;
    private HistoryRow recording;
    private MapComponent map;
    private GeoBoundary geoBoundary;

    boolean donePositionInit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m = Model.getInstance(this);

        TextView detailText = new TextView(this);

        int padding = Math.round(4 * spSize(this));
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

        ArrayList<LocationRow> locations = m.database.getLocations(recording.id);

        String text = "from " + formatDateTime(from) +
                " to " + (sameDay ? formatTime(to) : formatDateTime(to)) + "\n" +
                "locations: " + locations.size() + "\n" +
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
    protected void onDestroy() {
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

    private HistoryRow getHistoryItem() {
        Bundle b = getIntent().getExtras();
        if (b != null) {
            long id = b.getLong("id");
            if (id > 0) {
                return m.database.getHistoryItem(id);
            }
        }
        return null;
    }

    private void drawLine(ArrayList<LocationRow> locations) {
        if (locations.isEmpty()) {
            return;
        }

        LocationRow start = locations.get(0);
        map.setStartPosition(lonToMercatorX(start.lon), latToMercatorY(start.lat));

        LocationRow end = locations.get(locations.size() - 1);
        map.setEndPosition(lonToMercatorX(end.lon), latToMercatorY(end.lat));

        DoubleArrayList positions = new DoubleArrayList();
        geoBoundary = new GeoBoundary();
        for (LocationRow row : locations) {
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

    void doCenterMap() {
        map.moveToBoundary(geoBoundary.minX, geoBoundary.minY, geoBoundary.maxX, geoBoundary.maxY, 18, 30);
        map.commit();
    }

    void confirmDeleteRecording() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage("Really delete recording?")
                .setTitle("Delete recording?");

        AlertDialog alertDialog = builder.create();
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doDeleteRecording();
                    }
                });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", (DialogInterface.OnClickListener) null);
        alertDialog.show();
    }

    void doDeleteRecording() {
        m.deleteRecording(recording.id);
        finish();
    }
}
