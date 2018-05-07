package eichlerjiri.movementtracker;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eichlerjiri.movementtracker.db.HistoryItem;
import eichlerjiri.movementtracker.db.LocationDb;
import eichlerjiri.movementtracker.ui.MovementTypeButton;
import eichlerjiri.movementtracker.utils.Failure;
import eichlerjiri.movementtracker.utils.FormatUtils;
import eichlerjiri.movementtracker.utils.GeoUtils;

public class MovementTracker extends Activity {

    private Model m;

    private ActionBar.Tab recordingTab;
    private ActionBar.Tab historyTab;
    private LinearLayout recordingView;
    private LinearLayout historyView;
    private final ArrayList<MovementTypeButton> buttons = new ArrayList<>();

    private MapView mapView;
    private GoogleMap mapInterface;
    private Polyline polyline;
    private LocationSource.OnLocationChangedListener googleLocationChangedListener;
    private boolean keepMapCentered = true;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m = Model.getInstance();
        m.registerMovementTracker(this);
        mapView = new MapView(this);
        mapView.onCreate(savedInstanceState);

        try {
            doCreate();
        } catch (Failure ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m.unregisterMovementTracker(this);
        mapView.onDestroy();

        unbindService(serviceConnection);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();

        m.startedMovementTracker(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();

        m.stoppedMovementTracker(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        try {
            handlePermissionsResult(permissions, grantResults);
        } catch (Failure ignored) {
        }
    }

    private void doCreate() throws Failure {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            Intent intent = new Intent(this, TrackingService.class);
            startService(intent);
            if (!bindService(intent, serviceConnection, 0)) {
                throw new Failure("Cannot bind tracking service");
            }
        }

        recordingView = (LinearLayout) getLayoutInflater().inflate(R.layout.recording, null);

        buttons.add(new MovementTypeButton(this, "walk"));
        buttons.add(new MovementTypeButton(this, "run"));
        buttons.add(new MovementTypeButton(this, "bike"));

        for (MovementTypeButton button : buttons) {
            recordingView.addView(button);
        }

        setContentView(recordingView);

        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            throw new Failure("Action bar not available");
        }

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                if (tab == recordingTab) {
                    setContentView(recordingView);
                } else if (tab == historyTab) {
                    try {
                        setContentView(prepareHistoryView());
                    } catch (Failure ignored) {
                    }
                }
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            }
        };

        recordingTab = actionBar.newTab()
                .setText("Recording")
                .setTabListener(tabListener);
        historyTab = actionBar.newTab()
                .setText("History")
                .setTabListener(tabListener);

        actionBar.addTab(recordingTab);
        actionBar.addTab(historyTab);

        updateText();

        recordingView.addView(mapView);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mapInterface = googleMap;

                GeoUtils.waitForViewToBeReady(mapView, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            initMap();
                        } catch (Failure ignored) {
                        }
                    }
                });
            }
        });
    }

    private void handlePermissionsResult(String[] permissions, int[] grantResults) throws Failure {
        for (int i = 0; i < permissions.length; i++) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    throw new Failure("Permission to location service rejected.");
                }
                startService(new Intent(this, TrackingService.class));
                break;
            }
        }
    }

    private View prepareHistoryView() throws Failure {
        if (historyView == null) {
            historyView = (LinearLayout) getLayoutInflater().inflate(R.layout.history, null);
            loadHistoryList();
        }
        return historyView;
    }

    private void loadHistoryList() throws Failure {
        SimpleDateFormat formatter = new SimpleDateFormat("d.M.yyyy HH:mm", Locale.US);

        final ArrayList<HistoryItem> historyItems = m.getDatabase().getHistory();
        String[] items = new String[historyItems.size()];
        for (int i = 0; i < items.length; i++) {
            HistoryItem item = historyItems.get(i);
            items[i] = item.getMovementType() + " " + formatter.format(new Date(item.getTsFrom()));
        }

        ListView listView = historyView.findViewById(R.id.historyList);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MovementTracker.this, MovementDetail.class);
                intent.putExtra("id", historyItems.get(position).getId());
                startActivity(intent);
            }
        });
    }

    private void initMap() throws Failure {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            throw new Failure("Missing permissions for location service.");
        }

        mapInterface.setLocationSource(new LocationSource() {
            @Override
            public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
                googleLocationChangedListener = onLocationChangedListener;
                Location l = m.getLastLocation();
                if (l != null) {
                    googleLocationChangedListener.onLocationChanged(l);
                }
            }

            @Override
            public void deactivate() {
                googleLocationChangedListener = null;
            }
        });
        mapInterface.setMyLocationEnabled(true);

        mapInterface.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int var1) {
                if (var1 == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    keepMapCentered = false;
                }
            }
        });

        mapInterface.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                keepMapCentered = true;
                centerMap();
                return true;
            }
        });

        if (!m.getActiveRecordingType().isEmpty()) {
            ArrayList<LatLng> points = new ArrayList<>();
            for (LocationDb location : m.getDatabase().getLocationsFrom(m.getActiveTsFrom())) {
                points.add(new LatLng(location.getLat(), location.getLon()));
            }
            polyline = mapInterface.addPolyline(new PolylineOptions().addAll(points));
        }

        centerMap();
    }

    public void lastLocationUpdated() {
        updateText();
        updateMap();
    }

    private void updateText() {
        String text;

        Location l = m.getLastLocation();
        if (l != null) {
            text = "GPS: " + FormatUtils.formatCoord(l.getLatitude()) +
                    " " + FormatUtils.formatCoord(l.getLongitude()) +
                    " " + FormatUtils.formatAccuracy(l.getAccuracy());
        } else {
            text = "GPS: not yet obtained";
        }

        if (!m.getActiveRecordingType().isEmpty()) {
            long from = m.getActiveTsFrom();
            long to = m.getActiveTsTo();
            long duration = to - from;

            boolean sameDay = FormatUtils.isSameDay(from, to);

            text += "\nfrom " + FormatUtils.formatDateTimeSecs(from) +
                    " to " + (sameDay ? FormatUtils.formatTimeSecs(to) : FormatUtils.formatDateTimeSecs(to)) + "\n" +
                    "locations: " + m.getActiveLocations() + "\n" +
                    "duration: " + FormatUtils.formatDuration(duration) + "\n" +
                    "distance: " + FormatUtils.formatDistance(m.getActiveDistance());

            double avgSpeed = GeoUtils.avgSpeed(m.getActiveDistance(), duration);
            if (avgSpeed != 0.0) {
                text += "\navg. speed: " + FormatUtils.formatSpeed(avgSpeed);
            }
        }

        TextView tv = recordingView.findViewById(R.id.text);
        tv.setText(text);
    }

    private void updateMap() {
        Location l = m.getLastLocation();

        if (googleLocationChangedListener != null) {
            googleLocationChangedListener.onLocationChanged(l);
        }
        if (mapInterface != null) {
            if (!m.getActiveRecordingType().isEmpty()) {
                List<LatLng> points = polyline.getPoints();
                points.add(new LatLng(l.getLatitude(), l.getLongitude()));
                polyline.setPoints(points);
            }

            if (keepMapCentered) {
                centerMap();
            }
        }
    }

    private void centerMap() {
        if (!m.getActiveRecordingType().isEmpty()) {
            if (m.getActiveMinLat() != Double.MAX_VALUE) {
                GeoUtils.moveToRect(mapView, mapInterface, m.getActiveMinLat(), m.getActiveMinLon(),
                        m.getActiveMaxLat(), m.getActiveMaxLon());
            }
        } else {
            Location l = m.getLastLocation();
            if (l != null) {
                GeoUtils.moveToPoint(mapInterface, l.getLatitude(), l.getLongitude());
            }
        }
    }

    public void recordingStarted() {
        updateText();

        if (mapInterface != null) {
            polyline = mapInterface.addPolyline(new PolylineOptions());
        }
    }

    public void recordingStopped() throws Failure {
        for (MovementTypeButton button : buttons) {
            button.resetBackground();
        }

        updateText();

        if (mapInterface != null) {
            polyline.remove();
        }

        if (historyView != null) {
            loadHistoryList();
        }
    }
}
