package eichlerjiri.movementtracker;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import static android.view.ViewGroup.LayoutParams.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import eichlerjiri.mapcomponent.utils.ObjectList;
import eichlerjiri.movementtracker.models.RecordingModel;
import eichlerjiri.movementtracker.models.RecordingModel.RecordingRow;
import eichlerjiri.movementtracker.ui.MovementTypeButton;
import eichlerjiri.movementtracker.ui.TrackerMap;
import static eichlerjiri.movementtracker.utils.Common.*;
import static java.lang.Math.*;

public class MovementTracker extends Activity {

    public App app;

    public ActionBar actionBar;
    public ActionBar.Tab recordingTab;
    public ActionBar.Tab historyTab;
    public LinearLayout recordingView;
    public ListView historyList;

    public TextView recordingText;
    public ObjectList<RecordingRow> historyItems;

    public final ObjectList<MovementTypeButton> buttons = new ObjectList<>(MovementTypeButton.class);

    public TrackerMap map;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getApplicationContext();
        app.registerMovementTracker(this);

        if (Build.VERSION.SDK_INT >= 29) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 0);
            } else {
                app.locationPermissionDone = true;
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            } else {
                app.locationPermissionDone = true;
            }
        } else {
            app.locationPermissionDone = true;
        }

        recordingText = new TextView(this);

        int padding = round(8 * getResources().getDisplayMetrics().scaledDensity);
        recordingText.setPadding(padding, 0, padding, 0);

        buttons.add(new MovementTypeButton(this, "walk"));
        buttons.add(new MovementTypeButton(this, "run"));
        buttons.add(new MovementTypeButton(this, "bike"));

        recordingView = new LinearLayout(this);
        recordingView.setOrientation(LinearLayout.VERTICAL);

        recordingView.addView(recordingText);

        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        for (int i = 0; i < buttons.size; i++) {
            MovementTypeButton button = buttons.data[i];
            button.setLayoutParams(new LinearLayout.LayoutParams(0, MATCH_PARENT, 1));
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
        app.unregisterMovementTracker(this);

        map.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        app.resumedMovementTracker();
    }

    @Override
    public void onPause() {
        super.onPause();
        app.pausedMovementTracker();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    throw new Error("Permission to location service rejected.");
                }
                app.locationPermissionDone = true;
                app.refreshReceiving();
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
        map.donePositionInit = !map.d.centered;
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

    public void updateText() {
        String text = "GPS: ";

        Location l = app.lastLocation;
        if (l != null) {
            text += formatCoord(l.getLatitude()) + " " + formatCoord(l.getLongitude()) + " " + formatAccuracy(l.getAccuracy());
        } else {
            text += "no location";
        }

        if (app.activeRecordingType != null) {
            long from = app.activeTsFrom;
            long to = app.activeTsTo;
            long duration = to - from;

            boolean sameDay = isSameDay(from, to);

            text += "\nfrom " + formatDateTimeCzech(from) + " to " + (sameDay ? formatTime(to) : formatDateTimeCzech(to)) + "\n"
                    + "locations: " + app.activeLocations + "\n"
                    + "duration: " + formatDuration(duration) + "\n"
                    + "distance: " + formatDistance(app.activeDistance);

            double avgSpeed = avgSpeed(app.activeDistance, duration);
            if (avgSpeed != 0.0) {
                text += "\navg. speed: " + formatSpeed(avgSpeed);
            }
        }

        recordingText.setText(text);
    }

    public View prepareHistoryView() {
        if (historyList == null) {
            historyList = new ListView(this);
            historyList.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            reloadHistoryList();
        } else {
            historyList.setAdapter(historyList.getAdapter()); // solves random bug, when items are not clickable
        }

        return historyList;
    }

    public void reloadHistoryList() {
        if (historyList == null) {
            return;
        }

        historyItems = RecordingModel.getRecordings(app);
        String[] items = new String[historyItems.size];
        for (int i = 0; i < items.length; i++) {
            RecordingRow item = historyItems.data[i];
            items[i] = formatDistance(item.distance) + " " + item.movementType + " " + formatDateTimeCzech(item.ts);
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
