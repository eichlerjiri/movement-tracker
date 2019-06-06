package eichlerjiri.movementtracker.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import eichlerjiri.mapcomponent.utils.ObjectList;
import eichlerjiri.movementtracker.Database;
import eichlerjiri.movementtracker.Model;
import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.db.LocationRow;

import static eichlerjiri.movementtracker.utils.Common.*;

public class Exporter {

    public static void exportTracks(final Context c, final long sinceTs, final String format) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c)
                .setMessage("Please wait")
                .setTitle("Exporting " + format.toUpperCase(Locale.US));

        final AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false);
        alertDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                threaded(c, alertDialog, sinceTs, format);
            }
        }).start();
    }

    public static void threaded(final Context c, final AlertDialog alertDialog, long sinceTs, String format) {
        String dirLocs;
        if (Build.VERSION.SDK_INT >= 19) {
            dirLocs = Environment.DIRECTORY_DOCUMENTS;
        } else {
            dirLocs = Environment.DIRECTORY_DOWNLOADS;
        }
        File docsDir = Environment.getExternalStoragePublicDirectory(dirLocs);
        docsDir.mkdirs();

        String filename = "MovementTracker " + formatDateTimeISO(System.currentTimeMillis()) + "." + format;

        String res;
        try {
            int cnt = doExport(c, new File(docsDir, filename), sinceTs, format);
            res = "Exported " + cnt + " recordings to " + dirLocs + "/" + filename;
        } catch (IOException e) {
            Log.e("Exporter", "Export failed", e);
            res = "Export failed: " + e.getMessage();
        }
        final String ress = res;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                alertDialog.hide();
                Toast.makeText(c, ress, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static int doExport(Context c, File docsDir, long sinceTs, String format) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(docsDir), "UTF-8"));
        try {
            if ("tcx".equals(format)) {
                return doWriteTCX(Model.getInstance(c).database, sinceTs, w);
            } else {
                return doWriteGPX(Model.getInstance(c).database, sinceTs, w);
            }
        } finally {
            try {
                w.close();
            } catch (IOException e) {
                Log.e("Exporter", "Cannot close stream", e);
            }
        }
    }

    public static int doWriteTCX(Database d, long sinceTs, BufferedWriter w) throws IOException {
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        w.write("<TrainingCenterDatabase" +
                " xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2" +
                " http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\">\n");
        w.write("<Activities>\n");

        SimpleDateFormat utcFormatter = utcFormatter();

        ObjectList<HistoryRow> rows = d.getHistorySince(sinceTs);
        for (int i = 0; i < rows.size; i++) {
            HistoryRow row = rows.data[i];

            String type = "";
            if ("walk".equals(row.movementType)) {
                type = "Walking";
            } else if ("bike".equals(row.movementType)) {
                type = "Biking";
            } else if ("run".equals(row.movementType)) {
                type = "Running";
            }

            w.write("<Activity Sport=\"");
            w.write(type);
            w.write("\">\n");
            w.write("<Lap>\n");
            w.write("<Track>\n");

            ObjectList<LocationRow> locs = d.getLocations(row.id);
            for (int j = 0; j < locs.size; j++) {
                LocationRow loc = locs.data[j];

                w.write("<Trackpoint>\n");
                w.write("<Time>");
                w.write(utcFormatter.format(new Date(loc.ts)));
                w.write("</Time>\n");
                w.write("<Position>\n");
                w.write("<LatitudeDegrees>");
                w.write(Double.toString(loc.lat));
                w.write("</LatitudeDegrees>\n");
                w.write("<LongitudeDegrees>");
                w.write(Double.toString(loc.lon));
                w.write("</LongitudeDegrees>\n");
                w.write("</Position>\n");
                w.write("</Trackpoint>\n");
            }

            w.write("</Track>\n");
            w.write("</Lap>\n");
            w.write("</Activity>\n");
        }

        w.write("</Activities>\n");
        w.write("</TrainingCenterDatabase>\n");

        return rows.size;
    }

    public static int doWriteGPX(Database d, long sinceTs, BufferedWriter w) throws IOException {
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        w.write("<gpx version=\"1.1\"" +
                " xmlns=\"http://www.topografix.com/GPX/1/1\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1" +
                " http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");

        SimpleDateFormat utcFormatter = utcFormatter();

        ObjectList<HistoryRow> rows = d.getHistorySince(sinceTs);
        for (int i = 0; i < rows.size; i++) {
            HistoryRow row = rows.data[i];
            String type = "";
            if ("walk".equals(row.movementType)) {
                type = "Walking";
            } else if ("bike".equals(row.movementType)) {
                type = "Biking";
            } else if ("run".equals(row.movementType)) {
                type = "Running";
            }

            w.write("<trk>\n");
            w.write("<type>");
            w.write(type);
            w.write("</type>\n");
            w.write("<trkseg>\n");

            ObjectList<LocationRow> locs = d.getLocations(row.id);
            for (int j = 0; j < locs.size; j++) {
                LocationRow loc = locs.data[j];

                w.write("<trkpt lat=\"");
                w.write(Double.toString(loc.lat));
                w.write("\" lon=\"");
                w.write(Double.toString(loc.lon));
                w.write("\">\n");
                w.write("<time>");
                w.write(utcFormatter.format(new Date(loc.ts)));
                w.write("</time>\n");
                w.write("</trkpt>\n");
            }

            w.write("</trkseg>\n");
            w.write("</trk>\n");
        }

        w.write("</gpx>\n");

        return rows.size;
    }

    public static SimpleDateFormat utcFormatter() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter;
    }
}
