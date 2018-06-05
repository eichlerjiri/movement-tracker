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
import eichlerjiri.mapcomponent.utils.GeoBoundary;
import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.db.LocationRow;
import eichlerjiri.mapcomponent.utils.AndroidUtils;
import eichlerjiri.movementtracker.utils.Failure;
import eichlerjiri.movementtracker.utils.FormatUtils;
import eichlerjiri.movementtracker.utils.GeoUtils;
import eichlerjiri.movementtracker.utils.StringUtils;

public class MovementDetail extends Activity {

    private Model m;
    private HistoryRow recording;
    private MapComponent map;

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

    private void doCreate() throws Failure {
        TextView detailText = new TextView(this);

        int padding = Math.round(AndroidUtils.spToPix(this, 4.0f));
        detailText.setPadding(padding, 0, padding, 0);

        LinearLayout detailView = new LinearLayout(this);
        detailView.setOrientation(LinearLayout.VERTICAL);

        map = new MapComponent(this, StringUtils.splitNonEmpty(" ", getText(R.string.map_urls).toString()));

        detailView.addView(detailText);
        detailView.addView(map);
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

        drawLine(map, locations);
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

    private void drawLine(final MapComponent map, ArrayList<LocationRow> locations) {
        if (locations.isEmpty()) {
            return;
        }

        LocationRow start = locations.get(0);
        LocationRow end = locations.get(locations.size() - 1);

        map.setStartPosition(start.lat, start.lon);
        map.setEndPosition(end.lat, end.lon);

        DoubleArrayList positions = new DoubleArrayList();
        for (LocationRow row : locations) {
            positions.add(row.lat, row.lon);
        }
        map.setPath(positions);

        final GeoBoundary geoBoundary = new GeoBoundary();
        for (LocationRow location : locations) {
            geoBoundary.addPoint(location.lat, location.lon);
        }

        map.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            private boolean done;

            @Override
            public void onGlobalLayout() {
                if (!done) {
                    done = true;
                    map.moveToBoundary(geoBoundary, map.getWidth(), map.getHeight(), 18, 30);
                }
            }
        });
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
