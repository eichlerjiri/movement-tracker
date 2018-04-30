package eichlerjiri.movementtracker;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import eichlerjiri.movementtracker.db.HistoryItem;
import eichlerjiri.movementtracker.db.LocationDb;
import eichlerjiri.movementtracker.utils.GeoUtils;

public class MovementDetail extends Activity {

    private Model m;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m = Model.getInstance();
        m.registerMovementDetail(this);
        setContentView(R.layout.detail);

        TextView tv = findViewById(R.id.detailText);

        Bundle b = getIntent().getExtras();
        if (b == null) {
            tv.setText("Detail not available");
            return;
        }

        long id = b.getLong("id");
        if (id <= 0) {
            tv.setText("Detail not available");
            return;
        }

        HistoryItem item = m.getDatabase().getHistoryItem(id);
        if (item == null) {
            tv.setText("Detail not available");
            return;
        }

        long duration = item.getTsTo() - item.getTsFrom();
        double distance = 0.0;

        LocationDb prev = null;
        for (LocationDb location : m.getDatabase().getLocations(item.getTsFrom(), item.getTsTo())) {
            if (prev != null) {
                distance += GeoUtils.distance(prev.getLat(), prev.getLon(), location.getLat(), location.getLon());
            }
            prev = location;
        }

        double avgSpeed = -1;
        if (duration > 0) {
            avgSpeed = distance / duration;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("d.M.yyyy HH:mm", Locale.US);

        String text = "from: " + formatter.format(new Date(item.getTsFrom())) + "\n" +
                "to: " + formatter.format(new Date(item.getTsTo())) + "\n" +
                "type: " + item.getMovementType() +
                "duration: " + formatDuration(duration) +
                "distance: " + formatDistance(distance);

        if (avgSpeed >= 0) {
            text += "avg. speed: " + formatSpeed(avgSpeed);
        }

        tv.setText(text);
    }

    private String formatDuration(long millis) {
        long secondsFull = millis / 1000;
        long seconds = secondsFull % 60;
        long minutesFull = secondsFull / 60;
        long minutes = minutesFull % 60;
        long hours = minutesFull / 60;

        DecimalFormat df = new DecimalFormat("00", new DecimalFormatSymbols(Locale.US));
        return df.format(hours) + ":" + df.format(minutes) + ":" + df.format(seconds);
    }

    private String formatDistance(double distance) {
        if (distance >= 1000) {
            DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
            return df.format(distance / 1000) + "km";
        } else {
            DecimalFormat df = new DecimalFormat("0", new DecimalFormatSymbols(Locale.US));
            return df.format(distance) + "m";
        }
    }

    private String formatSpeed(double speed) {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        return df.format(speed / 3.6) + "km/h";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m.unregisterMovementDetail(this);
    }
}
