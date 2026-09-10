package com.example.campusconnectmobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.List;

public class HomeFeedFragment extends Fragment {

    private static final String ARG_EMAIL = "arg_email";

    public static HomeFeedFragment newInstance(String email) {
        HomeFeedFragment fragment = new HomeFeedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_feed, container, false);

        TextView tvHeader = view.findViewById(R.id.tvFeedHeader);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyFeed);
        RecyclerView rvFeed = view.findViewById(R.id.rvFeed);
        ShimmerFrameLayout shimmerLayout = view.findViewById(R.id.shimmer_view_container);
        rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Start shimmer effect
        shimmerLayout.startShimmer();

        String email = getArguments() != null ? getArguments().getString(ARG_EMAIL) : null;
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());

        if (email != null) {
            DatabaseHelper.UserProfile profile = dbHelper.getProfile(email);
            if (profile != null && profile.username != null) {
                tvHeader.setText(profile.campusLocation + " Feed");

                // Simulate data loading delay
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    List<DatabaseHelper.Post> posts = dbHelper.getFeedForUser(
                            profile.campusLocation, profile.preferences);

                    // Stop and hide shimmer
                    shimmerLayout.stopShimmer();
                    shimmerLayout.setVisibility(View.GONE);

                    if (posts.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvFeed.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvFeed.setVisibility(View.VISIBLE);
                        rvFeed.setAdapter(new PostAdapter(posts));
                    }
                }, 1500);
            }
        }

        return view;
    }
}