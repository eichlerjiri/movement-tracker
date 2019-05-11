package eichlerjiri.movementtracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.db.LocationRow;

public class Database {

    private final SQLiteDatabase d;

    public Database(Context c) {
        d = new SQLiteOpenHelper(c, "movement-tracker", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                createDatabase(db);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                upgradeDatabase(oldVersion, newVersion);
            }
        }.getWritableDatabase();
    }

    static void createDatabase(SQLiteDatabase sqlite) {
        execSQL(sqlite, "CREATE TABLE recording(id INTEGER PRIMARY KEY," +
                "ts INTEGER," +
                "ts_end INTEGER," +
                "movement_type VARCHAR(20)," +
                "distance DOUBLE)"
        );

        execSQL(sqlite, "CREATE TABLE location(id INTEGER PRIMARY KEY," +
                "id_recording INTEGER," +
                "ts INTEGER," +
                "lat DOUBLE," +
                "lon DOUBLE)"
        );

        execSQL(sqlite, "CREATE INDEX recording_ts ON recording(ts)");
        execSQL(sqlite, "CREATE INDEX location_recording_ts ON location(id_recording,ts)");
    }

    static void upgradeDatabase(int oldVersion, int newVersion) {
        Log.w("Database", "Unknown database upgrade. From: " + oldVersion + " to: " + newVersion);
    }

    public void saveLocation(long idRecording, long timestamp, double lat, double lon) {
        ContentValues values = new ContentValues();
        values.put("id_recording", Long.valueOf(idRecording));
        values.put("ts", Long.valueOf(timestamp));
        values.put("lat", Double.valueOf(lat));
        values.put("lon", Double.valueOf(lon));
        insert(d, "location", values);
    }

    public long startRecording(long timestamp, String movementType) {
        ContentValues values = new ContentValues();
        values.put("ts", Long.valueOf(timestamp));
        values.put("ts_end", Long.valueOf(0L));
        values.put("movement_type", movementType);
        values.put("distance", Double.valueOf(0.0));
        return insert(d, "recording", values);
    }

    public void finishRecording(long timestamp, long id, double distance) {
        ContentValues values = new ContentValues();
        values.put("ts_end", Long.valueOf(timestamp));
        values.put("distance", Double.valueOf(distance));
        update(d, "recording", values, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteRecording(long id) {
        delete(d, "location", "id_recording=?", new String[]{String.valueOf(id)});
        delete(d, "recording", "id=?", new String[]{String.valueOf(id)});
    }

    private ArrayList<HistoryRow> prepareHistory(String selection, String[] selectionArgs, String orderBy) {
        ArrayList<HistoryRow> ret = new ArrayList<>();

        Cursor c = query(d, "recording", new String[]{"id", "ts", "ts_end", "movement_type", "distance"},
                selection, selectionArgs, null, null, orderBy);
        while (c.moveToNext()) {
            ret.add(new HistoryRow(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3), c.getDouble(4)));
        }
        c.close();

        return ret;
    }

    public ArrayList<HistoryRow> getHistory() {
        return prepareHistory("ts_end<>0", null, "ts DESC,id");
    }

    public ArrayList<HistoryRow> getHistorySince(long ts) {
        return prepareHistory("ts_end<>0 AND ts>=?", new String[]{String.valueOf(ts)}, "ts,id");
    }

    public HistoryRow getHistoryItem(long id) {
        ArrayList<HistoryRow> rows = prepareHistory("ts_end<>0 AND id=?", new String[]{String.valueOf(id)}, null);
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    public ArrayList<LocationRow> getLocations(long idRecording) {
        ArrayList<LocationRow> ret = new ArrayList<>();

        Cursor c = query(d, "location", new String[]{"ts", "lat", "lon"}, "id_recording=?",
                new String[]{String.valueOf(idRecording)}, null, null, "ts,id");
        while (c.moveToNext()) {
            ret.add(new LocationRow(c.getLong(0), c.getDouble(1), c.getDouble(2)));
        }
        c.close();

        return ret;
    }

    private static void execSQL(SQLiteDatabase sqlite, String sql) {
        try {
            sqlite.execSQL(sql);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    private static long insert(SQLiteDatabase sqlite, String table, ContentValues values) {
        try {
            return sqlite.insertOrThrow(table, null, values);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    private static int update(SQLiteDatabase sqlite, String table, ContentValues values,
            String where, String[] whereValues) {
        try {
            return sqlite.update(table, values, where, whereValues);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    private static int delete(SQLiteDatabase sqlite, String table, String where, String[] whereValues) {
        try {
            return sqlite.delete(table, where, whereValues);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    private static Cursor query(SQLiteDatabase sqlite, String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having, String orderBy) {
        try {
            return sqlite.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }
}
