package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListingDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LISTING = "extra_listing";

    private Listing listing;
    private SessionManager sessionManager;
    private ViewPager2 imagePager;
    private TextView priceView;
    private TextView titleView;
    private TextView metaView;
    private TextView descriptionView;
    private TextView sellerView;
    private MaterialButton saveButton;
    private MaterialButton messageButton;
    private MaterialButton editButton;
    private MaterialButton soldButton;
    private MaterialButton archiveButton;
    private MaterialButton restoreButton;
    private MaterialButton reviewButton;
    private float swipeStartX;
    private float swipeStartY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listing = getListingExtra(getIntent());
        if (listing == null || listing.id <= 0) {
            finish();
            return;
        }
        sessionManager = new SessionManager(this);
        setContentView(R.layout.activity_listing_detail);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarListingDetail);
        toolbar.setNavigationContentDescription(R.string.cd_navigate_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        imagePager = findViewById(R.id.vpListingImages);
        priceView = findViewById(R.id.tvDetailPrice);
        titleView = findViewById(R.id.tvDetailTitle);
        metaView = findViewById(R.id.tvDetailMeta);
        descriptionView = findViewById(R.id.tvDetailDescription);
        sellerView = findViewById(R.id.tvDetailSeller);
        saveButton = findViewById(R.id.btnSaveListing);
        messageButton = findViewById(R.id.btnMessageSeller);
        editButton = findViewById(R.id.btnEditListing);
        soldButton = findViewById(R.id.btnMarkListingSold);
        archiveButton = findViewById(R.id.btnArchiveListing);
        restoreButton = findViewById(R.id.btnRestoreListing);
        reviewButton = findViewById(R.id.btnLeaveReview);

        bindListing();
        saveButton.setOnClickListener(v -> toggleSaved());
        messageButton.setOnClickListener(v -> showMessageComposer());
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateListingActivity.class);
            intent.putExtra(CreateListingActivity.EXTRA_EDIT_LISTING, listing);
            startActivity(intent);
        });
        soldButton.setOnClickListener(v -> confirmOwnerAction(
                R.string.listing_mark_sold_confirm, R.string.listing_action_confirm, this::markSold));
        archiveButton.setOnClickListener(v -> confirmOwnerAction(
                R.string.listing_archive_confirm, R.string.listing_action_confirm, this::archive));
        restoreButton.setOnClickListener(v -> restore());
        reviewButton.setOnClickListener(v -> showReviewDialog());
    }

    private static Listing getListingExtra(Intent intent) {
        Object value = intent.getSerializableExtra(EXTRA_LISTING);
        return value instanceof Listing ? (Listing) value : null;
    }

    private void bindListing() {
        String price = listing.price > 0
                ? String.format(Locale.getDefault(), "$%.2f", listing.price)
                : getString(R.string.listing_free);
        priceView.setText(price);
        titleView.setText(listing.title == null ? "" : listing.title);
        metaView.setText(joinMeta(listing.category, listing.campus, listing.isSold()
                ? getString(R.string.listing_status_sold) : null));
        descriptionView.setText(listing.description == null ? "" : listing.description);
        String seller = listing.sellerUsername == null || listing.sellerUsername.isEmpty()
                ? "" : "@" + listing.sellerUsername;
        sellerView.setText(seller.isEmpty() ? getString(R.string.listing_seller)
                : getString(R.string.listing_seller) + "  " + seller);

        ArrayList<String> images = new ArrayList<>();
        if (listing.coverImage != null && !listing.coverImage.isEmpty()) images.add(listing.coverImage);
        for (String url : listing.imageUrls) {
            if (url != null && !url.isEmpty() && !images.contains(url)) images.add(url);
        }
        imagePager.setAdapter(new ListingImageAdapter(images));

        saveButton.setText(listing.isSaved ? R.string.listing_unsave : R.string.listing_save);
        saveButton.setContentDescription(getString(listing.isSaved
                ? R.string.listing_unsave : R.string.listing_save));
        boolean isOwner = isCurrentUserListing();
        messageButton.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        editButton.setVisibility(isOwner && !listing.isSold() ? View.VISIBLE : View.GONE);
        soldButton.setVisibility(isOwner && !listing.isSold() ? View.VISIBLE : View.GONE);
        archiveButton.setVisibility(isOwner && !listing.isArchived() ? View.VISIBLE : View.GONE);
        restoreButton.setVisibility(isOwner && listing.isArchived() ? View.VISIBLE : View.GONE);
        reviewButton.setVisibility(!isOwner && listing.isSold() ? View.VISIBLE : View.GONE);
    }

    private String joinMeta(String category, String campus, String status) {
        ArrayList<String> parts = new ArrayList<>();
        if (category != null && !category.isEmpty()) parts.add(category);
        if (campus != null && !campus.isEmpty()) parts.add(campus);
        if (status != null && !status.isEmpty()) parts.add(status);
        return android.text.TextUtils.join(" · ", parts);
    }

    private boolean isCurrentUserListing() {
        String username = sessionManager.getLoggedInUsername();
        return username != null && !username.isEmpty() && username.equalsIgnoreCase(listing.sellerUsername);
    }

    private void toggleSaved() {
        boolean wasSaved = listing.isSaved;
        saveButton.setEnabled(false);
        CcwApiClient.Callback<JSONObject> callback = new CcwApiClient.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                listing.isSaved = !wasSaved;
                saveButton.setEnabled(true);
                bindListing();
                Toast.makeText(ListingDetailActivity.this,
                        listing.isSaved ? R.string.listing_saved : R.string.listing_unsaved,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                saveButton.setEnabled(true);
                showError(message);
            }
        };
        if (wasSaved) {
            CcwApiClient.unsaveListing(listing.id, sessionManager.getAuthToken(), callback);
        } else {
            CcwApiClient.saveListing(listing.id, sessionManager.getAuthToken(), callback);
        }
    }

    private void showMessageComposer() {
        EditText input = new EditText(this);
        input.setHint(R.string.listing_message_prompt);
        input.setMinLines(2);
        input.setMaxLines(5);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.listing_message_seller)
                .setView(input)
                .setNegativeButton(R.string.listing_action_cancel, null)
                .setPositiveButton(R.string.action_send, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String body = input.getText().toString().trim();
            if (body.isEmpty() || body.length() > 2000) {
                input.setError(getString(R.string.listing_message_prompt));
                return;
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            CcwApiClient.sendMessageAboutListing(listing.id, body, sessionManager.getAuthToken(),
                    new CcwApiClient.Callback<JSONObject>() {
                        @Override
                        public void onSuccess(JSONObject result) {
                            dialog.dismiss();
                            Toast.makeText(ListingDetailActivity.this, R.string.listing_message_sent,
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            showError(message);
                        }
                    });
        }));
        dialog.show();
    }

    private void confirmOwnerAction(int message, int positive, Runnable action) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setNegativeButton(R.string.listing_action_cancel, null)
                .setPositiveButton(positive, (dialog, which) -> action.run())
                .show();
    }

    private void markSold() {
        soldButton.setEnabled(false);
        CcwApiClient.markListingSold(listing.id, sessionManager.getAuthToken(),
                new CcwApiClient.Callback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        listing.status = "sold";
                        bindListing();
                        Toast.makeText(ListingDetailActivity.this, R.string.listing_sold_success,
                                Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                    }

                    @Override
                    public void onError(String message) {
                        soldButton.setEnabled(true);
                        showError(message);
                    }
                });
    }

    private void archive() {
        archiveButton.setEnabled(false);
        CcwApiClient.archiveListing(listing.id, sessionManager.getAuthToken(),
                new CcwApiClient.Callback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        listing.status = "archived";
                        setResult(RESULT_OK);
                        Toast.makeText(ListingDetailActivity.this, R.string.listing_archived_success,
                                Toast.LENGTH_SHORT).show();
                        archiveButton.setEnabled(true);
                        bindListing();
                    }

                    @Override
                    public void onError(String message) {
                        archiveButton.setEnabled(true);
                        showError(message);
                    }
                });
    }

    private void restore() {
        restoreButton.setEnabled(false);
        CcwApiClient.restoreListing(listing.id, sessionManager.getAuthToken(),
                new CcwApiClient.Callback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        listing.status = "active";
                        restoreButton.setEnabled(true);
                        bindListing();
                        Toast.makeText(ListingDetailActivity.this, R.string.listing_restored_success,
                                Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                    }

                    @Override
                    public void onError(String message) {
                        restoreButton.setEnabled(true);
                        showError(message);
                    }
                });
    }

    private void showReviewDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_review_create, null);
        android.widget.RatingBar ratingBar = content.findViewById(R.id.ratingReview);
        EditText comment = content.findViewById(R.id.etReviewComment);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.review_title)
                .setView(content)
                .setNegativeButton(R.string.listing_action_cancel, null)
                .setPositiveButton(R.string.review_submit, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int rating = Math.round(ratingBar.getRating());
            if (rating < 1 || rating > 5) {
                showError(getString(R.string.review_rating_required));
                return;
            }
            String body = comment.getText() == null ? "" : comment.getText().toString().trim();
            CcwApiClient.createReview(listing.id, rating, body, sessionManager.getAuthToken(),
                    new CcwApiClient.Callback<Review>() {
                        @Override
                        public void onSuccess(Review result) {
                            dialog.dismiss();
                            Toast.makeText(ListingDetailActivity.this, R.string.review_submitted,
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            showError(message);
                        }
                    });
        }));
        dialog.show();
    }

    private void showError(String message) {
        Toast.makeText(this, message == null || message.isEmpty()
                ? getString(R.string.generic_offline_message) : message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            swipeStartX = event.getX();
            swipeStartY = event.getY();
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float deltaY = event.getY() - swipeStartY;
            float deltaX = Math.abs(event.getX() - swipeStartX);
            int pagerBottom = imagePager == null ? 0 : imagePager.getBottom();
            if (swipeStartY < pagerBottom && deltaY > 150 && deltaY > deltaX * 1.25f) {
                finish();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private final class ListingImageAdapter extends RecyclerView.Adapter<ListingImageAdapter.ImageHolder> {
        private final List<String> images;

        ListingImageAdapter(List<String> images) {
            this.images = images;
        }

        @NonNull
        @Override
        public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView image = new ImageView(parent.getContext());
            image.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setContentDescription(getString(R.string.cd_listing_image));
            return new ImageHolder(image);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageHolder holder, int position) {
            if (images.isEmpty()) {
                Glide.with(holder.image).clear(holder.image);
                holder.image.setImageDrawable(null);
                holder.image.setBackgroundResource(R.drawable.bg_skeleton);
                return;
            }
            holder.image.setBackgroundResource(0);
            Glide.with(holder.image)
                    .load(CcwHttp.resolveUrl(images.get(position)))
                    .placeholder(R.drawable.bg_skeleton)
                    .error(R.drawable.bg_skeleton)
                    .centerCrop()
                    .into(holder.image);
        }

        @Override
        public int getItemCount() {
            return images.isEmpty() ? 1 : images.size();
        }

        final class ImageHolder extends RecyclerView.ViewHolder {
            final ImageView image;
            ImageHolder(@NonNull ImageView itemView) {
                super(itemView);
                image = itemView;
            }
        }
    }
}
