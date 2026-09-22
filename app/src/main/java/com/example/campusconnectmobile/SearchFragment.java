package com.example.campusconnectmobile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

public class SearchFragment extends Fragment {

    private static final String ARG_EMAIL = "arg_email";

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

        EditText etQuery = view.findViewById(R.id.etSearchQuery);
        TextView tvEmpty = view.findViewById(R.id.tvSearchEmpty);
        RecyclerView rvResults = view.findViewById(R.id.rvSearchResults);
        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.advertsRefresh);
        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));

        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());

        Runnable refreshResults = () -> {
            String query = etQuery.getText().toString().trim().toLowerCase();
            List<DatabaseHelper.Post> results = dbHelper.getAllPosts();
            if (!query.isEmpty()) {
                results.removeIf(post -> !(post.caption.toLowerCase().contains(query)
                        || post.category.toLowerCase().contains(query)
                        || post.username.toLowerCase().contains(query)));
            }
            tvEmpty.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
            tvEmpty.setText(query.isEmpty() ? "No adverts available yet." : "No results for \"" + query + "\".");
            rvResults.setAdapter(results.isEmpty() ? null : new PostAdapter(results));
        };

        refreshLayout.setOnRefreshListener(() -> {
            refreshResults.run();
            refreshLayout.setRefreshing(false);
            rvResults.scrollToPosition(0);
        });
        refreshResults.run();

        etQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                refreshResults.run();
            }
        });

        return view;
    }
}