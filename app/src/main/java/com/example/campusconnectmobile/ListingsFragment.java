package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;

/**
 * Real-API listings grid for the bottom-nav "Listings" tab: a 3-column square
 * grid of every active listing (server is the source of truth), with
 * pagination on scroll, pull-to-refresh, shimmer skeleton while loading, and
 * empty/error states with retry.
 */
public class ListingsFragment extends Fragment {

    private static final int SPAN_COUNT = 3;
    private static final int PAGE_SIZE = 24;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvListings;
    private ShimmerFrameLayout shimmerListings;
    private View layoutEmpty;
    private View layoutError;
    private TextView tvError;
    private Button btnRetry;

    private ListingsAdapter adapter;
    private SessionManager sessionManager;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private boolean loadedOnce = false;

    public static ListingsFragment newInstance() {
        return new ListingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listings, container, false);

        sessionManager = new SessionManager(requireContext());
        swipeRefresh = view.findViewById(R.id.swipeRefreshListings);
        rvListings = view.findViewById(R.id.rvListings);
        shimmerListings = view.findViewById(R.id.shimmerListings);
        layoutEmpty = view.findViewById(R.id.layoutListingsEmpty);
        layoutError = view.findViewById(R.id.layoutListingsError);
        tvError = view.findViewById(R.id.tvListingsError);
        btnRetry = view.findViewById(R.id.btnListingsRetry);

        RecyclerView rvSkeleton = view.findViewById(R.id.rvListingsSkeleton);
        rvSkeleton.setLayoutManager(new GridLayoutManager(requireContext(), SPAN_COUNT));
        rvSkeleton.setAdapter(new SkeletonAdapter());

        adapter = new ListingsAdapter(this::openListing);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), SPAN_COUNT);
        rvListings.setLayoutManager(layoutManager);
        rvListings.setAdapter(adapter);
        rvListings.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || isLoading || !hasMore) return;
                int visibleCount = layoutManager.getChildCount();
                int totalCount = layoutManager.getItemCount();
                int firstVisible = layoutManager.findFirstVisibleItemPosition();
                if (visibleCount + firstVisible >= totalCount - SPAN_COUNT * 2) {
                    loadPage(currentPage + 1, false);
                }
            }
        });

        swipeRefresh.setOnRefreshListener(() -> loadPage(1, true));
        btnRetry.setOnClickListener(v -> loadPage(1, false));

        loadPage(1, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Pick up listings posted or changed while another screen was on top
        if (loadedOnce) loadPage(1, true);
    }

    private void openListing(Listing listing) {
        Intent intent = new Intent(requireContext(), ListingDetailActivity.class);
        intent.putExtra(ListingDetailActivity.EXTRA_LISTING, listing);
        startActivity(intent);
    }

    private void loadPage(int page, boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;
        String token = sessionManager.getAuthToken();

        if (page == 1 && !isRefresh && adapter.isEmpty()) {
            shimmerListings.startShimmer();
            shimmerListings.setVisibility(View.VISIBLE);
            rvListings.setVisibility(View.GONE);
        }
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        CcwApiClient.listListings(null, null, page, PAGE_SIZE, token, new CcwApiClient.Callback<ListPage<Listing>>() {
            @Override
            public void onSuccess(ListPage<Listing> result) {
                if (!isAdded()) return;
                isLoading = false;
                loadedOnce = true;
                currentPage = page;
                hasMore = result.hasMore();
                finishLoading();
                if (page == 1) {
                    adapter.submitList(result.items);
                } else {
                    adapter.appendList(result.items);
                }
                if (adapter.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                isLoading = false;
                finishLoading();
                if (adapter.isEmpty()) {
                    tvError.setText(message == null || message.isEmpty()
                            ? getString(R.string.generic_offline_message) : message);
                    layoutError.setVisibility(View.VISIBLE);
                } else {
                    android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void finishLoading() {
        shimmerListings.stopShimmer();
        shimmerListings.setVisibility(View.GONE);
        rvListings.setVisibility(View.VISIBLE);
        swipeRefresh.setRefreshing(false);
    }

    /** Minimal adapter that just renders N square skeleton tiles while the shimmer runs. */
    private static final class SkeletonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int COUNT = 18;

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_listing_skeleton, parent, false);
            return new RecyclerView.ViewHolder(view) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            // Static skeleton tile, nothing to bind.
        }

        @Override
        public int getItemCount() {
            return COUNT;
        }
    }
}
