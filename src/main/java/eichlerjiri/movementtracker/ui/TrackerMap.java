package eichlerjiri.movementtracker.ui;

import android.content.Context;
import android.location.Location;
import android.view.ViewTreeObserver;

import java.util.ArrayList;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.DoubleArrayList;
import eichlerjiri.mapcomponent.utils.GeoBoundary;
import eichlerjiri.movementtracker.Model;
import eichlerjiri.movementtracker.db.LocationRow;
import eichlerjiri.movementtracker.utils.Failure;

public class TrackerMap extends MapComponent {

    private final Model m;

    private boolean startDisplayed;
    private DoubleArrayList pathPositions = new DoubleArrayList();

    public TrackerMap(Context c, ArrayList<String> mapUrls) {
        super(c, mapUrls);
        m = Model.getInstance();

        try {
            doInit();
        } catch (Failure ignored) {
        }
    }

    private void doInit() throws Failure {
        centered = true;

        Location l = m.getLastLocation();
        if (l != null) {
            moveTo(l.getLatitude(), l.getLongitude(), 18);
        } else {
            moveTo(50.083333, 14.416667, 4); // prague
        }

        if (!m.getActiveRecordingType().isEmpty()) {
            ArrayList<LocationRow> locs = m.getDatabase().getLocations(m.getActiveRecording());

            for (LocationRow row : locs) {
                pathPositions.add(row.lat, row.lon);
            }
            setPath(pathPositions);

            if (!locs.isEmpty()) {
                LocationRow first = locs.get(0);
                setStartPosition(first.lat, first.lon);
                startDisplayed = true;
            }
        }

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            private boolean done;

            @Override
            public void onGlobalLayout() {
                if (!done) {
                    done = true;
                    if (m.getActiveLocations() != 0) {
                        moveToBoundary(m.getActiveGeoBoundary(), getWidth(), getHeight(), 18, 30);
                    }
                }
            }
        });
    }

    public void updateLocation(boolean recorded) {
        Location l = m.getLastLocation();
        double lat = l.getLatitude();
        double lon = l.getLongitude();

        setCurrentPosition(l);

        if (recorded) {
            if (!startDisplayed) {
                setStartPosition(lat, lon);
                startDisplayed = true;
            }

            pathPositions.add(lat, lon);
            setPath(pathPositions);
        }

        if (centered) {
            if (!m.getActiveRecordingType().isEmpty()) {
                GeoBoundary geoBoundary = new GeoBoundary(m.getActiveGeoBoundary());
                geoBoundary.addPoint(lat, lon);
                moveToBoundary(geoBoundary, getWidth(), getHeight(), 18, 30);
            } else {
                moveTo(lat, lon, 18);
            }
        }
    }

    public void recordingStopped() {
        pathPositions.clear();
        setPath(null);

        setStartPosition(Double.MIN_VALUE, Double.MIN_VALUE);
        startDisplayed = false;

        if (centered) {
            Location l = m.getLastLocation();
            if (l != null) {
                moveTo(l.getLatitude(), l.getLongitude(), 18);
            }
        }
    }
}
