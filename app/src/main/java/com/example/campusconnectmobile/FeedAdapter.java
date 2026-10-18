package com.example.campusconnectmobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/** Card feed of server listings shown on the Home tab. */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    public interface Listener {
        void onOpen(Listing listing);

        void onToggleSave(Listing listing);
    }

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("$#,##0.##");

    private final List<Listing> listings = new ArrayList<>();
    private final Listener listener;
    private final String myUsername;

    public FeedAdapter(String myUsername, Listener listener) {
        this.myUsername = myUsername;
        this.listener = listener;
    }

    public void submitList(List<Listing> items) {
        listings.clear();
        listings.addAll(items);
        notifyDataSetChanged();
    }

    public void appendList(List<Listing> items) {
        int start = listings.size();
        listings.addAll(items);
        notifyItemRangeInserted(start, items.size());
    }

    public void updateListing(Listing updated) {
        for (int i = 0; i < listings.size(); i++) {
            if (listings.get(i).id == updated.id) {
                listings.set(i, updated);
                notifyItemChanged(i);
                return;
            }
        }
    }

    public boolean isEmpty() {
        return listings.isEmpty();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        Listing listing = listings.get(position);
        boolean mine = myUsername != null && myUsername.equalsIgnoreCase(listing.sellerUsername);

        holder.author.setText(listing.sellerUsername == null || listing.sellerUsername.isEmpty()
                ? "" : "@" + listing.sellerUsername);
        holder.category.setText(listing.category);

        StringBuilder caption = new StringBuilder(listing.title == null ? "" : listing.title);
        caption.append('\n').append(listing.price > 0 ? PRICE_FORMAT.format(listing.price)
                : holder.itemView.getContext().getString(R.string.listing_free));
        if (listing.description != null && !listing.description.isEmpty()) {
            caption.append('\n').append(listing.description);
        }
        holder.caption.setText(caption);

        ArrayList<String> meta = new ArrayList<>();
        if (listing.campus != null && !listing.campus.isEmpty()) meta.add(listing.campus);
        String stock = ListingUrgency.stockText(holder.itemView.getContext(), listing);
        if (stock != null) meta.add(stock);
        String event = ListingUrgency.eventText(holder.itemView.getContext(), listing);
        if (event != null) meta.add(event);
        holder.campus.setText(android.text.TextUtils.join(" · ", meta));

        String cover = listing.coverImage != null && !listing.coverImage.isEmpty()
                ? listing.coverImage
                : (listing.imageUrls.isEmpty() ? "" : listing.imageUrls.get(0));
        int placeholder = imageForCategory(listing.category);
        if (cover.isEmpty()) {
            holder.image.setImageResource(placeholder);
        } else {
            Glide.with(holder.image)
                    .load(CcwHttp.resolveUrl(cover))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(placeholder)
                    .error(placeholder)
                    .centerCrop()
                    .into(holder.image);
        }

        holder.save.setImageResource(listing.isSaved ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        holder.save.setColorFilter(holder.itemView.getContext().getColor(
                listing.isSaved ? R.color.colorAccent : R.color.colorTextMuted));
        holder.saveCount.setText(String.valueOf(listing.saves));
        holder.save.setOnClickListener(v -> listener.onToggleSave(listing));

        holder.action.setText(mine ? R.string.feed_view : R.string.feed_message);
        holder.action.setOnClickListener(v -> listener.onOpen(listing));
        holder.itemView.setOnClickListener(v -> listener.onOpen(listing));
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    private static int imageForCategory(String category) {
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

    static final class FeedViewHolder extends RecyclerView.ViewHolder {
        final TextView author;
        final TextView category;
        final TextView caption;
        final TextView campus;
        final TextView saveCount;
        final ImageView image;
        final ImageButton save;
        final TextView action;

        FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            author = itemView.findViewById(R.id.tvPostAuthor);
            category = itemView.findViewById(R.id.tvPostCategory);
            caption = itemView.findViewById(R.id.tvPostCaption);
            campus = itemView.findViewById(R.id.tvPostCampus);
            saveCount = itemView.findViewById(R.id.tvLikeCount);
            image = itemView.findViewById(R.id.ivPostImage);
            save = itemView.findViewById(R.id.btnLikePost);
            action = itemView.findViewById(R.id.btnMessageSeller);
        }
    }
}
