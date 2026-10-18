package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusconnectmobile.databinding.FragmentHomeFeedBinding;

import org.json.JSONObject;

/** Home tab: the latest listings on the user's campus, straight from the server. */
public class HomeFeedFragment extends Fragment {

    private static final String ARG_EMAIL = "arg_email";
    private static final int PAGE_SIZE = 20;

    private FragmentHomeFeedBinding binding;
    private FeedAdapter adapter;
    private SessionManager sessionManager;
    private String campus;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private boolean loadedOnce = false;

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
        binding = FragmentHomeFeedBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        String email = getArguments() != null ? getArguments().getString(ARG_EMAIL) : null;
        DatabaseHelper.UserProfile profile = email == null ? null : new DatabaseHelper(requireContext()).getProfile(email);
        campus = profile == null || profile.campusLocation == null || profile.campusLocation.isEmpty()
                ? null : profile.campusLocation;
        binding.tvFeedHeader.setText(campus == null ? getString(R.string.app_name) : campus + " Feed");
        binding.tvEmptyFeed.setText(R.string.feed_empty);

        adapter = new FeedAdapter(sessionManager.getLoggedInUsername(), new FeedAdapter.Listener() {
            @Override
            public void onOpen(Listing listing) {
                Intent intent = new Intent(requireContext(), ListingDetailActivity.class);
                intent.putExtra(ListingDetailActivity.EXTRA_LISTING, listing);
                startActivity(intent);
            }

            @Override
            public void onToggleSave(Listing listing) {
                toggleSave(listing);
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.rvFeed.setLayoutManager(layoutManager);
        binding.rvFeed.setAdapter(adapter);
        binding.rvFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMore) return;
                if (layoutManager.findLastVisibleItemPosition() >= adapter.getItemCount() - 3) {
                    loadPage(currentPage + 1);
                }
            }
        });
        binding.swipeRefresh.setOnRefreshListener(() -> loadPage(1));

        binding.shimmerViewContainer.startShimmer();
        loadPage(1);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Pick up listings posted or changed while another screen was on top
        if (loadedOnce && !isLoading) loadPage(1);
    }

    private void loadPage(int page) {
        if (isLoading || binding == null) return;
        isLoading = true;
        CcwApiClient.listListings(campus, null, page, PAGE_SIZE, sessionManager.getAuthToken(),
                new CcwApiClient.Callback<ListPage<Listing>>() {
                    @Override
                    public void onSuccess(ListPage<Listing> result) {
                        if (!isAdded() || binding == null) return;
                        isLoading = false;
                        loadedOnce = true;
                        currentPage = page;
                        hasMore = result.hasMore() && !result.items.isEmpty();
                        if (page == 1) {
                            adapter.submitList(result.items);
                        } else {
                            adapter.appendList(result.items);
                        }
                        showContent();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded() || binding == null) return;
                        isLoading = false;
                        String text = message == null || message.isEmpty()
                                ? getString(R.string.generic_offline_message) : message;
                        if (adapter.isEmpty()) {
                            binding.tvEmptyFeed.setText(text);
                        } else {
                            Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
                        }
                        showContent();
                    }
                });
    }

    private void showContent() {
        binding.shimmerViewContainer.stopShimmer();
        binding.shimmerViewContainer.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(false);
        boolean empty = adapter.isEmpty();
        binding.tvEmptyFeed.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvFeed.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void toggleSave(Listing listing) {
        CcwApiClient.Callback<JSONObject> callback = new CcwApiClient.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                if (!isAdded() || binding == null) return;
                listing.isSaved = result.optBoolean("saved", !listing.isSaved);
                listing.saves = result.optInt("saves", listing.saves);
                adapter.updateListing(listing);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        };
        String token = sessionManager.getAuthToken();
        if (listing.isSaved) {
            CcwApiClient.unsaveListing(listing.id, token, callback);
        } else {
            CcwApiClient.saveListing(listing.id, token, callback);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
