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

    static void threaded(final Context c, final AlertDialog alertDialog, long sinceTs, String format) {
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

    private static int doExport(Context c, File docsDir, long sinceTs, String format) throws IOException {
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

    private static int doWriteTCX(Database d, long sinceTs, BufferedWriter w) throws IOException {
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        w.write("<TrainingCenterDatabase" +
                " xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2" +
                " http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\">\n");
        w.write("<Activities>\n");

        SimpleDateFormat utcFormatter = utcFormatter();

        int cnt = 0;
        for (HistoryRow row : d.getHistorySince(sinceTs)) {
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

            for (LocationRow loc : d.getLocations(row.id)) {
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

            cnt++;
        }

        w.write("</Activities>\n");
        w.write("</TrainingCenterDatabase>\n");

        return cnt;
    }

    private static int doWriteGPX(Database d, long sinceTs, BufferedWriter w) throws IOException {
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        w.write("<gpx version=\"1.1\"" +
                " xmlns=\"http://www.topografix.com/GPX/1/1\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1" +
                " http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");

        SimpleDateFormat utcFormatter = utcFormatter();

        int cnt = 0;
        for (HistoryRow row : d.getHistorySince(sinceTs)) {
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

            for (LocationRow loc : d.getLocations(row.id)) {
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

            cnt++;
        }

        w.write("</gpx>\n");

        return cnt;
    }

    private static SimpleDateFormat utcFormatter() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter;
    }
}
