package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
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

    private LinearLayout bottomNav;
    private String email;
    private SessionManager sessionManager;
    private int currentSelectedTabId = R.id.nav_home;

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
        int[] destinationIds = {R.id.nav_home, R.id.nav_adverts, R.id.nav_search,
                R.id.nav_camera, R.id.nav_messages, R.id.nav_profile};
        for (int destinationId : destinationIds) {
            View destination = bottomNav.findViewById(destinationId);
            destination.setOnClickListener(view -> selectTab(view.getId()));
        }
    }

    private void selectTab(int destinationId) {
        if (destinationId == currentSelectedTabId) return;
        if (handleNavigation(destinationId)) {
            currentSelectedTabId = destinationId;
            updateNavigationAppearance();
        }
    }

    private void updateNavigationAppearance() {
        int[] destinationIds = {R.id.nav_home, R.id.nav_adverts, R.id.nav_search,
                R.id.nav_camera, R.id.nav_messages, R.id.nav_profile};
        int[] iconIds = {R.id.ivNavHome, R.id.ivNavAdverts, R.id.ivNavSearch,
                R.id.ivNavCamera, R.id.ivNavMessages, R.id.ivNavProfile};
        int[] labelIds = {R.id.tvNavHome, R.id.tvNavAdverts, R.id.tvNavSearch,
                R.id.tvNavCamera, R.id.tvNavMessages, R.id.tvNavProfile};
        int activeColor = getColor(R.color.colorAccent);
        int inactiveColor = getColor(R.color.colorTextMuted);

        for (int i = 0; i < destinationIds.length; i++) {
            boolean selected = destinationIds[i] == currentSelectedTabId;
            View destination = bottomNav.findViewById(destinationIds[i]);
            destination.setSelected(selected);
            ImageView icon = bottomNav.findViewById(iconIds[i]);
            TextView label = bottomNav.findViewById(labelIds[i]);
            if (destinationIds[i] != R.id.nav_camera) {
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
        } else if (menuItemId == R.id.nav_adverts) {
            fragment = new AdvertsFragment();
        } else if (menuItemId == R.id.nav_search) {
            fragment = SearchFragment.newInstance(email);
        } else if (menuItemId == R.id.nav_camera) {
            fragment = CameraFragment.newInstance(email);
        } else if (menuItemId == R.id.nav_messages) {
            fragment = MessagesFragment.newInstance(email);
        } else if (menuItemId == R.id.nav_profile) {
            fragment = ProfileFragment.newInstance(email);
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

        AuthApiClient.validateSession(sessionManager.getAuthToken(), new AuthApiClient.Callback() {
            @Override
            public void onSuccess(AuthApiClient.AuthResult result) {
                if (!email.equalsIgnoreCase(result.email)) {
                    sessionManager.clearSession();
                    redirectToLogin();
                }
            }

            @Override
            public void onError(String message) {
                sessionManager.clearSession();
                redirectToLogin();
            }
        });
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