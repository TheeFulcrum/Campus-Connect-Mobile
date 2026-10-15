package com.example.campusconnectmobile;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    /** How often the unread messages badge is refreshed while the app is in the foreground. */
    private static final long UNREAD_POLL_INTERVAL_MS = 20_000L;

    /** Request code for QR code scanning */
    private static final int REQUEST_QR_SCAN = 1001;

    private LinearLayout bottomNav;
    private TextView tvMessagesUnreadBadge;
    private TextView tvNotificationsUnreadBadge;
    private String email;
    private SessionManager sessionManager;
    private int currentSelectedTabId = R.id.nav_home;
    private String profileUsernameOverride;

    private final Handler unreadHandler = new Handler(Looper.getMainLooper());
    private final Runnable unreadPollRunnable = new Runnable() {
        @Override
        public void run() {
            refreshUnreadMessagesBadge();
            refreshUnreadNotificationsBadge();
            unreadHandler.postDelayed(this, UNREAD_POLL_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);

        // Get email from intent
        email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email == null) {
            email = sessionManager.getLoggedInEmail();
            if (email == null) {
                redirectToLogin();
                return;
            }
        } else {
            sessionManager.createSession(email);
        }

        // Initialize views
        bottomNav = findViewById(R.id.bottomNav);
        tvMessagesUnreadBadge = findViewById(R.id.tvMessagesUnreadBadge);
        tvNotificationsUnreadBadge = findViewById(R.id.tvNotificationsUnreadBadge);
        findViewById(R.id.btnMessagesTopBar).setOnClickListener(v -> openMessages());
        findViewById(R.id.btnNotificationsTopBar).setOnClickListener(v -> openNotifications());
        findViewById(R.id.btnScanQrTopBar).setOnClickListener(v -> scanQrCode());

        if (savedInstanceState != null) {
            currentSelectedTabId = savedInstanceState.getInt("selected_tab_id", R.id.nav_home);
        }

        // Default tab on launch
        if (savedInstanceState == null) {
            loadFragment(HomeFeedFragment.newInstance(email));
        }
        setupBottomNavigation();
        updateNavigationAppearance();
    }

    private void setupBottomNavigation() {
        int[] destinationIds = {R.id.nav_home, R.id.nav_search, R.id.nav_create,
                R.id.nav_listings, R.id.nav_profile};
        for (int destinationId : destinationIds) {
            View destination = bottomNav.findViewById(destinationId);
            destination.setOnClickListener(view -> onNavItemClicked(view.getId()));
        }
    }

    private void onNavItemClicked(int destinationId) {
        if (destinationId == R.id.nav_create) {
            // Create always opens the listing composer; it is a momentary action,
            // not a persistent tab, so the previously selected tab stays selected.
            startActivity(new Intent(this, CreateListingActivity.class));
            return;
        }
        if (destinationId != R.id.nav_profile) {
            profileUsernameOverride = null;
        }
        selectTab(destinationId);
    }

    private void selectTab(int destinationId) {
        if (destinationId == currentSelectedTabId) return;
        if (handleNavigation(destinationId)) {
            currentSelectedTabId = destinationId;
            updateNavigationAppearance();
        }
    }

    private void updateNavigationAppearance() {
        int[] destinationIds = {R.id.nav_home, R.id.nav_search, R.id.nav_create,
                R.id.nav_listings, R.id.nav_profile};
        int[] iconIds = {R.id.ivNavHome, R.id.ivNavSearch, R.id.ivNavCreate,
                R.id.ivNavListings, R.id.ivNavProfile};
        int[] labelIds = {R.id.tvNavHome, R.id.tvNavSearch, R.id.tvNavCreate,
                R.id.tvNavListings, R.id.tvNavProfile};
        int activeColor = getColor(R.color.colorAccent);
        int inactiveColor = getColor(R.color.colorTextMuted);

        for (int i = 0; i < destinationIds.length; i++) {
            boolean selected = destinationIds[i] == currentSelectedTabId;
            View destination = bottomNav.findViewById(destinationIds[i]);
            destination.setSelected(selected);
            ImageView icon = bottomNav.findViewById(iconIds[i]);
            TextView label = bottomNav.findViewById(labelIds[i]);
            if (destinationIds[i] != R.id.nav_create) {
                icon.setColorFilter(selected ? activeColor : inactiveColor);
            }
            label.setTextColor(selected ? activeColor : inactiveColor);
            label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    /**
     * Handles navigation to the appropriate fragment based on menu item ID.
     */
    private boolean handleNavigation(int menuItemId) {
        Fragment fragment = null;

        if (menuItemId == R.id.nav_home) {
            fragment = HomeFeedFragment.newInstance(email);
        } else if (menuItemId == R.id.nav_search) {
            fragment = SearchFragment.newInstance(email);
        } else if (menuItemId == R.id.nav_listings) {
            fragment = ListingsFragment.newInstance();
        } else if (menuItemId == R.id.nav_profile) {
            fragment = ProfileFragment.newInstance(email, profileUsernameOverride);
        } else {
            return false;
        }

        loadFragment(fragment);
        return true;
    }

    /**
     * Loads a fragment into the container with null safety.
     */
    private void loadFragment(Fragment fragment) {
        if (fragment == null) {
            return;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.commit();
    }

    private void openMessages() {
        // Messages lives outside the 5-item bottom tab set, so it is pushed on top of
        // whichever tab is currently showing rather than becoming a selected tab itself.
        loadFragment(MessagesFragment.newInstance(email));
    }

    private void openNotifications() {
        loadFragment(new NotificationsFragment());
    }

    public void openProfile(String username) {
        profileUsernameOverride = username;
        currentSelectedTabId = R.id.nav_profile;
        loadFragment(ProfileFragment.newInstance(email, username));
        updateNavigationAppearance();
    }

    private void refreshUnreadMessagesBadge() {
        String token = sessionManager.getAuthToken();
        if (token == null) return;
        CcwApiClient.getUnreadMessagesCount(token, new CcwApiClient.Callback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                if (isFinishing() || isDestroyed()) return;
                if (count != null && count > 0) {
                    tvMessagesUnreadBadge.setVisibility(View.VISIBLE);
                    tvMessagesUnreadBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                } else {
                    tvMessagesUnreadBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                // Keep the previous badge state when offline; nothing to show otherwise.
            }
        });
    }

    private void refreshUnreadNotificationsBadge() {
        String token = sessionManager.getAuthToken();
        if (token == null) return;
        CcwApiClient.getUnreadNotificationsCount(token, new CcwApiClient.Callback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                if (isFinishing() || isDestroyed()) return;
                if (count != null && count > 0) {
                    tvNotificationsUnreadBadge.setVisibility(View.VISIBLE);
                    tvNotificationsUnreadBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                } else {
                    tvNotificationsUnreadBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                // Keep previous state when offline.
            }
        });
    }

    /**
     * Scans a QR code using an external scanner app or built-in scanner.
     * If a username is scanned, opens that user's profile.
     */
    private void scanQrCode() {
        // Try to use a QR code scanning intent (like Barcode Scanner by ZXing Team)
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_FORMATS", "QR_CODE");

        // Check if there's an app to handle this intent
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        boolean isIntentSafe = activities.size() > 0;

        if (isIntentSafe) {
            startActivityForResult(intent, REQUEST_QR_SCAN);
        } else {
            // If no scanner app is available, show a dialog to download one
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.qr_code_scanner_not_found));
            builder.setMessage(getString(R.string.qr_code_scanner_download_prompt));
            builder.setPositiveButton(getString(R.string.action_download), (dialog, which) -> {
                // Open Play Store to Barcode Scanner app
                Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                marketIntent.setData(Uri.parse("market://details?id=com.google.zxing.client.android"));
                startActivity(marketIntent);
            });
            builder.setNegativeButton(getString(R.string.action_cancel), null);
            builder.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_QR_SCAN) {
            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                String format = data.getStringExtra("SCAN_RESULT_FORMAT");
                // Handle successful scan
                handleQrScanResult(contents);
            } else if (resultCode == RESULT_CANCELED) {
                // Handle cancel
                Toast.makeText(this, getString(R.string.qr_code_scan_cancelled), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Handles the result from a QR code scan.
     * If the result is a valid username, opens that user's profile.
     *
     * @param contents The scanned contents from the QR code
     */
    private void handleQrScanResult(String contents) {
        if (contents == null || contents.isEmpty()) {
            Toast.makeText(this, getString(R.string.qr_code_no_username), Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the scanned content looks like a username (starts with @ or is alphanumeric)
        String username = contents.trim();
        if (username.startsWith("@")) {
            username = username.substring(1); // Remove the @ prefix
        }

        // Basic validation: username should not be empty and should be reasonable length
        if (username.isEmpty() || username.length() > 30) {
            Toast.makeText(this, getString(R.string.qr_code_no_username), Toast.LENGTH_SHORT).show();
            return;
        }

        // Open the profile for the scanned username
        openProfile(username);
        Toast.makeText(this, getString(R.string.profile_found_opening, username), Toast.LENGTH_SHORT).show();
    }

    /**
     * Called by ProfileFragment when the user logs out.
     */
    public void onLoggedOut() {
        clearSessionAndRedirect();
    }

    /**
     * Clears user session and redirects to login screen.
     */
    private void clearSessionAndRedirect() {
        String token = sessionManager.getAuthToken();
        sessionManager.clearSession();
        if (token != null) {
            AuthApiClient.logout(token, new AuthApiClient.Callback() {
                @Override
                public void onSuccess(AuthApiClient.AuthResult result) {
                    // The local session has already been removed.
                }

                @Override
                public void onError(String message) {
                    // Local logout must still complete when the device is offline.
                }
            });
        }
        redirectToLogin();
    }

    /**
     * Redirects user to the MainActivity (login screen).
     */
    private void redirectToLogin() {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_tab_id", currentSelectedTabId);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentSelectedTabId = savedInstanceState.getInt("selected_tab_id", R.id.nav_home);
        updateNavigationAppearance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        unreadHandler.removeCallbacks(unreadPollRunnable);
        unreadHandler.post(unreadPollRunnable);

        AuthApiClient.validateSession(sessionManager.getAuthToken(), new AuthApiClient.Callback() {
            @Override
            public void onSuccess(AuthApiClient.AuthResult result) {
                if (!email.equalsIgnoreCase(result.email)) {
                    sessionManager.clearSession();
                    redirectToLogin();
                } else {
                    sessionManager.setLoggedInUsername(result.username);
                }
            }

            @Override
            public void onError(String message) {
                sessionManager.clearSession();
                redirectToLogin();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        unreadHandler.removeCallbacks(unreadPollRunnable);
    }

    /**
     * Handles back button press - exit app if on home tab, otherwise navigate to home.
     */
    @Override
    public void onBackPressed() {
        if (currentSelectedTabId != R.id.nav_home) {
            selectTab(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }
}
