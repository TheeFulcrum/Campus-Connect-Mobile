package com.example.campusconnectmobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final List<DatabaseHelper.Post> posts;

    public PostAdapter(List<DatabaseHelper.Post> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        DatabaseHelper.Post post = posts.get(position);
        holder.tvAuthor.setText(post.username);
        holder.tvCategory.setText(post.category);
        holder.tvCaption.setText(post.caption);
        holder.tvCampus.setText(post.campus);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvCategory, tvCaption, tvCampus;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvPostAuthor);
            tvCategory = itemView.findViewById(R.id.tvPostCategory);
            tvCaption = itemView.findViewById(R.id.tvPostCaption);
            tvCampus = itemView.findViewById(R.id.tvPostCampus);
        }
    }
}