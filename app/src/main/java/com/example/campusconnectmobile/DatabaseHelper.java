package com.example.campusconnectmobile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "campus_connect.db";
    private static final int DB_VERSION = 4; // bumped: added posts table

    // Users table
    public static final String TABLE_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";
    public static final String COL_USERNAME = "username";
    public static final String COL_CAMPUS = "campus_location";
    public static final String COL_PREFERENCES = "preferences"; // comma-separated

    // Posts table
    public static final String TABLE_POSTS = "posts";
    public static final String COL_POST_ID = "id";
    public static final String COL_POST_AUTHOR_EMAIL = "author_email";
    public static final String COL_POST_AUTHOR_USERNAME = "author_username";
    public static final String COL_POST_CAMPUS = "campus_location";
    public static final String COL_POST_CATEGORY = "category";
    public static final String COL_POST_CAPTION = "caption";
    public static final String COL_POST_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EMAIL + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT, " +
                COL_USERNAME + " TEXT, " +
                COL_CAMPUS + " TEXT, " +
                COL_PREFERENCES + " TEXT)";
        db.execSQL(createUsers);

        String createPosts = "CREATE TABLE " + TABLE_POSTS + " (" +
                COL_POST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_POST_AUTHOR_EMAIL + " TEXT, " +
                COL_POST_AUTHOR_USERNAME + " TEXT, " +
                COL_POST_CAMPUS + " TEXT, " +
                COL_POST_CATEGORY + " TEXT, " +
                COL_POST_CAPTION + " TEXT, " +
                COL_POST_CREATED_AT + " INTEGER)";
        db.execSQL(createPosts);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Prototype-stage upgrade: drop and recreate.
        // NOTE: wipes existing data on schema changes — fine for dev,
        // needs real ALTER TABLE migrations before any real users exist.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        onCreate(db);
    }

    // ---------------- Users ----------------

    public boolean registerUser(String email, String password, String username, String campusLocation) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        values.put(COL_USERNAME, username);
        values.put(COL_CAMPUS, campusLocation);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public String checkUserAndGetEmail(String identifier, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_EMAIL + " FROM " + TABLE_USERS +
                        " WHERE (" + COL_EMAIL + "=? OR " + COL_USERNAME + "=?) AND " + COL_PASSWORD + "=?",
                new String[]{identifier, identifier, password});

        String email = null;
        if (cursor.moveToFirst()) {
            email = cursor.getString(0);
        }
        cursor.close();
        return email;
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

    public boolean usernameExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=?",
                new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean updatePreferences(String email, String preferences) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PREFERENCES, preferences);
        int rows = db.update(TABLE_USERS, values, COL_EMAIL + "=?", new String[]{email});
        return rows > 0;
    }

    public static class UserProfile {
        public String username;
        public String campusLocation;
        public String preferences;
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

    // ---------------- Posts ----------------

    public boolean insertPost(String authorEmail, String authorUsername, String campus, String category, String caption) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_POST_AUTHOR_EMAIL, authorEmail);
        values.put(COL_POST_AUTHOR_USERNAME, authorUsername);
        values.put(COL_POST_CAMPUS, campus);
        values.put(COL_POST_CATEGORY, category);
        values.put(COL_POST_CAPTION, caption);
        values.put(COL_POST_CREATED_AT, System.currentTimeMillis());
        long result = db.insert(TABLE_POSTS, null, values);
        return result != -1;
    }

    public List<Post> getFeedForUser(String campusLocation, String preferencesCsv) {
        List<Post> posts = new ArrayList<>();
        if (preferencesCsv == null || preferencesCsv.isEmpty()) return posts;

        String[] categories = preferencesCsv.split(",");
        String placeholders = TextUtils.join(",", Collections.nCopies(categories.length, "?"));

        String[] args = new String[categories.length + 1];
        args[0] = campusLocation;
        System.arraycopy(categories, 0, args, 1, categories.length);

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_POSTS +
                        " WHERE " + COL_POST_CAMPUS + "=? AND " + COL_POST_CATEGORY + " IN (" + placeholders + ")" +
                        " ORDER BY " + COL_POST_CREATED_AT + " DESC",
                args);

        while (cursor.moveToNext()) {
            posts.add(mapCursorToPost(cursor));
        }
        cursor.close();
        return posts;
    }

    public List<Post> searchPosts(String query) {
        List<Post> posts = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String like = "%" + query + "%";

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_POSTS +
                        " WHERE " + COL_POST_CAPTION + " LIKE ? OR " +
                        COL_POST_CATEGORY + " LIKE ? OR " +
                        COL_POST_AUTHOR_USERNAME + " LIKE ?" +
                        " ORDER BY " + COL_POST_CREATED_AT + " DESC",
                new String[]{like, like, like});

        while (cursor.moveToNext()) {
            posts.add(mapCursorToPost(cursor));
        }
        cursor.close();
        return posts;
    }

    private Post mapCursorToPost(Cursor cursor) {
        Post post = new Post();
        post.username = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_AUTHOR_USERNAME));
        post.campus = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_CAMPUS));
        post.category = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_CATEGORY));
        post.caption = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_CAPTION));
        return post;
    }

    public static class Post {
        public String username;
        public String campus;
        public String category;
        public String caption;
    }
}