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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

// ViewBinding import
import com.example.campusconnectmobile.databinding.FragmentHomeFeedBinding;

import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.List;

public class HomeFeedFragment extends Fragment {

    private static final String ARG_EMAIL = "arg_email";
    private final Handler feedHandler = new Handler(Looper.getMainLooper());
    private Runnable loadRunnable;

    // ViewBinding
    private FragmentHomeFeedBinding binding;

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
        // Inflate ViewBinding
        binding = FragmentHomeFeedBinding.inflate(inflater, container, false);

        // Set up swipe to refresh
        binding.swipeRefresh.setOnRefreshListener(() -> {
            // Prevent multiple simultaneous refreshes
            if (!binding.swipeRefresh.isRefreshing()) {
                binding.swipeRefresh.setRefreshing(true);
                loadFeedData();
            }
        });

        // Start shimmer effect
        binding.shimmerViewContainer.startShimmer();

        String email = getArguments() != null ? getArguments().getString(ARG_EMAIL) : null;
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());

        DatabaseHelper.UserProfile profile = email == null ? null : dbHelper.getProfile(email);
        if (profile != null && profile.username != null) {
            binding.tvFeedHeader.setText(profile.campusLocation + " Feed");
            loadRunnable = () -> {
                if (!isAdded() || getView() == null) return;
                List<DatabaseHelper.Post> posts = dbHelper.getFeedForUser(
                        profile.campusLocation, profile.preferences);
                finishLoading(binding.shimmerViewContainer, binding.tvEmptyFeed, binding.rvFeed, posts);
                // Stop refreshing if we were refreshing
                if (binding.swipeRefresh.isRefreshing()) {
                    binding.swipeRefresh.setRefreshing(false);
                }
            };
            feedHandler.postDelayed(loadRunnable, 300);
        } else {
            finishLoading(binding.shimmerViewContainer, binding.tvEmptyFeed, binding.rvFeed, java.util.Collections.emptyList());
            binding.tvEmptyFeed.setText("Complete your profile to see campus listings.");
            // Stop refreshing if we were refreshing
            if (binding.swipeRefresh.isRefreshing()) {
                binding.swipeRefresh.setRefreshing(false);
            }
        }

        return binding.getRoot();
    }

    private void loadFeedData() {
        String email = getArguments() != null ? getArguments().getString(ARG_EMAIL) : null;
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());

        DatabaseHelper.UserProfile profile = email == null ? null : dbHelper.getProfile(email);
        if (profile != null && profile.username != null) {
            List<DatabaseHelper.Post> posts = dbHelper.getFeedForUser(
                    profile.campusLocation, profile.preferences);
            finishLoading(binding.shimmerViewContainer, binding.tvEmptyFeed, binding.rvFeed, posts);
        } else {
            finishLoading(binding.shimmerViewContainer, binding.tvEmptyFeed, binding.rvFeed, java.util.Collections.emptyList());
            binding.tvEmptyFeed.setText("Complete your profile to see campus listings.");
        }
        // Stop refreshing
        if (binding.swipeRefresh.isRefreshing()) {
            binding.swipeRefresh.setRefreshing(false);
        }
    }

    private void finishLoading(ShimmerFrameLayout shimmerLayout, TextView tvEmpty,
                               RecyclerView rvFeed, List<DatabaseHelper.Post> posts) {
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
    }

    @Override
    public void onDestroyView() {
        if (loadRunnable != null) {
            feedHandler.removeCallbacks(loadRunnable);
        }
        super.onDestroyView();
        // Clean up binding reference to prevent memory leaks
        binding = null;
    }
}