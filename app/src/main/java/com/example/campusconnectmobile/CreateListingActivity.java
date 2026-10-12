package com.example.campusconnectmobile;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateListingActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_LISTING = "extra_edit_listing";
    private static final String STATE_IMAGE_URIS = "state_image_uris";
    private static final int MAX_IMAGES = 10;

    private final ArrayList<Uri> imageUris = new ArrayList<>();
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), this::onImagesPicked);

    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText priceInput;
    private Spinner categorySpinner;
    private Spinner campusSpinner;
    private MaterialButton postButton;
    private ProgressBar uploadProgress;
    private TextView uploadStatus;
    private TextView errorText;
    private ImagePickerAdapter imageAdapter;
    private SessionManager sessionManager;
    private Listing editListing;
    private boolean submitting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_listing);

        sessionManager = new SessionManager(this);
        String token = sessionManager.getAuthToken();
        if (TextUtils.isEmpty(token)) {
            startActivity(new android.content.Intent(this, MainActivity.class));
            finish();
            return;
        }

        editListing = getListingExtra(getIntent(), EXTRA_EDIT_LISTING);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarCreateListing);
        toolbar.setTitle(editListing == null ? R.string.create_listing_title : R.string.edit_listing_title);
        toolbar.setNavigationOnClickListener(v -> finish());

        titleInput = findViewById(R.id.etListingTitle);
        descriptionInput = findViewById(R.id.etListingDescription);
        priceInput = findViewById(R.id.etListingPrice);
        categorySpinner = findViewById(R.id.spListingCategory);
        campusSpinner = findViewById(R.id.spListingCampus);
        postButton = findViewById(R.id.btnPostListing);
        uploadProgress = findViewById(R.id.progressUpload);
        uploadStatus = findViewById(R.id.tvUploadStatus);
        errorText = findViewById(R.id.tvCreateListingError);

        RecyclerView imageRecycler = findViewById(R.id.rvListingImages);
        imageRecycler.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        imageAdapter = new ImagePickerAdapter();
        imageRecycler.setAdapter(imageAdapter);
        imageRecycler.setNestedScrollingEnabled(false);

        if (savedInstanceState != null) {
            ArrayList<String> savedUris = savedInstanceState.getStringArrayList(STATE_IMAGE_URIS);
            if (savedUris != null) {
                for (String uri : savedUris) imageUris.add(Uri.parse(uri));
            }
        }
        if (editListing != null) populateListing();
        imageAdapter.notifyDataSetChanged();
        postButton.setText(editListing == null ? R.string.btn_post_listing : R.string.btn_save_changes);
        postButton.setOnClickListener(v -> submitListing());
    }

    private static Listing getListingExtra(android.content.Intent intent, String key) {
        Object value = intent.getSerializableExtra(key);
        return value instanceof Listing ? (Listing) value : null;
    }

    private void populateListing() {
        titleInput.setText(editListing.title);
        descriptionInput.setText(editListing.description);
        priceInput.setText(String.format(Locale.US, "%.2f", editListing.price));
        setSpinnerSelection(categorySpinner, editListing.category);
        setSpinnerSelection(campusSpinner, editListing.campus);
        if (editListing.coverImage != null && !editListing.coverImage.isEmpty()) {
            uploadStatus.setText(getString(R.string.listing_images_hint));
            uploadStatus.setVisibility(View.VISIBLE);
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equalsIgnoreCase(String.valueOf(spinner.getItemAtPosition(i)))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void onImagesPicked(List<Uri> picked) {
        if (picked == null || picked.isEmpty()) return;
        if (imageUris.size() >= MAX_IMAGES) {
            Toast.makeText(this, getString(R.string.listing_images_hint), Toast.LENGTH_SHORT).show();
            return;
        }
        for (Uri uri : picked) {
            if (imageUris.size() >= MAX_IMAGES) break;
            if (!imageUris.contains(uri)) imageUris.add(uri);
        }
        imageAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        ArrayList<String> uriStrings = new ArrayList<>();
        for (Uri uri : imageUris) uriStrings.add(uri.toString());
        outState.putStringArrayList(STATE_IMAGE_URIS, uriStrings);
        super.onSaveInstanceState(outState);
    }

    private void submitListing() {
        if (submitting) return;
        String title = textOf(titleInput);
        String description = textOf(descriptionInput);
        String rawPrice = textOf(priceInput);
        if (title.isEmpty()) {
            titleInput.setError(getString(R.string.listing_title_required));
            titleInput.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            descriptionInput.setError(getString(R.string.listing_description_required));
            descriptionInput.requestFocus();
            return;
        }
        double price;
        try {
            price = Double.parseDouble(rawPrice);
            if (price < 0 || Double.isInfinite(price) || Double.isNaN(price)) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            priceInput.setError(getString(R.string.listing_price_invalid));
            priceInput.requestFocus();
            return;
        }
        if (categorySpinner.getSelectedItem() == null || campusSpinner.getSelectedItem() == null) {
            showError(getString(R.string.generic_offline_message));
            return;
        }

        submitting = true;
        postButton.setEnabled(false);
        postButton.setText(editListing == null ? R.string.listing_posting : R.string.listing_saving);
        errorText.setVisibility(View.GONE);
        if (imageUris.isEmpty()) {
            createOrUpdateListing(title, description, price, new ArrayList<>());
            return;
        }

        uploadProgress.setProgress(0);
        uploadProgress.setVisibility(View.VISIBLE);
        uploadStatus.setText(R.string.listing_uploading);
        uploadStatus.setVisibility(View.VISIBLE);
        imageExecutor.execute(() -> {
            ArrayList<CcwHttp.FilePart> files = new ArrayList<>();
            try {
                for (int i = 0; i < imageUris.size(); i++) {
                    byte[] bytes = ImageUtils.readAndCompress(this, imageUris.get(i), 1600, 86);
                    files.add(new CcwHttp.FilePart("images[]", "listing_" + i + ".jpg", bytes, "image/jpeg"));
                }
            } catch (IOException exception) {
                runOnUiThread(() -> failSubmission(getString(R.string.listing_image_read_error)));
                return;
            }
            CcwApiClient.uploadListingImages(null, files, sessionManager.getAuthToken(), percent -> {
                uploadProgress.setProgress(percent);
                uploadStatus.setText(getString(R.string.listing_upload_progress, percent));
            }, new CcwApiClient.Callback<List<CcwApiClient.UploadedImage>>() {
                @Override
                public void onSuccess(List<CcwApiClient.UploadedImage> uploaded) {
                    ArrayList<String> urls = new ArrayList<>();
                    for (CcwApiClient.UploadedImage image : uploaded) {
                        if (image.url != null && !image.url.isEmpty()) urls.add(image.url);
                    }
                    if (urls.size() != files.size()) {
                        failSubmission(getString(R.string.listing_image_read_error));
                        return;
                    }
                    createOrUpdateListing(title, description, price, urls);
                }

                @Override
                public void onError(String message) {
                    failSubmission(message);
                }
            });
        });
    }

    private void createOrUpdateListing(String title, String description, double price, List<String> imageUrls) {
        String category = String.valueOf(categorySpinner.getSelectedItem());
        String campus = String.valueOf(campusSpinner.getSelectedItem());
        CcwApiClient.Callback<Listing> callback = new CcwApiClient.Callback<Listing>() {
            @Override
            public void onSuccess(Listing result) {
                if (isFinishing()) return;
                submitting = false;
                Toast.makeText(CreateListingActivity.this,
                        editListing == null ? R.string.listing_created : R.string.listing_updated,
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String message) {
                failSubmission(message);
            }
        };
        String token = sessionManager.getAuthToken();
        if (editListing == null) {
            CcwApiClient.createListing(title, description, price, category, campus, imageUrls, token, callback);
        } else {
            CcwApiClient.updateListing(editListing.id, title, description, price, category, campus,
                    imageUrls, token, callback);
        }
    }

    private void failSubmission(String message) {
        submitting = false;
        postButton.setEnabled(true);
        postButton.setText(editListing == null ? R.string.btn_post_listing : R.string.btn_save_changes);
        uploadProgress.setVisibility(View.GONE);
        uploadStatus.setVisibility(View.GONE);
        showError(TextUtils.isEmpty(message) ? getString(R.string.generic_offline_message) : message);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Override
    protected void onDestroy() {
        imageExecutor.shutdown();
        super.onDestroy();
    }

    private final class ImagePickerAdapter extends RecyclerView.Adapter<ImagePickerAdapter.ImageHolder> {

        @NonNull
        @Override
        public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_listing_image_pick, parent, false);
            return new ImageHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageHolder holder, int position) {
            if (position == 0) {
                holder.image.setImageDrawable(null);
                holder.image.setBackgroundResource(R.color.colorBorder);
                holder.addIcon.setVisibility(View.VISIBLE);
                holder.removeButton.setVisibility(View.GONE);
                holder.itemView.setContentDescription(getString(R.string.cd_add_photo));
                holder.itemView.setOnClickListener(v -> imagePicker.launch("image/*"));
                return;
            }
            Uri uri = imageUris.get(position - 1);
            holder.image.setBackgroundResource(R.color.colorBorder);
            holder.addIcon.setVisibility(View.GONE);
            holder.removeButton.setVisibility(View.VISIBLE);
            holder.itemView.setContentDescription(getString(R.string.cd_listing_image));
            Glide.with(holder.image).load(uri).centerCrop().into(holder.image);
            holder.itemView.setOnClickListener(null);
            holder.removeButton.setOnClickListener(v -> {
                int index = holder.getBindingAdapterPosition();
                if (index > 0 && index <= imageUris.size()) {
                    imageUris.remove(index - 1);
                    notifyItemRemoved(index);
                    notifyItemRangeChanged(index, getItemCount() - index);
                }
            });
        }

        @Override
        public int getItemCount() {
            return imageUris.size() + 1;
        }

        final class ImageHolder extends RecyclerView.ViewHolder {
            final ImageView image;
            final ImageView addIcon;
            final ImageButton removeButton;

            ImageHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.ivImageThumb);
                addIcon = itemView.findViewById(R.id.ivAddPhotoIcon);
                removeButton = itemView.findViewById(R.id.btnRemoveImage);
            }
        }
    }
}
