
package com.example.campusconnectmobile;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CameraFragment extends Fragment {

    private static final String ARG_EMAIL = "arg_email";

    private Spinner spCategory;
    private EditText etTitle;
    private EditText etPrice;
    private EditText etCaption;
    private TextView tvError;
    private Button btnShare;
    private Button btnSelectPhoto;
    private Button btnRemovePhoto;
    private ImageView ivSelectedPhotoPreview;

    private DatabaseHelper dbHelper;
    private String email;
    private Uri selectedImageUri;

    private ActivityResultLauncher<String> photoPickerLauncher;

    public static CameraFragment newInstance(String email) {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        ivSelectedPhotoPreview.setImageURI(uri);
                        ivSelectedPhotoPreview.setVisibility(View.VISIBLE);
                        btnRemovePhoto.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        email = getArguments() != null ? getArguments().getString(ARG_EMAIL) : null;
        dbHelper = new DatabaseHelper(requireContext());

        spCategory = view.findViewById(R.id.spPostCategory);
        etTitle = view.findViewById(R.id.etPostTitle);
        etPrice = view.findViewById(R.id.etPostPrice);
        etCaption = view.findViewById(R.id.etPostCaption);
        tvError = view.findViewById(R.id.tvPostError);
        btnShare = view.findViewById(R.id.btnSharePost);
        btnSelectPhoto = view.findViewById(R.id.btnSelectPhoto);
        btnRemovePhoto = view.findViewById(R.id.btnRemovePhoto);
        ivSelectedPhotoPreview = view.findViewById(R.id.ivSelectedPhotoPreview);

        btnSelectPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        btnRemovePhoto.setOnClickListener(v -> removeSelectedPhoto());
        btnShare.setOnClickListener(v -> attemptSharePost());

        return view;
    }

    private void removeSelectedPhoto() {
        selectedImageUri = null;
        ivSelectedPhotoPreview.setImageURI(null);
        ivSelectedPhotoPreview.setVisibility(View.GONE);
        btnRemovePhoto.setVisibility(View.GONE);
    }

    private void attemptSharePost() {
        String title = etTitle.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String description = etCaption.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();

        if (TextUtils.isEmpty(title)) {
            showError("Please add a listing title.");
            return;
        }
        if (TextUtils.isEmpty(price)) {
            showError("Please add a price or rate.");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            showError("Please add a short description.");
            return;
        }
        if (email == null) {
            showError("You must be logged in to post.");
            return;
        }

        DatabaseHelper.UserProfile profile = dbHelper.getProfile(email);
        if (profile == null || profile.username == null) {
            showError("Could not load your profile. Please try again.");
            return;
        }

        hideError();
        String caption = title + "\nPrice: " + price + "\n" + description;
        String imageUriString = selectedImageUri != null ? selectedImageUri.toString() : null;
        boolean success = dbHelper.insertPost(email, profile.username, profile.campusLocation, category, caption, imageUriString);
        if (success) {
            Toast.makeText(requireContext(), "Posted!", Toast.LENGTH_SHORT).show();
            etTitle.setText("");
            etPrice.setText("");
            etCaption.setText("");
            removeSelectedPhoto();
        } else {
            showError("Something went wrong. Please try again.");
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }
}