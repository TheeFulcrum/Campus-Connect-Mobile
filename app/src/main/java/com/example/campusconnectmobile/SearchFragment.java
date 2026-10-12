package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private static final String ARG_EMAIL = "arg_email";
    private static final long DEBOUNCE_MS = 350L;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::runSearch;
    private SessionManager sessionManager;
    private ProfileCacheDbHelper cacheDb;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView rvResults;
    private TextView tvEmpty;
    private TextInputEditText etQuery;
    private SearchResultAdapter adapter;
    private String lastQuery = "";
    private int searchPage = 1;
    private boolean hasMoreResults = false;
    private boolean isLoading = false;

    public static SearchFragment newInstance(String email) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        sessionManager = new SessionManager(requireContext());
        cacheDb = new ProfileCacheDbHelper(requireContext());
        refreshLayout = view.findViewById(R.id.searchRefresh);
        rvResults = view.findViewById(R.id.rvSearchResults);
        tvEmpty = view.findViewById(R.id.tvSearchEmpty);
        etQuery = view.findViewById(R.id.etSearchQuery);

        adapter = new SearchResultAdapter();
        LinearLayoutManager manager = new LinearLayoutManager(requireContext());
        rvResults.setLayoutManager(manager);
        rvResults.setAdapter(adapter);
        rvResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMoreResults) return;
                int visible = manager.getChildCount();
                int first = manager.findFirstVisibleItemPosition();
                int total = manager.getItemCount();
                if (visible + first >= total - 4) {
                    searchPage += 1;
                    runSearch();
                }
            }
        });

        etQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchPage = 1;
                debounceHandler.removeCallbacks(searchRunnable);
                debounceHandler.postDelayed(searchRunnable, DEBOUNCE_MS);
            }
        });

        refreshLayout.setOnRefreshListener(() -> {
            searchPage = 1;
            runSearch();
        });
        showRecents();
        return view;
    }

    private void showRecents() {
        List<String> recent = cacheDb.getRecentSearches();
        List<SearchResultItem> items = new ArrayList<>();
        for (String query : recent) {
            SearchResultItem item = SearchResultItem.recent(query);
            items.add(item);
        }
        adapter.submitList(items);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText(R.string.search_empty_default);
        hasMoreResults = false;
    }

    private void runSearch() {
        String query = etQuery.getText() == null ? "" : etQuery.getText().toString().trim();
        if (query.isEmpty()) {
            refreshLayout.setRefreshing(false);
            isLoading = false;
            showRecents();
            return;
        }

        String token = sessionManager.getAuthToken();
        if (token == null) {
            refreshLayout.setRefreshing(false);
            return;
        }
        if (searchPage == 1) {
            adapter.submitList(new ArrayList<>());
        }
        isLoading = true;
        lastQuery = query;
        CcwApiClient.search(query, searchPage, token, new CcwApiClient.Callback<SearchResults>() {
            @Override
            public void onSuccess(SearchResults result) {
                if (!isAdded()) return;
                isLoading = false;
                refreshLayout.setRefreshing(false);
                if (searchPage == 1) {
                    cacheDb.addRecentSearch(query);
                }
                List<SearchResultItem> page = toItems(result);
                if (searchPage == 1) {
                    adapter.submitList(page);
                } else {
                    adapter.appendList(page);
                }
                int shown = adapter.getItemCount();
                int total = (result.usersTotal > 0 ? result.usersTotal : result.users.size())
                        + (result.listingsTotal > 0 ? result.listingsTotal : result.listings.size());
                hasMoreResults = shown < total;
                if (adapter.getItemCount() == 0) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(getString(R.string.search_empty_for_query, query));
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                isLoading = false;
                refreshLayout.setRefreshing(false);
                Toast.makeText(requireContext(),
                        message == null || message.isEmpty()
                                ? getString(R.string.generic_offline_message) : message,
                        Toast.LENGTH_SHORT).show();
                if (adapter.getItemCount() == 0) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(getString(R.string.search_empty_for_query, query));
                }
            }
        });
    }

    private List<SearchResultItem> toItems(SearchResults result) {
        ArrayList<SearchResultItem> items = new ArrayList<>();
        for (SearchUserResult user : result.users) {
            String title = user.displayName == null || user.displayName.isEmpty()
                    ? "@" + user.username : user.displayName + " (@" + user.username + ")";
            String subtitle = joinParts(user.campus, user.bio);
            items.add(SearchResultItem.user(title, subtitle, user.username));
        }
        for (Listing listing : result.listings) {
            String price = listing.price > 0 ? String.format(Locale.getDefault(), "$%.2f", listing.price)
                    : getString(R.string.listing_free);
            String title = listing.title == null || listing.title.isEmpty()
                    ? price : listing.title;
            String subtitle = joinParts(price, listing.campus, listing.sellerUsername);
            items.add(SearchResultItem.listing(title, subtitle, listing));
        }
        return items;
    }

    private String joinParts(String... parts) {
        ArrayList<String> cleaned = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) cleaned.add(part);
        }
        return android.text.TextUtils.join(" · ", cleaned);
    }

    private final class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ResultHolder> {
        private final ArrayList<SearchResultItem> items = new ArrayList<>();

        void submitList(List<SearchResultItem> next) {
            items.clear();
            if (next != null) items.addAll(next);
            notifyDataSetChanged();
        }

        void appendList(List<SearchResultItem> more) {
            if (more == null || more.isEmpty()) return;
            int start = items.size();
            items.addAll(more);
            notifyItemRangeInserted(start, more.size());
        }

        @NonNull
        @Override
        public ResultHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_search_result, parent, false);
            return new ResultHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ResultHolder holder, int position) {
            SearchResultItem item = items.get(position);
            holder.title.setText(item.title);
            holder.subtitle.setText(item.subtitle == null ? "" : item.subtitle);
            holder.icon.setImageResource(item.type == SearchResultType.USER
                    ? R.drawable.ic_tab_profile
                    : item.type == SearchResultType.LISTING ? R.drawable.ic_tab_listings : R.drawable.ic_tab_search);
            holder.itemView.setOnClickListener(v -> onSearchItemSelected(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class ResultHolder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView title;
            final TextView subtitle;

            ResultHolder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.ivSearchResultIcon);
                title = itemView.findViewById(R.id.tvSearchResultTitle);
                subtitle = itemView.findViewById(R.id.tvSearchResultSubtitle);
            }
        }
    }

    private void onSearchItemSelected(SearchResultItem item) {
        if (item.type == SearchResultType.RECENT) {
            etQuery.setText(item.queryOrUsername);
            etQuery.setSelection(etQuery.getText() == null ? 0 : etQuery.getText().length());
            searchPage = 1;
            runSearch();
            return;
        }
        if (item.type == SearchResultType.LISTING && item.listing != null) {
            Intent intent = new Intent(requireContext(), ListingDetailActivity.class);
            intent.putExtra(ListingDetailActivity.EXTRA_LISTING, item.listing);
            startActivity(intent);
            return;
        }
        if (item.type == SearchResultType.USER && item.queryOrUsername != null && !item.queryOrUsername.isEmpty()) {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).openProfile(item.queryOrUsername);
            }
        }
    }

    private enum SearchResultType {
        USER, LISTING, RECENT
    }

    private static final class SearchResultItem {
        final SearchResultType type;
        final String title;
        final String subtitle;
        final String queryOrUsername;
        final Listing listing;

        private SearchResultItem(SearchResultType type, String title, String subtitle, String queryOrUsername,
                                 Listing listing) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.queryOrUsername = queryOrUsername;
            this.listing = listing;
        }

        static SearchResultItem user(String title, String subtitle, String username) {
            return new SearchResultItem(SearchResultType.USER, title, subtitle, username, null);
        }

        static SearchResultItem listing(String title, String subtitle, Listing listing) {
            return new SearchResultItem(SearchResultType.LISTING, title, subtitle, null, listing);
        }

        static SearchResultItem recent(String query) {
            return new SearchResultItem(SearchResultType.RECENT, query, null, query, null);
        }
    }
}
