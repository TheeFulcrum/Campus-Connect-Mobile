package com.example.campusconnectmobile;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProfileFragment extends Fragment implements FollowListBottomSheet.FollowListListener {

    private static final String ARG_EMAIL = "arg_email";
    private static final String ARG_USERNAME = "arg_username";

    private String email;
    private String targetUsername;
    private SessionManager sessionManager;
    private ProfileCacheDbHelper cacheDb;
    private Profile profile;

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeProfile;
    private TextView tvTopUsername;
    private TextView tvNote;
    private ImageView ivAvatar;
    private View ivAvatarEditBadge;
    private TextView tvCountListings;
    private TextView tvCountFollowers;
    private TextView tvCountFollowing;
    private TextView tvDisplayName;
    private TextView tvBio;
    private TextView tvLink;
    private TextView tvCampus;
    private View layoutOwnActions;
    private View layoutOtherActions;
    private MaterialButton btnEditProfile;
    private MaterialButton btnViewArchive;
    private MaterialButton btnProfileQr;
    private MaterialButton btnFollow;
    private MaterialButton btnMessage;
    private ImageButton btnCreate;
    private ImageButton btnMenu;
    private ImageButton btnOtherMenu;
    private RecyclerView rvCollections;
    private ViewPager2 profilePager;
    private CollectionsAdapter collectionsAdapter;
    private final List<Collection> collections = new ArrayList<>();

    private final ActivityResultLauncher<String> avatarPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadAvatar(uri);
            });

    public static ProfileFragment newInstance(String email) {
        return newInstance(email, null);
    }

    public static ProfileFragment newInstance(String email, String username) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        args.putString(ARG_USERNAME, username);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        email = getArguments() == null ? null : getArguments().getString(ARG_EMAIL);
        targetUsername = getArguments() == null ? null : getArguments().getString(ARG_USERNAME);
        sessionManager = new SessionManager(requireContext());
        cacheDb = new ProfileCacheDbHelper(requireContext());

        swipeProfile = view.findViewById(R.id.swipeProfile);
        tvTopUsername = view.findViewById(R.id.tvProfileTopUsername);
        tvNote = view.findViewById(R.id.tvProfileNote);
        ivAvatar = view.findViewById(R.id.ivProfileAvatar);
        ivAvatarEditBadge = view.findViewById(R.id.ivAvatarEditBadge);
        tvCountListings = view.findViewById(R.id.tvCountListings);
        tvCountFollowers = view.findViewById(R.id.tvCountFollowers);
        tvCountFollowing = view.findViewById(R.id.tvCountFollowing);
        tvDisplayName = view.findViewById(R.id.tvProfileDisplayName);
        tvBio = view.findViewById(R.id.tvProfileBio);
        tvLink = view.findViewById(R.id.tvProfileLink);
        tvCampus = view.findViewById(R.id.tvProfileCampus);
        layoutOwnActions = view.findViewById(R.id.layoutOwnActions);
        layoutOtherActions = view.findViewById(R.id.layoutOtherActions);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnViewArchive = view.findViewById(R.id.btnViewArchive);
        btnProfileQr = view.findViewById(R.id.btnProfileQr);
        btnFollow = view.findViewById(R.id.btnFollow);
        btnMessage = view.findViewById(R.id.btnMessage);
        btnCreate = view.findViewById(R.id.btnProfileCreate);
        btnMenu = view.findViewById(R.id.btnProfileMenu);
        btnOtherMenu = view.findViewById(R.id.btnOtherMenu);
        rvCollections = view.findViewById(R.id.rvCollections);
        rvCollections.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        collectionsAdapter = new CollectionsAdapter();
        rvCollections.setAdapter(collectionsAdapter);

        boolean ownProfile = targetUsername == null || targetUsername.isEmpty();
        btnMenu.setVisibility(ownProfile ? View.VISIBLE : View.GONE);
        btnCreate.setVisibility(ownProfile ? View.VISIBLE : View.GONE);

        setupTabs(view);
        wireActions();
        swipeProfile.setOnRefreshListener(this::reloadFromServer);
        // Pull-to-refresh only while the header is fully expanded, otherwise it fights the tab lists
        AppBarLayout appBar = view.findViewById(R.id.appBarProfile);
        appBar.addOnOffsetChangedListener((layout, offset) -> swipeProfile.setEnabled(offset == 0));
        loadFromCacheThenRefresh();
        return view;
    }

    private void setupTabs(View root) {
        TabLayout tabs = root.findViewById(R.id.profileTabs);
        profilePager = root.findViewById(R.id.profilePager);
        String username = getEffectiveUsername();
        boolean isMe = targetUsername == null || targetUsername.isEmpty();
        profilePager.setAdapter(new ProfileTabsPagerAdapter(requireActivity(), username, isMe));
        new TabLayoutMediator(tabs, profilePager, (tab, position) -> {
            tab.setText(null);
            int iconRes;
            if (position == 0) iconRes = R.drawable.ic_tab_listings;
            else if (position == 1) iconRes = R.drawable.ic_heart_filled;
            else if (position == 2) iconRes = R.drawable.ic_check;
            else iconRes = R.drawable.ic_message;
            tab.setIcon(iconRes);
        }).attach();
    }

    private void wireActions() {
        View.OnClickListener changePhoto = v -> {
            if (profile != null && profile.isMe) avatarPickerLauncher.launch("image/*");
        };
        ivAvatar.setOnClickListener(changePhoto);
        ivAvatarEditBadge.setOnClickListener(changePhoto);
        btnCreate.setOnClickListener(v -> startActivity(new Intent(requireContext(), CreateListingActivity.class)));
        btnMenu.setOnClickListener(v -> showOwnMenu());
        btnOtherMenu.setOnClickListener(v -> showOtherMenu());
        btnEditProfile.setOnClickListener(v -> showEditDialog());
        btnViewArchive.setOnClickListener(v -> openArchiveTab());
        btnProfileQr.setOnClickListener(v -> showQrCode());
        btnFollow.setOnClickListener(v -> toggleFollow());
        btnMessage.setOnClickListener(v -> showMessageDialog());
        tvCountFollowers.setOnClickListener(v -> openFollowSheet("followers"));
        tvCountFollowing.setOnClickListener(v -> openFollowSheet("following"));
        tvLink.setOnClickListener(v -> {
            String link = tvLink.getText() == null ? "" : tvLink.getText().toString();
            if (!link.startsWith("http")) return;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
        });
    }

    private void openArchiveTab() {
        if (profile == null || !profile.isMe) return;
        if (profilePager != null) profilePager.setCurrentItem(2, true);
    }

    private void showOwnMenu() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View menu = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_profile_menu, null);
        menu.findViewById(R.id.menuEditProfile).setOnClickListener(v -> {
            sheet.dismiss();
            showEditDialog();
        });
        menu.findViewById(R.id.menuChangePhoto).setOnClickListener(v -> {
            sheet.dismiss();
            if (profile != null && profile.isMe) avatarPickerLauncher.launch("image/*");
        });
        menu.findViewById(R.id.menuQrCode).setOnClickListener(v -> {
            sheet.dismiss();
            showQrCode();
        });
        menu.findViewById(R.id.menuArchive).setOnClickListener(v -> {
            sheet.dismiss();
            openArchiveTab();
        });
        menu.findViewById(R.id.menuAppearance).setOnClickListener(v -> {
            sheet.dismiss();
            showAppearanceDialog();
        });
        menu.findViewById(R.id.menuLogout).setOnClickListener(v -> {
            sheet.dismiss();
            confirmLogout();
        });
        sheet.setContentView(menu);
        sheet.show();
    }

    private void showAppearanceDialog() {
        int[] modes = {AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_NO,
                AppCompatDelegate.MODE_NIGHT_YES};
        String[] labels = {getString(R.string.profile_theme_system), getString(R.string.profile_theme_light),
                getString(R.string.profile_theme_dark)};
        int current = 0;
        int saved = sessionManager.getThemeMode();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == saved) current = i;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_menu_appearance)
                .setSingleChoiceItems(labels, current, (dialog, which) -> {
                    dialog.dismiss();
                    if (modes[which] == saved) return;
                    sessionManager.setTheme(modes[which]);
                    ThemeHelper.applyTheme(modes[which]);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_logout_confirm_title)
                .setMessage(R.string.profile_logout_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.profile_logout, (dialog, which) -> {
                    if (getActivity() instanceof HomeActivity) {
                        ((HomeActivity) getActivity()).onLoggedOut();
                    }
                })
                .show();
    }

    private void showOtherMenu() {
        String[] options = {getString(R.string.profile_report)};
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) ->
                        Toast.makeText(requireContext(), R.string.profile_report_ack, Toast.LENGTH_SHORT).show())
                .show();
    }

    private void showMessageDialog() {
        if (profile == null || TextUtils.isEmpty(profile.username)) return;
        EditText input = new EditText(requireContext());
        input.setHint(R.string.listing_message_prompt);
        input.setMinLines(2);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_message)
                .setView(input)
                .setNegativeButton(R.string.listing_action_cancel, null)
                .setPositiveButton(R.string.action_send, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String body = input.getText() == null ? "" : input.getText().toString().trim();
            if (body.isEmpty() || body.length() > 2000) {
                input.setError(getString(R.string.messages_body_too_long));
                return;
            }
            CcwApiClient.sendMessageToUsername(profile.username, body, sessionManager.getAuthToken(),
                    new CcwApiClient.Callback<JSONObject>() {
                        @Override
                        public void onSuccess(JSONObject result) {
                            dialog.dismiss();
                            Toast.makeText(requireContext(), R.string.listing_message_sent, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
        }));
        dialog.show();
    }

    private void toggleFollow() {
        if (profile == null || profile.isMe || profile.username == null || profile.username.isEmpty()) return;
        btnFollow.setEnabled(false);
        CcwApiClient.Callback<FollowResult> callback = new CcwApiClient.Callback<FollowResult>() {
            @Override
            public void onSuccess(FollowResult result) {
                btnFollow.setEnabled(true);
                profile.isFollowing = result.following;
                profile.followersCount = result.followers;
                renderProfile();
            }

            @Override
            public void onError(String message) {
                btnFollow.setEnabled(true);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        };
        if (profile.isFollowing) {
            CcwApiClient.unfollow(profile.username, sessionManager.getAuthToken(), callback);
        } else {
            CcwApiClient.follow(profile.username, sessionManager.getAuthToken(), callback);
        }
    }

    private void openFollowSheet(String mode) {
        if (profile == null || profile.username == null || profile.username.isEmpty()) return;
        FollowListBottomSheet sheet = FollowListBottomSheet.newInstance(profile.username, mode);
        sheet.setFollowListListener(this);
        sheet.show(getChildFragmentManager(), "follow-list-" + mode);
    }

    private void showEditDialog() {
        if (profile == null || !profile.isMe) return;
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_profile_edit, null);
        EditText etUsername = view.findViewById(R.id.etProfileUsername);
        EditText etRealName = view.findViewById(R.id.etProfileRealName);
        android.widget.Spinner spCampus = view.findViewById(R.id.spProfileCampus);
        EditText etBio = view.findViewById(R.id.etProfileBio);
        etUsername.setText(profile.username);
        etRealName.setText(profile.displayName);
        etBio.setText(profile.bio);
        setSpinnerSelection(spCampus, profile.campus);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_edit)
                .setView(view)
                .setNegativeButton(R.string.listing_action_cancel, null)
                .setPositiveButton(R.string.btn_save_changes, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nextUsername = textOf(etUsername);
            String nextName = textOf(etRealName);
            String nextBio = textOf(etBio);
            String nextCampus = String.valueOf(spCampus.getSelectedItem());
            if (nextUsername.length() < 3) {
                etUsername.setError(getString(R.string.profile_username_invalid));
                return;
            }
            CcwApiClient.updateProfile(nextName, nextUsername, nextBio, profile.link, nextCampus,
                    sessionManager.getAuthToken(), new CcwApiClient.Callback<Profile>() {
                        @Override
                        public void onSuccess(Profile result) {
                            sessionManager.setLoggedInUsername(result.username);
                            profile = result;
                            renderProfile();
                            dialog.dismiss();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
        }));
        dialog.show();
    }

    private void setSpinnerSelection(android.widget.Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equalsIgnoreCase(String.valueOf(spinner.getItemAtPosition(i)))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void uploadAvatar(Uri uri) {
        Toast.makeText(requireContext(), R.string.profile_photo_uploading, Toast.LENGTH_SHORT).show();
        Context appContext = requireContext().getApplicationContext();
        String token = sessionManager.getAuthToken();
        CcwHttp.runAsync(() -> {
            byte[] bytes;
            try {
                bytes = ImageUtils.readAndCompress(appContext, uri, 512, 86);
            } catch (Exception exception) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(appContext, R.string.listing_image_read_error, Toast.LENGTH_SHORT).show());
                return;
            }
            CcwApiClient.uploadAvatar(bytes, token, percent -> {}, new CcwApiClient.Callback<String>() {
                @Override
                public void onSuccess(String result) {
                    Toast.makeText(appContext, R.string.profile_photo_updated, Toast.LENGTH_SHORT).show();
                    if (isAdded()) reloadFromServer();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void loadFromCacheThenRefresh() {
        String username = getEffectiveUsername();
        if (username != null && !username.isEmpty()) {
            JSONObject cached = cacheDb.getProfile(username);
            if (cached != null) {
                profile = Profile.fromJson(cached);
                renderProfile();
            }
        }
        reloadFromServer();
        loadCollections();
    }

    private void reloadFromServer() {
        String username = getEffectiveUsername();
        if (username == null || username.isEmpty()) {
            swipeProfile.setRefreshing(false);
            return;
        }
        CcwApiClient.getProfile(username, sessionManager.getAuthToken(), new CcwApiClient.Callback<Profile>() {
            @Override
            public void onSuccess(Profile result) {
                if (!isAdded()) return;
                swipeProfile.setRefreshing(false);
                profile = result;
                renderProfile();
                cacheProfile(result);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                swipeProfile.setRefreshing(false);
                Toast.makeText(requireContext(),
                        message == null || message.isEmpty() ? getString(R.string.generic_offline_message) : message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cacheProfile(Profile profile) {
        try {
            JSONObject json = new JSONObject();
            json.put("username", profile.username);
            json.put("display_name", profile.displayName);
            json.put("real_name", profile.realName);
            json.put("bio", profile.bio);
            json.put("link", profile.link);
            json.put("campus", profile.campus);
            json.put("avatar_url", profile.avatarUrl);
            json.put("created_at", profile.createdAt);
            JSONObject counts = new JSONObject();
            counts.put("listings", profile.listingsCount);
            counts.put("followers", profile.followersCount);
            counts.put("following", profile.followingCount);
            json.put("counts", counts);
            JSONObject rating = new JSONObject();
            rating.put("count", profile.ratingCount);
            rating.put("average", profile.ratingAverage);
            json.put("rating", rating);
            if (profile.noteBody != null && !profile.noteBody.isEmpty()) {
                JSONObject note = new JSONObject();
                note.put("body", profile.noteBody);
                note.put("expires_at", profile.noteExpiresAt);
                json.put("note", note);
            }
            json.put("is_me", profile.isMe);
            json.put("is_following", profile.isFollowing);
            cacheDb.putProfile(profile.username, json);
        } catch (Exception ignored) {
        }
    }

    private void loadCollections() {
        CcwApiClient.listCollections(sessionManager.getAuthToken(), new CcwApiClient.Callback<List<Collection>>() {
            @Override
            public void onSuccess(List<Collection> result) {
                collections.clear();
                if (result != null) collections.addAll(result);
                collectionsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                // keep any cached list from prior load
            }
        });
    }

    private void renderProfile() {
        if (profile == null || !isAdded()) return;
        tvTopUsername.setText("@" + profile.username);
        tvCountListings.setText(String.valueOf(profile.listingsCount));
        tvCountFollowers.setText(String.valueOf(profile.followersCount));
        tvCountFollowing.setText(String.valueOf(profile.followingCount));
        String display = profile.displayName == null || profile.displayName.isEmpty()
                ? "@" + profile.username : profile.displayName;
        tvDisplayName.setText(display);
        tvBio.setText(profile.bio == null || profile.bio.isEmpty() ? getString(R.string.profile_bio_empty) : profile.bio);
        tvLink.setText(profile.link == null ? "" : profile.link);
        tvLink.setVisibility(profile.link == null || profile.link.isEmpty() ? View.GONE : View.VISIBLE);
        tvCampus.setText(profile.campus == null ? "" : profile.campus);
        tvCampus.setVisibility(profile.campus == null || profile.campus.isEmpty() ? View.GONE : View.VISIBLE);
        boolean hasNote = profile.hasActiveNote();
        tvNote.setVisibility(hasNote ? View.VISIBLE : View.GONE);
        if (hasNote) tvNote.setText(profile.noteBody);
        Glide.with(ivAvatar)
                .load(CcwHttp.resolveUrl(profile.avatarUrl))
                .placeholder(R.drawable.bg_avatar_circle)
                .error(R.drawable.bg_avatar_circle)
                .circleCrop()
                .into(ivAvatar);

        ivAvatarEditBadge.setVisibility(profile.isMe ? View.VISIBLE : View.GONE);
        btnMenu.setVisibility(profile.isMe ? View.VISIBLE : View.GONE);
        btnCreate.setVisibility(profile.isMe ? View.VISIBLE : View.GONE);
        layoutOwnActions.setVisibility(profile.isMe ? View.VISIBLE : View.GONE);
        layoutOtherActions.setVisibility(profile.isMe ? View.GONE : View.VISIBLE);
        btnFollow.setText(profile.isFollowing ? R.string.profile_unfollow : R.string.profile_follow);
    }

    private String getEffectiveUsername() {
        if (targetUsername != null && !targetUsername.isEmpty()) return targetUsername;
        String fromSession = sessionManager.getLoggedInUsername();
        return fromSession == null ? "" : fromSession;
    }

    private static String textOf(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    @Override
    public void onFollowCountChanged(int followers, boolean nowFollowing) {
        if (profile == null) return;
        profile.followersCount = followers;
        profile.isFollowing = nowFollowing;
        renderProfile();
    }

    @Override
    public void onOpenProfile(String username) {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).openProfile(username);
        }
    }

        /**
     * Generates and shows a QR code for the current user's profile
     * The QR code contains the username which can be scanned to quickly view/profile the user
     */
    private void showQrCode() {
        if (profile == null || TextUtils.isEmpty(profile.username)) {
            Toast.makeText(requireContext(), R.string.profile_qr_code_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        // Inflate the QR code dialog view
        View qrView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr_code, null);
        ImageView ivQrCode = qrView.findViewById(R.id.ivQrCode);
        TextView tvQrUsername = qrView.findViewById(R.id.tvQrUsername);
        MaterialButton btnShareQr = qrView.findViewById(R.id.btnShareQr);

        // Set the username text
        tvQrUsername.setText("@" + profile.username);

        // Generate and set the QR code
        try {
            Bitmap qrBitmap = generateQrCode(profile.username);
            ivQrCode.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            Toast.makeText(requireContext(), R.string.profile_qr_code_generation_error, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }

        // Set up share button
        btnShareQr.setOnClickListener(v -> shareQrCode(ivQrCode));

        // Show the dialog
        new AlertDialog.Builder(requireContext())
                .setView(qrView)
                .setPositiveButton(R.string.action_close, null)
                .show();
    }

    /**
     * Generates a QR code bitmap from the given data
     *
     * @param data The data to encode in the QR code
     * @return Bitmap representing the QR code
     * @throws WriterException If QR code generation fails
     */
    private Bitmap generateQrCode(String data) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 512, 512);

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    /**
     * Shares the QR code image via Android share intent
     *
     * @param qrImageView The ImageView containing the QR code to share
     */
    private void shareQrCode(ImageView qrImageView) {
        if (qrImageView.getDrawable() == null) {
            Toast.makeText(requireContext(), R.string.profile_qr_code_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        BitmapDrawable bitmapDrawable = ((BitmapDrawable) qrImageView.getDrawable());
        Bitmap bitmap = bitmapDrawable.getBitmap();

        // Save bitmap to cache directory
        File cachePath = new File(requireContext().getCacheDir(), "images");
        if (!cachePath.exists()) {
            cachePath.mkdirs();
        }
        String filename = "qr_code_" + System.currentTimeMillis() + ".png";
        File imageFile = new File(cachePath, filename);
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), R.string.profile_qr_code_share_error, Toast.LENGTH_SHORT).show();
            return;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Share the image
        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", imageFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setClipData(ClipData.newRawUri("", uri));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.profile_qr_code_share)));
    }

    private final class CollectionsAdapter extends RecyclerView.Adapter<CollectionsAdapter.CollectionHolder> {

        @NonNull
        @Override
        public CollectionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_profile_collection, parent, false);
            return new CollectionHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CollectionHolder holder, int position) {
            if (position == 0) {
                holder.name.setText(getString(R.string.profile_collection_new));
                holder.icon.setImageResource(R.drawable.ic_plus);
                holder.itemView.setOnClickListener(v -> showCreateCollectionDialog());
                return;
            }
            Collection collection = collections.get(position - 1);
            holder.icon.setImageResource(R.drawable.ic_archive);
            holder.name.setText(collection.name == null || collection.name.isEmpty()
                    ? getString(R.string.profile_collection_default_name, collection.id)
                    : collection.name);
            holder.itemView.setOnClickListener(v ->
                    Toast.makeText(requireContext(),
                            getString(R.string.profile_collection_items_count, collection.itemCount),
                            Toast.LENGTH_SHORT).show());
        }

        @Override
        public int getItemCount() {
            return collections.size() + 1;
        }

        final class CollectionHolder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;

            CollectionHolder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.ivCollectionIcon);
                name = itemView.findViewById(R.id.tvCollectionName);
            }
        }
    }

    private void showCreateCollectionDialog() {
        EditText input = new EditText(requireContext());
        input.setHint(R.string.profile_collection_name_hint);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_collection_new)
                .setView(input)
                .setNegativeButton(R.string.listing_action_cancel, null)
                .setPositiveButton(R.string.listing_action_confirm, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText() == null ? "" : input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError(getString(R.string.profile_collection_name_hint));
                return;
            }
            CcwApiClient.createCollection(name, sessionManager.getAuthToken(), new CcwApiClient.Callback<Collection>() {
                @Override
                public void onSuccess(Collection result) {
                    collections.add(0, result);
                    collectionsAdapter.notifyDataSetChanged();
                    dialog.dismiss();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        }));
        dialog.show();
    }
}
