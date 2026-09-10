package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private static final String ARG_EMAIL = "arg_email";
    private String email;

    public static ProfileFragment newInstance(String email) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        email = getArguments() != null ? getArguments().getString(ARG_EMAIL) : null;
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());

        TextView tvUsername = view.findViewById(R.id.tvProfileUsername);
        TextView tvEmail = view.findViewById(R.id.tvProfileEmail);
        TextView tvCampus = view.findViewById(R.id.tvProfileCampus);
        TextView tvPreferences = view.findViewById(R.id.tvProfilePreferences);
        Button btnEditPreferences = view.findViewById(R.id.btnEditPreferences);
        Button btnLogout = view.findViewById(R.id.btnLogout);
        Button btnLight = view.findViewById(R.id.btnLightTheme);
        Button btnDark = view.findViewById(R.id.btnDarkTheme);
        
        SessionManager sessionManager = new SessionManager(requireContext());

        if (email != null) {
            DatabaseHelper.UserProfile profile = dbHelper.getProfile(email);
            if (profile != null && profile.username != null) {
                tvUsername.setText(profile.username);
                tvEmail.setText(email);
                tvCampus.setText(profile.campusLocation);
                tvPreferences.setText(profile.preferences != null
                        ? profile.preferences.replace(",", " · ")
                        : "");
            }
        }

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
}