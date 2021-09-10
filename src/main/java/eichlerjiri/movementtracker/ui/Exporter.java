package eichlerjiri.movementtracker.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import eichlerjiri.mapcomponent.utils.ObjectList;
import eichlerjiri.movementtracker.App;
import eichlerjiri.movementtracker.Database;
import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.db.LocationRow;
import eichlerjiri.movementtracker.utils.FormatTools;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class Exporter implements Runnable {

    public final Context c;
    public final long sinceTs;
    public final String format;
    public final String formatTitle;

    public AlertDialog alertDialog;
    public FormatTools ft;
    public String res;

    public Exporter(Context c, long sinceTs, String format, String formatTitle) {
        this.c = c;
        this.sinceTs = sinceTs;
        this.format = format;
        this.formatTitle = formatTitle;
    }

    public void exportTracks() {
        alertDialog = new AlertDialog.Builder(c)
                .setMessage("Please wait")
                .setTitle("Exporting " + formatTitle)
                .setCancelable(false)
                .show();

        new Thread(this).start();
    }

    @Override
    public void run() {
        ft = new FormatTools();

        String dirLocs;
        if (Build.VERSION.SDK_INT >= 19) {
            dirLocs = Environment.DIRECTORY_DOCUMENTS;
        } else {
            dirLocs = Environment.DIRECTORY_DOWNLOADS;
        }
        File docsDir = Environment.getExternalStoragePublicDirectory(dirLocs);
        docsDir.mkdirs();

        String filename = "movementtracker" + ft.formatDateShort(System.currentTimeMillis()) + "." + format;

        try {
            int cnt = doExport(new File(docsDir, filename));
            res = "Exported " + cnt + " recordings to " + dirLocs + "/" + filename;
        } catch (IOException e) {
            Log.e("Exporter", "Export failed", e);
            res = "Export failed: " + e.getMessage();
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                alertDialog.dismiss();
                Toast.makeText(c, res, Toast.LENGTH_LONG).show();
            }
        });
    }

    public int doExport(File docsDir) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(docsDir), StandardCharsets.UTF_8));
        try {
            if ("tcx".equals(format)) {
                return doWriteTCX(App.get(c).database, w);
            } else {
                return doWriteGPX(App.get(c).database, w);
            }
        } finally {
            try {
                w.close();
            } catch (IOException e) {
                Log.e("Exporter", "Cannot close stream", e);
            }
        }
    }

    public int doWriteTCX(Database d, BufferedWriter w) throws IOException {
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        w.write("<TrainingCenterDatabase"
                + " xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"
                + " http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\">\n");
        w.write("<Activities>\n");

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
                w.write(ft.formatDateTimeUTC(loc.ts));
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

    public int doWriteGPX(Database d, BufferedWriter w) throws IOException {
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        w.write("<gpx version=\"1.1\""
                + " xmlns=\"http://www.topografix.com/GPX/1/1\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1"
                + " http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");

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
                w.write(ft.formatDateTimeUTC(loc.ts));
                w.write("</time>\n");
                w.write("</trkpt>\n");
            }

            w.write("</trkseg>\n");
            w.write("</trk>\n");
        }

        w.write("</gpx>\n");

        return rows.size;
    }
}
