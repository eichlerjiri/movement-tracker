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
import eichlerjiri.movementtracker.utils.FormatUtils;

public class Exporter {

    public static void exportGPX(final Context context, final long sinceTs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setMessage("Please wait")
                .setTitle("Exporting TCX");

        final AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false);
        alertDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    threaded(context, alertDialog, sinceTs);
                } catch (Failure ignored) {
                }
            }
        }).start();
    }

    private static void threaded(final Context context, final AlertDialog alertDialog, long sinceTs) throws Failure {
        String dirLocs;
        if (Build.VERSION.SDK_INT >= 19) {
            dirLocs = Environment.DIRECTORY_DOCUMENTS;
        } else {
            dirLocs = Environment.DIRECTORY_DOWNLOADS;
        }
        File docsDir = Environment.getExternalStoragePublicDirectory(dirLocs);
        docsDir.mkdirs();

        String filename = "MovementTracker " + FormatUtils.formatDateTimeISO(System.currentTimeMillis()) + ".tcx";

        String res;
        try {
            int cnt = doExport(new File(docsDir, filename), sinceTs);
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

    private static int doExport(File docsDir, long sinceTs) throws IOException, Failure {
        FileWriter fw = new FileWriter(docsDir);
        try {
            return doWriteGPX(Model.getInstance().getDatabase(), sinceTs, fw);
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                Log.e("Exporter", "Cannot close stream", e);
            }
        }
    }

    private static int doWriteGPX(Database d, long sinceTs, FileWriter fw) throws IOException, Failure {
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
                fw.write("<Time>" + FormatUtils.formatDateTimeTZ(loc.ts) + "</Time>\n");
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
}
