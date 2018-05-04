package eichlerjiri.movementtracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

import eichlerjiri.movementtracker.db.HistoryItem;
import eichlerjiri.movementtracker.db.LocationDb;
import eichlerjiri.movementtracker.utils.Failure;

public class Database {

    private final SQLiteDatabase sqlite;

    public Database() throws Failure {
        sqlite = new SQLiteOpenHelper(Model.getInstance().getAnyContext(), "movement-tracker", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                createDatabase(db);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                upgradeDatabase(db, oldVersion, newVersion);
            }
        }.getWritableDatabase();
    }

    private void createDatabase(SQLiteDatabase db) {
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

    private void upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("Database", "Unknown database upgrade. From: " + oldVersion + " to: " + newVersion);
    }

    public void saveLocation(long timestamp, double lat, double lon) throws Failure {
        ContentValues values = new ContentValues();
        values.put("ts", timestamp);
        values.put("lat", lat);
        values.put("lon", lon);
        insert("location", values);
    }

    public long startRecording(long timestamp, String movementType) throws Failure {
        ContentValues values = new ContentValues();
        values.put("ts_start", timestamp);
        values.put("ts_end", 0L);
        values.put("movement_type", movementType);
        return insert("recording", values);
    }

    public void stopRecording(long timestamp, long id) throws Failure {
        ContentValues values = new ContentValues();
        values.put("ts_end", timestamp);
        update("recording", values, "id=?", new String[]{String.valueOf(id)});
    }

    public ArrayList<HistoryItem> getHistory() throws Failure {
        ArrayList<HistoryItem> ret = new ArrayList<>();

        Cursor c = query("recording", new String[]{"id", "movement_type", "ts_start", "ts_end"},
                "ts_end<>0", null, null, null, "ts_start,id");
        while (c.moveToNext()) {
            ret.add(new HistoryItem(c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3)));
        }
        c.close();

        return ret;
    }

    public HistoryItem getHistoryItem(long id) throws Failure {
        HistoryItem ret = null;

        Cursor c = query("recording", new String[]{"id", "movement_type", "ts_start", "ts_end"},
                "ts_end<>0 AND id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (c.moveToNext()) {
            ret = new HistoryItem(c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3));
        }
        c.close();

        return ret;
    }

    public ArrayList<LocationDb> getLocations(long tsStart, long tsEnd) throws Failure {
        ArrayList<LocationDb> ret = new ArrayList<>();

        Cursor c = query("location", new String[]{"ts", "lat", "lon"}, "ts>=? AND ts<?",
                new String[]{String.valueOf(tsStart), String.valueOf(tsEnd)}, null, null, "ts,id");
        while (c.moveToNext()) {
            ret.add(new LocationDb(c.getLong(0), c.getDouble(1), c.getDouble(2)));
        }
        c.close();

        return ret;
    }

    private long insert(String table, ContentValues values) throws Failure {
        try {
            return sqlite.insertOrThrow(table, null, values);
        } catch (SQLException e) {
            throw new Failure(e);
        }
    }

    private int update(String table, ContentValues values, String where, String[] whereValues) throws Failure {
        try {
            return sqlite.update(table, values, where, whereValues);
        } catch (SQLException e) {
            throw new Failure(e);
        }
    }

    private Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
                         String groupBy, String having, String orderBy) throws Failure {
        try {
            return sqlite.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        } catch (SQLException e) {
            throw new Failure(e);
        }
    }
}
