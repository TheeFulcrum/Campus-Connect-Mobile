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

import java.util.List;

public class AdvertsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_adverts, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.rvAdverts);
        TextView emptyView = view.findViewById(R.id.tvAdvertsEmpty);
        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefreshAdverts);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());

        Runnable loadAdverts = () -> {
            List<DatabaseHelper.Post> posts = dbHelper.getAllPosts();
            if (posts.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.setAdapter(new PostAdapter(posts));
            }
        };

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                loadAdverts.run();
                swipeRefresh.setRefreshing(false);
            });
        }

        loadAdverts.run();
        return view;
    }
}
