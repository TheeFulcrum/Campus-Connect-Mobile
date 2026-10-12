package com.example.campusconnectmobile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A pure cache over the server API described in API.md. Every table here
 * stores a last-seen JSON snapshot keyed by the server's own identifiers so
 * screens can render instantly while a fresh network call is in flight, then
 * get overwritten once the server responds. This never competes with
 * DatabaseHelper's legacy local-only tables (users/posts/comments/likes/
 * follows/messages), which remain unchanged for the Home feed.
 */
public class ProfileCacheDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "campus_connect_cache.db";
    private static final int DB_VERSION = 1;

    public ProfileCacheDbHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE profile_cache (" +
                "username TEXT PRIMARY KEY, json TEXT NOT NULL, cached_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE listings_cache (" +
                "id INTEGER NOT NULL, owner_username TEXT NOT NULL, tab TEXT NOT NULL, " +
                "json TEXT NOT NULL, cached_at INTEGER NOT NULL, " +
                "PRIMARY KEY (id, owner_username, tab))");
        db.execSQL("CREATE TABLE collections_cache (" +
                "id INTEGER PRIMARY KEY, json TEXT NOT NULL, cached_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE conversations_cache (" +
                "id INTEGER PRIMARY KEY, json TEXT NOT NULL, cached_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE messages_cache (" +
                "id INTEGER PRIMARY KEY, conversation_id INTEGER NOT NULL, " +
                "json TEXT NOT NULL, cached_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE notifications_cache (" +
                "id INTEGER PRIMARY KEY, json TEXT NOT NULL, cached_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE recent_searches (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, query TEXT NOT NULL, searched_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No prior versions yet.
    }

    // ---------------------------------------------------------------- Profile

    public void putProfile(String username, JSONObject json) {
        ContentValues values = new ContentValues();
        values.put("username", username.toLowerCase());
        values.put("json", json.toString());
        values.put("cached_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("profile_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public JSONObject getProfile(String username) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT json FROM profile_cache WHERE username = ?", new String[]{username.toLowerCase()})) {
            if (cursor.moveToFirst()) {
                return new JSONObject(cursor.getString(0));
            }
        } catch (JSONException ignored) {
        }
        return null;
    }

    // --------------------------------------------------------------- Listings

    public void putListingsPage(String ownerUsername, String tab, List<JSONObject> listings) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (JSONObject listing : listings) {
                ContentValues values = new ContentValues();
                values.put("id", listing.optInt("id"));
                values.put("owner_username", ownerUsername.toLowerCase());
                values.put("tab", tab);
                values.put("json", listing.toString());
                values.put("cached_at", System.currentTimeMillis());
                db.insertWithOnConflict("listings_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<JSONObject> getListingsPage(String ownerUsername, String tab) {
        List<JSONObject> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT json FROM listings_cache WHERE owner_username = ? AND tab = ? ORDER BY cached_at DESC",
                new String[]{ownerUsername.toLowerCase(), tab})) {
            while (cursor.moveToNext()) {
                try {
                    results.add(new JSONObject(cursor.getString(0)));
                } catch (JSONException ignored) {
                }
            }
        }
        return results;
    }

    // ------------------------------------------------------------ Collections

    public void putCollections(List<JSONObject> collections) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("collections_cache", null, null);
            for (JSONObject collection : collections) {
                ContentValues values = new ContentValues();
                values.put("id", collection.optInt("id"));
                values.put("json", collection.toString());
                values.put("cached_at", System.currentTimeMillis());
                db.insertWithOnConflict("collections_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<JSONObject> getCollections() {
        List<JSONObject> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT json FROM collections_cache ORDER BY cached_at DESC", null)) {
            while (cursor.moveToNext()) {
                try {
                    results.add(new JSONObject(cursor.getString(0)));
                } catch (JSONException ignored) {
                }
            }
        }
        return results;
    }

    // ----------------------------------------------------------- Conversations

    public void putConversations(List<JSONObject> conversations) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (JSONObject conversation : conversations) {
                ContentValues values = new ContentValues();
                values.put("id", conversation.optInt("id"));
                values.put("json", conversation.toString());
                values.put("cached_at", System.currentTimeMillis());
                db.insertWithOnConflict("conversations_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<JSONObject> getConversations() {
        List<JSONObject> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT json FROM conversations_cache ORDER BY cached_at DESC", null)) {
            while (cursor.moveToNext()) {
                try {
                    results.add(new JSONObject(cursor.getString(0)));
                } catch (JSONException ignored) {
                }
            }
        }
        return results;
    }

    // -------------------------------------------------------------- Messages

    public void putMessages(int conversationId, List<JSONObject> messages) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (JSONObject message : messages) {
                ContentValues values = new ContentValues();
                values.put("id", message.optInt("id"));
                values.put("conversation_id", conversationId);
                values.put("json", message.toString());
                values.put("cached_at", System.currentTimeMillis());
                db.insertWithOnConflict("messages_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<JSONObject> getMessages(int conversationId) {
        List<JSONObject> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT json FROM messages_cache WHERE conversation_id = ? ORDER BY id ASC",
                new String[]{String.valueOf(conversationId)})) {
            while (cursor.moveToNext()) {
                try {
                    results.add(new JSONObject(cursor.getString(0)));
                } catch (JSONException ignored) {
                }
            }
        }
        return results;
    }

    // --------------------------------------------------------- Notifications

    public void putNotifications(List<JSONObject> notifications) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (JSONObject notification : notifications) {
                ContentValues values = new ContentValues();
                values.put("id", notification.optInt("id"));
                values.put("json", notification.toString());
                values.put("cached_at", System.currentTimeMillis());
                db.insertWithOnConflict("notifications_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<JSONObject> getNotifications() {
        List<JSONObject> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT json FROM notifications_cache ORDER BY cached_at DESC", null)) {
            while (cursor.moveToNext()) {
                try {
                    results.add(new JSONObject(cursor.getString(0)));
                } catch (JSONException ignored) {
                }
            }
        }
        return results;
    }

    // ------------------------------------------------------------- Searches

    public void addRecentSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        db.delete("recent_searches", "query = ?", new String[]{query});
        ContentValues values = new ContentValues();
        values.put("query", query);
        values.put("searched_at", System.currentTimeMillis());
        db.insert("recent_searches", null, values);
        db.execSQL("DELETE FROM recent_searches WHERE id NOT IN " +
                "(SELECT id FROM recent_searches ORDER BY searched_at DESC LIMIT 10)");
    }

    public List<String> getRecentSearches() {
        List<String> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT query FROM recent_searches ORDER BY searched_at DESC LIMIT 10", null)) {
            while (cursor.moveToNext()) {
                results.add(cursor.getString(0));
            }
        }
        return results;
    }

    public void clearRecentSearches() {
        getWritableDatabase().delete("recent_searches", null, null);
    }
}
