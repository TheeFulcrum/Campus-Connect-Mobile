package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private BottomNavigationView bottomNav;
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

        // Default tab on launch
        if (savedInstanceState == null) {
            loadFragment(HomeFeedFragment.newInstance(email));
        } else {
            // Restore previously selected tab if activity is recreated
            currentSelectedTabId = bottomNav.getSelectedItemId();
        }

        // Set up bottom navigation listener
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // Prevent reloading the same fragment
            if (currentSelectedTabId == id) {
                return true;
            }

            // Handle navigation based on selected item
            boolean handled = handleNavigation(id);
            if (handled) {
                currentSelectedTabId = id;
            }
            return handled;
        });
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
        bottomNav.setSelectedItemId(currentSelectedTabId);
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
            bottomNav.setSelectedItemId(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }
}