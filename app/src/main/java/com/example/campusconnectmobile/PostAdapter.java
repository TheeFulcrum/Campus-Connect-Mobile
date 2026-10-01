package com.example.campusconnectmobile;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
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

        if (post.imageUri != null && !post.imageUri.isEmpty()) {
            try {
                holder.ivImage.setImageURI(Uri.parse(post.imageUri));
            } catch (Exception e) {
                holder.ivImage.setImageResource(imageForCategory(post.category));
            }
        } else {
            holder.ivImage.setImageResource(imageForCategory(post.category));
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    private int imageForCategory(String category) {
        String value = category == null ? "" : category.toLowerCase();
        if (value.contains("tutor") || value.contains("academic")) return R.drawable.tutoring;
        if (value.contains("furniture") || value.contains("housing")) return R.drawable.furniture;
        if (value.contains("electronic")) return R.drawable.electronics;
        if (value.contains("book") || value.contains("textbook")) return R.drawable.books;
        if (value.contains("repair")) return R.drawable.repairs;
        if (value.contains("beauty")) return R.drawable.beauty;
        if (value.contains("creative") || value.contains("event")) return R.drawable.creative;
        if (value.contains("gig") || value.contains("job") || value.contains("help")) return R.drawable.campus_help;
        return R.drawable.tutoring;
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvCategory, tvCaption, tvCampus;
        ImageView ivImage;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvPostAuthor);
            tvCategory = itemView.findViewById(R.id.tvPostCategory);
            tvCaption = itemView.findViewById(R.id.tvPostCaption);
            tvCampus = itemView.findViewById(R.id.tvPostCampus);
            ivImage = itemView.findViewById(R.id.ivPostImage);
        }
    }
}