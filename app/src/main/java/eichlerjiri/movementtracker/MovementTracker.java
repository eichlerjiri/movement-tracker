package eichlerjiri.movementtracker;

import android.Manifest;
import android.app.ActionBar;
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
import com.google.android.gms.maps.OnMapReadyCallback;

import java.util.ArrayList;

import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.ui.MapViewActivity;
import eichlerjiri.movementtracker.ui.MovementTypeButton;
import eichlerjiri.movementtracker.ui.TrackerMap;
import eichlerjiri.movementtracker.utils.Failure;
import eichlerjiri.movementtracker.utils.FormatUtils;
import eichlerjiri.movementtracker.utils.GeoUtils;

public class MovementTracker extends MapViewActivity {

    private Model m;

    private ActionBar.Tab recordingTab;
    private ActionBar.Tab historyTab;
    private LinearLayout recordingView;
    private LinearLayout historyView;

    private TextView recordingText;
    private ListView historyList;

    private final ArrayList<MovementTypeButton> buttons = new ArrayList<>();

    private TrackerMap trackerMap;

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

        try {
            doCreate();
        } catch (Failure ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m.unregisterMovementTracker(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        m.startedMovementTracker(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        m.stoppedMovementTracker(this);
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
            initTrackingService();
        }

        recordingText = new TextView(this);

        buttons.add(new MovementTypeButton(this, "walk"));
        buttons.add(new MovementTypeButton(this, "run"));
        buttons.add(new MovementTypeButton(this, "bike"));

        recordingView = new LinearLayout(this);
        recordingView.setOrientation(LinearLayout.VERTICAL);

        recordingView.addView(recordingText);
        for (MovementTypeButton button : buttons) {
            recordingView.addView(button);
        }
        recordingView.addView(mapView);

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

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                GeoUtils.waitForViewToBeReady(mapView, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            trackerMap = new TrackerMap(MovementTracker.this, mapView, googleMap);
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

                initTrackingService();
                if (trackerMap != null) {
                    trackerMap.tryEnableSelfLocations(this);
                }
                break;
            }
        }
    }

    private void initTrackingService() {
        Intent intent = new Intent(this, TrackingService.class);
        startService(intent);
        bindService(intent, serviceConnection, 0);
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

        recordingText.setText(text);
    }

    private View prepareHistoryView() throws Failure {
        if (historyView == null) {
            historyList = new ListView(this);

            historyView = new LinearLayout(this);
            historyView.addView(historyList);

            loadHistoryList();
        }
        return historyView;
    }

    private void loadHistoryList() throws Failure {
        final ArrayList<HistoryRow> historyItems = m.getDatabase().getHistory();
        String[] items = new String[historyItems.size()];
        for (int i = 0; i < items.length; i++) {
            HistoryRow item = historyItems.get(i);
            items[i] = FormatUtils.formatDistance(item.distance) + " " + item.movementType + " " +
                    FormatUtils.formatDateTime(item.ts);
        }

        ListView listView = historyList;
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MovementTracker.this, MovementDetail.class);
                intent.putExtra("id", historyItems.get(position).id);
                startActivity(intent);
            }
        });
    }

    public void lastLocationUpdated() {
        updateText();
        if (trackerMap != null) {
            trackerMap.updateLocation();
        }
    }

    public void recordingStarted() {
        updateText();
        if (trackerMap != null) {
            trackerMap.recordingStarted();
        }
    }

    public void recordingStopped() throws Failure {
        for (MovementTypeButton button : buttons) {
            button.resetBackground();
        }

        updateText();
        if (trackerMap != null) {
            trackerMap.recordingStopped();
        }
        if (historyView != null) {
            loadHistoryList();
        }
    }
}
