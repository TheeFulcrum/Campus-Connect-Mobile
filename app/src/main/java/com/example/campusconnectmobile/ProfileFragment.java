package com.example.campusconnectmobile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileFragment extends Fragment {

    private static final String ARG_EMAIL = "arg_email";
    private String email;
    private String avatarData = "";
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private TextView tvUsername, tvRealName, tvEmail, tvCampus, tvPreferences, tvBio;
    private ImageView ivAvatar;
    private ActivityResultLauncher<String> avatarPickerLauncher;

    public static ProfileFragment newInstance(String email) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        avatarPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) updateAvatar(uri);
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        email = getArguments() != null ? getArguments().getString(ARG_EMAIL) : null;
        dbHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());

        tvUsername = view.findViewById(R.id.tvProfileUsername);
        tvRealName = view.findViewById(R.id.tvProfileRealName);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvCampus = view.findViewById(R.id.tvProfileCampus);
        tvPreferences = view.findViewById(R.id.tvProfilePreferences);
        tvBio = view.findViewById(R.id.tvProfileBio);
        ivAvatar = view.findViewById(R.id.ivProfileAvatar);
        Button btnEditProfile = view.findViewById(R.id.btnEditProfile);
        Button btnEditPreferences = view.findViewById(R.id.btnEditPreferences);
        Button btnLogout = view.findViewById(R.id.btnLogout);
        Button btnLight = view.findViewById(R.id.btnLightTheme);
        Button btnDark = view.findViewById(R.id.btnDarkTheme);
        
        renderProfile();
        ivAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        btnLight.setOnClickListener(v -> {
            sessionManager.setTheme(AppCompatDelegate.MODE_NIGHT_NO);
            ThemeHelper.applyTheme(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(requireContext(), "Light theme applied", Toast.LENGTH_SHORT).show();
        });

        btnDark.setOnClickListener(v -> {
            sessionManager.setTheme(AppCompatDelegate.MODE_NIGHT_YES);
            ThemeHelper.applyTheme(AppCompatDelegate.MODE_NIGHT_YES);
            Toast.makeText(requireContext(), "Dark theme applied", Toast.LENGTH_SHORT).show();
        });

        btnEditPreferences.setOnClickListener(v -> {
            if (email == null) {
                Toast.makeText(requireContext(), "Your session has expired. Please log in again.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(requireContext(), ProfileSetupActivity.class);
            intent.putExtra(ProfileSetupActivity.EXTRA_EMAIL, email);
            intent.putExtra(ProfileSetupActivity.EXTRA_EDIT_MODE, true);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).onLoggedOut();
            }
        });

        return view;
    }

    private void renderProfile() {
        if (email == null || getContext() == null) return;
        DatabaseHelper.UserProfile profile = dbHelper.getProfile(email);
        if (profile == null) return;

        tvUsername.setText(profile.username == null ? "Student" : profile.username);
        tvRealName.setText(profile.realName == null || profile.realName.isEmpty() ? "" : profile.realName);
        tvEmail.setText(email);
        tvCampus.setText(profile.campusLocation == null ? "" : profile.campusLocation);
        tvPreferences.setText(profile.preferences == null ? "" : profile.preferences.replace(",", " · "));
        tvBio.setText(profile.bio == null || profile.bio.isEmpty()
                ? "Add a short bio to introduce yourself to campus."
                : profile.bio);
        avatarData = profile.profilePicture == null ? "" : profile.profilePicture;
        displayAvatar(avatarData);
    }

    private void displayAvatar(String value) {
        if (ivAvatar == null || value == null || value.isEmpty()) return;
        try {
            if (value.startsWith("data:image/")) {
                String encoded = value.substring(value.indexOf(',') + 1);
                byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
                ivAvatar.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            } else {
                ivAvatar.setImageURI(Uri.parse(value));
            }
        } catch (Exception exception) {
            ivAvatar.setImageDrawable(null);
        }
    }

    private void showEditProfileDialog() {
        if (email == null) {
            Toast.makeText(requireContext(), "Your session has expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseHelper.UserProfile profile = dbHelper.getProfile(email);
        if (profile == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_profile_edit, null);
        EditText username = dialogView.findViewById(R.id.etProfileUsername);
        EditText realName = dialogView.findViewById(R.id.etProfileRealName);
        Spinner campus = dialogView.findViewById(R.id.spProfileCampus);
        EditText bio = dialogView.findViewById(R.id.etProfileBio);
        username.setText(profile.username);
        realName.setText(profile.realName);
        bio.setText(profile.bio);
        String[] campuses = getResources().getStringArray(R.array.campus_locations);
        for (int i = 0; i < campuses.length; i++) {
            if (campuses[i].equals(profile.campusLocation)) campus.setSelection(i);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Edit profile")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            String nextUsername = username.getText().toString().trim();
            String nextRealName = realName.getText().toString().trim();
            String nextBio = bio.getText().toString().trim();
            if (nextUsername.length() < 3) {
                username.setError("Use at least 3 characters.");
                return;
            }
            saveProfile(nextUsername, nextRealName, campus.getSelectedItem().toString(), nextBio, dialog);
        }));
        dialog.show();
    }

    private void saveProfile(String username, String realName, String campus, String bio, AlertDialog dialog) {
        AuthApiClient.updateProfile(username, realName, campus, bio, avatarData,
                sessionManager.getAuthToken(), new AuthApiClient.Callback() {
            @Override
            public void onSuccess(AuthApiClient.AuthResult result) {
                dbHelper.cacheRemoteUser(result.email, result.username, result.campus,
                        result.realName, result.bio, result.avatar);
                avatarData = result.avatar == null ? "" : result.avatar;
                renderProfile();
                dialog.dismiss();
                Toast.makeText(requireContext(), "Profile updated.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateAvatar(Uri uri) {
        try {
            Bitmap bitmap = readScaledBitmap(uri);
            if (bitmap == null) throw new IllegalArgumentException("Unsupported image.");
            String nextAvatar = bitmapToDataUrl(bitmap);
            DatabaseHelper.UserProfile profile = dbHelper.getProfile(email);
            if (profile == null) return;
            AuthApiClient.updateProfile(profile.username, profile.realName, profile.campusLocation,
                    profile.bio, nextAvatar, sessionManager.getAuthToken(), new AuthApiClient.Callback() {
                @Override
                public void onSuccess(AuthApiClient.AuthResult result) {
                    dbHelper.cacheRemoteUser(result.email, result.username, result.campus,
                            result.realName, result.bio, result.avatar);
                    avatarData = result.avatar;
                    renderProfile();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception exception) {
            Toast.makeText(requireContext(), "Could not read that image. Choose another.", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap readScaledBitmap(Uri uri) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream stream = requireContext().getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(stream, null, options);
        }
        int sample = 1;
        while (options.outWidth / sample > 512 || options.outHeight / sample > 512) sample *= 2;
        options.inJustDecodeBounds = false;
        options.inSampleSize = sample;
        try (InputStream stream = requireContext().getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(stream, null, options);
        }
    }

    private String bitmapToDataUrl(Bitmap bitmap) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int quality = 82;
        do {
            output.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output);
            quality -= 8;
        } while (output.size() > 300000 && quality > 34);
        if (output.size() > 300000) throw new IllegalArgumentException("Image is too large.");
        return "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
    }
}