package eichlerjiri.movementtracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import eichlerjiri.mapcomponent.utils.ObjectList;
import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.db.LocationRow;
import static eichlerjiri.movementtracker.utils.Common.*;

public class Database {

    public final App app;
    public SQLiteDatabase sqlite;

    public Database(App app) {
        this.app = app;
    }

    public SQLiteDatabase sqlite() {
        if (sqlite == null) {
            sqlite = new SQLiteOpenHelper(app, "movement-tracker", null, 1) {
                @Override
                public void onCreate(SQLiteDatabase db) {
                    sqlite = db;
                    createDatabase();
                }

                @Override
                public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                    upgradeDatabase(oldVersion, newVersion);
                }
            }.getWritableDatabase();

            finishLastRecording();
        }
        return sqlite;
    }

    public void createDatabase() {
        execSQL("CREATE TABLE recording(id INTEGER PRIMARY KEY,"
                + "ts INTEGER,"
                + "ts_end INTEGER,"
                + "movement_type VARCHAR(20),"
                + "distance DOUBLE)"
        );

        execSQL("CREATE TABLE location(id INTEGER PRIMARY KEY,"
                + "id_recording INTEGER,"
                + "ts INTEGER,"
                + "lat DOUBLE,"
                + "lon DOUBLE)"
        );

        execSQL("CREATE INDEX recording_ts ON recording(ts)");
        execSQL("CREATE INDEX location_recording_ts ON location(id_recording,ts)");
    }

    public static void upgradeDatabase(int oldVersion, int newVersion) {
        Log.w("Database", "Unknown database upgrade. From: " + oldVersion + " to: " + newVersion);
    }

    public void finishLastRecording() {
        Cursor c = query("recording", new String[]{"id", "ts_end"}, null, null, null, null, "id DESC", "1");
        if (c.moveToNext()) {
            long id = c.getLong(0);
            long tsEnd = c.getLong(1);
            if (tsEnd == 0) {
                ObjectList<LocationRow> locs = getLocations(id);
                if (locs.size < 2) {
                    Log.i("Database", "Deleting " + id);
                    deleteRecording(id);
                } else {
                    long distance = 0;
                    for (int i = 1; i < locs.size; i++) {
                        LocationRow row1 = locs.data[i - 1];
                        LocationRow row2 = locs.data[i];
                        distance += distance(row1.lat, row1.lon, row2.lat, row2.lon);
                    }
                    Log.i("Database", "Finishing " + id);
                    finishRecording(locs.data[locs.size - 1].ts, id, distance);
                }
            }
        }
        c.close();
    }

    public void saveLocation(long idRecording, long timestamp, double lat, double lon) {
        ContentValues values = new ContentValues();
        values.put("id_recording", Long.valueOf(idRecording));
        values.put("ts", Long.valueOf(timestamp));
        values.put("lat", Double.valueOf(lat));
        values.put("lon", Double.valueOf(lon));
        insert("location", values);
    }

    public long startRecording(long timestamp, String movementType) {
        ContentValues values = new ContentValues();
        values.put("ts", Long.valueOf(timestamp));
        values.put("ts_end", Long.valueOf(0L));
        values.put("movement_type", movementType);
        values.put("distance", Double.valueOf(0.0));
        return insert("recording", values);
    }

    public void finishRecording(long timestamp, long id, double distance) {
        ContentValues values = new ContentValues();
        values.put("ts_end", Long.valueOf(timestamp));
        values.put("distance", Double.valueOf(distance));
        update("recording", values, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteRecording(long id) {
        delete("location", "id_recording=?", new String[]{String.valueOf(id)});
        delete("recording", "id=?", new String[]{String.valueOf(id)});
    }

    public ObjectList<HistoryRow> prepareHistory(String selection, String[] selectionArgs, String orderBy) {
        ObjectList<HistoryRow> ret = new ObjectList<>(HistoryRow.class);

        Cursor c = query("recording", new String[]{"id", "ts", "ts_end", "movement_type", "distance"}, selection, selectionArgs, null, null, orderBy, null);
        while (c.moveToNext()) {
            ret.add(new HistoryRow(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3), c.getDouble(4)));
        }
        c.close();

        return ret;
    }

    public ObjectList<HistoryRow> getHistory() {
        return prepareHistory("ts_end<>0", null, "ts DESC,id");
    }

    public ObjectList<HistoryRow> getHistorySince(long ts) {
        return prepareHistory("ts_end<>0 AND ts>=?", new String[]{String.valueOf(ts)}, "ts,id");
    }

    public HistoryRow getHistoryItem(long id) {
        ObjectList<HistoryRow> rows = prepareHistory("ts_end<>0 AND id=?", new String[]{String.valueOf(id)}, null);
        if (rows.size == 0) {
            return null;
        }
        return rows.data[0];
    }

    public ObjectList<LocationRow> getLocations(long idRecording) {
        ObjectList<LocationRow> ret = new ObjectList<>(LocationRow.class);

        Cursor c = query("location", new String[]{"ts", "lat", "lon"}, "id_recording=?", new String[]{String.valueOf(idRecording)}, null, null, "ts,id", null);
        while (c.moveToNext()) {
            ret.add(new LocationRow(c.getLong(0), c.getDouble(1), c.getDouble(2)));
        }
        c.close();

        return ret;
    }

    public void execSQL(String sql) {
        try {
            sqlite().execSQL(sql);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    public long insert(String table, ContentValues values) {
        try {
            return sqlite().insertOrThrow(table, null, values);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    public int update(String table, ContentValues values, String where, String[] whereValues) {
        try {
            return sqlite().update(table, values, where, whereValues);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    public int delete(String table, String where, String[] whereValues) {
        try {
            return sqlite().delete(table, where, whereValues);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    public Cursor query(String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        try {
            return sqlite().query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }
}
