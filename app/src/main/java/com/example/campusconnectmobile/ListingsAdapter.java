package com.example.campusconnectmobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/** Square 3-column grid adapter shared by the standalone Listings tab and every Profile tab. */
public class ListingsAdapter extends RecyclerView.Adapter<ListingsAdapter.ListingViewHolder> {

    public interface OnListingClickListener {
        void onListingClick(Listing listing);
    }

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("$#,##0.##");

    private final List<Listing> listings = new ArrayList<>();
    private final OnListingClickListener listener;

    public ListingsAdapter(OnListingClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Listing> newListings) {
        listings.clear();
        listings.addAll(newListings);
        notifyDataSetChanged();
    }

    public void appendList(List<Listing> more) {
        int start = listings.size();
        listings.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public void removeListing(int listingId) {
        for (int i = 0; i < listings.size(); i++) {
            if (listings.get(i).id == listingId) {
                listings.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
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

    public List<Listing> currentList() {
        return listings;
    }

    public boolean isEmpty() {
        return listings.isEmpty();
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_listing_grid, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        holder.bind(listings.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    static final class ListingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivImage;
        private final TextView tvPrice;
        private final TextView tvSoldBadge;
        private final ImageView ivSaved;
        private final TextView tvUrgency;

        ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivListingImage);
            tvPrice = itemView.findViewById(R.id.tvListingPrice);
            tvSoldBadge = itemView.findViewById(R.id.tvListingSoldBadge);
            ivSaved = itemView.findViewById(R.id.ivListingSaved);
            tvUrgency = itemView.findViewById(R.id.tvListingUrgency);
        }

        void bind(Listing listing, OnListingClickListener listener) {
            String cover = listing.coverImage != null && !listing.coverImage.isEmpty()
                    ? listing.coverImage
                    : (listing.imageUrls.isEmpty() ? "" : listing.imageUrls.get(0));
            Glide.with(ivImage.getContext())
                    .load(CcwHttp.resolveUrl(cover))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.bg_skeleton)
                    .error(R.drawable.bg_skeleton)
                    .centerCrop()
                    .into(ivImage);

            tvPrice.setText(listing.price > 0 ? PRICE_FORMAT.format(listing.price) : "Free");
            tvSoldBadge.setVisibility(listing.isSold() ? View.VISIBLE : View.GONE);
            ivSaved.setVisibility(listing.isSaved ? View.VISIBLE : View.GONE);
            String urgency = ListingUrgency.badgeText(tvUrgency.getContext(), listing);
            tvUrgency.setText(urgency);
            tvUrgency.setVisibility(urgency == null || listing.isSold() ? View.GONE : View.VISIBLE);
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onListingClick(listing);
            });
        }
    }
}
