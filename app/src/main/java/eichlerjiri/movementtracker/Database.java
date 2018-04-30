package eichlerjiri.movementtracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.Closeable;
import java.util.ArrayList;

import eichlerjiri.movementtracker.db.HistoryItem;
import eichlerjiri.movementtracker.db.LocationDb;

public class Database implements Closeable {

    private final Model m;
    private SQLiteDatabase sqlite;

    public Database() {
        m = Model.getInstance();
    }

    public void saveLocation(long timestamp, double lat, double lon) {
        ContentValues values = new ContentValues();
        values.put("ts", timestamp);
        values.put("lat", lat);
        values.put("lon", lon);
        tryInsert("location", values);
    }

    public long startRecording(long timestamp, String movementType) {
        ContentValues values = new ContentValues();
        values.put("ts_start", timestamp);
        values.put("ts_end", 0L);
        values.put("movement_type", movementType);
        return tryInsert("recording", values);
    }

    public void stopRecording(long timestamp, long id) {
        ContentValues values = new ContentValues();
        values.put("ts_end", timestamp);
        tryUpdate("recording", values, "id=?", new String[]{String.valueOf(id)});
    }

    public ArrayList<HistoryItem> getHistory() {
        ArrayList<HistoryItem> ret = new ArrayList<>();

        Cursor c = tryQuery("recording", new String[]{"id", "movement_type", "ts_start", "ts_end"},
                "ts_end<>0", null, null, null, "ts_start,id");
        if (c != null) {
            while (c.moveToNext()) {
                ret.add(new HistoryItem(c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3)));
            }
            c.close();
        }

        return ret;
    }

    public HistoryItem getHistoryItem(long id) {
        HistoryItem ret = null;

        Cursor c = tryQuery("recording", new String[]{"id", "movement_type", "ts_start", "ts_end"},
                "ts_end<>0 AND id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (c != null) {
            ret = new HistoryItem(c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3));
            c.close();
        }

        return ret;
    }

    public ArrayList<LocationDb> getLocations(long tsStart, long tsEnd) {
        ArrayList<LocationDb> ret = new ArrayList<>();

        Cursor c = tryQuery("location", new String[]{"ts", "lat", "lon"}, "ts>=? AND ts<?",
                new String[]{String.valueOf(tsStart), String.valueOf(tsEnd)}, null, null, "ts,id");
        if (c != null) {
            while (c.moveToNext()) {
                ret.add(new LocationDb(c.getLong(0), c.getDouble(1), c.getDouble(2)));
            }
            c.close();
        }

        return ret;
    }

    private long tryInsert(String table, ContentValues values) {
        try {
            long ret = prepareDb().insertOrThrow(table, null, values);
            m.setDatabaseError("");
            return ret;
        } catch (SQLException e) {
            Log.e("Database", "Insert error", e);
            m.setDatabaseError(e.getLocalizedMessage());
            return -1L;
        }
    }

    private int tryUpdate(String table, ContentValues values, String where, String[] whereValues) {
        try {
            int ret = prepareDb().update(table, values, where, whereValues);
            m.setDatabaseError("");
            return ret;
        } catch (SQLException e) {
            Log.e("Database", "Update error", e);
            m.setDatabaseError(e.getLocalizedMessage());
            return -1;
        }
    }

    private Cursor tryQuery(String table, String[] columns, String selection, String[] selectionArgs, String groupBy,
                            String having, String orderBy) {
        try {
            Cursor ret = prepareDb().query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
            m.setDatabaseError("");
            return ret;
        } catch (SQLException e) {
            Log.e("Database", "Select error", e);
            m.setDatabaseError(e.getLocalizedMessage());
            return null;
        }
    }

    private SQLiteDatabase prepareDb() {
        if (sqlite == null) {
            sqlite = doPrepareDb();
        }
        return sqlite;
    }

    private SQLiteDatabase doPrepareDb() {
        return new SQLiteOpenHelper(m.getContext(), "movement-tracker", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE location(id INTEGER PRIMARY KEY," +
                        "ts INTEGER," +
                        "lat DOUBLE," +
                        "lon DOUBLE)"
                );
                db.execSQL("CREATE TABLE recording(id INTEGER PRIMARY KEY," +
                        "ts_start INTEGER," +
                        "ts_end INTEGER," +
                        "movement_type VARCHAR(20))"
                );
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                Log.w("Database", "Unknown database upgrade. From: " + oldVersion + " to: " + newVersion);
            }
        }.getWritableDatabase();
    }

    @Override
    public void close() {
        if (sqlite != null) {
            sqlite.close();
            sqlite = null;
        }
    }
}
