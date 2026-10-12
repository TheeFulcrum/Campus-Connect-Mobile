package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class ProfileListingsTabFragment extends Fragment {

    private static final String ARG_MODE = "mode";
    private static final String ARG_USERNAME = "username";
    private static final String ARG_IS_ME = "is_me";
    private static final int PAGE_SIZE = 24;

    private String mode;
    private String username;
    private boolean isMe;
    private SessionManager sessionManager;
    private ListingsAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    public static ProfileListingsTabFragment newInstance(String mode, String username, boolean isMe) {
        ProfileListingsTabFragment fragment = new ProfileListingsTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        args.putString(ARG_USERNAME, username);
        args.putBoolean(ARG_IS_ME, isMe);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_listings_tab, container, false);
        mode = getArguments() == null ? "listings" : getArguments().getString(ARG_MODE, "listings");
        username = getArguments() == null ? "" : getArguments().getString(ARG_USERNAME, "");
        isMe = getArguments() != null && getArguments().getBoolean(ARG_IS_ME, false);
        sessionManager = new SessionManager(requireContext());

        swipeRefresh = view.findViewById(R.id.swipeTabListings);
        RecyclerView rv = view.findViewById(R.id.rvTabListings);
        tvEmpty = view.findViewById(R.id.tvTabEmpty);
        GridLayoutManager manager = new GridLayoutManager(requireContext(), 3);
        rv.setLayoutManager(manager);
        adapter = new ListingsAdapter(listing -> {
            Intent intent = new Intent(requireContext(), ListingDetailActivity.class);
            intent.putExtra(ListingDetailActivity.EXTRA_LISTING, listing);
            startActivity(intent);
        });
        rv.setAdapter(adapter);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMore) return;
                int visible = manager.getChildCount();
                int total = manager.getItemCount();
                int first = manager.findFirstVisibleItemPosition();
                if (visible + first >= total - 6) loadPage(currentPage + 1, false);
            }
        });

        swipeRefresh.setOnRefreshListener(() -> loadPage(1, true));
        loadPage(1, false);
        return view;
    }

    private void loadPage(int page, boolean refresh) {
        if (isLoading) return;
        isLoading = true;
        CcwApiClient.Callback<ListPage<Listing>> callback = new CcwApiClient.Callback<ListPage<Listing>>() {
            @Override
            public void onSuccess(ListPage<Listing> result) {
                if (!isAdded()) return;
                isLoading = false;
                swipeRefresh.setRefreshing(false);
                currentPage = page;
                hasMore = result != null && result.hasMore();
                if (page == 1) {
                    adapter.submitList(result == null ? new java.util.ArrayList<>() : result.items);
                } else if (result != null) {
                    adapter.appendList(result.items);
                }
                tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                isLoading = false;
                swipeRefresh.setRefreshing(false);
                tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        };

        String token = sessionManager.getAuthToken();
        if ("saved".equals(mode)) {
            if (!isMe) {
                isLoading = false;
                swipeRefresh.setRefreshing(false);
                adapter.submitList(new java.util.ArrayList<>());
                tvEmpty.setVisibility(View.VISIBLE);
                return;
            }
            CcwApiClient.getProfileSaved(page, token, callback);
        } else if ("sold".equals(mode)) {
            CcwApiClient.getProfileSold(username, page, token, callback);
        } else {
            CcwApiClient.getProfileListings(username, page, token, callback);
        }
    }
}
