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

public class MovementTracker extends Activity {

    private Model m;

    private ActionBar.Tab recordingTab;
    private ActionBar.Tab historyTab;
    private LinearLayout recordingView;
    private LinearLayout historyView;
    private final ArrayList<MovementTypeButton> buttons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m = Model.getInstance();
        m.registerMovementTracker(this);

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            startService(new Intent(this, TrackingService.class));
        }

        recordingView = (LinearLayout) getLayoutInflater().inflate(R.layout.recording, null);

        buttons.add(new MovementTypeButton(this, "bike"));
        buttons.add(new MovementTypeButton(this, "walk"));

        for (MovementTypeButton button : buttons) {
            recordingView.addView(button);
        }

        setContentView(recordingView);

        ActionBar actionBar = getActionBar();
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
                .setText(R.string.recording)
                .setTabListener(tabListener);
        historyTab = actionBar.newTab()
                .setText(R.string.history)
                .setTabListener(tabListener);

        actionBar.addTab(recordingTab);
        actionBar.addTab(historyTab);

        updateText();
    }

    private View prepareHistoryView() {
        if (historyView == null) {
            historyView = (LinearLayout) getLayoutInflater().inflate(R.layout.history, null);
            loadHistoryList();
        }
        return historyView;
    }

    private void loadHistoryList() {
        SimpleDateFormat formatter = new SimpleDateFormat("d.M.yyyy HH:mm", Locale.US);

        final ArrayList<HistoryItem> historyItems = m.getDatabase().getHistory();
        String[] items = new String[historyItems.size()];
        for (int i = 0; i < items.length; i++) {
            HistoryItem item = historyItems.get(i);
            items[i] = item.getMovementType() + " " + formatter.format(new Date(item.getTsFrom()));
        }

        ListView listView = findViewById(R.id.historyList);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m.unregisterMovementTracker(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    startService(new Intent(this, TrackingService.class));
                } else {
                    m.setLocationStatus("Permission to location service rejected");
                }
            }
        }
    }

    public void locationStatusUpdated() {
        updateText();
    }

    public void databaseErrorUpdated() {
        updateText();
    }

    public void lastLocationUpdated() {
        updateText();
    }

    private void updateText() {
        String locationStatus = m.getLocationStatus();
        String databaseError = m.getDatabaseError();
        Location l = m.getLastLocation();

        String ret = "";
        if (!locationStatus.isEmpty()) {
            ret += "Status: " + locationStatus + "\n";
        }
        if (!databaseError.isEmpty()) {
            ret += "Database error: " + databaseError + "\n";
        }
        if (l != null) {
            ret += "Location: " + l.getProvider() + " " + l.getLatitude() + " " + l.getLongitude();
        }

        TextView tv = findViewById(R.id.text);
        tv.setText(ret);
    }

    public void recordingStopped() {
        for (MovementTypeButton button : buttons) {
            button.resetBackground();
        }

        if (historyView != null) {
            loadHistoryList();
        }
    }
}
