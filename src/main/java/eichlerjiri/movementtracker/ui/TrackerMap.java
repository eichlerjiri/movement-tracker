package eichlerjiri.movementtracker.ui;

public class TrackerMap {

   /* private final Model m;
    private final MapView mapView;
    private final GoogleMap googleMap;

    private LocationSource.OnLocationChangedListener googleLocationChangedListener;
    private boolean keepMapCentered = true;
    private Polyline polyline;
    private Marker marker;

    public TrackerMap(Context c, MapView mapView, GoogleMap googleMap) throws Failure {
        m = Model.getInstance();
        this.mapView = mapView;
        this.googleMap = googleMap;

        googleMap.setLocationSource(new LocationSource() {
            @Override
            public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
                googleLocationChangedListener = onLocationChangedListener;
            }

            @Override
            public void deactivate() {
                googleLocationChangedListener = null;
            }
        });

        googleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                if (i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    keepMapCentered = false;
                }
            }
        });

        googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                centerMap();
                return true;
            }
        });

        if (!m.getActiveRecordingType().isEmpty()) {
            ArrayList<LocationRow> locs = m.getDatabase().getLocations(m.getActiveRecording());

            polyline = googleMap.addPolyline(GeoUtils.createPolyline(locs));
            if (!locs.isEmpty()) {
                marker = googleMap.addMarker(GeoUtils.createMarker(locs.get(0), BitmapDescriptorFactory.HUE_GREEN));
            }
        }

        centerMap();
        tryEnableSelfLocations(c);
    }

    public void updateLocation(boolean recorded) {
        Location l = m.getLastLocation();

        if (googleLocationChangedListener != null) {
            googleLocationChangedListener.onLocationChanged(l);
        }

        if (recorded) {
            LatLng latLng = new LatLng(l.getLatitude(), l.getLongitude());

            List<LatLng> points = polyline.getPoints();
            points.add(latLng);
            polyline.setPoints(points);

            if (marker == null) {
                marker = googleMap.addMarker(GeoUtils.createMarker(latLng, BitmapDescriptorFactory.HUE_GREEN));
            }
        }

        if (keepMapCentered) {
            centerMap();
        }
    }

    private void centerMap() {
        keepMapCentered = true;

        if (!m.getActiveRecordingType().isEmpty() && m.getActiveLocations() >= 2) {
            GeoBoundary geoBoundary = m.getActiveGeoBoundary();
            Location l = m.getLastLocation();
            if (l != null) {
                geoBoundary = new GeoBoundary(geoBoundary);
                geoBoundary.addPoint(l.getLatitude(), l.getLongitude());
            }

            GeoUtils.moveToRect(mapView, googleMap, geoBoundary);
        } else {
            Location l = m.getLastLocation();
            if (l != null) {
                GeoUtils.moveToPoint(googleMap, l.getLatitude(), l.getLongitude());
            }
        }
    }

    public void recordingStarted() {
        polyline = googleMap.addPolyline(GeoUtils.createPolyline());
    }

    public void recordingStopped() {
        polyline.remove();
        if (marker != null) {
            marker.remove();
            marker = null;
        }
    }

    public void tryEnableSelfLocations(Context c) {
        if (Build.VERSION.SDK_INT >= 23 && c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }*/
}
