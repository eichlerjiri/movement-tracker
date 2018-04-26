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
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MovementTracker extends Activity {

    private static final String TAG = "MovementTracker";

    private Model m;

    private ActionBar.Tab recordingTab;
    private ActionBar.Tab historyTab;
    private View recordingView;
    private View historyView;

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

        recordingView = getLayoutInflater().inflate(R.layout.recording, null);
        historyView = getLayoutInflater().inflate(R.layout.history, null);
        setContentView(recordingView);

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                if (tab == recordingTab) {
                    setContentView(recordingView);
                } else if (tab == historyTab) {
                    setContentView(historyView);
                } else {
                    Log.e(TAG, "Unknown tab selected");
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
                    m.setStatus("Permission to location service rejected");
                }
            }
        }
    }

    public void statusUpdated() {
        updateText();
    }

    public void lastLocationUpdated() {
        updateText();
    }

    private void updateText() {
        String status = m.getStatus();
        Location l = m.getLastLocation();

        String ret = "";
        if (!status.isEmpty()) {
            ret += "Status: " + status + "\n";
        }
        if (l != null) {
            ret += "Location: " + l.getProvider() + " " + l.getLatitude() + " " + l.getLongitude();
        }

        Log.i(TAG, "UPDATING TEXT: " + ret);
        TextView tv = findViewById(R.id.text);
        tv.setText(ret);
    }
}
