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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import eichlerjiri.mapcomponent.utils.ObjectList;
import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.ui.ExportButton;
import eichlerjiri.movementtracker.ui.MovementTypeButton;
import eichlerjiri.movementtracker.ui.TrackerMap;

import static eichlerjiri.mapcomponent.utils.Common.*;
import static eichlerjiri.movementtracker.utils.Common.*;
import static java.lang.Math.*;

public class MovementTracker extends Activity {

    public Model m;

    public ActionBar actionBar;
    public ActionBar.Tab recordingTab;
    public ActionBar.Tab historyTab;
    public LinearLayout recordingView;
    public RelativeLayout historyView;

    public TextView recordingText;
    public ObjectList<HistoryRow> historyItems;
    public ListView historyList;

    public final ObjectList<MovementTypeButton> buttons = new ObjectList<>(MovementTypeButton.class);

    public TrackerMap map;

    public final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
    public boolean serviceBound;

    public ExportButton lastExportButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m = Model.getInstance(this);
        m.registerMovementTracker(this);

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            initTrackingService();
        }

        recordingText = new TextView(this);

        int padding = round(4 * spSize(this));
        recordingText.setPadding(padding, 0, padding, 0);

        buttons.add(new MovementTypeButton(this, "walk"));
        buttons.add(new MovementTypeButton(this, "run"));
        buttons.add(new MovementTypeButton(this, "bike"));

        recordingView = new LinearLayout(this);
        recordingView.setOrientation(LinearLayout.VERTICAL);

        recordingView.addView(recordingText);

        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < buttons.size; i++) {
            MovementTypeButton button = buttons.data[i];
            button.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            buttonsLayout.addView(button);
        }

        recordingView.addView(buttonsLayout);

        map = new TrackerMap(this, splitNonEmpty(" ", getText(R.string.map_urls).toString()));
        recordingView.addView(map);

        setContentView(recordingView);

        actionBar = getActionBar();
        if (actionBar == null) {
            throw new Error("Action bar not available");
        }

        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                if (tab == recordingTab) {
                    setContentView(recordingView);
                } else if (tab == historyTab) {
                    setContentView(prepareHistoryView());
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        m.unregisterMovementTracker(this);

        if (serviceBound) {
            unbindService(serviceConnection);
        }

        map.close();
    }

    @Override
    public void onStart() {
        super.onStart();
        m.startedMovementTracker(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        m.stoppedMovementTracker(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    throw new Error("Permission to location service rejected.");
                }
                initTrackingService();
                break;
            } else if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i])) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    lastExportButton.showDateExportSelector();
                }
                break;
            }
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        actionBar.setSelectedNavigationItem(savedInstanceState.getInt("selectedTabIndex"));
        if (historyList != null) {
            historyList.onRestoreInstanceState(savedInstanceState.getParcelable("historyList"));
        }
        map.restoreInstanceState(savedInstanceState.getBundle("map"));
        map.donePositionInit = !map.centered;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("selectedTabIndex", actionBar.getSelectedNavigationIndex());
        if (historyList != null) {
            outState.putParcelable("historyList", historyList.onSaveInstanceState());
        }
        outState.putBundle("map", map.saveInstanceState());
    }

    public void initTrackingService() {
        Intent intent = new Intent(this, TrackingService.class);
        startService(intent);
        bindService(intent, serviceConnection, 0);
        serviceBound = true;
    }

    public void updateText() {
        String text = "GPS: ";

        Location l = m.lastLocation;
        if (l != null) {
            text += m.ft.formatCoord(l.getLatitude()) +
                    " " + m.ft.formatCoord(l.getLongitude()) +
                    " " + m.ft.formatAccuracy(l.getAccuracy());
        } else {
            text += "no location";
        }

        if (!m.activeRecordingType.isEmpty()) {
            long from = m.activeTsFrom;
            long to = m.activeTsTo;
            long duration = to - from;

            boolean sameDay = m.ft.isSameDay(from, to);

            text += "\nfrom " + m.ft.formatDateTime(from) +
                    " to " + (sameDay ? m.ft.formatTime(to) : m.ft.formatDateTime(to)) + "\n" +
                    "locations: " + m.activeLocations + "\n" +
                    "duration: " + m.ft.formatDuration(duration) + "\n" +
                    "distance: " + m.ft.formatDistance(m.activeDistance);

            double avgSpeed = avgSpeed(m.activeDistance, duration);
            if (avgSpeed != 0.0) {
                text += "\navg. speed: " + m.ft.formatSpeed(avgSpeed);
            }
        }

        recordingText.setText(text);
    }

    public View prepareHistoryView() {
        if (historyView == null) {
            historyView = new RelativeLayout(this);

            historyList = new ListView(this);
            historyList.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            historyView.addView(historyList);

            LinearLayout exportButtonLayout = new LinearLayout(this);
            exportButtonLayout.setOrientation(LinearLayout.VERTICAL);
            exportButtonLayout.addView(new ExportButton(this, "tcx", "TCX"));
            exportButtonLayout.addView(new ExportButton(this, "gpx", "GPX"));

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            exportButtonLayout.setLayoutParams(params);

            historyView.addView(exportButtonLayout);

            reloadHistoryList();
        } else {
            historyList.setAdapter(historyList.getAdapter()); // solves random bug, when items are not clickable
        }

        return historyView;
    }

    public void reloadHistoryList() {
        if (historyList == null) {
            return;
        }

        historyItems = m.database.getHistory();
        String[] items = new String[historyItems.size];
        for (int i = 0; i < items.length; i++) {
            HistoryRow item = historyItems.data[i];
            items[i] = m.ft.formatDistance(item.distance) + " " + item.movementType
                    + " " + m.ft.formatDateTime(item.ts);
        }

        historyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MovementTracker.this, MovementDetail.class);
                intent.putExtra("id", historyItems.data[position].id);
                startActivity(intent);
            }
        });
        historyList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
    }

    public void lastLocationUpdated(boolean recorded) {
        updateText();
        map.updateLocation(recorded);
    }

    public void lastKnownLocationUpdated() {
        map.updateLastKnownLocation();
    }

    public void recordingStarted() {
        updateText();
    }

    public void recordingStopped() {
        for (int i = 0; i < buttons.size; i++) {
            buttons.data[i].resetBackground();
        }

        updateText();
        map.recordingStopped();
        reloadHistoryList();
    }
}
