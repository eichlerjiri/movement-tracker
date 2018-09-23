package eichlerjiri.movementtracker.ui;

import android.content.Context;
import android.location.Location;
import android.view.ViewTreeObserver;

import java.util.ArrayList;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.DoubleArrayList;
import eichlerjiri.movementtracker.utils.GeoBoundary;
import eichlerjiri.movementtracker.Model;
import eichlerjiri.movementtracker.db.LocationRow;
import eichlerjiri.movementtracker.utils.Failure;

import static eichlerjiri.movementtracker.utils.Common.*;

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
            geoBoundary.addPoint(lonToMercatorX(lon), latToMercatorY(lat));
            moveToBoundary(geoBoundary.minX, geoBoundary.minY, geoBoundary.maxX, geoBoundary.maxY, zoom, 30);
        } else {
            setPosition(lonToMercatorX(lon), latToMercatorY(lat));
            setZoom(zoom);
            setAzimuth(0);
        }
        commit();
    }

    private void doInit() throws Failure {
        if (m.getLastLocation() != null) {
            updateLocation(false);
        }

        if (!m.getActiveRecordingType().isEmpty()) {
            ArrayList<LocationRow> locs = m.getDatabase().getLocations(m.getActiveRecording());

            for (LocationRow row : locs) {
                pathPositions.add(lonToMercatorX(row.lon), latToMercatorY(row.lat));
            }
            setPath(pathPositions.data, 0, pathPositions.size);

            if (!locs.isEmpty()) {
                LocationRow first = locs.get(0);
                setStartPosition(lonToMercatorX(first.lon), latToMercatorY(first.lat));
                startDisplayed = true;
            }

            commit();
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

        if (l == null) {
            setCurrentPosition(Double.MIN_VALUE, Double.MIN_VALUE, Float.MIN_VALUE);
        } else {
            float bearing = l.hasBearing() ? l.getBearing() : Float.MIN_VALUE;
            setCurrentPosition(lonToMercatorX(l.getLongitude()), latToMercatorY(l.getLatitude()), bearing);
        }

        if (recorded) {
            if (!startDisplayed) {
                setStartPosition(lonToMercatorX(lon), latToMercatorY(lat));
                startDisplayed = true;
            }

            pathPositions.add(lonToMercatorX(lon), latToMercatorY(lat));
            setPath(pathPositions.data, 0, pathPositions.size);
        }

        commit();

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
        pathPositions = new DoubleArrayList();
        setPath(null, 0, 0);

        setStartPosition(Double.MIN_VALUE, Double.MIN_VALUE);
        startDisplayed = false;

        commit();

        if (centered) {
            centerMap();
        }
    }
}
