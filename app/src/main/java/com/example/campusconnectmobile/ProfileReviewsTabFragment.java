package com.example.campusconnectmobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class ProfileReviewsTabFragment extends Fragment {

    private static final String ARG_USERNAME = "username";
    private SessionManager sessionManager;
    private String username;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private final List<Review> reviews = new ArrayList<>();
    private ReviewsAdapter adapter;

    public static ProfileReviewsTabFragment newInstance(String username) {
        ProfileReviewsTabFragment fragment = new ProfileReviewsTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_reviews_tab, container, false);
        username = getArguments() == null ? "" : getArguments().getString(ARG_USERNAME, "");
        sessionManager = new SessionManager(requireContext());
        swipeRefresh = view.findViewById(R.id.swipeReviews);
        tvEmpty = view.findViewById(R.id.tvReviewsEmpty);
        RecyclerView rv = view.findViewById(R.id.rvReviews);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReviewsAdapter();
        rv.setAdapter(adapter);
        swipeRefresh.setOnRefreshListener(this::loadReviews);
        loadReviews();
        return view;
    }

    private void loadReviews() {
        CcwApiClient.getProfileReviews(username, 1, sessionManager.getAuthToken(),
                new CcwApiClient.Callback<ReviewsPage>() {
                    @Override
                    public void onSuccess(ReviewsPage result) {
                        if (!isAdded()) return;
                        swipeRefresh.setRefreshing(false);
                        reviews.clear();
                        if (result != null) reviews.addAll(result.items);
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        swipeRefresh.setRefreshing(false);
                        tvEmpty.setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private final class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewHolder> {
        @NonNull
        @Override
        public ReviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_profile_review, parent, false);
            return new ReviewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReviewHolder holder, int position) {
            Review review = reviews.get(position);
            String header = "@" + review.reviewerUsername + " • " +
                    getString(R.string.profile_rating_format, review.rating / 1.0);
            holder.header.setText(header);
            holder.comment.setText(review.comment == null || review.comment.isEmpty()
                    ? getString(R.string.profile_review_no_comment)
                    : review.comment);
        }

        @Override
        public int getItemCount() {
            return reviews.size();
        }

        final class ReviewHolder extends RecyclerView.ViewHolder {
            final TextView header;
            final TextView comment;

            ReviewHolder(@NonNull View itemView) {
                super(itemView);
                header = itemView.findViewById(R.id.tvReviewHeader);
                comment = itemView.findViewById(R.id.tvReviewComment);
            }
        }
    }
}
