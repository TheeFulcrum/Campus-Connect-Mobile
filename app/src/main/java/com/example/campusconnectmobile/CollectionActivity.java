package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shows the listings saved in one collection and lets the owner add or remove them. */
public class CollectionActivity extends AppCompatActivity {

    public static final String EXTRA_COLLECTION_ID = "extra_collection_id";
    public static final String EXTRA_COLLECTION_NAME = "extra_collection_name";

    private static final int SPAN_COUNT = 3;

    private SessionManager sessionManager;
    private int collectionId;
    private ListingsAdapter adapter;
    private TextView emptyView;
    private View hintView;
    private final Set<Integer> memberIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        collectionId = getIntent().getIntExtra(EXTRA_COLLECTION_ID, 0);
        if (collectionId <= 0 || TextUtils.isEmpty(sessionManager.getAuthToken())) {
            finish();
            return;
        }
        setContentView(R.layout.activity_collection);

        Toolbar toolbar = findViewById(R.id.toolbarCollection);
        String name = getIntent().getStringExtra(EXTRA_COLLECTION_NAME);
        toolbar.setTitle(name == null || name.isEmpty() ? getString(R.string.profile_collections) : name);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_collection);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_collection) {
                confirmDelete();
                return true;
            }
            return false;
        });

        emptyView = findViewById(R.id.tvCollectionEmpty);
        hintView = findViewById(R.id.tvCollectionHint);
        adapter = new ListingsAdapter(listing -> {
            Intent intent = new Intent(this, ListingDetailActivity.class);
            intent.putExtra(ListingDetailActivity.EXTRA_LISTING, listing);
            startActivity(intent);
        });
        adapter.setLongClickListener(this::confirmRemove);
        RecyclerView recycler = findViewById(R.id.rvCollectionItems);
        recycler.setLayoutManager(new GridLayoutManager(this, SPAN_COUNT));
        recycler.setAdapter(adapter);
        findViewById(R.id.btnCollectionAdd).setOnClickListener(v -> showAddDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (collectionId > 0) loadItems();
    }

    private void loadItems() {
        CcwApiClient.listCollections(sessionManager.getAuthToken(), new CcwApiClient.Callback<List<Collection>>() {
            @Override
            public void onSuccess(List<Collection> result) {
                if (isFinishing() || isDestroyed()) return;
                for (Collection collection : result) {
                    if (collection.id != collectionId) continue;
                    adapter.submitList(collection.items);
                    memberIds.clear();
                    memberIds.addAll(collection.listingIds);
                    boolean empty = collection.items.isEmpty();
                    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                    hintView.setVisibility(empty ? View.GONE : View.VISIBLE);
                    return;
                }
                // The collection no longer exists
                finish();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(CollectionActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDialog() {
        CcwApiClient.listListings(null, null, 1, 50, sessionManager.getAuthToken(),
                new CcwApiClient.Callback<ListPage<Listing>>() {
                    @Override
                    public void onSuccess(ListPage<Listing> result) {
                        if (isFinishing() || isDestroyed()) return;
                        ArrayList<Listing> candidates = new ArrayList<>();
                        for (Listing listing : result.items) {
                            if (!memberIds.contains(listing.id)) candidates.add(listing);
                        }
                        if (candidates.isEmpty()) {
                            Toast.makeText(CollectionActivity.this, R.string.collection_add_none, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String[] titles = new String[candidates.size()];
                        for (int i = 0; i < titles.length; i++) {
                            titles[i] = candidates.get(i).title;
                        }
                        boolean[] checked = new boolean[titles.length];
                        new AlertDialog.Builder(CollectionActivity.this)
                                .setTitle(R.string.collection_add_title)
                                .setMultiChoiceItems(titles, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                                .setNegativeButton(R.string.listing_action_cancel, null)
                                .setPositiveButton(R.string.collection_add_listings, (dialog, which) -> {
                                    ArrayList<Integer> ids = new ArrayList<>();
                                    for (int i = 0; i < checked.length; i++) {
                                        if (checked[i]) ids.add(candidates.get(i).id);
                                    }
                                    addSequentially(ids, 0);
                                })
                                .show();
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(CollectionActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addSequentially(List<Integer> ids, int index) {
        if (index >= ids.size()) {
            if (!ids.isEmpty()) Toast.makeText(this, R.string.collection_added, Toast.LENGTH_SHORT).show();
            if (!isFinishing() && !isDestroyed()) loadItems();
            return;
        }
        CcwApiClient.addCollectionItem(collectionId, ids.get(index), sessionManager.getAuthToken(),
                new CcwApiClient.Callback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        addSequentially(ids, index + 1);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(CollectionActivity.this, message, Toast.LENGTH_SHORT).show();
                        if (!isFinishing() && !isDestroyed()) loadItems();
                    }
                });
    }

    private void confirmRemove(Listing listing) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.collection_remove_confirm)
                .setNegativeButton(R.string.listing_action_cancel, null)
                .setPositiveButton(R.string.profile_remove, (dialog, which) ->
                        CcwApiClient.removeCollectionItem(collectionId, listing.id, sessionManager.getAuthToken(),
                                new CcwApiClient.Callback<JSONObject>() {
                                    @Override
                                    public void onSuccess(JSONObject result) {
                                        if (isFinishing() || isDestroyed()) return;
                                        Toast.makeText(CollectionActivity.this, R.string.collection_removed, Toast.LENGTH_SHORT).show();
                                        loadItems();
                                    }

                                    @Override
                                    public void onError(String message) {
                                        Toast.makeText(CollectionActivity.this, message, Toast.LENGTH_SHORT).show();
                                    }
                                }))
                .show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.collection_delete_confirm)
                .setNegativeButton(R.string.listing_action_cancel, null)
                .setPositiveButton(R.string.collection_delete, (dialog, which) ->
                        CcwApiClient.deleteCollection(collectionId, sessionManager.getAuthToken(),
                                new CcwApiClient.Callback<JSONObject>() {
                                    @Override
                                    public void onSuccess(JSONObject result) {
                                        Toast.makeText(CollectionActivity.this, R.string.collection_deleted, Toast.LENGTH_SHORT).show();
                                        finish();
                                    }

                                    @Override
                                    public void onError(String message) {
                                        Toast.makeText(CollectionActivity.this, message, Toast.LENGTH_SHORT).show();
                                    }
                                }))
                .show();
    }
}
