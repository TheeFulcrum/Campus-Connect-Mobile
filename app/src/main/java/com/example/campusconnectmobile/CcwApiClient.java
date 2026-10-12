package com.example.campusconnectmobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Facade over every Campus Connect server endpoint documented in API.md
 * (profile, follow, listings extras, collections, notes, reviews,
 * notifications, search, messages). AuthApiClient keeps handling
 * login/register/session/logout against auth_api.php; this client handles
 * everything else, all against the same CCW_AUTH_API_BASE_URL.
 */
public final class CcwApiClient {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private CcwApiClient() {
    }

    // ---------------------------------------------------------------- Profile

    public static void getProfile(String username, String token, Callback<Profile> callback) {
        String url = CcwHttp.endpoint("/api/profile/get.php?username=" + CcwHttp.encode(username));
        CcwHttp.get(url, token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                JSONObject profileJson = json.optJSONObject("profile");
                if (profileJson == null) {
                    callback.onError("Profile not found.");
                } else {
                    callback.onSuccess(Profile.fromJson(profileJson));
                }
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    public static void updateProfile(String displayName, String username, String bio, String link,
                                     String campus, String token, Callback<Profile> callback) {
        JSONObject body = new JSONObject();
        try {
            if (displayName != null) body.put("display_name", displayName);
            if (username != null) body.put("username", username);
            if (bio != null) body.put("bio", bio);
            if (link != null) body.put("link", link);
            if (campus != null) body.put("campus", campus);
        } catch (Exception exception) {
            callback.onError("Unable to prepare profile update.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/profile/update.php"), body, token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                JSONObject profileJson = json.optJSONObject("profile");
                callback.onSuccess(profileJson == null ? new Profile() : Profile.fromJson(profileJson));
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    public static void uploadAvatar(byte[] jpegBytes, String token, CcwHttp.ProgressCallback progress,
                                    Callback<String> callback) {
        List<CcwHttp.FilePart> files = new ArrayList<>();
        files.add(new CcwHttp.FilePart("avatar", "avatar.jpg", jpegBytes, "image/jpeg"));
        CcwHttp.postMultipart(CcwHttp.endpoint("/api/profile/avatar.php"), null, files, token, progress,
                new CcwHttp.JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        callback.onSuccess(json.optString("avatar_url", ""));
                    }

                    @Override
                    public void onError(String message, int statusCode) {
                        callback.onError(message);
                    }
                });
    }

    public static void checkUsernameAvailable(String username, String token, Callback<Boolean> callback) {
        String url = CcwHttp.endpoint("/api/profile/username-check.php?username=" + CcwHttp.encode(username));
        CcwHttp.get(url, token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                callback.onSuccess(json.optBoolean("available", false));
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    public static void saveProfileNote(String body, String token, Callback<JSONObject> callback) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("action", "save");
            payload.put("body", body);
        } catch (Exception exception) {
            callback.onError("Unable to prepare note.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/profile/note.php"), payload, token, simpleJsonCallback(callback));
    }

    public static void deleteProfileNote(String token, Callback<JSONObject> callback) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("action", "delete");
        } catch (Exception ignored) {
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/profile/note.php"), payload, token, simpleJsonCallback(callback));
    }

    public static void getProfileListings(String username, int page, String token, Callback<ListPage<Listing>> callback) {
        String url = CcwHttp.endpoint("/api/profile/listings.php?username=" + CcwHttp.encode(username) + "&page=" + page);
        CcwHttp.get(url, token, listingsPageCallback("listings", callback));
    }

    public static void getProfileSaved(int page, String token, Callback<ListPage<Listing>> callback) {
        String url = CcwHttp.endpoint("/api/profile/saved.php?page=" + page);
        CcwHttp.get(url, token, listingsPageCallback("listings", callback));
    }

    public static void getProfileSold(String username, int page, String token, Callback<ListPage<Listing>> callback) {
        String url = CcwHttp.endpoint("/api/profile/sold.php?username=" + CcwHttp.encode(username) + "&page=" + page);
        CcwHttp.get(url, token, listingsPageCallback("listings", callback));
    }

    public static void getProfileReviews(String username, int page, String token, Callback<ReviewsPage> callback) {
        String url = CcwHttp.endpoint("/api/profile/reviews.php?username=" + CcwHttp.encode(username) + "&page=" + page);
        CcwHttp.get(url, token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                ReviewsPage page1 = new ReviewsPage();
                JSONArray array = CcwHttp.optArray(json, "reviews");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item != null) page1.items.add(Review.fromJson(item));
                }
                page1.pagination = Pagination.fromJson(json);
                JSONObject rating = json.optJSONObject("rating");
                if (rating != null) {
                    page1.ratingCount = rating.optInt("count", 0);
                    page1.ratingAverage = rating.optDouble("average", 0);
                }
                callback.onSuccess(page1);
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    // ----------------------------------------------------------------- Follow

    public static void follow(String username, String token, Callback<FollowResult> callback) {
        followRequest("/api/follow/follow.php", username, token, callback);
    }

    public static void unfollow(String username, String token, Callback<FollowResult> callback) {
        followRequest("/api/follow/unfollow.php", username, token, callback);
    }

    private static void followRequest(String path, String username, String token, Callback<FollowResult> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
        } catch (Exception exception) {
            callback.onError("Unable to prepare request.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint(path), body, token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                FollowResult result = new FollowResult();
                result.following = json.optBoolean("following", false);
                result.followers = json.optInt("followers", 0);
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    public static void getFollowers(String username, String query, int page, String token,
                                    Callback<ListPage<FollowUser>> callback) {
        followListRequest("/api/follow/followers.php", username, query, page, token, callback);
    }

    public static void getFollowing(String username, String query, int page, String token,
                                    Callback<ListPage<FollowUser>> callback) {
        followListRequest("/api/follow/following.php", username, query, page, token, callback);
    }

    private static void followListRequest(String path, String username, String query, int page, String token,
                                          Callback<ListPage<FollowUser>> callback) {
        StringBuilder url = new StringBuilder(CcwHttp.endpoint(path))
                .append("?username=").append(CcwHttp.encode(username))
                .append("&page=").append(page);
        if (query != null && !query.isEmpty()) {
            url.append("&q=").append(CcwHttp.encode(query));
        }
        CcwHttp.get(url.toString(), token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                ListPage<FollowUser> result = new ListPage<>();
                JSONArray array = CcwHttp.optArray(json, "users");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item != null) result.items.add(FollowUser.fromJson(item));
                }
                result.pagination = Pagination.fromJson(json);
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    // --------------------------------------------------------------- Listings

    public static void listListings(String campus, String category, int page, int limit, String token,
                                    Callback<ListPage<Listing>> callback) {
        StringBuilder url = new StringBuilder(CcwHttp.endpoint("/api/listings/list.php?status=active"))
                .append("&page=").append(page)
                .append("&limit=").append(limit);
        if (campus != null && !campus.isEmpty()) url.append("&campus=").append(CcwHttp.encode(campus));
        if (category != null && !category.isEmpty()) url.append("&category=").append(CcwHttp.encode(category));
        CcwHttp.get(url.toString(), token, listingsPageCallback("listings", callback));
    }

    public static void createListing(String title, String description, double price, String category,
                                     String campus, List<String> imageIds, String token, Callback<Listing> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("title", title);
            body.put("description", description);
            body.put("price", price);
            body.put("category", category);
            body.put("campus", campus);
            if (imageIds != null && !imageIds.isEmpty()) {
                body.put("image_ids", new JSONArray(imageIds));
            }
        } catch (Exception exception) {
            callback.onError("Unable to prepare listing.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/listings/create.php"), body, token, listingResultCallback(callback));
    }

    public static void updateListing(int listingId, String title, String description, double price, String category,
                                     String campus, List<String> imageIds, String token, Callback<Listing> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("listing_id", listingId);
            if (title != null) body.put("title", title);
            if (description != null) body.put("description", description);
            if (price >= 0) body.put("price", price);
            if (category != null) body.put("category", category);
            if (campus != null) body.put("campus", campus);
            if (imageIds != null && !imageIds.isEmpty()) {
                body.put("image_ids", new JSONArray(imageIds));
            }
        } catch (Exception exception) {
            callback.onError("Unable to prepare listing update.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/listings/update.php"), body, token, listingResultCallback(callback));
    }

    public static void saveListing(int listingId, String token, Callback<JSONObject> callback) {
        listingAction("/api/listings/save.php", listingId, token, callback);
    }

    public static void unsaveListing(int listingId, String token, Callback<JSONObject> callback) {
        listingAction("/api/listings/unsave.php", listingId, token, callback);
    }

    public static void markListingSold(int listingId, String token, Callback<JSONObject> callback) {
        listingAction("/api/listings/mark-sold.php", listingId, token, callback);
    }

    public static void archiveListing(int listingId, String token, Callback<JSONObject> callback) {
        listingAction("/api/listings/archive.php", listingId, token, callback);
    }

    public static void restoreListing(int listingId, String token, Callback<JSONObject> callback) {
        listingAction("/api/listings/restore.php", listingId, token, callback);
    }

    private static void listingAction(String path, int listingId, String token, Callback<JSONObject> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("listing_id", listingId);
        } catch (Exception exception) {
            callback.onError("Unable to prepare request.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint(path), body, token, simpleJsonCallback(callback));
    }

    public static final class UploadedImage {
        public int id;
        public String url;
    }

    public static void uploadListingImages(Integer listingId, List<CcwHttp.FilePart> images, String token,
                                           CcwHttp.ProgressCallback progress, Callback<List<UploadedImage>> callback) {
        Map<String, String> fields = new HashMap<>();
        if (listingId != null) fields.put("listing_id", String.valueOf(listingId));
        CcwHttp.postMultipart(CcwHttp.endpoint("/api/listings/upload-images.php"), fields, images, token, progress,
                new CcwHttp.JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        List<UploadedImage> uploaded = new ArrayList<>();
                        JSONArray array = CcwHttp.optArray(json, "images");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject item = array.optJSONObject(i);
                            if (item == null) continue;
                            UploadedImage image = new UploadedImage();
                            image.id = item.optInt("id");
                            image.url = item.optString("image_url", "");
                            uploaded.add(image);
                        }
                        callback.onSuccess(uploaded);
                    }

                    @Override
                    public void onError(String message, int statusCode) {
                        callback.onError(message);
                    }
                });
    }

    // ------------------------------------------------------------- Collections

    public static void createCollection(String name, String token, Callback<Collection> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
        } catch (Exception exception) {
            callback.onError("Unable to prepare collection.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/collections/create.php"), body, token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                JSONObject collectionJson = json.optJSONObject("collection");
                callback.onSuccess(collectionJson == null ? new Collection() : Collection.fromJson(collectionJson));
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    public static void listCollections(String token, Callback<List<Collection>> callback) {
        CcwHttp.get(CcwHttp.endpoint("/api/collections/list.php"), token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                callback.onSuccess(Collection.listFromJson(CcwHttp.optArray(json, "collections")));
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    public static void addCollectionItem(int collectionId, int listingId, String token, Callback<JSONObject> callback) {
        collectionItemAction("/api/collections/add-item.php", collectionId, listingId, token, callback);
    }

    public static void removeCollectionItem(int collectionId, int listingId, String token, Callback<JSONObject> callback) {
        collectionItemAction("/api/collections/remove-item.php", collectionId, listingId, token, callback);
    }

    private static void collectionItemAction(String path, int collectionId, int listingId, String token,
                                             Callback<JSONObject> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("collection_id", collectionId);
            body.put("listing_id", listingId);
        } catch (Exception exception) {
            callback.onError("Unable to prepare request.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint(path), body, token, simpleJsonCallback(callback));
    }

    public static void deleteCollection(int collectionId, String token, Callback<JSONObject> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("collection_id", collectionId);
        } catch (Exception exception) {
            callback.onError("Unable to prepare request.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/collections/delete.php"), body, token, simpleJsonCallback(callback));
    }

    // ------------------------------------------------------------------ Notes

    public static void createListingNote(int listingId, String body, String token, Callback<JSONObject> callback) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("listing_id", listingId);
            payload.put("body", body);
        } catch (Exception exception) {
            callback.onError("Unable to prepare note.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/notes/create.php"), payload, token, simpleJsonCallback(callback));
    }

    public static void listListingNotes(int page, String token, Callback<JSONArray> callback) {
        CcwHttp.get(CcwHttp.endpoint("/api/notes/list.php?page=" + page), token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                callback.onSuccess(CcwHttp.optArray(json, "notes"));
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    public static void deleteListingNote(int noteId, String token, Callback<JSONObject> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("id", noteId);
        } catch (Exception exception) {
            callback.onError("Unable to prepare request.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/notes/delete.php"), body, token, simpleJsonCallback(callback));
    }

    // --------------------------------------------------------------- Reviews

    public static void createReview(int listingId, int rating, String comment, String token, Callback<Review> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("listing_id", listingId);
            body.put("rating", rating);
            body.put("comment", comment);
        } catch (Exception exception) {
            callback.onError("Unable to prepare review.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/reviews/create.php"), body, token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                JSONObject reviewJson = json.optJSONObject("review");
                callback.onSuccess(reviewJson == null ? new Review() : Review.fromJson(reviewJson));
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    // ---------------------------------------------------------- Notifications

    public static void listNotifications(int page, String token, Callback<ListPage<AppNotification>> callback) {
        String url = CcwHttp.endpoint("/api/notifications/list.php?page=" + page);
        CcwHttp.get(url, token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                ListPage<AppNotification> result = new ListPage<>();
                JSONArray array = CcwHttp.optArray(json, "notifications");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item != null) result.items.add(AppNotification.fromJson(item));
                }
                result.pagination = Pagination.fromJson(json);
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    public static void markAllNotificationsRead(String token, Callback<JSONObject> callback) {
        CcwHttp.postJson(CcwHttp.endpoint("/api/notifications/read-all.php"), new JSONObject(), token,
                simpleJsonCallback(callback));
    }

    public static void getUnreadNotificationsCount(String token, Callback<Integer> callback) {
        CcwHttp.get(CcwHttp.endpoint("/api/notifications/unread-count.php"), token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                callback.onSuccess(json.optInt("unread", 0));
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    // --------------------------------------------------------------- Search

    public static void search(String query, int page, String token, Callback<SearchResults> callback) {
        String url = CcwHttp.endpoint("/api/search/index.php?q=" + CcwHttp.encode(query) + "&page=" + page);
        CcwHttp.get(url, token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                SearchResults results = new SearchResults();
                results.query = json.optString("query", query);
                JSONArray users = CcwHttp.optArray(json, "users");
                for (int i = 0; i < users.length(); i++) {
                    JSONObject item = users.optJSONObject(i);
                    if (item != null) results.users.add(SearchUserResult.fromJson(item));
                }
                JSONArray listings = CcwHttp.optArray(json, "listings");
                for (int i = 0; i < listings.length(); i++) {
                    JSONObject item = listings.optJSONObject(i);
                    if (item != null) results.listings.add(Listing.fromJson(item));
                }
                JSONObject pagination = json.optJSONObject("pagination");
                if (pagination != null) {
                    results.usersTotal = pagination.optInt("users_total", 0);
                    results.listingsTotal = pagination.optInt("listings_total", 0);
                }
                callback.onSuccess(results);
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    // -------------------------------------------------------------- Messages

    public static void listConversations(String token, Callback<List<ServerConversation>> callback) {
        CcwHttp.get(CcwHttp.endpoint("/api/messages/conversations.php"), token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                List<ServerConversation> conversations = new ArrayList<>();
                JSONArray array = CcwHttp.optArray(json, "conversations");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item != null) conversations.add(ServerConversation.fromJson(item));
                }
                callback.onSuccess(conversations);
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    public static void getThread(int conversationId, int afterId, String token, String myUsername,
                                 Callback<List<ServerMessage>> callback) {
        String url = CcwHttp.endpoint("/api/messages/thread.php?conversation_id=" + conversationId
                + (afterId > 0 ? "&after_id=" + afterId : ""));
        CcwHttp.get(url, token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                List<ServerMessage> messages = new ArrayList<>();
                JSONArray array = CcwHttp.optArray(json, "messages");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item != null) messages.add(ServerMessage.fromJson(item, myUsername));
                }
                callback.onSuccess(messages);
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    public static void sendMessageToConversation(int conversationId, String body, String token,
                                                 Callback<JSONObject> callback) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("conversation_id", conversationId);
            payload.put("body", body);
        } catch (Exception exception) {
            callback.onError("Unable to prepare message.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/messages/send.php"), payload, token, simpleJsonCallback(callback));
    }

    public static void sendMessageToUsername(String username, String body, String token, Callback<JSONObject> callback) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("recipient", username);
            payload.put("body", body);
        } catch (Exception exception) {
            callback.onError("Unable to prepare message.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/messages/send.php"), payload, token, simpleJsonCallback(callback));
    }

    public static void sendMessageAboutListing(int listingId, String body, String token, Callback<JSONObject> callback) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("listing_id", listingId);
            payload.put("body", body);
        } catch (Exception exception) {
            callback.onError("Unable to prepare message.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/messages/send.php"), payload, token, simpleJsonCallback(callback));
    }

    public static void markConversationRead(int conversationId, String token, Callback<JSONObject> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("conversation_id", conversationId);
        } catch (Exception exception) {
            callback.onError("Unable to prepare request.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/messages/mark-read.php"), body, token, simpleJsonCallback(callback));
    }

    public static void getUnreadMessagesCount(String token, Callback<Integer> callback) {
        CcwHttp.get(CcwHttp.endpoint("/api/messages/unread-count.php"), token, new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                callback.onSuccess(json.optInt("unread", 0));
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        });
    }

    // -------------------------------------------------------------- Helpers

    private static CcwHttp.JsonCallback listingsPageCallback(String key, Callback<ListPage<Listing>> callback) {
        return new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                ListPage<Listing> result = new ListPage<>();
                JSONArray array = CcwHttp.optArray(json, key);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item != null) result.items.add(Listing.fromJson(item));
                }
                result.pagination = Pagination.fromJson(json);
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        };
    }

    private static CcwHttp.JsonCallback listingResultCallback(Callback<Listing> callback) {
        return new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                JSONObject listingJson = json.optJSONObject("listing");
                callback.onSuccess(listingJson == null ? new Listing() : Listing.fromJson(listingJson));
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        };
    }

    private static CcwHttp.JsonCallback simpleJsonCallback(Callback<JSONObject> callback) {
        return new CcwHttp.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                callback.onSuccess(json);
            }

            @Override
            public void onError(String message, int statusCode) {
                callback.onError(message);
            }
        };
    }
}
