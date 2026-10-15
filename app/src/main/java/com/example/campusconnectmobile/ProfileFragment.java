package com.example.campusconnectmobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// ViewBinding import
import com.example.campusconnectmobile.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    private static final String ARG_EMAIL = "arg_email";
    private static final String ARG_USERNAME_OVERRIDE = "arg_username_override";

    private String email;
    private String usernameOverride;

    // ViewBinding
    private FragmentProfileBinding binding;

    public static ProfileFragment newInstance(String email, String usernameOverride) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        args.putString(ARG_USERNAME_OVERRIDE, usernameOverride);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate ViewBinding and return the root view
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            email = getArguments().getString(ARG_EMAIL);
            usernameOverride = getArguments().getString(ARG_USERNAME_OVERRIDE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up binding reference to prevent memory leaks
        binding = null;
    }
}