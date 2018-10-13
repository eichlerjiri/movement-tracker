package eichlerjiri.movementtracker;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
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
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.ui.Exporter;
import eichlerjiri.movementtracker.ui.MovementTypeButton;
import eichlerjiri.movementtracker.ui.TrackerMap;

import static eichlerjiri.mapcomponent.utils.Common.*;
import static eichlerjiri.movementtracker.utils.Common.*;

public class MovementTracker extends Activity {

    private Model m;

    private ActionBar actionBar;
    private ActionBar.Tab recordingTab;
    private ActionBar.Tab historyTab;
    private LinearLayout recordingView;
    private RelativeLayout historyView;

    private TextView recordingText;
    private ListView historyList;

    private final ArrayList<MovementTypeButton> buttons = new ArrayList<>();

    private TrackerMap map;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
    private boolean serviceBound;

    private String formatClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        int padding = Math.round(4 * spSize(this));
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

        for (MovementTypeButton button : buttons) {
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
    protected void onDestroy() {
        super.onDestroy();
        m.unregisterMovementTracker(this);

        if (serviceBound) {
            unbindService(serviceConnection);
        }

        map.close();
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
                    showDateExportSelector(formatClicked);
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

    private void initTrackingService() {
        Intent intent = new Intent(this, TrackingService.class);
        startService(intent);
        bindService(intent, serviceConnection, 0);
        serviceBound = true;
    }

    private void updateText() {
        String text = "GPS: ";

        Location l = m.lastLocation;
        if (l != null) {
            text += formatCoord(l.getLatitude()) +
                    " " + formatCoord(l.getLongitude()) +
                    " " + formatAccuracy(l.getAccuracy());
        } else {
            text += "no location";
        }

        if (!m.activeRecordingType.isEmpty()) {
            long from = m.activeTsFrom;
            long to = m.activeTsTo;
            long duration = to - from;

            boolean sameDay = isSameDay(from, to);

            text += "\nfrom " + formatDateTime(from) +
                    " to " + (sameDay ? formatTime(to) : formatDateTime(to)) + "\n" +
                    "locations: " + m.activeLocations + "\n" +
                    "duration: " + formatDuration(duration) + "\n" +
                    "distance: " + formatDistance(m.activeDistance);

            double avgSpeed = avgSpeed(m.activeDistance, duration);
            if (avgSpeed != 0.0) {
                text += "\navg. speed: " + formatSpeed(avgSpeed);
            }
        }

        recordingText.setText(text);
    }

    private View prepareHistoryView() {
        if (historyView == null) {
            historyView = new RelativeLayout(this);

            historyList = new ListView(this);
            historyList.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            historyView.addView(historyList);

            LinearLayout exportButtonLayout = new LinearLayout(this);
            exportButtonLayout.setOrientation(LinearLayout.VERTICAL);
            exportButtonLayout.addView(prepareExportButton("tcx"));
            exportButtonLayout.addView(prepareExportButton("gpx"));

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

    private Button prepareExportButton(final String format) {
        Button exportButton = new Button(this);
        exportButton.setText("Export " + format.toUpperCase());
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    formatClicked = format;
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                } else {
                    showDateExportSelector(format);
                }
            }
        });
        return exportButton;
    }

    private void showDateExportSelector(final String format) {
        GregorianCalendar cal = new GregorianCalendar();

        DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                GregorianCalendar c = new GregorianCalendar(year, month, dayOfMonth);
                Exporter.exportTracks(MovementTracker.this, c.getTimeInMillis(), format);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        dialog.setMessage("Export recordings since:");
        dialog.show();
    }

    public void reloadHistoryList() {
        if (historyList == null) {
            return;
        }

        final ArrayList<HistoryRow> historyItems = m.database.getHistory();
        String[] items = new String[historyItems.size()];
        for (int i = 0; i < items.length; i++) {
            HistoryRow item = historyItems.get(i);
            items[i] = formatDistance(item.distance) + " " + item.movementType + " " + formatDateTime(item.ts);
        }

        historyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MovementTracker.this, MovementDetail.class);
                intent.putExtra("id", historyItems.get(position).id);
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
        for (MovementTypeButton button : buttons) {
            button.resetBackground();
        }

        updateText();
        map.recordingStopped();
        reloadHistoryList();
    }
}
