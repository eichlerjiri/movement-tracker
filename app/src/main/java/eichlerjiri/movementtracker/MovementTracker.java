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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import eichlerjiri.movementtracker.db.HistoryItem;
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

    private boolean bound;

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

        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
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
            Intent intent = new Intent(this, TrackingService.class);
            startService(intent);
            bound = bindService(intent, serviceConnection, 0);
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

    public void lastLocationUpdated() {
        updateText();
    }

    private void updateText() {
        String text;

        Location l = m.getLastLocation();
        if (l != null) {
            text = "GPS: " + l.getLatitude() + " " + l.getLongitude();
        } else {
            text = "GPS: not yet obtained";
        }

        if (!m.getActiveRecordingType().isEmpty()) {
            long duration = m.getActiveTsTo() - m.getActiveTsFrom();

            text += "\nfrom: " + FormatUtils.formatDate(m.getActiveTsFrom()) + "\n" +
                    "to: " + FormatUtils.formatDate(m.getActiveTsTo()) + "\n" +
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

    public void recordingStarted() {
        updateText();
    }

    public void recordingStopped() throws Failure {
        for (MovementTypeButton button : buttons) {
            button.resetBackground();
        }

        updateText();

        if (historyView != null) {
            loadHistoryList();
        }
    }
}
