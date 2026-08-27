package com.example.campusconnectmobile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "campus_connect.db";
    private static final int DB_VERSION = 2; // bumped for profile columns

    public static final String TABLE_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";
    public static final String COL_USERNAME = "username";
    public static final String COL_CAMPUS = "campus_location";
    public static final String COL_PREFERENCES = "preferences"; // comma-separated

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EMAIL + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT, " +
                COL_USERNAME + " TEXT, " +
                COL_CAMPUS + " TEXT, " +
                COL_PREFERENCES + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Prototype-stage upgrade: drop and recreate.
        // NOTE: this wipes existing accounts on schema changes — fine for dev,
        // not acceptable once real users exist (would need ALTER TABLE migrations).
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public boolean registerUser(String email, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + "=? AND " + COL_PASSWORD + "=?",
                new String[]{email, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean userExists(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + "=?",
                new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /** Saves profile details for an already-registered email. */
    public boolean saveProfile(String email, String username, String campusLocation, String preferences) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_CAMPUS, campusLocation);
        values.put(COL_PREFERENCES, preferences);
        int rows = db.update(TABLE_USERS, values, COL_EMAIL + "=?", new String[]{email});
        return rows > 0;
    }

    /** Simple data holder for profile display on Home. */
    public static class UserProfile {
        public String username;
        public String campusLocation;
        public String preferences; // comma-separated
    }

    public UserProfile getProfile(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_USERNAME + ", " + COL_CAMPUS + ", " + COL_PREFERENCES +
                        " FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + "=?",
                new String[]{email});

        UserProfile profile = null;
        if (cursor.moveToFirst()) {
            profile = new UserProfile();
            profile.username = cursor.getString(0);
            profile.campusLocation = cursor.getString(1);
            profile.preferences = cursor.getString(2);
        }
        cursor.close();
        return profile;
    }
}