package eichlerjiri.movementtracker.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import static android.view.ViewGroup.LayoutParams.*;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import eichlerjiri.mapcomponent.utils.ObjectList;
import eichlerjiri.movementtracker.App;
import eichlerjiri.movementtracker.Database.HistoryRow;
import eichlerjiri.movementtracker.Database.LocationRow;
import eichlerjiri.movementtracker.MovementDetail;
import static eichlerjiri.movementtracker.utils.Common.*;
import java.io.InterruptedIOException;
import static java.lang.Math.*;

public class Exporter implements Runnable {

    public final Context c;
    public final App app;
    public final SharedPreferences preferences;
    public final HistoryRow recording;
    public final String format;

    public String url;
    public String filename;

    public EditText urlText;
    public EditText filenameText;
    public AlertDialog alertDialog;
    public String error = "";

    public Exporter(MovementDetail c, HistoryRow recording, String format) {
        this.c = c;
        app = (App) c.getApplicationContext();
        preferences = c.getSharedPreferences("movement-tracker", Context.MODE_PRIVATE);
        this.recording = recording;
        this.format = format;

        url = preferences.getString("exportUrl", "http://");
        filename = formatDateTimeShort(recording.ts) + "." + format;
    }

    public void exportTracks() {
        int padding = round(16 * c.getResources().getDisplayMetrics().scaledDensity);

        TextView urlLabel = new TextView(c);
        urlLabel.setText("URL:");

        urlText = new EditText(c);
        urlText.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        urlText.setText(url);

        TextView filenameLabel = new TextView(c);
        filenameLabel.setText("Filename:");
        filenameLabel.setPadding(0, padding, 0, 0);

        filenameText = new EditText(c);
        filenameText.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        filenameText.setText(filename);

        LinearLayout dialog = new LinearLayout(c);
        dialog.setOrientation(LinearLayout.VERTICAL);
        dialog.setPadding(padding, padding, padding, padding);
        dialog.addView(urlLabel);
        dialog.addView(urlText);
        dialog.addView(filenameLabel);
        dialog.addView(filenameText);

        new AlertDialog.Builder(c)
                .setTitle("Export recording")
                .setView(dialog)
                .setPositiveButton("Export", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        url = urlText.getText().toString();
                        filename = filenameText.getText().toString();

                        alertDialog = new AlertDialog.Builder(c)
                                .setMessage("Please wait")
                                .setTitle("Exporting " + filename)
                                .setCancelable(false)
                                .show();

                        new Thread(Exporter.this).start();
                    }
                })
                .show();
    }

    @Override
    public void run() {
        try {
            String data;
            if ("tcx".equals(format)) {
                data = writeTCX();
            } else {
                data = writeGPX();
            }

            error = uploadMultipartFile(url, strToBytes(data), filename);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    alertDialog.dismiss();

                    if (error.isEmpty()) {
                        Toast.makeText(c, "Exported " + filename, Toast.LENGTH_LONG).show();
                        preferences.edit().putString("exportUrl", url).apply();
                    } else {
                        Toast.makeText(c, "Export failed: " + error, Toast.LENGTH_LONG).show();
                        exportTracks();
                    }
                }
            });
        } catch (InterruptedIOException e) {
            // end
        }
    }

    public String writeTCX() {
        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<TrainingCenterDatabase"
                + " xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"
                + " http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\">\n");
        sb.append("<Activities>\n");

        String type = "";
        if ("walk".equals(recording.movementType)) {
            type = "Walking";
        } else if ("bike".equals(recording.movementType)) {
            type = "Biking";
        } else if ("run".equals(recording.movementType)) {
            type = "Running";
        }

        sb.append("<Activity Sport=\"");
        sb.append(type);
        sb.append("\">\n");
        sb.append("<Lap>\n");
        sb.append("<Track>\n");

        ObjectList<LocationRow> locs = app.database.getLocations(recording.id);
        for (int i = 0; i < locs.size; i++) {
            LocationRow loc = locs.data[i];

            sb.append("<Trackpoint>\n");
            sb.append("<Time>");
            sb.append(formatDateTimeISOUTC(loc.ts));
            sb.append("</Time>\n");
            sb.append("<Position>\n");
            sb.append("<LatitudeDegrees>");
            sb.append(loc.lat);
            sb.append("</LatitudeDegrees>\n");
            sb.append("<LongitudeDegrees>");
            sb.append(loc.lon);
            sb.append("</LongitudeDegrees>\n");
            sb.append("</Position>\n");
            sb.append("</Trackpoint>\n");
        }

        sb.append("</Track>\n");
        sb.append("</Lap>\n");
        sb.append("</Activity>\n");

        sb.append("</Activities>\n");
        sb.append("</TrainingCenterDatabase>\n");

        return sb.toString();
    }

    public String writeGPX() {
        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<gpx version=\"1.1\""
                + " xmlns=\"http://www.topografix.com/GPX/1/1\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1"
                + " http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");

        String type = "";
        if ("walk".equals(recording.movementType)) {
            type = "Walking";
        } else if ("bike".equals(recording.movementType)) {
            type = "Biking";
        } else if ("run".equals(recording.movementType)) {
            type = "Running";
        }

        sb.append("<trk>\n");
        sb.append("<type>");
        sb.append(type);
        sb.append("</type>\n");
        sb.append("<trkseg>\n");

        ObjectList<LocationRow> locs = app.database.getLocations(recording.id);
        for (int i = 0; i < locs.size; i++) {
            LocationRow loc = locs.data[i];

            sb.append("<trkpt lat=\"");
            sb.append(loc.lat);
            sb.append("\" lon=\"");
            sb.append(loc.lon);
            sb.append("\">\n");
            sb.append("<time>");
            sb.append(formatDateTimeISOUTC(loc.ts));
            sb.append("</time>\n");
            sb.append("</trkpt>\n");
        }

        sb.append("</trkseg>\n");
        sb.append("</trk>\n");

        sb.append("</gpx>\n");

        return sb.toString();
    }
}
