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

    public boolean donePositionInit;

    public TrackerMap(Context c, ArrayList<String> mapUrls) {
        super(c, mapUrls);
        m = Model.getInstance();

        try {
            doInit();
        } catch (Failure ignored) {
        }
    }

    @Override
    public void centerMap() {
        Location last = m.getLastLocation();
        Location lastKnown = m.getLastKnownLocation();
        double lat;
        double lon;
        float zoom;
        if (last != null) {
            lat = last.getLatitude();
            lon = last.getLongitude();
            zoom = 18;
        } else if (lastKnown != null) {
            lat = lastKnown.getLatitude();
            lon = lastKnown.getLongitude();
            zoom = 15;
        } else {
            lat = 50.083333; // prague
            lon = 14.416667;
            zoom = 4;
        }

        if (!m.getActiveRecordingType().isEmpty()) {
            GeoBoundary geoBoundary = new GeoBoundary(m.getActiveGeoBoundary());
            geoBoundary.addPoint(lat, lon);
            moveToBoundary(geoBoundary, getWidth(), getHeight(), zoom, 30);
        } else {
            moveTo(lat, lon, zoom, 0);
        }
    }

    private void doInit() throws Failure {
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
            @Override
            public void onGlobalLayout() {
                if (!donePositionInit) {
                    donePositionInit = true;
                    centerMap();
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
            centerMap();
        }
    }

    public void updateLastKnownLocation() {
        if (centered) {
            centerMap();
        }
    }

    public void recordingStopped() {
        pathPositions.clear();
        setPath(null);

        setStartPosition(Double.MIN_VALUE, Double.MIN_VALUE);
        startDisplayed = false;

        if (centered) {
            centerMap();
        }
    }
}
