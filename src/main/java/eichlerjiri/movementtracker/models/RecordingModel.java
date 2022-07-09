package eichlerjiri.movementtracker.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import eichlerjiri.mapcomponent.utils.ObjectList;
import eichlerjiri.movementtracker.App;
import static eichlerjiri.movementtracker.utils.Common.*;

public class RecordingModel {

    public static long startRecording(App app, long timestamp, String movementType) {
        ContentValues values = new ContentValues();
        values.put("ts", Long.valueOf(timestamp));
        values.put("ts_end", Long.valueOf(0L));
        values.put("movement_type", movementType);
        values.put("distance", Double.valueOf(0.0));
        return app.sqlite().insert("recording", null, values);
    }

    public static void saveLocation(App app, long idRecording, long timestamp, double lat, double lon) {
        ContentValues values = new ContentValues();
        values.put("id_recording", Long.valueOf(idRecording));
        values.put("ts", Long.valueOf(timestamp));
        values.put("lat", Double.valueOf(lat));
        values.put("lon", Double.valueOf(lon));
        app.sqlite().insert("location", null, values);
    }

    public static void finishRecording(App app, long timestamp, long id, double distance) {
        ContentValues values = new ContentValues();
        values.put("ts_end", Long.valueOf(timestamp));
        values.put("distance", Double.valueOf(distance));
        app.sqlite().update("recording", values, "id=?", new String[]{String.valueOf(id)});
    }

    public static void deleteRecording(App app, long id) {
        app.sqlite().delete("location", "id_recording=?", new String[]{String.valueOf(id)});
        app.sqlite().delete("recording", "id=?", new String[]{String.valueOf(id)});
    }

    public static ObjectList<RecordingRow> getRecordings(App app) {
        return getRecordingsInternal(app, "ts_end<>0", null, "ts DESC,id");
    }

    public static RecordingRow getRecording(App app, long id) {
        ObjectList<RecordingRow> rows = getRecordingsInternal(app, "ts_end<>0 AND id=?", new String[]{String.valueOf(id)}, null);
        if (rows.size == 0) {
            return null;
        }
        return rows.data[0];
    }

    public static ObjectList<RecordingRow> getRecordingsInternal(App app, String selection, String[] selectionArgs, String orderBy) {
        ObjectList<RecordingRow> ret = new ObjectList<>(RecordingRow.class);
        try (Cursor c = app.sqlite().query("recording", new String[]{"id", "ts", "ts_end", "movement_type", "distance"}, selection, selectionArgs, null, null, orderBy, null)) {
            while (c.moveToNext()) {
                RecordingRow row = new RecordingRow();
                row.id = c.getLong(0);
                row.ts = c.getLong(1);
                row.tsEnd = c.getLong(2);
                row.movementType = c.getString(3);
                row.distance = c.getDouble(4);
                ret.add(row);
            }
        }
        return ret;
    }

    public static ObjectList<LocationRow> getLocations(App app, long idRecording) {
        ObjectList<LocationRow> ret = new ObjectList<>(LocationRow.class);
        try (Cursor c = app.sqlite().query("location", new String[]{"ts", "lat", "lon"}, "id_recording=?", new String[]{String.valueOf(idRecording)}, null, null, "ts,id", null)) {
            while (c.moveToNext()) {
                LocationRow row = new LocationRow();
                row.ts = c.getLong(0);
                row.lat = c.getDouble(1);
                row.lon = c.getDouble(2);
                ret.add(row);
            }
        }
        return ret;
    }

    public static void finishLastRecording(App app) {
        try (Cursor c = app.sqlite().query("recording", new String[]{"id", "ts_end"}, null, null, null, null, "id DESC", "1")) {
            if (c.moveToNext()) {
                long id = c.getLong(0);
                long tsEnd = c.getLong(1);
                if (tsEnd == 0) {
                    ObjectList<LocationRow> locs = getLocations(app, id);
                    if (locs.size < 2) {
                        Log.i("Database", "Deleting " + id);
                        deleteRecording(app, id);
                    } else {
                        long distance = 0;
                        for (int i = 1; i < locs.size; i++) {
                            LocationRow row1 = locs.data[i - 1];
                            LocationRow row2 = locs.data[i];
                            distance += distance(row1.lat, row1.lon, row2.lat, row2.lon);
                        }
                        Log.i("Database", "Finishing " + id);
                        finishRecording(app, locs.data[locs.size - 1].ts, id, distance);
                    }
                }
            }
        }
    }

    public static class RecordingRow {

        public long id;
        public long ts;
        public long tsEnd;
        public String movementType;
        public double distance;
    }

    public static class LocationRow {

        public long ts;
        public double lat;
        public double lon;
    }
}
