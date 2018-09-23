package eichlerjiri.movementtracker.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import eichlerjiri.movementtracker.Database;
import eichlerjiri.movementtracker.Model;
import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.db.LocationRow;
import eichlerjiri.movementtracker.utils.Failure;

import static eichlerjiri.movementtracker.utils.Common.*;

public class Exporter {

    public static void exportTracks(final Context context, final long sinceTs, final String format) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setMessage("Please wait")
                .setTitle("Exporting " + format.toUpperCase());

        final AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false);
        alertDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    threaded(context, alertDialog, sinceTs, format);
                } catch (Failure ignored) {
                }
            }
        }).start();
    }

    private static void threaded(final Context context, final AlertDialog alertDialog, long sinceTs, String format)
            throws Failure {
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
            int cnt = doExport(new File(docsDir, filename), sinceTs, format);
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
                Toast.makeText(context, ress, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static int doExport(File docsDir, long sinceTs, String format) throws IOException, Failure {
        FileWriter fw = new FileWriter(docsDir);
        try {
            if (format.equals("tcx")) {
                return doWriteTCX(Model.getInstance().getDatabase(), sinceTs, fw);
            } else {
                return doWriteGPX(Model.getInstance().getDatabase(), sinceTs, fw);
            }
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                Log.e("Exporter", "Cannot close stream", e);
            }
        }
    }

    private static int doWriteTCX(Database d, long sinceTs, FileWriter fw) throws IOException, Failure {
        fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        fw.write("<TrainingCenterDatabase" +
                " xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2" +
                " http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\">\n");
        fw.write("<Activities>\n");

        int cnt = 0;
        for (HistoryRow row : d.getHistorySince(sinceTs)) {
            String type = "";
            if (row.movementType.equals("walk")) {
                type = "Walking";
            } else if (row.movementType.equals("bike")) {
                type = "Biking";
            } else if (row.movementType.equals("run")) {
                type = "Running";
            }

            fw.write("<Activity Sport=\"" + type + "\">\n");
            fw.write("<Lap>\n");
            fw.write("<Track>\n");

            for (LocationRow loc : d.getLocations(row.id)) {
                fw.write("<Trackpoint>\n");
                fw.write("<Time>" + formatDateTimeTZ(loc.ts) + "</Time>\n");
                fw.write("<Position>\n");
                fw.write("<LatitudeDegrees>" + loc.lat + "</LatitudeDegrees>\n");
                fw.write("<LongitudeDegrees>" + loc.lon + "</LongitudeDegrees>\n");
                fw.write("</Position>\n");
                fw.write("</Trackpoint>\n");
            }

            fw.write("</Track>\n");
            fw.write("</Lap>\n");
            fw.write("</Activity>\n");

            cnt++;
        }

        fw.write("</Activities>\n");
        fw.write("</TrainingCenterDatabase>\n");

        return cnt;
    }

    private static int doWriteGPX(Database d, long sinceTs, FileWriter fw) throws IOException, Failure {
        fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        fw.write("<gpx version=\"1.1\"" +
                " xmlns=\"http://www.topografix.com/GPX/1/1\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1" +
                " http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");

        int cnt = 0;
        for (HistoryRow row : d.getHistorySince(sinceTs)) {
            String type = "";
            if (row.movementType.equals("walk")) {
                type = "Walking";
            } else if (row.movementType.equals("bike")) {
                type = "Biking";
            } else if (row.movementType.equals("run")) {
                type = "Running";
            }

            fw.write("<trk>\n");
            fw.write("<type>" + type + "</type>\n");
            fw.write("<trkseg>\n");

            for (LocationRow loc : d.getLocations(row.id)) {
                fw.write("<trkpt lat=\"" + loc.lat + "\" lon=\"" + loc.lon + "\">\n");
                fw.write("<time>" + formatDateTimeTZ(loc.ts) + "</time>\n");
                fw.write("</trkpt>\n");
            }

            fw.write("</trkseg>\n");
            fw.write("</trk>\n");

            cnt++;
        }

        fw.write("</gpx>\n");

        return cnt;
    }
}
