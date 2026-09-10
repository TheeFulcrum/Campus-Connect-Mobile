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
        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));

        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());

        etQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Search for gigs, textbooks, tutoring, and more.");
                    rvResults.setAdapter(null);
                    return;
                }

                List<DatabaseHelper.Post> results = dbHelper.searchPosts(query);
                if (results.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("No results for \"" + query + "\".");
                    rvResults.setAdapter(null);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvResults.setAdapter(new PostAdapter(results));
                }
            }
        });

        return view;
    }
}