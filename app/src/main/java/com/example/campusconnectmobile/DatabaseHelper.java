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
    private static final int DB_VERSION = 6;

    // Users table
    public static final String TABLE_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_EMAIL = "email";
    // This existing column name is retained for prototype database compatibility.
    // Its value now contains a PBKDF2 password hash, never a plaintext password.
    public static final String COL_PASSWORD = "password";
    public static final String COL_USERNAME = "username";
    public static final String COL_CAMPUS = "campus_location";
    public static final String COL_PREFERENCES = "preferences"; // comma-separated
    public static final String COL_PROFILE_PICTURE = "profile_picture";
    public static final String COL_BIO = "bio";

    // Posts table
    public static final String TABLE_POSTS = "posts";
    public static final String COL_POST_ID = "id";
    public static final String COL_POST_AUTHOR_EMAIL = "author_email";
    public static final String COL_POST_AUTHOR_USERNAME = "author_username";
    public static final String COL_POST_CAMPUS = "campus_location";
    public static final String COL_POST_CATEGORY = "category";
    public static final String COL_POST_CAPTION = "caption";
    public static final String COL_POST_CREATED_AT = "created_at";

    // Social interaction tables
    public static final String TABLE_COMMENTS = "comments";
    public static final String COL_COMMENT_ID = "id";
    public static final String COL_COMMENT_POST_ID = "post_id";
    public static final String COL_COMMENT_USER_ID = "user_id";
    public static final String COL_COMMENT_CONTENT = "content";
    public static final String COL_COMMENT_CREATED_AT = "created_at";

    public static final String TABLE_LIKES = "likes";
    public static final String COL_LIKE_ID = "id";
    public static final String COL_LIKE_POST_ID = "post_id";
    public static final String COL_LIKE_USER_ID = "user_id";
    public static final String COL_LIKE_CREATED_AT = "created_at";

    public static final String TABLE_FOLLOWS = "follows";
    public static final String COL_FOLLOW_ID = "id";
    public static final String COL_FOLLOWER_ID = "follower_id";
    public static final String COL_FOLLOWEE_ID = "followee_id";
    public static final String COL_FOLLOW_CREATED_AT = "created_at";

    // Messages table
    public static final String TABLE_MESSAGES = "messages";
    public static final String COL_MSG_ID = "id";
    public static final String COL_MSG_SENDER = "sender_email";
    public static final String COL_MSG_RECEIVER = "receiver_email";
    public static final String COL_MSG_TEXT = "message_text";
    public static final String COL_MSG_TIMESTAMP = "timestamp";

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
                COL_PREFERENCES + " TEXT, " +
                COL_PROFILE_PICTURE + " TEXT, " +
                COL_BIO + " TEXT)";
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

            String createComments = "CREATE TABLE " + TABLE_COMMENTS + " (" +
                COL_COMMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_COMMENT_POST_ID + " INTEGER NOT NULL, " +
                COL_COMMENT_USER_ID + " INTEGER NOT NULL, " +
                COL_COMMENT_CONTENT + " TEXT NOT NULL, " +
                COL_COMMENT_CREATED_AT + " INTEGER, " +
                "FOREIGN KEY(" + COL_COMMENT_POST_ID + ") REFERENCES " + TABLE_POSTS + "(" + COL_POST_ID + "), " +
                "FOREIGN KEY(" + COL_COMMENT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_ID + "))";
            db.execSQL(createComments);

            String createLikes = "CREATE TABLE " + TABLE_LIKES + " (" +
                COL_LIKE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LIKE_POST_ID + " INTEGER NOT NULL, " +
                COL_LIKE_USER_ID + " INTEGER NOT NULL, " +
                COL_LIKE_CREATED_AT + " INTEGER, " +
                "UNIQUE(" + COL_LIKE_POST_ID + ", " + COL_LIKE_USER_ID + "), " +
                "FOREIGN KEY(" + COL_LIKE_POST_ID + ") REFERENCES " + TABLE_POSTS + "(" + COL_POST_ID + "), " +
                "FOREIGN KEY(" + COL_LIKE_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_ID + "))";
            db.execSQL(createLikes);

            String createFollows = "CREATE TABLE " + TABLE_FOLLOWS + " (" +
                COL_FOLLOW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FOLLOWER_ID + " INTEGER NOT NULL, " +
                COL_FOLLOWEE_ID + " INTEGER NOT NULL, " +
                COL_FOLLOW_CREATED_AT + " INTEGER, " +
                "UNIQUE(" + COL_FOLLOWER_ID + ", " + COL_FOLLOWEE_ID + "), " +
                "CHECK(" + COL_FOLLOWER_ID + " != " + COL_FOLLOWEE_ID + "), " +
                "FOREIGN KEY(" + COL_FOLLOWER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_ID + "), " +
                "FOREIGN KEY(" + COL_FOLLOWEE_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_ID + "))";
            db.execSQL(createFollows);

        String createMessages = "CREATE TABLE " + TABLE_MESSAGES + " (" +
                COL_MSG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MSG_SENDER + " TEXT, " +
                COL_MSG_RECEIVER + " TEXT, " +
                COL_MSG_TEXT + " TEXT, " +
                COL_MSG_TIMESTAMP + " INTEGER)";
        db.execSQL(createMessages);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Prototype-stage upgrade: drop and recreate.
        // NOTE: wipes existing data on schema changes — fine for dev,
        // needs real ALTER TABLE migrations before any real users exist.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LIKES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOLLOWS);
        onCreate(db);
    }

    // ---------------- Users ----------------

    public boolean registerUser(String email, String password, String username, String campusLocation) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, PasswordHasher.hash(password));
        values.put(COL_USERNAME, username);
        values.put(COL_CAMPUS, campusLocation);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean cacheRemoteUser(String email, String username, String campusLocation) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_CAMPUS, campusLocation);
        int rows = db.update(TABLE_USERS, values, COL_EMAIL + "=?", new String[]{email});
        if (rows > 0) return true;

        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, "");
        return db.insert(TABLE_USERS, null, values) != -1;
    }

    public String checkUserAndGetEmail(String identifier, String password) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_EMAIL + ", " + COL_PASSWORD + " FROM " + TABLE_USERS +
                        " WHERE " + COL_EMAIL + "=? OR " + COL_USERNAME + "=?",
                new String[]{identifier, identifier});

        String email = null;
        if (cursor.moveToFirst()) {
            String storedPassword = cursor.getString(1);
            boolean verified = PasswordHasher.verify(password, storedPassword);

            // Upgrade local prototype accounts after their next successful login.
            if (!verified && PasswordHasher.matchesLegacyPlaintext(password, storedPassword)) {
                ContentValues values = new ContentValues();
                values.put(COL_PASSWORD, PasswordHasher.hash(password));
                db.update(TABLE_USERS, values, COL_EMAIL + "=?", new String[]{cursor.getString(0)});
                verified = true;
            }

            if (verified) email = cursor.getString(0);
        }
        cursor.close();
        return email;
    }

    public boolean checkUser(String email, String password) {
        return checkUserAndGetEmail(email, password) != null;
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

    public boolean updateProfile(String email, String profilePicture, String bio) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PROFILE_PICTURE, profilePicture);
        values.put(COL_BIO, bio);
        return db.update(TABLE_USERS, values, COL_EMAIL + "=?", new String[]{email}) > 0;
    }

    public static class UserProfile {
        public String username;
        public String campusLocation;
        public String preferences;
        public String profilePicture;
        public String bio;
    }

    public UserProfile getProfile(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_USERNAME + ", " + COL_CAMPUS + ", " + COL_PREFERENCES +
                ", " + COL_PROFILE_PICTURE + ", " + COL_BIO +
                        " FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + "=?",
                new String[]{email});

        UserProfile profile = null;
        if (cursor.moveToFirst()) {
            profile = new UserProfile();
            profile.username = cursor.getString(0);
            profile.campusLocation = cursor.getString(1);
            profile.preferences = cursor.getString(2);
            profile.profilePicture = cursor.getString(3);
            profile.bio = cursor.getString(4);
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
        String[] categories = preferencesCsv == null || preferencesCsv.isEmpty()
            ? new String[]{"tutoring", "furniture", "creative", "electronics", "books", "repairs", "beauty", "campus-help"}
            : preferencesCsv.split(",");

        addDemoPosts(posts, campusLocation, categories);
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

    private void addDemoPosts(List<Post> posts, String campusLocation, String[] categories) {
        Post[] demoPosts = new Post[]{
                demoPost("Alex M.", "Main Campus", "tutoring", "Calc II tutoring\nK50/hr\nPatient, exam-focused help at the library. Tue and Thu evenings."),
                demoPost("Tendai N.", "Great East Campus", "furniture", "Mini fridge, barely used\nK450\nClean, quiet, and ready for pickup near Block C."),
                demoPost("Sarah K.", "Main Campus", "creative", "Logo design for student clubs\nFrom K120\nFresh, simple logos for clubs, societies, and events."),
                demoPost("Joseph M.", "Main Campus", "electronics", "Wireless keyboard and mouse set\nK850\nLightly used and perfect for a laptop study setup."),
                demoPost("Lydia N.", "Great East Campus", "books", "First-year study bundle\nK80\nNotes, past papers, and revision guides bundled together."),
                demoPost("Brian K.", "Main Campus", "repairs", "Laptop and phone repairs\nFrom K100\nBasic diagnostics, software setup, and common fixes."),
                demoPost("Prisca M.", "Great East Campus", "beauty", "Campus hair and beauty appointments\nFrom K70\nBraids, simple styling, and nails by appointment."),
                demoPost("David T.", "Main Campus", "campus-help", "Campus moving and delivery help\nFrom K60\nHelp moving boxes or collecting a marketplace purchase.")
        };

        for (Post demo : demoPosts) {
            if (!campusLocation.equalsIgnoreCase(demo.campus) && !campusLocation.isEmpty()) continue;
            for (String category : categories) {
                if (category.trim().equalsIgnoreCase(demo.category)) {
                    posts.add(demo);
                    break;
                }
            }
        }
    }

    private Post demoPost(String username, String campus, String category, String caption) {
        Post post = new Post();
        post.username = username;
        post.campus = campus;
        post.category = category;
        post.caption = caption;
        return post;
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

    public List<Post> getAllPosts() {
        List<Post> posts = new ArrayList<>();
        String[] categories = new String[]{"tutoring", "furniture", "creative", "electronics", "books", "repairs", "beauty", "campus-help"};
        addDemoPosts(posts, "", categories);

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_POSTS + " ORDER BY " + COL_POST_CREATED_AT + " DESC", null);
        while (cursor.moveToNext()) posts.add(mapCursorToPost(cursor));
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

    // ---------------- Social interactions ----------------

    private long getUserId(SQLiteDatabase db, String email) {
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_ID}, COL_EMAIL + "=?",
                new String[]{email}, null, null, null);
        long id = -1;
        if (cursor.moveToFirst()) id = cursor.getLong(0);
        cursor.close();
        return id;
    }

    public long addComment(long postId, String authorEmail, String content) {
        SQLiteDatabase db = getWritableDatabase();
        long userId = getUserId(db, authorEmail);
        if (userId < 0 || TextUtils.isEmpty(content)) return -1;

        ContentValues values = new ContentValues();
        values.put(COL_COMMENT_POST_ID, postId);
        values.put(COL_COMMENT_USER_ID, userId);
        values.put(COL_COMMENT_CONTENT, content.trim());
        values.put(COL_COMMENT_CREATED_AT, System.currentTimeMillis());
        return db.insert(TABLE_COMMENTS, null, values);
    }

    public boolean toggleLike(long postId, String userEmail) {
        SQLiteDatabase db = getWritableDatabase();
        long userId = getUserId(db, userEmail);
        if (userId < 0) return false;

        int deleted = db.delete(TABLE_LIKES,
                COL_LIKE_POST_ID + "=? AND " + COL_LIKE_USER_ID + "=?",
                new String[]{String.valueOf(postId), String.valueOf(userId)});
        if (deleted > 0) return false;

        ContentValues values = new ContentValues();
        values.put(COL_LIKE_POST_ID, postId);
        values.put(COL_LIKE_USER_ID, userId);
        values.put(COL_LIKE_CREATED_AT, System.currentTimeMillis());
        return db.insert(TABLE_LIKES, null, values) != -1;
    }

    public int getLikeCount(long postId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LIKES +
                " WHERE " + COL_LIKE_POST_ID + "=?", new String[]{String.valueOf(postId)});
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    public boolean toggleFollow(String followerEmail, String followeeEmail) {
        SQLiteDatabase db = getWritableDatabase();
        long followerId = getUserId(db, followerEmail);
        long followeeId = getUserId(db, followeeEmail);
        if (followerId < 0 || followeeId < 0 || followerId == followeeId) return false;

        int deleted = db.delete(TABLE_FOLLOWS,
                COL_FOLLOWER_ID + "=? AND " + COL_FOLLOWEE_ID + "=?",
                new String[]{String.valueOf(followerId), String.valueOf(followeeId)});
        if (deleted > 0) return false;

        ContentValues values = new ContentValues();
        values.put(COL_FOLLOWER_ID, followerId);
        values.put(COL_FOLLOWEE_ID, followeeId);
        values.put(COL_FOLLOW_CREATED_AT, System.currentTimeMillis());
        return db.insert(TABLE_FOLLOWS, null, values) != -1;
    }

    // ---------------- Messages ----------------

    public boolean insertMessage(String sender, String receiver, String text) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MSG_SENDER, sender);
        values.put(COL_MSG_RECEIVER, receiver);
        values.put(COL_MSG_TEXT, text);
        values.put(COL_MSG_TIMESTAMP, System.currentTimeMillis());
        long result = db.insert(TABLE_MESSAGES, null, values);
        return result != -1;
    }

    public List<ChatMessage> getMessagesBetween(String userA, String userB) {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_MESSAGES +
                        " WHERE (" + COL_MSG_SENDER + "=? AND " + COL_MSG_RECEIVER + "=?)" +
                        " OR (" + COL_MSG_SENDER + "=? AND " + COL_MSG_RECEIVER + "=?)" +
                        " ORDER BY " + COL_MSG_TIMESTAMP + " ASC",
                new String[]{userA, userB, userB, userA});

        while (cursor.moveToNext()) {
            messages.add(new ChatMessage(
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_MSG_SENDER)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_MSG_RECEIVER)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_MSG_TEXT)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_MSG_TIMESTAMP))
            ));
        }
        cursor.close();
        return messages;
    }

    public List<Conversation> getConversationsFor(String userEmail) {
        List<Conversation> conversations = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // This query finds unique participants the user has chatted with
        // and gets the last message from each.
        String query = "SELECT m1.*, u." + COL_USERNAME + " FROM " + TABLE_MESSAGES + " m1 " +
                "LEFT JOIN " + TABLE_USERS + " u ON (u." + COL_EMAIL + " = CASE WHEN m1." + COL_MSG_SENDER + " = ? THEN m1." + COL_MSG_RECEIVER + " ELSE m1." + COL_MSG_SENDER + " END) " +
                "WHERE m1." + COL_MSG_ID + " IN (" +
                "  SELECT MAX(" + COL_MSG_ID + ") FROM " + TABLE_MESSAGES +
                "  WHERE " + COL_MSG_SENDER + " = ? OR " + COL_MSG_RECEIVER + " = ? " +
                "  GROUP BY CASE WHEN " + COL_MSG_SENDER + " = ? THEN " + COL_MSG_RECEIVER + " ELSE " + COL_MSG_SENDER + " END" +
                ") ORDER BY m1." + COL_MSG_TIMESTAMP + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{userEmail, userEmail, userEmail, userEmail});

        while (cursor.moveToNext()) {
            String sender = cursor.getString(cursor.getColumnIndexOrThrow(COL_MSG_SENDER));
            String receiver = cursor.getString(cursor.getColumnIndexOrThrow(COL_MSG_RECEIVER));
            String participantEmail = sender.equals(userEmail) ? receiver : sender;
            String username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME));
            if (username == null) username = participantEmail;

            conversations.add(new Conversation(
                    participantEmail,
                    username,
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_MSG_TEXT)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_MSG_TIMESTAMP))
            ));
        }
        cursor.close();

        // Add demo conversations if empty
        if (conversations.isEmpty()) {
            conversations.add(new Conversation("alex@edenuniversity.education", "Alex M.", "Is Tuesday at 17:00 okay?", System.currentTimeMillis() - 3600000));
            conversations.add(new Conversation("tendai@edenuniversity.education", "Tendai N.", "The mini fridge is still available.", System.currentTimeMillis() - 86400000));
        }

        return conversations;
    }
}