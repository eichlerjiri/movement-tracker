package eichlerjiri.movementtracker.ui;

import android.content.Context;
import android.location.Location;
import android.view.ViewTreeObserver;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.DoubleList;
import eichlerjiri.mapcomponent.utils.ObjectList;
import eichlerjiri.movementtracker.App;
import eichlerjiri.movementtracker.db.LocationRow;
import eichlerjiri.movementtracker.utils.GeoBoundary;

import static eichlerjiri.movementtracker.utils.Common.*;

public class TrackerMap extends MapComponent {

    public final App app;

    public boolean startDisplayed;
    public DoubleList pathPositions = new DoubleList();

    public boolean donePositionInit;

    public TrackerMap(Context c, ObjectList<String> mapUrls) {
        super(c, mapUrls);
        app = App.get(c);

        if (app.lastLocation != null) {
            updateLocation(false);
        }

        if (app.activeRecordingType != null) {
            ObjectList<LocationRow> locs = app.database.getLocations(app.activeRecording);
            for (int i = 0; i < locs.size; i++) {
                LocationRow row = locs.data[i];
                pathPositions.add(lonToMercatorX(row.lon), latToMercatorY(row.lat));
            }
            setPath(pathPositions.data, 0, pathPositions.size);

            if (locs.size != 0) {
                LocationRow first = locs.data[0];
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
                    commit();
                }
            }
        });
    }

    @Override
    public void centerMap() {
        Location last = app.lastLocation;
        Location lastKnown = app.lastKnownLocation;
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

        if (app.activeRecordingType != null) {
            GeoBoundary geoBoundary = new GeoBoundary(app.activeGeoBoundary);
            geoBoundary.addPoint(lonToMercatorX(lon), latToMercatorY(lat));
            moveToBoundary(geoBoundary.minX, geoBoundary.minY, geoBoundary.maxX, geoBoundary.maxY, zoom, 30);
        } else {
            setPosition(lonToMercatorX(lon), latToMercatorY(lat));
            setZoom(zoom);
            setAzimuth(0);
        }
    }

    public void updateLocation(boolean recorded) {
        Location l = app.lastLocation;
        double lat = l.getLatitude();
        double lon = l.getLongitude();

        float bearing = l.hasBearing() ? l.getBearing() : Float.NEGATIVE_INFINITY;
        setCurrentPosition(lonToMercatorX(lon), latToMercatorY(lat), bearing);

        if (recorded) {
            if (!startDisplayed) {
                setStartPosition(lonToMercatorX(lon), latToMercatorY(lat));
                startDisplayed = true;
            }

            pathPositions.add(lonToMercatorX(lon), latToMercatorY(lat));
            setPath(pathPositions.data, 0, pathPositions.size);
        }

        if (d.centered) {
            centerMap();
        }

        commit();
    }

    public void updateLastKnownLocation() {
        if (d.centered) {
            centerMap();
            commit();
        }
    }

    public void recordingStopped() {
        pathPositions = new DoubleList();
        setPath(null, 0, 0);

        setStartPosition(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        startDisplayed = false;

        if (d.centered) {
            centerMap();
        }

        commit();
    }
}
