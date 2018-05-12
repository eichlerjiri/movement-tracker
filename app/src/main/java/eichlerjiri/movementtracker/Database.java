package eichlerjiri.movementtracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

import eichlerjiri.movementtracker.db.HistoryRow;
import eichlerjiri.movementtracker.db.LocationRow;
import eichlerjiri.movementtracker.utils.Failure;

public class Database {

    private final SQLiteDatabase d;

    public Database() throws Failure {
        d = new SQLiteOpenHelper(Model.getInstance().getAnyContext(), "movement-tracker", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                try {
                    createDatabase(db);
                } catch (Failure ignored) {
                }
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                upgradeDatabase(oldVersion, newVersion);
            }
        }.getWritableDatabase();
    }

    private void createDatabase(SQLiteDatabase sqlite) throws Failure {
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

    private void upgradeDatabase(int oldVersion, int newVersion) {
        Log.w("Database", "Unknown database upgrade. From: " + oldVersion + " to: " + newVersion);
    }

    public void saveLocation(long idRecording, long timestamp, double lat, double lon) throws Failure {
        ContentValues values = new ContentValues();
        values.put("id_recording", idRecording);
        values.put("ts", timestamp);
        values.put("lat", lat);
        values.put("lon", lon);
        insert(d, "location", values);
    }

    public long startRecording(long timestamp, String movementType) throws Failure {
        ContentValues values = new ContentValues();
        values.put("ts", timestamp);
        values.put("ts_end", 0L);
        values.put("movement_type", movementType);
        values.put("distance", 0.0);
        return insert(d, "recording", values);
    }

    public void finishRecording(long timestamp, long id, double distance) throws Failure {
        ContentValues values = new ContentValues();
        values.put("ts_end", timestamp);
        values.put("distance", distance);
        update(d, "recording", values, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteRecording(long id) throws Failure {
        delete(d, "location", "id_recording=?", new String[]{String.valueOf(id)});
        delete(d, "recording", "id=?", new String[]{String.valueOf(id)});
    }

    public ArrayList<HistoryRow> getHistory() throws Failure {
        ArrayList<HistoryRow> ret = new ArrayList<>();

        Cursor c = query(d, "recording", new String[]{"id", "ts", "ts_end", "movement_type", "distance"},
                "ts_end<>0", null, null, null, "ts DESC,id");
        while (c.moveToNext()) {
            ret.add(new HistoryRow(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3), c.getDouble(4)));
        }
        c.close();

        return ret;
    }

    public HistoryRow getHistoryItem(long id) throws Failure {
        HistoryRow ret = null;

        Cursor c = query(d, "recording", new String[]{"id", "ts", "ts_end", "movement_type", "distance"},
                "ts_end<>0 AND id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (c.moveToNext()) {
            ret = new HistoryRow(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3), c.getDouble(4));
        }
        c.close();

        return ret;
    }

    public ArrayList<LocationRow> getLocations(long idRecording) throws Failure {
        ArrayList<LocationRow> ret = new ArrayList<>();

        Cursor c = query(d, "location", new String[]{"lat", "lon"}, "id_recording=?",
                new String[]{String.valueOf(idRecording)}, null, null, "ts,id");
        while (c.moveToNext()) {
            ret.add(new LocationRow(c.getDouble(0), c.getDouble(1)));
        }
        c.close();

        return ret;
    }

    private void execSQL(SQLiteDatabase sqlite, String sql) throws Failure {
        try {
            sqlite.execSQL(sql);
        } catch (SQLException e) {
            throw new Failure(e);
        }
    }

    private long insert(SQLiteDatabase sqlite, String table, ContentValues values) throws Failure {
        try {
            return sqlite.insertOrThrow(table, null, values);
        } catch (SQLException e) {
            throw new Failure(e);
        }
    }

    private int update(SQLiteDatabase sqlite, String table, ContentValues values, String where, String[] whereValues)
            throws Failure {
        try {
            return sqlite.update(table, values, where, whereValues);
        } catch (SQLException e) {
            throw new Failure(e);
        }
    }

    private int delete(SQLiteDatabase sqlite, String table, String where, String[] whereValues) throws Failure {
        try {
            return sqlite.delete(table, where, whereValues);
        } catch (SQLException e) {
            throw new Failure(e);
        }
    }

    private Cursor query(SQLiteDatabase sqlite, String table, String[] columns, String selection,
                         String[] selectionArgs, String groupBy, String having, String orderBy) throws Failure {
        try {
            return sqlite.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        } catch (SQLException e) {
            throw new Failure(e);
        }
    }
}
